package com.example.pos.domain

/** Where a checked-out order stands with the remote backend. */
enum class OrderSyncState {
    /** Stored locally, waiting for a sync attempt. */
    PENDING,

    /** A sync attempt is in flight. */
    SYNCING,

    /** Accepted by the backend. */
    SYNCED,

    /** A sync attempt failed; it will be retried on the next sync. */
    FAILED,
    ;

    val needsSync: Boolean get() = this == PENDING || this == FAILED
}

/** A single item as it was sold — priced at checkout time, never re-read from the catalog. */
data class OrderLine(
    val productId: String,
    val name: String,
    val unitPriceCents: Long,
    val quantity: Int,
    val taxable: Boolean,
) {
    val lineTotalCents: Long get() = unitPriceCents * quantity
}

/**
 * A completed checkout. [id] is a UUID generated at checkout and doubles as the idempotency key
 * for syncing, so retrying a failed order can never create a duplicate on the backend.
 */
data class Order(
    val id: String,
    val createdAtEpochMillis: Long,
    val lines: List<OrderLine>,
    val totals: CartTotals,
    val syncState: OrderSyncState,
    val attemptCount: Int = 0,
    val lastError: String? = null,
) {
    val itemCount: Int get() = lines.sumOf { it.quantity }
}

/**
 * Freezes a cart into an order. Totals are captured once here rather than recalculated later,
 * so a receipt always shows what the customer actually paid.
 */
fun Cart.toOrder(
    id: String,
    createdAtEpochMillis: Long,
    syncState: OrderSyncState = OrderSyncState.PENDING,
): Order =
    Order(
        id = id,
        createdAtEpochMillis = createdAtEpochMillis,
        lines =
            lines.map { line ->
                OrderLine(
                    productId = line.product.id,
                    name = line.product.name,
                    unitPriceCents = line.product.priceCents,
                    quantity = line.quantity,
                    taxable = line.product.taxable,
                )
            },
        totals = totals,
        syncState = syncState,
    )
