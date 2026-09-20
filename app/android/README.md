# MiROMS HUB · Android

`hub.miuier.com` 的 Android 客户端，**Compose Multiplatform + MIUIX**。

数据与网页端同源：直接消费 data 仓库导出的 v3 JSON 接口
（`https://api.miuier.com/api/v3`），不额外维护一份后端。

---

## 技术栈

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| Kotlin | 2.4.20 | MIUIX 0.9.4 要求 |
| Compose Multiplatform | 1.12.0 | MIUIX 0.9.4 要求 |
| MIUIX | 0.9.4 | 小米 HyperOS 设计语言。用到三个制品：`miuix-ui`（主组件）、`miuix-icons`（780 个图标）、`miuix-preference`（`OverlayDropdownMenu` 在它里面，不在 ui 里） |
| AGP | 9.4.1 | 需要 Gradle ≥ 9.5（**不是** 9.4.1：AGP 9 用到了新版 Gradle 的 ProjectType API） |
| Gradle | 9.7.1 | 见 `gradle/wrapper/gradle-wrapper.properties` |
| compileSdk | 37 | MIUIX 0.9.4 的 AAR metadata 强制要求 ≥ 37（Android 17） |
| Ktor Client | 3.6.0 | Android 用 OkHttp 引擎，桌面用 CIO |
| kotlinx.serialization | 1.11.0 | v3 JSON 反序列化 |
| JDK | 21 | AGP 9 支持范围；本机默认 JDK 24 未验证 |

> MIUIX 的版本约束是硬性的：它的 POM 明确依赖 `kotlin-stdlib 2.4.20` 与
> `compose foundation 1.12.0`，升 Kotlin 或 Compose 时要同时确认 MIUIX 有对应版本。
> 查最新版本：<https://repo1.maven.org/maven2/top/yukonga/miuix/kmp/miuix-ui/maven-metadata.xml>

## 目录结构

```
app/android/
├── composeApp/
│   └── src/
│       ├── commonMain/kotlin/com/miuier/hub/
│       │   ├── App.kt                    # 主题 + Scaffold + 底部导航（含条件项）+ 页面分发
│       │   ├── AppState.kt               # 导航栈、语言、主题模式、本机机型匹配结果
│       │   ├── data/
│       │   │   ├── Models.kt             # v3 JSON 的 @Serializable 数据模型
│       │   │   ├── HubApi.kt             # Ktor 客户端
│       │   │   ├── HubRepository.kt      # 内存缓存
│       │   │   └── LocalDeviceMatch.kt   # 本机代号 ↔ hub 机型名单
│       │   ├── platform/                 # expect/actual（系统返回键、下载、AES、剪贴板、本机硬件信息）
│       │   └── ui/
│       │       ├── Labels.kt             # 区域 / 运营商显示名（词条对齐 web 的 i18n）
│       │       ├── components/           # 三态加载、卡片、标签、按钮等复用件
│       │       ├── theme/Theme.kt        # 主题（照搬 Updater-KMP，用 MIUIX 自带色板）
│       │       └── screens/              # 首页 / 机型 / 机型详情 / 本机信息 / 刷机包 / 设置
│       ├── androidMain/                  # MainActivity、清单、窗口主题、各密度图标
│       └── desktopMain/                  # 桌面入口 + 无头截图（仅用于本机预览）
├── scripts/make_icons.py                 # 从 scripts/favion.png 生成启动图标
├── scripts/favion.png                    # Android 侧的 logo 源图（原始分辨率）
├── scripts/make_locales.py               # 从网页端语言包生成 18 种语言的资源
├── gradle/libs.versions.toml             # 版本目录（版本矩阵集中在这里）
└── gradlew / gradle/wrapper/
```

## 运行

**Android**（需要 Android SDK；本机路径写在 `local.properties` 的 `sdk.dir`）：

```bash
cd app/android
./gradlew :composeApp:assembleDebug          # 产物：composeApp/build/outputs/apk/debug/
./gradlew :composeApp:installDebug           # 装到已连接的设备/模拟器
```

首次构建会下载 Gradle 发行包与全部依赖，耗时较长。

**两个已踩过的坑**（都在仓库里处理好了，换机器时留意）：

