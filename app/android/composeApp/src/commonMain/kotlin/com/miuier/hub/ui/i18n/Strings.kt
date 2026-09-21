package com.miuier.hub.ui.i18n

/**
 * 一条语言的全部界面词条。
 *
 * 用 data class 而不是 `Map<String, String>`：漏翻译某个词条会直接编译不过，
 * 而不是等到界面上出现一串裸 key 才发现。
 *
 * 带占位符的词条写成 `{count}` / `{time}` / `{latest}`，渲染时用 [fill] 替换。
 * 具体每种语言的取值由 `scripts/make_locales.py` 生成到 `Locales.kt`。
 */
data class Strings(
    // —— 导航与通用 ——
    val home: String,
    val devices: String,
    val roms: String,
    val settings: String,
    val back: String,
    val backToTop: String,
    val all: String,

    // —— 搜索与列表 ——
    val searchDevice: String,
    val searchRom: String,
    val deviceCount: String,
    val entryCount: String,
    val romSummary: String,
    val romCountOnly: String,

    // —— 机型信息 ——
    val deviceCode: String,
    val brand: String,
    val series: String,
    val supports: String,
    val enterprise: String,
    val stable: String,
    val dev: String,

    // —— ROM 与下载 ——
    val download: String,
    val recovery: String,
    val fastboot: String,
    val hasRecovery: String,
    val noRecovery: String,
    val hasFastboot: String,
    val noFastboot: String,
    val noPackage: String,
    val noPatch: String,
    val copyLink: String,
    val expand: String,
    val collapse: String,

    // —— 更新日志 ——
    val changelog: String,
    val loading: String,
    val loadFailed: String,
    val retry: String,
    val changelogFailed: String,
    val noChangelog: String,

    // —— 高速下载 ——
    val highSpeed: String,
    val getHighSpeed: String,
    val fetching: String,
    val requestFailed: String,
    val otaOutdated: String,
    val otaUnsigned: String,
    val otaNotFound: String,

    // —— 分支线刷包查询 ——
    val getFastboot: String,
    val fastbootNotFound: String,
    /** 分支未指定运营商时那条 `n=` 留空的包 */
    val generic: String,

    // —— 首页 ——
    val recentUpdates: String,
    val updatedInDays: String,
    val romVersionsSuffix: String,
    val dataUpdatedAt: String,

    // —— 下拉刷新（MIUIX PullToRefresh 要一个 4 元文案列表）——
    val refreshPull: String,
    val refreshRelease: String,
    val refreshRefreshing: String,
    val refreshDone: String,

    // —— 本机信息（只在本机机型出现在 hub 名单里时才用得上）——
    val myDevice: String,
    val thisDevice: String,
    val systemVersion: String,
    val androidVersion: String,
    val runtimeVersion: String,
    val myDeviceFound: String,
    val myDeviceSummary: String,

    // —— 设置 ——
    val appearance: String,
    val themeSystem: String,
    val themeLight: String,
    val themeDark: String,
    val language: String,
    val about: String,
    val project: String,
    val builtWith: String,
    val authorSite: String,
    val feedback: String,
    val aboutSource: String,
    val appDisclaimer: String,
)

/** 把 `{count}` 这类占位符替换掉 */
fun String.fill(vararg values: Pair<String, Any>): String {
    var result = this
    for ((key, value) in values) {
        result = result.replace("{$key}", value.toString())
    }
    return result
}

/**
 * 界面取词条的统一入口，一次把三样东西带下去：
 * - [strings]  界面文案
 * - [locale]   区域名 / 运营商名（来自网页端语言包）
 * - [dataLang] v3 数据里的多语言字段只有 zh / en 两套，机型名要用它来取
 *
 * 打包成一个对象传递，省得每个 composable 都挂三个参数。
 */
data class UiText(
    val strings: Strings,
    val locale: LocaleData,
    val dataLang: String,
)
