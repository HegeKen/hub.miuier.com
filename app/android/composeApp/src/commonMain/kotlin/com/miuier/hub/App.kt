package com.miuier.hub

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
                when (val screen = app.current) {
                    Screen.Home -> HomeScreen(app, padding, scrollBehavior)
                    Screen.Devices -> DevicesScreen(app, padding, scrollBehavior)
                    Screen.Roms -> RomsScreen(app, padding, scrollBehavior)
                    Screen.Settings -> SettingsScreen(app, padding, scrollBehavior)
                    Screen.MyDevice -> MyDeviceScreen(app, padding, scrollBehavior)
                    is Screen.DeviceDetail -> DeviceDetailScreen(app, screen.code, padding, scrollBehavior)
                    is Screen.OsRoms -> OsRomsScreen(app, screen.os, padding, scrollBehavior)
                }
            }
        }
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
