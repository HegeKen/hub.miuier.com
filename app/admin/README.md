# MiROMS HUB - 数据管理后台 (app/admin)

管理后台：**直连 MySQL 数据库**（`miroms` 库），直接对 `devices` / `branches` / `roms` / `series` 四张表进行增删改查，
表结构与 `data/db_structure/*.sql` 保持一致。UI 设计风格与 `app/web` 统一（同一套 CSS 变量、Tailwind、深色模式、橙色主题）。

## 快速开始

```bash
cd app/admin
pnpm install
pnpm dev        # 开发服务器: http://localhost:3100
```

生产构建：

```bash
pnpm build      # 产物在 .output/
pnpm preview    # 预览生产构建
```

## 数据库连接

后台支持两种连接方式（登录页 `/login` 选择/验证后生效，无用户鉴权）：

- **本地默认连接**：使用 `nuxt.config.ts` 中 `runtimeConfig.db` 的默认配置，默认参数与 `data/scripts/config.py` 保持一致
  （`localhost:3306` / `root` / `miroms`），可通过环境变量或 `.env` 覆盖：

| 环境变量             | 默认值      | 说明       |
| -------------------- | ----------- | ---------- |
| `NUXT_DB_HOST`       | `localhost` | 数据库地址 |
| `NUXT_DB_PORT`       | `3306`      | 端口       |
| `NUXT_DB_USER`       | `root`      | 用户名     |
| `NUXT_DB_PASSWORD`   | 同 `data/scripts/config.py` | 密码 |
| `NUXT_DB_NAME`       | `miroms`    | 库名       |

- **自定义连接**：登录页填写 host / port / user / password / database，后台用该配置尝试连接并返回 `VERSION()`
  验证结果；验证通过后配置保存在浏览器 `localStorage`（`miroms_db_config`），由客户端插件
  （`app/plugins/db-config.client.ts`）自动附加到每次请求的 `x-db-config` 请求头，服务端按请求创建对应连接池
  （见 `server/utils/db.ts` 的 `setRequestDbConfig` / `getPool`）。

> 安全提示：本后台**没有用户鉴权**，登录页仅做数据库连接验证；读写直接生效，请勿直接暴露到公网，建议置于内网或加网关/代理保护。

## 功能

- **登录** `/login` — 验证数据库连接（本地默认 / 自定义），验证成功后进入工作台
- **仪表盘** `/` — 表行数统计、近 7 天每日新增、数据库版本与连接状态、表空间占用、各表最近记录、全库数据自查
- **设备管理** `/devices` — `devices` 表 CRUD（约 2000 行）
- **机型管理** `/devices-manager` — 聚合设备与分支的机型视图：设备列表、单机型全部分支详情，分支字段可批量编辑
- **分支管理** `/branches` — `branches` 表 CRUD
- **ROM 管理** `/roms` — `roms` 表 CRUD（5 万+ 行，服务端分页 / 搜索 / 排序）
- **系列管理** `/series-manager` — `series` 表 CRUD（品牌、中英文名、`device_ids` 设备归属），
  按品牌选择/排序设备，写入走通用 CRUD（`/api/db/series`）
- **SQL 控制台** `/sql` — 只读查询（SELECT / SHOW / DESCRIBE / EXPLAIN / WITH），自动追加 `LIMIT 1000`
- **数据自查** — 每个模块工具栏的「自查」按钮 + 仪表盘「运行自查」：依据 `data/db_structure/*.sql`
  中每列的注释与示例，主动筛选不符合逻辑的字段（枚举值、格式正则、JSON 合法性、空字符串、包文件名等），
  展示违规计数与样本，样本可直接跳转编辑；汇总区的「违规合计 / 错误 / 警告」可点击按级别筛选规则列表
- **自查忽略（dismiss）** — 特殊情况（如历史遗留格式、运营商定制机型）可忽略：
  - 行级：样本行「忽略」按钮，只跳过该记录
  - 规则级：规则头「忽略此规则」（可填原因），整条规则不再计入违规
  - 忽略记录持久化在 `check_dismissals` 表（自动创建），「已忽略」列表可随时「取消忽略」恢复检查

