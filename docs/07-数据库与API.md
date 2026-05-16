## 16. 数据库设计

> 本文件定义数据库表和 API 业务语义；具体请求/响应 Schema、统一错误码、权限和审计策略见 [docs/17-API契约与Schema设计](17-API契约与Schema设计.md)。状态机和错误码见 [docs/16-状态机与错误码设计](16-状态机与错误码设计.md)。数据库索引、唯一约束和迁移策略见 [docs/11-后端工程设计](11-后端工程设计.md)。

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
    status TEXT NOT NULL DEFAULT 'pending',
    qc_status TEXT NOT NULL DEFAULT 'unchecked',
    qpf_before_path TEXT,
    ptype_before_path TEXT,
    negative_count INTEGER DEFAULT 0,
    negative_min_value REAL,
    negative_abs_max REAL,
    missing_count INTEGER DEFAULT 0,
    ptype_missing_leads TEXT,
    data_ready_at TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);
```

| 字段 | 说明 |
|---|---|
| status | 窗口状态：`pending` / `available` / `partial` / `invalid` / `archived`，详见 §7.2 |
| qc_status | QC 结果：`unchecked` / `pass` / `warn`（有负值或缺测但可编辑）/ `fail`（不可编辑） |
| qpf_before_path | 差分计算后的 qpf_before 文件路径，`pending`/`invalid` 时为 NULL |
| ptype_before_path | 合成后的 ptype_before 文件路径，`pending`/`invalid` 时为 NULL |
| negative_count | 差分负值格点数，0 表示无异常 |
| negative_min_value | 负值中的最小值（如 -3.6） |
| negative_abs_max | 负值绝对值最大值（如 3.6） |
| missing_count | tp/ptype 文件中 NaN 缺测格点数 |
| ptype_missing_leads | 缺失的 3h ptype 时效 JSON 数组，如 `[33, 36]` |
| data_ready_at | 数据就绪时间（status 从 pending 变为 available/partial 的时刻） |

### 16.2.1 data_scan_log

记录每次数据扫描的执行情况，用于运维审计和问题排查。

```sql
CREATE TABLE data_scan_log (
    scan_id TEXT PRIMARY KEY,
    case_id TEXT NOT NULL,
    scan_type TEXT NOT NULL DEFAULT 'scheduled',
    scan_started_at TEXT NOT NULL,
    scan_finished_at TEXT,
    tp_files_found INTEGER DEFAULT 0,
    ptype_files_found INTEGER DEFAULT 0,
    windows_created INTEGER DEFAULT 0,
    windows_updated INTEGER DEFAULT 0,
    errors_json TEXT,
    status TEXT NOT NULL DEFAULT 'running'
);
```

| 字段 | 说明 |
|---|---|
| scan_type | `scheduled`（定时扫描）/ `manual`（手动触发）/ `startup`（服务启动时） |
| tp_files_found | 本次扫描发现的 tp 文件数量 |
| ptype_files_found | 本次扫描发现的 ptype 文件数量 |
| windows_created | 新创建的窗口数量 |
| windows_updated | 状态变更的窗口数量 |
| errors_json | 扫描中遇到的错误列表，如文件读取失败、格式异常等 |
| status | `running` / `completed` / `failed` |

### 16.3 edit_version

```sql
CREATE TABLE edit_version (
    version_id TEXT PRIMARY KEY,
    window_id TEXT NOT NULL,
    version_no INTEGER NOT NULL,
    base_version_id TEXT,
    session_id TEXT,
    status TEXT NOT NULL DEFAULT 'draft',
    qpf_after_path TEXT NOT NULL,
    ptype_after_path TEXT NOT NULL,
    delta_qpf_path TEXT NOT NULL,
    change_ptype_path TEXT NOT NULL,
    touched_mask_path TEXT NOT NULL,
    changed_mask_path TEXT NOT NULL,
    version_ptype_transition_path TEXT,
    before_image_path TEXT,
    after_image_path TEXT,
    delta_qpf_image_path TEXT,
    change_ptype_image_path TEXT,
    touched_mask_image_path TEXT,
    changed_mask_image_path TEXT,
    review_image_path TEXT,
    created_by TEXT,
    created_at TEXT NOT NULL
);
```

版本状态机：

| 状态 | 含义 | 可编辑 | 可发布 |
|---|---|:---:|:---:|
| `draft` | 刚保存的草稿版本 | 可基于此开新 session | 否 |
| `submitted` | 已提交审核 | 否 | 否 |
| `approved` | 审核通过 | 否 | 是 |
| `rejected` | 审核退回，需修订 | 可基于此开新 session | 否 |
| `released` | 已正式发布 | 否 | — |
| `superseded` | 已被更新版本替代 | 否 | — |

状态转换：

```text
draft ──(提交审核)──→ submitted
submitted ──(审核通过)──→ approved
submitted ──(审核退回)──→ rejected
approved ──(正式发布)──→ released
released ──(新版本发布)──→ superseded
rejected ──(开新 session 修订后保存)──→ 产生新 draft 版本
```

约束：
- 同一 window 同时只能有一个 `released` 版本
- 发布新版本时，旧的 `released` 自动变为 `superseded`
- `rejected` 版本不可直接改为 `approved`，必须新建版本

| 字段 | 说明 |
|---|---|
| base_version_id | 本版本基于哪个版本订正而来，形成版本链 `v000→v001→v002→...` |
| session_id | 产生本版本的编辑会话；session 归档后版本仍可独立追溯来源；v000 的 session_id 为 NULL |

#### v000_original 规则

DataScanService 构建窗口数据后，在 edit_version 表中插入一条真实记录作为原始版本：

```sql
INSERT INTO edit_version (
    version_id, window_id, version_no, base_version_id, session_id, status,
    qpf_after_path, ptype_after_path,
    delta_qpf_path, change_ptype_path, touched_mask_path, changed_mask_path,
    created_by, created_at
) VALUES (
    '{window_id}_v000', window_id, 0, NULL, NULL, 'original',
    'original/qpf_before.npz', 'original/ptype_before.npz',
    'original/v000_delta_qpf.npz', 'original/v000_change_ptype.npz',
    'original/v000_touched_mask.npz', 'original/v000_changed_mask.npz',
    'system', now()
);
```

- `version_no = 0`，`status = 'original'`，`base_version_id = NULL`，`session_id = NULL`
- `qpf_after_path` / `ptype_after_path` 指向 `original/` 目录下的原始数据
- `delta_qpf_path` / `change_ptype_path` / `touched_mask_path` / `changed_mask_path` 指向 `original/` 目录下的**全零场**派生文件（501×821 零数组），语义为"未发生任何编辑"
- DataScanService 在构建窗口时同步生成这 4 个零场 .npz 文件
- 首次编辑时 `session.base_version_id = '{window_id}_v000'`
- VersionService 查询最新版本、EditSession 加载基线、复盘回溯版本链均不需要特殊处理 v000

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

### 16.4.1 review_approval

记录审核操作，每次审核（通过或退回）产生一条记录。

```sql
CREATE TABLE review_approval (
    approval_id TEXT PRIMARY KEY,
    version_id TEXT NOT NULL,
    reviewer_id TEXT NOT NULL,
    action TEXT NOT NULL,
    comment TEXT,
    reviewed_at TEXT NOT NULL
);
```

| 字段 | 说明 |
|---|---|
| action | `approve`（通过）/ `reject`（退回） |
| comment | 审核意见，退回时必填 |

### 16.4.2 release_product

记录正式发布的产品，每个 window 同一时刻最多一条 `active` 记录。

```sql
CREATE TABLE release_product (
    release_id TEXT PRIMARY KEY,
    version_id TEXT NOT NULL,
    window_id TEXT NOT NULL,
    release_status TEXT NOT NULL DEFAULT 'active',
    product_path TEXT,
    manifest_path TEXT,
    released_by TEXT NOT NULL,
    released_at TEXT NOT NULL,
    superseded_at TEXT
);
```

| 字段 | 说明 |
|---|---|
| release_status | `active`（当前有效发布）/ `superseded`（已被新版本替代） |
| product_path | 发布产品的归档根路径 |
| manifest_path | product_manifest.json 路径（详见 docs/13 §13.8.1） |
| superseded_at | 被替代的时间，`active` 时为 NULL |

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
    op_ptype_transition_json TEXT,
    is_undone INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL
);
```

