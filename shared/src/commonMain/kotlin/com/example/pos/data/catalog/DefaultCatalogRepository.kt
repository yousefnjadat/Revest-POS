package com.example.pos.data.catalog

import com.example.pos.data.catalog.model.toProduct
import com.example.pos.data.catalog.remote.CatalogApi
import com.example.pos.domain.repository.CatalogRepository
import com.example.pos.domain.repository.CatalogResult
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fulfils [CatalogRepository] over the network: fetch, map DTOs to domain models, and turn any
 * failure — transport, HTTP status, malformed JSON, invalid values — into a single [CatalogResult].
 */
internal class DefaultCatalogRepository(
    private val api: CatalogApi,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : CatalogRepository {
    override suspend fun loadProducts(): CatalogResult =
        withContext(dispatcher) {
            try {
                CatalogResult.Success(api.fetchCatalog().map { it.toProduct() })
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                CatalogResult.Failure(
                    message = "Couldn't load the catalog. Check the connection and try again.",
                    cause = failure,
                )
            }
        }
}
