package com.miuier.hub.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.miuier.hub.HubAppState
import com.miuier.hub.Screen
import com.miuier.hub.data.HubRepository
import com.miuier.hub.data.I18nText
import com.miuier.hub.data.XmsAppEntry
import com.miuier.hub.data.XmsDeviceEntry
import com.miuier.hub.data.XmsDownloadRequest
import com.miuier.hub.data.XmsGroup
import com.miuier.hub.data.XmsLog
import com.miuier.hub.data.XmsPush
import com.miuier.hub.data.XmsStatus
import com.miuier.hub.data.pickXmsLog
import com.miuier.hub.platform.DownloadRequest
import com.miuier.hub.platform.rememberDownloader
import com.miuier.hub.ui.components.AsyncContent
import com.miuier.hub.ui.components.Tag
import com.miuier.hub.ui.components.rememberAsync
import com.miuier.hub.ui.components.rememberScreenListState
import com.miuier.hub.ui.formatBytes
import com.miuier.hub.ui.i18n.UiText
import com.miuier.hub.ui.i18n.fill
import com.miuier.hub.ui.regionLabel
import com.miuier.hub.ui.xmsFileVersion
import com.miuier.hub.ui.xmsTypeLabel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 应用更新（XMS / SOTA）首页：按机型 / 按应用两个入口。
 *
 * 数据来自 `v3/xms/index.json`。它只是一张总表，详情（推送明细、安装包、日志）
 * 在各自的 `apps/{pkg}.json` 与 `devices/{device}.json` 里，点进去才拉。
 */
