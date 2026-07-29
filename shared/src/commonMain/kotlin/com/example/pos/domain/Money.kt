package com.example.pos.domain

internal fun percentOfCents(amountCents: Long, percent: Int): Long {
    require(amountCents >= 0) { "amountCents must be non-negative, was $amountCents" }
    require(percent >= 0) { "percent must be non-negative, was $percent" }
    return (amountCents * percent + 50) / 100
}
