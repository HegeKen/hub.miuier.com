package com.miuier.hub.platform

import androidx.compose.ui.platform.Clipboard

/**
 * 往系统剪贴板写一段文本。
 *
 * Compose 已经不推荐 `LocalClipboardManager` / `ClipboardManager`（同步写，只有 Android 有实现），
 * 换成了 `Clipboard.setClipEntry`（suspend，两端都有）。但 `ClipEntry` 的构造是平台相关的
 * （Android 是 `ClipData`、桌面是 `StringSelection`），所以这里补一个 expect/actual 的小封装 ——
 * 与 Updater-KMP 的 `platform/Clipboard.kt` 同一套写法。
 */
internal expect suspend fun Clipboard.copyToClipboard(text: String)
