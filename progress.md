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
backend/             ← FastAPI 后端（M0 完成）
frontend/            ← Vue 3 前端（M0 完成）
.github/workflows/   ← CI pipeline（M0 完成）
openspec/changes/    ← m0-engineering-skeleton (archived), m1-data-ingestion-windows (ready)
```

## 关键设计决策

- 投影统一 EPSG:4326，前后端和绘图全链路一致
- qpf = tp(end_lead) - tp(start_lead) 差分计算
- ptype 由窗口内多 3h 文件合成，qpf_step > ptype_qpf_threshold_mm 过滤
- 编辑只处理 qpf + ptype，IFS 多要素仅进入复盘绘图
- 角色四类：admin / reviewer / forecaster / viewer
- 数据扫描权限：admin / reviewer（forecaster 不可触发）
- 绘图三层架构：原始脚本(只读) → plotter 纯函数 → 任务服务
- 中文产品：全界面中文，API message 中文，TDesign zh-CN，北京时间 UTC+8
- case_id 格式 YYYYMMDDHH，HH 仅允许 08/20，时区 UTC

## 当前阶段

**阶段：M3 #40 PreviewStatsPanel + OperationHistory 组件完成，等待后续 M3 编辑参数面板/版本保存任务**

### M0 完成总结（2026-05-16）

M0 工程骨架全部完成：后端 FastAPI + SQLAlchemy + Alembic + JWT 认证、前端 Vue 3 + TDesign + Pinia、DevOps CI 六 job。Epic #1 + Issues #2-#6 关闭。

### M1 当前状态

- OpenSpec Change `m1-data-ingestion-windows` 4/4 artifacts complete
- Codex 三路审核完成，P0 问题已修复（错误码/配置/响应格式/权限/字段对齐/状态规则/lead校验）
- GitHub Issues 已创建：Epic #12 + 5 子任务 #13-#17
- 依赖链：#13 基础设施 → #14 Engines → #15 Services → #16 API → #17 Frontend

### 待实现

- [x] M1 #13: 基础设施与数据模型
- [x] M1 #14: Engines 纯函数层
- [x] M1 #15: 服务与仓储层
- [x] M1 #16: API 层
- [x] M1 #17: 前端窗口选择器
- [x] M3 #36: shapely 依赖、edit_operation 迁移/模型/Repository
- [x] M3 #37: MaskBuilder、EditOps、统计、Preview 缓存、Replay
- [x] M3 #38: 后端 Edit API preview/apply/undo/redo/operations
- [x] M3 #39: 前端 API/Store 集成（editApi + editorStore 扩展）
- [x] M3 #40: PreviewStatsPanel 与 OperationHistory 组件集成
- [ ] M3 后续：编辑参数面板、版本保存与审核流前端集成

### 后续里程碑

- [ ] M2：地图与编辑工作台
- [ ] M3：编辑引擎与操作留痕
- [ ] M4：版本保存、审核与发布
- [ ] M5：复盘中心与绘图任务队列
- [ ] M6：运维、统计、配置与试运行

### 阻塞项

- 无

## 最近变更记录

| 日期 | 变更 |
|---|---|
| 2026-05-17 | **M3 #40 完成**：新增 PreviewStatsPanel/OperationHistory 右侧面板、相态选择弹窗与组件测试，frontend vue-tsc + 100 tests 全绿 |
| 2026-05-17 | **M3 #39 完成**：新增前端 editApi 五端点封装，扩展 editorStore preview/apply/undo/redo/operations 状态管理与 after 字段刷新，frontend vue-tsc + 92 tests 全绿 |
| 2026-05-17 | **PR #43 Round 1 修复**：补齐 target_ptype replay、undo/redo preview 失效、条件化参数持久化与 geometry 有限数校验，后端 204 tests + ruff 全绿 |
| 2026-05-17 | **M3 #38 完成**：新增 Edit API schemas/routes、preview/apply/undo/redo/operations 集成测试、mask 持久化与缺失错误码/TTL 配置，后端 202 tests + ruff + mypy 全绿 |
| 2026-05-17 | **PR #42 Round 1 修复**：replay 兼容 ORM 属性对象、统计 transition 纳入 valid_mask、编辑参数拒绝 NaN/Inf、preview cache 错误码与锁保护对齐，后端 187 tests + ruff + mypy 全绿 |
| 2026-05-17 | **M3 #37 完成**：新增 edit_engine 纯函数模块（MaskBuilder/EditOps/Stats/PreviewCache/Replay）与 48 个专项测试，后端 181 tests + ruff + mypy 全绿 |
| 2026-05-17 | **PR #41 Round 1 修复**：edit_operation 增加 session+sequence 唯一索引与 is_undone CHECK 约束，模型/迁移/测试对齐，后端 133 tests + ruff 全绿 |
| 2026-05-17 | **M3 #36 完成**：新增 shapely 依赖、v006 edit_operation 迁移、EditOperation 模型与 Repository，后端 132 tests + ruff 全绿 |
| 2026-05-17 | **M1 #17 完成**：新增前端 data API、windowStore、扫描进度/窗口选择/QC 组件及 12 个前端测试，frontend build + 28 tests 全绿 |
| 2026-05-17 | **M1 #16 完成**：新增 data_scan/windows API、JWT 权限控制、扫描状态查询与 17 个 API 测试，后端 102 tests 全绿 |
| 2026-05-17 | **M1 #15 完成**：新增 forecast_case/product_window/data_scan_log 仓储、DataScanService/worker 与 8 个扫描集成测试，后端 85 tests 全绿 |
| 2026-05-17 | **M1 #14 完成**：新增 grid_io/qpf_builder/ptype_builder/archive_builder、numpy 依赖与 27 个 engines/storage 测试，后端 77 tests 全绿 |
| 2026-05-16 | **M1 #13 完成**：新增 grid/config/error registry/PathBuilder、三张数据表模型与 v004 迁移，并补齐后端测试与 Alembic roundtrip 验证 |
| 2026-05-16 | **M1 Pipeline 完成**：OpenSpec change 创建 + Codex 三路审核 + P0 修复（错误码/配置/权限/字段/状态规则）+ GitHub Issues Epic #12 + #13-#17 创建 |
| 2026-05-16 | schemas 更新：error_codes.json 新增 scan 组 + window 组 3 码；product_config.json 新增 init_time_zone/ptype_qpf_threshold_mm/allow_zero_start_lead_fallback |
| 2026-05-16 | **M0 全部完成**：后端骨架/DB/认证/前端骨架/DevOps CI 五个 Issue 关闭 |
| 2026-05-15 | 设计文档全量审核定稿；CI 策略/索引口径/阶段计划对齐；前端 UI 交互细则与技术栈冻结 |
