package com.example.pos.presentation

import com.example.pos.domain.Cart
import com.example.pos.domain.CartLine
import com.example.pos.domain.CartTotals
import com.example.pos.domain.Order
import com.example.pos.domain.Product

/** What the catalog area is showing right now. */
sealed interface CatalogUiState {
    data object Loading : CatalogUiState

    data object Empty : CatalogUiState

    data class Error(val message: String) : CatalogUiState

    data class Content(val products: List<Product>) : CatalogUiState
}

/** A one-shot message for a snackbar. Delivered over a [kotlinx.coroutines.flow.SharedFlow]. */
data class UserMessage(
    val text: String,
    val tone: Tone = Tone.Info,
) {
    enum class Tone { Info, Success, Error }
}

/**
 * Everything the UI renders, in one immutable snapshot.
 *
 * Derived values are computed properties rather than stored fields, so they can never drift out
 * of step with the cart or the order list they come from. Totals in particular always come from
 * [com.example.pos.domain.CartCalculator].
 */
data class PosUiState(
    val destination: AppDestination = AppDestination.CATALOG,
    val catalog: CatalogUiState = CatalogUiState.Loading,
    val cart: Cart = Cart(),
    val isOnline: Boolean = true,
    val isCheckingOut: Boolean = false,
    val isSyncing: Boolean = false,
    val orders: List<Order> = emptyList(),
) {
    val cartLines: List<CartLine> get() = cart.lines

    val cartItemCount: Int get() = cart.itemCount

    val totals: CartTotals get() = cart.totals

    val products: List<Product>
        get() = (catalog as? CatalogUiState.Content)?.products.orEmpty()

    /** Orders still waiting to reach the backend, for the sync badge. */
    val pendingOrderCount: Int get() = orders.count { it.syncState.needsSync }

    fun productOrNull(productId: String): Product? = products.firstOrNull { it.id == productId }
}
