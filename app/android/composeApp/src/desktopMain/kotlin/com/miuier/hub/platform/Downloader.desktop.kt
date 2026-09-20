package com.miuier.hub.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.Desktop
import java.net.URI

/**
 * 桌面端没有 DownloadManager 这种东西，交给浏览器下载。
 * （桌面端本来就只是用来本机看 UI 的，不是交付形态。）
 */
@Composable
actual fun rememberDownloader(): (DownloadRequest) -> Unit = remember {
    { request -> runCatching { Desktop.getDesktop().browse(URI(request.url)) } }
}
