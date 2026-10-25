# MiROMS HUB - Data Admin Panel (app/admin)

> 🌐 **简体中文:** [README.md](README.md) · **English:** [README_EN.md](README_EN.md)

The admin panel **connects directly to a MySQL database** (`miroms`) and performs CRUD on six tables:
`devices` / `branches` / `roms` / `series` / `logs` / `dev`, matching the schemas in `data/db_structure/*.sql`.
The UI follows the same design language as `app/web` (shared CSS variables, Tailwind, dark mode, orange theme).

## Quick Start

```bash
cd app/admin
pnpm install
pnpm dev        # dev server: http://localhost:3100
```

Production build:

```bash
pnpm build      # output in .output/
pnpm preview    # preview production build
```

## Database Connection

The panel supports two connection modes (selected/verified on the login page `/login`; there is **no user authentication**):

- **Local default connection**: resolution order is **`NUXT_DB_*` env vars → `data/scripts/config.py` → built-in
  defaults** (`localhost:3306` / `root` / `miroms`). In dev the data repo's `data/scripts/config.py` lives in the
  same checkout and is reused automatically, so no extra setup is needed locally; use env vars in production.
  To override, copy `app/admin/.env.example` to `app/admin/.env` (gitignored) or set the env vars directly.
  The startup log prints the connection in use and where it came from
  (e.g. `root@localhost:3306/miroms (source: data/scripts/config.py)`).

| Env var              | Default    | Description       |
| -------------------- | ---------- | ----------------- |
| `NUXT_DB_HOST`       | `localhost`| Database host     |
| `NUXT_DB_PORT`       | `3306`     | Port              |
| `NUXT_DB_USER`       | `root`     | Username          |
| `NUXT_DB_PASSWORD`   | same as `data/scripts/config.py` | Password |
| `NUXT_DB_NAME`       | `miroms`   | Database name     |

> When the local connection fails, `/api/auth/test-local` now returns actionable guidance (configure `.env` or use
> the custom connection).

- **Custom connection**: fill in host / port / user / password / database on the login page; the backend attempts
  a connection and returns the `VERSION()` as verification. On success the config is stored in browser `localStorage`
  (`miroms_db_config`) and automatically attached to every request as the `x-db-config` header by the client plugin
  (`app/plugins/db-config.client.ts`); the server creates a per-request connection pool
  (`setRequestDbConfig` / `getPool` in `server/utils/db.ts`).

> Security note: this panel has **no user authentication** — the login page only verifies the database connection;
> reads and writes take effect immediately. Do not expose it directly to the public internet; keep it on an
> intranet or behind a gateway/proxy.

## Features

- **Login** `/login` — verify the database connection (local default / custom), then enter the workspace
- **Dashboard** `/` — row counts, last-7-days daily additions, DB version & connection status, table sizes, recent records per table, full-database data self-check
- **Devices** `/devices` — CRUD on the `devices` table (~2,000 rows)
- **Model manager** `/devices-manager` — aggregated devices+branches view: device list, full branch details per model, batch-edit branch fields
- **Branches** `/branches` — CRUD on the `branches` table
- **ROMs** `/roms` — CRUD on the `roms` table (50k+ rows; server-side pagination / search / sort)
- **Series** `/series-manager` — CRUD on the `series` table (brand, zh/en names, `device_ids` device membership),
  pick/order devices by brand; writes go through the generic CRUD (`/api/db/series`)
- **Log entry** `/logs` — enter changelog text by hand: each line becomes one log row in the shared

