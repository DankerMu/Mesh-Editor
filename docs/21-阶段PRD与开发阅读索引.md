# 21. 阶段 PRD 与开发阅读索引

## 21.1 使用方式

每个阶段开发前，开发人员必须：

1. 阅读本阶段 PRD 了解业务目标和范围边界
2. 按"必读文档"列表阅读技术规格
3. 按"开发前检查"确认前置条件
4. 以"验收标准"作为交付目标

阶段编号以 `docs/19-开发任务拆解与里程碑.md` 为唯一标准。

---

## 21.2 阶段总览表

| 阶段 | 主要目标 | 必读文档 | 辅助文档 |
|---|---|---|---|
| M0 工程骨架与基础设施 | 建立项目骨架、配置、权限、迁移、错误码 | `docs/10`、`docs/11`、`docs/16`、`docs/17` | `schemas/enums.json`、`schemas/error_codes.json`、`progress.md` |
| M1 数据接入与窗口生成 | 扫描数据、生成窗口、构建 qpf/ptype、QC | `docs/03`、`docs/13`、`docs/11`、`docs/18` | `schemas/product_config.json` |
| M2 地图与编辑工作台 | 地图显示、窗口选择、字段加载、绘制选区 | `docs/12`、`docs/17`、`docs/04`、`docs/10` | `docs/13`、`schemas/enums.json` |
| M3 编辑引擎与操作留痕 | preview/apply、EditOps、undo/redo、mask、统计 | `docs/14`、`docs/16`、`docs/17`、`docs/18` | `docs/05`、`docs/10` |
| M4 版本保存、审核与发布 | 保存版本、提交审核、发布、发布产物 | `docs/07`、`docs/13`、`docs/16`、`docs/17` | `docs/06`、`docs/11` |
| M5 复盘中心与绘图任务队列 | review_payload、plotter、任务队列、复盘包 | `docs/15`、`docs/08`、`docs/17`、`docs/18` | `docs/06`、参考绘图脚本 |
| M6 运维、统计、配置与试运行 | 用户、审计、配置、监控、统计、E2E | `docs/07`、`docs/16`、`docs/17`、`docs/18`、`docs/19` | `checklists/开发前检查清单.md` |

---

## 21.3 M0 PRD：工程骨架与基础设施

### 1. 阶段目标

建立可运行的前后端工程骨架，所有后续模块的基础设施（配置加载、权限认证、错误码、数据库迁移、路径管理）在本阶段全部就位。

### 2. 用户角色

admin（管理初始用户）、forecaster/reviewer/viewer（登录验证）

### 3. 业务入口

- 前端：登录页 → 空白工作台
- 后端：`/api/health`、`/api/auth/login`

### 4. 本阶段范围

- 后端：FastAPI 可启动、统一响应结构、trace_id、错误码加载、enums 加载、Alembic migration、PathBuilder、基础配置加载、JWT 登录与权限中间件
- 前端：Vue 可启动、登录页、路由守卫、authStore、http.ts 统一响应处理、403/500 错误页
- 部署：docker-compose dev、backend/frontend 容器、data/archive/tmp/db 卷挂载
- CI pipeline：lint（ruff + eslint）、type check（mypy + vue-tsc）、pytest、vitest、alembic upgrade→downgrade→upgrade、docker build smoke test

### 5. 本阶段不做

- 数据扫描和窗口生成（M1）
- 地图显示和编辑工作台（M2）
- 业务编辑操作（M3）
- 绘图服务（M5）

### 6. 用户故事

- 作为运维人员，我希望通过 docker-compose up 一键启动开发环境
- 作为预报员，我希望通过用户名密码登录系统并进入工作台
- 作为管理员，我希望系统在启动时自动加载配置和错误码

### 7. 关键接口

| 接口 | 说明 |
|---|---|
| `GET /api/health` | 健康检查 |
| `POST /api/auth/login` | JWT 登录 |
| `GET /api/auth/me` | 当前用户信息 |

### 8. 关键数据对象

