package com.miuier.hub.platform

import androidx.compose.runtime.Composable

/** 桌面端只为本地预览，没有「已安装应用」的等价物 —— 返回空表，隐藏「可更新」tab */
@Composable
actual fun rememberInstalledApps(): Map<String, InstalledApp> = emptyMap()
