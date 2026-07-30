package com.example.pos.domain.model

enum class OrderSyncState {
    PENDING,

    SYNCING,

    SYNCED,

    FAILED,
    ;

    val needsSync: Boolean get() = this == PENDING || this == FAILED
}

data class OrderLine(
    val productId: String,
    val name: String,
    val unitPriceCents: Long,
    val quantity: Int,
    val taxable: Boolean,
) {
    val lineTotalCents: Long get() = unitPriceCents * quantity
}

data class Order(
    val id: String,
    val createdAtEpochMillis: Long,
    val lines: List<OrderLine>,
    val totals: CartTotals,
    val syncState: OrderSyncState,
    val attemptCount: Int = 0,
    val lastError: String? = null,
    val syncedAtEpochMillis: Long? = null,
) {
    val itemCount: Int get() = lines.sumOf { it.quantity }
}


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
