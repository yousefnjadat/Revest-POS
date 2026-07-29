package com.example.pos.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CartTest {
    @Test
    fun addingAProductCreatesALine() {
        val coffee = product(id = "coffee", priceCents = 450)

        val cart = Cart().add(coffee)

        assertEquals(1, cart.lines.size)
        assertEquals(1, cart.quantityOf("coffee"))
        assertEquals(450, cart.totals.subtotalCents)
    }

    @Test
    fun addingTheSameProductAgainAccumulatesOntoOneLine() {
        val coffee = product(id = "coffee", priceCents = 450)

        val cart = Cart().add(coffee).add(coffee, quantity = 2)

        assertEquals(1, cart.lines.size)
        assertEquals(3, cart.quantityOf("coffee"))
        assertEquals(3, cart.itemCount)
    }

    @Test
    fun outOfStockProductsCannotBeAdded() {
        val soldOut = product(id = "sold-out", stock = 0)

        val cart = Cart().add(soldOut)

        assertTrue(cart.isEmpty)
    }

    @Test
    fun addingMoreThanStockClampsToStock() {
        val limited = product(id = "limited", stock = 3)

        val cart = Cart().add(limited, quantity = 5)

        assertEquals(3, cart.quantityOf("limited"))
    }

    @Test
    fun addingRepeatedlyStopsAtTheStockBoundary() {
        val limited = product(id = "limited", stock = 3)

        val cart = Cart().add(limited).add(limited).add(limited).add(limited)

        assertEquals(3, cart.quantityOf("limited"))
    }

    @Test
    fun settingQuantityAboveStockClampsToStock() {
        val limited = product(id = "limited", stock = 3)
        val cart = Cart().add(limited)

        val updated = cart.setQuantity("limited", 9)

        assertEquals(3, updated.quantityOf("limited"))
    }

    @Test
    fun settingQuantityToZeroRemovesTheLine() {
        val coffee = product(id = "coffee")
        val cart = Cart().add(coffee, quantity = 2)

        val updated = cart.setQuantity("coffee", 0)

        assertTrue(updated.isEmpty)
    }

    @Test
    fun steppingDownFromOneRemovesTheLine() {
        val coffee = product(id = "coffee")
        val cart = Cart().add(coffee)

        val updated = cart.setQuantity("coffee", cart.quantityOf("coffee") - 1)

        assertTrue(updated.isEmpty)
    }

    @Test
    fun negativeQuantitiesNeverReachALine() {
        val coffee = product(id = "coffee")
        val cart = Cart().add(coffee, quantity = 2)

        val updated = cart.setQuantity("coffee", -5)

        assertTrue(updated.isEmpty)
    }

    @Test
    fun editingAnUnknownProductLeavesTheCartUnchanged() {
        val cart = Cart().add(product(id = "coffee"))

        assertEquals(cart, cart.setQuantity("missing", 3))
        assertEquals(cart, cart.remove("missing"))
        assertEquals(0, cart.quantityOf("missing"))
    }

    @Test
    fun removeAndClearDropLines() {
        val cart = Cart().add(product(id = "a")).add(product(id = "b"))

        assertEquals(1, cart.remove("a").lines.size)
        assertTrue(cart.clear().isEmpty)
    }

    @Test
    fun editsDoNotMutateTheOriginalCart() {
        val coffee = product(id = "coffee")
        val original = Cart().add(coffee)

        original.add(coffee).setQuantity("coffee", 5)

        assertEquals(1, original.quantityOf("coffee"))
    }

    @Test
    fun aCartLineCannotBeConstructedOutsideItsStockBounds() {
        val limited = product(id = "limited", stock = 3)

        assertFailsWith<IllegalArgumentException> { CartLine(limited, quantity = 4) }
        assertFailsWith<IllegalArgumentException> { CartLine(limited, quantity = 0) }
    }
}
