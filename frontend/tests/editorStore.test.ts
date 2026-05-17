import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { fetchField, loadSession, startSession } from '@/api/sessions'
import { GRID_COLS, GRID_ROWS } from '@/constants/precipColors'
import { useEditorStore } from '@/stores/editorStore'
import type { MaskGeometry } from '@/types/editor'

vi.mock('@/api/sessions', () => ({
  startSession: vi.fn(),
  loadSession: vi.fn(),
  fetchField: vi.fn(),
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

  it('loadFields 成功后填充 7 个 TypedArray', async () => {
    mockLoadSession()
    mockFieldFetches()

    const store = useEditorStore()
    await store.loadFields('session-1')

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

  it('loadFields byte-length 不匹配时写入错误并清空数组', async () => {
    mockLoadSession()
    vi.mocked(fetchField).mockResolvedValue({ buffer: new ArrayBuffer(4), headers: {} })

    const store = useEditorStore()
    await store.loadFields('session-1')

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
    await store.loadFields('session-1')
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
})
