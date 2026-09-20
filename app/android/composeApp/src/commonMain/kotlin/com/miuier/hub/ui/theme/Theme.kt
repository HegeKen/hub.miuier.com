package com.miuier.hub.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.miuier.hub.ThemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * 主题与 Updater-KMP 对齐：**直接用 MIUIX 自带的明暗配色**。
 *
 * 这里刻意不用 `ThemeController(keyColor = …)` 那套「种子色 + Monet 推导」：
 * 种子色推出来的 primary / container 会随种子漂移，和 HyperOS 原生控件的观感对不上。
 * 只给一个布尔值，明暗两套色板全部由 MIUIX 决定，与参考实现（Updater-KMP 的
 * `AppTheme`）逐字一致。
 */
@Composable
fun AppTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    MiuixTheme(
        colors = if (dark) darkColorScheme() else lightColorScheme(),
        content = content,
    )
}
