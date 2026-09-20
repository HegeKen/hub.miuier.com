package com.miuier.hub

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.Density
import com.miuier.hub.data.AppLang
import com.miuier.hub.data.HubRepository
import com.miuier.hub.platform.LocalDevice
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Image
import java.io.File

/**
 * 无头渲染：把几个页面直接画成 PNG，**不需要模拟器也不需要窗口**。
 *
 * 用法：`./gradlew :composeApp:screenshot`
 * 产物：composeApp/build/screenshots 目录下的若干 png
 *（这里不能写通配路径：Kotlin 的块注释可嵌套，路径里的斜杠加星号会把注释再开一层）
 *
 * 它跑的是和 Android 端同一份 commonMain 界面代码，所以这些图能证明
 * 「接口能拉到 → JSON 能解析 → MIUIX 能渲染」整条链路是通的。
 *
 * 机型详情那张还会**真的点一下 ROM 卡片**：通过语义树
 * （`SemanticsActions.OnClick`）触发点击，再等更新日志回来才出图，
 * 所以「点击展开」这条交互本身也被验证到了，而不只是渲染出一个静态布局。
 */
fun main(args: Array<String>) {
    val outDir = File(args.firstOrNull() ?: "build/screenshots").apply { mkdirs() }

    listOf(
        "01-home" to Screen.Home,
        "02-devices" to Screen.Devices,
        "03-roms" to Screen.Roms,
        "04-settings" to Screen.Settings,
    ).forEach { (name, screen) -> renderTo(File(outDir, "$name.png"), screen) }

    // 多语言：同一页换语言的样子，阿拉伯语顺便验证 RTL 镜像
    renderTo(File(outDir, "07-devices-ja.png"), Screen.Devices, AppLang.JA)
    renderTo(File(outDir, "08-devices-ar.png"), Screen.Devices, AppLang.AR)
    renderTo(File(outDir, "09-settings-de.png"), Screen.Settings, AppLang.DE)

    // 语言选择器：点开 OverlayDropdownMenu 看弹层
    renderLanguageMenu(File(outDir, "11-language-menu.png"))

    // 悬浮按钮：先向下滚（此时按设计隐藏），再向上滚一点让它出现
    renderScrolledFab(File(outDir, "10-fab.png"))

    // 机型详情默认状态：分支**全部收起**（这是我们期望的默认行为）
    renderTo(File(outDir, "05-device-detail.png"), Screen.DeviceDetail(DEVICE_CODE, DEVICE_TITLE))
    // 展开一个分支 + 一条 ROM 之后的状态：下载链接与更新日志
    renderDetailWithExpandedRom(File(outDir, "06-device-detail-expanded.png"))

    // 本机信息：桌面端本身没有「本机机型」，注入一个假的来看这一页。
    // 顺带验证底栏的条件项 —— 匹配上之后底栏才是五项，否则是四项
    renderTo(File(outDir, "12-my-device.png"), Screen.MyDevice, local = FAKE_LOCAL_DEVICE)

    // 浅色主题：无头环境默认判成深色，这里显式锁一次浅色，
    // 确认 MIUIX 自带的两套色板都真的用上了（而不是只有深色能看）
    renderTo(File(outDir, "13-home-light.png"), Screen.Home, themeMode = ThemeMode.Light)
    renderTo(File(outDir, "14-my-device-light.png"), Screen.MyDevice, themeMode = ThemeMode.Light, local = FAKE_LOCAL_DEVICE)

    // 下拉刷新：合成一次真实的触摸下拉手势，看指示器出不出来
    renderPullToRefresh(outDir)
}

private const val DEVICE_CODE = "mist"
private const val DEVICE_TITLE = "REDMI Note 17 5G / POCO M8 Power 5G"

/**
 * 桌面预览用的假本机信息：代号取 [DEVICE_CODE]，所以能对上 hub 名单里的 `mist`。
 * 真机上这些字段全部来自 `android.os.Build` 与 `SystemProperties`。
 */
private val FAKE_LOCAL_DEVICE = LocalDevice(
    manufacturer = "Xiaomi",
    model = "25057RN09G",
    marketName = "REDMI Note 17 5G",
    product = DEVICE_CODE,
    codeName = DEVICE_CODE,
    androidVersion = "16",
    incremental = "OS3.0.305.0.WPUEUXM",
    rustVersion = "1.0.0.5.0",
)

