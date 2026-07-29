package com.example.pos.domain

/**
 * One product and the quantity the cashier has rung up.
 *
 * A line always holds at least one unit — dropping to zero removes the line from the [Cart] —
 * and never more units than the product has in stock.
 */
data class CartLine(
    val product: Product,
    val quantity: Int,
) {
    init {
        require(quantity >= 1) { "quantity must be at least 1, was $quantity" }
        require(quantity <= product.stock) {
            "quantity $quantity exceeds stock ${product.stock} for ${product.id}"
        }
    }

    val lineTotalCents: Long get() = product.priceCents * quantity
}
