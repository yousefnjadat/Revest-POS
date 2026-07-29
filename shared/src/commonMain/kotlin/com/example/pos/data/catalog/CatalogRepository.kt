package com.example.pos.data.catalog

import com.example.pos.domain.Product
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


sealed interface CatalogResult {
    data class Success(val products: List<Product>) : CatalogResult
    data class Failure(val message: String, val cause: Throwable? = null) : CatalogResult
}

interface CatalogRepository {
    suspend fun loadProducts(): CatalogResult
}

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
