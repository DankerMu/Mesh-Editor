# Mesh-Editor 项目指令

## 冷启动

每次新 session 首先读取 `progress.md`，了解项目当前阶段和上下文，避免重复探索文档。

## 任务完成后

完成任何实质性任务后，必须更新 `progress.md`：
- 勾选已完成项、添加新待办
- 更新"当前阶段"描述
- 追加"最近变更记录"（日期 + 一句话）
- 维护阻塞项
- 保持总行数不超过 200 行，必要时压缩旧记录

## 文档导航

- 项目进度与上下文：`progress.md`
- 业务方案入口：`建设方案.md` → `docs/01-09`
- 工程设计入口：`docs/10-开发总蓝图.md` → `docs/11-19`
- 里程碑与任务拆解：`docs/19-开发任务拆解与里程碑.md`
- 开发前检查清单：`checklists/开发前检查清单.md`
- 错误码定义：`schemas/error_codes.json`

## 开发规范

- 中文响应，技术术语保持英文
- 编辑系统核心对象：ProductWindow / EditSession / EditVersion / EditOperation / PlotTask
- 状态机定义在 `docs/16`，API Schema 在 `docs/17`，每个功能必须同时对齐 6 层（领域对象/后端/前端/文件IO/状态机/API）
- 参考项目在 `reference/` 目录下，绘图脚本不可直接调用，必须经过三层架构封装