与原表差异：
- `version_id` → `session_id`：草稿操作不再挂到虚拟 "draft" 版本
- 新增 `sequence_no`：操作在 session 内的顺序号，支撑撤销/重做的栈定位
- 新增 `is_undone`：0=生效，1=已撤销。重做时改回 0。保存版本时只归档 `is_undone=0` 的操作
- `mask_path` → `mask_geometry_json` + `mask_raster_path`：同时保存用户绘制的原始几何和栅格化结果
- 新增 `op_ptype_transition_json`：操作级相态变化矩阵（4×4，JSON），统计本次操作 mask 范围内 ptype 变化（详见 docs/14 §14.9.2）

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
    image_path TEXT,
    plot_config_path TEXT,
    plot_input_manifest_path TEXT,
    plot_code_version TEXT,
    plot_status TEXT NOT NULL DEFAULT 'pending',
    attempt INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    locked_by TEXT,
    locked_at TEXT,
    next_retry_at TEXT,
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

`image_path` 生命周期约束：
- `plot_status` 为 `pending` / `running` / `failed` / `permanently_failed` 时 `image_path` 可为 NULL
- `plot_status` 为 `success` / `partial_success` 时 `image_path` 必须非空（由 PlotWorker 写入结果时保证）

### 16.8 用户与权限

