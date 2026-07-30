package com.example.pos.data.order

import com.example.pos.data.order.remote.KtorOrderSyncApi
import com.example.pos.data.order.model.toRequestDto
import com.example.pos.data.remote.IDEMPOTENCY_KEY_HEADER
import com.example.pos.data.remote.MockPosBackend
import com.example.pos.data.remote.POS_BASE_URL
import com.example.pos.data.remote.TransientFailureMode
import com.example.pos.data.testClient
import com.example.pos.domain.model.Cart
import com.example.pos.domain.model.Order
import com.example.pos.domain.product
import com.example.pos.domain.model.toOrder
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class OrderSyncApiTest {
    @Test
    fun theOrderIdIsSentInBothTheHeaderAndTheBody() = runTest {
        var sentBody: String? = null
        var sentKey: String? = null
        val client =
            testClient { request ->
                sentBody = request.body.toByteArray().decodeToString()
                sentKey = request.headers[IDEMPOTENCY_KEY_HEADER]
                respond(
                    """{"order_id":"order-1","duplicate":false}""",
                    HttpStatusCode.Created,
                    headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }

        KtorOrderSyncApi(client).submit(orderOf("order-1"))

        assertEquals("order-1", sentKey)
        assertTrue(sentBody!!.contains("\"order_id\":\"order-1\""), sentBody!!)
    }

    @Test
    fun theRequestBodyCarriesTheOrderTotalsAndLinesAsWholeCents() = runTest {
        var sentBody: String? = null
        val client =
            testClient { request ->
                sentBody = request.body.toByteArray().decodeToString()
                respond(
                    """{"order_id":"order-1","duplicate":false}""",
                    HttpStatusCode.Created,
                    headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }

        KtorOrderSyncApi(client).submit(orderOf("order-1", quantity = 2))

        val body = sentBody!!
        assertTrue(body.contains("\"total_cents\":3850"), body)
        assertTrue(body.contains("\"unit_price_cents\":1750"), body)
        assertTrue(body.contains("\"quantity\":2"), body)
    }

    @Test
    fun theFirstAttemptFailsAndTheOrderIsNotAccepted() = runTest {
        val backend = MockPosBackend(TransientFailureMode.FirstOrderOnly)
        val api = KtorOrderSyncApi(backend.createClient())

        val failure = assertFailsWith<ServerResponseException> { api.submit(orderOf("order-1")) }

        assertEquals(HttpStatusCode.ServiceUnavailable, failure.response.status)
        assertTrue(backend.acceptedOrderIds.isEmpty(), "a failed order must not be accepted")
    }

    @Test
    fun aRetryAfterTheTransientFailureIsAcceptedExactlyOnce() = runTest {
        val backend = MockPosBackend(TransientFailureMode.FirstOrderOnly)
        val api = KtorOrderSyncApi(backend.createClient())
        runCatching { api.submit(orderOf("order-1")) }

        val acknowledgement = api.submit(orderOf("order-1"))

        assertEquals("order-1", acknowledgement.orderId)
        assertTrue(!acknowledgement.duplicate, "the retry is the first acceptance, not a duplicate")
        assertEquals(listOf("order-1"), backend.acceptedOrderIds)
        assertEquals(2, backend.requestCountFor("order-1"))
    }

    @Test
    fun replayingAnAcceptedOrderIsAcknowledgedWithoutAcceptingItTwice() = runTest {
        val backend = MockPosBackend(TransientFailureMode.None)
        val api = KtorOrderSyncApi(backend.createClient())

        val first = api.submit(orderOf("order-1"))
        val replay = api.submit(orderOf("order-1"))

        assertTrue(!first.duplicate)
        assertTrue(replay.duplicate, "the second submission must be reported as a duplicate")
        assertEquals(listOf("order-1"), backend.acceptedOrderIds)
        assertEquals(2, backend.requestCountFor("order-1"))
        assertEquals(listOf(201, 200), backend.orderRequests.map { it.statusCode })
    }

    @Test
    fun differentOrdersAreAcceptedIndependently() = runTest {
        val backend = MockPosBackend(TransientFailureMode.None)
        val api = KtorOrderSyncApi(backend.createClient())

        api.submit(orderOf("order-1"))
        api.submit(orderOf("order-2"))

        assertEquals(listOf("order-1", "order-2"), backend.acceptedOrderIds)
    }

    @Test
    fun anIdempotencyKeyThatDoesNotMatchTheBodyIsRejected() = runTest {
        val backend = MockPosBackend(TransientFailureMode.None)
        val client = backend.createClient()

        val failure =
            assertFailsWith<ClientRequestException> {
                client.post("$POS_BASE_URL/orders") {
                    header(IDEMPOTENCY_KEY_HEADER, "a-different-key")
                    contentType(ContentType.Application.Json)
                    setBody(orderOf("order-1").toRequestDto())
                }
            }

        assertEquals(HttpStatusCode.BadRequest, failure.response.status)
        assertTrue(backend.acceptedOrderIds.isEmpty())
    }

    private fun orderOf(id: String, quantity: Int = 1): Order =
        Cart()
            .add(product(id = "sku-2001", priceCents = 1_750, stock = 12, taxable = true), quantity)
            .toOrder(id = id, createdAtEpochMillis = 1_700_000_000_000)
}
