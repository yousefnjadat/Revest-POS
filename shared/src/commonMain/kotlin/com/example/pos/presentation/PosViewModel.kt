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

class PosViewModel internal constructor(
    private val catalog: CatalogRepository,
    private val orders: OrderRepository,
    private val sync: OrderSyncCoordinator,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val newOrderId: () -> String = { Uuid.random().toString() },
) : ViewModel() {
    private val _state = MutableStateFlow(PosUiState())
    val state: StateFlow<PosUiState> = _state.asStateFlow()

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

    fun checkout() {
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
