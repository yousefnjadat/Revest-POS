package com.example.pos.domain.repository

import com.example.pos.domain.model.Product

sealed interface CatalogResult {
    data class Success(val products: List<Product>) : CatalogResult

    data class Failure(val message: String, val cause: Throwable? = null) : CatalogResult
}

interface CatalogRepository {
    suspend fun loadProducts(): CatalogResult
}