- AGP 9 起 `com.android.application` 默认不再允许与 `kotlin-multiplatform` 插件共存。
  `gradle.properties` 里的 `android.builtInKotlin=false` / `android.newDsl=false` 是
  AGP 自己给出的兼容开关，用来继续沿用官方 CMP 模板的单模块结构（`composeApp`）。
  AGP 标注为「临时绕过」，将来要拆成 `com.android.kotlin.multiplatform.library`（共享库）
  + `com.android.application`（应用壳）两个模块。
- Gradle 发行包会 302 跳到 `release-assets.githubusercontent.com`，
  该域名不在 Oracle JDK 的 cacerts 里（curl 走系统信任库所以没事），
  于是 `./gradlew` 首次下载会报 `PKIX path building failed`。
  Maven 仓库（repo1.maven.org / dl.google.com）不受影响，依赖解析正常。
  如果撞上：用 curl 下载发行包，解到 `~/.gradle/wrapper/dists/gradle-9.7.1-bin/1w1c7tv4s851m17nbqdsro2tv/`，
  再 `touch gradle-9.7.1-bin.zip.ok` 即可（目录名是 distributionUrl 的 base36(MD5)）。

**桌面预览**（不想开模拟器时看 UI 最快的方式，与 Android 共用同一份 commonMain 界面代码）：

```bash
./gradlew :composeApp:desktopRun
```

App 左侧无导航，窗口按手机竖屏比例（400×860）打开。

> 项目里其实有**两个**能跑桌面的 task，入口要各配一次：
> `desktopRun` 是 KMP 给 jvm target 生成的（IDE 也用它跑 main），
> `run` 是 `compose.desktop.application` 那套（打包用的 `createDistributable` /
> `packageUberJarForCurrentOS` 也属于它）。
> `compose.desktop.application { mainClass = … }` **只管后者**，
> 所以 `desktopRun` 还要在 `jvm("desktop") { mainRun { mainClass.set(…) } }` 里再声明一次 ——
> 少了这三行，`./gradlew :composeApp:desktopRun` 会直接报
> `No main class specified and classpath is not an executable jar`。

**无头截图**（连窗口都不用开，直接出 PNG，用来验证界面真能画出来）：

```bash
./gradlew :composeApp:screenshot     # → composeApp/build/screenshots/01-home.png 等
```

产出的 16 张图里，有几张是**真的操作过后**才截的，不只是静态布局：

| 图 | 验证的东西 |
| --- | --- |
| `06-device-detail-expanded` | 点分支 → 点 ROM → 拉更新日志 → 取高速下载链接 → 点复制链接 |
| `11-language-menu` | 点开语言弹层，当前项打勾 |
| `12/14-my-device` | 注入假本机信息，验证底栏条件项（匹配上才是五项） |
| `15/16-pull-*` | 合成触摸下拉手势 → 松手真的触发刷新 |

它跑的是同一份 commonMain 界面代码，所以这几张图能证明「接口能拉到 → JSON 能解析 →
MIUIX 能渲染」整条链路是通的。注意 `ImageComposeScene` 的宽高是**像素**：
`840×1800 @2x = 420×900 dp`，写成 `420×900 @2x` 就只有 210dp 宽，比任何手机都窄。

等异步数据的方式是**等语义树里出现可滚动节点**（`settleUntilLoaded`），而不是固定推进 N 帧：
接口偶尔要等好几秒，固定帧数会截到一张只有转圈的加载页（`08-devices-ar` 就这么被截空过一次）。

> `local.properties` 不入库（见 `.gitignore`）。命令行构建也可以改用
> `ANDROID_HOME` 环境变量。

## 数据来源

| 路径 | 用途 |
| --- | --- |
| `/v3/index.json` | 机型总表（331 款）→ 机型列表页 |
| `/v3/devices/{code}.json` | 机型详情（分支 + 全部 ROM）→ 机型详情页 |
| `/v3/roms/index.json` | 系统大版本汇总 → 刷机包首页 |
| `/v3/roms/{OS}.json` | 单个大版本的全部 ROM |
| `/v3/stats.json` | 首页统计与最近更新 |

图片（`<code>.png`）在站点根域 `https://api.miuier.com/images/`，本骨架**尚未接**。

## 界面骨架（都用 MIUIX 组件搭）

