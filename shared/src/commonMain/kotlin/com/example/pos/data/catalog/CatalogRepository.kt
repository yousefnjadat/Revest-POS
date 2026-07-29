package com.example.pos.data.catalog

import com.example.pos.domain.Product
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The outcome of a catalog load. Intentionally specific to the catalog rather than a general
 * purpose result type: the presentation layer only has to handle these two cases.
 */
sealed interface CatalogResult {
    data class Success(val products: List<Product>) : CatalogResult

    /** [message] is safe to show to the cashier; [cause] is kept for logging. */
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
