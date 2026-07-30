package com.example.pos.data.catalog.model

import com.example.pos.domain.model.Product
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

internal fun ProductDto.toProduct(): Product =
    Product(
        id = id,
        name = name,
        priceCents = priceCents,
        stock = stock,
        taxable = taxable,
    )
