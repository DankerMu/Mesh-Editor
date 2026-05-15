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

### 16.4 edit_operation

```sql
CREATE TABLE edit_operation (
    operation_id TEXT PRIMARY KEY,
    version_id TEXT NOT NULL,
    window_id TEXT NOT NULL,
    tool_name TEXT NOT NULL,
    variable_name TEXT NOT NULL,
    operation_type TEXT NOT NULL,
    parameters_json TEXT,
    mask_path TEXT,
    before_stats_json TEXT,
    after_stats_json TEXT,
    created_by TEXT,
    created_at TEXT NOT NULL
);
```

### 16.5 review_field

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

### 16.6 review_product

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

### 17.2 加载窗口数据

```http
GET /api/window/{window_id}/load
```

返回当前 qpf、ptype 的数据摘要和图片路径。

### 17.3 编辑预览

```http
POST /api/edit/preview
```

请求：

```json
{
  "window_id": "2026010108_ACC24_024_048",
  "version_id": "draft",
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
  "affected_grid_count": 286,
  "affected_area_km2": 7150,
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

### 17.4 应用编辑

```http
POST /api/edit/apply
```

作用：把预览结果写入当前草稿，并记录一条操作日志。

### 17.5 撤销 / 重做

```http
POST /api/edit/undo
POST /api/edit/redo
```

### 17.6 保存版本

```http
POST /api/version/save
```

请求：

```json
{
  "window_id": "2026010108_ACC24_024_048",
  "user": "forecaster_001",
  "generate_review": true
}
```

返回：

```json
{
  "version_id": "v003",
  "before_image": "images/before_product.png",
  "after_image": "images/after_product.png",
  "review_image": "images/review_composite.png"
}
```

### 17.7 生成复盘图

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

---