#### 16.8.1 user

```sql
CREATE TABLE app_user (
    user_id TEXT PRIMARY KEY,
    username TEXT NOT NULL UNIQUE,
    display_name TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'forecaster',
    password_hash TEXT NOT NULL,
    is_active INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL,
    last_login_at TEXT
);
```

| 字段 | 说明 |
|---|---|
| role | `admin`（管理员）/ `reviewer`（审核员）/ `forecaster`（预报员）/ `viewer`（只读） |
| is_active | 1=启用，0=禁用 |

角色权限矩阵：

| 操作 | admin | reviewer | forecaster | viewer |
|---|:---:|:---:|:---:|:---:|
| 编辑 qpf/ptype | √ | √ | √ | × |
| 保存版本 | √ | √ | √ | × |
| 提交审核 | √ | √ | √ | × |
| 审核通过/退回 | √ | √ | × | × |
| 正式发布 | √ | √ | × | × |
| 查看复盘图 | √ | √ | √ | √ |
| 管理用户 | √ | × | × | × |
| 管理配置 | √ | × | × | × |
| 触发数据扫描 | √ | √ | × | × |
| 导出复盘包 | √ | √ | √ | √ |

#### 16.8.2 audit_log

系统级审计日志，记录所有关键操作（不限于编辑），用于安全审计和问题追溯。

```sql
CREATE TABLE audit_log (
    log_id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    action TEXT NOT NULL,
    resource_type TEXT NOT NULL,
    resource_id TEXT,
    detail_json TEXT,
    ip_address TEXT,
    created_at TEXT NOT NULL
);
```

| 字段 | 说明 |
|---|---|
| action | 操作类型：`login` / `logout` / `edit` / `save` / `submit` / `approve` / `reject` / `release` / `scan` / `export` / `config_change` |
| resource_type | 操作对象类型：`session` / `version` / `window` / `template` / `user` / `config` |
| resource_id | 操作对象 ID |
| detail_json | 操作详情，如配置变更的 before/after |

### 16.9 config_snapshot

记录每次配置变更，edit_version 可引用当时的配置快照。

```sql
CREATE TABLE config_snapshot (
    snapshot_id TEXT PRIMARY KEY,
    config_type TEXT NOT NULL,
    config_json TEXT NOT NULL,
    changed_by TEXT,
    created_at TEXT NOT NULL
);
```

| 字段 | 说明 |
|---|---|
| config_type | `product_config` / `grid_definition` / `plot_config` / `color_config` |
| config_json | 配置文件完整 JSON 内容 |

---

## 17. 后端 API 设计

### 17.0 数据接入 API

#### 17.0.1 触发数据扫描

```http
POST /api/data/scan
```

请求：

```json
{
  "case_id": "2026010108",
  "scan_type": "manual"
}
```

返回：

```json
{
  "scan_id": "scan_20260101_001",
  "case_id": "2026010108",
  "status": "completed",
  "tp_files_found": 81,
  "ptype_files_found": 81,
  "windows_created": 15,
  "windows_updated": 3,
  "errors": []
}
```

