package com.example.pos.data.datasource.remote

import com.example.pos.data.dto.OrderAcceptedDto
import com.example.pos.data.dto.toRequestDto
import com.example.pos.domain.model.Order
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/** [duplicate] is true when the backend had already accepted this order id. */
internal data class SyncAcknowledgement(
    val orderId: String,
    val duplicate: Boolean,
)

/** Submits checked-out orders to the backend. Throws when the request is not accepted. */
internal interface OrderSyncApi {
    suspend fun submit(order: Order): SyncAcknowledgement
}

internal class KtorOrderSyncApi(
    private val client: HttpClient,
) : OrderSyncApi {
    override suspend fun submit(order: Order): SyncAcknowledgement {
        val accepted =
            client
                .post("$POS_BASE_URL/orders") {
                    // The order UUID travels twice on purpose: the header is what an API gateway
                    // or proxy would deduplicate on, the body is what the service itself stores.
                    header(IDEMPOTENCY_KEY_HEADER, order.id)
                    contentType(ContentType.Application.Json)
                    setBody(order.toRequestDto())
                }.body<OrderAcceptedDto>()

        return SyncAcknowledgement(orderId = accepted.orderId, duplicate = accepted.duplicate)
    }
}
