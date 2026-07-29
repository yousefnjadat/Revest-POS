package com.example.pos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.pos.domain.OrderSyncState
import com.example.pos.presentation.PosUiState
import com.example.pos.ui.components.PlaceholderPanel
import com.example.pos.ui.formatMoney
import com.example.pos.ui.theme.PosSpacing

/** Placeholder order history. Status chips and per-order detail arrive in the next phase. */
@Composable
fun OrdersScreen(
    state: PosUiState,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = PosSpacing.screenHorizontal,
                    vertical = PosSpacing.md,
                ),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.md),
    ) {
        PlaceholderPanel(title = "Sync") {
            Text(
                text =
                    when {
                        state.isSyncing -> "Syncing..."
                        state.pendingOrderCount > 0 ->
                            "${state.pendingOrderCount} order(s) waiting to sync"

                        state.orders.isEmpty() -> "No orders yet"
                        else -> "All orders are synced"
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onSyncNow, enabled = !state.isSyncing) { Text("Sync now") }
        }

        if (state.orders.isEmpty()) {
            PlaceholderPanel(title = "No sales yet") {
                Text(
                    text = "Completed sales are stored on this device and appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Column
        }

        state.orders.forEach { order ->
            PlaceholderPanel(title = "Order ${order.id.take(8)}") {
                Text(
                    text = "${order.itemCount} item(s) · ${formatMoney(order.totals.totalCents)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text =
                        when (order.syncState) {
                            OrderSyncState.PENDING -> "Pending sync"
                            OrderSyncState.SYNCING -> "Syncing"
                            OrderSyncState.SYNCED -> "Synced"
                            OrderSyncState.FAILED -> "Failed: ${order.lastError.orEmpty()}"
                        },
                    style = MaterialTheme.typography.labelLarge,
                    color =
                        if (order.syncState == OrderSyncState.FAILED) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
                if (order.attemptCount > 0) {
                    Text(
                        text = "${order.attemptCount} attempt(s)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
