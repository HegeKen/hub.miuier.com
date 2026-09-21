package com.miuier.hub.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.miuier.hub.HubAppState
import com.miuier.hub.ThemeMode
import com.miuier.hub.data.AppLang
import com.miuier.hub.platform.rememberUrlOpener
import com.miuier.hub.ui.components.SectionCard
import com.miuier.hub.ui.components.rememberScreenListState
import com.miuier.hub.ui.i18n.UiText
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.menu.OverlayDropdownMenu
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 项目相关链接，与网页端 Footer.vue 用的是同一批 */
private const val REPO_URL = "https://github.com/HegeKen/hub.miuier.com"
private const val ISSUES_URL = "$REPO_URL/issues"
private const val AUTHOR_URL = "https://www.helilab.cn/#/"
private const val API_URL = "https://api.miuier.com/api/v3"

@Composable
fun SettingsScreen(
    app: HubAppState,
    contentPadding: PaddingValues,
    scrollBehavior: ScrollBehavior,
) {
    val l10n = app.ui
    val listState = rememberScreenListState(app)
    val openUrl = rememberUrlOpener()
    val themeModes = ThemeMode.entries.toList()

    LazyColumn(
        state = listState,
        modifier = Modifier
                .fillMaxSize()
                // 顶栏的大标题收起 + 悬浮按钮的方向判断，各接一条
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .nestedScroll(app.scrollToTop.nestedScrollConnection),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        // 整页统一用「分组卡片」：间隔交给 LazyColumn，不用手写 Spacer
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            // 外观原本是三段 TabRow。MIUIX 的 TabRow 是 LazyRow，内部还会把选中项
            // 「居中」滚动一次（见 TabRow.kt 的 scrollToItem(selected, -centerOffset)），
            // 放进卡片后实测会整体右移、贴着卡片边缘被圆角切掉。
            // 参考 Updater-KMP 的做法（Android 版本 / 区域 / 运营商全是 OverlayDropdownPreference），
            // 这里改成和「语言」完全同款的下拉行 —— 两张卡片结构一致，也就不用和布局打架。
            SectionCard(
                title = l10n.strings.appearance,
                modifier = CARD_MODIFIER,
                insideMargin = ROW_CARD_INSIDE,
                titlePadding = ROW_CARD_TITLE_PADDING,
            ) {
                OverlayDropdownMenu(
                    title = themeModeLabel(app.themeMode, l10n),
                    entry = DropdownEntry(
                        items = themeModes.map { mode ->
                            DropdownItem(
                                text = themeModeLabel(mode, l10n),
                                selected = mode == app.themeMode,
                                onClick = { app.themeMode = mode },
                            )
                        },
                    ),
                )
            }
        }

        item {
            SectionCard(
                title = l10n.strings.language,
                modifier = CARD_MODIFIER,
                insideMargin = ROW_CARD_INSIDE,
                titlePadding = ROW_CARD_TITLE_PADDING,
            ) {
                // OverlayDropdownMenu 的 title 是必填的，给空串仍会占掉一整行 headline1
                // （实测会在「语言」下面留出 50dp 空档），所以这里把**当前语言**当行主文本，
                // 小标题「语言」交给 SectionCard —— 再传一次 title 就重复了，
                // 那正是改造前那版的问题（卡片外写「语言」，卡片里又写一遍）。
                // 它依赖 Scaffold 提供的 MiuixPopupHost，本页就在内容区里，弹层能正常渲染。
                OverlayDropdownMenu(
                    title = app.lang.label,
                    entry = DropdownEntry(
                        items = AppLang.entries.map { lang ->
                            DropdownItem(
                                text = lang.label,
                                selected = lang == app.lang,
                                onClick = { app.lang = lang },
                            )
                        },
                    ),
                    // 21 种语言，限制一下高度免得弹层比屏幕还高
                    maxHeight = 420.dp,
                )
            }
        }

        item {
            SectionCard(l10n.strings.about, modifier = CARD_MODIFIER) {
                Text("MiROMS HUB", style = MiuixTheme.textStyles.title4)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = l10n.strings.aboutSource,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = API_URL,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MiuixTheme.colorScheme.dividerLine)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = l10n.strings.appDisclaimer,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }

        item {
            // 项目引导：仓库 / 问题反馈 / 作者主页，与网页端 Footer.vue 保持一致
            SectionCard(
                title = l10n.strings.project,
                modifier = CARD_MODIFIER,
                insideMargin = ROW_CARD_INSIDE,
                titlePadding = ROW_CARD_TITLE_PADDING,
            ) {
                BasicComponent(
                    title = "GitHub",
                    summary = "HegeKen/hub.miuier.com",
                    onClick = { openUrl(REPO_URL) },
                )
                HorizontalDivider(color = MiuixTheme.colorScheme.dividerLine)
                BasicComponent(
                    title = l10n.strings.feedback,
                    summary = "GitHub Issues",
                    onClick = { openUrl(ISSUES_URL) },
                )
                HorizontalDivider(color = MiuixTheme.colorScheme.dividerLine)
                BasicComponent(
                    title = l10n.strings.authorSite,
                    summary = "helilab.cn",
                    onClick = { openUrl(AUTHOR_URL) },
                )
            }
        }

        item {
            SectionCard(l10n.strings.builtWith, modifier = CARD_MODIFIER) {
                // 标签在上、值在下（Updater-KMP 的 MessageTextView 形态）：
                // 版本串很长，两列排版会把它挤成三行
                Text(
                    text = "Compose Multiplatform 1.12.0 · MIUIX 0.9.4 · Kotlin 2.4.20",
                    style = MiuixTheme.textStyles.body2,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "© 2019 - 2026 MiROMS HUB",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

/** 每个分组卡片左右各留 16dp 外边距 */
private val CARD_MODIFIER = Modifier.padding(horizontal = 16.dp)

/**
 * 内容是 MIUIX 行组件（`BasicComponent` / 下拉行）的卡片：这些行自带内边距，
 * 卡片左右不能再留白，否则行的按压反馈够不到卡片边缘；标题自己补 start 与行文字对齐。
 */
private val ROW_CARD_INSIDE = PaddingValues(vertical = 14.dp)
private val ROW_CARD_TITLE_PADDING = PaddingValues(start = 16.dp, bottom = 2.dp)

/**
 * 主题模式的显示名。
 *
 * 放在这里而不是 `ThemeMode` 上：枚举不该反向依赖界面文案表，
 * 否则 `AppState.kt` 要 import 整个 i18n 包。
 */
private fun themeModeLabel(mode: ThemeMode, l10n: UiText): String = when (mode) {
    ThemeMode.System -> l10n.strings.themeSystem
    ThemeMode.Light -> l10n.strings.themeLight
    ThemeMode.Dark -> l10n.strings.themeDark
}