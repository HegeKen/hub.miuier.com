package com.miuier.hub.data

import kotlinx.serialization.Serializable

/**
 * v3 数据里的多语言字段。
 *
 * 注意：站点（app/web）有 21 种语言，但**数据本身只提供中英两套**
 * （机型名、分支名都是 `{zh, en}`），所以 App 只做中/英切换。
 */
@Serializable
data class I18nText(val zh: String = "", val en: String = "") {
    fun pick(lang: String): String =
        (if (lang == "zh") zh.ifBlank { en } else en.ifBlank { zh }).ifBlank { "—" }
}

@Serializable
data class SeriesRef(val brand: String = "", val zh: String = "", val en: String = "") {
    fun pick(lang: String): String = if (lang == "zh") zh.ifBlank { en } else en.ifBlank { zh }
}

/** GET /v3/index.json —— 机型总表 */
@Serializable
data class DeviceSummary(
    val device: String = "",
    val name: I18nText = I18nText(),
    val brand: List<String> = emptyList(),
    val series: List<SeriesRef> = emptyList(),
    val code: String = "",
    val android: List<String> = emptyList(),
    val supports: List<String> = emptyList(),
    val regions: List<String> = emptyList(),
    val carriers: List<String> = emptyList(),
    val regionCarriers: Map<String, List<String>> = emptyMap(),
    val branchCount: Int = 0,
    val romCount: Int = 0,
)

/** GET /v3/devices/{code}.json —— 机型详情（含全部分支与 ROM） */
@Serializable
data class DeviceDetail(
    val device: String = "",
    val name: I18nText = I18nText(),
    val code: String = "",
    val brand: List<String> = emptyList(),
    val series: List<SeriesRef> = emptyList(),
    val android: List<String> = emptyList(),
    val supports: List<String> = emptyList(),
    val branches: List<Branch> = emptyList(),
)

/** 一个分支 = 某个区域下的一条版本线（如「中国大陆正式版」） */
@Serializable
data class Branch(
    val id: String = "",
    val brand: List<String> = emptyList(),
    val device: I18nText = I18nText(),
    val name: I18nText = I18nText(),
    val region: String = "",
    val carrier: List<String> = emptyList(),
    val tags: BranchTags = BranchTags(),
    val zone: String = "1",
    val show: String = "1",
    val ep: String = "0",
    val roms: List<Rom> = emptyList(),
)

@Serializable
data class BranchTags(
    val branch: String = "",
    val tag: String = "",
    val branchtag: String = "",
    val btag: String = "",
)

/** 分支下的一条 ROM */
@Serializable
data class Rom(
    val miui: String = "",
    val os: String = "",
    val bigver: String = "",
    val android: String = "",
    val release: String = "",
    val aspatch: String = "",
    val recovery: String = "",
    val fastboot: String = "",
    val ctelecom: String = "",
    val cunicom: String = "",
) {
    /** 卡刷包 / 线刷包是否可用（数据里为空串表示没有） */
    val hasRecovery: Boolean get() = recovery.isNotBlank()
    val hasFastboot: Boolean get() = fastboot.isNotBlank()
}

/** GET /v3/roms/index.json —— 按系统大版本汇总 */
@Serializable
data class OsIndex(val os: String = "", val count: Int = 0, val deviceCount: Int = 0)

/** GET /v3/roms/{OS}.json —— 某个大版本下的全部 ROM */
@Serializable
data class OsRom(
    val os: String = "",
    val bigver: String = "",
    val device: String = "",
    val name: I18nText = I18nText(),
    val brand: List<String> = emptyList(),
    val version: String = "",
    val android: String = "",
    val region: String = "",
    val zone: String = "1",
    val branchName: I18nText = I18nText(),
    val release: String = "",
    val aspatch: String = "",
    val recovery: String = "",
    val fastboot: String = "",
)

/** GET /v3/stats.json —— 首页统计 */
@Serializable
data class HubStats(
    val generatedAt: String = "",
    val recentDays: Int = 7,
    val since: String = "",
    val recentRoms: Int = 0,
    val recent: List<RecentRom> = emptyList(),
)

@Serializable
data class RecentRom(
    val device: String = "",
    val name: I18nText = I18nText(),
    val brand: List<String> = emptyList(),
    val version: String = "",
    val android: String = "",
    val release: String = "",
    val region: String = "",
    val branchName: I18nText = I18nText(),
)

/**
 * GET /v3/logs/{device}/{region}/{version}.json
 *
 * 结构是三层：语种 → 模块 → 逐行文案，例如
 * `{"logs_zh": {"系统应用全面优化": ["小米超级岛：", "新增 …"]}, "logs_en": {...}}`
 * 接口一共导出 18 个语种，但 App 只用得上中英两套。
 */
typealias ChangelogFile = Map<String, Map<String, List<String>>>

/** 界面直接消费的形态：已经按当前语言挑好并转成有序列表 */
data class ChangelogModule(val title: String, val lines: List<String>)

/** 从多语种日志里挑出当前语言；缺失时依次回落英文、简体中文 */
fun pickChangelog(file: ChangelogFile, lang: String): List<ChangelogModule> {
    val preferred = if (lang == "zh") "logs_zh" else "logs_en"
    val table = file[preferred]
        ?: file["logs_en"]
        ?: file["logs_zh"]
        ?: return emptyList()
    return table.map { (title, lines) -> ChangelogModule(title, lines) }
}

// —— XMS（系统应用更新 / SOTA）——

