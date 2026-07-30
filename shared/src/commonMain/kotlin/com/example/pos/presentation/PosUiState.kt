package com.example.pos.presentation

import com.example.pos.core.enums.AppDestination
import com.example.pos.domain.model.Cart
import com.example.pos.domain.model.CartLine
import com.example.pos.domain.model.CartTotals
import com.example.pos.domain.model.Order
import com.example.pos.domain.model.Product

sealed interface CatalogUiState {
    data object Loading : CatalogUiState

    data object Empty : CatalogUiState

    data class Error(val message: String) : CatalogUiState

    data class Content(val products: List<Product>) : CatalogUiState
}

data class UserMessage(
    val text: String,
    val tone: Tone = Tone.Info,
) {
    enum class Tone { Info, Success, Error }
}

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

    val pendingOrderCount: Int get() = orders.count { it.syncState.needsSync }

    fun productOrNull(productId: String): Product? = products.firstOrNull { it.id == productId }
}
