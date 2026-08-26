# MiROMS HUB - Frontend Site (app/web)

> 🌐 **简体中文:** [README.md](README.md) · **English:** [README_EN.md](README_EN.md)

Xiaomi / Redmi / POCO ROM archive site, deployed at [hub.miuier.com](https://hub.miuier.com).
Features device browsing, ROM package lookup (MIUI & HyperOS), changelogs, download links,
recent-update stats, and a feedback entry.

> 小米 / Redmi / POCO 设备 ROM 信息站，部署于 hub.miuier.com。

## Quick Start

```bash
cd app/web
pnpm install
pnpm dev        # dev server: http://localhost:3000 (runs generate-index.mjs first to build data indexes)
```

Production build:

```bash
pnpm build      # output in .output/
pnpm preview    # preview production build
pnpm generate   # static site generation (optional)
pnpm index      # rebuild data indexes only (no server)
```

## Features / Pages

| Route | Page | Description |
| --- | --- | --- |
| `/` | Home | Hero + site stats (devices / ROMs) + last-7-days updates list + feedback entry (GitHub Issues / email) |
| `/devices` | Device list | All indexed models, keyword search, brand-priority ordering (Xiaomi > Redmi > POCO) |
| `/devices/:codename` | Device detail | Device info (codename / brand / Android / supported systems), ROM branch list, per-version changelogs, recovery / fastboot download links |
| `/roms` | ROM packages | Browse all ROM packages grouped by major OS version (OS1 / OS2 / OS3 / V14 / V13 …) |
| `/roms/:os` | ROMs of one OS | Full ROM package list for a given major OS version |

- Download links point to Xiaomi's OSS CDN (`bkt-sgp-miui-ota-update-alisgp.oss-ap-southeast-1.aliyuncs.com`)
- The site includes a disclaimer (not affiliated with Xiaomi), a dark-mode toggle, and zh/en language switching

## Data Source

- **Production**: `runtimeConfig.public.apiBaseUrl` (`https://api.miuier.com/api`) — the JSON data of the `data`
  submodule hosted on Cloudflare Pages at `api.miuier.com` (see `../../data/README.md`).
- **Development**: `server/api/data/[...path].ts` proxy reads the static JSON under local `../../data/api`
  (with path-traversal protection); the frontend always uses `/api/data/v3/...` URLs.
- **Index generation**: `scripts/generate-index.mjs` aggregates `data/api/v3/devices/*.json` into
  `v3/index.json`, `v3/stats.json`, `v3/roms/index.json` + `v3/roms/{os}.json`, and `v3/series.json`;
  `pnpm dev` / `build` / `generate` run it automatically.

## i18n

- `@nuxtjs/i18n`, locales: `zh-cn` / `zh` / `en-us` / `en`, strategy `prefix` (locale-prefixed routes), default `zh-cn`
- Messages live in `i18n.config.ts`; switcher component: `app/components/LanguageSwitcher.vue`
- Example routes: `/zh-cn/devices`, `/en/devices/agate`

## Tech Stack

- Nuxt 4 (`ssr: true`) / Vue 3 / TypeScript / Tailwind CSS
- Modules: `@nuxtjs/tailwindcss`, `@nuxtjs/i18n`, `@nuxtjs/sitemap`, `@nuxtjs/device`
- SEO: `site.url = https://hub.miuier.com` (sitemap module)

## Directory Layout

```
app/web/
├── nuxt.config.ts              # SSR, i18n, sitemap, apiBaseUrl
├── i18n.config.ts              # zh/en message bundles
├── tailwind.config.ts          # Tailwind config shared with app/admin
├── scripts/generate-index.mjs  # data index generation (index / stats / roms / series)
├── server/api/data/[...path].ts # local data proxy for development
├── public/                     # favicon.svg / robots.txt
└── app/
    ├── app.vue / app.config.ts # layout shell + site metadata
    ├── assets/css/main.css     # design tokens (same set as admin)
    ├── composables/            # useApi (data URL builders) / useDarkMode
    ├── components/             # Header / Footer / MiRoms / LanguageSwitcher / DarkModeToggle / Disclaimer
    ├── utils/validation.ts     # input validation
    └── pages/                  # index / devices / devices/[codename] / roms / roms/[os]
```

## Deployment

- The site is hosted at `hub.miuier.com`; build command is `pnpm build` (output `.output/`). No hosting/CI config
  lives in the repo.
- The data side `api.miuier.com` is determined by the `data` submodule's `CNAME` and Cloudflare Pages deploy hook
  (see `../../data/README.md`).

## Related Docs

| Doc | 简体中文 | English |
| --- | --- | --- |
| Project overview | [../../README.md](../../README.md) | [../../README_EN.md](../../README_EN.md) |
| This frontend site | [README.md](README.md) | [README_EN.md](README_EN.md) |
| Admin panel | [../admin/README.md](../admin/README.md) | [../admin/README_EN.md](../admin/README_EN.md) |
| Data repo | [../../data/README.md](../../data/README.md) | [../../data/README_EN.md](../../data/README_EN.md) |
