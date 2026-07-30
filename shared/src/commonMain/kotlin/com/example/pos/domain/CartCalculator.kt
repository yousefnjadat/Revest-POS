package com.example.pos.domain

import com.example.pos.domain.model.CartTotals
import com.example.pos.domain.model.CartLine
object CartCalculator {
    const val TAX_PERCENT: Int = 10
    const val DISCOUNT_PERCENT: Int = 5

    const val DISCOUNT_THRESHOLD_CENTS: Long = 5_000

    fun totals(lines: List<CartLine>): CartTotals {
        if (lines.isEmpty()) return CartTotals.EMPTY

        val subtotalCents = lines.sumOf { it.lineTotalCents }
        val taxableSubtotalCents = lines.filter { it.product.taxable }.sumOf { it.lineTotalCents }

        val taxCents = percentOfCents(taxableSubtotalCents, TAX_PERCENT)
        val discountCents =
            if (subtotalCents >= DISCOUNT_THRESHOLD_CENTS) {
                percentOfCents(subtotalCents, DISCOUNT_PERCENT)
            } else {
                0
            }

        return CartTotals(
            subtotalCents = subtotalCents,
            taxableSubtotalCents = taxableSubtotalCents,
            taxCents = taxCents,
            discountCents = discountCents,
            totalCents = subtotalCents + taxCents - discountCents,
        )
    }
}