private const val WIDTH_PX = 840
private const val HEIGHT_PX = 1800

private fun newScene(
    screen: Screen,
    heightPx: Int = HEIGHT_PX,
    lang: AppLang = AppLang.Default,
    local: LocalDevice? = null,
    themeMode: ThemeMode = ThemeMode.System,
) = ImageComposeScene(
    // 注意 width/height 是**像素**不是 dp：
    // 840x1800 px @2x = 420x900 dp —— 一个正常手机的竖屏尺寸。
    // （一开始写成 420x900 @2x，实际只有 210dp 宽，比任何手机都窄，文字全被挤断行。）
    width = WIDTH_PX,
    height = heightPx,
    density = Density(2f),
) { App(initial = screen, lang = lang, themeMode = themeMode, local = local) }

/** 推进若干帧等异步数据到位；render() 会推进一帧并触发重组 */
private fun ImageComposeScene.settle(rounds: Int = 40): Image {
    var image = render(0L)
    repeat(rounds) { i ->
        Thread.sleep(150)
        image = render((i + 1) * 16_000_000L)
    }
    return image
}

/**
 * 推进帧直到列表真的出现在语义树里（即异步数据已到位），最多等 [rounds] 轮。
 *
 * 比「固定推进 40 帧」可靠：接口偶尔要等好几秒，固定帧数会截到一张只有转圈的加载页
 * （曾把 `08-devices-ar` 截成一片空白）。判据用「语义树里出现了可滚动节点」——
 * 加载态 / 失败态都不带滚动动作，四个页面的内容区都是 LazyColumn。
 */
private fun ImageComposeScene.settleUntilLoaded(rounds: Int = 120): Image {
    var image = render(0L)
    repeat(rounds) { i ->
        if (hasScrollable()) return image
        Thread.sleep(150)
        image = render((i + 1) * 16_000_000L)
    }
    return image
}

/** 语义树里是否有可滚动节点（有就说明列表已经组合出来了，不是加载态） */
private fun ImageComposeScene.hasScrollable(): Boolean {
    val owner = semanticsOwners.firstOrNull() ?: return false
    var found = false

    fun visit(node: SemanticsNode) {
        if (found) return
        if (node.config.contains(SemanticsActions.ScrollBy)) {
            found = true
            return
        }
        node.children.forEach(::visit)
    }

    visit(owner.rootSemanticsNode)
    return found
}

private fun Image.writeTo(file: File) {
    val data = encodeToData() ?: error("PNG 编码失败：${file.name}")
    file.writeBytes(data.bytes)
    println("  写出 ${file.name}  (${file.length()} 字节)")
}

private fun renderTo(
    file: File,
    screen: Screen,
    lang: AppLang = AppLang.Default,
    local: LocalDevice? = null,
    themeMode: ThemeMode = ThemeMode.System,
) {
    val scene = newScene(screen, lang = lang, local = local, themeMode = themeMode)
    try {
        scene.settleUntilLoaded().writeTo(file)
    } finally {
        scene.close()
    }
}

private fun renderDetailWithExpandedRom(file: File) {
    val code = DEVICE_CODE
    // 展开后会多出下载 + 日志 + 高速下载三块，用更高的画布保证按钮进入组合（LazyColumn 只组合可见项）
    val scene = newScene(Screen.DeviceDetail(code, DEVICE_TITLE), heightPx = 3000)
    try {
        // 1) 先等设备详情 + 分支列表加载出来
        scene.settleUntilLoaded()

        // 2) 分支默认是收起的，先点开第一个分支——顺带把「分支展开」这条交互也验证了
        val branchName = runBlocking { HubRepository().device(code) }
            .branches.first().name.pick("zh")
        val branchClicked = scene.clickFirstWhere { it.contains(branchName) }
        println("  点击分支「$branchName」：${if (branchClicked) "成功" else "失败"}")
        scene.settle(15)

        // 3) 再点开一条 ROM 卡片
        val versionRegex = Regex("^(OS|V)\\d[\\w.]*$")
        val romClicked = scene.clickFirstWhere { text ->
            text.split(' ').any { versionRegex.matches(it) }
        }
        println("  ROM 卡片点击：${if (romClicked) "成功" else "失败"}")

        // 4) 更新日志是展开后才请求的，再等它回来
        scene.settle(40)

        // 5) 点「获取高速下载链接」：真实走一遍小米 OTA 接口，
        //    等于把 Kotlin 版的表单构造 / AES 加解密 / MirrorList 解析整条链路都验证了
        val hs = scene.clickFirstWhere { it.contains("获取高速下载") }
        println("  获取高速下载链接：${if (hs) "已点击" else "未找到按钮"}")
        scene.settle(40)

        // 6) 点一次「复制链接」：新的 Clipboard API 是 suspend 的，踩一脚确认这条路不崩
        //    （图标按钮没有文字，只能按 contentDescription 找）
        val copied = scene.clickFirstWhereDescription { it.contains("复制链接") }
        println("  复制链接：${if (copied) "已点击" else "未找到按钮"}")
        scene.settle(5)

        scene.settle(2).writeTo(file)
    } finally {
        scene.close()
    }
}