/**
 * 一条 XMS 更新日志。接口导出的是「语言 → 文案」的对象，
 * 例如 `{"zh": "新增 …", "en": "Added …"}`，语言键不止 zh / en。
 */
typealias XmsLog = Map<String, String>

/** 按当前语言取一条 XMS 日志；缺失时依次回落英文、中文 */
fun pickXmsLog(log: XmsLog, lang: String): String {
    val preferred = if (lang == "zh") "zh" else "en"
    return log[preferred].orEmpty()
        .ifBlank { log["en"].orEmpty() }
        .ifBlank { log["zh"].orEmpty() }
}

/** GET /v3/xms/index.json —— 应用更新总表 */
@Serializable
data class XmsIndex(
    val generatedAt: String = "",
    val counts: XmsCounts = XmsCounts(),
    val versions: List<XmsVersion> = emptyList(),
    val devices: List<XmsDeviceEntry> = emptyList(),
    val apps: List<XmsAppEntry> = emptyList(),
)

@Serializable
data class XmsCounts(
    val pushes: Int = 0,
    val apps: Int = 0,
    val devices: Int = 0,
    val versions: Int = 0,
)

/** 一次 XMS 推送（如 C11 对应 OS3） */
@Serializable
data class XmsVersion(
    val version: String = "",
    val os: String = "",
    val pushDate: String = "",
    val devices: Int = 0,
    val apps: Int = 0,
)

/** 总表里的机型条目 */
@Serializable
data class XmsDeviceEntry(
    val device: String = "",
    val name: I18nText = I18nText(),
    val code: String = "",
    val region: String = "",
    val brands: List<String> = emptyList(),
    val tablet: Boolean = false,
    val pushCount: Int = 0,
    val latest: XmsDeviceLatest? = null,
)

@Serializable
data class XmsDeviceLatest(
    val version: String = "",
    val os: String = "",
    val romVersion: String = "",
    val pushDate: String = "",
    val isLatest: Boolean = false,
    val appCount: Int = 0,
)

/** 总表里的应用条目 */
@Serializable
data class XmsAppEntry(
    val pkg: String = "",
    val name: I18nText = I18nText(),
    val apkName: String = "",
    val latestVersion: String = "",
    val releaseDate: String = "",
    val recordCount: Int = 0,
    val deviceCount: Int = 0,
    val os: List<String> = emptyList(),
    val regions: List<String> = emptyList(),
    val brands: List<String> = emptyList(),
    val tablet: Boolean = false,
    val types: List<String> = emptyList(),
    val path: String = "",
)

/** GET /v3/xms/apps/{pkg}.json —— 单个应用的推送历史 */
@Serializable
data class XmsAppDetail(
    val pkg: String = "",
    val name: I18nText = I18nText(),
    val apkName: String = "",
    val releaseDate: String = "",
    val latestVersion: String = "",
    val recordCount: Int = 0,
    val deviceCount: Int = 0,
    val os: List<String> = emptyList(),
    val regions: List<String> = emptyList(),
    val brands: List<String> = emptyList(),
    val tablet: Boolean = false,
    val types: List<String> = emptyList(),
    val groups: List<XmsGroup> = emptyList(),
)

/** 同一「XMS 版本 + 应用版本」的一批推送：版本 / 安装包 / 日志只出现一次，下面列机型 */
@Serializable
data class XmsGroup(
    val version: String = "",
    val os: String = "",
    val appVersion: String = "",
    val type: String = "",
    val pushDate: String = "",
    val isLatest: Boolean = false,
    val apkName: String = "",
    val file: String = "",
    val releaseDate: String = "",
    val size: Long = 0,
    val md5: String = "",
    val sha256: String = "",
    val logs: List<XmsLog> = emptyList(),
    val recordCount: Int = 0,
    val devices: List<XmsGroupDevice> = emptyList(),
)

@Serializable
data class XmsGroupDevice(
    val device: String = "",
    val name: I18nText = I18nText(),
    val code: String = "",
    val region: String = "",
    val zone: Int = 1,
    val android: String = "",
    val pushDate: String = "",
    val isLatest: Boolean = false,
    val romVersions: List<String> = emptyList(),
)

/** GET /v3/xms/devices/{device}.json —— 单个机型的推送历史 */
@Serializable
data class XmsDeviceDetail(
    val device: String = "",
    val name: I18nText = I18nText(),
    val code: String = "",
    val region: String = "",
    val pushes: List<XmsPush> = emptyList(),
)

@Serializable
data class XmsPush(
    val version: String = "",
    val os: String = "",
    val android: String = "",
    val pushDate: String = "",
    val isLatest: Boolean = false,
    val pkgCnt: Int = 0,
    val prio: Int = 0,
    val notice: String = "",
    val romVersions: List<String> = emptyList(),
    val extra: List<XmsExtra> = emptyList(),
    val apps: List<XmsPushApp> = emptyList(),
)

/** 没有包名的日志模块（如「小米超级岛」），只展示模块名与日志 */
@Serializable
data class XmsExtra(
    val name: I18nText = I18nText(),
    val logs: List<XmsLog> = emptyList(),
)

@Serializable
data class XmsPushApp(
    val pkg: String = "",
    val name: I18nText = I18nText(),
    val version: String = "",
    val type: String = "",
    val file: String = "",
    val size: Long = 0,
    val releaseDate: String = "",
    val logs: List<XmsLog> = emptyList(),
)
