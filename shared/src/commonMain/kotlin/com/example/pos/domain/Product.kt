package com.example.pos.domain

data class Product(
    val id: String,
    val name: String,
    val priceCents: Long,
    val stock: Int,
    val taxable: Boolean,
) {
    init {
        require(priceCents >= 0) { "priceCents must be non-negative, was $priceCents" }
        require(stock >= 0) { "stock must be non-negative, was $stock" }
    }

    val inStock: Boolean get() = stock > 0
}
