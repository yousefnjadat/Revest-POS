package com.example.pos.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.pos.domain.Order
import com.example.pos.domain.OrderSyncState
import com.example.pos.presentation.PosUiState
import com.example.pos.ui.components.SyncStatusChip
import com.example.pos.ui.formatMoney
import com.example.pos.ui.formatOrderTime
import com.example.pos.ui.orderReference
import com.example.pos.ui.theme.PosSpacing
import com.example.pos.ui.theme.statusColors

@Composable
fun OrdersScreen(
    state: PosUiState,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        SyncSummaryCard(
            state = state,
            onSyncNow = onSyncNow,
            modifier =
                Modifier.padding(
                    start = PosSpacing.screenHorizontal,
                    end = PosSpacing.screenHorizontal,
                    top = PosSpacing.md,
                    bottom = PosSpacing.sm,
                ),
        )

        if (state.orders.isEmpty()) {
            NoOrders(modifier = Modifier.weight(1f))
            return@Column
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding =
                PaddingValues(
                    start = PosSpacing.screenHorizontal,
                    end = PosSpacing.screenHorizontal,
                    top = PosSpacing.sm,
                    bottom = PosSpacing.md,
                ),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            items(items = state.orders, key = { it.id }) { order ->
                OrderCard(order = order, modifier = Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun SyncSummaryCard(
    state: PosUiState,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pending = state.pendingOrderCount
    val synced = state.orders.count { it.syncState == OrderSyncState.SYNCED }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(PosSpacing.md).animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PosSpacing.md),
            ) {
                SummaryStat(label = "Pending", value = pending.toString())
                VerticalRule()
                SummaryStat(label = "Synced", value = synced.toString())
                VerticalRule()
                SummaryStat(
                    label = "Connection",
                    value = if (state.isOnline) "Online" else "Offline",
                    icon =
                        if (state.isOnline) Icons.Outlined.CloudDone else Icons.Outlined.CloudOff,
                    valueColor =
                        if (state.isOnline) {
                            MaterialTheme.statusColors.success
                        } else {
                            MaterialTheme.statusColors.warning
                        },
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Button(
                onClick = onSyncNow,
                enabled = state.isOnline && pending > 0 && !state.isSyncing,
                modifier = Modifier.fillMaxWidth().heightIn(min = PosSpacing.touchTarget),
            ) {
                if (state.isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Text(text = "Syncing", modifier = Modifier.padding(start = PosSpacing.sm))
                } else {
                    Icon(
                        imageVector = Icons.Filled.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(text = "Sync now", modifier = Modifier.padding(start = PosSpacing.sm))
                }
            }

            SyncHint(isOnline = state.isOnline, isSyncing = state.isSyncing, pending = pending)
        }
    }
}

/** Says why the button is in the state it is in, so a disabled button is never a dead end. */
@Composable
private fun SyncHint(isOnline: Boolean, isSyncing: Boolean, pending: Int) {
    val message =
        when {
            isSyncing -> "Sending orders to the backend..."
            !isOnline && pending > 0 ->
                "Offline — $pending order(s) are saved here and will sync automatically once " +
                    "you switch back online."

            !isOnline -> "Offline — nothing is waiting to sync."
            pending > 0 -> "$pending order(s) ready to send."
            else -> "Everything is synced."
        }

    Text(
        text = message,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SummaryStat(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PosSpacing.xs)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = valueColor,
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = valueColor,
            )
        }
    }
}

@Composable
private fun VerticalRule() {
    Surface(
        modifier = Modifier.width(1.dp).height(36.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
        content = {},
    )
}

@Composable
private fun OrderCard(order: Order, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(PosSpacing.md).animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = orderReference(order.id),
                    style = MaterialTheme.typography.titleSmall,
                )
                SyncStatusChip(order = order)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text =
                        "${formatOrderTime(order.createdAtEpochMillis)} · " +
                            if (order.itemCount == 1) "1 item" else "${order.itemCount} items",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatMoney(order.totals.totalCents),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            AnimatedVisibility(
                visible = order.syncState == OrderSyncState.FAILED && order.lastError != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                RetryNotice(error = order.lastError.orEmpty(), attempts = order.attemptCount)
            }

            if (order.syncState == OrderSyncState.SYNCED) {
                Text(
                    text =
                        buildString {
                            append("Synced")
                            order.syncedAtEpochMillis?.let { append(" ${formatOrderTime(it)}") }
                            if (order.attemptCount > 1) {
                                append(" after ${order.attemptCount} attempts")
                            }
                        },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Shown only for an order whose last attempt failed: what happened, and that nothing was lost. */
@Composable
private fun RetryNotice(error: String, attempts: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.statusColors.warningContainer,
        contentColor = MaterialTheme.statusColors.onWarningContainer,
    ) {
        Column(
            modifier = Modifier.padding(PosSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
        ) {
            Text(
                text = "Last attempt failed: $error",
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text =
                    "The sale is saved on this device. Sync now to retry — it will not be " +
                        "charged twice.",
                style = MaterialTheme.typography.labelMedium,
            )
            if (attempts > 0) {
                Text(
                    text = if (attempts == 1) "1 attempt" else "$attempts attempts",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun NoOrders(modifier: Modifier = Modifier) {
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
                        imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            Text(
                text = "No sales yet",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = PosSpacing.sm),
            )
            Text(
                text = "Completed sales are stored on this device and listed here with their sync state.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