- `app_user` 表（id, username, role, is_active, created_at）
- `schemas/error_codes.json`
- `schemas/enums.json`
- `schemas/product_config.json`

### 9. 必读文档

- `docs/10-开发总蓝图.md` — 领域模型和对象关系
- `docs/11-后端工程设计.md` — 目录结构、服务分层
- `docs/16-状态机与错误码设计.md` — 错误码定义
- `docs/17-API契约与Schema设计.md` — 统一响应格式、权限模型

### 10. 验收标准

- [ ] `/api/health` 返回 200
- [ ] `/api/auth/login` 正确用户名密码返回 JWT token
- [ ] 前端可登录并进入空白工作台
- [ ] 错误密码返回统一错误格式（含 error_code）
- [ ] 数据库 migration 可执行且可回滚
- [ ] docker-compose up 一次成功
- [ ] CI pipeline 全部 job 通过（lint + type check + test + migration + docker build）

---

## 21.4 M1 PRD：数据接入与窗口生成

### 1. 阶段目标

实现数据扫描、qpf 差分计算、ptype 窗口合成，生成可编辑的 ProductWindow 列表并展示 QC 信息。

### 2. 用户角色

forecaster（查看窗口列表和 QC 状态）、admin（触发数据扫描）

### 3. 业务入口

- 前端：窗口选择器（WindowSelector 组件）
- 后端：`/api/data/scan`、`/api/windows`

### 4. 本阶段范围

- `read_txt_grid` 文件读取
- QpfBuilder：tp 差分、000 fallback、negative/missing mask
- PtypeBuilder：窗口相态合成（has_rain/has_snow 逐格点）
- DataScanService：扫描数据目录、生成窗口
- product_window / data_scan_log 入库
- 前端 WindowSelector 和数据状态展示

### 5. 本阶段不做

- 地图显示（M2）
- 编辑操作（M3）
- qpf/ptype 数组的人工修改

### 6. 用户故事

- 作为预报员，我希望看到当前可用的编辑窗口列表及其数据完整度
- 作为预报员，我希望 partial 窗口被清晰标记，以便决定是否进入编辑
- 作为管理员，我希望触发数据扫描后看到扫描结果

### 7. 关键接口

| 接口 | 说明 |
|---|---|
| `POST /api/data/scan` | 触发数据扫描 |
| `GET /api/data/status` | 数据扫描状态 |
| `GET /api/windows` | 可用窗口列表 |

### 8. 关键数据对象

- `product_window` 表
- `data_scan_log` 表
- `qpf_build_manifest` / `ptype_source_manifest`

### 9. 必读文档

- `docs/03-数据与编码.md` — 窗口设计、数据对象、相态编码
- `docs/13-数据适配与文件IO设计.md` — 文件读取、qpf 差分、ptype 合成
- `docs/11-后端工程设计.md` — 服务边界、Repository
- `docs/18-测试用例与验收数据设计.md` — 测试数据样例

### 10. 验收标准

- [ ] normal_case 正确生成 available 窗口
- [ ] missing_ptype_partial 生成 partial 窗口并标记
- [ ] negative_qpf 记录 negative_count
- [ ] 前端 WindowSelector 显示窗口列表、状态和 QC 信息
- [ ] 扫描日志入库可查
- [ ] CI 通过：数据接入 fixture 测试全绿

---

## 21.5 M2 PRD：地图与编辑工作台

### 1. 阶段目标

在地图上加载和显示 qpf/ptype 网格数据，实现选区绘制工具，完成编辑工作台的 UI 框架。**本阶段不实现真实编辑计算。**

### 2. 用户角色

forecaster（加载窗口、查看数据、绘制选区）

### 3. 业务入口

- 前端：编辑工作台页面 → BaseMap → PrecipPhaseGridLayer → DrawTools

### 4. 本阶段范围

