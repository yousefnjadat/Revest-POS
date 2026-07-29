package com.example.pos.presentation

/**
 * Where the app currently is. A sealed type rather than a navigation library — there are four
 * places to be, and only the receipt needs an argument.
 */
sealed interface AppDestination {
    data object Catalog : AppDestination

    data object Cart : AppDestination

    data object Orders : AppDestination

    data class Receipt(val orderId: String) : AppDestination
}