| 位置 | 组件 | 说明 |
| --- | --- | --- |
| 底部导航 | `FloatingNavigationBar` | 悬浮胶囊样式。注意 0.9.4 的 `FloatingNavigationBarItem` **只画图标**，`label` 仅作为 `contentDescription`（经典 `NavigationBar` 才画文字）。常规四项（首页 / 机型 / 刷机包 / 设置），匹配到本机机型时中间插入「本机信息」变成五项 |
| 顶部标题 | `TopAppBar` + `MiuixScrollBehavior` | 大标题：展开时是大字，往上滚收进栏里、小标题淡入（`collapsedFraction ≥ 1/3` 时出现） |
| 回到顶部 | `FloatingActionButton` | 挂在 `Scaffold` 的 `floatingActionButton`，向下滑时滑出屏幕、向上滑回来、已经在顶部时不显示 |
| 语言选择 | `OverlayDropdownMenu` | 18 种语言做成下拉弹层，当前项打勾 |
| 下拉刷新 | `PullToRefresh` | 包在 `AsyncContent` 的成功态外面，所以**凡是走三态加载的页面都有**，不用各页自己包 |
| 主操作 | `ActionButton`（`ui/components/Widgets.kt`） | 整行填充的主色 `TextButton`，如「获取高速下载链接」 |
| 状态提示 | `StatusCard` | 图标 + 标题 + 说明的整块底色卡片（`primaryContainer`），「本机信息」页用它提示已识别到本机机型 |

大标题能收起的前提是**列表要能滚到顶栏底下去**，所以：

- `AsyncContent` 把 Scaffold 的 `contentPadding` 单独传进来 —— 加载中/失败态用它避开顶栏底栏，
  成功态交给内容自己处理（列表用 `contentPadding` 而不是 `Modifier.padding`）。
- 各页面的 `LazyColumn` 都挂 `Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)`，
  搜索框、品牌页签这些原本固定在列表上方的控件也挪进列表当 item，否则它们不跟着滚，会留出空档。

悬浮按钮的方向判断**没有用 `snapshotFlow`**：无头渲染时它的收集可能落在别的线程，
而布局/绘制在主线程，Compose 会抛 `Detected multithreaded access to SnapshotStateObserver`。
改成自己实现一个 `NestedScrollConnection` 收滚动事件算方向（见 `ui/components/ScrollToTop.kt`）——
nestedScroll 的回调本来就在主线程的滚动手势里跑。Android 上两种写法都没问题，纯粹是无头渲染的坑。

## 下拉刷新

首页 / 机型 / 机型详情 / 本机信息 / 刷机包 / 某个大版本 六个数据页都能下拉刷新。
实现集中在 `ui/components/Async.kt`，各页面只多传一个参数：

```kotlin
val holder = rememberAsync("stats", onRefresh = app.repo::invalidate) { app.repo.stats() }
AsyncContent(holder, contentPadding, l10n.strings, scrollBehavior) { stats -> … }
```

几处不显然但必须这么写的地方：

- **`onRefresh` 里要清缓存**。`HubRepository` 是内存缓存，不清的话「刷新」会直接命中缓存、
  界面一点变化都没有。`invalidate()` 一次清掉全部缓存 —— 手动刷新时用户明确表达了
  「要最新的」，为省几个请求去维护「哪个缓存归哪个页面」更容易出错。
- **`AsyncHolder.refresh()` 必须同步置位**。这是 `PullToRefresh` 的约定：指示器收回进入刷新态时
  `isRefreshing` 还是 false 的话，刷新会被当成已完成、直接播完成动画。
  同时它和 `retry()` 分开：刷新**保留已加载的内容**（只有指示器在转），失败重试才回到骨架态。
- **`rememberAsync` 里不能用 `runCatching`**。它会把 `CancellationException` 一起吞掉，
  于是「换机型 / 又拉了一次」导致协程被取消时，旧那轮会把状态写成 `Failed`、把新一轮的结果盖掉。
  现在是显式 catch，`CancellationException` 原样抛出。这个坑原来的代码就有，顺手一起修了。
- **刷新文案要本地化**。MIUIX 默认给的是英文四条（"Pull down to refresh" …），
  这里换成 `Strings` 里的四条（18 种语言），否则一个 18 语言的 App 会在这处露怯。
