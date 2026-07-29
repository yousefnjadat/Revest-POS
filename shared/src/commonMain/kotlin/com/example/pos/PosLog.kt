package com.example.pos

/**
 * Deliberately the smallest thing that works. Sync activity is the one flow worth tracing in
 * this app, and a logging framework would be more setup than it is worth here.
 */
internal object PosLog {
    fun sync(message: String) {
        println("[POS][sync] $message")
    }
}
