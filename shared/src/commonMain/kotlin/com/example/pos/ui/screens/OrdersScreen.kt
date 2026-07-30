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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.pos.domain.model.Order
import com.example.pos.domain.model.OrderSyncState
import com.example.pos.presentation.PosUiState
import com.example.pos.ui.components.BannerTone
import com.example.pos.ui.components.EmptyState
import com.example.pos.ui.components.MoneyText
import com.example.pos.ui.components.PosBanner
import com.example.pos.ui.components.PosCard
import com.example.pos.ui.components.ScreenHeader
import com.example.pos.ui.components.SyncStatusChip
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
        ScreenHeader(
            title = "Orders",
            trailing =
                when (state.orders.size) {
                    0 -> null
                    1 -> "1 sale"
                    else -> "${state.orders.size} sales"
                },
        )

        SyncSummaryCard(
            state = state,
            onSyncNow = onSyncNow,
            modifier =
                Modifier.padding(
                    start = PosSpacing.screenHorizontal,
                    end = PosSpacing.screenHorizontal,
                    bottom = PosSpacing.sm,
                ),
        )

        if (state.orders.isEmpty()) {
            EmptyState(
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                title = "No sales yet",
                message = "Completed sales are stored on this device and listed here with their sync state.",
                modifier = Modifier.weight(1f),
            )
            return@Column
        }

        val listState = rememberLazyListState()
        val newestOrderId = state.orders.firstOrNull()?.id
        LaunchedEffect(newestOrderId) {
            if (newestOrderId != null) listState.animateScrollToItem(index = 0)
        }

        LazyColumn(
            state = listState,
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

    PosCard(modifier = modifier.animateContentSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.md),
        ) {
            SummaryStat(label = "Pending", value = pending.toString())
            StatDivider()
            SummaryStat(label = "Synced", value = synced.toString())
            StatDivider()
            SummaryStat(
                label = "Connection",
                value = if (state.isOnline) "Online" else "Offline",
                icon = if (state.isOnline) Icons.Outlined.CloudDone else Icons.Outlined.CloudOff,
                valueColor =
                    if (state.isOnline) {
                        MaterialTheme.statusColors.success
                    } else {
                        MaterialTheme.statusColors.warning
                    },
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = PosSpacing.xs),
            color = MaterialTheme.colorScheme.outlineVariant,
        )

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
            } else {
                Icon(
                    imageVector = Icons.Filled.Sync,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = if (state.isSyncing) "Syncing" else "Sync now",
                modifier = Modifier.padding(start = PosSpacing.sm),
            )
        }

        Text(
            text = syncHint(state.isOnline, state.isSyncing, pending),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun syncHint(isOnline: Boolean, isSyncing: Boolean, pending: Int): String =
    when {
        isSyncing -> "Sending orders to the backend..."
        !isOnline && pending > 0 ->
            "Offline — $pending order(s) are saved here and will sync automatically once you " +
                "switch back online."

        !isOnline -> "Offline — nothing is waiting to sync."
        pending > 0 -> "$pending order(s) ready to send."
        else -> "Everything is synced."
    }

@Composable
private fun SummaryStat(
    label: String,
    value: String,
    icon: ImageVector? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
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
private fun StatDivider() {
    Surface(
        modifier = Modifier.width(1.dp).height(32.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
        content = {},
    )
}

@Composable
private fun OrderCard(order: Order, modifier: Modifier = Modifier) {
    PosCard(modifier = modifier.animateContentSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = orderReference(order.id), style = MaterialTheme.typography.titleSmall)
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
            MoneyText(
                cents = order.totals.totalCents,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        AnimatedVisibility(
            visible = order.syncState == OrderSyncState.FAILED && order.lastError != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            PosBanner(tone = BannerTone.Warning, icon = Icons.Outlined.ErrorOutline) {
                Text("Saved here — the backend replied \"${order.lastError.orEmpty()}\".")
                Text("Sync now to retry. It cannot be charged twice.")
            }
        }

        if (order.syncState == OrderSyncState.SYNCED) {
            Text(
                text =
                    buildString {
                        append("Synced")
                        order.syncedAtEpochMillis?.let { append(" ${formatOrderTime(it)}") }
                        if (order.attemptCount > 1) append(" after ${order.attemptCount} attempts")
                    },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
