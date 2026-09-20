package com.miuier.hub.platform

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberDownloader(): (DownloadRequest) -> Unit {
    val context = LocalContext.current
    // Android 10 以下往公共下载目录写文件需要 WRITE_EXTERNAL_STORAGE；
    // 先记住这次请求，拿到授权后再入队。
    var pending by remember { mutableStateOf<DownloadRequest?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val request = pending
        pending = null
        if (granted && request != null) enqueue(context, request)
    }

    return remember(context, permissionLauncher) {
        { request ->
            if (needsWritePermission(context)) {
                pending = request
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                enqueue(context, request)
            }
        }
    }
}

private fun needsWritePermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
        context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
        PackageManager.PERMISSION_GRANTED

private fun enqueue(context: Context, request: DownloadRequest) {
    // 文件名可能带签名查询串（?t=...&s=...），入队前要去掉，否则会被当成文件名的一部分
    val fileName = request.url.substringAfterLast('/').substringBefore('?').ifBlank { "download.zip" }
    runCatching {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(
            DownloadManager.Request(Uri.parse(request.url))
                .setTitle(request.title)
                .setDescription(fileName)
                .setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED,
                )
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true),
        )
    }
}
