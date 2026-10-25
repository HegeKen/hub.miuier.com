# MiROMS HUB - 数据管理后台 (app/admin)

> 🌐 **简体中文:** [README.md](README.md) · **English:** [README_EN.md](README_EN.md)

管理后台：**直连 MySQL 数据库**（`miroms` 库），直接对 `devices` / `branches` / `roms` / `series` / `logs` / `dev` 六张表进行增删改查，
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

- **本地默认连接**：取值优先级为 **`NUXT_DB_*` 环境变量 → `data/scripts/config.py` → 内置默认值**
  （`localhost:3306` / `root` / `miroms`）。dev 环境下数据仓库的 `data/scripts/config.py` 就在同一个仓库里，
  后台会直接复用它，所以本地开发无需额外配置；生产环境请用环境变量注入。
  覆盖方式：把 `app/admin/.env.example` 复制为 `app/admin/.env`（已被 gitignore）后修改，或直接设置环境变量。
  启动日志会打印实际使用的连接与来源（如 `root@localhost:3306/miroms（来源：data/scripts/config.py）`）。

| 环境变量             | 默认值      | 说明       |

| 环境变量             | 默认值      | 说明       |
| -------------------- | ----------- | ---------- |
| `NUXT_DB_HOST`       | `localhost` | 数据库地址 |
| `NUXT_DB_PORT`       | `3306`      | 端口       |
| `NUXT_DB_USER`       | `root`      | 用户名     |
| `NUXT_DB_PASSWORD`   | 同 `data/scripts/config.py` | 密码 |
| `NUXT_DB_NAME`       | `miroms`    | 库名       |

> 「本地数据库」连不上时，`/api/auth/test-local` 的报错会直接给出排查指引（配置 `.env` 或用「自定义连接」）。

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
- **日志录入** `/logs` — 手动录入更新日志：多行文本按换行符拆成多条，写入 `logs` 表（页内「去重体检」可一键检查重复分组与孤儿行）
- **开发版绑定** `/dev-bind` — 手动把 ROM 关联到某一周开发版：左侧是该周已绑定的 ROM（可单条 / 全部解绑），
  右侧按机型代号 · 版本号 · 包文件名检索候选（可按年份和绑定状态过滤），勾选后「绑定选中」。
  开发版周报的机型清单不单独建表——绑定关系就是 `roms.dev_week`，导出与周报页都按
  `dev_week + zone` 现算；老周报（发布会只有机型列表、没有版本号）和延期公告这类
  自动匹配不上的周次，在这里补齐。检索框会自动用该周版本号预填，命中的完整版本号会以
  标签列出（例如公告写 `5.20`、库里是 `21.5.20` / `22.5.20`，避免挑错年份）
  （同一日志的各语言版本共用一行）；录入前可「解析比对」预览每行是新增还是命中已有分组，
  保存时会用库中已有数据反推补齐其他语言（把兼容的重复分组的语言文本合并到一起）。
  右侧可检索库中日志、按「缺少某语言」筛选，查看每种语言的分组数
- **开发版周报** `/dev` — `dev` 表 CRUD（开发版周更公告、机型与 ROM 清单、更新日志）
- **XMS 应用更新** `/xms` — XMS（SOTA 系统应用更新）推送与应用明细：按机型 / 版本 / 关键词筛选，
  展开推送编辑每个应用的包名、中英文名与多语言 `names`、版本号、安装包信息与更新日志（JSON），
  可新增 / 删除应用；推送本身（备注、`changelog` 无包名模块、是否最新、推送日期等）走通用记录弹窗。
  XMS 版本号带 OS 标注（C → HyperOS 3、D → HyperOS 4），无包名模块只存在推送的 `changelog` 里。
  修改应用名时可用「用 logs 表补全翻译」按中文 / 英文名匹配 `logs` 的 module 行补齐缺失语种；
  保存时默认把 `name` / `name_en` / `names` **同步到所有 pkg 相同的推送**（日志按推送各自保留，
  可在表单里取消勾选）。手工修过的 `apk_name` 会被后续抓取的新推送沿用（同机型优先），
  避免接口返回的杂乱名称覆盖人工修复。应用名在 `logs` 的 module 行里**不存在时会自动登记一条 zh/en 占位**
  （保存、点「用 logs 表补全翻译」、抓取脚本写入时都会触发），之后可在「日志录入」里继续补全其它语种
- **XMS 应用明细** `/xms_app` — `xms_app` 表 CRUD（多语言 `names` 始终以 JSON 文本框编辑，不会被截断；
  该表没有品牌字段，所以不显示设备表那个「从分支生成」，改为「用 logs 表补全翻译」按 `name` / `name_en` 补语种）。
  自查额外检查 `names` / `apk_name` **缺失**与**同包名不一致**（平板 / 折叠等变体的 apk_name 差异会作为警告列出，便于人工核对）。
  编辑记录时底部有「保存后把 name / name_en / names / apk_name 同步到同包名的所有记录」勾选项（默认开），
  自查里发现的问题可以一次修完同包名的全部行；apk_name 变体需要保留时取消勾选即可
