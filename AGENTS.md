# Mesh-Editor Agent 指令

## 冷启动

所有 agent 进入本仓库后，首先读取 `progress.md` 获取项目当前状态，不要盲目扫描全部文档。

## 任务完成后

完成任务后必须更新 `progress.md`，保持项目状态可追踪。

## 仓库结构速查

```
progress.md          ← 项目进度（必读）
建设方案.md          ← V2.0 主文档入口
docs/01-09           ← 业务方案
docs/10-20           ← 工程设计
checklists/          ← 开发检查清单
schemas/             ← 错误码 JSON
data/                ← 测试数据
reference/           ← 参考项目（只读）
```

## 核心约束

- 投影：EPSG:4326，全链路一致
- 网格：501×821，0.05°，70-111°E / 25-50°N
- 编辑范围：仅 qpf + ptype，IFS 变量不进入编辑
- 角色：admin / reviewer / forecaster / viewer
- 功能对齐：每个功能必须同时覆盖领域对象、后端、前端、文件IO、状态机、API 六层
- 绘图：三层架构（原始脚本只读 → plotter 纯函数 → 任务服务）
