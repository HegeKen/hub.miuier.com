package com.miuier.hub.ui

import com.miuier.hub.data.BranchTags
import com.miuier.hub.ui.i18n.LocaleData
import com.miuier.hub.ui.i18n.Strings

/**
 * 区域 / 运营商显示名。
 *
 * 词条不写在这里：全部 21 种语言的区域名与运营商名都由 `scripts/make_locales.py`
 * 从网页端语言包（app/web/i18n/locales 下的 ts 文件）取来，生成到 `ui/i18n/Locales.kt`，
 * 与网页端的译法保持一致。
 * 未收录的代号按网页端的做法回退：区域回退为大写代号，运营商回退为原代号。
 */
fun regionLabel(code: String, locale: LocaleData): String =
    if (code.isBlank()) "" else locale.regions[code] ?: code.uppercase()

fun carrierLabel(code: String, locale: LocaleData): String =
    if (code.isBlank()) "" else locale.carriers[code] ?: code

/** 版本号里区分正式版 / 开发版 */
fun branchKindLabel(tags: BranchTags, strings: Strings): String {
    val isDev = tags.branchtag.equals("X", ignoreCase = true) || tags.btag.equals("X", ignoreCase = true)
    return if (isDev) strings.dev else strings.stable
}

/** XMS 应用类型（数据里是 phone / pad / fold / universal） */
fun xmsTypeLabel(type: String, strings: Strings): String = when (type.lowercase()) {
    "phone" -> strings.xmsTypePhone
    "pad" -> strings.xmsTypePad
    "fold" -> strings.xmsTypeFold
    "universal" -> strings.xmsTypeUniversal
    else -> ""
}

/** 字节数 → 人类可读体积，规则与网页端 `utils/xms.ts` 的 formatBytes 一致 */
fun formatBytes(size: Long): String {
    if (size <= 0) return ""
    val units = listOf("B", "KB", "MB", "GB")
    var value = size.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit += 1
    }
    val number = if (value >= 100 || unit == 0) {
        value.toLong().toString()
    } else {
        // 保留一位小数，避免依赖各平台不同的 String.format
        val tenths = (value * 10).toLong()
        "${tenths / 10}.${tenths % 10}"
    }
    return "$number ${units[unit]}"
}

/**
 * 从 APK 文件名里提取更适合展示的版本号，规则与网页端一致：
 * `cleanmaster-1.0.3.8_260629_cn-release-universal-2d1f13b229.apk` → `1.0.3.8_260629`，
 * 没有日期段时退回纯数字版本。
 */
fun xmsFileVersion(file: String, fallback: String): String {
    if (file.isBlank()) return fallback
    Regex("(\\d+(?:\\.\\d+)+_\\d+)").find(file)?.let { return it.value }
    Regex("(\\d+(?:\\.\\d+)+)").find(file)?.let { return it.value }
    return fallback
}
