package com.example.pos.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.pos.domain.Product
import com.example.pos.ui.theme.PosSpacing
import com.example.pos.ui.theme.statusColors

private const val LOW_STOCK_THRESHOLD = 3

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

    PosCard(modifier = modifier, muted = soldOut) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProductMonogram(name = product.name, muted = soldOut)
            LabelChip(text = if (product.taxable) "Taxable" else "Tax exempt")
        }

        Text(
            text = product.name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.heightIn(min = 40.dp),
        )

        MoneyText(
            cents = product.priceCents,
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = PosSpacing.touchTarget),
                    ) {
                        Text("Out of stock")
                    }

                quantityInCart == 0 ->
                    Button(
                        onClick = onAdd,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = PosSpacing.touchTarget),
                    ) {
                        Text(
                            text = "Add",
                            modifier =
                                Modifier.semantics {
                                    contentDescription = "Add ${product.name} to the cart"
                                },
                        )
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
            Text(text = name.monogram(), style = MaterialTheme.typography.titleSmall)
        }
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

    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun String.monogram(): String =
    trim()
        .split(' ')
        .filter { it.firstOrNull()?.isLetterOrDigit() == true }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString(separator = "")
        .ifEmpty { "?" }
