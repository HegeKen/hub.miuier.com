package com.miuier.hub.ui.screens

import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import com.miuier.hub.ui.components.rememberScreenListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.miuier.hub.HubAppState
import com.miuier.hub.data.Branch
import com.miuier.hub.data.ChangelogModule
import com.miuier.hub.data.DeviceDetail
import com.miuier.hub.data.FastbootResult
import com.miuier.hub.data.HubRepository
import com.miuier.hub.data.HighSpeedLink
import com.miuier.hub.data.HighSpeedResult
import com.miuier.hub.data.OtaRequest
import com.miuier.hub.data.Rom
import com.miuier.hub.data.downloadUrl
import com.miuier.hub.data.pickChangelog
import com.miuier.hub.platform.DownloadRequest
import com.miuier.hub.platform.copyToClipboard
import com.miuier.hub.platform.rememberDownloader
import com.miuier.hub.ui.branchKindLabel
import com.miuier.hub.ui.carrierLabel
import com.miuier.hub.ui.components.ActionButton
import com.miuier.hub.ui.components.Async
import com.miuier.hub.ui.components.AsyncContent
import com.miuier.hub.ui.components.DeviceThumbnail
import com.miuier.hub.ui.components.MetaRow
import com.miuier.hub.ui.components.SectionLabel
import com.miuier.hub.ui.components.Tag
import com.miuier.hub.ui.components.rememberAsync
import com.miuier.hub.ui.i18n.UiText
import com.miuier.hub.ui.i18n.fill
import com.miuier.hub.ui.regionLabel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 机型详情：设备信息卡 + 每个分支一段可折叠的 ROM 列表 */
@Composable
fun DeviceDetailScreen(
    app: HubAppState,
    code: String,
    contentPadding: PaddingValues,
    scrollBehavior: ScrollBehavior,
) {
    DeviceDetailList(app, code, contentPadding, scrollBehavior)
}

/**
 * 机型详情的列表本体。
 *
 * 抽出来是为了「本机信息」页能复用同一份分支 / ROM 列表，只在最前面插一张本机卡片
 * （[leading]），而不是把那几百行再抄一遍。
 */
