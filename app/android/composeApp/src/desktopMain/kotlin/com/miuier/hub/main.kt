package com.miuier.hub

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/**
 * 桌面入口：只为方便在开发机上直接看 UI（`./gradlew :composeApp:desktopRun`），
 * 不是交付形态。窗口大小按手机竖屏比例，方便对照移动端布局。
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "MiROMS HUB",
        state = rememberWindowState(size = DpSize(400.dp, 860.dp)),
    ) {
        App()
    }
}
