package com.example.pos.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.pos.domain.Product
import com.example.pos.ui.formatMoney
import com.example.pos.ui.theme.PosSpacing
import com.example.pos.ui.theme.statusColors

/** Stock at or below this many units is called out rather than stated flatly. */
private const val LOW_STOCK_THRESHOLD = 3

/**
 * One catalog product. No remote images — a tinted monogram identifies the product, which keeps
 * the card fast, offline-safe, and free of an image loading library.
 */
@Composable
fun ProductCard(
    product: Product,
    quantityInCart: Int,
    onAdd: () -> Unit,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val soldOut = !product.inStock
    val atStockLimit = quantityInCart >= product.stock

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (soldOut) {
                        MaterialTheme.colorScheme.surfaceContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(PosSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProductMonogram(name = product.name, muted = soldOut)
                TaxBadge(taxable = product.taxable)
            }

            Text(
                text = product.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(40.dp),
            )

            Text(
                text = formatMoney(product.priceCents),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            StockLabel(stock = product.stock, atStockLimit = atStockLimit && !soldOut)

            Box(modifier = Modifier.animateContentSize()) {
                when {
                    soldOut ->
                        Button(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Out of stock")
                        }

                    quantityInCart == 0 ->
                        Button(
                            onClick = onAdd,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(PosSpacing.touchTarget)
                                    .clearAndSetSemantics {
                                        contentDescription = "Add ${product.name} to the cart"
                                    },
                        ) {
                            Text("Add")
                        }

                    else ->
                        QuantityStepper(
                            quantity = quantityInCart,
                            productName = product.name,
                            canIncrease = !atStockLimit,
                            onDecrease = onDecrease,
                            onIncrease = onAdd,
                            modifier = Modifier.fillMaxWidth(),
                        )
                }
            }
        }
    }
}

@Composable
private fun ProductMonogram(name: String, muted: Boolean) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = MaterialTheme.shapes.medium,
        color =
            if (muted) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
        contentColor =
            if (muted) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name.monogram(),
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

@Composable
private fun TaxBadge(taxable: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            text = if (taxable) "Taxable" else "Tax exempt",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = PosSpacing.sm, vertical = PosSpacing.xs),
        )
    }
}

@Composable
private fun StockLabel(stock: Int, atStockLimit: Boolean) {
    val warning = MaterialTheme.statusColors.warning
    val text: String
    val color: androidx.compose.ui.graphics.Color

    when {
        stock == 0 -> {
            text = "None left"
            color = warning
        }

        atStockLimit -> {
            text = "All $stock in the cart"
            color = warning
        }

        stock <= LOW_STOCK_THRESHOLD -> {
            text = "Only $stock left"
            color = warning
        }

        else -> {
            text = "$stock in stock"
            color = MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    Text(text = text, style = MaterialTheme.typography.labelMedium, color = color)
}

/** First letters of the first two words, e.g. "Travel Flask 500ml" becomes "TF". */
private fun String.monogram(): String =
    trim()
        .split(' ')
        .filter { it.firstOrNull()?.isLetterOrDigit() == true }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString(separator = "")
        .ifEmpty { "?" }
