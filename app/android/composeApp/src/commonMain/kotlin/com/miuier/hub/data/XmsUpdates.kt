package com.miuier.hub.data

import com.miuier.hub.platform.InstalledApp
import com.miuier.hub.platform.LocalDevice

/** 本机装着、且 hub 数据里有更高版本的一个应用 */
data class XmsUpdate(
    val entry: XmsAppEntry,
    /** 本机的 `versionName`，如 `4.2.1`（个别应用为空） */
    val installedVersion: String,
    /** 数据里的最新版本，和 `latestVersion` 一样是 versionCode 编码 */
    val latestVersion: String,
    /** 这个最新版本对应的安装包类型（phone / pad / fold / universal） */
    val type: String,
)

/**
 * 本机类别和某个安装包类型是否可比。
 *
 * `universal` 是通用包，任何机型都能装；折叠屏本质还是手机，数据里折叠机也经常
 * 收到 phone / universal 包（只有部分应用单独出 fold 包），所以 fold 也接受 phone。
 * 平板与手机之间绝不互相比较 —— 两边的 versionCode 是两条独立的递增序列，
 * 直接比会把手机误判成「有更新」，还会把平板的安装包下到手机上。
 */
private fun xmsTypeMatches(deviceType: String, appType: String): Boolean {
    val app = appType.lowercase()
    if (app.isBlank() || app == "universal") return true
    val device = deviceType.lowercase()
    if (app == device) return true
    return device == LocalDevice.TYPE_FOLD && app == LocalDevice.TYPE_PHONE
}

/**
 * 用 XMS 应用总表和本机已安装应用做比对。
 *
 * 总表里同一个包名只给了一个 `latestVersion`，它是所有类别（手机 / 平板 / 折叠）里
 * 最大的那个，不能直接拿来和本机比（例如「手机/平板管家」平板的 versionCode 比手机的高）。
 * 因此：
 * 1. 先按本机类别过滤，本机类别没有对应安装包的应用直接跳过；
 * 2. 应用只有一种类型时，`latestVersion` 就是该类型的版本，直接用；
 * 3. 应用跨多种类型时，拉一次应用详情，只在**与本机类别可比**的分组里取版本最高的那个。
 *
 * `latestVersion` 取自 APK 的 versionCode（纯数字），本机也用 `PackageInfo.longVersionCode`，
 * 两边是同一套编码，直接数值比较即可；还没解析出版本号的数据（空串）跳过。
 * 结果按发布日期倒序，最新的更新排在最前。
 */
suspend fun findXmsUpdates(
    index: XmsIndex,
    installed: Map<String, InstalledApp>,
    deviceType: String,
    detailOf: suspend (pkg: String) -> XmsAppDetail?,
): List<XmsUpdate> {
    if (installed.isEmpty()) return emptyList()
    return index.apps
        .mapNotNull { entry ->
            val local = installed[entry.pkg] ?: return@mapNotNull null

            // 总表里一个类型都不适配本机（例如平板专属应用装在手机上）就跳过
            if (entry.types.isNotEmpty() &&
                entry.types.none { xmsTypeMatches(deviceType, it) }
            ) {
                return@mapNotNull null
            }

            val resolved = resolveLatest(entry, deviceType, detailOf) ?: return@mapNotNull null
            val latest = resolved.first.toLongOrNull() ?: return@mapNotNull null
            if (latest <= local.versionCode) return@mapNotNull null

            XmsUpdate(
                entry = entry,
                installedVersion = local.versionName,
                latestVersion = resolved.first,
                type = resolved.second,
            )
        }
        .sortedByDescending { it.entry.releaseDate }
}

/** 找到与本机类别可比的最新版本，返回 versionCode 文本 + 安装包类型 */
private suspend fun resolveLatest(
    entry: XmsAppEntry,
    deviceType: String,
    detailOf: suspend (pkg: String) -> XmsAppDetail?,
): Pair<String, String>? {
    val compatible = entry.types.filter { xmsTypeMatches(deviceType, it) }
    // 单一类型：总表的 latestVersion 就是它，不用再拉详情
    if (entry.types.size <= 1) {
        val version = entry.latestVersion.takeIf { it.isNotBlank() } ?: return null
        return version to (compatible.firstOrNull() ?: entry.types.firstOrNull().orEmpty())
    }

    // 跨类型：总表版本可能来自别的类别，必须看详情里本机可比分组
    val group = detailOf(entry.pkg)
        ?.groups
        ?.filter { xmsTypeMatches(deviceType, it.type) }
        ?.maxByOrNull { it.appVersion.toLongOrNull() ?: Long.MIN_VALUE }
        ?: return null
    val version = group.appVersion.takeIf { it.isNotBlank() } ?: return null
    return version to group.type
}
