package com.miuier.hub.ui.screens

import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.miuier.hub.HubAppState
import com.miuier.hub.Screen
import com.miuier.hub.data.RecentRom
import com.miuier.hub.data.utcToGmt8
import com.miuier.hub.ui.components.AsyncContent
import com.miuier.hub.ui.components.Tag
import com.miuier.hub.ui.components.rememberAsync
import com.miuier.hub.ui.i18n.UiText
import com.miuier.hub.ui.i18n.fill
import com.miuier.hub.ui.regionLabel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun HomeScreen(app: HubAppState, contentPadding: PaddingValues, scrollBehavior: ScrollBehavior) {
    val l10n = app.ui
    val listState = rememberScreenListState(app)
    val holder = rememberAsync("stats", onRefresh = app.repo::invalidate) { app.repo.stats() }

    AsyncContent(holder, contentPadding, l10n.strings, scrollBehavior) { stats ->
        LazyColumn(
            state = listState,
            // 把滚动喂给顶栏，才有大标题收起的效果
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Card(onClick = null, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(18.dp)) {
                        Text(
                            text = l10n.strings.updatedInDays.fill("days" to stats.recentDays),
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = stats.recentRoms.toString(),
                                style = MiuixTheme.textStyles.title1,
                                color = MiuixTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(0.dp))
                            Text(
                                text = l10n.strings.romVersionsSuffix,
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            // 接口给的是 UTC（`…T14:41:54.111Z`），展示统一换成 GMT+8，
                            // 并把时区标出来，免得被当成本地时间读
                            text = l10n.strings.dataUpdatedAt.fill(
                                "time" to "${utcToGmt8(stats.generatedAt)} GMT+8",
                            ),
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }

            item {
                Text(
                    text = l10n.strings.recentUpdates,
                    style = MiuixTheme.textStyles.title4,
                    modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 2.dp),
                )
            }

            items(stats.recent, key = { it.device + it.version }) { rom ->
                RecentRomCard(rom, l10n) {
                    app.push(Screen.DeviceDetail(rom.device, rom.name.pick(l10n.dataLang)))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecentRomCard(rom: RecentRom, l10n: UiText, onClick: () -> Unit) {
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
            // FlowRow：标签在窄屏上换行，而不是把每个标签内部撑成竖排
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
