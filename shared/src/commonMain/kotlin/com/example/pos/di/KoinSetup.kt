package com.example.pos.di

import com.example.pos.data.catalog.CatalogApi
import com.example.pos.data.catalog.CatalogRepository
import com.example.pos.data.catalog.DefaultCatalogRepository
import com.example.pos.data.catalog.KtorCatalogApi
import com.example.pos.data.remote.MockPosBackend
import io.ktor.client.HttpClient
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Platform-specific bindings. On Android this supplies the SQLDelight driver,
 * which needs a [android.content.Context].
 */
expect val platformModule: Module

/** Shared bindings: repositories, sync, and the view model. */
val appModule: Module =
    module {
        single<HttpClient> { MockPosBackend.createClient() }
        single<CatalogApi> { KtorCatalogApi(get()) }
        single<CatalogRepository> { DefaultCatalogRepository(get()) }
    }

fun initKoin(appDeclaration: KoinApplication.() -> Unit = {}): KoinApplication =
    startKoin {
        appDeclaration()
        modules(platformModule, appModule)
    }
