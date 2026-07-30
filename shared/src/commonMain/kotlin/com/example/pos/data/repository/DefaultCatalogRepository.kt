package com.example.pos.data.repository

import com.example.pos.data.datasource.remote.CatalogApi
import com.example.pos.data.dto.toProduct
import com.example.pos.domain.repository.CatalogRepository
import com.example.pos.domain.repository.CatalogResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

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