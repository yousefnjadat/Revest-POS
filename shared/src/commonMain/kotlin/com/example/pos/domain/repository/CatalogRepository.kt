package com.example.pos.domain.repository

import com.example.pos.domain.model.Product

/**
 * The outcome of a catalog load. Intentionally specific to the catalog rather than a general
 * purpose result type: the presentation layer only has to handle these two cases.
 */
sealed interface CatalogResult {
    data class Success(val products: List<Product>) : CatalogResult

    /** [message] is safe to show to the cashier; [cause] is kept for logging. */
    data class Failure(val message: String, val cause: Throwable? = null) : CatalogResult
}

/**
 * Reads the product catalog. Declared here in `domain` so the rest of the app depends on the
 * contract, never on the Ktor implementation that fulfils it.
 */
interface CatalogRepository {
    suspend fun loadProducts(): CatalogResult
}
