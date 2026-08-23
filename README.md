# hub.miuier.com

小米 / Redmi / POCO 设备 ROM 数据平台，提供 MIUI 与 HyperOS 全量固件信息查询。

> Xiaomi / Redmi / POCO ROM data platform, covering MIUI & HyperOS firmware information.

---

## 项目结构

```
.
├── app/
│   ├── web/          # 前端站点（Nuxt 3 + Vue 3）
│   └── admin/        # 管理后台（Nuxt 3 + Vue 3）
├── data/             # ROM 数据仓库（JSON API + Python 脚本）
│   ├── api/v3/       # 导出的 JSON 数据
│   ├── scripts/      # 数据同步 / 导出 / 抓取脚本
│   └── db_structure/ # MySQL 表结构
├── package.json      # 根 monorepo 脚本
└── LICENSE           # Apache License 2.0
```

## 快速开始

```bash
# 安装依赖
pnpm install

# 同时启动前端 + 管理后台
pnpm dev

# 仅启动前端
pnpm dev:web

# 仅启动管理后台
pnpm dev:admin
```

## 前端站点

基于 Nuxt4 构建，部署于 [hub.miuier.com](https://hub.miuier.com)，提供设备 ROM 查询、版本对比、下载链接等功能。

- 入口：`app/web/`
- 技术栈：Nuxt4 / Vue 3 / TypeScript / Tailwind CSS
- 数据来源：`data/api/v3/`（通过 Cloudflare Pages 部署）

## 管理后台

基于 Nuxt4 构建，用于管理设备信息、ROM 数据、分支配置等。

- 入口：`app/admin/`
- 技术栈：Nuxt4 / Vue 3 / TypeScript

## 数据层

详见 [data/README.md](data/README.md)。

- **数据源**：MySQL 数据库（`roms` / `devices` / `branches` 表）
- **导出**：Python 脚本从数据库生成 V1/V2/V3 格式 JSON
- **API 版本**：
  - V1 — 仅 MIUI（旧格式）
  - V2 — 仅 HyperOS
  - V3 — MIUI + HyperOS（推荐）

### 常用脚本

```bash
# 执行全部数据抓取任务
python3 data/scripts/get_new_branch.py    # Fastboot + OTA 探测
python3 data/scripts/ota_former.py         # OTA 版本检测
python3 data/scripts/ota_full.py           # 全量 OTA 偏移探测
python3 data/scripts/xfu_full.py           # 本地 HTML 核查
python3 data/scripts/get_current_fastboot.py  # 当前 Fastboot 包
python3 data/scripts/mgc_fastboot.py       # 小米社区 API
python3 data/scripts/fetch_changelog.py    # changelog + 卡刷包
python3 data/scripts/aspatch.py            # 安全补丁提取

# 数据导出
python3 data/scripts/exporter.py           # V1/V2/V3 导出

# 设备同步
python3 data/scripts/sync_devices.py       # 同步设备列表到 data.py
```

也可以通过 VS Code Tasks（`Ctrl+Shift+B`）运行上述任务。

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Nuxt4 /  Vue 3 / TypeScript / Tailwind CSS |
| 后台 | Nuxt4 / Vue 3 / TypeScript |
| 数据 | MySQL / Python 3 |
| 部署 | Cloudflare Pages |

## License

[Apache License 2.0](LICENSE)
