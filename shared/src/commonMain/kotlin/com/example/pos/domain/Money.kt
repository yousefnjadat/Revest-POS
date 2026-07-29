package com.example.pos.domain

/**
 * Money is always a [Long] count of minor units (cents). Doubles are never used for prices,
 * because binary floating point cannot represent decimal cent values exactly.
 *
 * Rounding rule for every percentage in this app (tax and discount alike):
 * **half up to the nearest cent** — exactly `.5` of a cent rounds away from zero, anything
 * below stays down. It is implemented with integer arithmetic only, so it is exact and
 * deterministic: `(amount * percent + 50) / 100` adds half a cent's worth of numerator before
 * the truncating integer division.
 *
 * Example: 10% of 1005 cents is 100.5 cents, which becomes 101 cents.
 *
 * Both operands are required to be non-negative — a cart never holds negative money, and the
 * `+ 50` bias would round the wrong way for negative amounts.
 */
internal fun percentOfCents(amountCents: Long, percent: Int): Long {
    require(amountCents >= 0) { "amountCents must be non-negative, was $amountCents" }
    require(percent >= 0) { "percent must be non-negative, was $percent" }
    return (amountCents * percent + 50) / 100
}
