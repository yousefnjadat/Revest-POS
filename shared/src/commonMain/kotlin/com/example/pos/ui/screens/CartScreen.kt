package com.example.pos.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.pos.domain.CartLine
import com.example.pos.presentation.PosUiState
import com.example.pos.ui.components.BannerTone
import com.example.pos.ui.components.CartTotalsPanel
import com.example.pos.ui.components.EmptyState
import com.example.pos.ui.components.MoneyText
import com.example.pos.ui.components.PosBanner
import com.example.pos.ui.components.PosCard
import com.example.pos.ui.components.QuantityStepper
import com.example.pos.ui.components.ScreenHeader
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
            EmptyState(
                icon = Icons.Outlined.ShoppingCart,
                title = "Your cart is empty",
                message = "Pick products from the catalog to start a sale.",
                actionLabel = "Browse catalog",
                onAction = onBrowseCatalog,
                modifier = Modifier.weight(1f),
            )
            return@Column
        }

        ScreenHeader(
            title = "Cart",
            trailing = if (state.cartItemCount == 1) "1 item" else "${state.cartItemCount} items",
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
                    modifier = Modifier.animateItem(),
                    onIncrease = { onIncrease(line.product.id) },
                    onDecrease = { onDecrease(line.product.id) },
                    onRemove = { onRemove(line.product.id) },
                )
            }
        }

        CheckoutSection(state = state, onCheckout = onCheckout)
    }
}

@Composable
private fun CartLineCard(
    line: CartLine,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val atStockLimit = line.quantity >= line.product.stock

    PosCard(modifier = modifier) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MoneyText(
                        cents = line.product.priceCents,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text =
                            " each · " +
                                if (line.product.taxable) "taxable" else "tax exempt",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            MoneyText(
                cents = line.lineTotalCents,
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
            IconButton(onClick = onRemove, modifier = Modifier.size(PosSpacing.touchTarget)) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Remove ${line.product.name} from the cart",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        AnimatedVisibility(
            visible = atStockLimit,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Text(
                text = "That's all ${line.product.stock} in stock",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.statusColors.warning,
            )
        }
    }
}

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
                    Modifier
                        .padding(
                            horizontal = PosSpacing.screenHorizontal,
                            vertical = PosSpacing.md,
                        )
                        .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(PosSpacing.md),
            ) {
                CartTotalsPanel(totals = state.totals)

                AnimatedVisibility(
                    visible = !state.isOnline,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    PosBanner(tone = BannerTone.Warning, icon = Icons.Outlined.CloudOff) {
                        Text("Offline — this sale is saved on the device and synced once you're back online.")
                    }
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
