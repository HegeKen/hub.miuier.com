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

    return LocalDevice(
        manufacturer = Build.MANUFACTURER.orEmpty(),
        model = Build.MODEL.orEmpty(),
        marketName = systemProp("ro.product.marketname"),
        product = product,
        codeName = device,
        androidVersion = Build.VERSION.RELEASE.orEmpty(),
        incremental = Build.VERSION.INCREMENTAL.orEmpty(),
        rustVersion = systemProp("rust.runtime_version"),
    )
}

/** 尽力而为地读一个系统属性；隐藏 API 在个别 ROM 上会抛，直接当空值处理 */
private fun systemProp(key: String): String = try {
    Class.forName("android.os.SystemProperties")
        .getMethod("get", String::class.java)
        .invoke(null, key) as? String ?: ""
} catch (_: Throwable) {
    ""
}
