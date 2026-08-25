# hub.miuier.com

小米 / Redmi / POCO 设备 ROM 数据平台，提供 MIUI 与 HyperOS 全量固件信息查询。

> Xiaomi / Redmi / POCO ROM data platform, covering MIUI & HyperOS firmware information.

---

## 项目结构

```
.
├── app/
│   ├── web/          # 前端站点（Nuxt 4 + Vue 3，部署于 hub.miuier.com）
│   ├── admin/        # 管理后台（Nuxt 4 + Vue 3，端口 3100，直连 MySQL）
│   └── android/ apple/ miniprogram/ unified/  # 移动端预留目录（空）
├── data/             # ROM 数据仓库（git submodule → HegeKen/miroms，部署于 api.miuier.com）
│   ├── api/          # 导出的 JSON 数据（v1 / v2 / v3）
│   ├── scripts/      # 数据同步 / 抓取 / 导出 / 部署脚本（Python 3）
│   └── db_structure/ # MySQL 表结构（devices / branches / roms / series）
├── graphify-out/     # 代码知识图谱（生成物，勿手改，见 AGENTS.md）
├── .trae/rules/      # AI 协作规则（如 git commit 规范）
├── AGENTS.md         # AI 代理工作指引（graphify 用法等）
├── .vscode/          # VS Code 任务（数据抓取脚本一键运行）
├── package.json      # 根 monorepo 脚本
├── .gitmodules       # data 子模块声明
└── LICENSE           # Apache License 2.0
```

## 快速开始

```bash
# 安装依赖（含子模块；若 data/ 为空先执行 git submodule update --init）
pnpm install

# 同时启动前端 + 管理后台
pnpm dev

# 仅启动前端（默认端口 3000）
pnpm dev:web

# 仅启动管理后台（端口 3100）
pnpm dev:admin
```

> 管理后台需要可连接的 MySQL（默认连接参数见 `app/admin/README.md`）。

## 前端站点

基于 Nuxt 4 构建，部署于 [hub.miuier.com](https://hub.miuier.com)，提供机型浏览、ROM 刷机包查询（MIUI & HyperOS）、更新日志与下载链接、近期更新统计、反馈入口。

- 入口：`app/web/`
- 技术栈：Nuxt 4 / Vue 3 / TypeScript / Tailwind CSS
- 数据来源：`data/api/`（Cloudflare Pages 部署至 `https://api.miuier.com`，`apiBaseUrl` 见 `app/web/nuxt.config.ts`）

## 管理后台

基于 Nuxt 4 构建，直连 MySQL（`miroms` 库），管理设备、机型、ROM、分支、系列数据，并提供数据自查（基于表结构注释的违规检测）与忽略管理。

- 入口：`app/admin/`
- 端口：3100（`ssr: false`，SPA 模式）
- 技术栈：Nuxt 4 / Vue 3 / TypeScript / Tailwind CSS / mysql2
- 数据库连接：登录页可验证「本地默认连接」或「自定义连接」，凭据保存在浏览器本地，后续请求经 `x-db-config` 头携带；**无用户鉴权，请勿直接暴露到公网**
- 详见 [app/admin/README.md](app/admin/README.md)

## 数据层

详见 [data/README.md](data/README.md)。

- **数据源**：MySQL 数据库（`devices` / `branches` / `roms` / `series` 表）
- **导出**：Python 脚本从数据库生成 V1/V2/V3 格式 JSON
- **API 版本**：
  - V1 — 仅 MIUI（旧格式）
  - V2 — 仅 HyperOS
  - V3 — MIUI + HyperOS（推荐，含独立更新日志、按 OS 分组的 ROM 列表、系列数据）

### 常用脚本

```bash
# 执行全部数据抓取任务
python3 data/scripts/get_new_branch.py         # Fastboot + OTA 探测
python3 data/scripts/ota_former.py             # OTA 版本检测
python3 data/scripts/ota_full.py               # 全量 OTA 偏移探测
python3 data/scripts/xfu_full.py               # 本地 HTML 核查
python3 data/scripts/get_current_fastboot.py   # 当前 Fastboot 包
python3 data/scripts/mgc_fastboot.py           # 小米社区 API
python3 data/scripts/fetch_changelog.py        # changelog + 卡刷包
python3 data/scripts/aspatch.py                # 安全补丁提取

# 数据导出
python3 data/scripts/exporter.py               # V1/V2/V3 导出 + index.json

# 设备同步
python3 data/scripts/sync_devices.py           # 同步设备列表到 data.py

# 全流程：导出 → 提交并推送 data 子模块 → 触发 Cloudflare Pages 部署
python3 data/scripts/push.py
python3 data/scripts/deploy.py                 # 仅触发部署（deploy hook）
```

也可以通过 VS Code Tasks（`Ctrl+Shift+B`，见 `.vscode/tasks.json`）运行抓取任务。

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Nuxt 4 / Vue 3 / TypeScript / Tailwind CSS |
| 后台 | Nuxt 4 / Vue 3 / TypeScript / mysql2 |
| 数据 | MySQL / Python 3（纯标准库） |
| 部署 | Cloudflare Pages（web → hub.miuier.com，data → api.miuier.com） |

## License

[Apache License 2.0](LICENSE)