@Composable
fun XmsScreen(app: HubAppState, contentPadding: PaddingValues, scrollBehavior: ScrollBehavior) {
    val l10n = app.ui
    val listState = rememberScreenListState(app)
    val holder = rememberAsync("xmsIndex", onRefresh = app.repo::invalidate) { app.repo.xmsIndex() }

    // 搜索/页签状态放在 AsyncContent 之外，下拉刷新不会把用户的输入清掉
    var query by remember { mutableStateOf("") }
    var tab by remember { mutableIntStateOf(0) }

    AsyncContent(holder, contentPadding, l10n.strings, scrollBehavior) { index ->
        val q = query.trim().lowercase()
        val devices = remember(index, q) {
            index.devices.filter { entry ->
                q.isBlank() ||
                    entry.device.lowercase().contains(q) ||
                    entry.name.zh.lowercase().contains(q) ||
                    entry.name.en.lowercase().contains(q)
            }
        }
        val apps = remember(index, q) {
            index.apps.filter { entry ->
                q.isBlank() ||
                    entry.pkg.lowercase().contains(q) ||
                    entry.name.zh.lowercase().contains(q) ||
                    entry.name.en.lowercase().contains(q)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .nestedScroll(app.scrollToTop.nestedScrollConnection),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    text = l10n.strings.xmsSub,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                )
            }

            item {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    label = l10n.strings.xmsSearchPlaceholder,
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                TabRow(
                    tabs = listOf(l10n.strings.xmsByDevice, l10n.strings.xmsByApp),
                    selectedTabIndex = tab,
                    onTabSelected = { tab = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                val summary = if (tab == 0) {
                    l10n.strings.totalDevices.fill("count" to devices.size)
                } else {
                    l10n.strings.xmsApps.fill("count" to apps.size)
                }
                Text(
                    text = summary,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                )
            }

            if (tab == 0) {
                if (devices.isEmpty()) item { EmptyHint(l10n.strings.xmsEmpty) }
                items(devices, key = { "device:${it.device}" }) { entry ->
                    XmsDeviceRow(entry, l10n) {
                        app.push(Screen.XmsDevice(entry.device, nameOf(entry.name, entry.device, l10n)))
                    }
                }
            } else {
                if (apps.isEmpty()) item { EmptyHint(l10n.strings.xmsEmpty) }
                items(apps, key = { "app:${it.pkg}" }) { entry ->
                    XmsAppRow(entry, l10n) {
                        app.push(Screen.XmsAppDetail(entry.pkg, nameOf(entry.name, entry.pkg, l10n)))
                    }
                }
            }
        }
    }
}

/** 单个应用的推送历史：按「XMS 版本 + 应用版本」归组，安装包 / 日志只展示一次 */
@Composable
fun XmsAppDetailScreen(
    app: HubAppState,
    pkg: String,
    contentPadding: PaddingValues,
    scrollBehavior: ScrollBehavior,
) {
    val l10n = app.ui
    val listState = rememberScreenListState(app)
    val holder = rememberAsync("xms-app", pkg, onRefresh = app.repo::invalidate) { app.repo.xmsApp(pkg) }

    AsyncContent(holder, contentPadding, l10n.strings, scrollBehavior) { detail ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .nestedScroll(app.scrollToTop.nestedScrollConnection),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Card(onClick = null, insideMargin = PaddingValues(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            text = nameOf(detail.name, detail.pkg, l10n),
                            style = MiuixTheme.textStyles.title3,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = detail.pkg,
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                        if (detail.apkName.isNotBlank()) {
                            Text(
                                text = detail.apkName,
                                style = MiuixTheme.textStyles.footnote2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = l10n.strings.totalDevices.fill("count" to detail.deviceCount) +
                                " · " + l10n.strings.xmsRecords + " " + detail.recordCount,
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }

            if (detail.groups.isEmpty()) {
                item { EmptyHint(l10n.strings.xmsEmpty) }
            }

            itemsIndexed(detail.groups, key = { _, group -> "${group.version}|${group.appVersion}" }) { _, group ->
                XmsGroupCard(app, pkg, group, l10n)
            }
        }
    }
}

/** 单个机型的推送历史：默认展开最新一条，历史推送折叠 */
@Composable
fun XmsDeviceScreen(
    app: HubAppState,
    device: String,
    contentPadding: PaddingValues,
    scrollBehavior: ScrollBehavior,
) {
    val l10n = app.ui
    val listState = rememberScreenListState(app)
    val holder = rememberAsync("xms-device", device, onRefresh = app.repo::invalidate) {
        app.repo.xmsDevice(device)
    }

    AsyncContent(holder, contentPadding, l10n.strings, scrollBehavior) { detail ->
        // 下标集合：初始只展开第一条（最新的那次推送）
        val expanded = remember(detail) { mutableStateListOf(0) }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .nestedScroll(app.scrollToTop.nestedScrollConnection),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Card(onClick = null, insideMargin = PaddingValues(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            text = nameOf(detail.name, detail.device, l10n),
                            style = MiuixTheme.textStyles.title3,
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = detail.device,
                                style = MiuixTheme.textStyles.footnote2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                            if (detail.region.isNotBlank()) {
                                Spacer(Modifier.width(8.dp))
                                Tag(regionLabel(detail.region, l10n.locale))
                            }
                        }
                    }
                }
            }

            if (detail.pushes.isEmpty()) {
                item { EmptyHint(l10n.strings.xmsEmpty) }
            }

            itemsIndexed(detail.pushes, key = { index, push -> "${push.version}-$index" }) { index, push ->
                XmsPushCard(
                    push = push,
                    expanded = expanded.contains(index),
                    onToggle = {
                        if (expanded.contains(index)) expanded.remove(index) else expanded.add(index)
                    },
                    l10n = l10n,
                    onOpenApp = { pkg, name ->
                        app.push(Screen.XmsAppDetail(pkg, name.ifBlank { pkg }))
                    },
                )
            }
        }
    }
}

// —— 列表项 ——

