package com.example.pos.domain

/**
 * The single place cart money is calculated. Pure and deterministic: the same lines always
 * produce the same [CartTotals], with no clock, no I/O, and no hidden state.
 *
 * total = subtotal + tax - discount, where
 * - subtotal is every line's unit price times its quantity,
 * - tax is [TAX_PERCENT] of the taxable lines only,
 * - discount is [DISCOUNT_PERCENT] of the whole subtotal, once the subtotal reaches
 *   [DISCOUNT_THRESHOLD_CENTS].
 *
 * Tax and discount are each rounded independently against their own base, using the half-up
 * rule documented in [percentOfCents]. Tax is deliberately calculated on the pre-discount
 * taxable subtotal, matching the assignment's left-to-right formula.
 */
object CartCalculator {
    const val TAX_PERCENT: Int = 10
    const val DISCOUNT_PERCENT: Int = 5

    /** 50.00 in cents. The discount applies at exactly this subtotal, not only above it. */
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
