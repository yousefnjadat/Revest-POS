package com.example.pos.data.order.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.example.pos.db.PosDatabase
import com.example.pos.domain.model.Order
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

internal class OrderLocalDataSource(
    database: PosDatabase,
    private val dispatcher: CoroutineDispatcher,
) {
    private val queries = database.pendingOrdersQueries

    fun observeOrders(): Flow<List<Order>> =
        queries.selectAll(::orderFromRow).asFlow().mapToList(dispatcher)

    suspend fun insert(order: Order): Unit =
        withContext(dispatcher) {
            queries.insertOrder(
                id = order.id,
                payload = encodeLines(order.lines),
                subtotal_cents = order.totals.subtotalCents,
                tax_cents = order.totals.taxCents,
                discount_cents = order.totals.discountCents,
                total_cents = order.totals.totalCents,
                created_at = order.createdAtEpochMillis,
            )
        }

    suspend fun unsyncedOrders(): List<Order> =
        withContext(dispatcher) { queries.selectUnsynced(::orderFromRow).executeAsList() }

    suspend fun findById(orderId: String): Order? =
        withContext(dispatcher) { queries.selectById(orderId, ::orderFromRow).executeAsOneOrNull() }

    suspend fun recordSyncAttempt(orderId: String): Unit =
        withContext(dispatcher) { queries.recordSyncAttempt(orderId) }

    suspend fun markSynced(orderId: String, syncedAtEpochMillis: Long): Unit =
        withContext(dispatcher) { queries.markSynced(syncedAtEpochMillis, orderId) }

    suspend fun recordSyncError(orderId: String, message: String): Unit =
        withContext(dispatcher) { queries.recordSyncError(message, orderId) }
}
