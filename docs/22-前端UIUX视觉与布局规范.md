# 22. 前端 UI/UX 视觉与布局规范

## 22.1 文档定位

本文用于约束前端实现的视觉风格、布局尺寸、颜色、状态标签、地图图层、卡片、表格、按钮和中文化行为，目标是让实现效果与 `docs/assets/frontend/effects/` 下的效果图保持一致。

本文件与现有文档关系：

```text
docs/12：前端工程结构、路由、Store、组件
docs/17：API Schema、字段二进制传输
docs/21：阶段 PRD 与开发入口
docs/22：视觉和布局规范
docs/23：18 张效果图页面规格
docs/24：交互和组件状态
docs/25：Mock 与联调
docs/26：验收和走查
```

---

## 22.2 前端技术栈冻结

| 类别 | 技术 | 说明 |
|---|---|---|
| 构建工具 | Vite | 统一开发与构建入口 |
| 框架 | Vue 3 + TypeScript | 使用 Composition API |
| 路由 | Vue Router 4 | 路由守卫校验角色权限 |
| 状态管理 | Pinia | `authStore/windowStore/editorStore/reviewStore/...` |
| UI 组件 | TDesign Vue Next | 中文 locale：zh-CN |
| 地图 | OpenLayers 10.x | EPSG:4326，不使用 global wrapX |
| HTTP | axios | 统一封装 `api/http.ts`，自动注入 JWT 与 trace_id |
| 图表 | ECharts | 历史分析、统计看板、任务趋势 |
| 单元测试 | Vitest + Vue Test Utils | Store、组件、工具函数 |
| E2E | Playwright | 关键流程端到端 |
| 代码规范 | ESLint + Prettier + TypeScript strict | CI 强制检查 |

---

## 22.3 中文化与时间规范

```text
- 全界面中文，不做国际化。
- TDesign locale 固定为 zh-CN。
- API message 使用中文，但错误码仍用英文 code。
- 所有时间显示为北京时间 UTC+8。
- 页面显示格式：YYYY-MM-DD HH:mm。
- 起报时间显示：YYYY-MM-DD HH时。
- 时效显示：+024h ~ +048h。
- 数值单位固定显示，如 mm、km²、格点。
- 小数规则：QPF 默认 1 位或 2 位；面积默认整数或 1 位；总量默认 1 位。
```

---

## 22.4 视觉 Token

机器可读版本见 `schemas/frontend_ui_tokens.json`。以下为人工说明。

### 22.4.1 品牌色

| Token | 色值 | 用途 |
|---|---|---|
| `color-primary` | `#1664FF` | 主按钮、选中态、导航高亮 |
| `color-primary-hover` | `#4080FF` | hover |
| `color-primary-bg` | `#E8F1FF` | 选中背景、浅蓝卡片 |
| `color-link` | `#1664FF` | 链接 |

### 22.4.2 状态色

| 状态 | 色值 | 背景 | 用途 |
|---|---|---|---|
| success / available / qc_pass | `#00A870` | `#E8F8F2` | 可用、成功 |
| warning / partial / qc_warn | `#ED7B2F` | `#FFF3E8` | 部分缺失、警告 |
| danger / invalid / failed | `#E34D59` | `#FDECEE` | 异常、失败 |
| info / submitted / running | `#1664FF` | `#E8F1FF` | 提交、运行中 |
| neutral / archived / disabled | `#86909C` | `#F2F3F5` | 归档、禁用 |

### 22.4.3 文本色

| Token | 色值 | 用途 |
|---|---|---|
| `text-primary` | `#1D2129` | 标题、主文本 |
| `text-secondary` | `#4E5969` | 次级文本 |
| `text-placeholder` | `#86909C` | 占位、说明 |
| `text-disabled` | `#C9CDD4` | 禁用 |

### 22.4.4 背景、边框、阴影

| Token | 色值/值 | 用途 |
|---|---|---|
| `page-bg` | `#F5F7FA` | 页面背景 |
| `card-bg` | `#FFFFFF` | 卡片背景 |
| `border-color` | `#E5E6EB` | 分割线、边框 |
| `shadow-card` | `0 4px 16px rgba(29,33,41,0.06)` | 卡片阴影 |
| `shadow-popover` | `0 8px 24px rgba(29,33,41,0.12)` | 浮层 |
| `radius-card` | `8px` | 卡片圆角 |
| `radius-control` | `6px` | 按钮、输入框 |

### 22.4.5 字号与行高

| Token | 字号 | 行高 | 用途 |
|---|---:|---:|---|
| `font-title-lg` | 22px | 30px | 系统标题 |
| `font-title` | 18px | 26px | 页面标题、卡片标题 |
| `font-body` | 14px | 22px | 常规文本 |
| `font-caption` | 12px | 18px | 辅助文本、状态栏 |
| `font-number-lg` | 24px | 32px | 统计数字 |

---

## 22.5 布局规范

### 22.5.1 AppShell

| 区域 | 尺寸 | 说明 |
|---|---:|---|
| 顶部导航栏 | 56px | Logo、系统名「降水相态网格编辑系统」、一级菜单、上下文控件（起报时间/累计长度/窗口/版本）、用户区。**单行结构，不拆分二级上下文栏** |
| 左侧栏 | 260px | 查看内容、筛选、目录树，可折叠 |
| 右侧栏 | 340px | 编辑工具、审核信息、复盘详情，可折叠 |
| 底部状态栏 | 36px | 坐标、格点、Session、连接状态 |
| 主内容边距 | 12px | 卡片之间的基础间距 |

默认桌面画布按 16:9 设计，推荐最小分辨率：`1440 × 900`。效果图以约 `1672 × 941` 展示。

