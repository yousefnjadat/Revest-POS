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
import com.example.pos.presentation.CatalogUiState
import com.example.pos.presentation.PosUiState
import com.example.pos.ui.theme.PosTheme

private val PreviewProducts =
    listOf(
        Product("sku-1001", "Espresso Beans 250g", priceCents = 1_250, stock = 24, taxable = false),
        Product("sku-1003", "Blueberry Muffin", priceCents = 295, stock = 6, taxable = false),
        Product("sku-2001", "Ceramic Mug", priceCents = 1_750, stock = 12, taxable = true),
        Product("sku-2002", "Travel Flask 500ml", priceCents = 3_250, stock = 2, taxable = true),
        Product("sku-3002", "Barista Apron", priceCents = 2_600, stock = 0, taxable = true),
    )

@Composable
private fun PreviewCatalog(state: PosUiState) {
    PosTheme {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            CatalogScreen(
                state = state,
                onAddProduct = {},
                onDecreaseProduct = {},
                onRetry = {},
            )
        }
    }
}

@Preview(name = "Catalog - loading", showBackground = true)
@Composable
private fun CatalogLoadingPreview() {
    PreviewCatalog(PosUiState(catalog = CatalogUiState.Loading))
}

@Preview(name = "Catalog - error", showBackground = true)
@Composable
private fun CatalogErrorPreview() {
    PreviewCatalog(
        PosUiState(
            catalog =
                CatalogUiState.Error("Couldn't load the catalog. Check the connection and try again."),
        ),
    )
}

@Preview(name = "Catalog - empty", showBackground = true)
@Composable
private fun CatalogEmptyPreview() {
    PreviewCatalog(PosUiState(catalog = CatalogUiState.Empty))
}

@Preview(name = "Catalog - populated", showBackground = true)
@Composable
private fun CatalogPopulatedPreview() {
    PreviewCatalog(PosUiState(catalog = CatalogUiState.Content(PreviewProducts)))
}

@Preview(name = "Catalog - product in cart", showBackground = true)
@Composable
private fun CatalogWithCartPreview() {
    val mug = PreviewProducts.first { it.id == "sku-2001" }
    PreviewCatalog(
        PosUiState(
            catalog = CatalogUiState.Content(PreviewProducts),
            cart = Cart().add(mug, quantity = 2),
        ),
    )
}

@Preview(name = "Catalog - stock limit reached", showBackground = true)
@Composable
private fun CatalogAtStockLimitPreview() {
    val flask = PreviewProducts.first { it.id == "sku-2002" }
    PreviewCatalog(
        PosUiState(
            catalog = CatalogUiState.Content(PreviewProducts),
            cart = Cart().add(flask, quantity = flask.stock),
        ),
    )
}

@Preview(name = "Catalog - narrow, list fallback", showBackground = true, widthDp = 320)
@Composable
private fun CatalogNarrowPreview() {
    PreviewCatalog(PosUiState(catalog = CatalogUiState.Content(PreviewProducts)))
}

@Preview(name = "Catalog - large font scale", showBackground = true, fontScale = 1.5f)
@Composable
private fun CatalogLargeFontPreview() {
    val mug = PreviewProducts.first { it.id == "sku-2001" }
    PreviewCatalog(
        PosUiState(
            catalog = CatalogUiState.Content(PreviewProducts),
            cart = Cart().add(mug, quantity = 2),
        ),
    )
}
