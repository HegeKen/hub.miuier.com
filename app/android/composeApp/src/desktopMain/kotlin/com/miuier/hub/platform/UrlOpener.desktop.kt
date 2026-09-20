package com.miuier.hub.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.Desktop
import java.net.URI

@Composable
actual fun rememberUrlOpener(): (String) -> Unit = remember {
    { url -> runCatching { Desktop.getDesktop().browse(URI(url)) } }
}
