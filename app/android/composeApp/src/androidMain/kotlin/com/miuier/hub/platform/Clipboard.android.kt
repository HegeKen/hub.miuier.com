package com.miuier.hub.platform

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard

internal actual suspend fun Clipboard.copyToClipboard(text: String) {
    setClipEntry(ClipEntry(ClipData.newPlainText("MiROMS HUB", text)))
}