后端逻辑：
1. 根据 `case_id` 解析 init_time，定位数据目录
2. 扫描该目录下所有 `tp_999_deeplearning_*.txt` 和 `ptype_999_revised_*.txt`，统计可用时效
3. 根据 `product_config.json` 中的 `supported_accum_hours` 和时效步长，枚举所有可能的窗口
4. 对每个窗口：检查 end_lead tp 文件是否存在 → 计算 qpf_before → 合成 ptype_before → 执行 QC → 设置 status
5. 写入/更新 `product_window` 表，记录 `data_scan_log`

#### 17.0.2 查询数据接入状态

```http
GET /api/data/status?case_id=2026010108
```

返回：

```json
{
  "case_id": "2026010108",
  "init_time": "2026-01-01T08:00:00Z",
  "tp_files": { "total_expected": 81, "found": 81 },
  "ptype_files": { "total_expected": 81, "found": 78, "missing_leads": [222, 225, 228] },
  "windows": {
    "total": 20,
    "available": 15,
    "partial": 3,
    "invalid": 1,
    "pending": 1
  },
  "last_scan": {
    "scan_id": "scan_20260101_001",
    "finished_at": "2026-01-01T09:15:30Z",
    "status": "completed"
  }
}
```

#### 17.0.3 查询扫描日志

```http
GET /api/data/scan-logs?case_id=2026010108&limit=10
```

返回 `data_scan_log` 记录列表，按 `scan_started_at` 倒序。

### 17.1 查询产品窗口

```http
GET /api/windows?case_id=2026010108&accum_hours=24&status=available
```

支持按 `status` 过滤（可选，不传则返回所有非 `archived` 窗口）。

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
      "status": "available",
      "qc_status": "pass",
      "negative_count": 0,
      "missing_count": 0,
      "ptype_missing_leads": [],
      "data_ready_at": "2026-01-01T09:15:30Z"
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

返回当前 session 的元数据、操作状态和字段 URL 列表（不内联网格数据）。前端通过返回的 `field_urls` 并行请求 `/api/session/{session_id}/field/{field_name}` 获取 flat binary 格点数据（详见 `docs/17` §17.8.1）。若 session 已有未撤销的操作，字段接口返回的是操作叠加后的状态。

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

### 17.7.2 提交审核

```http
POST /api/version/submit
```

请求：

```json
{
  "version_id": "v003"
}
```

返回：

```json
{
  "version_id": "v003",
  "status": "submitted",
  "submitted_at": "2026-01-01T10:30:00Z"
}
```

后端逻辑：
1. 校验版本当前 status 必须为 `draft`，否则返回 409
2. 更新 `edit_version.status = 'submitted'`

### 17.7.3 审核版本

```http
POST /api/version/review
```

请求：

```json
{
  "version_id": "v003",
  "reviewer_id": "reviewer_001",
  "action": "approve",
  "comment": "降水调整合理"
}
```

返回：

```json
{
  "approval_id": "appr_20260101_001",
  "version_id": "v003",
  "action": "approve",
  "status": "approved"
}
```

后端逻辑：
1. 校验版本当前 status 必须为 `submitted`，否则返回 409
2. `action=approve` → `edit_version.status = 'approved'`
3. `action=reject` → `edit_version.status = 'rejected'`，`comment` 必填
4. 写入 `review_approval` 记录

### 17.7.4 发布版本

```http
POST /api/version/release
```

请求：

```json
{
  "version_id": "v003",
  "released_by": "forecaster_001"
}
```

返回：

```json
{
  "release_id": "rel_20260101_001",
  "version_id": "v003",
  "window_id": "2026010108_ACC24_024_048",
  "release_status": "active",
  "product_path": "archive/2026010108/ACC24_024_048/released/v003/",
  "manifest_path": "archive/2026010108/ACC24_024_048/released/v003/product_manifest.json"
}
```

后端逻辑：
1. 校验版本当前 status 必须为 `approved`，否则返回 409
2. 将该 window 已有的 `active` release_product 标记为 `superseded`（`superseded_at = now`）
3. 对应 edit_version 从 `released` 改为 `superseded`
4. 当前版本 `edit_version.status = 'released'`
5. 创建 `release_product` 记录（status=active）
6. 将版本数据复制到发布归档路径

### 17.7.5 查询版本审核历史

```http
GET /api/version/{version_id}/reviews
```

返回该版本的所有 `review_approval` 记录，按 `reviewed_at` 倒序。

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

### 17.9 复盘中心 API

