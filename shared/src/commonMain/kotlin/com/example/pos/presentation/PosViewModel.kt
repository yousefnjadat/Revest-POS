package com.example.pos.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pos.PosLog
import com.example.pos.data.catalog.CatalogRepository
import com.example.pos.data.catalog.CatalogResult
import com.example.pos.data.order.OrderRepository
import com.example.pos.data.sync.OrderSyncCoordinator
import com.example.pos.domain.SyncTrigger
import com.example.pos.domain.toOrder
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The single view model behind every screen. One cart, one catalog, and one order history are
 * shared across destinations, so splitting them apart would mean synchronising them again.
 *
 * All work runs in [viewModelScope]; repositories move their own blocking work off the main
 * thread, so nothing here needs a dispatcher of its own.
 */
class PosViewModel internal constructor(
    private val catalog: CatalogRepository,
    private val orders: OrderRepository,
    private val sync: OrderSyncCoordinator,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val newOrderId: () -> String = { Uuid.random().toString() },
) : ViewModel() {
    private val _state = MutableStateFlow(PosUiState())
    val state: StateFlow<PosUiState> = _state.asStateFlow()

    /**
     * One-shot messages. A small [SharedFlow] rather than an effect framework: a replay of 0 and
     * a little buffer is all a snackbar needs.
     */
    private val _messages = MutableSharedFlow<UserMessage>(extraBufferCapacity = 8)
    val messages: SharedFlow<UserMessage> = _messages.asSharedFlow()

    init {
        loadCatalog()
        observeOrders()
        observeSyncProgress()
    }

    fun loadCatalog() {
        viewModelScope.launch {
            _state.update { it.copy(catalog = CatalogUiState.Loading) }
            val catalogState =
                when (val result = catalog.loadProducts()) {
                    is CatalogResult.Success ->
                        if (result.products.isEmpty()) {
                            CatalogUiState.Empty
                        } else {
                            CatalogUiState.Content(result.products)
                        }

                    is CatalogResult.Failure -> CatalogUiState.Error(result.message)
                }
            _state.update { it.copy(catalog = catalogState) }
        }
    }

    fun retryCatalog() = loadCatalog()

    fun selectDestination(destination: AppDestination) {
        _state.update { it.copy(destination = destination) }
    }

    fun addProduct(productId: String) {
        val current = _state.value
        val product = current.productOrNull(productId) ?: return

        if (!product.inStock) {
            notify("${product.name} is out of stock", UserMessage.Tone.Error)
            return
        }

        val updated = current.cart.add(product)
        if (updated.quantityOf(productId) == current.cart.quantityOf(productId)) {
            notify("Only ${product.stock} of ${product.name} in stock")
            return
        }
        _state.update { it.copy(cart = updated) }
    }

    fun decreaseProduct(productId: String) {
        _state.update { state ->
            state.copy(cart = state.cart.setQuantity(productId, state.cart.quantityOf(productId) - 1))
        }
    }

    fun removeProduct(productId: String) {
        _state.update { it.copy(cart = it.cart.remove(productId)) }
    }

    /**
     * Persist first, then sync. The sale is complete once the order is in the database — the
     * network attempt afterwards must never hold up the cashier or the next customer.
     */
    fun checkout() {
        // Claim the checkout before doing anything else: a second tap must not be able to start a
        // second sale for the same cart, whichever dispatcher this runs on.
        val current = _state.getAndUpdate { it.copy(isCheckingOut = true) }
        if (current.isCheckingOut) return
        if (current.cart.isEmpty) {
            _state.update { it.copy(isCheckingOut = false) }
            notify("Add something to the cart first")
            return
        }

        viewModelScope.launch {
            val order =
                current.cart.toOrder(id = newOrderId(), createdAtEpochMillis = now())

            try {
                orders.save(order)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                _state.update { it.copy(isCheckingOut = false) }
                notify("Couldn't save the order — nothing was charged.", UserMessage.Tone.Error)
                return@launch
            }

            // Land on Orders so the cashier immediately sees the sale and how its sync is going.
            _state.update {
                it.copy(
                    cart = it.cart.clear(),
                    isCheckingOut = false,
                    destination = AppDestination.ORDERS,
                )
            }
            notify("Order saved", UserMessage.Tone.Success)

            if (current.isOnline) {
                launch { sync.sync(SyncTrigger.CHECKOUT) }
            } else {
                PosLog.sync("order ${order.id.take(8)} stored offline, sync deferred")
            }
        }
    }

    /** Flipping from offline to online is the only edge that starts a sync. */
    fun setOnline(isOnline: Boolean) {
        val cameOnline = isOnline && !_state.value.isOnline
        _state.update { it.copy(isOnline = isOnline) }
        if (cameOnline) {
            viewModelScope.launch { sync.sync(SyncTrigger.CAME_ONLINE) }
        }
    }

    fun syncNow() {
        val current = _state.value
        if (!current.isOnline) {
            notify("You're offline — pending orders will sync when you reconnect.")
            return
        }

        viewModelScope.launch {
            val outcome = sync.sync(SyncTrigger.MANUAL)
            when {
                // A run was already in flight; it covers the same backlog.
                outcome.skipped -> Unit
                outcome.failedCount > 0 ->
                    notify(
                        "${outcome.failedCount} order(s) still pending — try again.",
                        UserMessage.Tone.Error,
                    )

                outcome.syncedCount > 0 ->
                    notify("Synced ${outcome.syncedCount} order(s)", UserMessage.Tone.Success)

                else -> notify("Everything is already synced")
            }
        }
    }

    private fun observeOrders() {
        viewModelScope.launch {
            orders.observeOrders().collect { stored ->
                _state.update { it.copy(orders = stored) }
            }
        }
    }

    private fun observeSyncProgress() {
        viewModelScope.launch {
            sync.isSyncing.collect { syncing ->
                _state.update { it.copy(isSyncing = syncing) }
            }
        }
    }

    private fun notify(text: String, tone: UserMessage.Tone = UserMessage.Tone.Info) {
        _messages.tryEmit(UserMessage(text, tone))
    }
}
