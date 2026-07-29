package com.example.pos.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.pos.db.PosDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module =
    module {
        single<SqlDriver> {
            AndroidSqliteDriver(
                schema = PosDatabase.Schema,
                context = androidContext(),
                name = "pos.db",
            )
        }
    }
