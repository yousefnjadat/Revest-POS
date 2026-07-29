package com.example.pos

import com.example.pos.di.appModule
import com.example.pos.di.platformModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.uuid.Uuid
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

/** Verifies the multiplatform primitives and DI wiring this project is built on. */
class FoundationTest {
    @Test
    fun uuidsAreUniqueAndCanonicallyFormatted() {
        val ids = List(1_000) { Uuid.random().toString() }

        assertEquals(1_000, ids.toSet().size, "UUIDs must be unique")
        assertTrue(ids.all { id -> id.length == 36 && id.count { it == '-' } == 4 })
    }

    @Test
    fun clockReturnsAWallClockTimestamp() {
        val now = Clock.System.now().toEpochMilliseconds()

        assertTrue(now > 1_700_000_000_000, "expected a current epoch-millis value, got $now")
    }

    @Test
    fun koinStartsAndStopsWithSharedModules() {
        val application = startKoin { modules(platformModule, appModule) }
        try {
            assertNotNull(application.koin)
        } finally {
            stopKoin()
        }
    }
}
