package com.miuier.hub.platform

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import java.awt.GraphicsEnvironment
import java.awt.datatransfer.StringSelection

@OptIn(ExperimentalComposeUiApi::class)
internal actual suspend fun Clipboard.copyToClipboard(text: String) {
    // 无头渲染（`./gradlew :composeApp:screenshot`）下没有系统剪贴板，
    // 取它会抛 HeadlessException —— 预览里点「复制链接」不该把界面搞崩，直接跳过
    if (GraphicsEnvironment.isHeadless()) return
    setClipEntry(ClipEntry(StringSelection(text)))
}
