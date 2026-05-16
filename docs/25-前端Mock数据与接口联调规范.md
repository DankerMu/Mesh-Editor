# 25. 前端 Mock 数据与接口联调规范

## 25.1 文档定位

本文定义前端在后端接口未完全就绪前如何使用 Mock 数据开发页面，并规范与后端联调时的字段、二进制数据、错误码和任务状态。目标是保证 18 张效果图对应页面均可先用 mock 开发，后续平滑切换真实接口。

---

## 25.2 Mock 目录结构

```text
frontend/src/mock/
  handlers.ts
  fixtures/
    auth.ts
    windows.ts
    session.ts
    fields.ts
    operations.ts
    versions.ts
    approval.ts
    review.ts
    analysis.ts
    admin.ts
```

推荐使用 MSW（Mock Service Worker）拦截请求；也可以在 `api/http.ts` 中通过 `VITE_USE_MOCK=true` 切换。

---

## 25.3 样例 ID 统一规则

Mock 数据与效果图统一使用：

```text
case_id: CASE_2026011508
window_id: ACC24_024_048
session_id: sess_20260115_001
version_id: V003
review_id: RVW_20260115_003
```

状态示例：

```text
window.status: available / partial / invalid
version.status: draft / submitted / approved / released
plot_status: success / partial_success / failed
```

---

## 25.4 Window Mock

`windows.ts` 应包含：

```ts
export const windows = [
  { window_id: 'ACC24_000_024', accum_hours: 24, start_lead: 0, end_lead: 24, status: 'available', qc_status: 'pass' },
  { window_id: 'ACC24_024_048', accum_hours: 24, start_lead: 24, end_lead: 48, status: 'available', qc_status: 'pass' },
  { window_id: 'ACC24_048_072', accum_hours: 24, start_lead: 48, end_lead: 72, status: 'partial', qc_status: 'warn', ptype_missing_leads: [57,60] },
  { window_id: 'ACC168_000_168', accum_hours: 168, start_lead: 0, end_lead: 168, status: 'invalid', qc_status: 'fail' }
]
```

用于 F02 数据窗口选择页。

---

## 25.5 字段二进制 Mock

生成 501×821 flat binary：

```ts
const rows = 501
const cols = 821
const qpf = new Float32Array(rows * cols)
const ptype = new Uint8Array(rows * cols)
```

Mock 响应 header：

```text
Content-Type: application/octet-stream
X-Grid-Rows: 501
X-Grid-Cols: 821
X-Grid-Dtype: float32 | uint8
X-Grid-Order: C
X-Grid-Byte-Length: {byteLength}
X-Grid-Variable: qpf_after
```

注意：Mock 数据应包含雨、雪、雨夹雪、无降水四类，否则 F01/F03/F04 图层效果无法验证。

---

## 25.6 Session Mock

`GET /api/session/{session_id}/load` mock：

```json
{
  "session_id": "sess_20260115_001",
  "window_id": "ACC24_024_048",
  "base_version_id": "ACC24_024_048_v000",
  "status": "editing",
  "grid_rows": 501,
  "grid_cols": 821,
  "operation_count": 2,
  "can_undo": true,
  "can_redo": false,
  "field_urls": {
    "qpf_before": "/api/session/sess_20260115_001/field/qpf_before",
    "qpf_after": "/api/session/sess_20260115_001/field/qpf_after",
    "ptype_before": "/api/session/sess_20260115_001/field/ptype_before",
    "ptype_after": "/api/session/sess_20260115_001/field/ptype_after",
    "invalid_mask": "/api/session/sess_20260115_001/field/invalid_mask"
  }
}
```

---

## 25.7 Preview Mock

普通 preview response：

```json
{
  "preview_id": "PREV_20260115_003",
  "affected_grid_count": 326,
  "affected_area_km2": 1045,
  "area_mode": "approximate_25km2",
  "before_stats": {"mean": 6.2, "sum": 2021.2},
  "after_stats": {"mean": 8.2, "sum": 3000.0},
  "op_ptype_transition": {"1_to_2": 214, "1_to_3": 86, "0_to_2": 26},
  "new_precip_needs_ptype": false,
  "new_precip_count": 0
}
```

新增降水相态选择场景：

```json
{
  "preview_id": "PREV_20260115_004",
  "new_precip_needs_ptype": true,
  "new_precip_count": 26
}
```

---

## 25.8 Review Mock

用于 F06/F15：

```ts
export const reviewSuccess = {
  review_id: 'RVW_20260115_003',
  plot_status: 'success',
  total_panels: 7,
  success_panels: 7,
  skipped_panels: 0,
  missing_fields: []
}

export const reviewPartial = {
  review_id: 'RVW_20260115_004',
  plot_status: 'partial_success',
  total_panels: 8,
  success_panels: 6,
  skipped_panels: 2,
  missing_fields: [
    { name: 'rh', level_type: 'pressure', level_value: 700, lead_hour: 72, reason: 'file_not_found' }
  ]
}
```

---

## 25.9 Mock 页面演示路由

开发阶段可提供 demo query：

```text
/editor?mock=normal
/editor?mock=partial
/editor?mock=preview
/approval/version/V003?mock=submitted
/review/product/RVW_20260115_003?mock=success
/review/product/RVW_20260115_004?mock=partial
```

这些 demo 路由只用于开发环境，不进入生产。

---

## 25.10 联调规则

1. 前端以 `schemas/enums.json` 和 `schemas/error_codes.json` 生成类型，不手写枚举。
2. 字段二进制接口必须校验 header 中 rows/cols/dtype/byteLength。
3. 所有 API 响应统一 `code/message/data/trace_id`。
4. 真实接口替换 mock 前，必须先跑 `docs/18` 中的集成测试。
5. 联调失败必须记录 trace_id 和请求参数。
