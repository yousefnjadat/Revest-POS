package com.example.pos.data.order

import com.example.pos.data.order.local.OrderLocalDataSource
import com.example.pos.domain.model.Order
import com.example.pos.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow

/** Fulfils [OrderRepository] by delegating to the SQLDelight-backed local data source. */
internal class DefaultOrderRepository(
    private val local: OrderLocalDataSource,
) : OrderRepository {
    override fun observeOrders(): Flow<List<Order>> = local.observeOrders()

    override suspend fun save(order: Order) = local.insert(order)

    override suspend fun unsyncedOrders(): List<Order> = local.unsyncedOrders()

    override suspend fun findById(orderId: String): Order? = local.findById(orderId)

    override suspend fun recordSyncAttempt(orderId: String) = local.recordSyncAttempt(orderId)

    override suspend fun markSynced(orderId: String, syncedAtEpochMillis: Long) =
        local.markSynced(orderId, syncedAtEpochMillis)

    override suspend fun recordSyncError(orderId: String, message: String) =
        local.recordSyncError(orderId, message)
}