@Composable
internal fun DeviceDetailList(
    app: HubAppState,
    code: String,
    contentPadding: PaddingValues,
    scrollBehavior: ScrollBehavior,
    leading: (@Composable () -> Unit)? = null,
    showDeviceCard: Boolean = true,
) {
    val l10n = app.ui
    val holder = rememberAsync("device", code, onRefresh = app.repo::invalidate) { app.repo.device(code) }

    AsyncContent(holder, contentPadding, l10n.strings, scrollBehavior) { detail ->
        // 记录**已展开**的分支下标：空集合 = 全部收起，这是默认状态。
        // 一个机型最多 7 个分支、单个分支上百条 ROM，全展开的话一进来就是几千行。
        val expandedBranches = remember(detail) { mutableStateListOf<Int>() }
        val listState = rememberScreenListState(app)

        // 把「分支标题 + 该分支的 ROM」摊平成一个列表，避免 LazyColumn 嵌套
        val entries = remember(detail, expandedBranches.toList()) {
            buildList {
                detail.branches.forEachIndexed { index, branch ->
                    add(Entry.Header(index, branch))
                    if (expandedBranches.contains(index)) {
                        branch.roms.forEach { add(Entry.RomRow(index, branch, it)) }
                    }
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                // 顶栏的大标题收起 + 悬浮按钮的方向判断，各接一条
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
            if (leading != null) {
                item(key = "leading") { leading() }
            }

            // 「本机信息」页用自己的本机卡片承载机型档案，就不再重复这张通用卡
            if (showDeviceCard) {
                item { DeviceInfoCard(detail, l10n) }
            }

            // 必须给稳定 key：折叠分支会改变列表下标，没有 key 的话
            // 「某条 ROM 已展开」的状态会跟着下标跑到别的 ROM 上
            itemsIndexed(entries, key = { _, entry -> entry.key }) { _, entry ->
                when (entry) {
                    is Entry.Header -> BranchHeader(
                        branch = entry.branch,
                        romCount = entry.branch.roms.size,
                        expanded = expandedBranches.contains(entry.index),
                        l10n = l10n,
                        repo = app.repo,
                        onToggle = {
                            if (expandedBranches.contains(entry.index)) expandedBranches.remove(entry.index)
                            else expandedBranches.add(entry.index)
                        },
                    )

                    is Entry.RomRow -> RomCard(
                        rom = entry.rom,
                        device = detail.device,
                        region = entry.branch.region,
                        // 高速下载要用的请求表单里，d 就是分支代号（branch.id）
                        branchCode = entry.branch.id,
                        branchTag = entry.branch.tags.branchtag.ifBlank { entry.branch.tags.btag },
                        zone = entry.branch.zone.toIntOrNull() ?: 1,
                        l10n = l10n,
                        repo = app.repo,
                    )
                }
            }
        }
    }
}

private sealed interface Entry {
    val key: String

    data class Header(val index: Int, val branch: Branch) : Entry {
        override val key: String get() = "h:$index:${branch.region}:${branch.tags.branch}"
    }

    data class RomRow(val index: Int, val branch: Branch, val rom: Rom) : Entry {
        override val key: String get() = "r:$index:${rom.miui}"
    }
}

@Composable
private fun DeviceInfoCard(detail: DeviceDetail, l10n: UiText) {
    Card(
        onClick = null,
        insideMargin = PaddingValues(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        // 图片按整张卡片的高度垂直居中：信息行比缩略图高，默认的 Top 对齐会让图片吊在顶上
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DeviceThumbnail(
                device = detail.device,
                brand = detail.brand.firstOrNull(),
                contentDescription = detail.name.pick(l10n.dataLang),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(detail.name.pick(l10n.dataLang), style = MiuixTheme.textStyles.title3)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = detail.device,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Spacer(Modifier.height(10.dp))
                MetaRow(l10n.strings.deviceCode, detail.code)
                MetaRow(l10n.strings.brand, detail.brand.joinToString(" / "))
                MetaRow("Android", detail.android.joinToString(", "))
                MetaRow(l10n.strings.supports, detail.supports.joinToString(", "))
                val series = detail.series.joinToString(" / ") { it.pick(l10n.dataLang) }
                if (series.isNotBlank()) MetaRow(l10n.strings.series, series)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BranchHeader(
    branch: Branch,
    romCount: Int,
    expanded: Boolean,
    l10n: UiText,
    repo: HubRepository,
    onToggle: () -> Unit,
) {
    // 线刷包是按**分支**发货的（不是每个版本一份），所以按钮放在分支标题卡里：
    // 点一次就拿这个分支当前最新的线刷包
    var loading by remember(branch.id) { mutableStateOf(false) }
    var result by remember(branch.id) { mutableStateOf<FastbootResult?>(null) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxWidth()) {
        Card(
            onClick = onToggle,
            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = branch.name.pick(l10n.dataLang),
                        style = MiuixTheme.textStyles.title4,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "$romCount",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    Spacer(Modifier.size(6.dp))
                    ExpandChevron(expanded = expanded, contentDescription = null)
                }
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Tag(regionLabel(branch.region, l10n.locale))
                    Tag(branchKindLabel(branch.tags, l10n.strings))
                    if (branch.ep == "1") Tag(l10n.strings.enterprise)
                }
                val carriers = branch.carrier.filter { it.isNotBlank() }
                if (carriers.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = carriers.joinToString(" · ") { carrierLabel(it, l10n.locale) },
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
                Spacer(Modifier.height(10.dp))
                // 把分支支持的运营商整份交下去：接口是按运营商发货的，
                // 每个运营商各问一次，有几家就出来几条（含 n= 留空的通用包）
                ActionButton(
                    text = if (loading) l10n.strings.fetching else l10n.strings.getFastboot,
                    onClick = {
                        loading = true
                        scope.launch {
                            result = repo.fastboot(
                                branchId = branch.id,
                                branchTag = branch.tags.branchtag.ifBlank { branch.tags.btag },
                                region = branch.region,
                                carriers = branch.carrier,
                            )
                            loading = false
                        }
                    },
                    enabled = !loading,
                )
            }
        }

        // 结果放在卡片外：标题卡是「点哪都折叠」的，条目落在里面会被误点收起
        result?.let { res ->
            Spacer(Modifier.height(8.dp))
            FastbootResultCard(res, l10n)
        }
    }
}

/**
 * 线刷包查询结果。
 *
 * 接口是按运营商发货的，所以结果本身就是「一个运营商一条」；
 * `carrier` 为空串的那条是 `n=` 留空问到的通用包。
 */
@Composable
private fun FastbootResultCard(result: FastbootResult, l10n: UiText) {
    val download = rememberDownloader()
    val clipboard = LocalClipboard.current

    Card(
        onClick = null,
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth()) {
            SectionLabel(l10n.strings.fastboot)
            Spacer(Modifier.height(4.dp))
            when (result) {
                is FastbootResult.Available -> result.packages.forEach { pkg ->
                    DownloadRow(
                        label = if (pkg.carrier.isBlank()) l10n.strings.generic
                        else carrierLabel(pkg.carrier, l10n.locale),
                        url = pkg.url,
                        l10n = l10n,
                        download = download,
                        clipboard = clipboard,
                    )
                }

                FastbootResult.NotFound -> Hint(l10n.strings.fastbootNotFound)

                is FastbootResult.Failed -> Hint(
                    text = l10n.strings.requestFailed + result.message,
                    error = true,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RomCard(
    rom: Rom,
    device: String,
    region: String,
    branchCode: String,
    branchTag: String,
    zone: Int,
    l10n: UiText,
    repo: HubRepository,
) {
    // 用版本号做 remember 的 key；配合 LazyColumn 的稳定 key，折叠分支后状态不会串位
    var expanded by remember(rom.miui) { mutableStateOf(false) }

    Card(
        onClick = { expanded = !expanded },
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 11.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = rom.miui.ifBlank { "—" },
                    style = MiuixTheme.textStyles.body1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Tag(rom.os.ifBlank { rom.bigver })
                Spacer(Modifier.size(6.dp))
                ExpandChevron(
                    expanded = expanded,
                    contentDescription = if (expanded) l10n.strings.collapse else l10n.strings.expand,
                )
            }
            Spacer(Modifier.height(5.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Tag(if (rom.android.isBlank()) "—" else "Android ${rom.android}")
                Tag(rom.release.ifBlank { "—" })
                Tag(if (rom.aspatch.isBlank()) l10n.strings.noPatch else rom.aspatch)
            }
            Spacer(Modifier.height(7.dp))
            HorizontalDivider(color = MiuixTheme.colorScheme.dividerLine)
            Spacer(Modifier.height(7.dp))
            // 收起时也保留这两个标签：一眼能看出这个版本有没有包可下
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Tag(
                    text = if (rom.hasRecovery) l10n.strings.hasRecovery else l10n.strings.noRecovery,
                    container = if (rom.hasRecovery) MiuixTheme.colorScheme.primaryContainer
                    else MiuixTheme.colorScheme.secondaryContainer,
                    content = if (rom.hasRecovery) MiuixTheme.colorScheme.onPrimaryContainer
                    else MiuixTheme.colorScheme.onSecondaryContainer,
                )
                Tag(
                    text = if (rom.hasFastboot) l10n.strings.hasFastboot else l10n.strings.noFastboot,
                    container = if (rom.hasFastboot) MiuixTheme.colorScheme.primaryContainer
                    else MiuixTheme.colorScheme.secondaryContainer,
                    content = if (rom.hasFastboot) MiuixTheme.colorScheme.onPrimaryContainer
                    else MiuixTheme.colorScheme.onSecondaryContainer,
                )
            }

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                DownloadSection(rom, l10n)
                Spacer(Modifier.height(16.dp))
                // 日志只在展开后才进入组合，所以是「按需拉取」：
                // 一个分支上百个 ROM，不会一进页面就把日志全请求一遍
                ChangelogSection(device, region, rom.miui, l10n, repo)
                Spacer(Modifier.height(16.dp))
                HighSpeedSection(
                    request = OtaRequest(
                        code = branchCode,
                        device = device,
                        branchTag = branchTag,
                        region = region,
                        zone = zone,
                        android = rom.android,
                        version = rom.miui,
                    ),
                    l10n = l10n,
                    repo = repo,
                )
            }
        }
    }
}

@Composable
private fun DownloadSection(rom: Rom, l10n: UiText) {
    // 交给系统下载器（Android 是 DownloadManager），不在应用里自己下
    val download = rememberDownloader()
    val clipboard = LocalClipboard.current

    Column(Modifier.fillMaxWidth()) {
        SectionLabel(l10n.strings.download)
        Spacer(Modifier.height(4.dp))
        if (!rom.hasRecovery && !rom.hasFastboot) {
            Text(
                text = l10n.strings.noPackage,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        if (rom.hasRecovery) {
            DownloadRow(l10n.strings.recovery, downloadUrl(rom.miui, rom.recovery), l10n, download, clipboard)
        }
        if (rom.hasFastboot) {
            DownloadRow(l10n.strings.fastboot, downloadUrl(rom.miui, rom.fastboot), l10n, download, clipboard)
        }
    }
}

@Composable
private fun DownloadRow(
    label: String,
    url: String,
    l10n: UiText,
    download: (DownloadRequest) -> Unit,
    clipboard: Clipboard,
) {
    // 新的 Clipboard API 是 suspend 的，复制得在协程里做
    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MiuixTheme.textStyles.body2)
            Spacer(Modifier.height(2.dp))
            // 只显示文件名：完整 URL 一百多字符，在手机上排不下也没必要
            Text(
                text = url.substringAfterLast('/'),
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // 文档：https://compose-miuix-ui.github.io/miuix/zh_CN/components/iconbutton
        // 图标色用 onSurface（Updater-KMP 的做法）：这两个是卡片里的次级操作，
        // 用 primary 会和「获取高速下载链接」那个主操作抢注意力
        IconButton(onClick = { scope.launch { clipboard.copyToClipboard(url) } }) {
            Icon(
                imageVector = MiuixIcons.Demibold.Copy,
                contentDescription = l10n.strings.copyLink,
                modifier = Modifier.size(20.dp),
                tint = MiuixTheme.colorScheme.onSurface,
            )
        }
        IconButton(onClick = { download(DownloadRequest(url, label)) }) {
            Icon(
                imageVector = MiuixIcons.Demibold.Download,
                contentDescription = l10n.strings.download,
                modifier = Modifier.size(20.dp),
                tint = MiuixTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ChangelogSection(
    device: String,
    region: String,
    version: String,
    l10n: UiText,
    repo: HubRepository,
) {
    val holder = rememberAsync("changelog", device, region, version) {
        repo.changelog(device, region, version)
    }

    Column(Modifier.fillMaxWidth()) {
        SectionLabel(l10n.strings.changelog)
        Spacer(Modifier.height(6.dp))
        when (val state = holder.state) {
            is Async.Loading -> Text(
                text = l10n.strings.loading,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )

            is Async.Failed -> Text(
                text = l10n.strings.changelogFailed,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.error,
            )

            is Async.Ready -> {
                val modules = pickChangelog(state.value, l10n.dataLang)
                if (modules.isEmpty()) {
                    // 老机型和部分冷门区域没有导出过日志，接口直接 404
                    Text(
                        text = l10n.strings.noChangelog,
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                } else {
                    modules.forEach { ChangelogBlock(it) }
                }
            }
        }
    }
}

@Composable
private fun ChangelogBlock(module: ChangelogModule) {
    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Text(
            text = module.title,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurface,
        )
        module.lines.forEach { line ->
            Text(
                text = line,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(start = 10.dp, top = 3.dp),
            )
        }
    }
}

/**
 * 展开 / 收起的指示箭头。
 *
 * 这里**不用** `MiuixIcons.Demibold.ExpandMore` / `ExpandLess`：MIUIX 0.9.4 里
 * 这两个字形本身是坏的——把它单独放大到 64dp 渲染，出来的仍然只是几段断开的折线和一个孤点，
 * 根本不成箭头（ChevronForward / ChevronBackward 则完全正常）。
 * 所以改用渲染正常的 ChevronForward 旋转 90°：向下表示可展开，向上表示可收起。
 */
@Composable
private fun ExpandChevron(expanded: Boolean, contentDescription: String?) {
    Icon(
        imageVector = MiuixIcons.Demibold.ChevronForward,
        contentDescription = contentDescription,
        modifier = Modifier.size(16.dp).rotate(if (expanded) -90f else 90f),
        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    )
}

/**
 * 高速下载。
 *
 * 点按钮才去问小米 OTA 接口（`update.miui.com/updates/miotaV3.php`），
 * 拿它下发的 `MirrorList` 里的 superota / ultimateota 域名，拼成
 * `<mirror>/<版本号>/<带签名的文件名>` —— 已实测这样的地址返回 206，可以下载。
 *
 * 这个接口本职是「查有没有新版本」，所以只对**该分支最新版**签发带签名的文件名：
 * 请求旧版本时返回的是「能升到的最新版」，而旧版本自己的文件名没有签名，
 * 直接拼到 mirror 上会 403。这几种情况都在下面如实说明，不假装成功。
 */
@Composable
private fun HighSpeedSection(
    request: OtaRequest,
    l10n: UiText,
    repo: HubRepository,
) {
    var loading by remember(request) { mutableStateOf(false) }
    var result by remember(request) { mutableStateOf<HighSpeedResult?>(null) }
    val scope = rememberCoroutineScope()
    val download = rememberDownloader()
    val clipboard = LocalClipboard.current

    Column(Modifier.fillMaxWidth()) {
        SectionLabel(l10n.strings.highSpeed)
        Spacer(Modifier.height(6.dp))

        // 参照 Updater-KMP：主操作 = 整行填充的主色文字按钮，展开卡片里的其余操作都是图标按钮
        ActionButton(
            text = if (loading) l10n.strings.fetching else l10n.strings.getHighSpeed,
            onClick = {
                loading = true
                scope.launch {
                    result = repo.highSpeed(request)
                    loading = false
                }
            },
            enabled = !loading,
        )

        result?.let { res ->
            Spacer(Modifier.height(8.dp))
            when (res) {
                is HighSpeedResult.Available -> res.links.forEach { link ->
                    HighSpeedLinkRow(link, l10n, download, clipboard)
                }

                is HighSpeedResult.Outdated -> Hint(
                    l10n.strings.otaOutdated.fill("latest" to res.latestVersion),
                )

                HighSpeedResult.Unsigned -> Hint(
                    l10n.strings.otaUnsigned,
                )

                HighSpeedResult.NotFound -> Hint(
                    l10n.strings.otaNotFound,
                )

                is HighSpeedResult.Failed -> Hint(
                    text = l10n.strings.requestFailed + res.message,
                    error = true,
                )
            }
        }
    }
}

@Composable
private fun HighSpeedLinkRow(
    link: HighSpeedLink,
    l10n: UiText,
    download: (DownloadRequest) -> Unit,
    clipboard: Clipboard,
) {
    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = link.mirror.removePrefix("https://"),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(2.dp))
            // 只显示文件名：带签名的完整地址很长，手机上排不下也没必要
            Text(
                text = link.url.substringAfterLast('/').substringBefore('?'),
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = { scope.launch { clipboard.copyToClipboard(link.url) } }) {
            Icon(
                imageVector = MiuixIcons.Demibold.Copy,
                contentDescription = l10n.strings.copyLink,
                modifier = Modifier.size(20.dp),
                tint = MiuixTheme.colorScheme.onSurface,
            )
        }
        IconButton(onClick = { download(DownloadRequest(link.url, link.mirror)) }) {
            Icon(
                imageVector = MiuixIcons.Demibold.Download,
                contentDescription = l10n.strings.download,
                modifier = Modifier.size(20.dp),
                tint = MiuixTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun Hint(text: String, error: Boolean = false) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.footnote2,
        color = if (error) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.onSurfaceVariantSummary,
    )
}
