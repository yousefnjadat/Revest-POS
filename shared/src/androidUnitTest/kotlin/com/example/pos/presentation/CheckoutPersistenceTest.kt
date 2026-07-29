package com.example.pos.presentation

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.pos.data.order.DefaultOrderRepository
import com.example.pos.data.order.KtorOrderSyncApi
import com.example.pos.data.order.OrderLocalDataSource
import com.example.pos.data.order.OrderRepository
import com.example.pos.data.remote.MockPosBackend
import com.example.pos.data.remote.TransientFailureMode
import com.example.pos.data.sync.OrderSyncCoordinator
import com.example.pos.db.PosDatabase
import com.example.pos.domain.Order
import com.example.pos.domain.OrderSyncState
import com.example.pos.domain.SyncTrigger
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * The acceptance path with nothing faked below the view model: a real SQLite database, the real
 * repository, the real sync coordinator, and the Ktor MockEngine backend. The view-model unit
 * tests use in-memory fakes for speed; this one proves the wiring they stand in for holds.
 *
 * Waiting note: Ktor's MockEngine completes on a real dispatcher, so virtual-time
 * `advanceUntilIdle()` cannot be used to observe a sync finishing. Tests either await the
 * database reaching a state (`awaitOrders`) or call the coordinator's suspending `sync` directly.
 */