- BaseMap 和固定底图（EPSG:4326）
- PrecipPhaseGridLayer 色斑渲染
- GridTooltip 格点拾取（grid_i/grid_j/qpf/ptype）
- DrawTools：polygon / line_buffer / brush_path 选区绘制
- editorStore 基础状态管理
- `/api/session/start`、`/api/session/{id}/load` 接口
- 前端编辑页面布局（工具栏、地图区、信息面板）

### 5. 本阶段不做

- preview / apply / undo / redo（M3）
- 真实修改 qpf/ptype 数组（M3）
- 版本保存和审核（M4）
- 操作入库和操作历史回放（M3）

### 6. 用户故事

- 作为预报员，我希望选择一个窗口后看到 qpf/ptype 组合色斑图
- 作为预报员，我希望鼠标悬停时看到当前格点的坐标和值
- 作为预报员，我希望用多边形/线缓冲/笔刷绘制编辑区域

### 7. 关键接口

| 接口 | 说明 |
|---|---|
| `POST /api/session/start` | 创建编辑会话 |
| `GET /api/session/{session_id}/load` | 加载会话元数据和字段 URL 列表 |
| `GET /api/session/{session_id}/field/{field_name}` | 逐字段 flat binary 下载（qpf/ptype 等） |
| `GET /api/window/{window_id}/field/{field_name}` | 窗口原始字段 binary（qpf_before/ptype_before/invalid_mask） |

### 8. 关键数据对象

- `edit_session` 表
- qpf/ptype numpy 数组（501×821）
- MaskGeometry：polygon / line_buffer / brush_path

### 9. 必读文档

- `docs/12-前端工程设计.md` — 路由、组件、Store、图层
- `docs/17-API契约与Schema设计.md` — session 接口 Schema
- `docs/04-前端设计.md` — 界面布局设计
- `docs/10-开发总蓝图.md` — 领域模型

### 10. 验收标准

- [ ] 选择 window 后地图加载 qpf/ptype 色斑显示正确
- [ ] 鼠标拾取显示 grid_i / grid_j / qpf / ptype 值
- [ ] polygon / line_buffer / brush_path 三种选区均可绘制并生成 geometry
- [ ] 编辑页面布局完整（工具栏、地图区、信息面板）
- [ ] CI 通过：前端组件测试全绿

---

## 21.6 M3 PRD：编辑引擎与操作留痕

### 1. 阶段目标

实现完整的网格编辑能力：preview/apply 闭环、undo/redo、操作入库留痕、降水-相态一致性规则。

### 2. 用户角色

forecaster（执行编辑、预览、应用、撤销）

### 3. 业务入口

- 前端：编辑工具栏（编辑类型选择、参数输入、预览/应用/撤销按钮）
- 前端：PreviewStatsPanel、OperationHistory

### 4. 本阶段范围

- MaskBuilder：geometry → valid_mask（501×821 bool）
- EditOps：qpf set/increase/decrease/multiply/clear
- EditOps：ptype set
- consistency rules：qpf 清零 → ptype=0，ptype 4×4 转换矩阵
- stats_calc：影响格点数、min/max/mean
- preview_cache：预览计算缓存
- `/api/edit/preview`、`/api/edit/apply`
- edit_operation 入库（geometry + raster mask）
- undo/redo 重放
- 前端 PreviewStatsPanel 和 OperationHistory

### 5. 本阶段不做

- 版本保存（M4）
- 审核与发布（M4）
- delta/change/touched/changed 图生成（M4）
- 复盘出图（M5）

### 6. 用户故事

- 作为预报员，我希望在应用编辑前预览影响范围和统计信息
- 作为预报员，我希望清零降水时系统自动同步相态为"无降水"
- 作为预报员，我希望随时撤销/重做编辑操作

### 7. 关键接口

| 接口 | 说明 |
|---|---|
| `POST /api/edit/preview` | 预览编辑效果 |
| `POST /api/edit/apply` | 应用编辑 |
| `POST /api/edit/undo` | 撤销 |
| `POST /api/edit/redo` | 重做 |

### 8. 关键数据对象

- `edit_operation` 表
- valid_mask / touched_mask / changed_mask（numpy bool 数组）
- preview_cache（内存临时）

