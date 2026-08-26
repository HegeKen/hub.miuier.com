# MiROMS HUB - Data Admin Panel (app/admin)

> 🌐 **简体中文:** [README.md](README.md) · **English:** [README_EN.md](README_EN.md)

The admin panel **connects directly to a MySQL database** (`miroms`) and performs CRUD on four tables:
`devices` / `branches` / `roms` / `series`, matching the schemas in `data/db_structure/*.sql`.
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

- **Local default connection**: uses the default `runtimeConfig.db` config from `nuxt.config.ts`, matching
  `data/scripts/config.py` (`localhost:3306` / `root` / `miroms`). Overridable via environment variables or `.env`:

| Env var              | Default    | Description       |
| -------------------- | ---------- | ----------------- |
| `NUXT_DB_HOST`       | `localhost`| Database host     |
| `NUXT_DB_PORT`       | `3306`     | Port              |
| `NUXT_DB_USER`       | `root`     | Username          |
| `NUXT_DB_PASSWORD`   | same as `data/scripts/config.py` | Password |
| `NUXT_DB_NAME`       | `miroms`   | Database name     |

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
- **SQL console** `/sql` — read-only queries (SELECT / SHOW / DESCRIBE / EXPLAIN / WITH), auto-appends `LIMIT 1000`
- **Data self-check** — "Check" button in each module toolbar + "Run check" on the dashboard: based on the column
  comments & examples in `data/db_structure/*.sql`, flags illogical field values (enum values, format regexes, JSON
  validity, empty strings, package filenames, etc.), shows violation counts and samples; samples link directly to
  editing; the "total / errors / warnings" summary is clickable to filter the rule list by severity
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
| GET    | `/api/devices-manager`            | Model-manager data (device list + per-model branch details + all branch defs) |
| GET    | `/api/series-manager`             | Series-manager data (all series + device base rows + brand labels) |

Security: table whitelist (`devices|branches|roms|series`), column whitelist validation, parameterized queries
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
    └── pages/                # index(dashboard) / login / devices / devices-manager / branches / roms / series-manager / sql
```

## Related Docs

| Doc | 简体中文 | English |
| --- | --- | --- |
| Project overview | [../../README.md](../../README.md) | [../../README_EN.md](../../README_EN.md) |
| Frontend site | [../web/README.md](../web/README.md) | [../web/README_EN.md](../web/README_EN.md) |
| This admin panel | [README.md](README.md) | [README_EN.md](README_EN.md) |
| Data repo | [../../data/README.md](../../data/README.md) | [../../data/README_EN.md](../../data/README_EN.md) |
