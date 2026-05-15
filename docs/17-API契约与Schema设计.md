# 17. API 契约与 Schema 设计

## 17.1 设计目标

`docs/07` 已列出 API 功能和业务语义；本文补充代码开发所需的统一响应格式、Pydantic Schema、权限、幂等性、审计规则和典型错误码。

---

## 17.2 统一响应格式

成功：

```json
{
  "code": "OK",
  "message": "success",
  "data": {},
  "trace_id": "req_20260101_001"
}
```

失败：

```json
{
  "code": "VERSION_STATUS_CONFLICT",
  "message": "当前版本状态不允许该操作",
  "detail": {},
  "trace_id": "req_20260101_001"
}
```

后端所有接口统一返回该结构，文件下载接口除外。`message` 字段使用中文（本产品面向中国气象业务用户，不做国际化）。

---

## 17.3 通用 Schema

```python
class ApiResponse(BaseModel, Generic[T]):
    code: str = 'OK'
    message: str = 'success'
    data: T | None = None
    trace_id: str

class ErrorResponse(BaseModel):
    code: str
    message: str
    detail: dict = {}
    trace_id: str
```

---

## 17.4 Auth Schema

```python
class LoginRequest(BaseModel):
    username: str
    password: str

class LoginResponse(BaseModel):
    user_id: str
    username: str
    display_name: str
    role: Literal['admin', 'reviewer', 'forecaster', 'viewer']
    token: str
    expires_at: datetime
```

权限：公开接口。审计：`login`。

---

## 17.5 Data Scan Schema

```python
class DataScanRequest(BaseModel):
    case_id: str
    scan_type: Literal['manual', 'scheduled', 'startup'] = 'manual'

class DataScanResponse(BaseModel):
    scan_id: str
    case_id: str
    status: Literal['running', 'completed', 'failed']
    tp_files_found: int
    ptype_files_found: int
    windows_created: int
    windows_updated: int
    errors: list[dict] = []
```

接口：`POST /api/data/scan`

权限：`admin`, `reviewer`。

审计：`action=scan, resource_type=case`。

错误码：`CASE_NOT_FOUND`, `FILE_NOT_FOUND`, `CONFIG_VALIDATION_FAILED`。

---

## 17.6 Window Schema

```python
class ProductWindowDTO(BaseModel):
    window_id: str
    case_id: str
    accum_hours: Literal[24, 48, 168]
    start_lead: int
    end_lead: int
    status: Literal['pending', 'available', 'partial', 'invalid', 'archived']
    qc_status: Literal['unchecked', 'pass', 'warn', 'fail']
    negative_count: int = 0
    negative_min_value: float | None = None
    negative_abs_max: float | None = None
    missing_count: int = 0
    ptype_missing_leads: list[int] = []
    data_ready_at: datetime | None = None
```

接口：`GET /api/windows`

权限：所有登录用户。

---

## 17.7 Session Schema

```python
class SessionStartRequest(BaseModel):
    window_id: str

class SessionStartResponse(BaseModel):
    session_id: str
    window_id: str
    base_version_id: str
    status: Literal['editing']
```

接口：`POST /api/session/start`

权限：`admin`, `reviewer`, `forecaster`。

错误码：`WINDOW_NOT_FOUND`, `WINDOW_NOT_EDITABLE`, `WINDOW_LOCKED`。

审计：`session_start`。

---

## 17.8 Edit Preview Schema

```python
class MaskGeometry(BaseModel):
    type: Literal['polygon', 'line_buffer', 'brush_path']
    coordinates: list[tuple[float, float]] | None = None
    points: list[tuple[float, float]] | None = None
    width_grid: int | None = None
    radius_grid: int | None = None

class EditParameters(BaseModel):
    value: float | None = None
    delta_mm: float | None = None
    factor: float | None = None
    target_ptype: Literal[0,1,2,3] | None = None
    threshold: float | None = None
    only_nonzero: bool = False

class EditPreviewRequest(BaseModel):
    session_id: str
    tool: Literal['polygon', 'line_buffer', 'brush']
    variable: Literal['qpf', 'ptype']
    operation: Literal['set_value', 'increase', 'decrease', 'multiply', 'clear', 'set_ptype', 'screen_clear']
    mask: MaskGeometry
    parameters: EditParameters

class EditPreviewResponse(BaseModel):
    preview_id: str
    affected_grid_count: int
    affected_area_km2: float
    area_mode: str
    before_stats: dict
    after_stats: dict
    op_ptype_transition: dict[str, int] | None = None
    new_precip_needs_ptype: bool = False
    new_precip_count: int = 0
    warnings: list[dict] = []
    preview_image: str | None = None
```

