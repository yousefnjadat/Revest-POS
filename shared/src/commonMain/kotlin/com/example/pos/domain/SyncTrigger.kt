package com.example.pos.domain

/** Why a sync run started. Logged on every run so the trigger is visible in a bug report. */
enum class SyncTrigger(val label: String) {
    /** The cashier pressed sync. */
    MANUAL("manual trigger"),

    /** The connection toggle moved from offline to online. */
    CAME_ONLINE("changed to online"),

    /** An order was checked out while already online. */
    CHECKOUT("checkout while online"),
}
