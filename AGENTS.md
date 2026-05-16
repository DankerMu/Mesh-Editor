# Mesh-Editor 项目指令

## 冷启动

所有 agent 进入本仓库后，首先读取 `progress.md` 获取项目当前状态，不要盲目扫描全部文档。

## 任务完成后

完成任何实质性任务后，必须更新 `progress.md`：
- 勾选已完成项、添加新待办
- 更新"当前阶段"描述
- 追加"最近变更记录"（日期 + 一句话）
- 维护阻塞项
- 保持总行数不超过 200 行，必要时压缩旧记录

## 仓库结构速查

```
progress.md          ← 项目进度（必读）
建设方案.md          ← V2.0 主文档入口
docs/01-09           ← 业务方案
docs/10-20           ← 工程设计
checklists/          ← 开发检查清单
schemas/             ← 错误码、枚举、产品配置 JSON（前后端共享唯一真相源）
data/                ← 测试数据
reference/           ← 参考项目（只读）
```

## 文档导航

- 项目进度与上下文：`progress.md`
- 业务方案入口：`建设方案.md` → `docs/01-09`
- 工程设计入口：`docs/10-开发总蓝图.md` → `docs/11-19`
- 里程碑与任务拆解：`docs/19-开发任务拆解与里程碑.md`
- 阶段 PRD 与开发入口：`docs/21-阶段PRD与开发阅读索引.md`
- 开发前检查清单：`checklists/开发前检查清单.md`
- 错误码定义：`schemas/error_codes.json`

## 核心约束

- 投影：EPSG:4326，全链路一致
- 网格：501×821，0.05°，70-111°E / 25-50°N
- 编辑范围：仅 qpf + ptype，IFS 变量不进入编辑
- 角色：admin / reviewer / forecaster / viewer
- 功能对齐：每个功能必须同时覆盖领域对象、后端、前端、文件IO、状态机、API 六层
- 绘图：三层架构（原始脚本只读 → plotter 纯函数 → 任务服务）
- 中文产品：全界面中文，不做国际化，API message 中文，TDesign zh-CN，时区北京时间

## 开发规范

- Python 虚拟环境和依赖管理统一使用 uv（pyproject.toml + uv.lock），禁止 pip install / requirements.txt / poetry
- CI pipeline 结构在 M0 建立，后续阶段只增加测试文件不改 pipeline 配置；每阶段合入前 CI 全部 job 必须通过
- 中文响应，技术术语保持英文
- 编辑系统核心对象：ProductWindow / EditSession / EditVersion / EditOperation / PlotTask
- 状态机定义在 `docs/16`，API Schema 在 `docs/17`
- 参考项目在 `reference/` 目录下，绘图脚本不可直接调用，必须经过三层架构封装