接口：`POST /api/edit/preview`

权限：`admin`, `reviewer`, `forecaster`。

审计：不写 audit_log。

错误码：`SESSION_NOT_FOUND`, `SESSION_NOT_EDITING`, `MASK_OUT_OF_BOUNDS`, `INVALID_OPERATION_PARAM`。

---

## 17.8.1 字段数据传输协议（Grid Field Binary API）

### 设计决策

501×821 = 411,321 格点，JSON 传输不可接受（qpf Float32 JSON ≈ 4MB+，解析开销大）。采用 **逐字段 flat binary** 方案：

- 后端 `ndarray.astype(dtype).tobytes()` 输出行优先（C order）原始字节
- 前端 `new Float32Array(arrayBuffer)` / `new Uint8Array(arrayBuffer)` 零拷贝构造
- HTTP gzip 压缩后：qpf ~300-500KB，ptype/mask ~50-100KB
- 直接可用于 WebGL DataTileSource，无需二次解码

### 接口定义

```http
GET /api/session/{session_id}/field/{field_name}
```

路径参数：

| 参数 | 类型 | 说明 |
|---|---|---|
| session_id | string | 编辑会话 ID |
| field_name | enum | 见下表 |

field_name 枚举：

| field_name | dtype | 字节数 | 说明 |
|---|---|---:|---|
| qpf_before | float32 | 1,645,284 | 订正前累计降水 |
| qpf_after | float32 | 1,645,284 | 订正后累计降水（含已 apply 操作） |
| ptype_before | uint8 | 411,321 | 订正前累计相态 |
| ptype_after | uint8 | 411,321 | 订正后累计相态 |
| touched_mask | uint8 | 411,321 | 用户操作曾影响过的格点（所有 operation_mask 并集） |
| changed_mask | uint8 | 411,321 | 最终 after 与 before 实际不同的格点（保存后可用） |
| invalid_mask | uint8 | 411,321 | 无效格点标记（QC 不合格） |

响应：

```text
Content-Type: application/octet-stream
Content-Encoding: gzip
X-Grid-Rows: 501
X-Grid-Cols: 821
X-Grid-Dtype: float32 | uint8
X-Grid-Order: C

Body: raw bytes, row-major, shape=(501, 821)
```

前端使用示例：

```ts
const res = await fetch(`/api/session/${sid}/field/qpf_after`)
const buf = await res.arrayBuffer()
const qpfAfter = new Float32Array(buf) // length === 411321
```

### 预览字段接口

编辑预览产生的临时字段，preview 过期后不可访问：

```http
GET /api/preview/{preview_id}/field/{field_name}
```

field_name 限 `qpf_preview` | `ptype_preview`，格式与 session 字段接口一致。

### 版本字段接口（只读回看）

```http
GET /api/version/{version_id}/field/{field_name}
```

field_name 限 `qpf_after` | `ptype_after` | `touched_mask` | `changed_mask`。用于审核页面和复盘回看。

### 原始窗口字段接口

未进入编辑会话时查看原始数据：

```http
GET /api/window/{window_id}/field/{field_name}
```

field_name 限 `qpf_before` | `ptype_before` | `invalid_mask`。

### Session Load 接口补充

`GET /api/session/{session_id}/load` 返回元数据和字段 URL 列表，**不内联字段数据**：

```python
class SessionLoadResponse(BaseModel):
    session_id: str
    window_id: str
    base_version_id: str
    status: Literal['editing']
    grid_rows: int = 501
    grid_cols: int = 821
    operation_count: int
    can_undo: bool
    can_redo: bool
    field_urls: dict[str, str]
    # 示例: {"qpf_before": "/api/session/sess_001/field/qpf_before", ...}
    before_image: str | None = None
    after_image: str | None = None
```

前端 `loadSession()` 后并行 fetch 所需字段：

```ts
const meta = await api.loadSession(sessionId)
const [qpfBuf, ptypeBuf] = await Promise.all([
  fetch(meta.field_urls.qpf_after).then(r => r.arrayBuffer()),
  fetch(meta.field_urls.ptype_after).then(r => r.arrayBuffer()),
])
editorStore.qpfAfter = new Float32Array(qpfBuf)
editorStore.ptypeAfter = new Uint8Array(ptypeBuf)
```

