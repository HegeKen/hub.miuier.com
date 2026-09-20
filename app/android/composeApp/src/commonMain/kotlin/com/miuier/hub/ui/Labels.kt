package com.miuier.hub.ui

import com.miuier.hub.data.BranchTags
import com.miuier.hub.ui.i18n.LocaleData
import com.miuier.hub.ui.i18n.Strings

/**
 * 区域 / 运营商显示名。
 *
 * 词条不写在这里：全部 18 种语言的区域名与运营商名都由 `scripts/make_locales.py`
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