@Composable
private fun XmsDeviceRow(entry: XmsDeviceEntry, l10n: UiText, onClick: () -> Unit) {
    Card(onClick = onClick, insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = nameOf(entry.name, entry.device, l10n),
                    style = MiuixTheme.textStyles.title4,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.device,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    if (entry.region.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Tag(regionLabel(entry.region, l10n.locale))
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                entry.latest?.let { latest ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Tag(latest.version)
                        if (latest.os.isNotBlank()) {
                            Spacer(Modifier.width(4.dp))
                            Tag(latest.os)
                        }
                    }
                }
                Text(
                    text = l10n.strings.xmsApps.fill("count" to (entry.latest?.appCount ?: 0)),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = MiuixIcons.Demibold.ChevronForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun XmsAppRow(entry: XmsAppEntry, l10n: UiText, onClick: () -> Unit) {
    Card(onClick = onClick, insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = nameOf(entry.name, entry.pkg, l10n),
                    style = MiuixTheme.textStyles.title4,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = entry.pkg,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (entry.types.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        entry.types.forEach { type ->
                            val label = xmsTypeLabel(type, l10n.strings)
                            if (label.isNotBlank()) Tag(label)
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (entry.latestVersion.isNotBlank()) Tag(entry.latestVersion)
                Text(
                    text = l10n.strings.totalDevices.fill("count" to entry.deviceCount),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = MiuixIcons.Demibold.ChevronForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        }
    }
}

// —— 应用详情里的版本卡 ——

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun XmsGroupCard(app: HubAppState, pkg: String, group: XmsGroup, l10n: UiText) {
    Card(onClick = null, insideMargin = PaddingValues(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (group.version.isNotBlank()) Tag(group.version)
                if (group.os.isNotBlank()) Tag(group.os)
                val typeLabel = xmsTypeLabel(group.type, l10n.strings)
                if (typeLabel.isNotBlank()) Tag(typeLabel)
                if (group.isLatest) {
                    Tag(
                        text = l10n.strings.xmsLatest,
                        container = MiuixTheme.colorScheme.primaryContainer,
                        content = MiuixTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = l10n.strings.xmsAppVersion,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = group.appVersion.ifBlank { "—" },
                    style = MiuixTheme.textStyles.body1,
                )
            }

            if (group.file.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Tag("APK")
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = xmsFileVersion(group.file, group.appVersion),
                            style = MiuixTheme.textStyles.footnote1,
                        )
                        val meta = listOfNotNull(
                            formatBytes(group.size).ifBlank { null },
                            group.releaseDate.ifBlank { null }?.let { l10n.strings.release + " " + it },
                        ).joinToString(" · ")
                        if (meta.isNotBlank()) {
                            Text(
                                text = meta,
                                style = MiuixTheme.textStyles.footnote2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                    XmsDownloadButton(app.repo, pkg, group, l10n)
                }
                if (group.md5.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "MD5: ${group.md5}",
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            if (group.logs.isEmpty()) {
                Text(
                    text = l10n.strings.noChangelog,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            } else {
                XmsLogList(group.logs, l10n.dataLang)
            }

            if (group.devices.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = l10n.strings.totalDevices.fill("count" to group.devices.size),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Spacer(Modifier.height(4.dp))
                group.devices.forEach { device ->
                    XmsDeviceLinkRow(
                        name = nameOf(device.name, device.device, l10n),
                        code = device.device,
                        isLatest = device.isLatest,
                        romVersions = device.romVersions,
                        trailing = device.pushDate,
                        onClick = { app.push(Screen.XmsDevice(device.device, nameOf(device.name, device.device, l10n))) },
                    )
                }
            }
        }
    }
}

/**
 * 换取签名下载地址。链接与「机型 + 系统版本」绑定，所以先按网页端的规则挑一台设备：
 * 最新且带系统版本 → 带系统版本 → 第一台。
 */
@Composable
private fun XmsDownloadButton(repo: HubRepository, pkg: String, group: XmsGroup, l10n: UiText) {
    val target = remember(group) {
        group.devices.firstOrNull { it.isLatest && it.romVersions.isNotEmpty() }
            ?: group.devices.firstOrNull { it.romVersions.isNotEmpty() }
            ?: group.devices.firstOrNull()
    }
    if (target == null) return

    val scope = rememberCoroutineScope()
    val download = rememberDownloader()
    var loading by remember(group) { mutableStateOf(false) }
    var error by remember(group) { mutableStateOf("") }

    Column(horizontalAlignment = Alignment.End) {
        IconButton(
            onClick = {
                if (loading) return@IconButton
                loading = true
                error = ""
                scope.launch {
                    val result = runCatching {
                        repo.xmsDownload(
                            XmsDownloadRequest(
                                pkg = pkg,
                                code = target.code.ifBlank { target.device },
                                region = target.region.ifBlank { "cn" },
                                android = target.android,
                                romVersion = target.romVersions.firstOrNull().orEmpty(),
                                xmsVersion = group.version,
                            ),
                        )
                    }.getOrNull()

                    loading = false
                    when {
                        result == null -> error = l10n.strings.requestFailed
                        result.status == XmsStatus.AVAILABLE && result.urls.isNotEmpty() ->
                            download(DownloadRequest(result.urls.first(), result.fileName.ifBlank { pkg }))

                        result.status == XmsStatus.UNSIGNED -> error = l10n.strings.otaUnsigned
                        result.status == XmsStatus.NOTFOUND -> error = l10n.strings.otaNotFound
                        else -> error = l10n.strings.xmsDownloadFailed
                    }
                }
            },
        ) {
            Icon(
                imageVector = MiuixIcons.Demibold.Download,
                contentDescription = l10n.strings.download,
                modifier = Modifier.size(20.dp),
                tint = if (loading) {
                    MiuixTheme.colorScheme.onSurfaceVariantSummary
                } else {
                    MiuixTheme.colorScheme.primary
                },
            )
        }
        if (error.isNotBlank()) {
            Text(
                text = error,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.error,
            )
        }
    }
}

// —— 机型详情里的推送卡 ——

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun XmsPushCard(
    push: XmsPush,
    expanded: Boolean,
    onToggle: () -> Unit,
    l10n: UiText,
    onOpenApp: (pkg: String, name: String) -> Unit,
) {
    Card(onClick = null, insideMargin = PaddingValues(0.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Tag(push.version)
                        if (push.os.isNotBlank()) Tag(push.os)
                        if (push.isLatest) {
                            Tag(
                                text = l10n.strings.xmsLatest,
                                container = MiuixTheme.colorScheme.primaryContainer,
                                content = MiuixTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = l10n.strings.xmsPushDate + " " + push.pushDate + " · " +
                            l10n.strings.xmsApps.fill("count" to push.pkgCnt),
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    if (push.romVersions.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            push.romVersions.forEach { Tag(it) }
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = MiuixIcons.Demibold.ChevronForward,
                    contentDescription = if (expanded) l10n.strings.collapse else l10n.strings.expand,
                    modifier = Modifier.size(16.dp).rotate(if (expanded) -90f else 90f),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }

            if (expanded) {
                Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
                    if (push.notice.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = l10n.strings.attentions,
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                        Spacer(Modifier.height(2.dp))
                        // notice 本身已经是「· 一行一条」的排版，直接按行铺开，不要再补前缀
                        push.notice.split('\n').filter { it.isNotBlank() }.forEach { line ->
                            Text(
                                text = line,
                                style = MiuixTheme.textStyles.footnote2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    push.apps.forEach { app ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = nameOf(app.name, app.pkg, l10n),
                                        style = MiuixTheme.textStyles.body1,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (app.pkg.isNotBlank()) {
                                        Text(
                                            text = app.pkg,
                                            style = MiuixTheme.textStyles.footnote2,
                                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                val typeLabel = xmsTypeLabel(app.type, l10n.strings)
                                if (typeLabel.isNotBlank()) Tag(typeLabel)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = app.version.ifBlank { "—" },
                                    style = MiuixTheme.textStyles.footnote1,
                                )
                            }
                            if (app.logs.isNotEmpty()) {
                                Spacer(Modifier.height(2.dp))
                                XmsLogList(app.logs, l10n.dataLang)
                            }
                            if (app.file.isNotBlank()) {
                                Text(
                                    text = "APK · " + xmsFileVersion(app.file, app.version),
                                    style = MiuixTheme.textStyles.footnote2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onOpenApp(app.pkg, nameOf(app.name, app.pkg, l10n)) }
                                        .padding(vertical = 4.dp),
                                )
                            }
                        }
                    }

                    push.extra.forEach { extra ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text(
                                text = extra.name.pick(l10n.dataLang),
                                style = MiuixTheme.textStyles.body1,
                            )
                            if (extra.logs.isNotEmpty()) {
                                Spacer(Modifier.height(2.dp))
                                XmsLogList(extra.logs, l10n.dataLang)
                            }
                        }
                    }
                }
            }
        }
    }
}

// —— 通用小件 ——

@Composable
private fun XmsDeviceLinkRow(
    name: String,
    code: String,
    isLatest: Boolean,
    romVersions: List<String>,
    trailing: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isLatest) {
                    Spacer(Modifier.width(4.dp))
                    Tag("★")
                }
            }
            Text(
                text = code,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        if (romVersions.isNotEmpty()) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = romVersions.joinToString(" / "),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = trailing,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

/** 多语言日志：每行取当前语言，缺失回落 en / zh */
@Composable
private fun XmsLogList(logs: List<XmsLog>, lang: String) {
    val lines = remember(logs, lang) {
        logs.map { pickXmsLog(it, lang) }.filter { it.isNotBlank() }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        lines.forEach { line ->
            Row(Modifier.fillMaxWidth()) {
                Text(
                    text = "·",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = line,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

private fun nameOf(name: I18nText, fallback: String, l10n: UiText): String {
    val picked = name.pick(l10n.dataLang)
    // I18nText.pick 在两边都为空时返回 “—”，这时退回代号比留个破折号有用
    return if (picked.isBlank() || picked == "—") fallback else picked
}
