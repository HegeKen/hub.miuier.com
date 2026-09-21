package com.miuier.hub.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

/**
 * 这一页现在是不是「正在显示的那一页」。
 *
 * Pager 会预先组合左右相邻页备用，那些页面也在组合、也会调 [rememberScreenListState]，
 * 不区分的话「回到顶部」悬浮按钮会认错列表（跟着邻页的状态走）。
 * 默认 true：非 Pager 的场景（二级页、桌面预览）无需关心。
 */
val LocalPageActive = compositionLocalOf { true }

/**
 * 页面列表状态：除了自己用，还注册到 [com.miuier.hub.HubAppState.listState]，
 * 让外层 Scaffold 里的「回到顶部」悬浮按钮能读到它。
 * 只有活跃页（见 [LocalPageActive]）才注册，预加载的邻页不抢。
 */
@Composable
fun rememberScreenListState(app: com.miuier.hub.HubAppState): LazyListState {
    val state = rememberLazyListState()
    val active = LocalPageActive.current
    LaunchedEffect(state, active) { if (active) app.listState = state }
    DisposableEffect(state) {
        onDispose { if (app.listState === state) app.listState = null }
    }
    return state
}

/**
 * 「回到顶部」悬浮按钮的滚动方向。
 *
 * 方向是靠 [nestedScrollConnection] 收滚动事件算出来的，**没用 snapshotFlow**：
 * 无头渲染时 snapshotFlow 的收集可能在别的线程读写快照，而布局/绘制在主线程，
 * Compose 会抛 `Detected multithreaded access to SnapshotStateObserver`。
 * nestedScroll 的回调本来就在主线程的滚动手势里跑，天然没这个问题。
 */
class ScrollToTopController {
    private var goingDown by mutableStateOf(false)

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // Compose 的约定：内容往上拖（也就是向下翻）available.y 为负
            if (available.y < -0.5f) goingDown = true
            else if (available.y > 0.5f) goingDown = false
            return Offset.Zero
        }
    }

    /**
     * 是否显示：已经离开顶部、且不是正在向下滑。
     * 回到顶部时按钮本身也没意义，一并隐藏。
     */
    fun isVisible(listState: LazyListState?): Boolean {
        if (listState == null) return false
        val atTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        return !atTop && !goingDown
    }
}

@Composable
fun rememberScrollToTopController(): ScrollToTopController = remember { ScrollToTopController() }