/**
 * 在语义树里找到第一个「文本满足条件且有 onClick」的节点并触发它。
 *
 * 为什么用语义树而不是模拟坐标点击：LazyColumn 只组合可见项，坐标很脆；
 * 而语义树是 Compose 自己暴露的可访问性结构，`OnClick` 正是 MIUIX Card 注册的那个回调。
 */
private fun ImageComposeScene.clickFirstWhere(predicate: (String) -> Boolean): Boolean =
    clickFirst(predicate) { node ->
        // SemanticsConfiguration 只有 contains + get（没有 getOrNull），所以先判存在再取
        if (node.config.contains(SemanticsProperties.Text)) {
            node.config[SemanticsProperties.Text].joinToString(" ") { it.text }.takeIf { it.isNotBlank() }
        } else {
            null
        }
    }

/**
 * 按 `contentDescription` 找节点并点击。
 * 图标按钮（复制 / 下载）没有文字，语义树里只有无障碍描述，只能这样找。
 */
private fun ImageComposeScene.clickFirstWhereDescription(predicate: (String) -> Boolean): Boolean =
    clickFirst(predicate) { node ->
        if (node.config.contains(SemanticsProperties.ContentDescription)) {
            node.config[SemanticsProperties.ContentDescription].joinToString(" ").takeIf { it.isNotBlank() }
        } else {
            null
        }
    }

private fun ImageComposeScene.clickFirst(
    predicate: (String) -> Boolean,
    keyOf: (SemanticsNode) -> String?,
): Boolean {
    val owner = semanticsOwners.firstOrNull() ?: return false
    var clicked = false

    fun clickOf(node: SemanticsNode): (() -> Boolean)? =
        if (node.config.contains(SemanticsActions.OnClick)) node.config[SemanticsActions.OnClick].action
        else null

    fun visit(node: SemanticsNode, inheritedClick: (() -> Boolean)?, inheritedKey: String?) {
        if (clicked) return
        val key = keyOf(node) ?: inheritedKey
        val click = clickOf(node) ?: inheritedClick

        if (click != null && key != null && predicate(key)) {
            click()
            clicked = true
            return
        }
        node.children.forEach { visit(it, click, key) }
    }

    visit(owner.rootSemanticsNode, null, null)
    return clicked
}

/**
 * 滚动一段距离（走 LazyColumn 暴露的 ScrollBy 语义动作）。
 * 正数 = 向下翻。
 */
private fun ImageComposeScene.scrollBy(dy: Float): Boolean {
    val owner = semanticsOwners.firstOrNull() ?: return false
    var done = false

    fun visit(node: SemanticsNode) {
        if (done) return
        val hasScrollAction = node.config.contains(SemanticsActions.ScrollBy)
        if (hasScrollAction && node.config[SemanticsActions.ScrollBy].action?.invoke(0f, dy) == true) {
            done = true
            return
        }
        node.children.forEach(::visit)
    }

    visit(owner.rootSemanticsNode)
    return done
}

/**
 * 「回到顶部」悬浮按钮只在**离开顶部、且不在向下滑**时出现，
 * 所以这里先向下滚（隐藏），再向上滚一点（出现）才截得到。
 */