> **系统全称**：降水相态网格编辑系统。顶部导航 Logo 旁统一使用此名称，所有页面一致。

### 22.5.2 网格编辑页布局

```text
┌──────────────────────────────────────────────────────────────┐
│ 顶部主导航：Logo | 一级菜单 | 通知 | 帮助 | 用户             │
├──────────────────────────────────────────────────────────────┤
│ （上下文控件区：起报时间 | 累计长度 | 窗口 | 版本 | 保存 | 提交）│
├──────────────┬───────────────────────────────┬───────────────┤
│ 左侧查看内容 │              地图区             │ 右侧编辑工具   │
│ 图例/状态    │   qpf/ptype 图层 + 选区 + tooltip │ 区域/降水/相态 │
├──────────────┴───────────────────────────────┴───────────────┤
│ 底部状态栏：lon/lat | row/col | qpf | ptype | session       │
└──────────────────────────────────────────────────────────────┘
```

### 22.5.3 表格/列表页布局

用于 F02 数据窗口选择、F05 审核列表、F06 复盘目录：

```text
左侧过滤/目录 260px + 主表格/内容自适应 + 右侧详情 340px
```

表格行高：`48px`；表头高：`44px`；分页区高：`48px`。

---

## 22.6 地图图层视觉规范

### 22.6.1 底图

```text
- 使用固定区域底图/边界图，底图浅色，不能抢占降水色斑主视觉。
- 省界/市界：浅灰线，选中省界可加深。
- 城市标签：深灰，西安市可加粗。
- 地图控件固定在右上或左上，不遮挡选区。
```

### 22.6.2 降水相态色斑

| ptype | 含义 | 视觉 |
|---:|---|---|
| 0 | 无降水 | 透明 |
| 1 | 雨 | 绿色系，随 qpf 增强转深/蓝 |
| 2 | 雪 | 灰色系（浅灰→中灰→深灰→暗灰，随量级加深） |
| 3 | 雨夹雪 | 紫色/粉紫色系 |

色标以 `docs/03` 与 `product_config.json` 为准；前端只能消费配置，禁止硬编码。

### 22.6.3 选区与 Mask

| 类型 | 线色 | 填充 | 节点 |
|---|---|---|---|
| polygon | `#1664FF` | `rgba(22,100,255,0.16)` | 白底蓝边圆点 |
| line_buffer | `#1664FF` | `rgba(22,100,255,0.12)` | 线段端点/控制点 |
| brush_path | `#1664FF` | `rgba(22,100,255,0.18)` | 鼠标圆形 brush 边界 |
| preview overlay | `#7B61FF` | `rgba(123,97,255,0.22)` | 虚线边界 |
| touched_mask | `#7B61FF` | `rgba(123,97,255,0.14)` | 紫色虚线 |
| changed_mask | `#E34D59` | `rgba(227,77,89,0.14)` | 红色虚线 |
| invalid_mask | `#86909C` | 斜线纹理 | 不可编辑提示 |

### 22.6.4 Tooltip

格点 Tooltip 固定字段：

```text
经纬度：108.95E, 34.27N
行列号：185, 322
qpf_before：10.2 mm
qpf_after：12.4 mm
ptype_before：雨
ptype_after：雨
```

Tooltip 使用白底卡片、轻阴影、圆角 6px；在地图边缘自动避让。

---

## 22.7 通用组件规范

### 22.7.1 状态 Tag

| 状态 | 文案 | 样式 |
|---|---|---|
| available | available / 可编辑 | 绿色 |
| partial | partial / 部分缺失 | 橙色 |
| invalid | invalid / 异常不可用 | 红色 |
| archived | archived / 已归档 | 灰色 |
| draft | draft / 草稿 | 蓝色浅底 |
| submitted | submitted / 已提交 | 蓝色 |
| approved | approved / 已通过 | 绿色 |
| rejected | rejected / 已退回 | 红色 |
| success | success | 绿色 |
| partial_success | partial_success | 橙色 |
| failed | failed | 红色 |

### 22.7.2 卡片

```text
- 卡片标题左对齐，字号 16/18px。
- 卡片内边距 16px。
- 卡片之间垂直间距 12px。
- 信息卡采用 label/value 双列，label 灰色，value 主色。
```

### 22.7.3 表格

```text
- 表头背景 #FAFAFA。
- hover 行背景 #F5F8FF。
- 选中行背景 #E8F1FF，左侧可加蓝色竖线或单选圆点。
- 操作列按钮右对齐。
- 状态类字段使用 Tag，不直接用纯文本。
```

### 22.7.4 按钮

| 类型 | 用途 |
|---|---|
| Primary | 查询、进入编辑、预览、应用预览、审核通过、导出复盘包 |
| Outline | 保存草稿、查看详情、保存审核意见、重新生成复盘图 |
| Ghost/Text | 查看日志、查看 manifest、查看更多 |
| Danger | 退回修改、删除/禁用类操作 |

---

## 22.8 视觉一致性规则

1. 所有页面顶部主导航保持一致，不能为每个页面单独设计不同 header。
2. 五个一级菜单顺序固定：网格编辑 / 版本审核 / 复盘中心 / 历史分析 / 系统管理。
3. 所有状态使用统一 Tag，不允许同一状态多种颜色。
4. 地图图层、表格、卡片、按钮必须使用同一套 token。
5. 业务 ID 格式必须保持一致：`CASE_2026011508`、`ACC24_024_048`、`V003`、`RVW_20260115_003`。
6. 中文文案使用业务口径：窗口、累计时段、起止预报时效、相态、复盘、审核、发布。
7. 页面中英文混排仅限技术字段：`qpf_before`、`ptype_after`、`review_id` 等。
