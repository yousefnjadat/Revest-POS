package com.example.pos.di

import com.example.pos.data.catalog.CatalogApi
import com.example.pos.data.catalog.CatalogRepository
import com.example.pos.data.catalog.DefaultCatalogRepository
import com.example.pos.data.catalog.KtorCatalogApi
import com.example.pos.data.order.DefaultOrderRepository
import com.example.pos.data.order.KtorOrderSyncApi
import com.example.pos.data.order.OrderLocalDataSource
import com.example.pos.data.order.OrderRepository
import com.example.pos.data.order.OrderSyncApi
import com.example.pos.data.remote.MockPosBackend
import com.example.pos.data.sync.OrderSyncCoordinator
import com.example.pos.db.PosDatabase
import com.example.pos.presentation.PosViewModel
import io.ktor.client.HttpClient
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

expect val platformModule: Module

val appModule: Module =
    module {
        single { MockPosBackend() }
        single<HttpClient> { get<MockPosBackend>().createClient() }
        single<CatalogApi> { KtorCatalogApi(get()) }
        single<CatalogRepository> { DefaultCatalogRepository(get()) }

        single { PosDatabase(get()) }
        single { OrderLocalDataSource(get(), ioDispatcher) }
        single<OrderRepository> { DefaultOrderRepository(get()) }

        single<OrderSyncApi> { KtorOrderSyncApi(get()) }
        single { OrderSyncCoordinator(orders = get(), api = get()) }

        viewModel { PosViewModel(catalog = get(), orders = get(), sync = get()) }
    }

fun initKoin(appDeclaration: KoinApplication.() -> Unit = {}): KoinApplication =
    startKoin {
        appDeclaration()
        modules(platformModule, appModule)
    }
