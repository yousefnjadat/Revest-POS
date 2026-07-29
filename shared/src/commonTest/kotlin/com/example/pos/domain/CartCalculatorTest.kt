package com.example.pos.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CartCalculatorTest {
    @Test
    fun emptyCartHasZeroTotals() {
        val totals = CartCalculator.totals(emptyList())

        assertEquals(CartTotals.EMPTY, totals)
        assertFalse(totals.discountApplied)
    }

    @Test
    fun nonTaxableProductIsNotTaxed() {
        val lines = listOf(CartLine(product(priceCents = 1_200, taxable = false), quantity = 1))

        val totals = CartCalculator.totals(lines)

        assertEquals(1_200, totals.subtotalCents)
        assertEquals(0, totals.taxableSubtotalCents)
        assertEquals(0, totals.taxCents)
        assertEquals(1_200, totals.totalCents)
    }

    @Test
    fun taxableProductIsTaxedAtTenPercent() {
        val lines = listOf(CartLine(product(priceCents = 1_200, taxable = true), quantity = 1))

        val totals = CartCalculator.totals(lines)

        assertEquals(1_200, totals.subtotalCents)
        assertEquals(1_200, totals.taxableSubtotalCents)
        assertEquals(120, totals.taxCents)
        assertEquals(1_320, totals.totalCents)
    }

    @Test
    fun mixedCartTaxesOnlyTheTaxableLines() {
        val lines =
            listOf(
                CartLine(product(id = "taxed", priceCents = 1_200, taxable = true), quantity = 1),
                CartLine(product(id = "exempt", priceCents = 2_000, taxable = false), quantity = 1),
            )

        val totals = CartCalculator.totals(lines)

        assertEquals(3_200, totals.subtotalCents)
        assertEquals(1_200, totals.taxableSubtotalCents)
        assertEquals(120, totals.taxCents)
        assertEquals(0, totals.discountCents)
        assertEquals(3_320, totals.totalCents)
    }

    @Test
    fun subtotalJustBelowFiftyGetsNoDiscount() {
        val lines = listOf(CartLine(product(priceCents = 4_999, taxable = false), quantity = 1))

        val totals = CartCalculator.totals(lines)

        assertEquals(4_999, totals.subtotalCents)
        assertEquals(0, totals.discountCents)
        assertFalse(totals.discountApplied)
        assertEquals(4_999, totals.totalCents)
    }

    @Test
    fun subtotalOfExactlyFiftyGetsTheDiscount() {
        val lines = listOf(CartLine(product(priceCents = 5_000, taxable = false), quantity = 1))

        val totals = CartCalculator.totals(lines)

        assertEquals(5_000, totals.subtotalCents)
        assertEquals(250, totals.discountCents)
        assertTrue(totals.discountApplied)
        assertEquals(4_750, totals.totalCents)
    }

    @Test
    fun subtotalOneCentAboveTheThresholdIsAlsoDiscounted() {
        val lines = listOf(CartLine(product(priceCents = 5_001, taxable = false), quantity = 1))

        val totals = CartCalculator.totals(lines)

        assertEquals(5_001, totals.subtotalCents)
        // 5% of 50.01 is 2.5005, which rounds down to 2.50.
        assertEquals(250, totals.discountCents)
        assertTrue(totals.discountApplied)
        assertEquals(4_751, totals.totalCents)
    }

    @Test
    fun theThresholdIsCrossedByTheWholeSubtotalNotTheTaxableParts() {
        // Neither line reaches 50 on its own, and the taxable part is only 20.
        val lines =
            listOf(
                CartLine(product(id = "taxed", priceCents = 2_000, taxable = true), quantity = 1),
                CartLine(product(id = "exempt", priceCents = 3_000, taxable = false), quantity = 1),
            )

        val totals = CartCalculator.totals(lines)

        assertEquals(5_000, totals.subtotalCents)
        assertEquals(2_000, totals.taxableSubtotalCents)
        assertEquals(200, totals.taxCents)
        assertEquals(250, totals.discountCents)
        assertEquals(4_950, totals.totalCents)
    }

    @Test
    fun subtotalAboveFiftyGetsTheDiscountOnTopOfTax() {
        val lines = listOf(CartLine(product(priceCents = 6_000, taxable = true), quantity = 1))

        val totals = CartCalculator.totals(lines)

        assertEquals(6_000, totals.subtotalCents)
        assertEquals(600, totals.taxCents)
        assertEquals(300, totals.discountCents)
        assertEquals(6_300, totals.totalCents)
    }

    @Test
    fun discountIsCalculatedOnTheWholeSubtotalIncludingExemptLines() {
        val lines =
            listOf(
                CartLine(product(id = "taxed", priceCents = 2_000, taxable = true), quantity = 1),
                CartLine(product(id = "exempt", priceCents = 4_000, taxable = false), quantity = 1),
            )

        val totals = CartCalculator.totals(lines)

        assertEquals(6_000, totals.subtotalCents)
        assertEquals(2_000, totals.taxableSubtotalCents)
        assertEquals(200, totals.taxCents)
        assertEquals(300, totals.discountCents)
        assertEquals(5_900, totals.totalCents)
    }

    @Test
    fun quantitiesMultiplyLineTotals() {
        val lines =
            listOf(
                CartLine(product(id = "taxed", priceCents = 250, taxable = true), quantity = 4),
                CartLine(product(id = "exempt", priceCents = 300, taxable = false), quantity = 3),
            )

        val totals = CartCalculator.totals(lines)

        assertEquals(1_900, totals.subtotalCents)
        assertEquals(1_000, totals.taxableSubtotalCents)
        assertEquals(100, totals.taxCents)
        assertEquals(2_000, totals.totalCents)
    }

    @Test
    fun taxOfHalfACentRoundsUp() {
        // 3 x 3.35 = 10.05 taxable, so 10% is 1.005 -> 1.01
        val lines = listOf(CartLine(product(priceCents = 335, taxable = true), quantity = 3))

        val totals = CartCalculator.totals(lines)

        assertEquals(1_005, totals.taxableSubtotalCents)
        assertEquals(101, totals.taxCents)
        assertEquals(1_106, totals.totalCents)
    }

    @Test
    fun taxBelowHalfACentRoundsDown() {
        // 4 x 2.51 = 10.04 taxable, so 10% is 1.004 -> 1.00
        val lines = listOf(CartLine(product(priceCents = 251, taxable = true), quantity = 4))

        val totals = CartCalculator.totals(lines)

        assertEquals(1_004, totals.taxableSubtotalCents)
        assertEquals(100, totals.taxCents)
    }

    @Test
    fun discountOfHalfACentRoundsUp() {
        // 5% of 60.50 is 3.025 -> 3.03
        val lines = listOf(CartLine(product(priceCents = 6_050, taxable = false), quantity = 1))

        val totals = CartCalculator.totals(lines)

        assertEquals(6_050, totals.subtotalCents)
        assertEquals(303, totals.discountCents)
        assertEquals(5_747, totals.totalCents)
    }

    @Test
    fun percentOfCentsRoundsHalfUp() {
        assertEquals(0, percentOfCents(0, CartCalculator.TAX_PERCENT))
        assertEquals(100, percentOfCents(1_004, CartCalculator.TAX_PERCENT))
        assertEquals(101, percentOfCents(1_005, CartCalculator.TAX_PERCENT))
        assertEquals(101, percentOfCents(1_006, CartCalculator.TAX_PERCENT))
        assertEquals(250, percentOfCents(5_000, CartCalculator.DISCOUNT_PERCENT))
        assertEquals(303, percentOfCents(6_050, CartCalculator.DISCOUNT_PERCENT))
    }

    @Test
    fun totalAlwaysEqualsSubtotalPlusTaxMinusDiscount() {
        val lines =
            listOf(
                CartLine(product(id = "a", priceCents = 1_999, taxable = true), quantity = 3),
                CartLine(product(id = "b", priceCents = 449, taxable = false), quantity = 7),
            )

        val totals = CartCalculator.totals(lines)

        assertEquals(
            totals.subtotalCents + totals.taxCents - totals.discountCents,
            totals.totalCents,
        )
    }

    @Test
    fun repeatedCalculationsAreIdentical() {
        val lines =
            listOf(
                CartLine(product(id = "a", priceCents = 1_337, taxable = true), quantity = 2),
                CartLine(product(id = "b", priceCents = 899, taxable = false), quantity = 5),
            )

        assertEquals(CartCalculator.totals(lines), CartCalculator.totals(lines))
        assertEquals(CartCalculator.totals(lines), CartCalculator.totals(lines.reversed()))
    }
}
