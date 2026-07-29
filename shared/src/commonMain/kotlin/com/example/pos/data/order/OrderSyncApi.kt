package com.example.pos.data.order

import com.example.pos.data.remote.IDEMPOTENCY_KEY_HEADER
import com.example.pos.data.remote.POS_BASE_URL
import com.example.pos.domain.Order
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class OrderRequestDto(
    @SerialName("order_id") val orderId: String,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("subtotal_cents") val subtotalCents: Long,
    @SerialName("tax_cents") val taxCents: Long,
    @SerialName("discount_cents") val discountCents: Long,
    @SerialName("total_cents") val totalCents: Long,
    val lines: List<OrderLineRequestDto>,
)

@Serializable
internal data class OrderLineRequestDto(
    @SerialName("product_id") val productId: String,
    val name: String,
    @SerialName("unit_price_cents") val unitPriceCents: Long,
    val quantity: Int,
    val taxable: Boolean,
)

@Serializable
internal data class OrderAcceptedDto(
    @SerialName("order_id") val orderId: String,
    val duplicate: Boolean,
)

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

internal fun Order.toRequestDto(): OrderRequestDto =
    OrderRequestDto(
        orderId = id,
        createdAt = createdAtEpochMillis,
        subtotalCents = totals.subtotalCents,
        taxCents = totals.taxCents,
        discountCents = totals.discountCents,
        totalCents = totals.totalCents,
        lines =
            lines.map {
                OrderLineRequestDto(
                    productId = it.productId,
                    name = it.name,
                    unitPriceCents = it.unitPriceCents,
                    quantity = it.quantity,
                    taxable = it.taxable,
                )
            },
    )
