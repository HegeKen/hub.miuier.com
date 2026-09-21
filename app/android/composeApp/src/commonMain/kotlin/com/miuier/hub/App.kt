package com.miuier.hub

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.miuier.hub.data.AppLang
import com.miuier.hub.platform.LocalDevice
import com.miuier.hub.platform.PlatformBackHandler
import com.miuier.hub.platform.currentLocalDevice
import com.miuier.hub.ui.components.LocalPageActive
import com.miuier.hub.ui.screens.DeviceDetailScreen
import com.miuier.hub.ui.screens.DevicesScreen
import com.miuier.hub.ui.screens.HomeScreen
import com.miuier.hub.ui.screens.MyDeviceScreen
import com.miuier.hub.ui.screens.OsRomsScreen
import com.miuier.hub.ui.screens.RomsScreen
import com.miuier.hub.ui.screens.SettingsScreen
import com.miuier.hub.ui.theme.AppTheme
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.FabPosition
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Phone
import top.yukonga.miuix.kmp.icon.extended.SearchDevice
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun App(
    initial: Screen = Screen.Home,
    lang: AppLang = AppLang.Default,
    themeMode: ThemeMode = ThemeMode.System,
    /** 本机硬件信息。桌面预览 / 无头截图可以注入一个假的，来看「本机信息」这一页 */
    local: LocalDevice? = currentLocalDevice(),
) {
    val app = remember {
        HubAppState(initial = initial, local = local).also {
            it.lang = lang
            it.themeMode = themeMode
        }
    }
    val l10n = app.ui

    // 本机机型在不在 hub 名单里，决定底栏要不要多出「本机信息」这一项。
    // 名单本身有缓存，和机型列表页共用同一次请求（见 HubRepository.devices）。
    LaunchedEffect(Unit) { app.resolveLocalDevice() }

    // 主题照搬 Updater-KMP：只给明暗，色板全部由 MIUIX 决定
    AppTheme(app.themeMode) {
        // 阿拉伯语从右往左：整棵界面树套一层 RTL，Compose 会自动镜像水平布局
        CompositionLocalProvider(
            LocalLayoutDirection provides if (app.lang.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        ) {
            // 系统返回：有上层就返回，根页面交还系统（退出）
            PlatformBackHandler(enabled = app.canGoBack) { app.back() }

            // 大标题顶部栏的滚动行为：各页面的列表都通过 nestedScroll 把滚动喂给它
            val scrollBehavior = MiuixScrollBehavior()
            val scope = rememberCoroutineScope()
            // 悬浮按钮：离开顶部且不在向下滑时才出现
            val fabVisible = app.scrollToTop.isVisible(app.listState)

            val title = when (val screen = app.current) {
                Screen.Home -> l10n.strings.home
                Screen.Devices -> l10n.strings.devices
                Screen.Roms -> l10n.strings.roms
                Screen.Settings -> l10n.strings.settings
                // 直接显示机型名比显示「本机信息」更有用；拿不到就退回 tab 名
                Screen.MyDevice -> app.localSummary?.name?.pick(l10n.dataLang)
                    ?: l10n.strings.myDevice
                is Screen.DeviceDetail -> screen.title
                is Screen.OsRoms -> screen.title
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = title,
                        // 大标题与小标题同文案：展开时是大字，往上滚就收进栏里
                        largeTitle = title,
                        scrollBehavior = scrollBehavior,
                        navigationIcon = {
                            if (app.canGoBack) {
                                IconButton(onClick = { app.back() }) {
                                    Icon(
                                        imageVector = MiuixIcons.Demibold.Back,
                                        contentDescription = l10n.strings.back,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                        },
                    )
                },
                bottomBar = {
                    // 「本机信息」是条件项：本机机型不在 hub 名单里时底栏就是原来的四项
                    FloatingNavigationBar {
                        app.rootScreens.forEach { screen ->
                            val (icon, label) = tabAppearance(screen, app)
                            FloatingNavigationBarItem(
                                selected = app.activeRoot == screen,
                                onClick = { app.selectRoot(screen) },
                                icon = icon,
                                label = label,
                            )
                        }
                    }
                },
                floatingActionButton = {
                    // 向下滑时从下方滑出屏幕，向上滑再回来
                    AnimatedVisibility(
                        visible = fabVisible,
                        enter = fadeIn() + slideInVertically { it },
                        exit = fadeOut() + slideOutVertically { it },
                    ) {
                        FloatingActionButton(
                            onClick = {
                                val state = app.listState ?: return@FloatingActionButton
                                scope.launch { state.animateScrollToItem(0) }
                            },
                        ) {
                            Icon(
                                // 没有 ArrowUp 字形，把 ChevronForward 转 90° 当作向上箭头，
                                // 和展开/收起用的是同一套视觉语言
                                imageVector = MiuixIcons.Demibold.ChevronForward,
                                contentDescription = l10n.strings.backToTop,
                                modifier = Modifier.size(22.dp).rotate(-90f),
                                tint = MiuixTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                },
                floatingActionButtonPosition = FabPosition.End,
                modifier = Modifier.fillMaxSize(),
            ) { padding ->
                val screens = app.rootScreens
                // 当前页在底栏里的位置；-1 表示它不是根页面（详情页，或本机机型还没落进名单）
                val rootIndex = screens.indexOf(app.current)

                // 详情页这类非根页面横滑没有明确目标，误滑反而会丢掉当前页，所以单独铺满。
                // 判断依据是「当前页在不在底栏里」而不是 canGoBack：冷启动直接落在详情页时
                // 栈里只有一页、canGoBack 是 false，但照样不该挂 Pager。
                if (app.canGoBack || rootIndex < 0) {
                    when (val screen = app.current) {
                        is Screen.DeviceDetail ->
                            DeviceDetailScreen(app, screen.code, padding, scrollBehavior)

                        is Screen.OsRoms -> OsRomsScreen(app, screen.os, padding, scrollBehavior)
                        else -> RootScreenContent(screen, app, padding, scrollBehavior)
                    }
                } else {
                    // 根页面之间用 Pager：左右滑动跟手、来回都带过渡动画，
                    // 并且顺手把左右相邻各一页预先组合好（beyondViewportPageCount），
                    // 滑过去的时候数据和布局都已经就位，不会先白一下再填。
                    val pagerState = rememberPagerState(initialPage = rootIndex) { screens.size }

                    // 正在往哪一页滚（-1 = 没有程序触发的滚动在跑）。
                    // 手点底栏点得快时，上一个动画还没落定就被取消，途中会经过中间页；
                    // 那些中间页不能回写 activeRoot，否则它会跟新目标互相打架，
                    // 表现就是「连点两下，最后停在半路或者弹回原来的页」。
                    var pendingPage by remember { mutableIntStateOf(-1) }

                    // 点底栏 → 带动画滚到对应页
                    LaunchedEffect(rootIndex) {
                        if (pagerState.currentPage == rootIndex) return@LaunchedEffect
                        pendingPage = rootIndex
                        try {
                            pagerState.animateScrollToPage(rootIndex)
                        } finally {
                            // 被下一次点击取消时也会走到这里；只有目标还是自己设的那个才清，
                            // 否则会把新一次点击刚设下的目标一起抹掉，闸门就白设了
                            if (pendingPage == rootIndex) pendingPage = -1
                        }
                    }
                    // 滑动的结果 → 回写导航状态，底栏高亮 / 标题 / 悬浮按钮都跟着走。
                    // 这里没用 snapshotFlow：无头渲染时它会跨线程读快照，
                    // Compose 会抛 multithreaded access to SnapshotStateObserver（见 ScrollToTop.kt）。
                    LaunchedEffect(pagerState.currentPage, pendingPage) {
                        if (pendingPage >= 0) return@LaunchedEffect
                        val screen = app.rootScreens.getOrNull(pagerState.currentPage)
                            ?: return@LaunchedEffect
                        if (screen != app.activeRoot) app.selectRoot(screen)
                    }

                    HorizontalPager(
                        state = pagerState,
                        beyondViewportPageCount = 1,
                        // 用页面自己的 key：本机机型是名单拉回来之后才插进底栏的，
                        // 有了 key，插进来之后 Pager 还认得出当前页是哪一个，不会整体错位。
                        // 注意必须是能存进 Bundle 的类型 —— 直接给 Screen 会崩在
                        // SaveableStateProvider（Android 侧只收基本类型/String/Parcelable）。
                        key = { page -> screens.getOrNull(page)?.pagerKey() ?: "page-$page" },
                        modifier = Modifier.fillMaxSize(),
                    ) { page ->
                        val screen = screens.getOrNull(page) ?: return@HorizontalPager
                        // 只有正在显示的那一页才去注册列表状态：
                        // 预加载的邻页也在组合、也在注册，不区分的话「回到顶部」会认错列表
                        CompositionLocalProvider(
                            LocalPageActive provides (page == pagerState.currentPage),
                        ) {
                            RootScreenContent(screen, app, padding, scrollBehavior)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Pager 每页的 key。必须是能存进 Bundle 的类型（Android 侧 SaveableStateProvider 的限制），
 * 用稳定字符串而不是页码：本机机型插进底栏会顶掉后面所有页码。
 */
private fun Screen.pagerKey(): String = when (this) {
    Screen.Home -> "home"
    Screen.Devices -> "devices"
    Screen.MyDevice -> "my-device"
    Screen.Roms -> "roms"
    Screen.Settings -> "settings"
    is Screen.DeviceDetail -> "device:$code"
    is Screen.OsRoms -> "os:$os"
}

/** 根页面（底栏那几项）的内容。Pager 里按页渲染，兜底单页渲染也走它，避免两处 when 走偏 */
@Composable
private fun RootScreenContent(
    screen: Screen,
    app: HubAppState,
    contentPadding: PaddingValues,
    scrollBehavior: ScrollBehavior,
) {
    when (screen) {
        Screen.Home -> HomeScreen(app, contentPadding, scrollBehavior)
        Screen.Devices -> DevicesScreen(app, contentPadding, scrollBehavior)
        Screen.Roms -> RomsScreen(app, contentPadding, scrollBehavior)
        Screen.Settings -> SettingsScreen(app, contentPadding, scrollBehavior)
        Screen.MyDevice -> MyDeviceScreen(app, contentPadding, scrollBehavior)
        else -> Unit
    }
}

/** 底栏每一项的图标与文案。`FloatingNavigationBarItem` 只画图标，label 仅作为无障碍描述 */
private fun tabAppearance(screen: Screen, app: HubAppState): Pair<ImageVector, String> {
    val strings = app.strings
    return when (screen) {
        Screen.Home -> MiuixIcons.Demibold.Home to strings.home
        Screen.Devices -> MiuixIcons.Demibold.Phone to strings.devices
        Screen.MyDevice -> MiuixIcons.Demibold.SearchDevice to strings.myDevice
        Screen.Roms -> MiuixIcons.Demibold.Folder to strings.roms
        else -> MiuixIcons.Demibold.Settings to strings.settings
    }
}