### 9. 必读文档

- `docs/14-编辑引擎详细设计.md` — mask、EditOps、preview/apply、undo/redo
- `docs/16-状态机与错误码设计.md` — 编辑状态机
- `docs/17-API契约与Schema设计.md` — 编辑接口 Schema
- `docs/18-测试用例与验收数据设计.md` — 编辑测试场景

### 10. 验收标准

- [ ] preview 与 apply 结果一致
- [ ] qpf 清零时 ptype 自动同步为 0
- [ ] ptype 4×4 转换矩阵完整覆盖
- [ ] undo/redo 操作正确（数组还原、操作历史同步）
- [ ] invalid mask 区域不参与编辑
- [ ] edit_operation 入库包含 geometry 和 raster mask
- [ ] CI 通过：编辑引擎单元测试全绿

---

## 21.7 M4 PRD：版本保存、审核与发布

### 1. 阶段目标

实现编辑版本的保存、提交审核、审核通过/退回、正式发布全流程，生成发布产物并归档。

### 2. 用户角色

forecaster（保存版本、提交审核）、reviewer（审核通过/退回）、admin（管理发布）

### 3. 业务入口

- 前端：版本保存按钮 → 提交审核 → ApprovalView 审核页面

### 4. 本阶段范围

- VersionService.save_version
- delta_qpf / change_ptype / touched_mask / changed_mask 生成
- 基础 before/after/delta/change/mask 出图
- 版本保存、提交、审核、发布接口
- ApprovalService 审核状态机
- ReleaseService 发布逻辑 + release_product 归档
- 前端 ApprovalView

### 5. 本阶段不做

- 复盘中心和复盘包（M5）
- IFS 多要素绘图（M5）
- 历史统计分析（M6）
- 模板管理（M6）

### 6. 用户故事

- 作为预报员，我希望保存编辑版本时自动生成 before/after 对比图
- 作为审核员，我希望查看版本的 delta 图和编辑统计后审核通过或退回
- 作为预报员，我希望版本发布后旧发布自动标记为 superseded

### 7. 关键接口

| 接口 | 说明 |
|---|---|
| `POST /api/version/save` | 保存版本 |
| `POST /api/version/submit` | 提交审核 |
| `POST /api/version/review` | 审核通过/退回 |
| `POST /api/version/release` | 正式发布 |

### 8. 关键数据对象

- `edit_version` 表
- `release_product` 表
- delta_qpf / change_ptype / touched_mask / changed_mask 文件
- before/after/delta 图片文件

### 9. 必读文档

- `docs/07-数据库与API.md` — 版本表结构、发布表结构
- `docs/13-数据适配与文件IO设计.md` — 归档路径、发布产物契约
- `docs/16-状态机与错误码设计.md` — 版本状态机、审核状态机
- `docs/17-API契约与Schema设计.md` — 版本接口 Schema

### 10. 验收标准

- [ ] 保存版本生成完整字段和对比图片
- [ ] submit → review → release 状态机转换正确
- [ ] 同一 window 只有一个 active release
- [ ] 旧发布自动标记 superseded
- [ ] 审核退回后可重新编辑并再次提交
- [ ] CI 通过：状态机集成测试全绿

---

## 21.8 M5 PRD：复盘中心与绘图任务队列

### 1. 阶段目标

实现复盘图生成、绘图任务队列管理、复盘包归档和复盘中心查看。

### 2. 用户角色

forecaster/reviewer（查看复盘图）、admin（管理绘图任务）

### 3. 业务入口

- 前端：ReviewCenterView 复盘中心

### 4. 本阶段范围

**M5-A 基础复盘（优先）**：
- plotter 重构：precip_phase（before/after/delta/change/touched/changed 图）
- ReviewService 组装 review_payload
- PlotTaskService 队列、状态、重试
- review_product 入库
- 复盘中心查看、重生成、导出 zip

