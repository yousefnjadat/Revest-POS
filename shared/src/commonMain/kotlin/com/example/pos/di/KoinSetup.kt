package com.example.pos.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Platform-specific bindings. On Android this supplies the SQLDelight driver,
 * which needs a [android.content.Context].
 */
expect val platformModule: Module

/** Shared bindings: repositories, sync, and the view model. Populated in later phases. */
val appModule: Module = module {}

fun initKoin(appDeclaration: KoinApplication.() -> Unit = {}): KoinApplication =
    startKoin {
        appDeclaration()
        modules(platformModule, appModule)
    }