class CheckoutPersistenceTest {
    private val mainDispatcher = StandardTestDispatcher()
    private val syncedAtMillis = 1_772_000_500_000L

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: OrderRepository
    private lateinit var backend: MockPosBackend
    private lateinit var coordinator: OrderSyncCoordinator
    private val syncLogs = mutableListOf<String>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        PosDatabase.Schema.create(driver)
        repository = newRepository()
        backend = MockPosBackend(TransientFailureMode.FirstOrderOnly)
        coordinator =
            OrderSyncCoordinator(
                orders = repository,
                api = KtorOrderSyncApi(backend.createClient()),
                now = { syncedAtMillis },
                log = { syncLogs += it },
            )
        syncLogs.clear()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        driver.close()
    }

    @Test
    fun anOfflineCheckoutIsWrittenToTheDatabaseBeforeAnyRequest() = runTest(mainDispatcher) {
        val viewModel = readyViewModel()
        viewModel.setOnline(false)
        repeat(3) { viewModel.addProduct("mug") }

        viewModel.checkout()

        // A separate repository over the same database — what a relaunch would read.
        val stored = newRepository().observeOrders().first { it.isNotEmpty() }.single()
        assertEquals(OrderSyncState.PENDING, stored.syncState)
        assertNull(stored.syncedAtEpochMillis)
        assertEquals(3, stored.itemCount)
        assertEquals(5_250, stored.totals.subtotalCents)
        assertEquals(525, stored.totals.taxCents)
        assertEquals(263, stored.totals.discountCents)
        assertEquals(5_512, stored.totals.totalCents)

        assertTrue(backend.orderRequests.isEmpty(), "an offline checkout must not call the backend")
        advanceUntilIdle()
        assertTrue(viewModel.state.value.cart.isEmpty, "the cart clears once the order is stored")
    }

    @Test
    fun theStoredOrderIdIsARandomUuidUsableAsAnIdempotencyKey() = runTest(mainDispatcher) {
        // No id is injected here, so this exercises the production UUID generator.
        val viewModel = readyViewModel()
        viewModel.setOnline(false)
        viewModel.addProduct("mug")

        viewModel.checkout()

        val id = awaitOrders { it.isNotEmpty() }.single().id
        assertEquals(36, id.length, "expected a canonical UUID, got '$id'")
        assertEquals(4, id.count { it == '-' })
        assertEquals(id.lowercase(), id, "ids are stored lower-case so key comparison is stable")
    }

    @Test
    fun reconnectingSyncsThePersistedOrderAndTheRetryAddsNoRowAndNoSecondAcceptance() =
        runTest(mainDispatcher) {
            val viewModel = readyViewModel()
            viewModel.setOnline(false)
            viewModel.addProduct("mug")
            viewModel.checkout()
            val orderId = awaitOrders { it.isNotEmpty() }.single().id

            // Reconnecting fires the automatic sync, which the backend fails exactly once.
            viewModel.setOnline(true)
            val failed = awaitOrders { it.single().syncState == OrderSyncState.FAILED }.single()

            assertEquals("Server responded 503", failed.lastError)
            assertEquals(1, failed.attemptCount)
            assertNull(failed.syncedAtEpochMillis)
            assertTrue(backend.acceptedOrderIds.isEmpty(), "a failed order is never accepted")
            assertTrue(syncLogs.any { it.contains("changed to online") }, syncLogs.toString())

            // The retry goes out under the same UUID.
            val retry = coordinator.sync(SyncTrigger.MANUAL)
            assertEquals(1, retry.syncedCount)

            val synced = assertNotNull(repository.findById(orderId))
            assertEquals(OrderSyncState.SYNCED, synced.syncState)
            assertEquals(syncedAtMillis, synced.syncedAtEpochMillis)
            assertNull(synced.lastError)
            assertEquals(2, synced.attemptCount)

            assertEquals(listOf(orderId), backend.acceptedOrderIds, "accepted exactly once")
            assertEquals(2, backend.requestCountFor(orderId))
            assertEquals(1, repository.observeOrders().first().size, "no duplicate row")
        }

    @Test
    fun syncingAgainAfterEverythingIsSyncedSendsNothing() = runTest(mainDispatcher) {
        val viewModel = readyViewModel()
        viewModel.setOnline(false)
        viewModel.addProduct("mug")
        viewModel.checkout()
        awaitOrders { it.isNotEmpty() }
        coordinator.sync(SyncTrigger.MANUAL) // fails once
        coordinator.sync(SyncTrigger.MANUAL) // succeeds
        val requestsSoFar = backend.orderRequests.size
        val accepted = backend.acceptedOrderIds

        val outcome = coordinator.sync(SyncTrigger.MANUAL)

        assertTrue(!outcome.skipped)
        assertEquals(0, outcome.syncedCount)
        assertEquals(0, outcome.failedCount)
        assertEquals(requestsSoFar, backend.orderRequests.size, "nothing pending, nothing sent")
        assertEquals(accepted, backend.acceptedOrderIds)
        assertEquals(1, repository.observeOrders().first().size)
        assertTrue(syncLogs.any { it.contains("nothing pending") }, syncLogs.toString())
    }

    @Test
    fun everyCheckoutIsStoredSeparatelyAndOneFailureDoesNotHoldUpTheOther() =
        runTest(mainDispatcher) {
            val viewModel = readyViewModel()
            viewModel.setOnline(false)
            viewModel.addProduct("mug")
            viewModel.checkout()
            awaitOrders { it.size == 1 }
            viewModel.addProduct("muffin")
            viewModel.checkout()
            val pending = awaitOrders { it.size == 2 }

            assertEquals(2, pending.map { it.id }.toSet().size, "each sale gets its own UUID")
            assertTrue(pending.all { it.syncState == OrderSyncState.PENDING })

            // The backend fails the first order of the batch once; the second still goes through.
            val batch = coordinator.sync(SyncTrigger.CAME_ONLINE)
            assertEquals(1, batch.syncedCount)
            assertEquals(1, batch.failedCount)

            val retry = coordinator.sync(SyncTrigger.MANUAL)
            assertEquals(1, retry.syncedCount)

            val settled = repository.observeOrders().first()
            assertTrue(
                settled.all { it.syncState == OrderSyncState.SYNCED },
                "states=${settled.map { it.syncState }}",
            )
            assertEquals(2, backend.acceptedOrderIds.size)
            assertEquals(2, settled.size, "retrying never adds a row")
        }

    /** Suspends until the stored orders satisfy [predicate]; SQLDelight emits on every write. */
    private suspend fun awaitOrders(predicate: (List<Order>) -> Boolean): List<Order> =
        repository.observeOrders().first(predicate)

    private fun newRepository(): OrderRepository =
        DefaultOrderRepository(OrderLocalDataSource(PosDatabase(driver), Dispatchers.Unconfined))

    private fun TestScope.readyViewModel(): PosViewModel =
        PosViewModel(
            catalog = FakeCatalogRepository(),
            orders = repository,
            sync = coordinator,
        ).also { advanceUntilIdle() }
}
