package com.example.pos.data.order.model

import com.example.pos.domain.model.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The JSON body of `POST /orders`. Money crosses the wire as whole cents, never a decimal. */
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

/** The backend's answer. [duplicate] is true when this order id had already been accepted. */
@Serializable
internal data class OrderAcceptedDto(
    @SerialName("order_id") val orderId: String,
    val duplicate: Boolean,
)

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
