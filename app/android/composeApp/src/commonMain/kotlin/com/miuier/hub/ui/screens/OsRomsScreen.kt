package com.miuier.hub.ui.screens

import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.Column
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import com.miuier.hub.ui.components.rememberScreenListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.miuier.hub.HubAppState
import com.miuier.hub.Screen
import com.miuier.hub.data.OsRom
import com.miuier.hub.ui.components.AsyncContent
import com.miuier.hub.ui.components.Tag
import com.miuier.hub.ui.components.rememberAsync
import com.miuier.hub.ui.i18n.UiText
import com.miuier.hub.ui.i18n.fill
import com.miuier.hub.ui.regionLabel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 某个系统大版本下的全部 ROM（OS1 有 5000+ 条，靠 LazyColumn 虚拟化 + 搜索收敛） */
@Composable
fun OsRomsScreen(
    app: HubAppState,
    os: String,
    contentPadding: PaddingValues,
    scrollBehavior: ScrollBehavior,
) {
    val l10n = app.ui
    val listState = rememberScreenListState(app)
    val holder = rememberAsync("osRoms", os, onRefresh = app.repo::invalidate) { app.repo.osRoms(os) }
    var query by remember { mutableStateOf("") }

    AsyncContent(holder, contentPadding, l10n.strings, scrollBehavior) { all ->
        val filtered = remember(all, query) {
            val q = query.trim().lowercase()
            if (q.isBlank()) all
            else all.filter {
                it.name.en.lowercase().contains(q) ||
                    it.name.zh.lowercase().contains(q) ||
                    it.device.lowercase().contains(q) ||
                    it.version.lowercase().contains(q)
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
            item {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    label = l10n.strings.searchRom,
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Text(
                    text = l10n.strings.entryCount.fill("count" to filtered.size),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                )
            }
            items(filtered) { rom ->
                OsRomCard(rom, l10n) {
                    app.push(Screen.DeviceDetail(rom.device, rom.name.pick(l10n.dataLang)))
                }
            }
        }
    }
}

@Composable
private fun OsRomCard(rom: OsRom, l10n: UiText, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = rom.name.pick(l10n.dataLang),
                    style = MiuixTheme.textStyles.title4,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = rom.version,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (rom.region.isNotBlank()) Tag(regionLabel(rom.region, l10n.locale))
                Tag(rom.branchName.pick(l10n.dataLang))
                Tag(if (rom.android.isBlank()) "—" else "Android ${rom.android}")
                Tag(rom.release)
            }
        }
    }
}
