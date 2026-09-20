package com.miuier.hub.platform

import androidx.compose.runtime.Composable

/** 一次下载请求。title 会作为系统下载通知里的标题 */
data class DownloadRequest(val url: String, val title: String)

/**
 * 把下载交给**系统下载器**（Android 的 DownloadManager：
 * 通知栏进度、可暂停续传、失败重试都是系统行为），而不是自己在应用里下。
 *
 * ROM 包动辄 4~5GB，交给系统下载器才能在切后台/断网时继续下。
 * 桌面端没有等价物，退化成用浏览器打开。
 */
@Composable
expect fun rememberDownloader(): (DownloadRequest) -> Unit
