package com.example.pos.data.remote

import com.example.pos.data.order.OrderAcceptedDto
import com.example.pos.data.order.OrderRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/** Host the mock backend answers on. MockEngine ignores it, but Ktor still needs a valid URL. */
const val POS_BASE_URL: String = "https://pos.example.com"

/** Carries the order UUID so a retry can be recognised as the same order. */
const val IDEMPOTENCY_KEY_HEADER: String = "Idempotency-Key"

internal val PosJson: Json = Json {
    ignoreUnknownKeys = true
    prettyPrint = false
}

/** Which orders the fake backend fails once before accepting. */
enum class TransientFailureMode {
    /** The first order the backend ever sees fails its first attempt, then succeeds on retry. */
    FirstOrderOnly,

    /** Nothing fails. */
    None,
}

/** One request the backend handled, kept so tests can assert what actually went over the wire. */
data class RecordedOrderRequest(
    val orderId: String,
    val idempotencyKey: String?,
    val statusCode: Int,
    val duplicate: Boolean,
)

/**
 * A stand-in for the POS backend, served in-process by Ktor's [MockEngine]. It is the app's real
 * HTTP path — same client, same serialization — with a canned server on the other end.
 *
 * `POST /orders` is idempotent: the backend keeps the set of order ids it has accepted, so a
 * replayed UUID is acknowledged as a duplicate instead of being recorded a second time. One
 * transient `503` is simulated according to [failureMode], and the failed order is deliberately
 * *not* added to the accepted set, so a retry goes through the full accept path.
 *
 * State is guarded by a [Mutex] so overlapping requests cannot corrupt it.
 */
class MockPosBackend(
    private val failureMode: TransientFailureMode = TransientFailureMode.FirstOrderOnly,
) {
    private val mutex = Mutex()
    private val accepted = LinkedHashSet<String>()
    private val failedOnce = LinkedHashSet<String>()
    private val requests = mutableListOf<RecordedOrderRequest>()

    /** Order ids the backend has accepted, in acceptance order. */
    val acceptedOrderIds: List<String> get() = accepted.toList()

    /** Every `POST /orders` the backend handled, oldest first. */
    val orderRequests: List<RecordedOrderRequest> get() = requests.toList()

    fun requestCountFor(orderId: String): Int = requests.count { it.orderId == orderId }

    fun createClient(): HttpClient =
        HttpClient(
            MockEngine { request ->
                when {
                    request.method == HttpMethod.Get && request.url.encodedPath == "/catalog" ->
                        respondJson(CATALOG_RESPONSE_JSON, HttpStatusCode.OK)

                    request.method == HttpMethod.Post && request.url.encodedPath == "/orders" ->
                        handleOrderSubmission(request)

                    else ->
                        respondJson("""{"error":"unknown endpoint"}""", HttpStatusCode.NotFound)
                }
            },
        ) {
            expectSuccess = true
            install(ContentNegotiation) { json(PosJson) }
        }

    private suspend fun MockRequestHandleScope.handleOrderSubmission(
        request: HttpRequestData,
    ): HttpResponseData {
        val body =
            PosJson.decodeFromString(
                OrderRequestDto.serializer(),
                request.body.toByteArray().decodeToString(),
            )
        val idempotencyKey = request.headers[IDEMPOTENCY_KEY_HEADER]

        val (status, duplicate) =
            mutex.withLock {
                when {
                    idempotencyKey != body.orderId ->
                        HttpStatusCode.BadRequest to false

                    body.orderId in accepted ->
                        // Already recorded: acknowledge without accepting it twice.
                        HttpStatusCode.OK to true

                    shouldFailNow() -> {
                        failedOnce += body.orderId
                        HttpStatusCode.ServiceUnavailable to false
                    }

                    else -> {
                        accepted += body.orderId
                        HttpStatusCode.Created to false
                    }
                }.also { (status, duplicate) ->
                    requests +=
                        RecordedOrderRequest(
                            orderId = body.orderId,
                            idempotencyKey = idempotencyKey,
                            statusCode = status.value,
                            duplicate = duplicate,
                        )
                }
            }

        val responseBody =
            when (status) {
                HttpStatusCode.BadRequest ->
                    """{"error":"idempotency key must match the order id"}"""

                HttpStatusCode.ServiceUnavailable ->
                    """{"error":"the order service is temporarily unavailable"}"""

                else ->
                    PosJson.encodeToString(
                        OrderAcceptedDto.serializer(),
                        OrderAcceptedDto(orderId = body.orderId, duplicate = duplicate),
                    )
            }

        return respondJson(responseBody, status)
    }

    private fun shouldFailNow(): Boolean =
        when (failureMode) {
            TransientFailureMode.None -> false
            // Only the very first order the backend sees is disrupted, and only once.
            TransientFailureMode.FirstOrderOnly -> failedOnce.isEmpty() && accepted.isEmpty()
        }

    private fun MockRequestHandleScope.respondJson(
        body: String,
        status: HttpStatusCode,
    ) = respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
}

/**
 * The catalog the fake backend serves: a small coffee-shop counter.
 *
 * Food is tax exempt and hardware is taxable, so mixed carts exercise the taxable subtotal.
 * The mug (17.50) plus the flask (32.50) lands on exactly 50.00, which demonstrates the
 * inclusive discount threshold; the grinder alone (54.99) clears it on its own. Stock ranges
 * from 120 down to 0, so both the quantity cap and the out-of-stock case are reachable.
 */
private val CATALOG_RESPONSE_JSON =
    """
    {
      "products": [
        { "id": "sku-1001", "name": "Espresso Beans 250g",  "price_cents": 1250, "taxable": false, "stock": 24 },
        { "id": "sku-1002", "name": "Oat Milk 1L",          "price_cents": 349,  "taxable": false, "stock": 40 },
        { "id": "sku-1003", "name": "Blueberry Muffin",     "price_cents": 295,  "taxable": false, "stock": 6 },
        { "id": "sku-2001", "name": "Ceramic Mug",          "price_cents": 1750, "taxable": true,  "stock": 12 },
        { "id": "sku-2002", "name": "Travel Flask 500ml",   "price_cents": 3250, "taxable": true,  "stock": 5 },
        { "id": "sku-2003", "name": "Burr Coffee Grinder",  "price_cents": 5499, "taxable": true,  "stock": 3 },
        { "id": "sku-3001", "name": "Paper Cups 50-pack",   "price_cents": 899,  "taxable": true,  "stock": 120 },
        { "id": "sku-3002", "name": "Barista Apron",        "price_cents": 2600, "taxable": true,  "stock": 0 }
      ]
    }
    """.trimIndent()
