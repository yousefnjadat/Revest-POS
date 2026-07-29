package com.example.pos.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** A client wired exactly like the app's, but answering with whatever the test needs. */
internal fun testClient(handler: MockRequestHandler): HttpClient =
    HttpClient(MockEngine(handler)) {
        expectSuccess = true
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

internal fun clientRespondingWith(
    body: String,
    status: HttpStatusCode = HttpStatusCode.OK,
): HttpClient =
    testClient {
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
    }
