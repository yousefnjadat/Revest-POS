package com.example.pos.ui.components

import kotlin.math.absoluteValue

fun formatMoney(cents: Long): String {
    val sign = if (cents < 0) "-" else ""
    val amount = cents.absoluteValue
    val fraction = (amount % 100).toString().padStart(2, '0')
    return "$sign$${amount / 100}.$fraction"
}
