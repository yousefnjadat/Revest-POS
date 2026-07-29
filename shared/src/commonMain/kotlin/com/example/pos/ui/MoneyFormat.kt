package com.example.pos.ui

import kotlin.math.absoluteValue

/**
 * Cents to a display string. The catalog is priced in a single currency, so the symbol is fixed
 * rather than locale-resolved — see the currency assumption in the README.
 */
fun formatMoney(cents: Long): String {
    val sign = if (cents < 0) "-" else ""
    val amount = cents.absoluteValue
    val fraction = (amount % 100).toString().padStart(2, '0')
    return "$sign$${amount / 100}.$fraction"
}