private fun renderScrolledFab(file: File) {
    val scene = newScene(Screen.Devices)
    try {
        scene.settle(40)
        println("  向下滚：" + scene.scrollBy(4000f))
        // 小标题是 spring 动画淡入的，帧数不够会停在透明状态
        scene.settle(40)
        println("  向上滚：" + scene.scrollBy(-700f))
        scene.settle(40)
        scene.settle(2).writeTo(file)
    } finally {
        scene.close()
    }
}

/** 点开设置页的语言选择器，确认 OverlayDropdownMenu 的弹层能出来 */
private fun renderLanguageMenu(file: File) {
    val scene = newScene(Screen.Settings)
    try {
        scene.settle(40)
        val opened = scene.clickFirstWhere { it.contains(AppLang.ZH_HANS.label) }
        println("  打开语言选择器：$opened")
        scene.settle(150)
        // 诊断：菜单项里独有的文案是否出现在语义树里
        for (probe in listOf("English", "日本語", "Deutsch")) {
            println("  弹层里是否出现「$probe」：" + scene.hasText(probe))
        }
        scene.settle(2).writeTo(file)
    } finally {
        scene.close()
    }
}

/**
 * 下拉刷新：合成一次**真实的触摸下拉手势**，验证 MIUIX `PullToRefresh` 接得对不对 ——
 * 手势 → `onRefresh` → 清缓存 → 重新拉数据 → 指示器收回，整条链路都跑到。
 *
 * 必须用 `PointerType.Touch`：PullToRefresh 只认触摸，鼠标拖拽不触发。
 * 拖动要分多步 Move，一步到位的坐标跳跃不会被识别成拖动。
 */
private fun renderPullToRefresh(outDir: File) {
    val scene = newScene(Screen.Home)
    try {
        scene.settleUntilLoaded()

        val x = 420f
        var y = 900f
        scene.sendPointerEvent(
            eventType = PointerEventType.Press,
            position = Offset(x, y),
            type = PointerType.Touch,
        )
        repeat(10) { i ->
            y += 70f
            scene.sendPointerEvent(
                eventType = PointerEventType.Move,
                position = Offset(x, y),
                type = PointerType.Touch,
            )
            scene.render((i + 1) * 16_000_000L)
        }
        // 松手前先出图：此时指示器应该已经拉出来并显示「松开刷新」
        scene.settle(4).writeTo(File(outDir, "15-pull-to-refresh.png"))

        scene.sendPointerEvent(
            eventType = PointerEventType.Release,
            position = Offset(x, y),
            type = PointerType.Touch,
        )
        // 松手后逐帧看语义树：只要出现过「正在刷新 / 刷新完成」，
        // 就说明 onRefresh 真的被调用了 —— 纯粹动画弹回去的话这两条文案都不会出现。
        //
        // 注意无头场景的动画时钟由 render(nanos) 决定，而 settle() 每轮只推 16ms，
        // 慢到根本追不上刷新状态，所以这里自己按 100ms 步长推。
        // 只做断言、不出图：文案可能还在语义树里、但指示器已经划出屏幕，
        // 那样截出来的图看着跟普通首页一样，会误导人。
        var sawRefreshing = false
        repeat(60) { i ->
            scene.render(i * 100_000_000L)
            if (listOf("正在刷新", "刷新完成").any { scene.hasText(it) }) sawRefreshing = true
            Thread.sleep(50)
        }
        println("  下拉刷新：松手触发 onRefresh = $sawRefreshing")

        // 刷完再等数据回来，确认列表还是正常渲染的（这张图和普通首页应当一致）
        scene.settle(30)
        scene.settle(1).writeTo(File(outDir, "16-pull-refreshed.png"))
    } finally {
        scene.close()
    }
}

/** 语义树里是否存在包含该文案的节点（用于诊断弹层有没有渲染出来） */
private fun ImageComposeScene.hasText(text: String): Boolean {
    val owner = semanticsOwners.firstOrNull() ?: return false
    var found = false

    fun visit(node: SemanticsNode) {
        if (found) return
        if (node.config.contains(SemanticsProperties.Text) &&
            node.config[SemanticsProperties.Text].any { it.text.contains(text) }
        ) {
            found = true
            return
        }
        node.children.forEach(::visit)
    }

    visit(owner.rootSemanticsNode)
    return found
}