- **XMS app updates** `/xms` — XMS (SOTA system-app update) pushes and app details: filter by device / version /
  keyword, expand a push to edit each app's package, zh/en names plus the multilingual `names`, version code, package
  info and changelog (JSON); apps can be added or removed. The push itself (notes, `changelog` modules without a
  package, latest flag, push date, …) is edited through the generic record modal. The XMS version carries an OS label
  (C → HyperOS 3, D → HyperOS 4); package-less modules live in the push `changelog`.
  "Fill translations from logs" matches the zh/en name against `logs` module rows and adds missing languages; saving
  syncs `name` / `name_en` / `names` to **every push with the same pkg** (changelogs stay per push; the checkbox can
  turn the sync off). Hand-fixed `apk_name` values are reused by later XMS fetches (same-device rows first), so messy
  API names do not overwrite manual repairs. If the name is missing from `logs` modules, a zh/en placeholder row is registered automatically
  (on save, on "fill translations", and while the fetcher writes XMS), so other languages can be filled later in "Log entry"
- **XMS app rows** `/xms_app` — CRUD on the `xms_app` table (multilingual `names` is always a JSON textarea so no language
  is truncated; this table has no brand fields, so instead of the device-page "generate from brands" button it offers
  "fill translations from logs" using `name` / `name_en`). Its self-check also flags **missing** `names` / `apk_name`
  and **inconsistent** `names` / `apk_name` within the same package (tablet/fold apk_name variants show up as warnings
  for manual review). The edit modal has a "sync name / name_en / names / apk_name to every row of the same package"
  checkbox (on by default), so a self-check finding can be fixed for all rows at once; untick it to keep apk_name variants
- **Dev binding** `/dev-bind` — associate ROMs with a developer week by hand: the left column lists the week's bound
  ROMs (unbind one or all), the right column searches candidates by codename / version / package filename (filter by
  year and binding state) and binds the selected ones. The dev weekly device list is not stored separately — the
  binding *is* `roms.dev_week`, and both the exporter and the weekly page derive the list from `dev_week + zone`.
  Weeks that cannot be matched automatically (old weekly posts that only listed devices without a version, or
  postponed-release notices) are filled in here. The search box is pre-filled with the week's version, and matching
  full versions are offered as chips (an announcement may say `5.20` while the DB has `21.5.20` / `22.5.20`).
  `logs` table (all language versions of a log share a row); "parse & compare" previews whether a line is new or
  matches an existing group, and saving back-fills other languages from the data already in the database
- **Developer weekly** `/dev` — CRUD on the `dev` table (weekly developer announcements, device/ROM lists, changelogs)
- **SQL console** `/sql` — read-only queries (SELECT / SHOW / DESCRIBE / EXPLAIN / WITH), auto-appends `LIMIT 1000`
- **Data self-check** — "Check" button in each module toolbar + "Run check" on the dashboard: based on the column
  comments & examples in `data/db_structure/*.sql`, flags illogical field values (enum values, format regexes, JSON
  validity, empty strings, package filenames, etc.), shows violation counts and samples; samples link directly to
  editing; the "total / errors / warnings" summary is clickable to filter the rule list by severity
- **ID renumbering (AUTO_INCREMENT repair)** — every table's self-check panel shows the current `AUTO_INCREMENT` and
  `MAX(id)` (plus the number of skipped ids); "renumber ids" compacts existing ids to `1..N` and **updates the
  references** in the same transaction: `series.device_ids` (devices.id), `xms.rom_id` (roms.id), `xms_app.xms_id`
  (xms.id), `roms.logs` / `dev.logs` (logs.id; only module & entry ids are mapped, language masks stay as-is), then
  resets `AUTO_INCREMENT` to `N+1`. The operation is transactional (rolls back on error) but irreversible; large
  tables (roms / logs) may take seconds to tens of seconds
- **Dismiss** — special cases (e.g. legacy formats, carrier-customized models) can be ignored:
  - Row level: "Ignore" button on a sample row — skips only that record
  - Rule level: "Ignore this rule" in the rule header (optional reason) — the whole rule no longer counts
  - Dismissals persist in the `check_dismissals` table (auto-created); the "dismissed" list can restore checks anytime

