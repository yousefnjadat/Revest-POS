package com.example.pos.ui

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Order timestamps for a cashier: the clock time, plus the date only when it isn't today.
 * A shift's worth of orders reads as a list of times rather than a wall of repeated dates.
 */
fun formatOrderTime(epochMillis: Long, nowEpochMillis: Long = Clock.System.now().toEpochMilliseconds()): String {
    val zone = TimeZone.currentSystemDefault()
    val moment = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zone)
    val today = Instant.fromEpochMilliseconds(nowEpochMillis).toLocalDateTime(zone).date

    val time = "${moment.hour.padded()}:${moment.minute.padded()}"
    return if (moment.date == today) "Today $time" else "${moment.date} $time"
}

private fun Int.padded(): String = toString().padStart(length = 2, padChar = '0')

/** `6b1efd47-...` becomes `#6B1EFD47` — short enough to read aloud, long enough to be unique here. */
fun orderReference(orderId: String): String = "#" + orderId.take(8).uppercase()
