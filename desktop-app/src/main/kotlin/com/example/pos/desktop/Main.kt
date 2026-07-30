package com.example.pos.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.pos.di.initKoin
import com.example.pos.ui.PosApp

fun main() {
    initKoin()
    application {
        val windowState = rememberWindowState(width = 440.dp, height = 900.dp)
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "Revest POS",
        ) {
            PosApp()
        }
    }
}
