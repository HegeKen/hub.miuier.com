package com.miuier.hub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.miuier.hub.data.brandImageUrl
import com.miuier.hub.data.deviceImageUrl
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/**
 * 本文件里的组件都是参照 Updater-KMP 补的：它那边的排版习惯是
 * 「卡片统管内边距 + 主操作做成整行填充的 TextButton + 次级说明用 onSecondaryVariant」，
 * 这里把它抽成可复用件，省得每个页面各写一遍。
 */

/**
 * 主操作按钮：整行填充的 `TextButton` + 主色配色。
 *
 * 与 Updater-KMP 的「提交」按钮一致：HyperOS 的主操作是**文字型实心按钮**，
 * 撑满一行、圆角由 `ButtonDefaults` 决定，而不是默认尺寸的小按钮。
 */
@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        text = text,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        colors = ButtonDefaults.textButtonColorsPrimary(),
    )
}

/**
 * 卡片内的小节标题。
 *
 * 颜色用 `onSecondaryVariant`（MIUIX 里「次级说明文字」的角色）而不是 primary：
 * 小节标题是结构标记不是可点操作，用主色会和同卡片里的图标按钮抢注意力。
 */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.footnote1,
        color = colorScheme.onSecondaryVariant,
        modifier = modifier,
    )
}

/**
 * 状态卡片：图标 + 标题 + 说明，底色可换。
 *
 * 对应 Updater-KMP 的 `LoginCardView`（登录状态用不同底色的卡片提示），
 * 区别是这里不写死十六进制色值，取当前色板的 container 角色，明暗两套自动跟上。
 */
@Composable
fun StatusCard(
    icon: ImageVector,
    title: String,
    summary: String,
    modifier: Modifier = Modifier,
    container: Color = colorScheme.primaryContainer,
    content: Color = colorScheme.onPrimaryContainer,
) {
    Card(
        onClick = null,
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        colors = CardDefaults.defaultColors(color = container),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MiuixTheme.textStyles.body1,
                    color = content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (summary.isNotBlank()) {
                    Text(
                        text = summary,
                        style = MiuixTheme.textStyles.footnote1,
                        color = content,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * 分组卡片：**标题在卡片内**、用统一的小节标题样式（[SectionLabel]），内容跟在下面。
 *
 * 设置页每一块都用它。之前是外面挂 MIUIX 的 `SmallTitle`、里面另写一套，
 * 同一个页面出现两种小节标题（外观 / 语言 / 关于 在卡片外，技术栈 在卡片内）。
 *
 * [insideMargin] 与 [titlePadding] 是给「内容自带内边距」的卡片留的：MIUIX 的
 * `BasicComponent` / 下拉行要撑满整张卡片才有正确的按压反馈，这时卡片左右不留内边距，
 * 由标题自己补 `start = 16.dp` 去和行文字对齐。
 */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    insideMargin: PaddingValues = PaddingValues(16.dp),
    titlePadding: PaddingValues = PaddingValues(bottom = 10.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        onClick = null,
        insideMargin = insideMargin,
        modifier = modifier.fillMaxWidth(),
    ) {
        SectionLabel(title, modifier = Modifier.padding(titlePadding))
        content()
    }
}

/**
 * 「标签 / 值」两列信息行，用于机型信息、ROM 详情。
 *
 * 标签列宽度可调：默认 74dp 够放「设备代号」，而本机信息里的「系统版本」更长，
 * 窄了会把值挤到第二行。
 */
@Composable
fun MetaRow(label: String, value: String, labelWidth: Dp = 74.dp) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.body2,
            color = colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.width(labelWidth),
        )
        Text(
            text = value.ifBlank { "—" },
            style = MiuixTheme.textStyles.body2,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * 机型照片。数据里没有照片的机型（采样 24 款里有 1 款）会走到品牌兜底图，
 * 与网页端 `buildBrandImageUrl()` 的规则一致：Xiaomi → mi.svg、POCO / REDMI → 各自 png。
 * 兜底图本身也失败时就不再重试，停在占位底色上。
 *
 * 「机型详情」和「本机信息」两处共用：后者手上只有机型总表里的 `DeviceSummary`，
 * 没有 `DeviceDetail`，所以这里只收 device / brand 两个字段。
 */
@Composable
fun DeviceThumbnail(
    device: String,
    brand: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    var useBrandFallback by remember(device) { mutableStateOf(false) }
    val model = if (useBrandFallback) brandImageUrl(brand) else deviceImageUrl(device)

    Box(
        modifier = modifier
            .size(width = 96.dp, height = 104.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(6.dp),
            onError = { if (!useBrandFallback) useBrandFallback = true },
        )
    }
}
