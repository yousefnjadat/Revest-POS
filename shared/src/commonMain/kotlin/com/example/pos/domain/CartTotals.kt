package com.example.pos.domain

data class CartTotals(
    val subtotalCents: Long,
    val taxableSubtotalCents: Long,
    val taxCents: Long,
    val discountCents: Long,
    val totalCents: Long,
) {
    val discountApplied: Boolean get() = discountCents > 0

    companion object {
        val EMPTY = CartTotals(
            subtotalCents = 0,
            taxableSubtotalCents = 0,
            taxCents = 0,
            discountCents = 0,
            totalCents = 0,
        )
    }
}
