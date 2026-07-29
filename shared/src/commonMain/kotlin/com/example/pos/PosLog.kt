package com.example.pos


internal object PosLog {
    fun sync(message: String) {
        println("[POS][sync] $message")
    }
}
