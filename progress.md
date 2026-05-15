# Mesh-Editor 项目进度

> 本文件是每次 AI session 的冷启动上下文。完成任务后必须更新本文件。最多 200 行。

## 项目概述

固定区域（70-111°E, 25-50°N）5km 等经纬度网格（501×821）累计降水与雨雪相态编辑及复盘系统。
技术栈：Vue 3 + OpenLayers + TDesign / Python FastAPI / Matplotlib + Cartopy / Docker。

## 仓库结构

```
建设方案.md          ← V2.0 主文档，文档索引入口
docs/01-09           ← 业务方案与功能设计
docs/10-20           ← 工程开发设计（2026-05-15 合并自 dev-design-pack）
checklists/          ← 开发前检查清单
schemas/             ← error_codes.json
data/                ← 测试数据（EC预报/phase/量级，大文件已 gitignore）
reference/           ← 参考项目（yy-webgis-bj 前端、cdsz 后端、要素场绘图脚本）
Mesh-Editor-dev-design-pack/  ← 设计补全包原始文件（已合并到 docs/10-20）
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

**阶段：设计文档完成，尚未进入代码开发**

### 已完成

- [x] 业务方案文档 01-09 编写
- [x] 工程设计文档 10-20 编写
- [x] 设计补全包合并到主仓库（2026-05-15）
- [x] 现有文档对齐修改（版本号、架构、角色、里程碑）
- [x] 创建 progress.md / CLAUDE.md / AGENTS.md

### 待开始

- [ ] M0：工程骨架搭建（前后端项目初始化、CI、Docker 基础）
- [ ] M1：配置与权限基础
- [ ] M2：数据接入与窗口生成
- [ ] M3：编辑工作台与编辑引擎
- [ ] M4：版本审核发布
- [ ] M5：复盘中心与绘图任务队列
- [ ] M6：历史统计、监控运维、试运行

### 阻塞项

- 无（文档阶段无阻塞）

## 最近变更记录

| 日期 | 变更 |
|---|---|
| 2026-05-15 | docs/11 §11.10.1 补充数据库索引（含 partial index）和 §11.10.2 迁移策略；v000_original 定义为真实版本（docs/07/16/enums）；docs/09 §25.3 扩展为成品级性能目标 + §25.4 运维验收 |
| 2026-05-15 | 新增 schemas/enums.json（16 类枚举），docs/10 更新共享枚举规则为生成而非手抄 |
| 2026-05-15 | docs/13 §13.4/§13.5 补充 qpf_build_manifest.json 和 ptype_source_manifest.json（含 schema），归档目录同步 |
| 2026-05-15 | error_codes.json 与 docs/16 §16.10 完全同步（42 码），补齐 PREVIEW_CONFLICT/FIELD_NOT_AVAILABLE/NEW_PRECIP_NEEDS_PTYPE/CONSISTENCY_VIOLATION |
| 2026-05-15 | docs/13 §13.8.1 定义发布产物契约 product_manifest.json（fields/derived/images/review + txt 输出），更新 docs/07/11 |
| 2026-05-15 | 并发策略冻结为 single_editing_session_per_window，docs/11 §11.10 和 docs/16 §16.8 删除 multi_session 推荐/可选 |
| 2026-05-15 | docs/13 §13.5 PtypeBuilder 中间 tp QC 规则（缺失/负值/NaN），PtypeBuildResult 增加诊断字段；docs/03 §8.2.3 同步 |
| 2026-05-15 | 新增降水落区 ptype 强制规则（三层防护：preview→apply→save），docs/14 §14.8.1，新增错误码 NEW_PRECIP_NEEDS_PTYPE / CONSISTENCY_VIOLATION |
| 2026-05-15 | ptype_transition 拆分为 op_ptype_transition（操作级）+ version_ptype_transition（版本级），docs/14 §14.9.2 重写 |
| 2026-05-15 | edit_mask 拆分为 touched_mask + changed_mask（12 个文件全量重命名，docs/14 §14.11.1 补充计算规则）|
| 2026-05-15 | docs/17 §17.8.1 新增 Grid Field Binary API（逐字段 flat binary 传输协议）；更新 docs/07 session/load 描述 |
| 2026-05-15 | 修复 progress.md ptype 合成规则描述（对齐 docs/03 §8.2.3）|
| 2026-05-15 | docs/03 §7.1.1 补充 start_lead 枚举规则（统一 24h 步进滑动窗口，23 窗口/case）；新增 schemas/product_config.json |
| 2026-05-15 | 初始文档 01-09 完成；设计补全包 10-20 合并；建设方案升级 V2.0 |
