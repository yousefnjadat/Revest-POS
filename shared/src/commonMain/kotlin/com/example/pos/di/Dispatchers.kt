package com.example.pos.di

import kotlinx.coroutines.CoroutineDispatcher

/** Dispatcher for blocking I/O — SQLite in particular. Never the main thread. */
expect val ioDispatcher: CoroutineDispatcher
