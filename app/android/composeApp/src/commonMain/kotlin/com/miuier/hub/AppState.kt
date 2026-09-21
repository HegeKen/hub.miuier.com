package com.miuier.hub

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.miuier.hub.data.AppLang
import com.miuier.hub.data.DeviceSummary
import com.miuier.hub.data.HubRepository
import com.miuier.hub.data.matchLocalDevice
import com.miuier.hub.platform.LocalDevice
import com.miuier.hub.platform.currentLocalDevice
import com.miuier.hub.ui.components.ScrollToTopController
import com.miuier.hub.ui.i18n.LocaleData
import com.miuier.hub.ui.i18n.Locales
import com.miuier.hub.ui.i18n.Strings
import com.miuier.hub.ui.i18n.UiText

enum class ThemeMode { System, Light, Dark }

/**
 * 极简导航：一个后进先出的栈 + 若干根页面。
 * 页面很少，没必要为此引入 navigation-compose（其多平台版本目前仍在 beta）。
 */
sealed interface Screen {
    data object Home : Screen
    data object Devices : Screen
    data object Roms : Screen
    data object Settings : Screen

    /** 本机信息。只在本机机型出现在 hub 名单里时才会进底栏 */
    data object MyDevice : Screen

    /** 带 title 是为了顶栏能立刻显示机型名，而不用等详情加载完 */
    data class DeviceDetail(val code: String, val title: String) : Screen
    data class OsRoms(val os: String, val title: String) : Screen

    /**
     * 这个页面归属于底栏的哪一项。详情页是从某个根页面推进去的，
     * 这里只给出「没有额外上下文时的归属」（例如冷启动直接落在详情页）。
     * 运行中真正高亮哪一项由 [HubAppState.activeRoot] 记着。
     */
    val rootOf: Screen
        get() = when (this) {
            Screen.Home -> Screen.Home
            Screen.Devices, is Screen.DeviceDetail -> Screen.Devices
            Screen.MyDevice -> Screen.MyDevice
            Screen.Roms, is Screen.OsRoms -> Screen.Roms
            Screen.Settings -> Screen.Settings
        }
}

class HubAppState(
    val repo: HubRepository = HubRepository(),
    /** 初始页面。留了参数是为了预览与无头截图能直接落到指定页面 */
    initial: Screen = Screen.Home,
    /** 本机硬件信息。默认读平台实现，桌面预览可以注入一个假的来看「本机信息」这一页 */
    val local: LocalDevice? = currentLocalDevice(),
) {
    var lang by mutableStateOf(AppLang.Default)
    var themeMode by mutableStateOf(ThemeMode.System)

    /** 当前语言的全部本地化数据（21 种语言，见 ui/i18n/Locales.kt） */
    val locale: LocaleData get() = Locales.getValue(lang)

    val strings: Strings get() = locale.strings

    /** 传给数据层的语言：v3 只有 zh / en 两套 */
    val dataLang: String get() = lang.dataCode

    /** 界面取词条的统一入口 */
    val ui: UiText get() = UiText(strings, locale, dataLang)

    /**
     * 本机机型在 hub 名单里的那一条 —— 拿到之前（以及拿不到时）都是 null，
     * 底栏据此决定「本机信息」这项要不要出现。
     */
    var localSummary: DeviceSummary? by mutableStateOf(null)
        private set

    /**
     * 当前页面的列表滚动状态，由各页面注册进来。
     * 悬浮按钮要靠它做两件事：判断「是否已经离开顶部 / 是否正在向下滑」，以及回到顶部。
     * 页面没有列表时为 null，此时悬浮按钮不显示。
     */
    var listState: LazyListState? by mutableStateOf(null)

    /** 「回到顶部」悬浮按钮的滚动方向状态，由各页面的列表通过 nestedScroll 喂进来 */
    val scrollToTop = ScrollToTopController()

    /**
     * 导航栈。初始页原样放进栈底 —— 预览 / 无头截图会直接指定「机型详情」这类非根页面，
     * 那时不能把它替换成首页（否则截出来的是首页）。底栏高亮另有 [activeRoot]。
     */
    private val stack = mutableStateListOf(initial)

    /**
     * 当前所处的**根页面**。单独记一个字段而不是从栈顶反推：
     * 从「本机信息」点进某个机型详情时，底栏该继续高亮「本机信息」，
     * 而不是跳回「机型列表」。
     */
    var activeRoot: Screen by mutableStateOf(initial.rootOf)
        private set

    val current: Screen get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1

    /** 底栏内容。本机机型在名单里时才插进「本机信息」 */
    val rootScreens: List<Screen>
        get() = buildList {
            add(Screen.Home)
            add(Screen.Devices)
            if (localSummary != null) add(Screen.MyDevice)
            add(Screen.Roms)
            add(Screen.Settings)
        }

    fun push(screen: Screen) = stack.add(screen)

    fun back() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    /** 切根页面：先退回到栈底，再替换栈底，避免详情页残留在返回栈里 */
    fun selectRoot(screen: Screen) {
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
        stack[0] = screen
        activeRoot = screen
    }

    /**
     * 把本机代号和 hub 机型名单对一次。App 启动时调一次即可；
     * 对不上（或不在小米设备上）就保持 null，tab 不出现。
     */
    suspend fun resolveLocalDevice() {
        val local = local ?: return
        if (localSummary != null) return
        localSummary = runCatching { repo.devices() }.getOrNull()?.matchLocalDevice(local)
    }
}