**M5-B IFS 多要素复盘（次优先）**：
- plotter 重构：review_synoptic（circulation/synoptic/element）
- missing_fields 记录
- partial_success 状态
- 组合图

> M5-B 可视 IFS 数据适配进度独立排期，不阻塞 M5-A 上线。

### 5. 本阶段不做

- 历史统计分析和批量导出（M6）
- 模板管理（M6）
- 用户管理（M6）

### 6. 用户故事

- 作为预报员，我希望版本发布后自动生成复盘图
- 作为审核员，我希望在复盘中心按窗口查看所有版本的复盘图
- 作为管理员，我希望看到绘图任务的执行状态和失败原因

### 7. 关键接口

| 接口 | 说明 |
|---|---|
| `POST /api/review/generate` | 生成复盘任务 |
| `GET /api/tasks/plot/{review_id}` | 绘图任务状态 |
| `GET /api/review/case/{case_id}` | 复盘详情 |
| `GET /api/review/window/{window_id}/versions` | 窗口版本复盘列表 |
| `POST /api/review/export` | 创建导出任务并下载复盘包 zip |

### 8. 关键数据对象

- `review_product` 表
- review_payload（JSON）
- 复盘图片文件

### 9. 必读文档

- `docs/15-绘图任务与复盘服务设计.md` — plotter、绘图队列、复盘包、重试
- `docs/08-技术与绘图.md` — 绘图三层架构、函数签名
- `docs/17-API契约与Schema设计.md` — 复盘接口 Schema
- `docs/18-测试用例与验收数据设计.md` — 绘图测试

### 10. 验收标准

- [ ] 复盘任务 success / partial_success / failed 三种状态可验证
- [ ] missing_fields 被正确记录
- [ ] 复盘图可查看、重生成
- [ ] 复盘包可导出为 zip
- [ ] 绘图任务失败后可自动重试
- [ ] CI 通过：plotter smoke test 全绿

---

## 21.9 M6 PRD：运维、统计、配置与试运行

### 1. 阶段目标

完成运维管理功能（用户、审计、配置、监控），实现历史统计分析和批量导出，完成端到端试运行。

### 2. 用户角色

admin（全部功能）、reviewer/forecaster（查看统计）

### 3. 业务入口

- 前端：用户管理页、审计日志页、配置管理页、监控页、统计页

### 4. 本阶段范围

- 用户管理页面（CRUD、角色分配）
- audit_log 查询
- config 管理与 config_snapshot
- template 管理
- storage monitor（磁盘用量）
- task monitor（任务状态总览）
- 编辑操作统计（按窗口/用户/时间）
- ptype transition 统计
- 批量统计导出 CSV
- 端到端试运行（3 个 case 全流程）

### 5. 本阶段不做

- 多语言国际化（不做）
- 数据备份自动化（运维手册覆盖）
- 性能压测（单独排期）

### 6. 用户故事

- 作为管理员，我希望创建和管理用户账号并分配角色
- 作为管理员，我希望查看系统操作审计日志
- 作为审核员，我希望查看历史编辑统计并导出 CSV
- 作为管理员，我希望在试运行中完成 3 个完整 case 验证系统

### 7. 关键接口

| 接口 | 说明 |
|---|---|
| `GET /api/users` | 用户列表 |
| `POST /api/users` | 创建用户 |
| `PUT /api/users/{user_id}` | 更新用户信息/角色/启用状态 |
| `GET /api/audit/logs` | 审计日志查询 |
| `GET /api/config/{config_type}` | 读取配置 |
| `PUT /api/config/{config_type}` | 更新配置（生成 config_snapshot） |
| `GET /api/monitor/storage` | 磁盘监控 |
| `GET /api/monitor/tasks` | 任务监控 |
| `GET /api/stats/operations` | 操作统计 |
| `GET /api/stats/ptype-transitions` | 相态转换统计 |
| `GET /api/stats/export` | 批量导出 CSV |

### 8. 关键数据对象

- `app_user` 表（创建/更新/禁用，不做物理删除）
- `audit_log` 表
- `config_snapshot` 表
- 统计聚合视图