表格页支持：关键字搜索（自动匹配文本字段）、任意列排序、分页、行内新增/编辑（表单根据
`information_schema` 列元数据自动生成，JSON 字段自动校验并支持格式化）、删除确认。

## 服务端 API（Nitro）

| 方法   | 路径                               | 说明                                   |
| ------ | ---------------------------------- | -------------------------------------- |
| POST   | `/api/auth/login`                  | 验证数据库连接（自定义配置），返回 `{ ok, version, config }` |
| GET    | `/api/auth/test-local`             | 验证本地默认连接（`runtimeConfig.db`） |
| GET    | `/api/db/stats`                    | 统计信息（行数、版本、表大小、最近记录） |
| GET    | `/api/db/daily-new`                | 近 7 天每日新增（roms 精确 / devices、branches 估算） |
| GET    | `/api/db/meta/:table`              | 表结构元数据（列类型、JSON/日期标记等）  |
| GET    | `/api/db/:table`                   | 分页列表（`page/pageSize/search/sort/order`） |
| POST   | `/api/db/:table`                   | 新增记录                               |
| GET    | `/api/db/:table/:id`               | 单条记录                               |
| PUT    | `/api/db/:table/:id`               | 更新记录                               |
| DELETE | `/api/db/:table/:id`               | 删除记录                               |
| POST   | `/api/db/:table/batch-update-device` | 批量更新设备（机型管理用）           |
| POST   | `/api/db/sql`                      | 只读 SQL 控制台                        |
| GET    | `/api/db/check/:table`             | 单表数据自查（`?summary=1` 仅返回计数） |
| GET    | `/api/db/check/summary`            | 全表自查计数汇总（60s 缓存）            |
| POST   | `/api/db/check/dismiss`            | 忽略违规 `{table, ruleId, rowId?, reason?}`（rowId=0 或缺省 = 整条规则） |
| DELETE | `/api/db/check/dismiss`            | 取消忽略 `{table, ruleId, rowId?}`        |
| GET    | `/api/db/check/dismissals`         | 已忽略列表（`?table=` 可选）          |
| GET    | `/api/devices-manager`             | 机型管理数据（设备列表 + 单机型分支详情 + 全部分支定义） |
| GET    | `/api/series-manager`              | 系列管理数据（全部 series + 设备基准行 + 品牌归属） |

安全措施：表名白名单（`devices|branches|roms|series`）、列名白名单校验、全程参数化查询、
SQL 控制台仅允许只读语句且自动限制行数、JSON 字段服务端二次校验、空串自动转 NULL。

## 技术栈

- Nuxt 4（SPA 模式 `ssr: false`）+ Nitro 服务端路由
- Tailwind CSS（配置与 `app/web` 一致，见 `tailwind.config.ts` / `app/assets/css/main.css`）
- `mysql2`（连接池 + 参数化查询，`server/utils/db.ts`）

## 目录结构

```
app/admin/
├── nuxt.config.ts            # 端口 3100、DB 运行时配置（ssr: false）
├── tailwind.config.ts        # 与 app/web 一致的 Tailwind 配置
├── server/
│   ├── utils/db.ts           # MySQL 连接池（请求级 x-db-config）、表白名单、列元数据、CRUD 辅助
│   ├── utils/checker.ts      # 数据自查规则引擎（依据 db_structure 注释定义违规判定）
│   └── api/
│       ├── auth/             # 数据库连接验证（login / test-local）
│       ├── db/               # stats / daily-new / meta / CRUD / batch-update-device / sql / check
│       ├── devices-manager.get.ts   # 机型管理聚合接口
│       └── series-manager.get.ts    # 系列管理聚合接口
└── app/
    ├── app.vue               # 侧边栏 + 顶栏 + 深色模式布局
    ├── assets/css/main.css   # 与 app/web 同一套设计令牌
    ├── composables/          # useAdminApi / useDarkMode / useDbConfig / useToast
    ├── components/           # TableManager / RecordModal / CheckPanel / Pagination / ConfirmDialog / DbStatus / Toast / DarkModeToggle / InfoItem / SeriesModal
    ├── plugins/              # db-config.client.ts（自动附加 x-db-config 请求头）
    └── pages/                # index(仪表盘) / login / devices / devices-manager / branches / roms / series-manager / sql
```
