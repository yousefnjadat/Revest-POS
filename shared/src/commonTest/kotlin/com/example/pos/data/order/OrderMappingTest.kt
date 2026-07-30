package com.example.pos.data.order

import com.example.pos.data.order.local.orderFromRow
import com.example.pos.data.order.local.encodeLines
import com.example.pos.data.order.local.decodeLines
import com.example.pos.domain.model.OrderLine
import com.example.pos.domain.model.OrderSyncState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Serialization and row mapping, exercised without a database. */
class OrderMappingTest {
    private val lines =
        listOf(
            OrderLine(
                productId = "sku-2001",
                name = "Ceramic Mug",
                unitPriceCents = 1_750,
                quantity = 2,
                taxable = true,
            ),
            OrderLine(
                productId = "sku-1003",
                name = "Blueberry Muffin",
                unitPriceCents = 295,
                quantity = 3,
                taxable = false,
            ),
        )

    @Test
    fun linesSurviveARoundTrip() {
        assertEquals(lines, decodeLines(encodeLines(lines)))
    }

    @Test
    fun anEmptyLineListRoundTrips() {
        assertEquals(emptyList(), decodeLines(encodeLines(emptyList())))
    }

    @Test
    fun theEncodedPayloadKeepsPricesAsWholeCents() {
        val payload = encodeLines(lines)

        assertTrue(payload.contains("\"unitPriceCents\":1750"), payload)
        assertTrue(payload.contains("\"unitPriceCents\":295"), payload)
        assertTrue(!payload.contains("."), "money must never be serialized as a decimal: $payload")
    }

    @Test
    fun aRowWithoutSyncMarkersMapsToAPendingOrder() {
        val order = row()

        assertEquals("order-1", order.id)
        assertEquals(OrderSyncState.PENDING, order.syncState)
        assertEquals(0, order.attemptCount)
        assertNull(order.lastError)
        assertNull(order.syncedAtEpochMillis)
        assertEquals(lines, order.lines)
        assertEquals(5, order.itemCount)
    }

    @Test
    fun aRowWithAnErrorMapsToAFailedOrder() {
        val order = row(syncAttempts = 2, lastError = "503 Service Unavailable")

        assertEquals(OrderSyncState.FAILED, order.syncState)
        assertEquals(2, order.attemptCount)
        assertEquals("503 Service Unavailable", order.lastError)
    }

    @Test
    fun aRowWithSyncedAtMapsToASyncedOrder() {
        val order = row(syncedAt = 1_700_000_100_000, syncAttempts = 1)

        assertEquals(OrderSyncState.SYNCED, order.syncState)
        assertEquals(1_700_000_100_000, order.syncedAtEpochMillis)
    }

    @Test
    fun syncedAtWinsOverALeftoverError() {
        val order = row(syncedAt = 1_700_000_100_000, lastError = "earlier failure")

        assertEquals(OrderSyncState.SYNCED, order.syncState)
    }

    @Test
    fun storedMoneyColumnsAreReadBackExactly() {
        val order = row()

        assertEquals(4_385, order.totals.subtotalCents)
        assertEquals(350, order.totals.taxCents)
        assertEquals(0, order.totals.discountCents)
        assertEquals(4_735, order.totals.totalCents)
    }

    @Test
    fun theTaxableSubtotalIsRecomputedFromTheStoredLines() {
        val order = row()

        // Only the mug is taxable: 2 x 17.50.
        assertEquals(3_500, order.totals.taxableSubtotalCents)
    }

    private fun row(
        syncedAt: Long? = null,
        syncAttempts: Long = 0,
        lastError: String? = null,
    ) = orderFromRow(
        id = "order-1",
        payload = encodeLines(lines),
        subtotalCents = 4_385,
        taxCents = 350,
        discountCents = 0,
        totalCents = 4_735,
        createdAt = 1_700_000_000_000,
        syncedAt = syncedAt,
        syncAttempts = syncAttempts,
        lastError = lastError,
    )
}
