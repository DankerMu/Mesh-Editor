# Mesh-Editor 项目进度

> 本文件是每次 AI session 的冷启动上下文。完成任务后必须更新本文件。最多 200 行。

## 项目概述

固定区域（70-111°E, 25-50°N）5km 等经纬度网格（501×821）累计降水与雨雪相态编辑及复盘系统。
技术栈：Vue 3 + OpenLayers + TDesign / Python FastAPI / Matplotlib + Cartopy / Docker。

## 仓库结构

```
建设方案.md          ← V2.0 主文档，文档索引入口
docs/01-09           ← 业务方案与功能设计
docs/10-21           ← 工程开发设计与阶段 PRD
docs/22-26           ← 前端 UI/UX 规格与效果图落地规范
schemas/             ← error_codes.json、product_config.json、frontend_ui_tokens.json
backend/             ← FastAPI 后端（392 tests）
frontend/            ← Vue 3 前端（146 tests）
.github/workflows/   ← CI pipeline（6 jobs）
openspec/changes/    ← 变更管理
checklists/          ← E2E 测试清单与结果
data/source/         ← 样例数据符号链接（→ data/量级/ + data/phase/）
```

## 关键设计决策

- 投影统一 EPSG:4326，前后端和绘图全链路一致
- qpf = tp(end_lead) - tp(start_lead) 差分计算
- ptype 由窗口内多 3h 文件合成，qpf_step > ptype_qpf_threshold_mm 过滤
- 编辑只处理 qpf + ptype，IFS 多要素仅进入复盘绘图
- 编辑引擎为纯函数模块，preview/apply 分离，undo/redo 基于操作链重放
- 角色四类：admin / reviewer / forecaster / viewer
- 绘图三层架构：原始脚本(只读) → plotter 纯函数 → 任务服务
- 中文产品：全界面中文，API message 中文，TDesign zh-CN，北京时间 UTC+8
- case_id 格式 YYYYMMDDHH，HH 仅允许 08/20，时区 UTC

## 当前阶段

**M0-M6 + Epic #81 全部完成 → UI 视觉深度对齐阶段；Issue #109 CSS Token 系统完成**

### 里程碑总览

| 里程碑 | 完成日期 | 摘要 |
|---|---|---|
| M0 工程骨架 | 2026-05-16 | FastAPI+SQLAlchemy+Alembic+JWT、Vue3+TDesign+Pinia、CI 六 job |
| M1 数据摄入与窗口 | 2026-05-17 | grid_io/qpf/ptype/archive engines、DataScan worker、windows API、前端窗口选择器 |
| M2 地图与编辑工作台 | 2026-05-17 | Session API、editorStore、OpenLayers BaseMap+WebGL DataTile、选区工具、EditorView |
| M3 编辑引擎与操作留痕 | 2026-05-17 | MaskBuilder+EditOps+Stats+Preview+Replay、Edit API 5 端点、前端编辑面板 |
| M4 版本保存、审核与发布 | 2026-05-18 | 版本快照、审核流程、发布管理全链路 |
| M5 复盘中心与绘图任务队列 | 2026-05-18 | Plotter/Templates/ReviewService/PlotTaskService/API/前端 |
| M6 运维、统计、配置与试运行 | 2026-05-18 | User/Audit/Config/Template/Monitor/Stats API + 管理页 + E2E 试运行 |

**测试：backend 392 tests + frontend 372 passed / 1 skipped；Issue #93 frontend vitest/vue-tsc 通过**

### E2E 测试执行结果（2026-05-18）

用 agent-browser 对真实样例数据（case_id=2025122208，53 tp + 53 ptype 文件）执行前 3 类 E2E 测试：
- 认证与权限：13/13 通过（4 种角色路由守卫、登录/登出/Token 保持全部正确）
- 数据扫描与窗口：14/14 通过（23 窗口=5 available+4 partial+14 invalid，三 Tab 分组正确）
- 编辑器工作台：8/8 通过（会话/字段加载/7 视图模式/地图渲染/preview/apply/undo/redo/save）
- 附加编辑操作：8/8 通过（QPF increase/新降水 ptype 弹窗/一致性拦截/版本保存）

