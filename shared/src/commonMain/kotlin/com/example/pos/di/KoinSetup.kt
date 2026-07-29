package com.example.pos.di

import com.example.pos.data.catalog.CatalogApi
import com.example.pos.data.catalog.CatalogRepository
import com.example.pos.data.catalog.DefaultCatalogRepository
import com.example.pos.data.catalog.KtorCatalogApi
import com.example.pos.data.order.DefaultOrderRepository
import com.example.pos.data.order.OrderLocalDataSource
import com.example.pos.data.order.OrderRepository
import com.example.pos.data.remote.MockPosBackend
import com.example.pos.db.PosDatabase
import io.ktor.client.HttpClient
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Platform-specific bindings: the SQLDelight driver, which needs an
 * [android.content.Context] on Android.
 */
expect val platformModule: Module

/** Shared bindings: repositories, sync, and the view model. */
val appModule: Module =
    module {
        single<HttpClient> { MockPosBackend.createClient() }
        single<CatalogApi> { KtorCatalogApi(get()) }
        single<CatalogRepository> { DefaultCatalogRepository(get()) }

        single { PosDatabase(get()) }
        single { OrderLocalDataSource(get(), ioDispatcher) }
        single<OrderRepository> { DefaultOrderRepository(get()) }
    }

fun initKoin(appDeclaration: KoinApplication.() -> Unit = {}): KoinApplication =
    startKoin {
        appDeclaration()
        modules(platformModule, appModule)
    }
