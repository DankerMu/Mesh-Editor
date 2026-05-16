# Mesh-Editor 项目进度

> 本文件是每次 AI session 的冷启动上下文。完成任务后必须更新本文件。最多 200 行。

## 项目概述

固定区域（70-111°E, 25-50°N）5km 等经纬度网格（501×821）累计降水与雨雪相态编辑及复盘系统。
技术栈：Vue 3 + OpenLayers + TDesign / Python FastAPI / Matplotlib + Cartopy / Docker。

## 仓库结构

```
建设方案.md          ← V2.0 主文档，文档索引入口
docs/01-09           ← 业务方案与功能设计
docs/10-21           ← 工程开发设计与阶段 PRD（2026-05-15 合并自 dev-design-pack）
docs/22-26           ← 前端 UI/UX 规格与效果图落地规范
docs/assets/frontend/effects/ ← 效果图资产（F01-F06 已生成）
checklists/          ← 开发检查清单（含前端 UIUX 开发检查清单）
schemas/             ← error_codes.json、frontend_ui_tokens.json、frontend_effect_screens.json
data/                ← 测试数据（EC预报/phase/量级，大文件已 gitignore）
reference/           ← 参考项目（yy-webgis-bj 前端、cdsz 后端、要素场绘图脚本）
设计效果图/           ← 设计效果图原始文件（只读参考）
```

## 文档体系

| 范围 | 文件 | 职责 |
|---|---|---|
| 业务方案 | 01-背景与目标 | 项目背景、建设边界 |
| 业务方案 | 02-架构与流程 | 8 层业务平台架构、编辑/复盘流程 |
| 业务方案 | 03-数据与编码 | 窗口设计、数据对象、相态编码 |
| 业务方案 | 04-前端设计 | 编辑界面、PrecipPhaseGridLayer |
| 业务方案 | 05-编辑功能 | 多边形/笔刷/降水/相态算法、一致性规则 |
| 业务方案 | 06-版本与复盘 | 版本留痕、复盘 payload、IFS 变量、绘图模板 |
| 业务方案 | 07-数据库与API | 7+ 张表、REST API |
| 业务方案 | 08-技术与绘图 | 绘图三层架构、函数签名、色标 |
| 业务方案 | 09-实施与验收 | 6 期实施、4 角色、验收标准、Docker 部署 |
| 工程设计 | 10-开发总蓝图 | 领域模型、对象关系、端到端链路 |
| 工程设计 | 11-后端工程设计 | 目录结构、服务边界、Repository、Worker |
| 工程设计 | 12-前端工程设计 | 路由、组件、Store、图层 |
| 工程设计 | 13-数据适配与文件IO | tp/ptype 读取、qpf 差分、缓存 |
| 工程设计 | 14-编辑引擎详细设计 | mask、EditOps、preview/apply、undo/redo |
| 工程设计 | 15-绘图任务与复盘服务 | 绘图队列、复盘包、失败重试 |
| 工程设计 | 16-状态机与错误码 | 6 个状态机、统一错误码 |
| 工程设计 | 17-API契约与Schema | Pydantic Schema、权限、审计、幂等 |
| 工程设计 | 18-测试用例与验收数据 | 测试分层、样例数据 |
| 工程设计 | 19-开发任务拆解 | M0-M6 里程碑 |
| 工程设计 | 20-对齐修改建议 | 已执行完毕 |

## 关键设计决策

- 投影统一 EPSG:4326，前后端和绘图全链路一致
- qpf = tp(end_lead) - tp(start_lead) 差分计算
- ptype 由窗口内多个 3h ptype 文件合成：读取 (start_lead, end_lead] 内各 3h 时效；仅 qpf_step > qpf_threshold 的时效参与；逐格点统计 has_rain / has_snow；雨=1，雪=2，雨雪均出现=3，无有效降水=0
- 编辑只处理 qpf + ptype，IFS 多要素仅进入复盘绘图
- 角色四类：admin / reviewer / forecaster / viewer
- 绘图三层架构：原始脚本(只读) → plotter 纯函数 → 任务服务
- 中文产品：全界面中文，不做国际化，API message 中文，TDesign zh-CN，北京时间 UTC+8

## 当前阶段

**阶段：设计文档定稿，进入 M0 工程骨架搭建**

### 已完成