- **设置页没有下拉刷新**：它不加载任何接口数据，加上去只会是个没有反馈的假动作。

验证方式是**合成真实触摸手势**（`16-pull-refreshed.png` / `15-pull-to-refresh.png`）：
截图脚本向场景发 Press → 多步 Move → Release，其中 `PointerType` 必须是 `Touch`
（鼠标拖拽不触发），拖拽也要分多步（一步到位的坐标跳跃不会被识别成拖动）。
松手后逐帧读语义树，出现「正在刷新」即说明 `onRefresh` 真的被调用了 —— 只是动画弹回去的话这条文案不会出现。

> 无头场景的动画时钟由 `render(nanos)` 决定，而 `settle()` 每轮只推进 16ms，
> 慢到追不上刷新状态，所以那段探测自己按 100ms 步长推进。

## 数据更新时间显示为 GMT+8

接口的 `stats.generatedAt` 是 **UTC**（`2026-09-20T14:41:54.111Z`），首页原来直接
`take(19).replace('T', ' ')`，等于把 UTC 当本地时间显示 —— 数据明明是 22:41 生成的，界面上写 14:41。
现在统一经 `data/TimeFormat.kt` 的 `utcToGmt8()` 转成东八区，并在界面上标出 `GMT+8`。

没有为此引入 `kotlinx-datetime`：偏移固定（东八区没有夏令时）、输入格式由我们自己的导出脚本决定，
只需要「加 8 小时 + 处理进位」这点算术。闰年 / 跨月 / 跨年都覆盖了，
用 2023–2027 逐日逐时的 10956 个用例和 `datetime` 对拍过（0 处不一致），解析不出来就原样返回。

## 主题与配色：照搬 Updater-KMP

