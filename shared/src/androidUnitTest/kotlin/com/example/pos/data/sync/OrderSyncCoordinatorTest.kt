package com.example.pos.data.sync

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.pos.data.order.DefaultOrderRepository
import com.example.pos.data.order.remote.KtorOrderSyncApi
import com.example.pos.data.order.local.OrderLocalDataSource
import com.example.pos.domain.repository.OrderRepository
import com.example.pos.data.order.remote.OrderSyncApi
import com.example.pos.data.order.remote.SyncAcknowledgement
import com.example.pos.data.remote.MockPosBackend
import com.example.pos.data.remote.TransientFailureMode
import com.example.pos.db.PosDatabase
import com.example.pos.domain.model.Cart
import com.example.pos.domain.model.Order
import com.example.pos.domain.model.OrderSyncState
import com.example.pos.domain.model.Product
import com.example.pos.domain.model.SyncTrigger
import com.example.pos.domain.model.toOrder
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/**
 * The offline-first workflow end to end: real SQLite underneath, the Ktor MockEngine backend on
 * top, and nothing stubbed in between.
 */
class OrderSyncCoordinatorTest {
    private val syncedAtMillis = 1_700_000_500_000L

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: OrderRepository
    private val logs = mutableListOf<String>()

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        PosDatabase.Schema.create(driver)
        repository =
            DefaultOrderRepository(
                OrderLocalDataSource(PosDatabase(driver), Dispatchers.Unconfined),
            )
        logs.clear()
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun anOrderIsOnlySentAfterItIsAlreadyInTheDatabase() = runTest {
        var wasStoredWhenTheRequestWasMade = false
        val api =
            OrderSyncApi { submitted ->
                wasStoredWhenTheRequestWasMade = repository.findById(submitted.id) != null
                SyncAcknowledgement(submitted.id, duplicate = false)
            }
        repository.save(orderOf("order-1"))

        coordinator(api).sync(SyncTrigger.CHECKOUT)

        assertTrue(wasStoredWhenTheRequestWasMade, "the order must be persisted before syncing")
        assertEquals(OrderSyncState.SYNCED, assertNotNull(repository.findById("order-1")).syncState)
    }

    @Test
    fun theFirstSyncAttemptFailsAndTheOrderStaysPending() = runTest {
        val backend = MockPosBackend(TransientFailureMode.FirstOrderOnly)
        repository.save(orderOf("order-1"))

        val outcome = coordinator(backend).sync(SyncTrigger.MANUAL)

        assertEquals(0, outcome.syncedCount)
        assertEquals(1, outcome.failedCount)

        val stored = assertNotNull(repository.findById("order-1"))
        assertEquals(OrderSyncState.FAILED, stored.syncState)
        assertNull(stored.syncedAtEpochMillis, "a failed order must not be stamped as synced")
        assertEquals("Server responded 503", stored.lastError)
        assertEquals(1, stored.attemptCount)
        assertEquals(listOf("order-1"), repository.unsyncedOrders().map { it.id })
        assertTrue(backend.acceptedOrderIds.isEmpty())
    }

    @Test
    fun theRetryAfterATransientFailureSucceedsAndStampsSyncedAt() = runTest {
        val backend = MockPosBackend(TransientFailureMode.FirstOrderOnly)
        val coordinator = coordinator(backend)
        repository.save(orderOf("order-1"))
        coordinator.sync(SyncTrigger.MANUAL)

        val retry = coordinator.sync(SyncTrigger.MANUAL)

        assertEquals(1, retry.syncedCount)
        assertEquals(0, retry.failedCount)

        val stored = assertNotNull(repository.findById("order-1"))
        assertEquals(OrderSyncState.SYNCED, stored.syncState)
        assertEquals(syncedAtMillis, stored.syncedAtEpochMillis)
        assertNull(stored.lastError, "a successful sync clears the earlier error")
        assertEquals(2, stored.attemptCount)
        assertTrue(repository.unsyncedOrders().isEmpty())
    }

    @Test
    fun aRetriedOrderIsNeverAcceptedTwice() = runTest {
        val backend = MockPosBackend(TransientFailureMode.FirstOrderOnly)
        val coordinator = coordinator(backend)
        repository.save(orderOf("order-1"))

        coordinator.sync(SyncTrigger.MANUAL) // fails
        coordinator.sync(SyncTrigger.MANUAL) // succeeds
        coordinator.sync(SyncTrigger.MANUAL) // nothing left to send

        assertEquals(listOf("order-1"), backend.acceptedOrderIds)
        assertEquals(2, backend.requestCountFor("order-1"), "the synced order is not sent again")
        assertEquals(1, repository.observeOrdersOnce().size)
    }