- [x] 业务方案文档 01-09 编写
- [x] 工程设计文档 10-20 编写
- [x] 设计补全包合并到主仓库
- [x] 文档对齐修改（版本号、架构、角色、里程碑）
- [x] 创建 progress.md / CLAUDE.md / AGENTS.md
- [x] 设计文档全量审核与修复（2026-05-15，详见下方）

### 进行中

- [ ] M0：工程骨架与基础设施（含配置、权限、JWT、PathBuilder、错误码、迁移）

### 待开始

- [ ] M1：数据接入与窗口生成
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
| 2026-05-15 | **CI 策略落地**：docs/21 新增 §21.11 CI 策略（M0 建 pipeline、M1-M6 增量测试）；M0-M6 验收标准均追加 CI 通过项；检查清单每阶段追加 CI 增量检查；AGENTS.md 追加 CI 开发规范 |
| 2026-05-15 | **索引口径对齐**：docs/21 与 docs/07/17 的表名（app_user）、API 路径、接口方法（POST export）、MaskGeometry 命名统一；建设方案.md docs/07 描述更新；docs/11 补 migration downgrade 要求 |
| 2026-05-15 | **阶段计划对齐**：progress.md 阶段编号与 docs/19 统一；新增 docs/21 阶段 PRD 与开发阅读索引（M0-M6 目标/范围/不做/必读/验收）；开发前检查清单按 M0-M6 重写；建设方案.md 和 AGENTS.md 索引追加 docs/21 |
| 2026-05-15 | DEM 数据已就位（data/DEM_0P05_CHINA.nc），更新 docs/01 状态和 data/生产环境数据结构.md |
| 2026-05-15 | **前端 UI 交互细则与技术栈冻结**：docs/12 新增 §12.1.2 技术栈冻结（Vite/Pinia/Vue Router/axios/ECharts/Vitest/Playwright/ESLint+Prettier）；editorStore 补充 qpfBefore/ptypeBefore/touchedMask/changedMask/invalidMask + loading/error 状态 + selectedViewMode；新增 §12.11 含布局尺寸、查看模式图层规则、按钮启禁用规则、新增降水相态弹窗流程、选区工具交互细节、页面线框图、测试方案；docs/21 M2 验收增加布局/模式/按钮/选区交互项，M3 验收增加相态弹窗/统计面板/操作历史项 |
| 2026-05-15 | **设计文档全量审核定稿**：修复 DDL/Schema/枚举/API 共 10 类问题（v000 零场派生、edit_operation 补字段、review_product nullable+队列字段、brush→brush_path 统一、target_ptype 校验规则、changed_mask 浮点容差、Grid Binary API 完善、MASK_EMPTY 错误码、partial 窗口可编辑默认值、绘图 worker 认领与恢复策略），涉及 docs/06/07/11/12/13/14/15/16/17/19 + schemas/enums.json + schemas/error_codes.json + schemas/product_config.json，全部 20 篇设计文档达到可编码状态 |
| 2026-05-16 | **效果图地理范围偏差标注**：F01-F06 效果图仅展示陕西，实际业务为西部 7 省（新疆/西藏/青海/四川/甘肃/宁夏/重庆，70-111°E, 25-50°N）；docs/22 底图规范补充 7 省省界和 7 省会标注；docs/23 新增 §23.4 已知效果图偏差表 |
| 2026-05-16 | **效果图 vs 规格一致性修复**：统一右侧栏 340px（docs/12/22/21/tokens.json）；合并二级上下文栏到顶部导航单行；统一系统全称「降水相态网格编辑系统」；修正雪色描述为灰色系；F05 审核页交互改为缩略图并排；F02 标注网格分辨率控件为误标；F06 补充 2×2 网格布局规格；删除 Mesh-Editor-frontend-uiux-pack 原始包 |
| 2026-05-16 | **前端 UI/UX 规格补充包集成**：新增 docs/22-26（视觉 token、18 张效果图页面规格、组件状态、Mock 数据、验收走查）；新增 schemas/frontend_ui_tokens.json + frontend_effect_screens.json；新增 checklists/前端UIUX开发检查清单.md；已生成 6 张效果图资产 F01-F06 置入 docs/assets/frontend/effects/；更新建设方案.md、docs/12、docs/21、AGENTS.md 索引 |
| 2026-05-15 | 初始文档 01-09 完成；设计补全包 10-20 合并；建设方案升级 V2.0 |
