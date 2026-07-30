package com.example.pos.core


internal object PosLog {
    fun sync(message: String) {
        println("[POS][sync] $message")
    }
}