Table pages support: keyword search (auto-matches text columns), arbitrary column sorting, pagination, inline
add/edit (forms generated from `information_schema` column metadata; JSON fields auto-validated and formatable),
and delete confirmation.

## Server API (Nitro)

| Method | Path                              | Description                            |
| ------ | --------------------------------- | -------------------------------------- |
| POST   | `/api/auth/login`                 | Verify DB connection (custom config), returns `{ ok, version, config }` |
| GET    | `/api/auth/test-local`            | Verify the local default connection (`runtimeConfig.db`) |
| GET    | `/api/db/stats`                   | Stats (row counts, version, table sizes, recent records) |
| GET    | `/api/db/daily-new`               | Last-7-days daily additions (roms exact / devices, branches estimated) |
| GET    | `/api/db/meta/:table`             | Table metadata (column types, JSON/date flags, etc.) |
| GET    | `/api/db/:table`                  | Paginated list (`page/pageSize/search/sort/order`) |
| POST   | `/api/db/:table`                  | Create record                         |
| GET    | `/api/db/:table/:id`              | Get single record                     |
| PUT    | `/api/db/:table/:id`              | Update record                         |
| DELETE | `/api/db/:table/:id`              | Delete record                         |
| POST   | `/api/db/:table/batch-update-device` | Batch-update devices (model manager) |
| POST   | `/api/db/sql`                     | Read-only SQL console                 |
| GET    | `/api/db/check/:table`            | Single-table data self-check (`?summary=1` returns counts only) |
| GET    | `/api/db/check/summary`           | All-table check summary (60s cache)   |
| POST   | `/api/db/check/dismiss`           | Dismiss violation `{table, ruleId, rowId?, reason?}` (rowId=0 or omitted = whole rule) |
| DELETE | `/api/db/check/dismiss`           | Undismiss `{table, ruleId, rowId?}`   |
| GET    | `/api/db/check/dismissals`        | Dismissed list (`?table=` optional)   |
| GET    | `/api/db/:table/auto-increment`   | Auto-increment status: current `AUTO_INCREMENT`, `MAX(id)`, skipped ids |
| POST   | `/api/db/:table/auto-increment`   | Renumber existing ids to `1..N`, update references and reset `AUTO_INCREMENT` (`{dryRun:true}` previews and rolls back) |
| POST   | `/api/logs/manual`                | Manual log entry `{text, type?, lang?, preview?}`: split lines → compare with `logs` → write / back-fill other languages |
| GET    | `/api/logs/list`                  | Log group list (`page/pageSize/search/type/missing`) with per-language coverage |
| GET    | `/api/logs/audit`                 | Dedup audit: duplicate groups / duplicate rows / rows referenced by no `roms`·`dev` record |
| GET    | `/api/dev/weeks`                  | Dev week overview (bound ROM / device counts; `bound=0` means it needs manual binding) |
| GET    | `/api/dev/bind`                   | ROMs currently bound to a week (`week` / `region`) |
| GET    | `/api/dev/roms`                   | Candidate ROM search (`search` / `week` / `region` / `bound` / `year` / paging, plus full-version facets) |
| POST   | `/api/dev/bind`                   | Bind / unbind: `{ week, region, action: bind\|unbind, romIds, force }` |
| GET    | `/api/devices-manager`            | Model-manager data (device list + per-model branch details + all branch defs) |
| GET    | `/api/series-manager`             | Series-manager data (all series + device base rows + brand labels) |
| GET    | `/api/xms/list`                   | XMS push list (`page/pageSize/search/latest/version`) with app count and device name |
| GET    | `/api/xms/apps`                   | App details of one push (`xmsId`) plus the push `changelog` (package-less modules) |
| GET    | `/api/xms/translate`              | Look up `logs` module translations by `name` / `nameEn` to complete app names |
| POST   | `/api/xms/app`                    | Create/update an app and sync the same pkg: validates JSON, fills translations from `logs`, and with `propagate` (default) syncs names to every row of that pkg |
| POST   | `/api/xms/module`                 | Register an app name in `logs` modules (creates a zh/en placeholder when missing) for later translation |
| POST   | `/api/xms/propagate`              | Sync `name` / `name_en` / `names` / `apk_name` to every row of the same package (self-check repair) |

