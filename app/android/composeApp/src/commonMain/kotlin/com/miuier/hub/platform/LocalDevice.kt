package com.miuier.hub.platform

/**
 * 本机硬件信息。
 *
 * 字段与 Updater-KMP 的 `platform.DeviceInfo` 一一对应：Android 侧全部来自
 * `android.os.Build` 与 `SystemProperties`（`ro.product.marketname` 这些只读系统属性
 * 没有公开 API，只能反射读）。
 *
 * [codeName] / [product] 是拿去和 hub 机型名单（`/v3/index.json` 的 `device` 字段）
 * 对的键 —— 两者都是小米机型代号，如 `mist`、`cancro`。
 */
data class LocalDevice(
    /** `Build.MANUFACTURER`，如 Xiaomi / Redmi / POCO */
    val manufacturer: String,
    /** `Build.MODEL`，如 `23078PND5G` */
    val model: String,
    /** `ro.product.marketname`，营销名，如 `Xiaomi 14` */
    val marketName: String,
    /** `Build.PRODUCT` */
    val product: String,
    /** `Build.DEVICE`，机型代号 */
    val codeName: String,
    /** `Build.VERSION.RELEASE`，如 `16` */
    val androidVersion: String,
    /** `Build.VERSION.INCREMENTAL`，如 `OS3.0.305.0.WPUEUXM` */
    val incremental: String,
    /** `rust.runtime_version`，HyperOS 3 起才有 */
    val rustVersion: String,
) {
    /** 用来和 hub 机型名单比对的代号候选（有些机型 `Build.DEVICE` 与数据里的代号不同） */
    val codeNameCandidates: List<String>
        get() = listOf(codeName, product).filter { it.isNotBlank() }.distinct()

    /** 界面上优先显示营销名；没有就退回 `Build.MODEL` */
    val displayName: String get() = marketName.ifBlank { model }
}

/**
 * 读本机硬件信息。拿不到就返回 null —— 此时「本机信息」这个 tab 不会出现。
 * 桌面端没有等价物，`actual` 恒为 null。
 */
expect fun currentLocalDevice(): LocalDevice?