### 已修复的 Bug

| Bug | 修复 | 文件 |
|---|---|---|
| TDesign 全局注册失败 | `app.use(TDesign)` 无参 + ConfigProvider locale | `main.ts`、`App.vue` |
| HomeView 窗口点击无跳转 | 添加 watch selectedWindowId → router.push | `HomeView.vue` |
| frontend_ui_tokens.json 缺 shadow | 补充 shadows 分类 | `frontend_ui_tokens.json` |

### UI 视觉深度对齐（进行中）

**Phase 1 已完成**: Epic #81 + #82-#88（全部 CLOSED）— 基础 Token/Header/登录/面板骨架

**Phase 2 — 4 个新 Change + 4 Epic + 12 子 issue**：

| Epic | Change | 子 Issue | 范围 |
|------|--------|----------|------|
| #105 全局基底 | `ui-global-foundation` | #109 #110 #111 | Token 补全/Header 子菜单/Admin 统一 |
| #106 首页重做 | `home-page-redesign` | #112 #113 #114 | 三栏布局/筛选/表格/详情 |
| #107 编辑器对齐 | `editor-visual-polish` | #115 #116 #117 | 左栏/面板激活/状态栏/预览 |
| #108 审核+复盘 | `approval-review-redesign` | #118 #119 #120 | 三栏/版本对比/操作面板/复盘图 |

**Codex 4×3 路审核 + P0 修复完成**：API 契约/Store 方法名/状态枚举/字段名全部对齐现有代码

### 阻塞项

- 暂无

### 样例数据

- `data/量级/20251222/` — tp 53 文件（case_id=2025122208，0-156h 每 3h）
- `data/phase/20251222/` — ptype 53 文件
- `data/source/` — 符号链接映射到后端期望路径结构
- 启动后端需设 `DATA_SOURCE_ROOT=/path/to/data/source`

### 最近变更记录

- 2026-05-19：完成 Issue #109 CSS Token 补全：font-weight/TDesign light 覆盖/Login 渐变 token 化/JSON 同步测试，frontend vitest 384 passed / 1 skipped
- 2026-05-19：Stage Change Pipeline 完成 — 4 个 OpenSpec Change + Codex 12 路审核 + P0 修复 + 4 Epic(#105-#108) + 12 子 issue(#109-#120) 创建
- 2026-05-19：完成 Issue #93 smooth 控件前端：DrawTools toggle/sigma、editorStore smooth_sigma、PreviewStats badge，frontend vitest/vue-tsc 通过
- 2026-05-19：完成 Issue #92 lasso 前端：DrawTools 套索交互、SelectionOverlay 渲染、类型/API 接入，frontend 365 tests + vue-tsc 通过
- 2026-05-19：完成 Issue #91 mask smooth 后端：smooth_mask/scipy/smooth_sigma preview 接入，targeted 38 tests 通过
- 2026-05-18：完成 PR #101 Round 1 修复：保留 MultiPolygon 全几何、lasso 10000 点上限、补 apply/回归测试
- 2026-05-18：完成 Issue #90 套索 mask 后端：lasso_to_mask、自交叉 buffer(0) 修复、API/schema/enums 接入与 targeted tests
- 2026-05-18：审核 lasso-smooth-tool tasks.md 可执行性，输出 P0/P1 与 spec→task 覆盖映射
- 2026-05-18：完成 lasso-smooth-tool OpenSpec 设计一致性审核，发现 API 契约与异常类型等 P0/P1 风险
- 2026-05-18：创建 E2E 测试清单（12 大类 ~120 用例）+ 执行前 3 类（43 通过/0 失败）
- 2026-05-18：修复 TDesign 注册 bug + HomeView 跳转 + 数据源路径配置，推送 a66e049
- 2026-05-18：完成 ui-visual-alignment OpenSpec Change（proposal/design/7 specs/tasks）
- 2026-05-18：Codex 3 路并行审核，发现 12 个 P0，全部修复
- 2026-05-18：创建 GitHub Epic #81 + 7 个子 issue #82-#88
- 2026-05-18：审核 lasso-smooth-tool 4 个 spec，发现 API/geometry/smooth/交互兼容性 P0 缺口