#### 17.9.1 查询个例下所有窗口的复盘状态

```http
GET /api/review/case/{case_id}
```

返回：

```json
{
  "case_id": "2026010108",
  "windows": [
    {
      "window_id": "2026010108_ACC24_024_048",
      "accum_hours": 24,
      "start_lead": 24,
      "end_lead": 48,
      "latest_version_id": "v003",
      "version_status": "released",
      "review_count": 2,
      "latest_review": {
        "review_id": "rev_20260101_003",
        "plot_status": "success",
        "template_id": "snow_phase_review_v1"
      }
    }
  ]
}
```

#### 17.9.2 查询窗口的所有版本及复盘图

```http
GET /api/review/window/{window_id}/versions
```

返回该窗口所有 edit_version（含 status）及每个版本关联的 review_product 列表。

#### 17.9.3 查询单个复盘图详情

```http
GET /api/review/{review_id}
```

返回 `review_product` 完整记录，包括 image_path、missing_fields、plot_status、error_log 等。

#### 17.9.4 重新生成复盘图

```http
POST /api/review/regenerate
```

请求：

```json
{
  "review_id": "rev_20260101_003"
}
```

后端逻辑：
1. 读取原 review_product 的 window_id、version_id、template_id
2. 重新组装 review_payload
3. 提交绘图任务队列
4. 更新 review_product 的 plot_status 为 `pending`

#### 17.9.5 批量生成复盘图

```http
POST /api/review/batch-generate
```

请求：

```json
{
  "case_id": "2026010108",
  "template_id": "snow_phase_review_v1",
  "version_filter": "released"
}
```

后端逻辑：查找该 case 下所有匹配 version_filter 的版本，逐个提交绘图任务。返回 batch_id 用于进度查询。

#### 17.9.6 导出复盘包

```http
POST /api/review/export
```

请求：

```json
{
  "window_id": "2026010108_ACC24_024_048",
  "version_id": "v003",
  "format": "zip"
}
```

后端逻辑：按 §15 归档目录结构打包 zip，返回下载链接。

### 17.10 绘图任务队列 API

#### 17.10.1 绘图任务队列设计

绘图任务通过内部队列管理，支持并发控制和失败重试。

任务生命周期：

```text
pending ──(调度器取出)──→ running ──(成功)──→ success / partial_success
                                   ──(失败)──→ failed ──(自动重试 ≤ max_retries)──→ pending
                                                       ──(超过重试次数)──→ permanently_failed
```

队列配置（写入 product_config.json）：

```json
{
  "plot_queue": {
    "max_concurrent": 2,
    "max_retries": 3,
    "retry_delay_seconds": 30,
    "task_timeout_seconds": 300
  }
}
```

#### 17.10.2 查询绘图任务状态

```http
GET /api/tasks/plot/{review_id}
```

返回：

```json
{
  "review_id": "rev_20260101_003",
  "plot_status": "running",
  "attempt": 1,
  "max_retries": 3,
  "started_at": "2026-01-01T10:30:00Z",
  "progress": {
    "total_panels": 8,
    "completed_panels": 5
  }
}
```

#### 17.10.3 查询任务队列概览

```http
GET /api/tasks/plot/queue
```

返回当前队列中各状态的任务数量和最近的任务列表。

#### 17.10.4 手动重试失败任务

```http
POST /api/tasks/plot/retry
```

请求：

```json
{
  "review_id": "rev_20260101_003"
}
```

后端逻辑：仅 `failed` / `permanently_failed` 状态可重试，重置 attempt 计数，重新入队。

### 17.11 绘图模板管理 API

#### 17.11.1 查询可用模板列表

```http
GET /api/templates
```

返回所有绘图模板配置（§14.5 定义的模板列表）。

#### 17.11.2 查询单个模板详情

```http
GET /api/templates/{template_id}
```

返回模板的 required_fields、optional_fields、面板布局等完整配置。

#### 17.11.3 新增/更新模板

```http
PUT /api/templates/{template_id}
```

请求体为模板 JSON 配置。后端校验 required_fields 中的变量名是否合法，保存到模板配置目录。

### 17.12 用户与权限 API

#### 17.12.1 登录

```http
POST /api/auth/login
```

请求：

```json
{
  "username": "forecaster_001",
  "password": "..."
}
```

返回：

