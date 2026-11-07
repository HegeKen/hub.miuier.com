package com.miuier.hub.platform

import androidx.compose.runtime.Composable

/** 本机某个已安装应用的版本信息 */
data class InstalledApp(
    /** `PackageInfo.versionName`，如 `4.2.1`；个别应用可能为空 */
    val versionName: String,
    /** `PackageInfo.longVersionCode`，与 XMS 数据里的 `latestVersion` 是同一套编码 */
    val versionCode: Long,
)

/**
 * 读本机已安装应用（包名 → 版本），用来和 XMS 数据比对是否有更新。
 *
 * 桌面端没有「已安装应用」这个概念，`actual` 恒为空表，
 * 此时 XMS 页不会出现「可更新」tab。
 */
@Composable
expect fun rememberInstalledApps(): Map<String, InstalledApp>
