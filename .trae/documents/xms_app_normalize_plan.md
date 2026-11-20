# xms_app / xms 库表结构优化（去重 APK 元数据）实施方案

## 1. 仓库研究结论

### 现状与问题
- `xms_app` 表当前是「一次 XMS 推送里的每个应用一行」，唯一键 `(xms_id, pkg)`。
- 同一个 APK（同一 `pkg` + `pkg_ver`）会被多次不同的 XMS 推送（不同分支 / 系统版本）引用，导致其全部元数据被**逐行重复存储**。
- 实测（本地库 miroms）：
  - `xms_app` 共 **2983 行**，但只有 **83 个不同的 `(pkg, pkg_ver)`**（即 83 个真实 APK），平均重复约 **36 倍**。
  - 用户举例的 `pkg_ver=1306003100`（com.android.deskclock）确实有 **36 行**，除 `xms_id` 外完全相同。
  - 对每个 `(pkg, pkg_ver)`，`md5 / file_name / apk_name / sha256 / size / names / logs / release_date` 的 distinct 计数全部为 1（`logs` 为 NULL 时记 0）；且每个 `(pkg, pkg_ver)` 只属于**一个** XMS 版本。
- 结论：`(pkg, pkg_ver)` 是「APK 发布物」的完美主键，所有元数据在该粒度上恒定，可安全地抽离成一张独立表。

### `xms` 表本身
- 104 行，`(code, version, rom_version)` 全部唯一，无整行重复。
- 仅有 `notice / changelog / pkg_cnt / prio` 在同一 `(code, version)` 的多个系统版本行之间重复（最多 5 倍，字段小）。**冗余程度低，不是本次核心问题**；方案默认不动 `xms`（见「可选扩展」）。

### 消费者盘点（决定改动面）
**写入端：**
- `data/scripts/miroms/xms.py` — `XmsStore`：`_store_apps`（写 xms_app）、`canonicalize`、`normalize_push_dates`、`_apk_release_date`、`_existing_app_info`、`enrich_all`、`clean_empty_apps`、`ensure_module_logs`、`migrate_legacy_extras`。
- `data/scripts/backfill_xms_release_dates.py` — `UPDATE xms_app SET release_date ... WHERE xms_id=? AND pkg=?`。
- `data/scripts/import_xms_from_device.py` — 同上，外加 `module_packages()` 读 `xms_app(name,pkg)`。
- `app/admin/server/api/xms/app.post.ts` — INSERT/UPDATE xms_app + 按 pkg 传播 name。
- `app/admin/server/api/xms/propagate.post.ts` — 按 pkg 更新 name/name_en/names/apk_name。
- `app/admin/server/api/db/[table]/[id].put.ts`、`[table].post.ts`、`[id].delete.ts` — 通用表 CRUD（按 `getTableMeta` 动态列）。
- `app/admin/server/api/db/[table]/auto-increment.post.ts` — 重排 xms.id 时更新 `xms_app.xms_id`（保留 xms_id 列即可兼容）。

**读取端：**
- `data/scripts/miroms/exporters.py` `export_xms()` — 一条 `SELECT ... FROM xms_app`，生成 `data/api/v3/xms/*.json`（前端/安卓消费）。
- `app/admin/server/api/xms/apps.get.ts` — `SELECT * FROM xms_app WHERE xms_id=?`。
- `app/admin/server/api/xms/list.get.ts` — `SELECT COUNT(*) FROM xms_app WHERE xms_id=x.id`。
- `app/admin/server/utils/checker.ts` — `xms_app` 的数据质量规则（含按 pkg 跨行一致性校验）。
- `app/web/server/api/xms/download.post.ts` — **不读库**（实时调小米接口），无需改。

**UI：**
- `app/admin/app/pages/xms.vue` — 在某个推送内增删改应用，调用 `/api/xms/app` 与通用 `deleteRecord('xms_app', id)`。
- `app/admin/app/pages/xms_app.vue` — xms_app 原始表浏览页（通用表格组件）。
- `app/admin/app/components/RecordModal.vue` — 对 `table==='xms_app'` 有 names JSON 编辑 / 按 pkg 传播的特判。

## 2. 目标结构

新增一张「APK 发布物」表，把 `xms_app` 瘦身为「推送 ↔ 发布物」的关联表。

