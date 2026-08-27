# hub.miuier.com

Xiaomi / Redmi / POCO device ROM data platform, providing full firmware information for MIUI and HyperOS.

> 小米 / Redmi / POCO 设备 ROM 数据平台，提供 MIUI 与 HyperOS 全量固件信息查询。

> 🌐 **简体中文:** [README.md](README.md) · **English:** [README_EN.md](README_EN.md)

---

## Project Structure

```
.
├── app/
│   ├── web/          # Frontend site (Nuxt 4 + Vue 3, deployed at hub.miuier.com)
│   ├── admin/        # Admin panel (Nuxt 4 + Vue 3, port 3100, direct MySQL access)
│   └── android/ apple/ miniprogram/ unified/  # Reserved mobile client dirs (empty)
├── data/             # ROM data repo (git submodule → HegeKen/miroms, deployed at api.miuier.com)
│   ├── api/          # Exported JSON data (v1 / v2 / v3)
│   ├── scripts/      # Data sync / fetch / export / deploy scripts (Python 3)
│   └── db_structure/ # MySQL table schemas (devices / branches / roms / series)
├── graphify-out/     # Code knowledge graph (generated; do not hand-edit, see AGENTS.md)
├── .trae/rules/      # AI collaboration rules (e.g. git commit conventions)
├── AGENTS.md         # AI agent instructions (graphify usage, etc.)
├── .vscode/          # VS Code tasks (one-click data fetch scripts)
├── package.json      # Root monorepo scripts
├── .gitmodules       # data submodule declaration
└── LICENSE           # Apache License 2.0
```

## Quick Start

```bash
# Install dependencies (incl. submodule; if data/ is empty run git submodule update --init first)
pnpm install

# Start frontend + admin panel together
pnpm dev

# Frontend only (default port 3000)
pnpm dev:web

# Admin panel only (port 3100)
pnpm dev:admin
```

> The admin panel needs a reachable MySQL instance (default connection params: see `app/admin/README.md`).

## Frontend Site

Built with Nuxt 4, deployed at [hub.miuier.com](https://hub.miuier.com). Features device browsing, ROM package lookup (MIUI & HyperOS), changelogs & download links, recent-update stats, and a feedback entry.

- Entry: `app/web/`
- Stack: Nuxt 4 / Vue 3 / TypeScript / Tailwind CSS
- Data source: `data/api/` (Cloudflare Pages, served at `https://api.miuier.com`; `apiBaseUrl` in `app/web/nuxt.config.ts`)

## Admin Panel

Built with Nuxt 4, connects directly to MySQL (`miroms` database) to manage devices, models, ROMs, branches and series, plus a data self-check (violation detection based on table-schema comments) and dismiss management.

- Entry: `app/admin/`
- Port: 3100 (`ssr: false`, SPA mode)
- Stack: Nuxt 4 / Vue 3 / TypeScript / Tailwind CSS / mysql2
- Database connection: the login page verifies a "local default connection" or a "custom connection"; credentials are stored in the browser and sent with every request via the `x-db-config` header; **no user authentication — do not expose directly to the public internet**
- See [app/admin/README.md](app/admin/README.md)

## Data Layer

See [data/README.md](https://github.com/HegeKen/miroms/blob/master/README.md).

- **Source**: MySQL database (`devices` / `branches` / `roms` / `series` tables)
- **Export**: Python scripts generate V1/V2/V3 JSON from the database
- **API versions**:
  - V1 — MIUI only (legacy format)
  - V2 — HyperOS only
  - V3 — MIUI + HyperOS (recommended; includes per-ROM changelogs, OS-grouped ROM lists, series data)

### Common Scripts

```bash
# Run all data fetch tasks
python3 data/scripts/get_new_branch.py         # Fastboot + OTA probing
python3 data/scripts/ota_former.py             # OTA version detection
python3 data/scripts/ota_full.py               # Full OTA offset probing
python3 data/scripts/xfu_full.py               # Local HTML verification
python3 data/scripts/get_current_fastboot.py   # Current Fastboot packages
python3 data/scripts/mgc_fastboot.py           # Xiaomi community API
python3 data/scripts/fetch_changelog.py        # changelog + recovery packages
python3 data/scripts/aspatch.py                # Android security patch extraction

# Data export
python3 data/scripts/exporter.py               # V1/V2/V3 export + index.json

# Device sync
python3 data/scripts/sync_devices.py           # Sync device list into data.py

# Full pipeline: export → commit & push data submodule → trigger Cloudflare Pages deploy
python3 data/scripts/push.py
python3 data/scripts/deploy.py                 # Trigger deploy only (deploy hook)
```

The fetch tasks can also be run via VS Code Tasks (`Ctrl+Shift+B`, see `.vscode/tasks.json`).

## Tech Stack

| Layer | Tech |
|---|---|
| Frontend | Nuxt 4 / Vue 3 / TypeScript / Tailwind CSS |
| Admin | Nuxt 4 / Vue 3 / TypeScript / mysql2 |
| Data | MySQL / Python 3 (stdlib only) |
| Deploy | Cloudflare Pages (web → hub.miuier.com, data → api.miuier.com) |

## Docs

| Doc | 简体中文 | English |
| --- | --- | --- |
| Project overview | [README.md](README.md) | [README_EN.md](README_EN.md) |
| Frontend site | [app/web/README.md](app/web/README.md) | [app/web/README_EN.md](app/web/README_EN.md) |
| Admin panel | [app/admin/README.md](app/admin/README.md) | [app/admin/README_EN.md](app/admin/README_EN.md) |
| Data repo | [data/README.md](data/README.md) | [data/README_EN.md](data/README_EN.md) |

## License

[Apache License 2.0](LICENSE)
