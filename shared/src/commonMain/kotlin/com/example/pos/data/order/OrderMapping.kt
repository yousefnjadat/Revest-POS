package com.example.pos.data.order

import com.example.pos.domain.CartTotals
import com.example.pos.domain.Order
import com.example.pos.domain.OrderLine
import com.example.pos.domain.OrderSyncState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The stored shape of an order's line items. Kept separate from the domain model so a future
 * domain change cannot silently reinterpret rows already written to disk.
 */
@Serializable
internal data class OrderPayload(
    val lines: List<OrderLinePayload>,
)

@Serializable
internal data class OrderLinePayload(
    val productId: String,
    val name: String,
    val unitPriceCents: Long,
    val quantity: Int,
    val taxable: Boolean,
)

internal val OrderJson: Json = Json { ignoreUnknownKeys = true }

internal fun encodeLines(lines: List<OrderLine>): String {
    val payload =
        OrderPayload(
            lines.map {
                OrderLinePayload(
                    productId = it.productId,
                    name = it.name,
                    unitPriceCents = it.unitPriceCents,
                    quantity = it.quantity,
                    taxable = it.taxable,
                )
            },
        )
    return OrderJson.encodeToString(OrderPayload.serializer(), payload)
}

internal fun decodeLines(payload: String): List<OrderLine> =
    OrderJson.decodeFromString(OrderPayload.serializer(), payload).lines.map {
        OrderLine(
            productId = it.productId,
            name = it.name,
            unitPriceCents = it.unitPriceCents,
            quantity = it.quantity,
            taxable = it.taxable,
        )
    }

/**
 * Rebuilds an [Order] from one `pending_orders` row.
 *
 * The sync state is derived rather than stored — `synced_at` and `last_error` already say
 * everything a persisted row can say. [OrderSyncState.SYNCING] is deliberately not representable
 * here: an in-flight attempt is runtime state, not something that should survive a process death.
 *
 * The taxable subtotal is recomputed from the stored lines instead of occupying its own column,
 * which keeps the schema to the columns the app actually queries.
 */
@Suppress("LongParameterList")
internal fun orderFromRow(
    id: String,
    payload: String,
    subtotalCents: Long,
    taxCents: Long,
    discountCents: Long,
    totalCents: Long,
    createdAt: Long,
    syncedAt: Long?,
    syncAttempts: Long,
    lastError: String?,
): Order {
    val lines = decodeLines(payload)
    return Order(
        id = id,
        createdAtEpochMillis = createdAt,
        lines = lines,
        totals =
            CartTotals(
                subtotalCents = subtotalCents,
                taxableSubtotalCents = lines.filter { it.taxable }.sumOf { it.lineTotalCents },
                taxCents = taxCents,
                discountCents = discountCents,
                totalCents = totalCents,
            ),
        syncState =
            when {
                syncedAt != null -> OrderSyncState.SYNCED
                lastError != null -> OrderSyncState.FAILED
                else -> OrderSyncState.PENDING
            },
        attemptCount = syncAttempts.toInt(),
        lastError = lastError,
        syncedAtEpochMillis = syncedAt,
    )
}
