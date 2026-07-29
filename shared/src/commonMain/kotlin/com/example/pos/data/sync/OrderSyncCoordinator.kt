package com.example.pos.data.sync

import com.example.pos.PosLog
import com.example.pos.data.order.OrderRepository
import com.example.pos.data.order.OrderSyncApi
import com.example.pos.domain.Order
import com.example.pos.domain.SyncTrigger
import io.ktor.client.plugins.ResponseException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex

/** What one sync run did. [skipped] means another run already held the lock. */
internal data class SyncOutcome(
    val syncedCount: Int = 0,
    val failedCount: Int = 0,
    val skipped: Boolean = false,
)

/**
 * Pushes locally stored orders to the backend, one at a time.
 *
 * This class knows nothing about connectivity — the caller decides whether the app is online and
 * only then asks for a sync. What it does own is that a checkout is written to the database
 * *before* any network call, and that two overlapping triggers never run the loop twice.
 */
internal class OrderSyncCoordinator(
    private val orders: OrderRepository,
    private val api: OrderSyncApi,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val log: (String) -> Unit = PosLog::sync,
) {
    private val mutex = Mutex()
    private val _isSyncing = MutableStateFlow(false)

    /** True while a sync run is in flight, for the syncing indicator in the UI. */
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    /**
     * Sends every unsynced order, oldest first. A run that overlaps another is skipped rather
     * than queued: the run already in flight will pick up the same backlog.
     */
    suspend fun sync(trigger: SyncTrigger): SyncOutcome {
        log("sync requested (${trigger.label})")

        if (!mutex.tryLock()) {
            log("sync skipped (${trigger.label}): a sync is already running")
            return SyncOutcome(skipped = true)
        }

        _isSyncing.value = true
        try {
            val pending = orders.unsyncedOrders()
            if (pending.isEmpty()) {
                log("sync finished (${trigger.label}): nothing pending")
                return SyncOutcome()
            }

            log("sync started (${trigger.label}): ${pending.size} order(s) pending")
            var synced = 0
            var failed = 0
            for (order in pending) {
                if (submit(order)) synced++ else failed++
            }

            log("sync finished (${trigger.label}): synced=$synced failed=$failed")
            return SyncOutcome(syncedCount = synced, failedCount = failed)
        } finally {
            _isSyncing.value = false
            mutex.unlock()
        }
    }

    /** Returns true when the backend accepted the order. One order failing never stops the run. */
    private suspend fun submit(order: Order): Boolean {
        val shortId = order.id.take(8)
        orders.recordSyncAttempt(order.id)
        return try {
            val acknowledgement = api.submit(order)
            orders.markSynced(order.id, now())
            val note = if (acknowledgement.duplicate) " (already accepted, not duplicated)" else ""
            log("order $shortId synced$note")
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            val message = failure.toSyncErrorMessage()
            orders.recordSyncError(order.id, message)
            log("order $shortId failed: $message")
            false
        }
    }
}

/** A short, storable reason. Kept to one line so the Orders screen can show it as-is. */
internal fun Throwable.toSyncErrorMessage(): String =
    when (this) {
        is ResponseException -> "Server responded ${response.status.value}"
        else -> message?.takeIf { it.isNotBlank() }?.take(120) ?: "Sync failed"
    }