主题不用「种子色 + Monet 推导」，而是**直接用 MIUIX 自带的明暗两套色板**，
与 [Updater-KMP](https://github.com/YuKongA/Updater-KMP) 的 `AppTheme` 逐字一致：

```kotlin
MiuixTheme(colors = if (dark) darkColorScheme() else lightColorScheme()) { … }
```

原来的写法是 `ThemeController(mode, keyColor = BrandPrimary /* #2655FF */)`：
给出一个种子色，由 material-color-utilities 推导整套配色。种子色推出来的 primary /
container 会随种子漂移，和 HyperOS 原生控件的观感对不上；而机型列表、卡片这些
本来就用 MIUIX 组件搭，色板跟着组件自带的那套走才是一致的。
`ThemeMode.System / Light / Dark` 仍然保留，只是最终只落成一个布尔值交给 `AppTheme`。

界面上**不写死任何十六进制色值**（除 `Brand.kt` 已删除外，其余一律走
`MiuixTheme.colorScheme.*` 的角色）：色板一换，深浅两套自动跟上。

组件与按钮同样参照 Updater-KMP 补了几处（都在 `ui/components/`）：

- **卡片内边距交给 `Card(insideMargin = …)`**，而不是在内容里自己 `padding`：
  按压反馈的底色会盖住整张卡片，边距区域点下去也有反应。
- **主操作 = 整行填充的主色 `TextButton`**（`ActionButton`）：HyperOS 的主操作是文字型实心按钮，
  不是默认尺寸的小按钮。次级操作（复制 / 下载）一律是 `IconButton`，图标色用
  `onSurface` 而不是 `primary`，免得跟主操作抢注意力。
- **列表右箭头用 `onSurfaceVariantActions`**：MIUIX 给「列表动作」的角色色，
  比 `onSurfaceVariantSummary` 更淡，不与右侧文字标签打架。
- **小节标题用 `onSecondaryVariant`**（`SectionLabel`）：小节标题是结构标记不是可点操作。
- **`SectionCard`**：分组卡片 = 卡片内的统一小标题 + 内容。设置页每一块都用它，
  标题样式只有一处定义（见下面「设置页」）。

`composeApp/build/screenshots/13-home-light.png` 与 `14-my-device-light.png`
就是显式锁浅色截的 —— 用来确认两套色板都真的用上了，而不是只有深色能看。

## 本机信息（条件出现的第五个 tab）

底栏会**多出一项「本机信息」**，但只在本机机型出现在 hub 名单里时才出现；
没匹配上时底栏就是原来的四项，用户感知不到这个功能存在。

判定链路（`AppState.kt` + `platform/LocalDevice.kt` + `data/LocalDeviceMatch.kt`）：

1. `androidMain` 读 `Build.DEVICE` / `Build.PRODUCT` / `Build.MANUFACTURER` / `Build.VERSION.*`，
   再反射 `SystemProperties` 取 `ro.product.marketname`、`rust.runtime_version`
   （与 Updater-KMP 的 `platform/Device.android.kt` 同一套读法；桌面端 `actual` 恒为 null）；
2. 拿 `/v3/index.json` 的机型名单比对代号 —— 先精确匹配，再放宽到
   「名单代号 = 本机代号 + `_` + 后缀」，因为小米给运营商 / 区域变体单独建条目
   （本机 `cancro`、名单里另有 `cancro_lte_ct`）。**不做包含式模糊匹配**：
   `cmi`、`umi` 这种短代号一旦模糊匹配，会把一堆不相关机型也认成本机；
3. 匹配上才把 `Screen.MyDevice` 插进底栏（`HubAppState.rootScreens`）。

名单请求和机型列表页共用同一次 —— `HubRepository.devices()` 缓存未命中时会并发进来两处，
所以那里加了一把 `Mutex`，避免冷启动打两次接口。

页面**只显示当前这一台机器**，没有机型列表：

- 顶部是 `StatusCard`（已识别到本机机型 + hub 收录了多少个包）；
- 中间是本机卡片：机型照片 + hub 机型名 + `本机` 角标，下面依次是**系统版本**
  （`Build.VERSION.INCREMENTAL`，如 `OS3.0.305.0.WPUEUXM`）、**Android 版本**、
  **运行时版本**（`rust.runtime_version`，HyperOS 3 起才有，拿不到就不显示这一行）、
  设备代号、品牌、支持系统；卡片副标题同时给出**机器自己报的营销名 / 型号**，
  和 hub 的译名对不上时一眼能看出来；
- 下面是该机型的全部分支与 ROM —— 直接复用机型详情页的同一个 `DeviceDetailList`，
  只多传一张前置卡片（`leading`）并关掉通用的机型档案卡（`showDeviceCard = false`），
  几百行列表不抄第二遍。

> 无头截图里 `12-my-device.png` / `14-my-device-light.png` 用注入的假 `LocalDevice`
> 来渲染这一页（桌面端本身没有「本机机型」），同时也验证了底栏的条件项：
> 匹配上之后是五项，其余截图都是四项。

## 机型详情页

**分支默认全部收起**。一个机型最多 7 个分支、单个分支上百条 ROM，全展开的话一进页面
就是几千行；所以 `expandedBranches` 初始为空集合，点分支标题才展开那一段。

**信息卡片带机型照片**：`<device>.png`（`https://api.miuier.com/images/`）。
数据里没有照片的机型走品牌兜底，规则与网页端 `buildBrandImageUrl()` 一致：
Xiaomi → `mi.svg`、POCO → `POCO.png`、REDMI → `REDMI.png`；兜底图也失败就停在占位底色。
图片用 Coil 3（`coil-compose` + `coil-network-ktor3` + `coil-svg`），
这三个包都通过 `META-INF/services/coil3.util.*ServiceLoaderTarget` 自动注册，不需要手写初始化。

**点任意一条 ROM 会展开**，显示三组信息（再点收起）：

- **下载**：卡刷包 / 线刷包各自的文件名，右侧是 MIUIX 的
  [IconButton](https://compose-miuix-ui.github.io/miuix/zh_CN/components/iconbutton)（复制 / 下载）。
  复制走 `platform/Clipboard.kt` 这组 expect/actual：Compose 已弃用同步的
  `LocalClipboardManager`，新 API 是 `Clipboard.setClipEntry`（suspend），而 `ClipEntry`
  的构造分平台（Android `ClipData` / 桌面 `StringSelection`），所以两头各写一个 actual。
  无头截图下 AWT 取不到系统剪贴板（会抛 `HeadlessException`），桌面 actual 里显式跳过。
  点下载会把任务交给**系统下载器**（Android 的 `DownloadManager`：通知栏进度、暂停续传、
  失败重试都是系统行为），而不是在应用内自己下 —— ROM 包动辄 4~5GB，切后台还得能继续。
  Android 10 以下往公共下载目录写文件需要 `WRITE_EXTERNAL_STORAGE`，
  已在清单里用 `maxSdkVersion="28"` 声明并在运行时按需申请；桌面端没有等价物，退化成用浏览器打开。
  地址与网页端完全一致 —— `<CDN 基址>/<版本号>/<文件名>`，版本号取数据里的 `rom.miui`，
  实测返回 200（线刷包单个可达 4~5GB，所以是交给系统下载管理器 / 浏览器，而不是自己下载）。
- **更新日志**：`/v3/logs/<device>/<region>/<version>.json`，按语种挑中/英两套渲染。
  只有部分 ROM 导出过日志，接口会给 404，这时显示「该版本没有更新日志」而不是报错。

### 高速下载（superota / ultimateota）

日志下方的「获取高速下载链接」按钮会**按需**请求小米 OTA 接口
（`update.miui.com/updates/miotaV3.php`），用返回的 `MirrorList` 拼出超级 OTA 直链。
复刻了 `data/scripts/fetch_changelog.py` 那一套：HyperOSForm 表单 → AES-128-CBC → base64 → URL 编码。

实测结论（都写进代码注释了，避免以后重新踩）：

- **`d` 必须是分支代号**（`branch.id`，如 `mist_eea_global`），只给机型代号接口什么都不返回。
  v3 数据里没有单独的 `code` 字段，但 `branch.id` 与数据库 `roms.code` 逐条对得上。
- **直链格式是 `<mirror>/<版本号>/<文件名>`**，且文件名**必须带签名**
  （`?t=<时间戳>&s=...`，取自 `LatestRom.filename`）。少一段目录、或换成 `CurrentRom`
  的未签名名字，都会 403。实测这样拼出来返回 **206 application/zip**。
- **接口只对「该分支最新版」签发签名**：请求旧版本时它返回的是「能升到的最新版」，
  旧版本自己的文件名没有签名。这种情况 UI 会直说，并给出最新版版本号，而不是假装成功。
- **`Accept` 头不能是 `application/json`**：小米这个接口对它会回 **406**
  （`*/*` 或带通配的列表才 200）。所以 OTA 用了一个**不装 ContentNegotiation** 的独立
  HttpClient —— Ktor 装了它就会自动加 `Accept: application/json`。
- **表单必须 `encodeDefaults = true`**：kotlinx.serialization 默认会省掉「值等于默认值」的
  字段，而这张表单的意义就在于把一堆固定字段原样发过去；省掉之后服务端认不出请求，
  只会回一个没有 `LatestRom` 的响应。

AES 用到 `javax.crypto`，commonMain 里没这个东西；Android 和桌面都是 JVM，
所以实现放在中间的 `jvmCommonMain` 源集里，两边 `dependsOn`，只写一份。

两个实现细节：

- 日志是**展开后才请求**的（`ChangelogSection` 只在展开分支里进入组合）。
  一个分支上百条 ROM，不能一进页面就把日志全拉一遍；`HubRepository` 还会按版本缓存，
  重复展开同一条不会二次请求。
- `LazyColumn` 给了稳定 key（`Entry.key`）。折叠分支会改变列表下标，
  没有 key 的话「已展开」的状态会跟着下标跑到别的 ROM 上。

## 应用图标

`scripts/make_icons.py` 从 `scripts/favion.png` 生成：

```bash
python3 scripts/make_icons.py          # 需要 Pillow
```

产出各密度的旧版图标 + 圆形版 + 自适应图标（`mipmap-anydpi-v26`）与 108dp 前景。

**只做缩放，不动源图内容**：图标就是 logo 按各密度尺寸等比重采样（LANCZOS）——
不改颜色、不抠字标。曾经有一版会先把 `MiROMS HUB` 字标抹掉（理由是尺寸小时认不出来），
那样虽然干净，但等于把 logo 的细节改掉了；实测新 logo 的字标在 48dp 下仍然认得出，不必抹。

**源图保持原始分辨率**（5715×5715，2.0MB）。曾经把它降到 1024 再生成：
后果其实很小（192px 下平均差 0.10/255），但 432px 前景有约 0.9% 的像素在边缘处会软化 ——
原图就在手边，没必要多降一次。

网页端 `app/web/public/favion.png` 仍是**上一版 logo**，两边独立 —— 要一起换的话，
把新图放进 `app/web/public/` 再重跑 `app/web/scripts/make-favicon.py`。

> 已知可改进：自适应图标的 `monochrome` 目前在复用前景图，而前景区是不透明的圆角方形，
> 所以「主题图标」渲染出来只是一个纯色块。要让它有意义，得单独出一张
> 「把中间白带挖成透明」的蒙版层。

## 已知的上游问题：MIUIX 0.9.4 的两个箭头字形是坏的

`MiuixIcons.Demibold.ExpandMore` / `ExpandLess` 渲染出来**不是箭头**——把它们单独放大到
64dp 也是几段断开的折线加一个孤点。`ChevronForward` / `ChevronBackward` 则完全正常。
所以展开/收起的指示箭头改用 `ChevronForward` 旋转 90°（见 `DeviceDetailScreen.ExpandChevron`）。
升级 MIUIX 后可以回头确认这两个字形是否修好。

## 与 web 端的关系

界面是重新设计的，不是把网页端照搬到手机上：

- **布局**：web 用宽表格横向铺开（`#/版本号/Android/区域/发布时间/…`），
  手机窄屏照抄会挤成一团。这里改成卡片：机型名 + 代号在主行，版本号、Android、
  日期、补丁日期用圆角标签分行排。
- **导航**：web 是顶部横向导航 + 面包屑；这里换成底部导航（首页 / 机型 / 刷机包 / 设置）
  + 顶栏返回，符合手机的单手操作习惯。
- **筛选**：web 的机型页有区域 / 运营商 / Android / 支持系统四组筛选器（十几行胶囊）。
  手机上四组筛选会吃掉整屏，这里先只保留「搜索 + 品牌」两个最常用的维度。
- **沿用 web 的部分**：区域与运营商的译名（词条与 `app/web/i18n/locales/zh-cn.ts`、
  `en-us.ts` 一致）、机型卡片「名称 + 代号 + 品牌角标 + 右箭头」的信息层级。
- **不再沿用 web 的品牌主色**：配色改为 MIUIX 自带色板（见上面的「主题与配色」），
  与 Updater-KMP 保持一致。

## 多语言（18 种，与网页端一致）

语言列表对齐网页端 `nuxt.config.ts` 的 `i18n.locales`：简中 / 繁中 / English / 日本語 /
한국어 / Русский / Українська / Polski / Deutsch / Français / Italiano / Español /
Português / Türkçe / Bahasa Indonesia / Tiếng Việt / ไทย / العربية。

资源由 `scripts/make_locales.py` 生成到 `ui/i18n/Locales.kt`：

- **能对上的词条直接复用网页端语言包**（下载 / 更新日志 / 品牌 / 支持系统 / 关于 /
  免责声明 / 作者主页 / 搜索框 …），以及**全部区域名与运营商名**（20 + 19 条 × 18 语言），
  不重复翻译；网页端加了新语言，重跑脚本即可同步。
- 只在本 App 出现的词条在生成脚本里维护。

界面侧用 `Strings` 这个 data class 承接（而不是 `Map<String, String>`）：
漏翻译某个词条会直接编译不过，而不是等到界面上出现一串裸 key。
带占位符的词条写成 `{count}` / `{time}` / `{latest}`，用 `fill()` 替换。

**阿拉伯语是 RTL**：`AppLang.isRtl` 为真时在根节点套一层
`LocalLayoutDirection = LayoutDirection.Rtl`，Compose 会自动镜像水平布局
（底栏顺序、卡片右箭头、文本对齐都会跟着翻）——截图 `08-devices-ar.png` 里可以看到。

> v3 数据里的多语言字段**只有 `{zh, en}` 两套**（机型名、分支名），
> 所以除简繁中文取中文外，其余 16 种语言的机型名一律取英文（`AppLang.dataCode`）。

## 设置页

整页统一成**五张同款分组卡片**（`SectionCard`：卡片内的小灰标 + 内容），
间隔交给 `LazyColumn` 的 `verticalArrangement`：

| 分组 | 内容 |
| --- | --- |
| 外观 | 下拉行：跟随系统 / 浅色 / 深色 |
| 语言 | 下拉行：18 种语言，当前项打勾 |
| 关于 | 应用名、数据来源、接口地址、免责声明 |
| 项目 | GitHub 仓库、问题反馈（Issues）、作者主页，与网页端 `Footer.vue` 同一批链接 |
| 技术栈 | Compose Multiplatform / MIUIX / Kotlin 版本与版权 |

改造前这一页是「卡片外用 MIUIX 的 `SmallTitle`，卡片里另写一套」，
同一个页面出现两种小节标题；现在标题只有 `SectionLabel` 一种写法。

两个踩过的坑，都写在代码注释里了：

- **`OverlayDropdownMenu` 的 `title` 是必填的**，给空串仍会占掉一整行 `headline1`
  （实测在标题下留出 50dp 空档）。所以这两张下拉卡片把**当前值**当行主文本
  （「跟随系统」「简体中文」），分组名交给 `SectionCard` —— 否则「语言」会出现两次。
- **`TabRow` 放进卡片会整体右移**：它是 `LazyRow`，内部还会把选中项「居中」滚动一次
  （`TabRow.kt` 的 `scrollToItem(selected, -centerOffset)`），实测被卡片圆角切掉。
  外观因此改用与语言同款的下拉行 —— 参考 Updater-KMP 也是这么做的
  （Android 版本 / 区域 / 运营商全是 `OverlayDropdownPreference`，没有 TabRow）。

> 语言选择目前存在内存里（`HubAppState`），重启会回到默认值。
> 要持久化需要 expect/actual 接 SharedPreferences / java.util.prefs。

## 已知限制 / 后续

- **无分页**：`OS1` 有 5867 条 ROM，目前靠 `LazyColumn` 虚拟化 + 搜索收敛。
  后续可以接「按发布日期分年拉取」（`/v3/releases/{year}.json`）。
- **无离线缓存**：`HubRepository` 只是内存缓存，进程重启即失效。
  要离线可用需要引入 Room / SQLDelight。
- **发布配置**：release 未开混淆、未配签名。骨架阶段只保证 debug 可跑。

## Credits

App 的构建与运行依赖以下开源项目与资源，谨向各作者与社区致谢。

| 名称 | 用途 | 许可 |
| --- | --- | --- |
| [Kotlin](https://kotlinlang.org/) / [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) | 语言与跨平台源集（`commonMain` / `androidMain` / `desktopMain` / `jvmCommonMain`） | Apache-2.0 |
| [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) | 全部界面代码（Android 与桌面共用同一份） | Apache-2.0 |
| [MIUIX](https://github.com/compose-miuix-ui/miuix) | HyperOS 设计语言的组件与图标（`miuix-ui` / `miuix-icons` / `miuix-preference`） | Apache-2.0 |
| [Updater-KMP](https://github.com/YuKongA/Updater-KMP) | 主题与配色方案（直接用 MIUIX 明暗色板）、部分组件与按钮的设计、本机机型信息的读法（`Build` + `SystemProperties`） | Apache-2.0 |
| [Ktor Client](https://ktor.io/) | v3 JSON 与 OTA 接口请求（Android 用 OkHttp、桌面用 CIO 引擎） | Apache-2.0 |
| [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) | v3 JSON 反序列化与 OTA 表单序列化 | Apache-2.0 |
| [Coil 3](https://coil-kt.github.io/coil/) | 机型照片加载（`coil-compose` / `coil-network-ktor3` / `coil-svg`） | Apache-2.0 |
| [AndroidX Activity Compose](https://developer.android.com/jetpack/androidx) | `ComponentActivity` 与 Compose 入口 | Apache-2.0 |
| [Android Gradle Plugin](https://developer.android.com/build) / [Gradle](https://gradle.org/) | 构建与打包工具链 | Apache-2.0 |
| [Pillow](https://python-pillow.org/) | `scripts/make_icons.py` 生成各密度启动图标 | MIT-CMU |
| [data 子模块](../../data/README.md) | 全部 ROM 数据与机型图片（`https://api.miuier.com`） | Apache-2.0 |

> MIUI、HyperOS、Xiaomi、Redmi、POCO 等为小米科技或其关联公司的商标；本 App 与小米科技无隶属关系。
