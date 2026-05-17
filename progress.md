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
backend/             ← FastAPI 后端（204 tests）
frontend/            ← Vue 3 前端（100 tests）
.github/workflows/   ← CI pipeline（6 jobs）
openspec/changes/    ← 变更管理
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

**阶段：M3 完成，准备进入 M4 版本保存与审核**

### 已完成里程碑

| 里程碑 | 完成日期 | Epic | 摘要 |
|---|---|---|---|
| M0 工程骨架 | 2026-05-16 | #1 ✅ | 后端 FastAPI+SQLAlchemy+Alembic+JWT、前端 Vue3+TDesign+Pinia、CI 六 job |
| M1 数据摄入与窗口 | 2026-05-17 | #12 ✅ | grid_io/qpf/ptype/archive engines、DataScanService+worker、windows API+JWT 权限、前端窗口选择器 |
| M2 地图与编辑工作台 | 2026-05-17 | #23 ✅ | Session API、editorStore、OpenLayers BaseMap+WebGL DataTile、选区工具、EditorView 五区工作台 |
| M3 编辑引擎与操作留痕 | 2026-05-17 | #35 ✅ | MaskBuilder+EditOps+Stats+Preview+Replay、Edit API 5端点、前端 editApi/Store/PreviewStatsPanel/OperationHistory |

### M3 交付物

| PR | 内容 | 测试 |
|---|---|---|
| #41 | shapely + edit_operation 迁移/模型/Repo | 8 后端 |
| #42 | 编辑引擎核心 5 模块（纯函数） | 57 后端 |
| #43 | Edit API（preview/apply/undo/redo/operations） | 11 集成 |
| #44 | 前端 editApi + editorStore 扩展 | 6 store |
| #45 | PreviewStatsPanel + OperationHistory | 8 组件 |

### 后续里程碑

- [ ] M4：版本保存、审核与发布
- [ ] M5：复盘中心与绘图任务队列
- [ ] M6：运维、统计、配置与试运行

### 阻塞项

- 无

## 最近变更记录

| 日期 | 变更 |
|---|---|
| 2026-05-17 | **M3 Epic #35 关闭**：5 个 PR merged，编辑引擎全链路闭环（选区→编辑→预览→应用→撤销/重做→操作留痕→前端面板），304 tests 全绿 |
| 2026-05-17 | M2 全部完成并关闭 Epic #23：Session API + 前端地图/选区/EditorView + post-merge 修复 |
| 2026-05-17 | M1 全部完成并关闭 Epic #12：数据摄入+窗口选择全链路就绪 |
| 2026-05-16 | M0 全部完成并关闭 Epic #1：后端骨架/DB/认证/前端骨架/DevOps CI |
