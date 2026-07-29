package com.example.pos.data.catalog

import com.example.pos.data.remote.POS_BASE_URL
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

internal interface CatalogApi {
    suspend fun fetchCatalog(): List<ProductDto>
}

internal class KtorCatalogApi(
    private val client: HttpClient,
) : CatalogApi {
    override suspend fun fetchCatalog(): List<ProductDto> =
        client.get("$POS_BASE_URL/catalog").body<CatalogResponseDto>().products
}
