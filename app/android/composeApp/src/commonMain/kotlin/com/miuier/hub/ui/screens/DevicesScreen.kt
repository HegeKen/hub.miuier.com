package com.miuier.hub.ui.screens

import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import com.miuier.hub.ui.components.rememberScreenListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.miuier.hub.ui.i18n.fill
import com.miuier.hub.HubAppState
import com.miuier.hub.Screen
import com.miuier.hub.ui.components.AsyncContent
import com.miuier.hub.ui.components.DeviceCard
import com.miuier.hub.ui.components.rememberAsync
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun DevicesScreen(app: HubAppState, contentPadding: PaddingValues, scrollBehavior: ScrollBehavior) {
    val l10n = app.ui
    val listState = rememberScreenListState(app)
    val holder = rememberAsync("devices", onRefresh = app.repo::invalidate) { app.repo.devices() }

    // 搜索/筛选状态放在 AsyncContent 之外，重新加载数据不会把用户的输入清掉
    var query by remember { mutableStateOf("") }
    var brandIndex by remember { mutableIntStateOf(0) }

    AsyncContent(holder, contentPadding, l10n.strings, scrollBehavior) { all ->
        // 品牌选项直接来自数据，避免写死（数据里目前是 Xiaomi / REDMI / POCO）
        val brands = remember(all) { all.flatMap { it.brand }.distinct().sorted() }
        val tabs = remember(brands, l10n) { listOf(l10n.strings.all) + brands }
        val selectedBrand = brands.getOrElse(brandIndex - 1) { "" }

        val filtered = remember(all, query, selectedBrand) {
            val q = query.trim().lowercase()
            all.filter { d ->
                (selectedBrand.isBlank() || d.brand.any { it.equals(selectedBrand, ignoreCase = true) }) &&
                    (q.isBlank() ||
                        d.device.lowercase().contains(q) ||
                        d.name.zh.lowercase().contains(q) ||
                        d.name.en.lowercase().contains(q))
            }
        }

        // 搜索框和品牌页签也放进列表里当普通 item：
        // 这样它们跟列表一起滚，大标题收起的效果才连贯（内容要能滚到顶栏下面去）
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
                    label = l10n.strings.searchDevice,
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                TabRow(
                    tabs = tabs,
                    selectedTabIndex = brandIndex.coerceIn(0, tabs.lastIndex),
                    onTabSelected = { brandIndex = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                Text(
                    text = l10n.strings.deviceCount.fill("count" to filtered.size),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                )
            }

            items(filtered, key = { it.device }) { device ->
                DeviceCard(
                    name = device.name.pick(l10n.dataLang),
                    codename = device.device,
                    brand = device.brand.firstOrNull().orEmpty(),
                    romCount = device.romCount,
                    strings = l10n.strings,
                ) {
                    app.push(Screen.DeviceDetail(device.device, device.name.pick(l10n.dataLang)))
                }
            }
        }
    }
}
