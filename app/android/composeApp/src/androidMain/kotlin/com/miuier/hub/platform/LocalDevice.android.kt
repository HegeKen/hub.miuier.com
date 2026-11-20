package com.miuier.hub.platform

import android.os.Build

/**
 * 本机硬件信息（Android）。
 *
 * `ro.product.marketname` / `rust.runtime_version` 是小米加的系统属性，没有公开 API，
 * 只能反射 `android.os.SystemProperties`（隐藏类，读不到就退化成空串，不影响其他字段）。
 * 这套读法与 Updater-KMP 的 `platform/Device.android.kt` 一致。
 */
actual fun currentLocalDevice(): LocalDevice? {
    val device = Build.DEVICE.orEmpty()
    val product = Build.PRODUCT.orEmpty()
    if (device.isBlank() && product.isBlank()) return null
    val marketName = systemProp("ro.product.marketname")

    return LocalDevice(
        manufacturer = Build.MANUFACTURER.orEmpty(),
        model = Build.MODEL.orEmpty(),
        marketName = marketName,
        product = product,
        codeName = device,
        androidVersion = Build.VERSION.RELEASE.orEmpty(),
        incremental = Build.VERSION.INCREMENTAL.orEmpty(),
        rustVersion = systemProp("rust.runtime_version"),
        deviceType = detectDeviceType(marketName),
    )
}

/** 折叠屏 / 平板在营销名里的特征词（英文按整词匹配，避免代号里的字母碰巧命中） */
private val FOLD_NAME = Regex("""\b(fold|flip)\b""", RegexOption.IGNORE_CASE)
private val PAD_NAME = Regex("""\b(pad|tablet)\b""", RegexOption.IGNORE_CASE)

/**
 * 判断本机类别，供 XMS 应用更新只在同类机型之间比对。
 *
 * `ro.build.characteristics` 里平板会带 `tablet`；折叠屏在该属性上并不可靠，
 * 所以再看营销名里的 Fold / Flip。两者都拿不到时按手机处理。
 */
private fun detectDeviceType(marketName: String): String {
    val characteristics = systemProp("ro.build.characteristics").lowercase()
    val name = listOf(marketName, Build.MODEL.orEmpty()).joinToString(" ")
    return when {
        characteristics.contains("foldable") ||
            FOLD_NAME.containsMatchIn(name) ||
            name.contains("折叠") || name.contains("摺疊") -> LocalDevice.TYPE_FOLD

        characteristics.contains("tablet") ||
            PAD_NAME.containsMatchIn(name) ||
            name.contains("平板") -> LocalDevice.TYPE_PAD

        else -> LocalDevice.TYPE_PHONE
    }
}

/** 尽力而为地读一个系统属性；隐藏 API 在个别 ROM 上会抛，直接当空值处理 */
private fun systemProp(key: String): String = try {
    Class.forName("android.os.SystemProperties")
        .getMethod("get", String::class.java)
        .invoke(null, key) as? String ?: ""
} catch (_: Throwable) {
    ""
}