```json
{
  "user_id": "user_001",
  "username": "forecaster_001",
  "display_name": "张三",
  "role": "forecaster",
  "token": "jwt_token_here",
  "expires_at": "2026-01-02T08:00:00Z"
}
```

后端逻辑：校验密码，签发 JWT token，记录 audit_log（action=login）。

#### 17.12.2 用户管理（仅 admin）

```http
GET /api/users                    # 查询用户列表
POST /api/users                   # 创建用户
PUT /api/users/{user_id}          # 更新用户信息/角色/启用状态
```

所有用户管理操作记录 audit_log。

#### 17.12.3 权限中间件

所有 API 请求需携带 `Authorization: Bearer {token}`。后端中间件：
1. 解析 JWT，提取 user_id 和 role
2. 按 §16.8.1 角色权限矩阵校验是否有权执行该操作
3. 无权限返回 403

### 17.13 审计与监控 API

#### 17.13.1 查询审计日志

```http
GET /api/audit/logs?user_id=user_001&action=edit&resource_type=session&limit=50&offset=0
```

支持按 user_id、action、resource_type、时间范围过滤。返回 audit_log 记录列表。

#### 17.13.2 健康检查

```http
GET /api/health
```

返回：

```json
{
  "status": "healthy",
  "version": "1.0.0",
  "uptime_seconds": 86400,
  "database": "ok",
  "data_scan_service": "running",
  "plot_queue": {
    "pending": 2,
    "running": 1,
    "failed": 0
  },
  "disk": {
    "archive_dir_gb_used": 12.5,
    "archive_dir_gb_free": 87.5,
    "tmp_dir_gb_used": 0.3
  }
}
```

#### 17.13.3 磁盘与归档监控

```http
GET /api/monitor/storage
```

返回各目录的磁盘使用情况、归档包数量、最大/最小归档包大小。

#### 17.13.4 任务监控总览

```http
GET /api/monitor/tasks
```

返回：数据扫描任务状态、绘图任务队列状态、最近失败任务列表。

### 17.14 历史统计与导出 API

#### 17.14.1 操作统计查询

```http
GET /api/stats/operations?start_date=2026-01-01&end_date=2026-01-31
```

返回：

```json
{
  "period": "2026-01-01 ~ 2026-01-31",
  "total_sessions": 120,
  "total_operations": 856,
  "total_versions_saved": 95,
  "total_versions_released": 72,
  "by_accum_hours": {
    "24": { "sessions": 65, "versions": 52 },
    "48": { "sessions": 35, "versions": 28 },
    "168": { "sessions": 20, "versions": 15 }
  },
  "by_tool": {
    "polygon": 412,
    "line_buffer": 156,
    "brush_path": 288
  },
  "by_operation": {
    "increase": 320,
    "decrease": 180,
    "set_value": 95,
    "multiply": 45,
    "clear": 60,
    "set_ptype": 156
  }
}
```

#### 17.14.2 相态变化统计

```http
GET /api/stats/ptype-transitions?start_date=2026-01-01&end_date=2026-01-31
```

返回时段内所有版本的相态转换矩阵汇总（4×4 矩阵）。

#### 17.14.3 批量导出统计数据

```http
POST /api/stats/export
```

请求：

```json
{
  "start_date": "2026-01-01",
  "end_date": "2026-01-31",
  "format": "csv",
  "include": ["operations", "ptype_transitions", "version_summary"]
}
```

后端逻辑：按请求的 include 列表，查询数据库，生成 CSV 文件打包下载。

导出字段包含：case_id、window_id、version_id、init_time、accum_hours、start_lead、end_lead、modified_grid_count、modified_area、qpf_sum_before、qpf_sum_after、ptype_transition_matrix、operation_count、review_plot_status。

### 17.15 配置管理 API

#### 17.15.1 查询当前配置

```http
GET /api/config/{config_type}
```

config_type：`product_config` / `grid_definition` / `plot_config`。

#### 17.15.2 更新配置

```http
PUT /api/config/{config_type}
```

后端逻辑：
1. 校验 JSON 格式和必填字段
2. 保存新配置文件
3. 创建 config_snapshot 记录（含 before/after 对比）
4. 记录 audit_log（action=config_change）

#### 17.15.3 查询配置变更历史

```http
GET /api/config/{config_type}/history?limit=10
```

返回 config_snapshot 记录列表。

---

