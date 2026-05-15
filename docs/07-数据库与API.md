## 16. 数据库设计

### 16.1 forecast_case

```sql
CREATE TABLE forecast_case (
    case_id TEXT PRIMARY KEY,
    init_time TEXT NOT NULL,
    region_id TEXT NOT NULL,
    grid_id TEXT NOT NULL,
    created_at TEXT NOT NULL
);
```

### 16.2 product_window

```sql
CREATE TABLE product_window (
    window_id TEXT PRIMARY KEY,
    case_id TEXT NOT NULL,
    accum_hours INTEGER NOT NULL,
    start_lead INTEGER NOT NULL,
    end_lead INTEGER NOT NULL,
    qpf_before_path TEXT NOT NULL,
    ptype_before_path TEXT NOT NULL,
    created_at TEXT NOT NULL
);
```

### 16.3 edit_version

```sql
CREATE TABLE edit_version (
    version_id TEXT PRIMARY KEY,
    window_id TEXT NOT NULL,
    version_no INTEGER NOT NULL,
    base_version_id TEXT,
    session_id TEXT,
    qpf_after_path TEXT NOT NULL,
    ptype_after_path TEXT NOT NULL,
    delta_qpf_path TEXT NOT NULL,
    change_ptype_path TEXT NOT NULL,
    edit_mask_path TEXT NOT NULL,
    before_image_path TEXT,
    after_image_path TEXT,
    delta_qpf_image_path TEXT,
    change_ptype_image_path TEXT,
    edit_mask_image_path TEXT,
    review_image_path TEXT,
    created_by TEXT,
    created_at TEXT NOT NULL
);
```

| 字段 | 说明 |
|---|---|
| base_version_id | 本版本基于哪个版本订正而来；`v000_original` 表示首次编辑，形成版本链 `v000→v001→v002→...` |
| session_id | 产生本版本的编辑会话；session 归档后版本仍可独立追溯来源 |

### 16.4 edit_session

每次用户打开一个产品窗口进入编辑，后端创建一个 session。草稿期间所有操作绑定 session_id 而非 version_id。保存版本时 session 关联到新生成的 version_id。

```sql
CREATE TABLE edit_session (
    session_id TEXT PRIMARY KEY,
    window_id TEXT NOT NULL,
    base_version_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'editing',
    started_at TEXT NOT NULL,
    saved_version_id TEXT,
    ended_at TEXT
);
```

| 字段 | 说明 |
|---|---|
| session_id | 唯一标识，如 `sess_20260101_001` |
| base_version_id | 进入编辑时的基线版本（首次编辑为 `v000_original`） |
| status | `editing`：草稿编辑中；`saved`：已保存为正式版本；`discarded`：用户放弃草稿 |
| saved_version_id | 保存后回填的正式版本 ID，`editing` / `discarded` 时为 NULL |

生命周期：

```text
打开窗口 → 创建 session (status=editing, base_version_id=当前最新版本)
    ↓
操作/撤销/重做 → edit_operation 绑定 session_id
    ↓
保存版本 → 生成 version_id, session.saved_version_id = version_id, status=saved
    ↓
或 放弃 → status=discarded, ended_at=now
```

### 16.5 edit_operation

```sql
CREATE TABLE edit_operation (
    operation_id TEXT PRIMARY KEY,
    session_id TEXT NOT NULL,
    window_id TEXT NOT NULL,
    sequence_no INTEGER NOT NULL,
    tool_name TEXT NOT NULL,
    variable_name TEXT NOT NULL,
    operation_type TEXT NOT NULL,
    parameters_json TEXT,
    mask_geometry_json TEXT,
    mask_raster_path TEXT,
    before_stats_json TEXT,
    after_stats_json TEXT,
    is_undone INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL
);
```

与原表差异：
- `version_id` → `session_id`：草稿操作不再挂到虚拟 "draft" 版本
- 新增 `sequence_no`：操作在 session 内的顺序号，支撑撤销/重做的栈定位
- 新增 `is_undone`：0=生效，1=已撤销。重做时改回 0。保存版本时只归档 `is_undone=0` 的操作
- `mask_path` → `mask_geometry_json` + `mask_raster_path`：同时保存用户绘制的原始几何和栅格化结果

