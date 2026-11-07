package com.miuier.hub.platform

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberInstalledApps(): Map<String, InstalledApp> {
    val context = LocalContext.current
    return remember(context) { readInstalledApps(context) }
}

/**
 * 枚举本机全部已安装应用。
 *
 * Android 11 起默认只能看到少量「可见」应用，需要在 Manifest 里声明
 * `QUERY_ALL_PACKAGES`（见 AndroidManifest.xml），否则大部分系统应用会被过滤掉。
 */
private fun readInstalledApps(context: Context): Map<String, InstalledApp> {
    val manager = context.packageManager
    val packages = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            manager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            manager.getInstalledPackages(0)
        }
    }.getOrNull() ?: return emptyMap()

    val result = LinkedHashMap<String, InstalledApp>(packages.size)
    for (info in packages) {
        @Suppress("DEPRECATION")
        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            info.versionCode.toLong()
        }
        result[info.packageName] = InstalledApp(
            versionName = info.versionName.orEmpty(),
            versionCode = code,
        )
    }
    return result
}
