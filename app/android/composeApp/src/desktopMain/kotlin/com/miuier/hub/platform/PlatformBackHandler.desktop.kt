package com.miuier.hub.platform

import androidx.compose.runtime.Composable

/**
 * 桌面端没有系统返回键，这里不做事。
 * 之所以留空而不是省略，是为了让 commonMain 的调用点两端完全一致。
 */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // no-op
}
