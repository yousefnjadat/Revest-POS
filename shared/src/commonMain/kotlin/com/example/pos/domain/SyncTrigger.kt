package com.example.pos.domain

enum class SyncTrigger(val label: String) {
    MANUAL("manual trigger"),

    CAME_ONLINE("changed to online"),

    CHECKOUT("checkout while online"),
}
