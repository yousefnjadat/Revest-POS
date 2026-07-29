package com.example.pos.presentation

import com.example.pos.data.catalog.CatalogRepository
import com.example.pos.data.catalog.CatalogResult
import com.example.pos.data.order.OrderRepository
import com.example.pos.data.order.OrderSyncApi
import com.example.pos.data.order.SyncAcknowledgement
import com.example.pos.domain.Order
import com.example.pos.domain.OrderSyncState
import com.example.pos.domain.Product
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeCatalogRepository(
    var result: CatalogResult = CatalogResult.Success(SampleProducts),
) : CatalogRepository {
    var loadCount = 0
        private set

    override suspend fun loadProducts(): CatalogResult {
        loadCount++
        return result
    }
}

/** An in-memory stand-in for the SQLDelight repository, with the same idempotent save. */
internal class FakeOrderRepository : OrderRepository {
    private val stored = MutableStateFlow<List<Order>>(emptyList())
    var failOnSave = false

    override fun observeOrders(): Flow<List<Order>> = stored

    override suspend fun save(order: Order) {
        if (failOnSave) error("disk full")
        if (stored.value.none { it.id == order.id }) {
            stored.value = (stored.value + order).sortedByDescending { it.createdAtEpochMillis }
        }
    }

    override suspend fun unsyncedOrders(): List<Order> =
        stored.value.filter { it.syncState.needsSync }.sortedBy { it.createdAtEpochMillis }

    override suspend fun findById(orderId: String): Order? =
        stored.value.firstOrNull { it.id == orderId }

    override suspend fun recordSyncAttempt(orderId: String) =
        update(orderId) { it.copy(attemptCount = it.attemptCount + 1) }

    override suspend fun markSynced(orderId: String, syncedAtEpochMillis: Long) =
        update(orderId) {
            it.copy(
                syncState = OrderSyncState.SYNCED,
                syncedAtEpochMillis = syncedAtEpochMillis,
                lastError = null,
            )
        }

    override suspend fun recordSyncError(orderId: String, message: String) =
        update(orderId) { it.copy(syncState = OrderSyncState.FAILED, lastError = message) }

    private fun update(orderId: String, change: (Order) -> Order) {
        stored.value = stored.value.map { if (it.id == orderId) change(it) else it }
    }
}

/** Records what was submitted, and can be made to fail or to block until released. */
internal class FakeOrderSyncApi : OrderSyncApi {
    val submitted = mutableListOf<String>()
    var failure: Exception? = null
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun submit(order: Order): SyncAcknowledgement {
        submitted += order.id
        gate?.await()
        failure?.let { throw it }
        return SyncAcknowledgement(order.id, duplicate = false)
    }
}

internal val SampleProducts =
    listOf(
        Product(id = "mug", name = "Ceramic Mug", priceCents = 1_750, stock = 12, taxable = true),
        Product(id = "flask", name = "Travel Flask", priceCents = 3_250, stock = 2, taxable = true),
        Product(id = "muffin", name = "Blueberry Muffin", priceCents = 295, stock = 6, taxable = false),
        Product(id = "apron", name = "Barista Apron", priceCents = 2_600, stock = 0, taxable = true),
    )
