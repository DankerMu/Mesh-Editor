import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { editApply, editPreview, editRedo, editUndo, getOperations } from '@/api/edit'
import { fetchField, loadSession, startSession } from '@/api/sessions'
import { GRID_COLS, GRID_ROWS } from '@/constants/precipColors'
import { useEditorStore } from '@/stores/editorStore'
import type { MaskGeometry } from '@/types/editor'

vi.mock('@/api/sessions', () => ({
  startSession: vi.fn(),
  loadSession: vi.fn(),
  fetchField: vi.fn(),
}))

vi.mock('@/api/edit', () => ({
  editPreview: vi.fn(),
  editApply: vi.fn(),
  editUndo: vi.fn(),
  editRedo: vi.fn(),
  getOperations: vi.fn(),
}))

const GRID_COUNT = GRID_ROWS * GRID_COLS
const QPF_BYTES = GRID_COUNT * Float32Array.BYTES_PER_ELEMENT
const UINT8_BYTES = GRID_COUNT

const FIELD_URLS = {
  qpf_before: '/field/qpf_before',
  ptype_before: '/field/ptype_before',
  qpf_after: '/field/qpf_after',
  ptype_after: '/field/ptype_after',
  touched_mask: '/field/touched_mask',
  changed_mask: '/field/changed_mask',
  invalid_mask: '/field/invalid_mask',
}

const PREVIEW_RESPONSE = {
  preview_id: 'preview-1',
  affected_grid_count: 12,
  affected_area_km2: 34.5,
  before_stats: { min: 0, max: 3, mean: 1.2, sum: 14.4 },
  after_stats: { min: 1, max: 5, mean: 2.2, sum: 26.4 },
  op_ptype_transition: { '0->1': 2 },
  new_precip_needs_ptype: false,
  new_precip_count: 0,
  warnings: [],
}

const OPERATION = {
  sequence_no: 1,
  tool_name: 'polygon',
  operation_type: 'increase',
  variable_name: 'qpf',
  affected_grid_count: 12,
  is_undone: 0,
  created_at: '2026-05-17T00:00:00Z',
}

function mockLoadSession() {
  vi.mocked(loadSession).mockResolvedValue({
    session_id: 'session-1',
    window_id: 'window-1',
    base_version_id: 'version-0',
    status: 'editing',
    grid_rows: GRID_ROWS,
    grid_cols: GRID_COLS,
    operation_count: 0,
    can_undo: false,
    can_redo: false,
    field_urls: FIELD_URLS,
    before_image: null,
    after_image: null,
  })
}

function mockFieldFetches() {
  vi.mocked(fetchField).mockImplementation(async (url: string) => ({
    buffer: new ArrayBuffer(url.includes('qpf_') ? QPF_BYTES : UINT8_BYTES),
    headers: {},
  }))
}

