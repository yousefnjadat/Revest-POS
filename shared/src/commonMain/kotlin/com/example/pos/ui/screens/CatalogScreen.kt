package com.example.pos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.pos.presentation.CatalogUiState
import com.example.pos.presentation.PosUiState
import com.example.pos.ui.components.PlaceholderPanel
import com.example.pos.ui.formatMoney
import com.example.pos.ui.theme.PosSpacing

/** Placeholder catalog. Real product cards and the grid arrive in the next phase. */
@Composable
fun CatalogScreen(
    state: PosUiState,
    onAddProduct: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.md),
    ) {
        when (val catalog = state.catalog) {
            is CatalogUiState.Loading ->
                PlaceholderPanel(title = "Loading catalog") {
                    CircularProgressIndicator()
                }

            is CatalogUiState.Empty ->
                PlaceholderPanel(title = "No products") {
                    Text(
                        text = "The catalog came back empty.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = onRetry) { Text("Reload") }
                }

            is CatalogUiState.Error ->
                PlaceholderPanel(title = "Couldn't load the catalog") {
                    Text(
                        text = catalog.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Button(onClick = onRetry) { Text("Try again") }
                }

            is CatalogUiState.Content ->
                catalog.products.forEach { product ->
                    PlaceholderPanel(title = product.name) {
                        Text(
                            text =
                                "${formatMoney(product.priceCents)} · " +
                                    (if (product.taxable) "taxable" else "tax exempt") +
                                    " · ${product.stock} in stock",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = { onAddProduct(product.id) },
                            enabled = product.inStock,
                        ) {
                            Text(if (product.inStock) "Add to cart" else "Out of stock")
                        }
                    }
                }
        }
    }
}
