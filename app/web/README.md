# MiROMS HUB - 前端站点 (app/web)

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
  Cloudflare Pages 托管在 `api.miuier.com` 的 JSON 数据（详见 `../data/README.md`）。
- **开发环境**：`server/api/data/[...path].ts` 代理，直接读取本地 `../../data/api` 下的静态 JSON
  （含路径穿越防护），前端 URL 统一为 `/api/data/v3/...`。
- **索引生成**：`scripts/generate-index.mjs` 从 `data/api/v3/devices/*.json` 汇总生成
  `v3/index.json`、`v3/stats.json`、`v3/roms/index.json` + `v3/roms/{os}.json`、`v3/series.json`；
  `pnpm dev` / `build` / `generate` 会自动执行。

## 多语言（i18n）

- `@nuxtjs/i18n`，locales：`zh-cn` / `zh` / `en-us` / `en`，策略 `prefix`（路由带语言前缀），默认 `zh-cn`
- 语言包在 `i18n.config.ts`，切换组件为 `app/components/LanguageSwitcher.vue`
- 路由示例：`/zh-cn/devices`、`/en/devices/agate`

## 技术栈

- Nuxt 4（`ssr: true`）/ Vue 3 / TypeScript / Tailwind CSS
- 模块：`@nuxtjs/tailwindcss`、`@nuxtjs/i18n`、`@nuxtjs/sitemap`、`@nuxtjs/device`
- SEO：`site.url = https://hub.miuier.com`（sitemap 模块）

## 目录结构

```
app/web/
├── nuxt.config.ts              # SSR、i18n、sitemap、apiBaseUrl
├── i18n.config.ts              # 中英文语言包
├── tailwind.config.ts          # 与 app/admin 一致的 Tailwind 配置
├── scripts/generate-index.mjs  # 数据索引生成（index / stats / roms / series）
├── server/api/data/[...path].ts # 开发环境本地数据代理
├── public/                     # favicon.svg / robots.txt
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
- 数据侧 `api.miuier.com` 由 `data` 子模块的 `CNAME` 与 Cloudflare Pages deploy hook 决定（见 `../data/README.md`）。

## 相关文档

- 项目总览：`../README.md`
- 管理后台：`../admin/README.md`
- 数据仓库（API 格式 / 表结构 / 脚本）：`../data/README.md`
