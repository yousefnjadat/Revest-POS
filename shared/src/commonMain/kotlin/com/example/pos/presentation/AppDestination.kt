package com.example.pos.presentation

/**
 * Where the app currently is. An enum in state rather than a navigation library — there are
 * three places to be and none of them takes an argument.
 */
enum class AppDestination(val label: String) {
    CATALOG("Catalog"),
    CART("Cart"),
    ORDERS("Orders"),
}
