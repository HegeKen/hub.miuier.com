package com.miuier.hub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.miuier.hub.ui.i18n.Strings
import com.miuier.hub.ui.i18n.fill
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 圆角小标签：区域 / 分支 / Android 版本用它 */
@Composable
fun Tag(
    text: String,
    container: Color = MiuixTheme.colorScheme.secondaryContainer,
    content: Color = MiuixTheme.colorScheme.onSecondaryContainer,
) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.footnote2,
        color = content,
        modifier = Modifier
            .background(container, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** 机型列表项：机型名 + 代号 + 品牌 + 包数量 */
@Composable
fun DeviceCard(
    name: String,
    codename: String,
    brand: String,
    romCount: Int,
    strings: Strings,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        // 内边距交给 Card 的 insideMargin（而不是自己 padding）：
        // 这样按压反馈的底色会盖住整张卡片，边距区域点下去也有反应
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MiuixTheme.textStyles.title4,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = codename,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (brand.isNotBlank()) Tag(brand)
                Text(
                    text = strings.romCountOnly.fill("count" to romCount),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = MiuixIcons.Demibold.ChevronForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                // onSurfaceVariantActions 是 MIUIX 给「列表右箭头」的角色色，
                // 比 onSurfaceVariantSummary 更淡，不跟右侧的文字标签抢注意力
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        }
    }
}