- **SQL 控制台** `/sql` — 只读查询（SELECT / SHOW / DESCRIBE / EXPLAIN / WITH），自动追加 `LIMIT 1000`
- **数据自查** — 每个模块工具栏的「自查」按钮 + 仪表盘「运行自查」：依据 `data/db_structure/*.sql`
  中每列的注释与示例，主动筛选不符合逻辑的字段（枚举值、格式正则、JSON 合法性、空字符串、包文件名等），
  展示违规计数与样本，样本可直接跳转编辑；汇总区的「违规合计 / 错误 / 警告」可点击按级别筛选规则列表
- **AUTO_INCREMENT 修复** — 每个表的自查面板会显示当前 `AUTO_INCREMENT` 与 `MAX(id)`（以及跳号数），
  点「重置自增 ID」把计数器重置为 `MAX(id)+1`，去掉删除/回滚留下的尾部空号；
  只影响后续新增行的编号，**不会重排已有 ID**（重排会破坏 `xms_app.xms_id`、`roms.dev_week` 等引用关系）
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
| GET    | `/api/db/:table/auto-increment`    | 查看自增状态：当前 `AUTO_INCREMENT`、`MAX(id)`、跳号数 |
| POST   | `/api/db/:table/auto-increment`    | 重置 `AUTO_INCREMENT` 为 `MAX(id)+1`（只影响后续新行） |
| POST   | `/api/logs/manual`                 | 手动录入日志 `{text, type?, lang?, preview?}`：按换行拆行 → 比对 `logs` 表 → 写库 / 补齐其他语言 |
| GET    | `/api/logs/list`                   | 日志分组列表（`page/pageSize/search/type/missing`），含各语言覆盖数 |
| GET    | `/api/logs/audit`                  | 去重体检：重复分组 / 重复行数 / 未被 `roms`·`dev` 引用的孤儿行 |
| GET    | `/api/dev/weeks`                   | 开发版周次一览（含每周已绑定 ROM / 机型数，`bound=0` 即待手动绑定） |
| GET    | `/api/dev/bind`                    | 某周已绑定的 ROM 列表（`week` / `region`） |
| GET    | `/api/dev/roms`                    | 候选 ROM 检索（`search` / `week` / `region` / `bound` / `year` / 分页，附完整版本号候选） |
| POST   | `/api/dev/bind`                    | 绑定 / 解绑：`{ week, region, action: bind\|unbind, romIds, force }` |
| GET    | `/api/devices-manager`             | 机型管理数据（设备列表 + 单机型分支详情 + 全部分支定义） |
| GET    | `/api/series-manager`              | 系列管理数据（全部 series + 设备基准行 + 品牌归属） |
| GET    | `/api/xms/list`                    | XMS 推送列表（`page/pageSize/search/latest/version`），附应用数与机型名 |
| GET    | `/api/xms/apps`                    | 某次推送（`xmsId`）的应用明细 + 推送的 `changelog`（无包名模块） |
| GET    | `/api/xms/translate`               | 按 `name` / `nameEn` 查 `logs` 的 module 多语言译文，用于补全应用名 |
| POST   | `/api/xms/app`                     | 新增 / 更新应用并同步同包名：校验 JSON、用 `logs` 补全译文、`propagate` 默认把名称同步到同 pkg 的所有行 |
| POST   | `/api/xms/module`                  | 把应用名登记到 `logs` 的 module 行（不存在时创建 zh/en 占位），供后续补全其它语种 |
| POST   | `/api/xms/propagate`               | 把 `name` / `name_en` / `names` / `apk_name` 同步到同包名的所有记录（`xms_app` 自查修复用） |

安全措施：表名白名单（`devices|branches|roms|series|logs|dev`）、列名白名单校验、全程参数化查询、
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
    └── pages/                # index(仪表盘) / login / devices / devices-manager / branches / roms / series-manager / logs / sql
```

## 相关文档

| 文档 | 简体中文 | English |
| --- | --- | --- |
| 项目总览 | [../../README.md](../../README.md) | [../../README_EN.md](../../README_EN.md) |
| 前端站点 | [../web/README.md](../web/README.md) | [../web/README_EN.md](../web/README_EN.md) |
| 本后台 | [README.md](README.md) | [README_EN.md](README_EN.md) |
| 数据仓库 | [../../data/README.md](../../data/README.md) | [../../data/README_EN.md](../../data/README_EN.md) |

## Credits

后台的构建与运行依赖以下开源项目，谨向各作者与社区致谢。

| 名称 | 用途 | 许可 |
| --- | --- | --- |
| [Nuxt 4](https://nuxt.com/) | 应用框架（SPA `ssr: false`、构建产物 `.output/`） | MIT |
| [Vue 3](https://vuejs.org/) | 视图层（页面 / 组件 / 组合式函数） | MIT |
| [Nitro](https://nitro.build/) | `server/api/**` 服务端路由与运行时 | MIT |
| [TypeScript](https://www.typescriptlang.org/) | 类型系统 | Apache-2.0 |
| [Tailwind CSS](https://tailwindcss.com/) 与 [@nuxtjs/tailwindcss](https://tailwindcss.nuxtjs.org/) | 样式体系与设计令牌（与 `app/web` 同一套） | MIT |
| [mysql2](https://github.com/sidorares/node-mysql2) | MySQL 连接池、参数化查询与预处理语句 | MIT |
| [MySQL](https://www.mysql.com/) | 数据存储（`miroms` 库：`devices` / `roms` / `branches` / `series`） | GPL-2.0 / 商业许可 |

> 管理的数据内容与相关商标归小米科技所有；本项目与小米科技无隶属关系。
