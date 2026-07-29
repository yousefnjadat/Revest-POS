package com.example.pos.presentation

import com.example.pos.data.catalog.CatalogResult
import com.example.pos.data.sync.OrderSyncCoordinator
import com.example.pos.domain.OrderSyncState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class PosViewModelTest {
    private val mainDispatcher = StandardTestDispatcher()
    private val checkoutMillis = 1_700_000_000_000L
    private val syncedAtMillis = 1_700_000_500_000L

    private lateinit var catalog: FakeCatalogRepository
    private lateinit var orders: FakeOrderRepository
    private lateinit var api: FakeOrderSyncApi

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        catalog = FakeCatalogRepository()
        orders = FakeOrderRepository()
        api = FakeOrderSyncApi()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- catalog -------------------------------------------------------------------------

    @Test
    fun theCatalogLoadsOnInitialisation() = runTest(mainDispatcher) {
        val viewModel = viewModel()

        assertIs<CatalogUiState.Loading>(viewModel.state.value.catalog)
        advanceUntilIdle()

        val content = assertIs<CatalogUiState.Content>(viewModel.state.value.catalog)
        assertEquals(SampleProducts, content.products)
        assertEquals(1, catalog.loadCount)
    }

    @Test
    fun anEmptyCatalogIsItsOwnState() = runTest(mainDispatcher) {
        catalog.result = CatalogResult.Success(emptyList())

        val viewModel = viewModel().also { advanceUntilIdle() }

        assertIs<CatalogUiState.Empty>(viewModel.state.value.catalog)
    }

    @Test
    fun aCatalogFailureSurfacesItsMessageAndRetryRecovers() = runTest(mainDispatcher) {
        catalog.result = CatalogResult.Failure("Couldn't load the catalog.")
        val viewModel = viewModel().also { advanceUntilIdle() }

        assertEquals(
            "Couldn't load the catalog.",
            assertIs<CatalogUiState.Error>(viewModel.state.value.catalog).message,
        )

        catalog.result = CatalogResult.Success(SampleProducts)
        viewModel.retryCatalog()
        advanceUntilIdle()

        assertIs<CatalogUiState.Content>(viewModel.state.value.catalog)
        assertEquals(2, catalog.loadCount)
    }

    // --- cart ----------------------------------------------------------------------------

    @Test
    fun addingProductsUpdatesTheLinesAndTotals() = runTest(mainDispatcher) {
        val viewModel = readyViewModel()

        viewModel.addProduct("mug")
        viewModel.addProduct("mug")
        viewModel.addProduct("muffin")

        val state = viewModel.state.value
        assertEquals(2, state.cartLines.size)
        assertEquals(3, state.cartItemCount)
        // 2 x 17.50 taxable + 1 x 2.95 exempt
        assertEquals(3_795, state.totals.subtotalCents)
        assertEquals(3_500, state.totals.taxableSubtotalCents)
        assertEquals(350, state.totals.taxCents)
        assertEquals(0, state.totals.discountCents)
        assertEquals(4_145, state.totals.totalCents)
    }

    @Test
    fun theDiscountAppearsOnceTheSubtotalReachesFifty() = runTest(mainDispatcher) {
        val viewModel = readyViewModel()

        viewModel.addProduct("mug")
        viewModel.addProduct("flask")

        val totals = viewModel.state.value.totals
        assertEquals(5_000, totals.subtotalCents)
        assertTrue(totals.discountApplied)
        assertEquals(250, totals.discountCents)
        assertEquals(5_250, totals.totalCents)
    }

    @Test
    fun quantityStopsAtTheAvailableStock() = runTest(mainDispatcher) {
        val viewModel = readyViewModel()
        val messages = collectMessages(viewModel)

        repeat(4) { viewModel.addProduct("flask") } // stock is 2
        advanceUntilIdle()

        assertEquals(2, viewModel.state.value.cart.quantityOf("flask"))
        assertTrue(messages.any { it.text.contains("Only 2") }, messages.toString())
    }

    @Test
    fun anOutOfStockProductIsNeverAddedToTheCart() = runTest(mainDispatcher) {
        val viewModel = readyViewModel()
        val messages = collectMessages(viewModel)

        viewModel.addProduct("apron")
        advanceUntilIdle()

        assertTrue(viewModel.state.value.cart.isEmpty)
        assertEquals(UserMessage.Tone.Error, messages.single().tone)
    }

    @Test
    fun decreasingTheLastUnitRemovesTheLine() = runTest(mainDispatcher) {
        val viewModel = readyViewModel()
        viewModel.addProduct("mug")
        viewModel.addProduct("mug")

        viewModel.decreaseProduct("mug")
        assertEquals(1, viewModel.state.value.cart.quantityOf("mug"))

        viewModel.decreaseProduct("mug")
        assertTrue(viewModel.state.value.cart.isEmpty)
    }

    @Test
    fun removingAProductDropsTheWholeLine() = runTest(mainDispatcher) {
        val viewModel = readyViewModel()
        viewModel.addProduct("mug")
        viewModel.addProduct("mug")

        viewModel.removeProduct("mug")

        assertTrue(viewModel.state.value.cart.isEmpty)
        assertEquals(0, viewModel.state.value.totals.totalCents)
    }

    @Test
    fun anUnknownProductIdIsIgnored() = runTest(mainDispatcher) {
        val viewModel = readyViewModel()

        viewModel.addProduct("nope")
        viewModel.decreaseProduct("nope")
        viewModel.removeProduct("nope")

        assertTrue(viewModel.state.value.cart.isEmpty)
    }

    // --- navigation ----------------------------------------------------------------------

    @Test
    fun selectingADestinationMovesTheApp() = runTest(mainDispatcher) {
        val viewModel = readyViewModel()

        viewModel.selectDestination(AppDestination.Orders)

        assertEquals(AppDestination.Orders, viewModel.state.value.destination)
    }

    // --- checkout ------------------------------------------------------------------------

    @Test
    fun checkingOutAnEmptyCartDoesNothingButSayWhy() = runTest(mainDispatcher) {
        val viewModel = readyViewModel()
        val messages = collectMessages(viewModel)

        viewModel.checkout()
        advanceUntilIdle()

        assertNull(orders.findById("order-1"))
        assertEquals(1, messages.size)
        assertEquals(AppDestination.Catalog, viewModel.state.value.destination)
    }

    @Test
    fun checkoutPersistsTheOrderClearsTheCartAndOpensTheReceipt() = runTest(mainDispatcher) {
        val viewModel = readyViewModel()
        viewModel.addProduct("mug")

        viewModel.checkout()
        advanceUntilIdle()

        val stored = assertNotNull(orders.findById("order-1"))
        assertEquals(1_750, stored.totals.subtotalCents)
        assertEquals(checkoutMillis, stored.createdAtEpochMillis)

        val state = viewModel.state.value
        assertTrue(state.cart.isEmpty, "the cart is cleared once the order is stored")
        assertTrue(!state.isCheckingOut)
        assertEquals(AppDestination.Receipt("order-1"), state.destination)
        assertEquals(listOf("order-1"), state.orders.map { it.id })
    }

    @Test
    fun aFailedLocalSaveKeepsTheCartAndNeverReachesTheBackend() = runTest(mainDispatcher) {
        val viewModel = readyViewModel()
        val messages = collectMessages(viewModel)
        viewModel.addProduct("mug")
        orders.failOnSave = true

        viewModel.checkout()
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.cart.quantityOf("mug"))
        assertTrue(!viewModel.state.value.isCheckingOut)
        assertEquals(AppDestination.Catalog, viewModel.state.value.destination)
        assertTrue(api.submitted.isEmpty(), "nothing may be sent if the order was not stored")
        assertEquals(UserMessage.Tone.Error, messages.last().tone)
    }

    @Test
    fun checkoutWhileOnlineSyncsTheOrder() = runTest(mainDispatcher) {
        val viewModel = readyViewModel()
        viewModel.addProduct("mug")

        viewModel.checkout()
        advanceUntilIdle()

        assertEquals(listOf("order-1"), api.submitted)
        val stored = assertNotNull(orders.findById("order-1"))
        assertEquals(OrderSyncState.SYNCED, stored.syncState)
        assertEquals(syncedAtMillis, stored.syncedAtEpochMillis)
        assertEquals(0, viewModel.state.value.pendingOrderCount)
    }

    @Test
    fun checkoutWhileOfflineStoresTheOrderAsPending() = runTest(mainDispatcher) {
        val viewModel = readyViewModel()
        viewModel.setOnline(false)
        viewModel.addProduct("mug")

        viewModel.checkout()
        advanceUntilIdle()

        assertTrue(api.submitted.isEmpty(), "an offline checkout never calls the backend")
        val stored = assertNotNull(orders.findById("order-1"))
        assertEquals(OrderSyncState.PENDING, stored.syncState)
        assertNull(stored.syncedAtEpochMillis)
        assertEquals(1, viewModel.state.value.pendingOrderCount)
    }

    // --- connectivity and sync -----------------------------------------------------------

    @Test
    fun comingBackOnlineSyncsTheBacklog() = runTest(mainDispatcher) {
        val viewModel = readyViewModel()
        viewModel.setOnline(false)
        viewModel.addProduct("mug")
        viewModel.checkout()
        advanceUntilIdle()
        assertEquals(1, viewModel.state.value.pendingOrderCount)

        viewModel.setOnline(true)
        advanceUntilIdle()

        assertEquals(listOf("order-1"), api.submitted)
        assertEquals(0, viewModel.state.value.pendingOrderCount)
    }

    @Test
    fun goingOfflineNeverStartsASync() = runTest(mainDispatcher) {
        val viewModel = readyViewModel()
        viewModel.addProduct("mug")
        viewModel.checkout()
        advanceUntilIdle()
        api.submitted.clear()

        viewModel.setOnline(false)
        advanceUntilIdle()

        assertTrue(api.submitted.isEmpty())
    }

    @Test
    fun stayingOnlineDoesNotResync() = runTest(mainDispatcher) {
        val viewModel = readyViewModel()
        viewModel.setOnline(false)
        viewModel.addProduct("mug")
        viewModel.checkout()
        advanceUntilIdle()

        viewModel.setOnline(true)
        advanceUntilIdle()
        api.submitted.clear()

        viewModel.setOnline(true)
        advanceUntilIdle()

        assertTrue(api.submitted.isEmpty(), "only the offline -> online edge triggers a sync")
    }

    @Test
    fun manualSyncWhileOfflineExplainsItselfAndSendsNothing() = runTest(mainDispatcher) {
        val viewModel = readyViewModel()
        val messages = collectMessages(viewModel)
        viewModel.setOnline(false)

        viewModel.syncNow()
        advanceUntilIdle()

        assertTrue(api.submitted.isEmpty())
        assertTrue(messages.last().text.contains("offline"), messages.toString())
    }

    @Test
    fun aFailedSyncLeavesTheOrderPendingAndSaysSo() = runTest(mainDispatcher) {
        val viewModel = readyViewModel()
        val messages = collectMessages(viewModel)
        api.failure = IllegalStateException("connection reset")
        viewModel.addProduct("mug")

        viewModel.checkout()
        advanceUntilIdle()

        val stored = assertNotNull(orders.findById("order-1"))
        assertEquals(OrderSyncState.FAILED, stored.syncState)
        assertEquals("connection reset", stored.lastError)
        assertEquals(1, viewModel.state.value.pendingOrderCount)

        api.failure = null
        viewModel.syncNow()
        advanceUntilIdle()

        assertEquals(OrderSyncState.SYNCED, assertNotNull(orders.findById("order-1")).syncState)
        assertEquals(0, viewModel.state.value.pendingOrderCount)
        assertTrue(messages.any { it.text.contains("Synced 1") }, messages.toString())
    }

    @Test
    fun twoSyncTriggersDoNotRunTheLoopTwice() = runTest(mainDispatcher) {
        val viewModel = readyViewModel()
        val gate = CompletableDeferred<Unit>()
        viewModel.setOnline(false)
        viewModel.addProduct("mug")
        viewModel.checkout()
        advanceUntilIdle()

        api.gate = gate
        viewModel.setOnline(true) // came-online sync starts and blocks on the gate
        viewModel.syncNow() // manual trigger arrives while the first run holds the lock
        advanceUntilIdle()
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf("order-1"), api.submitted)
    }

    @Test
    fun syncProgressIsReflectedInState() = runTest(mainDispatcher) {
        val viewModel = readyViewModel()
        val gate = CompletableDeferred<Unit>()
        viewModel.setOnline(false)
        viewModel.addProduct("mug")
        viewModel.checkout()
        advanceUntilIdle()

        api.gate = gate
        viewModel.setOnline(true)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isSyncing, "the syncing indicator should be on")

        gate.complete(Unit)
        advanceUntilIdle()
        assertTrue(!viewModel.state.value.isSyncing)
    }

    // --- helpers -------------------------------------------------------------------------

    private fun viewModel(): PosViewModel {
        var issued = 0
        return PosViewModel(
            catalog = catalog,
            orders = orders,
            sync =
                OrderSyncCoordinator(
                    orders = orders,
                    api = api,
                    now = { syncedAtMillis },
                    log = {},
                ),
            now = { checkoutMillis },
            newOrderId = { "order-${++issued}" },
        )
    }

    private fun TestScope.readyViewModel(): PosViewModel = viewModel().also { advanceUntilIdle() }

    /**
     * Starts collecting before the test acts, so no one-shot message is missed. The unconfined
     * dispatcher subscribes eagerly and delivers on emit, which keeps the assertions independent
     * of when the scheduler happens to run.
     */
    private fun TestScope.collectMessages(viewModel: PosViewModel): List<UserMessage> {
        val messages = mutableListOf<UserMessage>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.messages.toList(messages)
        }
        return messages
    }
}