`mask_geometry_json` 格式按工具类型：

```json
// 多边形
{"type": "polygon", "coordinates": [[lon1, lat1], [lon2, lat2], ...]}

// 绘制笔（带宽度线缓冲区）
{"type": "line_buffer", "width_grid": 2, "coordinates": [[lon1, lat1], [lon2, lat2], ...]}

// 笔刷（轨迹点序列）
{"type": "brush_path", "radius_grid": 3, "points": [[lon1, lat1], [lon2, lat2], ...]}
```

> geometry 复现用户画了什么，raster mask 复现实际影响了哪些格点。两者独立保存，互不依赖。
- 移除 `created_by`：用户信息已在 session 层记录

### 16.6 review_field

```sql
CREATE TABLE review_field (
    field_id TEXT PRIMARY KEY,
    window_id TEXT NOT NULL,
    version_id TEXT,
    source_model TEXT NOT NULL,
    variable_name TEXT NOT NULL,
    level_type TEXT,
    level_value INTEGER,
    lead_hour INTEGER,
    valid_time TEXT,
    unit TEXT,
    file_path TEXT NOT NULL,
    created_at TEXT NOT NULL
);
```

### 16.7 review_product

```sql
CREATE TABLE review_product (
    review_id TEXT PRIMARY KEY,
    window_id TEXT NOT NULL,
    version_id TEXT NOT NULL,
    template_id TEXT NOT NULL,
    image_path TEXT NOT NULL,
    plot_config_path TEXT,
    plot_input_manifest_path TEXT,
    plot_code_version TEXT,
    plot_status TEXT NOT NULL DEFAULT 'pending',
    plot_started_at TEXT,
    plot_finished_at TEXT,
    total_panels INTEGER,
    success_panels INTEGER,
    skipped_panels INTEGER,
    missing_fields_json TEXT,
    error_log_path TEXT,
    created_at TEXT NOT NULL
);
```

---

## 17. 后端 API 设计

### 17.1 查询产品窗口

```http
GET /api/windows?case_id=2026010108&accum_hours=24
```

返回：

```json
{
  "windows": [
    {
      "window_id": "2026010108_ACC24_024_048",
      "case_id": "2026010108",
      "accum_hours": 24,
      "start_lead": 24,
      "end_lead": 48,
      "status": "available"
    }
  ]
}
```

### 17.2 开始编辑（创建 session）

```http
POST /api/session/start
```

请求：

```json
{
  "window_id": "2026010108_ACC24_024_048",
  "user_id": "forecaster_001"
}
```

返回：

```json
{
  "session_id": "sess_20260101_001",
  "window_id": "2026010108_ACC24_024_048",
  "base_version_id": "v002",
  "status": "editing"
}
```

后端逻辑：查找该 window 最新已保存版本作为 base_version_id（首次编辑为 `v000_original`），加载其 qpf/ptype 数组到内存作为编辑起点。

### 17.3 加载窗口数据

```http
GET /api/session/{session_id}/load
```

返回当前 session 的 qpf、ptype 数据摘要和图片路径。若 session 已有未撤销的操作，返回的是操作叠加后的状态。

### 17.4 编辑预览

```http
POST /api/edit/preview
```

预览**不改变**服务端草稿状态，只计算并缓存结果，返回 `preview_id` 供后续 apply 引用。

请求：

```json
{
  "session_id": "sess_20260101_001",
  "tool": "polygon",
  "variable": "qpf",
  "operation": "increase",
  "mask": {
    "type": "polygon",
    "coordinates": []
  },
  "parameters": {
    "delta_mm": 3.0,
    "only_nonzero": true
  }
}
```

返回：

```json
{
  "preview_id": "prev_20260101_001_007",
  "affected_grid_count": 286,
  "affected_area_km2": 7150,
  "area_mode": "approximate_25km2",
  "before_stats": {
    "min": 0.2,
    "max": 8.5,
    "mean": 3.1,
    "sum": 886.6
  },
  "after_stats": {
    "min": 3.2,
    "max": 11.5,
    "mean": 6.1,
    "sum": 1744.6
  },
  "preview_image": "images/preview.png"
}
```

