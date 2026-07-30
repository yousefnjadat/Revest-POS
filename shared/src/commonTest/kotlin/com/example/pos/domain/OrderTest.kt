package com.example.pos.domain

import com.example.pos.domain.model.toOrder
import com.example.pos.domain.model.OrderSyncState
import com.example.pos.domain.model.Cart
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OrderTest {
    @Test
    fun checkoutFreezesLinesPricesAndTotals() {
        val cart =
            Cart()
                .add(product(id = "taxed", priceCents = 2_000, taxable = true), quantity = 2)
                .add(product(id = "exempt", priceCents = 1_500, taxable = false), quantity = 1)

        val order = cart.toOrder(id = "order-1", createdAtEpochMillis = 1_700_000_000_000)

        assertEquals("order-1", order.id)
        assertEquals(1_700_000_000_000, order.createdAtEpochMillis)
        assertEquals(2, order.lines.size)
        assertEquals(3, order.itemCount)
        assertEquals(cart.totals, order.totals)

        val taxedLine = order.lines.first { it.productId == "taxed" }
        assertEquals(2_000, taxedLine.unitPriceCents)
        assertEquals(4_000, taxedLine.lineTotalCents)
        assertTrue(taxedLine.taxable)
    }

    @Test
    fun aNewOrderStartsPendingWithNoAttempts() {
        val order = Cart().add(product()).toOrder(id = "order-2", createdAtEpochMillis = 0)

        assertEquals(OrderSyncState.PENDING, order.syncState)
        assertEquals(0, order.attemptCount)
        assertNull(order.lastError)
    }

    @Test
    fun onlyPendingAndFailedOrdersNeedSyncing() {
        assertTrue(OrderSyncState.PENDING.needsSync)
        assertTrue(OrderSyncState.FAILED.needsSync)
        assertFalse(OrderSyncState.SYNCING.needsSync)
        assertFalse(OrderSyncState.SYNCED.needsSync)
    }
}
