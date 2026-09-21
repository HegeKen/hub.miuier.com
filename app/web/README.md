# MiROMS HUB - 前端站点 (app/web)

> 🌐 **简体中文:** [README.md](README.md) · **English:** [README_EN.md](README_EN.md)

小米 / Redmi / POCO 设备 ROM 信息站，部署于 [hub.miuier.com](https://hub.miuier.com)，
提供机型浏览、ROM 刷机包查询（MIUI & HyperOS）、更新日志、下载链接、近期更新统计与反馈入口。

> Xiaomi / Redmi / POCO ROM archive site, powered by the `data` submodule (served at `https://api.miuier.com`).

## 快速开始

```bash
cd app/web
pnpm install
pnpm dev        # 开发服务器: http://localhost:3000（自动先运行 generate-index.mjs 生成数据索引）
```

生产构建：

```bash
pnpm build      # 产物在 .output/
pnpm preview    # 预览生产构建
pnpm generate   # 静态站点生成（可选）
pnpm index      # 仅重新生成数据索引（不启动服务）
```

## 功能 / 页面

| 路由 | 页面 | 说明 |
| --- | --- | --- |
| `/` | 首页 | Hero + 站点统计（设备 / ROM 数）+ 近 7 天更新列表 + 反馈入口（GitHub Issues / 邮箱） |
| `/devices` | 机型列表 | 全部已收录机型，支持关键字搜索，品牌优先排序（Xiaomi > Redmi > POCO） |
| `/devices/:codename` | 机型详情 | 设备信息（代号 / 品牌 / Android / 支持系统）、ROM 分支列表、各版本更新日志、卡刷 / 线刷包下载链接 |
| `/roms` | 刷机包 | 按系统大版本（OS1 / OS2 / OS3 / V14 / V13 …）分组浏览全部 ROM 包 |
| `/roms/:os` | 指定版本 ROM | 某个系统大版本下的完整 ROM 包列表 |

- 下载链接指向小米 OSS CDN（`bkt-sgp-miui-ota-update-alisgp.oss-ap-southeast-1.aliyuncs.com`）
- 站点含免责声明（非小米官方 / 与小米无关）、深色模式切换、中英文语言切换

## 数据来源

- **生产环境**：`runtimeConfig.public.apiBaseUrl`（`https://api.miuier.com/api`），即 `data` 子模块经
  Cloudflare Pages 托管在 `api.miuier.com` 的 JSON 数据（详见 `../../data/README.md`）。
- **开发环境**：`server/api/data/[...path].ts` 代理，直接读取本地 `../../data/api` 下的静态 JSON
  （含路径穿越防护），前端 URL 统一为 `/api/data/v3/...`。
- **索引生成**：`scripts/generate-index.mjs` 从 `data/api/v3/devices/*.json` 汇总生成
  `v3/index.json`、`v3/stats.json`、`v3/roms/index.json` + `v3/roms/{os}.json`、`v3/series.json`；
  `pnpm dev` / `build` / `generate` 会自动执行。

## 多语言（i18n）

- `@nuxtjs/i18n`，策略 `prefix`（路由带语言前缀，如 `/ja/devices`），默认 `zh-cn`
- 语言包放在 `i18n/locales/<code>.ts`，由 `i18n.config.ts` 聚合
- 已启用 21 种语言（按数据中的区域对应其官方语言）：`zh-cn` 简体中文、`zh-tw` 繁體中文、`en-us` English、`ja` 日本語、`ko` 한국어、`ru` Русский、`uk` Українська、`pl` Polski、`de` Deutsch、`fr` Français、`it` Italiano、`es` Español、`pt` Português、`tr` Türkçe、`id` Bahasa Indonesia、`vi` Tiếng Việt、`th` ไทย、`ar` العربية、`hi` हिन्दी、`ug` ئۇيغۇرچە（维吾尔文）、`bo` བོད་ཡིག（藏文）；另有 `zh` / `en` 两个隐藏别名仅为兼容旧链接
- **区域 / 运营商译名也在语言包里**：`regions.<region>`（如 `regions.cn` = 中国大陆）与 `carriers.<carrier>`（如 `carriers.vf` = 沃达丰（Vodafone）），键即数据里的代号，未收录代号由 `useRegionName()` / `useCarrierName()` 回退为大写代号；筛选按钮顺序由 `app/utils/region.ts`、`app/utils/carrier.ts` 维护
- 切换组件 `app/components/LanguageSwitcher.vue` 直接读取 `nuxt.config.ts` 的 `i18n.locales`（`name` 下拉展示、`short` 顶栏短标签、`hidden` 不进列表）
- 新增语言：复制 `i18n/locales/en-us.ts` → 翻译 → 在 `i18n.config.ts` 中 import 并登记 → 在 `nuxt.config.ts` 的 `i18n.locales` 中登记
- **根路径按浏览器语言跳转**：`/` 由 `@nuxtjs/i18n` 默认行为按 `Accept-Language` 服务端 302 到对应语言；只对根路径生效（`redirectOn: 'root'`），不带前缀的 `/devices` 等仍是 404；跳转时下发 `i18n_redirected` cookie（1 年），**cookie 优先于浏览器语言**，用户手选过的语言不会被覆盖。
- **中文区域标签修正**（`server/plugins/i18n-zh-locale.ts`）：模块的 `findBrowserLocale` 第一轮按 `language` 完整串匹配、第二轮只按主语言子标签匹配并取列表里第一个命中的，而 `zh-cn` / `zh-tw` 的 `language` 是 `zh-Hans` / `zh-Hant`，浏览器发的却是区域形式（`zh-TW` / `zh-HK` / `zh-MO` / `zh-Hant-TW`）——两轮都落到 `zh`，于是繁中用户被跳到简体。该插件在 i18n 读取请求头之前，把请求头里的中文区域标签归一化成脚本形式（繁体类 → `zh-Hant`，简体类 → `zh-Hans`），让模块自己的精确匹配落到 `zh-tw` / `zh-cn`。**只改请求头、不自己发跳转**，所以 cookie 优先、`redirectOn`、查询串保留等行为仍由模块负责。
- **RTL（从右到左）**：RTL 语言在 `nuxt.config.ts` 的 `i18n.locales` 里用 `dir: 'rtl'` 声明（`ar` / `ug` 已声明），`app/components/MiRoms.vue` 据此写入 `<html lang dir>`。**`<MiRoms />` 必须留在 `app.vue` 的 `ClientOnly` 之外**——放进去就只在浏览器端执行，SSR 的 HTML 没有 `dir`，RTL 语言会先按 LTR 排版再翻转，出现方向闪烁。为让布局自动镜像，方向敏感的工具类一律使用**逻辑属性**而不是物理方向：`ps-*` / `pe-*`（内边距）、`ms-*` / `me-*`（外边距）、`start-*` / `end-*`（定位）、`text-start`、`border-e`。方向性图标（chevron / 箭头）加 `dir-flip` 类，由 `main.css` 的 `[dir='rtl'] .dir-flip { scale: -1 1 }` 水平镜像——镜像后元素自身的 `translate-x` 位移会视觉反向，因此悬停动效无需再写 RTL 版本
- 自检：`pnpm --filter miroms check:i18n`（校验各语言包键集合、占位符与 `zh-cn` 完全一致，以及三方登记是否对得上）

## 技术栈

- Nuxt 4（`ssr: true`）/ Vue 3 / TypeScript / Tailwind CSS
- 模块：`@nuxtjs/tailwindcss`、`@nuxtjs/i18n`、`@nuxtjs/sitemap`、`@nuxtjs/device`
- SEO：`site.url = https://hub.miuier.com`（sitemap 模块）

## 目录结构

```
app/web/
├── nuxt.config.ts              # SSR、i18n.locales、sitemap、apiBaseUrl
├── i18n.config.ts              # 语言包聚合入口（各语言见 i18n/locales/*.ts）
├── i18n/locales/<code>.ts      # 单语言词条（含 regions / carriers 译名）
├── tailwind.config.ts          # 与 app/admin 一致的 Tailwind 配置
├── scripts/generate-index.mjs  # 数据索引生成（index / stats / roms / releases / series）
├── scripts/check-locales.mjs   # i18n 词条自检（pnpm check:i18n）
├── scripts/make-favicon.py     # 由方形 logo 源图生成 favicon / 应用图标（需 Pillow）
├── server/api/data/[...path].ts # 开发环境本地数据代理
├── public/                     # favicon.ico / favicon-*.png / apple-touch-icon.png / icon-*.png / site.webmanifest / robots.txt
└── app/
    ├── app.vue / app.config.ts # 布局壳 + 站点元信息
    ├── assets/css/main.css     # 设计令牌（与 admin 同一套）
    ├── composables/            # useApi（数据 URL 构建）/ useDarkMode
    ├── components/             # Header / Footer / MiRoms / LanguageSwitcher / DarkModeToggle / Disclaimer
    ├── utils/validation.ts     # 输入校验
    └── pages/                  # index / devices / devices/[codename] / roms / roms/[os]
```

## 部署

- 站点托管于 `hub.miuier.com`，构建命令为 `pnpm build`（产物 `.output/`）；仓库内不含托管 / CI 配置。
- 数据侧 `api.miuier.com` 由 `data` 子模块的 `CNAME` 与 Cloudflare Pages deploy hook 决定（见 `../../data/README.md`）。

## 相关文档

| 文档 | 简体中文 | English |
| --- | --- | --- |
| 项目总览 | [../../README.md](../../README.md) | [../../README_EN.md](../../README_EN.md) |
| 本站点 | [README.md](README.md) | [README_EN.md](README_EN.md) |
| 管理后台 | [../admin/README.md](../admin/README.md) | [../admin/README_EN.md](../admin/README_EN.md) |
| 数据仓库（API 格式 / 表结构 / 脚本） | [../../data/README.md](../../data/README.md) | [../../data/README_EN.md](../../data/README_EN.md) |

## Credits

本站的构建依赖以下开源项目与资源，谨向各作者与社区致谢。

| 名称 | 用途 | 许可 |
| --- | --- | --- |
| [Nuxt 4](https://nuxt.com/) | 应用框架（SSR + Nitro 服务端路由、构建产物 `.output/`） | MIT |
| [Vue 3](https://vuejs.org/) | 视图层（页面 / 组件 / 组合式函数） | MIT |
| [TypeScript](https://www.typescriptlang.org/) | 类型系统 | Apache-2.0 |
| [Tailwind CSS](https://tailwindcss.com/) 与 [@nuxtjs/tailwindcss](https://tailwindcss.nuxtjs.org/) | 样式体系与设计令牌（`app/assets/css/main.css`） | MIT |
| [@nuxtjs/i18n](https://i18n.nuxtjs.org/) | 21 语言前缀路由、词条聚合、RTL 与根路径跳转 | MIT |
| [@nuxtjs/sitemap](https://nuxtseo.com/sitemap) | `sitemap.xml` 生成（`site.url`） | MIT |
| [@nuxtjs/device](https://github.com/nuxt-modules/device) | 设备类型识别 | MIT |
| [Pillow](https://python-pillow.org/) | `scripts/make-favicon.py` 生成 favicon / 应用图标 | MIT-CMU |
| [Cloudflare Pages](https://pages.cloudflare.com/) | 站点托管与部署（`hub.miuier.com`） | Cloudflare 服务条款 |
| [data 子模块](../../data/README.md) | 全部 ROM 数据与机型图片（`https://api.miuier.com/api`） | Apache-2.0 |

> 页面展示的 ROM 数据、更新日志与机型图片来自小米官方接口，相关商标归小米科技所有；本项目与小米科技无隶属关系。
