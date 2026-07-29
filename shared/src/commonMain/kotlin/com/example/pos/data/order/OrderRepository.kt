package com.example.pos.data.order

import com.example.pos.domain.Order
import kotlinx.coroutines.flow.Flow

/**
 * The local database is the source of truth for checked-out orders: a checkout is persisted
 * here first and only then offered to the backend.
 */
interface OrderRepository {
    /** Newest first. Emits again on every write. */
    fun observeOrders(): Flow<List<Order>>

    /** Idempotent — saving an order id that already exists leaves the stored row untouched. */
    suspend fun save(order: Order)

    /** Oldest first, so a backlog syncs in the order it was rung up. */
    suspend fun unsyncedOrders(): List<Order>

    suspend fun findById(orderId: String): Order?

    suspend fun recordSyncAttempt(orderId: String)

    suspend fun markSynced(orderId: String, syncedAtEpochMillis: Long)

    suspend fun recordSyncError(orderId: String, message: String)
}

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
