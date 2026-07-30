package com.example.pos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.pos.domain.model.Cart
import com.example.pos.domain.model.Product
import com.example.pos.presentation.PosUiState
import com.example.pos.ui.theme.PosTheme

private val Mug =
    Product("sku-2001", "Ceramic Mug", priceCents = 1_750, stock = 12, taxable = true)
private val Flask =
    Product("sku-2002", "Travel Flask 500ml", priceCents = 3_250, stock = 2, taxable = true)
private val Muffin =
    Product("sku-1003", "Blueberry Muffin", priceCents = 295, stock = 6, taxable = false)

private val SmallCart = Cart().add(Mug).add(Muffin, quantity = 2)

private val DiscountedCart = Cart().add(Mug).add(Flask)

@Composable
private fun PreviewCart(state: PosUiState) {
    PosTheme {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            CartScreen(
                state = state,
                onIncrease = {},
                onDecrease = {},
                onRemove = {},
                onCheckout = {},
                onBrowseCatalog = {},
            )
        }
    }
}

@Preview(name = "Cart - empty", showBackground = true)
@Composable
private fun CartEmptyPreview() {
    PreviewCart(PosUiState())
}

@Preview(name = "Cart - below discount threshold", showBackground = true)
@Composable
private fun CartBelowThresholdPreview() {
    PreviewCart(PosUiState(cart = SmallCart))
}

@Preview(name = "Cart - discount applied", showBackground = true)
@Composable
private fun CartWithDiscountPreview() {
    PreviewCart(PosUiState(cart = DiscountedCart))
}

@Preview(name = "Cart - offline", showBackground = true)
@Composable
private fun CartOfflinePreview() {
    PreviewCart(PosUiState(cart = SmallCart, isOnline = false))
}

@Preview(name = "Cart - checkout in progress", showBackground = true)
@Composable
private fun CartCheckingOutPreview() {
    PreviewCart(PosUiState(cart = DiscountedCart, isCheckingOut = true))
}

@Preview(name = "Cart - large font scale", showBackground = true, fontScale = 1.5f)
@Composable
private fun CartLargeFontPreview() {
    PreviewCart(PosUiState(cart = DiscountedCart))
}

@Preview(name = "Cart - small phone", showBackground = true, widthDp = 320, heightDp = 640)
@Composable
private fun CartSmallScreenPreview() {
    PreviewCart(PosUiState(cart = DiscountedCart, isOnline = false))
}
