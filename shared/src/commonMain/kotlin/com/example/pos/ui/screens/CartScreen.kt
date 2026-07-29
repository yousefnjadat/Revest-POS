package com.example.pos.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.pos.domain.CartLine
import com.example.pos.presentation.PosUiState
import com.example.pos.ui.components.CartTotalsPanel
import com.example.pos.ui.components.QuantityStepper
import com.example.pos.ui.formatMoney
import com.example.pos.ui.theme.PosSpacing
import com.example.pos.ui.theme.statusColors

@Composable
fun CartScreen(
    state: PosUiState,
    onIncrease: (String) -> Unit,
    onDecrease: (String) -> Unit,
    onRemove: (String) -> Unit,
    onCheckout: () -> Unit,
    onBrowseCatalog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (state.cartLines.isEmpty()) {
            EmptyCart(onBrowseCatalog = onBrowseCatalog, modifier = Modifier.weight(1f))
            return@Column
        }

        CartHeader(
            itemCount = state.cartItemCount,
            modifier = Modifier.padding(horizontal = PosSpacing.screenHorizontal),
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding =
                PaddingValues(
                    start = PosSpacing.screenHorizontal,
                    end = PosSpacing.screenHorizontal,
                    bottom = PosSpacing.md,
                ),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            items(items = state.cartLines, key = { it.product.id }) { line ->
                CartLineCard(
                    line = line,
                    onIncrease = { onIncrease(line.product.id) },
                    onDecrease = { onDecrease(line.product.id) },
                    onRemove = { onRemove(line.product.id) },
                )
            }
        }

        CheckoutSection(
            state = state,
            onCheckout = onCheckout,
        )
    }
}

@Composable
private fun CartHeader(itemCount: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = PosSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "Cart", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = if (itemCount == 1) "1 item" else "$itemCount items",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CartLineCard(
    line: CartLine,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
) {
    val atStockLimit = line.quantity >= line.product.stock

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = line.product.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text =
                            "${formatMoney(line.product.priceCents)} each · " +
                                if (line.product.taxable) "taxable" else "tax exempt",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = formatMoney(line.lineTotalCents),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuantityStepper(
                    quantity = line.quantity,
                    productName = line.product.name,
                    canIncrease = !atStockLimit,
                    onDecrease = onDecrease,
                    onIncrease = onIncrease,
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (atStockLimit) {
                        Text(
                            text = "All ${line.product.stock} in stock",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.statusColors.warning,
                        )
                    }
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(PosSpacing.touchTarget),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Remove ${line.product.name} from the cart",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** Totals, the offline notice, and the checkout action, docked together above the nav bar. */
@Composable
private fun CheckoutSection(
    state: PosUiState,
    onCheckout: () -> Unit,
) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier =
                    Modifier.padding(
                        horizontal = PosSpacing.screenHorizontal,
                        vertical = PosSpacing.md,
                    ),
                verticalArrangement = Arrangement.spacedBy(PosSpacing.md),
            ) {
                CartTotalsPanel(totals = state.totals)

                if (!state.isOnline) {
                    OfflineNotice()
                }

                Button(
                    onClick = onCheckout,
                    enabled = !state.isCheckingOut && state.cartLines.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = PosSpacing.touchTarget),
                ) {
                    if (state.isCheckingOut) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(
                            text = "Saving order",
                            modifier = Modifier.padding(start = PosSpacing.sm),
                        )
                    } else {
                        Text(if (state.isOnline) "Checkout & Sync" else "Save Offline")
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineNotice() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.statusColors.warningContainer,
        contentColor = MaterialTheme.statusColors.onWarningContainer,
    ) {
        Row(
            modifier = Modifier.padding(PosSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Offline — this sale is saved on the device and synced once you're back online.",
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun EmptyCart(onBrowseCatalog: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().padding(PosSpacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            Text(
                text = "Your cart is empty",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = PosSpacing.sm),
            )
            Text(
                text = "Pick products from the catalog to start a sale.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onBrowseCatalog,
                modifier = Modifier.height(PosSpacing.touchTarget).padding(top = PosSpacing.xs),
            ) {
                Text("Browse catalog")
            }
        }
    }
}