权限：与 session 接口一致（admin/reviewer/forecaster）。

错误码：`SESSION_NOT_FOUND`, `FIELD_NOT_AVAILABLE`（preview 过期或字段尚未生成）。

---

## 17.9 Edit Apply Schema

```python
class EditApplyRequest(BaseModel):
    session_id: str
    preview_id: str

class EditApplyResponse(BaseModel):
    operation_id: str
    sequence_no: int
    applied: bool = True
```

接口：`POST /api/edit/apply`

幂等性：不幂等。重复 apply 同一 preview 返回 `PREVIEW_EXPIRED` 或 `PREVIEW_CONFLICT`。

审计：可选写 `edit`，建议关键系统中写 audit_log，detail 只记录摘要，不存完整 mask。

---

## 17.10 Save / Submit / Review / Release Schema

```python
class VersionSaveRequest(BaseModel):
    session_id: str
    generate_review: bool = True

class VersionSaveResponse(BaseModel):
    session_id: str
    version_id: str
    before_image: str | None
    after_image: str | None
    review_image: str | None
```

```python
class VersionSubmitRequest(BaseModel):
    version_id: str

class VersionReviewRequest(BaseModel):
    version_id: str
    action: Literal['approve', 'reject']
    comment: str | None = None

class VersionReleaseRequest(BaseModel):
    version_id: str
```

权限：

| API | 权限 |
|---|---|
| save | admin/reviewer/forecaster |
| submit | admin/reviewer/forecaster |
| review | admin/reviewer |
| release | admin/reviewer |

审计：全部写 audit_log。

状态冲突均返回 `VERSION_STATUS_CONFLICT`。

---

## 17.11 Review / Task Schema

```python
class ReviewGenerateRequest(BaseModel):
    window_id: str
    version_id: str
    template_id: str

class MissingField(BaseModel):
    name: str
    level_type: str | None = None
    level_value: int | None = None
    lead_hour: int | None = None
    reason: Literal['file_not_found', 'read_error', 'dimension_mismatch']
    expected_path: str | None = None

class ReviewGenerateResponse(BaseModel):
    review_id: str
    plot_status: Literal['pending', 'running', 'success', 'partial_success', 'failed', 'permanently_failed']
    total_panels: int | None = None
    success_panels: int | None = None
    skipped_panels: int | None = None
    missing_fields: list[MissingField] = []
    image_path: str | None = None
    error_log: str | None = None
```

绘图生成异步返回 `pending`，前端通过 `/api/tasks/plot/{review_id}` 轮询。

---

## 17.12 Config Schema

```python
class ConfigUpdateRequest(BaseModel):
    config_type: Literal['product_config', 'grid_definition', 'plot_config', 'color_config']
    config_json: dict
```

权限：admin。

审计：`config_change`，detail_json 记录 before/after 摘要。

错误码：`CONFIG_VALIDATION_FAILED`。

---

## 17.13 API 幂等性

| API | 幂等性 | 说明 |
|---|---|---|
| GET 查询 | 幂等 | 不修改状态 |
| data scan | 非幂等但可重复 | 重复扫描会 update windows |
| session/start | 非幂等 | 每次生成新 session，除非锁策略拒绝 |
| preview | 非幂等 | 新 preview 覆盖旧 preview |
| apply | 非幂等 | 同 preview 只能 apply 一次 |
| undo/redo | 非幂等 | 每次改变当前状态 |
| version/save | 非幂等 | 每次产生新 version |
| submit/review/release | 状态机幂等性受限 | 重复请求返回状态冲突 |
| review/regenerate | 非幂等 | 产生新任务或更新 review 状态 |
| config/update | 非幂等 | 产生 config_snapshot |

---

## 17.14 审计策略

| API | audit action |
|---|---|
| login | login |
| data scan | scan |
| session start | session_start |
| session discard | session_discard |
| apply | edit，可选 |
| save | save |
| submit | submit |
| review approve/reject | approve/reject |
| release | release |
| review export | export |
| template update | config_change |
| config update | config_change |
| user create/update | user_manage |

---

## 17.15 OpenAPI 生成建议

FastAPI 自动生成 OpenAPI，但应在代码中显式设置：

```python
app = FastAPI(
    title='Mesh Editor API',
    version='2.0.0',
    description='固定区域累计降水与相态网格编辑及复盘平台'
)
```

所有路由加 tags：

```text
Auth / DataScan / Windows / Sessions / Edit / Versions / Review / Tasks / Templates / Audit / Monitor / Stats / Config
```
