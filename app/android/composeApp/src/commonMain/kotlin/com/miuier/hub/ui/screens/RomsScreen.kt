package com.miuier.hub.ui.screens

import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.fillMaxSize
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import com.miuier.hub.ui.components.rememberScreenListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.miuier.hub.ui.i18n.fill
import com.miuier.hub.HubAppState
import com.miuier.hub.Screen
import com.miuier.hub.ui.components.AsyncContent
import com.miuier.hub.ui.components.Tag
import com.miuier.hub.ui.components.rememberAsync
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 刷机包首页：按系统大版本（OS4 / OS3 / OS2 / OS1…）分组 */
@Composable
fun RomsScreen(app: HubAppState, contentPadding: PaddingValues, scrollBehavior: ScrollBehavior) {
    val l10n = app.ui
    val listState = rememberScreenListState(app)
    val holder = rememberAsync("osIndex", onRefresh = app.repo::invalidate) { app.repo.osIndex() }

    AsyncContent(holder, contentPadding, l10n.strings, scrollBehavior) { list ->
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(list, key = { it.os }) { item ->
                Card(
                    onClick = { app.push(Screen.OsRoms(item.os, item.os)) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            // 标签跟标题同一行（两者都短），把整列宽度留给下面的统计文案，
                            // 否则「2999 个包 · 111 款机型」会被标签挤成三行
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.os, style = MiuixTheme.textStyles.title3)
                                Spacer(Modifier.size(8.dp))
                                Tag(bigverLabel(item.os))
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = l10n.strings.romSummary.fill("count" to item.count, "devices" to item.deviceCount),
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                        Spacer(Modifier.size(8.dp))
                        Icon(
                            imageVector = MiuixIcons.Demibold.ChevronForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }
        }
    }
}

/** 大版本的展示名（数据里 roms/<os>.json 才有 bigver 字段，索引里没有，这里按 OS 代号给出别名） */
fun bigverLabel(os: String): String = when (os.uppercase()) {
    "OS1" -> "HyperOS 1"
    "OS2" -> "HyperOS 2"
    "OS3" -> "HyperOS 3"
    "OS4" -> "HyperOS 4"
    "V816" -> "HyperOS"
    else -> if (os.startsWith("V")) "MIUI" else os
}