describe('editorStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(startSession).mockReset()
    vi.mocked(loadSession).mockReset()
    vi.mocked(fetchField).mockReset()
    vi.mocked(editPreview).mockReset()
    vi.mocked(editApply).mockReset()
    vi.mocked(editUndo).mockReset()
    vi.mocked(editRedo).mockReset()
    vi.mocked(getOperations).mockReset()
  })

  it('startSession 成功后写入会话状态并触发字段加载', async () => {
    vi.mocked(startSession).mockResolvedValue({
      session_id: 'session-1',
      window_id: 'window-1',
      base_version_id: 'version-0',
      status: 'editing',
      created_at: '2026-05-17T00:00:00Z',
    })
    mockLoadSession()
    mockFieldFetches()

    const store = useEditorStore()
    await store.startSession('window-1')

    expect(startSession).toHaveBeenCalledWith('window-1')
    expect(loadSession).toHaveBeenCalledWith('session-1')
    expect(store.sessionId).toBe('session-1')
    expect(store.windowId).toBe('window-1')
    expect(store.baseVersionId).toBe('version-0')
    expect(store.qpfBefore).toBeInstanceOf(Float32Array)
    expect(store.fieldLoadError).toBeNull()
    expect(store.loadingSession).toBe(false)
  })

  it('startSession 失败时写入 fieldLoadError', async () => {
    vi.mocked(startSession).mockRejectedValue(new Error('创建会话失败'))

    const store = useEditorStore()
    await store.startSession('window-1')

    expect(store.fieldLoadError).toBe('创建会话失败')
    expect(store.loadingSession).toBe(false)
  })

  it('loadSession 成功后填充 7 个 TypedArray', async () => {
    mockLoadSession()
    mockFieldFetches()

    const store = useEditorStore()
    await store.loadSession('session-1')

    expect(store.qpfBefore).toBeInstanceOf(Float32Array)
    expect(store.qpfBefore).toHaveLength(GRID_COUNT)
    expect(store.qpfAfter).toBeInstanceOf(Float32Array)
    expect(store.qpfAfter).toHaveLength(GRID_COUNT)
    expect(store.ptypeBefore).toBeInstanceOf(Uint8Array)
    expect(store.ptypeBefore).toHaveLength(GRID_COUNT)
    expect(store.ptypeAfter).toBeInstanceOf(Uint8Array)
    expect(store.touchedMask).toBeInstanceOf(Uint8Array)
    expect(store.changedMask).toBeInstanceOf(Uint8Array)
    expect(store.invalidMask).toBeInstanceOf(Uint8Array)
    expect(fetchField).toHaveBeenCalledTimes(7)
    expect(store.loadingFields).toBe(false)
  })

  it('loadSession byte-length 不匹配时写入错误并清空数组', async () => {
    mockLoadSession()
    vi.mocked(fetchField).mockResolvedValue({ buffer: new ArrayBuffer(4), headers: {} })

    const store = useEditorStore()
    await store.loadSession('session-1')

    expect(store.fieldLoadError).toContain('byte-length mismatch')
    expect(store.qpfBefore).toBeNull()
    expect(store.ptypeBefore).toBeNull()
    expect(store.qpfAfter).toBeNull()
    expect(store.ptypeAfter).toBeNull()
    expect(store.touchedMask).toBeNull()
    expect(store.changedMask).toBeNull()
    expect(store.invalidMask).toBeNull()
    expect(store.loadingFields).toBe(false)
  })

  it('setActiveTool 有 session 时设置工具', () => {
    const store = useEditorStore()
    store.sessionId = 'session-1'

    store.setActiveTool('polygon')

    expect(store.activeTool).toBe('polygon')
  })

  it('setActiveTool 无 session 时 no-op', () => {
    const store = useEditorStore()

    store.setActiveTool('polygon')

    expect(store.activeTool).toBeNull()
  })

  it('setMaskGeometry 设置当前几何', () => {
    const geom: MaskGeometry = {
      type: 'polygon',
      coordinates: [
        [100, 30],
        [101, 30],
        [101, 31],
      ],
    }
    const store = useEditorStore()

    store.setMaskGeometry(geom)

    expect(store.currentMaskGeometry).toEqual(geom)
  })

  it('clearMask 清空几何和工具', () => {
    const store = useEditorStore()
    store.sessionId = 'session-1'
    store.setActiveTool('brush_path')
    store.setMaskGeometry({
      type: 'brush_path',
      points: [[100, 30]],
      radius_grid: 3,
    })

    store.clearMask()

    expect(store.currentMaskGeometry).toBeNull()
    expect(store.activeTool).toBeNull()
  })

  it('reset 恢复初始状态', async () => {
    mockLoadSession()
    mockFieldFetches()
    const store = useEditorStore()
    await store.loadSession('session-1')
    store.setActiveTool('line_buffer')
    store.setMaskGeometry({
      type: 'line_buffer',
      coordinates: [[100, 30]],
      width_grid: 5,
    })
    store.fieldLoadError = 'error'

    store.reset()

    expect(store.sessionId).toBeNull()
    expect(store.windowId).toBeNull()
    expect(store.baseVersionId).toBeNull()
    expect(store.currentVersionId).toBeNull()
    expect(store.qpfBefore).toBeNull()
    expect(store.ptypeBefore).toBeNull()
    expect(store.activeTool).toBeNull()
    expect(store.currentMaskGeometry).toBeNull()
    expect(store.selectedViewMode).toBe('after')
    expect(store.operations).toEqual([])
    expect(store.canUndo).toBe(false)
    expect(store.canRedo).toBe(false)
    expect(store.dirty).toBe(false)
    expect(store.loadingSession).toBe(false)
    expect(store.loadingFields).toBe(false)
    expect(store.previewLoading).toBe(false)
    expect(store.applyLoading).toBe(false)
    expect(store.saveLoading).toBe(false)
    expect(store.fieldLoadError).toBeNull()
    expect(store.previewError).toBeNull()
  })

  it('requestPreview 设置 previewResult 并维护 previewLoading', async () => {
    let resolvePreview: (value: typeof PREVIEW_RESPONSE) => void = () => undefined
    vi.mocked(editPreview).mockReturnValue(
      new Promise((resolve) => {
        resolvePreview = resolve
      }),
    )

    const store = useEditorStore()
    store.sessionId = 'session-1'
    const request = store.requestPreview(
      'polygon',
      'qpf',
      'increase',
      { coordinates: [[100, 30]] },
      { delta_mm: 2 },
    )

    expect(store.previewLoading).toBe(true)
    resolvePreview(PREVIEW_RESPONSE)
    await request

    expect(editPreview).toHaveBeenCalledWith({
      session_id: 'session-1',
      tool: 'polygon',
      variable: 'qpf',
      operation: 'increase',
      mask: { coordinates: [[100, 30]] },
      parameters: { delta_mm: 2 },
    })
    expect(store.previewResult).toEqual(PREVIEW_RESPONSE)
    expect(store.previewId).toBe('preview-1')
    expect(store.previewLoading).toBe(false)
  })

  it('applyEdit 调用 API、清空 preview、刷新字段并更新 undo/redo', async () => {
    mockFieldFetches()
    vi.mocked(editApply).mockResolvedValue({
      operation_id: 'operation-1',
      sequence_no: 1,
      applied: true,
      can_undo: true,
      can_redo: false,
    })
    vi.mocked(getOperations).mockResolvedValue({ operations: [OPERATION] })

    const store = useEditorStore()
    store.sessionId = 'session-1'
    store.previewResult = PREVIEW_RESPONSE
    store.previewId = PREVIEW_RESPONSE.preview_id

    await store.applyEdit(2)

    expect(editApply).toHaveBeenCalledWith({
      session_id: 'session-1',
      preview_id: 'preview-1',
      target_ptype: 2,
    })
    expect(fetchField).toHaveBeenCalledWith('/api/session/session-1/field/qpf_after')
    expect(fetchField).toHaveBeenCalledWith('/api/session/session-1/field/ptype_after')
    expect(getOperations).toHaveBeenCalledWith('session-1')
    expect(store.previewResult).toBeNull()
    expect(store.previewId).toBeNull()
    expect(store.canUndo).toBe(true)
    expect(store.canRedo).toBe(false)
    expect(store.operations).toEqual([OPERATION])
  })

  it('undoEdit 调用 API、刷新字段并更新 undo/redo', async () => {
    mockFieldFetches()
    vi.mocked(editUndo).mockResolvedValue({
      can_undo: false,
      can_redo: true,
      operation_count: 1,
    })
    vi.mocked(getOperations).mockResolvedValue({
      operations: [{ ...OPERATION, is_undone: 1 }],
    })

    const store = useEditorStore()
    store.sessionId = 'session-1'

    await store.undoEdit()

    expect(editUndo).toHaveBeenCalledWith({ session_id: 'session-1' })
    expect(fetchField).toHaveBeenCalledWith('/api/session/session-1/field/qpf_after')
    expect(fetchField).toHaveBeenCalledWith('/api/session/session-1/field/ptype_after')
    expect(store.canUndo).toBe(false)
    expect(store.canRedo).toBe(true)
  })

  it('redoEdit 调用 API、刷新字段并更新 undo/redo', async () => {
    mockFieldFetches()
    vi.mocked(editRedo).mockResolvedValue({
      can_undo: true,
      can_redo: false,
      operation_count: 1,
    })
    vi.mocked(getOperations).mockResolvedValue({ operations: [OPERATION] })

    const store = useEditorStore()
    store.sessionId = 'session-1'

    await store.redoEdit()

    expect(editRedo).toHaveBeenCalledWith({ session_id: 'session-1' })
    expect(fetchField).toHaveBeenCalledWith('/api/session/session-1/field/qpf_after')
    expect(fetchField).toHaveBeenCalledWith('/api/session/session-1/field/ptype_after')
    expect(store.canUndo).toBe(true)
    expect(store.canRedo).toBe(false)
  })

  it('fetchOperations 填充 operations 列表', async () => {
    vi.mocked(getOperations).mockResolvedValue({ operations: [OPERATION] })

    const store = useEditorStore()
    store.sessionId = 'session-1'

    await store.fetchOperations()

    expect(getOperations).toHaveBeenCalledWith('session-1')
    expect(store.operations).toEqual([OPERATION])
    expect(store.canUndo).toBe(true)
    expect(store.canRedo).toBe(false)
  })

  it('clearPreview 重置 previewResult', () => {
    const store = useEditorStore()
    store.previewResult = PREVIEW_RESPONSE
    store.previewId = PREVIEW_RESPONSE.preview_id

    store.clearPreview()

    expect(store.previewResult).toBeNull()
    expect(store.previewId).toBeNull()
  })
})
