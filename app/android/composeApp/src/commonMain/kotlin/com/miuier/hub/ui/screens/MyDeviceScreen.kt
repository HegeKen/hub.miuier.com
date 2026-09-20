package com.miuier.hub.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.miuier.hub.HubAppState
import com.miuier.hub.data.DeviceSummary
import com.miuier.hub.platform.LocalDevice
import com.miuier.hub.ui.components.DeviceThumbnail
import com.miuier.hub.ui.components.MetaRow
import com.miuier.hub.ui.components.StatusCard
import com.miuier.hub.ui.components.Tag
import com.miuier.hub.ui.i18n.UiText
import com.miuier.hub.ui.i18n.fill
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 本机信息：**只显示当前这一台机器**的档案与刷机包，不提供机型列表。
 *
 * 底栏这一项是条件出现的：只有 `Build.DEVICE` / `Build.PRODUCT` 能对上
 * `/v3/index.json` 里的机型代号时才会挂上来（见 `HubAppState.resolveLocalDevice`），
 * 所以进到这里就说明一定匹配成功过。列表本体复用机型详情页的
 * [DeviceDetailList]，只在最前面多插一张本机卡片。
 */
@Composable
fun MyDeviceScreen(app: HubAppState, contentPadding: PaddingValues, scrollBehavior: ScrollBehavior) {
    // 两个都为 null 时底栏不会出现这一项。真走到这里（例如无头截图直接指定了这一页
    // 但没注入假的本机信息）就什么都不画，而不是抛异常或显示半张空卡。
    val local = app.local ?: return
    val summary = app.localSummary ?: return
    val l10n = app.ui

    DeviceDetailList(
        app = app,
        code = summary.device,
        contentPadding = contentPadding,
        scrollBehavior = scrollBehavior,
        leading = { MyDeviceCard(local, summary, l10n) },
        // 本机卡片本身就是「机型档案 + 本机硬件」的合并版，不再叠一张通用机型卡
        showDeviceCard = false,
    )
}

/** 本机卡片：本机硬件（来自 Android 系统）+ hub 收录情况（来自接口） */
@Composable
private fun MyDeviceCard(local: LocalDevice, summary: DeviceSummary, l10n: UiText) {
    val strings = l10n.strings

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 状态条：借用 Updater-KMP 登录状态卡的形态（图标 + 标题 + 说明的整块底色）
        StatusCard(
            icon = MiuixIcons.Demibold.Ok,
            title = strings.myDeviceFound,
            summary = strings.myDeviceSummary.fill("count" to summary.romCount),
        )

        Card(
            onClick = null,
            insideMargin = PaddingValues(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DeviceThumbnail(
                    device = summary.device,
                    brand = summary.brand.firstOrNull(),
                    contentDescription = summary.name.pick(l10n.dataLang),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = summary.name.pick(l10n.dataLang),
                            style = MiuixTheme.textStyles.title3,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(8.dp))
                        Tag(
                            text = strings.thisDevice,
                            container = MiuixTheme.colorScheme.primaryContainer,
                            content = MiuixTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    // 本机拿到的营销名 / 硬件型号：hub 的机型名是站点译名，
                    // 这里补上「这台机器自己报的名字」，对不上时一眼能看出来
                    Text(
                        text = listOf(local.displayName, local.codeName)
                            .filter { it.isNotBlank() }
                            .distinct()
                            .joinToString(" · "),
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MiuixTheme.colorScheme.dividerLine)
            Spacer(Modifier.height(6.dp))

            // 标签列留宽一点：本机这些词条（系统版本 / Android 版本）比「设备代号」长
            MetaRow(strings.systemVersion, local.incremental, labelWidth = 92.dp)
            MetaRow(strings.androidVersion, local.androidVersion, labelWidth = 92.dp)
            if (local.rustVersion.isNotBlank()) {
                MetaRow(strings.runtimeVersion, local.rustVersion, labelWidth = 92.dp)
            }
            MetaRow(strings.deviceCode, summary.device, labelWidth = 92.dp)
            MetaRow(strings.brand, summary.brand.joinToString(" / "), labelWidth = 92.dp)
            MetaRow(strings.supports, summary.supports.joinToString(", "), labelWidth = 92.dp)
        }
    }
}