### 2.1 新表 `xms_app_release`（一个 APK 一行）
```sql
CREATE TABLE `xms_app_release` (
  `id`           bigint NOT NULL AUTO_INCREMENT,
  `pkg`          varchar(128) NOT NULL DEFAULT '' COMMENT '应用包名',
  `pkg_ver`      varchar(64)  NOT NULL DEFAULT '' COMMENT '应用版本号 versionCode',
  `name`         varchar(128) DEFAULT NULL,
  `name_en`      varchar(128) DEFAULT NULL,
  `names`        longtext     DEFAULT NULL COMMENT '多语言名 JSON',
  `apk_name`     varchar(255) DEFAULT NULL,
  `file_name`    varchar(255) DEFAULT NULL,
  `size`         bigint       DEFAULT NULL,
  `md5`          char(32)     DEFAULT NULL,
  `sha256`       char(64)     DEFAULT NULL,
  `logs`         longtext     DEFAULT NULL COMMENT '更新日志 JSON 数组',
  `release_date` date         DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_release_pkg_ver` (`pkg`,`pkg_ver`),
  KEY `idx_release_md5` (`md5`),
  KEY `idx_release_pkg` (`pkg`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
```
- 以 `(pkg, pkg_ver)` 唯一；一个 APK 只存一份元数据。
- `logs` 上移到此处（实测同一 `(pkg, pkg_ver)` 的日志恒定），后台编辑即「对所有推送生效」，与现有 propagate 语义一致。

### 2.2 改造 `xms_app` 为关联表
```sql
CREATE TABLE `xms_app` (
  `id`          bigint NOT NULL AUTO_INCREMENT,
  `xms_id`      bigint NOT NULL COMMENT '所属推送 xms.id',
  `release_id`  bigint NOT NULL COMMENT '所属 APK xms_app_release.id',
  `pkg`         varchar(128) NOT NULL DEFAULT '' COMMENT '冗余：便于查询与唯一约束，恒等于 release.pkg',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_xms_app_xms_pkg` (`xms_id`,`pkg`),
  UNIQUE KEY `uq_xms_app_xms_release` (`xms_id`,`release_id`),
  KEY `idx_xms_app_release` (`release_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
```
- 保留 `pkg` 列：① 维持「同一推送内一个包名只出现一次」的 DB 级唯一约束；② 让 `SELECT * FROM xms_app WHERE xms_id=?`、checker、auto-increment 等无需大改；③ 与 `release.pkg` 由写入端保证一致（并加一条自查规则校验一致性）。
- 删除原 `xms_app` 的宽列（name/names/apk_name/...），它们只存在于 `xms_app_release`。

### 2.3 不变
- `xms` 表结构不动（仅重复小字段，性价比低）。

## 3. 待改文件清单

**data 子模块（git submodule，需两步提交）：**
- `data/db_structure/xms_app_release.sql` — 新建表结构（含说明注释）。
- `data/db_structure/xms_app.sql` — 改为关联表结构。
- `data/db_structure/migrations/20261121_xms_app_normalize.sql` — 迁移脚本（建 release 表 + 旧表改名归档 + 建关联表）。
- `data/scripts/migrate_xms_app_normalize.py` — 数据回填脚本：从旧 xms_app 选「最完整」行生成 release、再建关联。
- `data/scripts/miroms/xms.py` — 写入/维护逻辑全面改读写两张表。
- `data/scripts/miroms/exporters.py` — `export_xms` 的读取 SQL 改 JOIN。
- `data/scripts/backfill_xms_release_dates.py` — release_date 写到 release 表。
- `data/scripts/import_xms_from_device.py` — 同上；`module_packages` 改读 release 表。

**主仓库 admin：**
- `app/admin/server/utils/db.ts` — `ALLOWED_TABLES` 增加 `xms_app_release`。
- `app/admin/server/utils/checker.ts` — xms_app 规则中「列属性类」（names/logs JSON、size、apk_name 缺失、空条目）迁到 `xms_app_release`；跨行一致性规则（同 pkg names/apk_name 不一致）因单源化而删除；xms_app 仅保留 `pkg` 非空 + 与 release.pkg 一致性校验；`xms_app_release` 纳入 `tableSearches/meta`。
- `app/admin/server/api/xms/apps.get.ts` — `SELECT` 改 JOIN `xms_app_release`，返回 `release_id`。
- `app/admin/server/api/xms/app.post.ts` — upsert release（按 `pkg,pkg_ver` 或 `release_id`）+ upsert 关联；propagate 改为作用于 release（天然全量生效）。
- `app/admin/server/api/xms/propagate.post.ts` — 改为更新 `xms_app_release`（按 pkg）。
- `app/admin/app/components/RecordModal.vue` — `table==='xms_app'` 的 names/propagate 特判扩展到 `xms_app_release`。
- `app/admin/app/pages/xms_app.vue` — 描述文案调整（可选：增加 release 表入口）。
- `app/admin/app/pages/xms.vue` — 适配 `release_id`（保存/删除传 release 维度）。

## 4. 实施步骤（依赖顺序）

1. **建表 + 迁移脚本**（不碰现有表）：新增 `xms_app_release.sql`、更新 `xms_app.sql`、写迁移 SQL 与 Python 回填脚本。回填逻辑：对每个 `(pkg, pkg_ver)` 选「最完整」一行（优先 names/logs/release_date 非空，再取最小 id）写入 release。
2. **执行迁移**（本地库验证用）：
   - `CREATE TABLE xms_app_release` → Python 回填 → 校验 `release 行数 == 83` 且每个旧 xms_app 行都能命中 release。
   - `RENAME TABLE xms_app TO xms_app_archive` → 建新 `xms_app` 关联表 → 从 `xms_app_archive JOIN xms_app_release` 填入 → 校验新 xms_app 行数 == 2983。
   - 保留 `xms_app_archive` 作为回滚备份，验证通过后再 DROP（在计划中标记，不在本次迁移立即删除）。
3. **写入端 `xms.py`**：`_store_apps` 改为「upsert release → upsert 关联」；同步修改 `canonicalize / normalize_push_dates / _apk_release_date / _existing_app_info / enrich_all / clean_empty_apps / ensure_module_logs`。
4. **维护脚本**：`backfill_xms_release_dates.py`、`import_xms_from_device.py` 改写到 release 表。
5. **导出端 `exporters.py`**：读取 SQL 改 `xms_app JOIN xms_app_release`。
6. **管理后台**：db 白名单 → checker → 三个 xms API → RecordModal / 页面。
7. **联调验证**（见下节）。
8. `graphify update .` 刷新知识图谱。

## 5. 验证

- **行数**：迁移后 `SELECT COUNT(*) FROM xms_app` = 2983（不变），`SELECT COUNT(*) FROM xms_app_release` = 83；`pkg_ver=1306003100` 在 release 表只有 1 行、在 xms_app 仍有 36 条关联（指向同一 release_id）。
- **无损**：`xms_app_archive` 与「新 xms_app JOIN xms_app_release」按旧列做全字段 MINUS 比对，差异为 0。
- **导出一致**：迁移前跑一次 `export_xms()` 存快照；迁移 + 改完后再跑一次，`data/api/v3/xms/` 下全部 JSON 字节级 diff 为空（前端/安卓无感）。
- **后台可用**：`/api/xms/list`、`/api/xms/apps?xmsId=` 返回正常；xms 页面能编辑应用名 / 日志并保存，编辑一处后同包名所有推送同步变化（JOIN 验证）；删除某推送里的应用只删关联、不影响其它推送的 release。
- **类型检查**：`cd app/admin && ../../node_modules/.bin/tsc --noEmit -p tsconfig.json` 无新增错误。
- **自查**：checker 对 xms_app / xms_app_release 不报因结构变更产生的误报。

## 6. 风险与对策

- **迁移中写入冲突**：迁移在抓取任务不运行时执行；先建 release 再切换，`RENAME` 原子切换，窗口极短。回滚：`DROP TABLE xms_app; RENAME TABLE xms_app_archive TO xms_app; DROP TABLE xms_app_release;`。
- **`pkg` 冗余列漂移**：写入端统一维护；checker 增加「xms_app.pkg ≠ 对应 release.pkg」规则，发现即报。
- **导出 JSON 变化**：以迁移前快照做字节级 diff，任何差异都需排查；目标是零差异。
- **admin 通用 CRUD 对视图的误解**：本方案 `xms_app` 仍是**真实表**（非视图），通用 PUT/DELETE 可用；release 表加入白名单后同样可在通用表浏览器编辑。
- **logs 粒度假设**：实测同一 `(pkg, pkg_ver)` 日志恒定才上移；若未来出现同版本不同推送日志不同，需把 logs 留在关联表——已在验证中覆盖（回填时统计 logs distinct，若 >1 则报警并中止）。

## 7. 可选扩展（不在本次默认范围，需确认）

- **`xms` 表 `notice/changelog/pkg_cnt/prio` 归一**：把同一 `(code, version)` 共享的字段抽到 `xms_version` 表。收益小（104 行、字段小）、改动面大，**默认不做**；如需要可作为后续单独变更。