后端逻辑：
1. 基于 session 当前草稿数组和请求中的 mask/parameters 计算结果数组和统计量
2. 将计算结果（含已栅格化的 mask、参数快照、结果数组）缓存到 `preview_id` 下
3. 不修改 session 的草稿数组
4. 同一 session 新的 preview 请求会覆盖上一次未 apply 的缓存

Preview 缓存策略：

| 阶段 | 存储方式 | 路径 |
|---|---|---|
| 原型 | 内存字典 + 临时目录 | `tmp/previews/{session_id}/{preview_id}.npz` |
| 生产 | 可升级为 Redis 元信息 + 临时 npz | 同上，元信息迁入 Redis |

清理规则：
- TTL = 10 分钟，超时自动清理
- apply 成功后立即删除对应 preview 缓存
- session 结束（saved / discarded）时清理该 session 下所有残留 preview
- preview 只作为短期计算缓存，不进入正式版本或复盘包

### 17.5 应用编辑

```http
POST /api/edit/apply
```

请求：

```json
{
  "session_id": "sess_20260101_001",
  "preview_id": "prev_20260101_001_007"
}
```

返回：

```json
{
  "operation_id": "op_000023",
  "sequence_no": 3,
  "applied": true
}
```

后端逻辑：
1. 通过 `preview_id` 取回缓存的 mask、参数、结果数组——**不重新计算**，确保与预览完全一致
2. 将结果写入 session 草稿数组
3. 记录一条 edit_operation（session_id、sequence_no、mask_geometry_json 保存原始几何、mask_raster_path 指向持久化的栅格 mask）
4. 清除该 preview 缓存

约束：
- `preview_id` 必须属于同一 `session_id`，否则返回 400
- `preview_id` 已被 apply 或已过期（被新 preview 覆盖）时返回 409 Conflict
- 前端可多次 preview 但只能 apply 最近一次有效的 preview

### 17.6 撤销 / 重做

```http
POST /api/edit/undo
POST /api/edit/redo
```

请求：

```json
{
  "session_id": "sess_20260101_001"
}
```

撤销：将当前 session 最后一条 `is_undone=0` 的操作标记为 `is_undone=1`，重算数组状态。
重做：将当前 session 最后一条 `is_undone=1` 的操作标记为 `is_undone=0`，重算数组状态。

### 17.7 保存版本

```http
POST /api/version/save
```

请求：

```json
{
  "session_id": "sess_20260101_001",
  "generate_review": true
}
```

返回：

```json
{
  "session_id": "sess_20260101_001",
  "version_id": "v003",
  "before_image": "images/before_product.png",
  "after_image": "images/after_product.png",
  "review_image": "images/review_composite.png"
}
```

后端逻辑：
1. 生成新 version_id，持久化当前数组为 qpf_after / ptype_after / delta / mask 文件
2. 写入 edit_version 记录：`base_version_id = session.base_version_id`，`session_id = session.session_id`
3. 归档 session 中 `is_undone=0` 的操作到该版本
4. 更新 session：`saved_version_id = v003`，`status = saved`，`ended_at = now`

### 17.7.1 放弃草稿

```http
POST /api/session/discard
```

请求：

```json
{
  "session_id": "sess_20260101_001"
}
```

后端逻辑：`status = discarded`，`ended_at = now`。操作记录保留（供审计），但不生成版本。

### 17.8 生成复盘图

```http
POST /api/review/generate
```

请求：

```json
{
  "window_id": "2026010108_ACC24_024_048",
  "version_id": "v003",
  "template_id": "snow_phase_review_v1"
}
```

返回（异步任务提交后轮询）：

```json
{
  "review_id": "rev_20260101_003",
  "plot_status": "partial_success",
  "total_panels": 8,
  "success_panels": 7,
  "skipped_panels": 1,
  "missing_fields": [
    {
      "name": "rh",
      "level_type": "pressure",
      "level_value": 700,
      "lead_hour": 72,
      "reason": "file_not_found"
    }
  ],
  "image_path": "images/review_composite.png",
  "error_log": null
}
```

`plot_status` 取值：`pending`（排队中）、`running`（执行中）、`success`（全部面板成功）、`partial_success`（部分面板因数据缺失跳过）、`failed`（绘图代码异常）。

---

