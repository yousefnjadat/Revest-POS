package com.example.pos.domain

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