    @Test
    fun anOrderAlreadyAcceptedByTheBackendIsAcknowledgedNotDuplicated() = runTest {
        // The backend accepted the order but the response never made it back, so the app still
        // has it pending. Re-sending the same UUID must be a no-op on the backend.
        val backend = MockPosBackend(TransientFailureMode.None)
        val api = KtorOrderSyncApi(backend.createClient())
        val order = orderOf("order-1")
        api.submit(order)
        repository.save(order)

        val outcome = coordinator(backend).sync(SyncTrigger.MANUAL)

        assertEquals(1, outcome.syncedCount)
        assertEquals(listOf("order-1"), backend.acceptedOrderIds)
        assertTrue(backend.orderRequests.last().duplicate)
        assertEquals(OrderSyncState.SYNCED, assertNotNull(repository.findById("order-1")).syncState)
    }

    @Test
    fun twoSimultaneousTriggersDoNotRunTheLoopTwice() = runTest {
        val gate = CompletableDeferred<Unit>()
        var submissions = 0
        val api =
            OrderSyncApi { order ->
                submissions++
                gate.await()
                SyncAcknowledgement(order.id, duplicate = false)
            }
        val coordinator = coordinator(api)
        repository.save(orderOf("order-1"))

        val firstRun = async { coordinator.sync(SyncTrigger.MANUAL) }
        runCurrent()
        assertTrue(coordinator.isSyncing.value, "the first run should be in flight")

        val secondRun = coordinator.sync(SyncTrigger.CAME_ONLINE)
        gate.complete(Unit)
        val firstOutcome = firstRun.await()

        assertTrue(secondRun.skipped, "an overlapping trigger must be skipped, not run twice")
        assertEquals(0, secondRun.syncedCount)
        assertEquals(1, firstOutcome.syncedCount)
        assertEquals(1, submissions, "the order must be submitted exactly once")
        assertTrue(!coordinator.isSyncing.value)
        assertTrue(logs.any { it.contains("already running") }, logs.toString())
    }

    @Test
    fun oneFailingOrderDoesNotStopTheRestOfTheBacklog() = runTest {
        val backend = MockPosBackend(TransientFailureMode.FirstOrderOnly)
        repository.save(orderOf("older", createdAt = 1_000))
        repository.save(orderOf("newer", createdAt = 2_000))

        val outcome = coordinator(backend).sync(SyncTrigger.CAME_ONLINE)

        assertEquals(1, outcome.syncedCount)
        assertEquals(1, outcome.failedCount)
        assertEquals(OrderSyncState.FAILED, assertNotNull(repository.findById("older")).syncState)
        assertEquals(OrderSyncState.SYNCED, assertNotNull(repository.findById("newer")).syncState)
        assertEquals(listOf("newer"), backend.acceptedOrderIds)
    }

    @Test
    fun syncingWithAnEmptyBacklogDoesNothing() = runTest {
        val backend = MockPosBackend(TransientFailureMode.None)

        val outcome = coordinator(backend).sync(SyncTrigger.MANUAL)

        assertEquals(0, outcome.syncedCount)
        assertEquals(0, outcome.failedCount)
        assertTrue(!outcome.skipped)
        assertTrue(backend.orderRequests.isEmpty())
        assertTrue(logs.any { it.contains("nothing pending") }, logs.toString())
    }

    @Test
    fun everyRunLogsWhySyncStarted() = runTest {
        val backend = MockPosBackend(TransientFailureMode.None)
        val coordinator = coordinator(backend)

        coordinator.sync(SyncTrigger.MANUAL)
        coordinator.sync(SyncTrigger.CAME_ONLINE)
        coordinator.sync(SyncTrigger.CHECKOUT)

        assertTrue(logs.any { it.contains("manual trigger") }, logs.toString())
        assertTrue(logs.any { it.contains("changed to online") }, logs.toString())
        assertTrue(logs.any { it.contains("checkout while online") }, logs.toString())
    }

    private fun coordinator(backend: MockPosBackend) =
        coordinator(KtorOrderSyncApi(backend.createClient()))

    private fun coordinator(api: OrderSyncApi) =
        OrderSyncCoordinator(
            orders = repository,
            api = api,
            now = { syncedAtMillis },
            log = { logs += it },
        )

    private suspend fun OrderRepository.observeOrdersOnce(): List<Order> = observeOrders().first()

    private fun orderOf(
        id: String,
        quantity: Int = 1,
        createdAt: Long = 1_700_000_000_000,
    ): Order {
        val mug =
            Product(
                id = "sku-2001",
                name = "Ceramic Mug",
                priceCents = 1_750,
                stock = 12,
                taxable = true,
            )
        return Cart().add(mug, quantity).toOrder(id = id, createdAtEpochMillis = createdAt)
    }
}

/** Small SAM-style helper so tests can express a fake backend as a lambda. */
private fun OrderSyncApi(submit: suspend (Order) -> SyncAcknowledgement): OrderSyncApi =
    object : OrderSyncApi {
        override suspend fun submit(order: Order): SyncAcknowledgement = submit(order)
    }
