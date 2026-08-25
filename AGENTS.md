# AGENTS.md

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

When the user types `/graphify`, use the installed graphify skill or instructions before doing anything else.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- Dirty graphify-out/ files are expected after hooks or incremental updates; dirty graph files are not a reason to skip graphify. Only skip graphify if the task is about stale or incorrect graph output, or the user explicitly says not to use it.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).

## 项目概览

Monorepo（pnpm workspace 脚本见根 `package.json`）：

- `app/web/` — 前端站点（Nuxt 4，SPA 消费 `https://api.miuier.com/api`），部署于 hub.miuier.com。
- `app/admin/` — 管理后台（Nuxt 4，端口 3100，`ssr: false`），直连 MySQL（`miroms` 库），数据访问层在 `app/admin/server/utils/db.ts`，数据自查引擎在 `server/utils/checker.ts`。
- `data/` — ROM 数据仓库，**git submodule**（HegeKen/miroms，部署于 api.miuier.com）。其内容在子仓库内维护，主仓库只记录 gitlink 指针。
- `graphify-out/` — 知识图谱生成物，不要手改。

## 常用命令

```bash
pnpm dev          # 同时启动 web + admin
pnpm dev:web      # 仅前端（端口 3000）
pnpm dev:admin    # 仅管理后台（端口 3100）
```

管理后台修改后建议做类型检查（Nuxt 项目 tsc 走 `.nuxt` 生成的 tsconfig）：

```bash
cd app/admin && ../../node_modules/.bin/tsc --noEmit -p tsconfig.json
```

## 提交规范

- 提交消息需带项目前缀（`[admin]` / `[web]` / `[data]` / `[root]`），详见 `.trae/rules/git-commit-message.md`。
- 改动 `data/` 时需**两步提交**：先在 `data/` 子仓库内提交，再回到主仓库提交子模块指针更新。
- 文档/知识图谱相关改动同样遵守前缀规则（`[root]`）。

## 文档

- 项目总览：`README.md`
- 管理后台：`app/admin/README.md`
- 数据仓库（API 格式 / 表结构 / 脚本）：`data/README.md`
