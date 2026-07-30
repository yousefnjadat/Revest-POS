package com.example.pos.domain.repository

import com.example.pos.domain.model.Order
import kotlinx.coroutines.flow.Flow

/**
 * The local database is the source of truth for checked-out orders: a checkout is persisted
 * here first and only then offered to the backend.
 *
 * Declared in `domain` so the view model and sync coordinator depend on this contract rather than
 * on SQLDelight.
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
