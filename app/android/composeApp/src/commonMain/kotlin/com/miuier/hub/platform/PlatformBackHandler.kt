package com.miuier.hub.platform

import androidx.compose.runtime.Composable

/**
 * 系统返回。Android 上是返回手势 / 返回键；桌面端没有这个概念，
 * 由顶栏的返回按钮承担（见 App.kt）。
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