### 9. 必读文档

- `docs/07-数据库与API.md` — 表结构
- `docs/16-状态机与错误码设计.md` — 审计错误码
- `docs/17-API契约与Schema设计.md` — 管理接口 Schema
- `docs/18-测试用例与验收数据设计.md` — E2E 测试场景
- `docs/19-开发任务拆解与里程碑.md` — M6 任务清单

### 10. 验收标准

- [ ] admin 可创建/更新/禁用用户（通过 is_active 软删除，不做物理删除）
- [ ] 审计日志可按时间/用户/操作类型查询
- [ ] 配置变更生成 config_snapshot
- [ ] 监控页面显示磁盘用量和任务状态
- [ ] 历史统计可按维度筛选并导出 CSV
- [ ] 3 个 case 端到端试运行通过（从数据扫描到复盘导出）
- [ ] CI 通过：E2E 全流程测试全绿

---

## 21.10 阶段交付物矩阵

| 阶段 | 后端交付 | 前端交付 | 数据/部署交付 | 文档交付 |
|---|---|---|---|---|
| M0 | FastAPI 骨架、auth、错误码、PathBuilder、migration | Vue 骨架、登录页、路由守卫、authStore | docker-compose、卷挂载、CI pipeline | API 文档自动生成 |
| M1 | DataScanService、QpfBuilder、PtypeBuilder、窗口接口 | WindowSelector、数据状态面板 | 测试数据 normal/partial/negative | 扫描日志 |
| M2 | session 接口 | BaseMap、GridLayer、GridTooltip、DrawTools、编辑页面 | — | — |
| M3 | MaskBuilder、EditOps、preview/apply、undo/redo | PreviewStatsPanel、OperationHistory | — | — |
| M4 | VersionService、ApprovalService、ReleaseService | ApprovalView | release_product 归档 | — |
| M5 | ReviewService、PlotTaskService、plotter | ReviewCenterView | 复盘包归档 | — |
| M6 | 用户/审计/配置/监控/统计接口 | 管理页面、统计页面 | E2E 试运行报告 | 运维手册 |

---

## 21.11 CI 策略

CI pipeline 结构在 M0 建立一次，后续阶段只增加测试文件，不改 pipeline 配置。

### M0 建立 CI 基础 pipeline

```text
lint         ruff（后端）+ eslint（前端）
type-check   mypy（后端）+ vue-tsc（前端）
test-backend pytest（M0 仅 health/auth 用例）
test-frontend vitest（M0 仅 authStore 基础用例）
migration    alembic upgrade head → downgrade base → upgrade head
docker       docker build smoke test
```

### M1-M6 CI 增量

| 阶段 | 新增测试内容（只加测试文件，不改 pipeline） |
|---|---|
| M1 | 数据接入 fixture 测试（read_txt_grid、QpfBuilder、PtypeBuilder、normal/partial/negative） |
| M2 | 前端组件测试（GridLayer 渲染、DrawTools geometry 输出） |
| M3 | 编辑引擎单元测试（preview/apply 一致性、ptype 4×4 矩阵、undo/redo、mask 边界） |
| M4 | 状态机集成测试（submit→review→release 流转、superseded 逻辑） |
| M5 | plotter smoke test（生成一张 precip_phase 图不报错、review_payload 结构验证） |
| M6 | E2E 全流程测试（3 case 从数据扫描到复盘导出） |

### 阶段验收 CI 要求

每个阶段合入主分支前，CI pipeline 全部 job 必须通过。新增测试不允许降低已有测试覆盖率。

---

## 21.12 阶段依赖关系

```text
M0 ──→ M1 ──→ M2 ──→ M3 ──→ M4 ──→ M5
                                      ↓
                                     M6

并行可能：
- M1 数据接入 与 M2 前端工作台可部分并行（M2 用 mock 数据）
- M3 编辑引擎 与 M2 DrawTools 可部分并行（M3 用 fixture mask）
- M5 plotter 重构可独立于 M4 开发
```
