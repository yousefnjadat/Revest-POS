package com.example.pos.android

import android.app.Application
import com.example.pos.di.initKoin
import org.koin.android.ext.koin.androidContext

class PosApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin { androidContext(this@PosApplication) }
    }
}
