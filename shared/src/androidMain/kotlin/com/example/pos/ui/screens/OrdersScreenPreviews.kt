package com.example.pos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.pos.domain.Cart
import com.example.pos.domain.Order
import com.example.pos.domain.OrderSyncState
import com.example.pos.domain.Product
import com.example.pos.domain.toOrder
import com.example.pos.presentation.PosUiState
import com.example.pos.ui.theme.PosTheme

private val Mug =
    Product("sku-2001", "Ceramic Mug", priceCents = 1_750, stock = 12, taxable = true)
private val Muffin =
    Product("sku-1003", "Blueberry Muffin", priceCents = 295, stock = 6, taxable = false)

private const val MORNING = 1_772_000_000_000L

private fun sampleOrder(
    id: String,
    quantity: Int = 2,
    createdAt: Long = MORNING,
    syncState: OrderSyncState = OrderSyncState.PENDING,
    attemptCount: Int = 0,
    lastError: String? = null,
    syncedAt: Long? = null,
): Order =
    Cart()
        .add(Mug, quantity)
        .add(Muffin)
        .toOrder(id = id, createdAtEpochMillis = createdAt)
        .copy(
            syncState = syncState,
            attemptCount = attemptCount,
            lastError = lastError,
            syncedAtEpochMillis = syncedAt,
        )

@Composable
private fun PreviewOrders(state: PosUiState) {
    PosTheme {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            OrdersScreen(state = state, onSyncNow = {})
        }
    }
}

@Preview(name = "Orders - empty", showBackground = true)
@Composable
private fun OrdersEmptyPreview() {
    PreviewOrders(PosUiState())
}

@Preview(name = "Orders - one pending", showBackground = true)
@Composable
private fun OrdersPendingPreview() {
    PreviewOrders(PosUiState(orders = listOf(sampleOrder("a1b2c3d4-pending"))))
}

@Preview(name = "Orders - pending after a failure", showBackground = true)
@Composable
private fun OrdersFailedPreview() {
    PreviewOrders(
        PosUiState(
            orders =
                listOf(
                    sampleOrder(
                        id = "6b1efd47-failed",
                        syncState = OrderSyncState.FAILED,
                        attemptCount = 1,
                        lastError = "Server responded 503",
                    ),
                ),
        ),
    )
}

@Preview(name = "Orders - mixed pending and synced", showBackground = true)
@Composable
private fun OrdersMixedPreview() {
    PreviewOrders(
        PosUiState(
            orders =
                listOf(
                    sampleOrder(id = "9f0e1d2c-pending", createdAt = MORNING + 900_000),
                    sampleOrder(
                        id = "6b1efd47-failed",
                        createdAt = MORNING + 600_000,
                        syncState = OrderSyncState.FAILED,
                        attemptCount = 1,
                        lastError = "Server responded 503",
                    ),
                    sampleOrder(
                        id = "03307f89-synced",
                        quantity = 1,
                        syncState = OrderSyncState.SYNCED,
                        attemptCount = 2,
                        syncedAt = MORNING + 120_000,
                    ),
                ),
        ),
    )
}

@Preview(name = "Orders - syncing", showBackground = true)
@Composable
private fun OrdersSyncingPreview() {
    PreviewOrders(
        PosUiState(
            isSyncing = true,
            orders = listOf(sampleOrder("a1b2c3d4-pending", syncState = OrderSyncState.SYNCING)),
        ),
    )
}

@Preview(name = "Orders - large font scale", showBackground = true, fontScale = 1.5f)
@Composable
private fun OrdersLargeFontPreview() {
    PreviewOrders(
        PosUiState(
            orders =
                listOf(
                    sampleOrder(
                        id = "6b1efd47-failed",
                        syncState = OrderSyncState.FAILED,
                        attemptCount = 1,
                        lastError = "Server responded 503",
                    ),
                ),
        ),
    )
}

@Preview(name = "Orders - offline", showBackground = true)
@Composable
private fun OrdersOfflinePreview() {
    PreviewOrders(
        PosUiState(
            isOnline = false,
            orders =
                listOf(
                    sampleOrder("9f0e1d2c-pending"),
                    sampleOrder("a1b2c3d4-pending", createdAt = MORNING - 300_000),
                ),
        ),
    )
}
