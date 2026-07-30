package com.example.pos.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.pos.db.PosDatabase
import java.io.File
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module =
    module {
        single<SqlDriver> {
            val dbFile = File(appDataDir(), "pos.db")
            val isNew = !dbFile.exists()
            JdbcSqliteDriver(url = "jdbc:sqlite:${dbFile.absolutePath}").also { driver ->
                if (isNew) PosDatabase.Schema.create(driver)
            }
        }
    }

private fun appDataDir(): File =
    File(System.getProperty("user.home"), ".revest-pos").apply { mkdirs() }
