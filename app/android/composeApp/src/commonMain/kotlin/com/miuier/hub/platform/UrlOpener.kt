package com.miuier.hub.platform

import androidx.compose.runtime.Composable

/**
 * 打开外部链接（ROM 下载地址）。
 *
 * 做成 @Composable 是因为 Android 侧需要 `LocalContext`；
 * 返回一个函数而不是直接暴露 Activity，调用方就不用关心平台差异。
 */
@Composable
expect fun rememberUrlOpener(): (String) -> Unit
