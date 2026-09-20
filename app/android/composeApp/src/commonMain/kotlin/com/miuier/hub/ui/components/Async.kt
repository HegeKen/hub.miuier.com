package com.miuier.hub.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.miuier.hub.ui.i18n.Strings
import kotlin.coroutines.cancellation.CancellationException
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 加载 / 失败 / 成功三态 */
sealed interface Async<out T> {
    data object Loading : Async<Nothing>
    data class Failed(val message: String) : Async<Nothing>
    data class Ready<T>(val value: T) : Async<T>
}

class AsyncHolder<T : Any> {
    var state by mutableStateOf<Async<T>>(Async.Loading)
        internal set

    /**
     * 是否正在「下拉刷新」。和首次加载 / 失败重试区分开：
     * 刷新时**保留已加载的内容**，只有指示器在转，列表不会闪一下变成骨架。
     */
    var refreshing by mutableStateOf(false)
        internal set

    /** 改这个值即可重新加载 */
    internal var tick by mutableStateOf(0)

    /** 失败重试：退回加载态重来 */
    fun retry() {
        tick++
    }

    /**
     * 下拉刷新。必须**同步**把它置为 true —— 这是 MIUIX `PullToRefresh` 的约定：
     * 指示器收回进入刷新态时若 `isRefreshing` 还是 false，刷新会被当成已完成直接播放完成动画。
     */
    fun refresh() {
        refreshing = true
        tick++
    }
}

/**
 * 把一次 suspend 加载收敛成三态。keys 变化会重新请求，
 * 配合 HubRepository 的内存缓存，返回上一层再进来不会重复打网络。
 *
 * [onRefresh] 只在下拉刷新时执行（典型用法是清掉 `HubRepository` 的缓存，
 * 否则「刷新」会直接命中内存缓存、什么都不会变）。失败重试不走它。
 */
@Composable
fun <T : Any> rememberAsync(
    vararg keys: Any?,
    onRefresh: (suspend () -> Unit)? = null,
    loader: suspend () -> T,
): AsyncHolder<T> {
    val holder = remember(*keys) { AsyncHolder<T>() }
    LaunchedEffect(holder.tick, *keys) {
        val isRefresh = holder.refreshing
        // 刷新时保留旧内容，只有第一次加载和失败重试才回到加载态
        if (isRefresh) onRefresh?.invoke() else holder.state = Async.Loading

        try {
            holder.state = Async.Ready(loader())
        } catch (e: CancellationException) {
            // 被取消（换机型、或者用户又拉了一次）不算失败：
            // 必须原样抛出，否则下面会把状态写成 Failed，把新一轮的结果盖掉。
            // 也不能在这里复位 refreshing —— 新一轮已经在跑了，它跑完会自己复位。
            throw e
        } catch (e: Throwable) {
            holder.state = Async.Failed(e.message ?: e.toString())
        }
        holder.refreshing = false
    }
    return holder
}

/**
 * 三态渲染。成功态外面套一层 `PullToRefresh`，所以**所有用它的页面自动都有下拉刷新**，
 * 不用各页面自己包一遍。
 *
 * [scrollBehavior] 传顶栏的滚动行为：指示器要挂在大标题下面，而不是压在大标题上。
 */
@Composable
fun <T : Any> AsyncContent(
    holder: AsyncHolder<T>,
    contentPadding: PaddingValues,
    strings: Strings,
    scrollBehavior: ScrollBehavior? = null,
    content: @Composable (T) -> Unit,
) {
    when (val state = holder.state) {
        // 加载中 / 失败：直接铺满，但要避开顶栏和底栏
        is Async.Loading -> LoadingBox(Modifier.fillMaxSize().padding(contentPadding))
        is Async.Failed -> ErrorBox(
            message = state.message,
            onRetry = holder::retry,
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            strings = strings,
        )
        // 成功：padding 交给内容自己处理。列表必须能滚到顶栏底下去，
        // 大标题才会跟着收起；套在外层就没这个效果了。
        is Async.Ready -> PullToRefresh(
            isRefreshing = holder.refreshing,
            onRefresh = holder::refresh,
            modifier = Modifier.fillMaxSize(),
            topAppBarScrollBehavior = scrollBehavior,
            // MIUIX 默认是英文，这里换成本地化的四条
            refreshTexts = remember(strings) {
                listOf(
                    strings.refreshPull,
                    strings.refreshRelease,
                    strings.refreshRefreshing,
                    strings.refreshDone,
                )
            },
        ) {
            content(state.value)
        }
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        InfiniteProgressIndicator()
    }
}

@Composable
fun ErrorBox(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier, strings: Strings) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(onClick = null, modifier = Modifier.padding(24.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = strings.loadFailed,
                    style = MiuixTheme.textStyles.title4,
                    color = MiuixTheme.colorScheme.error,
                )
                Text(
                    text = message,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center,
                )
                TextButton(text = strings.retry, onClick = onRetry)
            }
        }
    }
}
