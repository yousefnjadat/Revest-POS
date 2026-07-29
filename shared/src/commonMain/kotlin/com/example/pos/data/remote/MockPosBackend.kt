package com.example.pos.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Host the mock backend answers on. MockEngine ignores it, but Ktor still needs a valid URL. */
const val POS_BASE_URL: String = "https://pos.example.com"

internal val PosJson: Json = Json {
    ignoreUnknownKeys = true
    prettyPrint = false
}

/**
 * A stand-in for the POS backend, served entirely in-process by Ktor's [MockEngine].
 * It is the app's real HTTP path — the same client and serialization the production
 * app would use, with a canned server on the other end.
 */
object MockPosBackend {
    fun createClient(): HttpClient =
        HttpClient(MockEngine { request ->
            when (request.url.encodedPath) {
                "/catalog" ->
                    respond(
                        content = CATALOG_RESPONSE_JSON,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )

                else ->
                    respond(
                        content = """{"error":"unknown endpoint"}""",
                        status = HttpStatusCode.NotFound,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
            }
        }) {
            expectSuccess = true
            install(ContentNegotiation) { json(PosJson) }
        }
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