Security: table whitelist (`devices|branches|roms|series|logs|dev`), column whitelist validation, parameterized queries
everywhere, read-only SQL console with row limits, server-side JSON re-validation, and empty-string → NULL conversion.

## Tech Stack

- Nuxt 4 (SPA mode `ssr: false`) + Nitro server routes
- Tailwind CSS (config shared with `app/web`; see `tailwind.config.ts` / `app/assets/css/main.css`)
- `mysql2` (connection pool + parameterized queries, `server/utils/db.ts`)

## Directory Layout

```
app/admin/
├── nuxt.config.ts            # port 3100, DB runtime config (ssr: false)
├── tailwind.config.ts        # Tailwind config shared with app/web
├── server/
│   ├── utils/db.ts           # MySQL pool (per-request x-db-config), table whitelist, column metadata, CRUD helpers
│   ├── utils/checker.ts      # data self-check rule engine (violations derived from db_structure comments)
│   └── api/
│       ├── auth/             # DB connection verification (login / test-local)
│       ├── db/               # stats / daily-new / meta / CRUD / batch-update-device / sql / check
│       ├── devices-manager.get.ts   # model-manager aggregate endpoint
│       └── series-manager.get.ts    # series-manager aggregate endpoint
└── app/
    ├── app.vue               # sidebar + topbar + dark-mode layout
    ├── assets/css/main.css   # design tokens shared with app/web
    ├── composables/          # useAdminApi / useDarkMode / useDbConfig / useToast
    ├── components/           # TableManager / RecordModal / CheckPanel / Pagination / ConfirmDialog / DbStatus / Toast / DarkModeToggle / InfoItem / SeriesModal
    ├── plugins/              # db-config.client.ts (auto-attaches x-db-config header)
    └── pages/                # index(dashboard) / login / devices / devices-manager / branches / roms / series-manager / logs / sql
```

## Related Docs

| Doc | 简体中文 | English |
| --- | --- | --- |
| Project overview | [../../README.md](../../README.md) | [../../README_EN.md](../../README_EN.md) |
| Frontend site | [../web/README.md](../web/README.md) | [../web/README_EN.md](../web/README_EN.md) |
| This admin panel | [README.md](README.md) | [README_EN.md](README_EN.md) |
| Data repo | [../../data/README.md](../../data/README.md) | [../../data/README_EN.md](../../data/README_EN.md) |

## Credits

The admin panel is built and runs on the following open-source projects. Thanks to their authors and communities.

| Name | Purpose | License |
| --- | --- | --- |
| [Nuxt 4](https://nuxt.com/) | App framework (SPA `ssr: false`, build output `.output/`) | MIT |
| [Vue 3](https://vuejs.org/) | View layer (pages / components / composables) | MIT |
| [Nitro](https://nitro.build/) | `server/api/**` server routes and runtime | MIT |
| [TypeScript](https://www.typescriptlang.org/) | Type system | Apache-2.0 |
| [Tailwind CSS](https://tailwindcss.com/) with [@nuxtjs/tailwindcss](https://tailwindcss.nuxtjs.org/) | Styling system and design tokens (same set as `app/web`) | MIT |
| [mysql2](https://github.com/sidorares/node-mysql2) | MySQL connection pool, parameterized and prepared queries | MIT |
| [MySQL](https://www.mysql.com/) | Data storage (`miroms` database: `devices` / `roms` / `branches` / `series`) | GPL-2.0 / commercial |

> The managed data and the trademarks involved belong to Xiaomi; this project is not affiliated with Xiaomi.
