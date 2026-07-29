package com.example.pos.data.catalog

import com.example.pos.domain.Product
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CatalogResponseDto(
    val products: List<ProductDto>,
)

@Serializable
internal data class ProductDto(
    val id: String,
    val name: String,
    @SerialName("price_cents") val priceCents: Long,
    val taxable: Boolean,
    val stock: Int,
)

/**
 * Wire model to domain model. [Product] validates its own price and stock, so a response with
 * nonsense values fails here and surfaces as a catalog failure rather than a broken cart later.
 */
internal fun ProductDto.toProduct(): Product =
    Product(
        id = id,
        name = name,
        priceCents = priceCents,
        stock = stock,
        taxable = taxable,
    )
