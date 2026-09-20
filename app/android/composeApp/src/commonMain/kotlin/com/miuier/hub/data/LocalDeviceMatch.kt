package com.miuier.hub.data

import com.miuier.hub.platform.LocalDevice

/**
 * 把本机代号和 hub 机型名单对上，对不上返回 null（调用方据此隐藏「本机信息」tab）。
 *
 * 三级匹配，从精确到宽松：
 * 1. **精确**：`Build.DEVICE` / `Build.PRODUCT` 与名单里的 `device` 相等 —— 绝大多数机型在这步命中；
 * 2. **名单更细**：小米给运营商 / 区域变体单独建条目（本机 `cancro`、名单里另有
 *    `cancro_lte_ct`），放宽到「名单代号 = 本机代号 + `_` + 后缀」；
 * 3. **本机更细**：反过来，本机 `Build.PRODUCT` 是带后缀的（`mist_global`）而名单里只有
 *    不带后缀的 `mist`。
 *
 * 刻意不做「包含」式的模糊匹配：`device` 里像 `cmi`、`umi` 这样的短代号一旦模糊匹配，
 * 会把一堆不相关的机型也认成本机。三级都只认「整段 + 下划线」这种明确的变体关系。
 */
fun List<DeviceSummary>.matchLocalDevice(local: LocalDevice): DeviceSummary? {
    val candidates = local.codeNameCandidates.map { it.lowercase() }
    if (candidates.isEmpty()) return null

    // 1) 精确
    firstOrNull { it.device.lowercase() in candidates }?.let { return it }

    // 2) 名单代号 = 本机代号 + 后缀
    firstOrNull { item ->
        val code = item.device.lowercase()
        candidates.any { it.isNotEmpty() && code.startsWith("${it}_") }
    }?.let { return it }

    // 3) 本机代号 = 名单代号 + 后缀
    return firstOrNull { item ->
        val code = item.device.lowercase()
        code.isNotEmpty() && candidates.any { it.startsWith("${code}_") }
    }
}
