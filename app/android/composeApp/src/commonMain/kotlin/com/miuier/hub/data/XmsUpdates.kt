package com.miuier.hub.data

import com.miuier.hub.platform.InstalledApp

/** 本机装着、且 hub 数据里有更高版本的一个应用 */
data class XmsUpdate(
    val entry: XmsAppEntry,
    /** 本机的 `versionName`，如 `4.2.1`（个别应用为空） */
    val installedVersion: String,
    /** 数据里的最新版本，和 `latestVersion` 一样是 versionCode 编码 */
    val latestVersion: String,
)

/**
 * 用 XMS 应用总表和本机已安装应用做比对。
 *
 * `latestVersion` 取自 APK 的 versionCode（纯数字），本机也用 `PackageInfo.longVersionCode`，
 * 两边是同一套编码，直接数值比较即可；还没解析出版本号的数据（空串）跳过。
 * 结果按发布日期倒序，最新的更新排在最前。
 */
fun findXmsUpdates(
    index: XmsIndex,
    installed: Map<String, InstalledApp>,
): List<XmsUpdate> {
    if (installed.isEmpty()) return emptyList()
    return index.apps
        .mapNotNull { entry ->
            val local = installed[entry.pkg] ?: return@mapNotNull null
            val latest = entry.latestVersion.toLongOrNull() ?: return@mapNotNull null
            if (latest <= local.versionCode) return@mapNotNull null
            XmsUpdate(
                entry = entry,
                installedVersion = local.versionName,
                latestVersion = entry.latestVersion,
            )
        }
        .sortedByDescending { it.entry.releaseDate }
}
