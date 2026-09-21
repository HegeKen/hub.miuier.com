package com.miuier.hub.data

/**
 * 支持的语言，顺序与网页端 `app/web/nuxt.config.ts` 的 `i18n.locales` 一致
 * （网页端还有 zh / en 两个只用于兼容旧链接的隐藏别名，App 不需要）。
 */
enum class AppLang(val code: String, val label: String) {
    ZH_HANS("zh-cn", "简体中文"),
    ZH_HANT("zh-tw", "繁體中文"),
    EN("en-us", "English"),
    JA("ja", "日本語"),
    KO("ko", "한국어"),
    RU("ru", "Русский"),
    UK("uk", "Українська"),
    PL("pl", "Polski"),
    DE("de", "Deutsch"),
    FR("fr", "Français"),
    IT("it", "Italiano"),
    ES("es", "Español"),
    PT("pt", "Português"),
    TR("tr", "Türkçe"),
    ID("id", "Bahasa Indonesia"),
    VI("vi", "Tiếng Việt"),
    TH("th", "ไทย"),
    AR("ar", "العربية"),
    HI("hi", "हिन्दी"),
    UG("ug", "ئۇيغۇرچە"),
    BO("bo", "བོད་ཡིག"),
    ;

    /** 阿拉伯语、维吾尔语要从右往左排版 */
    val isRtl: Boolean get() = this == AR || this == UG

    /**
     * v3 数据里的多语言字段**只有 `{zh, en}` 两套**（机型名、分支名），
     * 所以除了简体/繁体中文取中文，其余 19 种语言一律取英文。
     */
    val dataCode: String get() = if (this == ZH_HANS || this == ZH_HANT) "zh" else "en"

    companion object {
        val Default = ZH_HANS

        fun fromCode(code: String?): AppLang =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: Default
    }
}
