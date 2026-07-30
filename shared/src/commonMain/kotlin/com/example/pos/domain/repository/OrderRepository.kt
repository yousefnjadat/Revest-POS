package com.example.pos.domain.repository

import com.example.pos.domain.model.Order
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    fun observeOrders(): Flow<List<Order>>

    suspend fun save(order: Order)

    suspend fun unsyncedOrders(): List<Order>

    suspend fun findById(orderId: String): Order?

    suspend fun recordSyncAttempt(orderId: String)

    suspend fun markSynced(orderId: String, syncedAtEpochMillis: Long)

    suspend fun recordSyncError(orderId: String, message: String)
}
