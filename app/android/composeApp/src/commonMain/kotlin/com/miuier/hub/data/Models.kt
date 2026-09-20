package com.miuier.hub.data

import kotlinx.serialization.Serializable

/**
 * v3 数据里的多语言字段。
 *
 * 注意：站点（app/web）有 18 种语言，但**数据本身只提供中英两套**
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
