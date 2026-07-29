package com.example.pos.data.order

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.pos.db.PosDatabase
import com.example.pos.domain.Cart
import com.example.pos.domain.Order
import com.example.pos.domain.OrderSyncState
import com.example.pos.domain.Product
import com.example.pos.domain.toOrder
import kotlin.coroutines.CoroutineContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/**
 * Exercises the real schema and queries against in-memory SQLite. Android unit tests run on the
 * JVM, so this needs no device and no extra Kotlin target.
 */
class OrderRepositoryTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: OrderRepository

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        PosDatabase.Schema.create(driver)
        repository =
            DefaultOrderRepository(
                OrderLocalDataSource(PosDatabase(driver), Dispatchers.Unconfined),
            )
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun aSavedOrderComesBackWithItsLinesAndTotals() = runTest {
        val order = orderOf(id = "order-1", quantity = 2)

        repository.save(order)

        val stored = assertNotNull(repository.findById("order-1"))
        assertEquals(order.lines, stored.lines)
        assertEquals(order.totals, stored.totals)
        assertEquals(order.createdAtEpochMillis, stored.createdAtEpochMillis)
        assertEquals(OrderSyncState.PENDING, stored.syncState)
        assertEquals(0, stored.attemptCount)
        assertNull(stored.syncedAtEpochMillis)
    }

    @Test
    fun savingTheSameUuidTwiceCannotCreateADuplicate() = runTest {
        val original = orderOf(id = "order-1", quantity = 2)
        repository.save(original)

        // A retry of the same checkout, even carrying different numbers, must not land twice.
        repository.save(orderOf(id = "order-1", quantity = 9))

        val orders = repository.observeOrders().first()
        assertEquals(1, orders.size)
        assertEquals(original.totals, orders.single().totals)
    }

    @Test
    fun ordersAreObservedNewestFirst() = runTest {
        repository.save(orderOf(id = "older", createdAt = 1_000))
        repository.save(orderOf(id = "newer", createdAt = 2_000))

        assertEquals(listOf("newer", "older"), repository.observeOrders().first().map { it.id })
    }

    @Test
    fun observingEmitsAgainAfterEveryWrite() = runTest {
        val emissions = mutableListOf<List<Order>>()
        val collection =
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                repository.observeOrders().toList(emissions)
            }

        runCurrent()
        repository.save(orderOf(id = "order-1"))
        runCurrent()
        repository.markSynced("order-1", syncedAtEpochMillis = 5_000)
        runCurrent()
        collection.cancel()

        assertEquals(3, emissions.size, "expected empty, after insert, after update")
        assertTrue(emissions.first().isEmpty())
        assertEquals(OrderSyncState.PENDING, emissions[1].single().syncState)
        assertEquals(OrderSyncState.SYNCED, emissions[2].single().syncState)
    }

    @Test
    fun unsyncedOrdersSkipSyncedOnesAndRunOldestFirst() = runTest {
        repository.save(orderOf(id = "older", createdAt = 1_000))
        repository.save(orderOf(id = "newer", createdAt = 2_000))
        repository.save(orderOf(id = "done", createdAt = 3_000))
        repository.markSynced("done", syncedAtEpochMillis = 4_000)

        assertEquals(listOf("older", "newer"), repository.unsyncedOrders().map { it.id })
    }

    @Test
    fun aFailedOrderStaysUnsyncedAndKeepsItsError() = runTest {
        repository.save(orderOf(id = "order-1"))

        repository.recordSyncAttempt("order-1")
        repository.recordSyncError("order-1", "503 Service Unavailable")

        val failed = assertNotNull(repository.findById("order-1"))
        assertEquals(OrderSyncState.FAILED, failed.syncState)
        assertEquals(1, failed.attemptCount)
        assertEquals("503 Service Unavailable", failed.lastError)
        assertEquals(listOf("order-1"), repository.unsyncedOrders().map { it.id })
    }

    @Test
    fun attemptsAccumulateAcrossRetries() = runTest {
        repository.save(orderOf(id = "order-1"))

        repeat(3) { repository.recordSyncAttempt("order-1") }

        assertEquals(3, assertNotNull(repository.findById("order-1")).attemptCount)
    }

    @Test
    fun aSuccessfulSyncClearsTheEarlierError() = runTest {
        repository.save(orderOf(id = "order-1"))
        repository.recordSyncAttempt("order-1")
        repository.recordSyncError("order-1", "503 Service Unavailable")

        repository.recordSyncAttempt("order-1")
        repository.markSynced("order-1", syncedAtEpochMillis = 9_000)

        val synced = assertNotNull(repository.findById("order-1"))
        assertEquals(OrderSyncState.SYNCED, synced.syncState)
        assertEquals(9_000, synced.syncedAtEpochMillis)
        assertNull(synced.lastError)
        assertEquals(2, synced.attemptCount)
        assertTrue(repository.unsyncedOrders().isEmpty())
    }

    @Test
    fun anUnknownOrderIdIsSimplyMissing() = runTest {
        assertNull(repository.findById("nope"))
    }

    /**
     * SQLite calls block. This checks they are handed to the injected dispatcher rather than run
     * inline on the caller — which on Android is the main thread.
     */
    @Test
    fun databaseWorkIsDispatchedAwayFromTheCaller() = runTest {
        val dispatcher = CountingDispatcher()
        val dispatched =
            DefaultOrderRepository(OrderLocalDataSource(PosDatabase(driver), dispatcher))

        dispatched.save(orderOf("order-1"))
        assertNotNull(dispatched.findById("order-1"))

        assertTrue(
            dispatcher.dispatchCount >= 2,
            "each database call should hop onto the I/O dispatcher, saw ${dispatcher.dispatchCount}",
        )
    }

    private class CountingDispatcher : CoroutineDispatcher() {
        var dispatchCount = 0
            private set

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            dispatchCount++
            Dispatchers.Default.dispatch(context, block)
        }
    }

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
