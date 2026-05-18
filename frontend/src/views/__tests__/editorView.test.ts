import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useEditorStore } from '@/stores/editorStore'
import { editUndo, editRedo } from '@/api/edit'
import EditorView from '@/views/EditorView.vue'

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue-router')>()
  return {
    ...actual,
    useRoute: () => ({
      path: '/editor/2026051800_ACC06_000_006',
      params: { windowId: '2026051800_ACC06_000_006' },
    }),
    useRouter: () => ({
      push: vi.fn(),
      currentRoute: { value: { path: '/editor/2026051800_ACC06_000_006' } },
    }),
  }
})

vi.mock('tdesign-vue-next', () => ({
  MessagePlugin: { error: vi.fn(), success: vi.fn() },
}))

vi.mock('tdesign-icons-vue-next', () => new Proxy({}, { get: () => ({ template: '<i />' }) }))

vi.mock('@/api/sessions', () => ({
  startSession: vi.fn().mockResolvedValue({
    session_id: 's-1',
    window_id: '2026051800_ACC06_000_006',
    base_version_id: 'v-base',
  }),
  loadSession: vi.fn().mockResolvedValue({
    session_id: 's-1',
    window_id: '2026051800_ACC06_000_006',
    base_version_id: 'v-base',
    can_undo: false,
    can_redo: false,
    field_urls: {
      qpf_before: '/f/qpf_before',
      ptype_before: '/f/ptype_before',
      qpf_after: '/f/qpf_after',
      ptype_after: '/f/ptype_after',
      touched_mask: '/f/touched_mask',
      changed_mask: '/f/changed_mask',
      invalid_mask: '/f/invalid_mask',
    },
  }),
  fetchField: vi.fn().mockImplementation((url: string) => {
    const isFloat = url.includes('qpf_')
    const size = isFloat ? 501 * 821 * 4 : 501 * 821
    return Promise.resolve({ buffer: new ArrayBuffer(size) })
  }),
}))

vi.mock('@/api/edit', () => ({
  editPreview: vi.fn(),
  editApply: vi.fn(),
  editUndo: vi.fn(),
  editRedo: vi.fn(),
  getOperations: vi.fn().mockResolvedValue({ operations: [] }),
}))

vi.mock('@/api/version', () => ({
  saveVersion: vi.fn().mockResolvedValue({ version_id: 'v-1', session_id: 's-1' }),
  submitVersion: vi.fn().mockResolvedValue({ version_id: 'v-1', status: 'submitted' }),
  getVersions: vi.fn().mockResolvedValue([]),
  getVersionDetail: vi.fn(),
  reviewVersion: vi.fn(),
  releaseVersion: vi.fn(),
}))

vi.mock('@/components/map/BaseMap.vue', () => ({
  default: { template: '<div data-test="base-map-stub" />' },
}))
vi.mock('@/components/map/DrawTools.vue', () => ({
  default: { template: '<div data-test="draw-tools-stub" />' },
}))
vi.mock('@/components/map/GridTooltip.vue', () => ({
  default: { template: '<div data-test="grid-tooltip-stub" />' },
}))
vi.mock('@/components/map/MaskOverlayLayer', () => ({
  MaskOverlayLayer: vi.fn(),
}))
vi.mock('@/components/map/PrecipPhaseGridLayer', () => ({
  PrecipPhaseGridLayer: vi.fn(),
  getGridDataValue: vi.fn(),
}))
vi.mock('@/components/map/SelectionOverlay', () => ({
  SelectionOverlay: vi.fn(),
}))
vi.mock('@/components/WindowSelector.vue', () => ({
  default: { template: '<div data-test="window-selector-stub" />' },
}))
vi.mock('@/components/editor/OperationHistory.vue', () => ({
  default: { template: '<div data-test="operation-history-stub" />' },
}))
vi.mock('@/components/editor/PreviewStatsPanel.vue', () => ({
  default: { template: '<div data-test="preview-stats-stub" />' },
}))

function mountEditor() {
  const pinia = createPinia()
  setActivePinia(pinia)
  return mount(EditorView, {
    global: { plugins: [pinia] },
  })
}

describe('EditorView – layout and structure', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders top bar at 56px', () => {
    const wrapper = mountEditor()
    const topbar = wrapper.find('[data-test="editor-topbar"]')
    expect(topbar.exists()).toBe(true)
    expect(topbar.attributes('style')).toContain('56px')
  })

  it('renders bottom status bar at 36px', () => {
    const wrapper = mountEditor()
    const bar = wrapper.find('[data-test="bottom-statusbar"]')
    expect(bar.exists()).toBe(true)
    expect(bar.attributes('style')).toContain('36px')
  })

  it('renders left sidebar at 260px', () => {
    const wrapper = mountEditor()
    const sidebar = wrapper.find('[data-test="left-sidebar"]')
    expect(sidebar.exists()).toBe(true)
    expect(sidebar.attributes('style')).toContain('260px')
  })

  it('renders right sidebar at 340px', () => {
    const wrapper = mountEditor()
    const sidebar = wrapper.find('[data-test="right-sidebar"]')
    expect(sidebar.exists()).toBe(true)
    expect(sidebar.attributes('style')).toContain('340px')
  })

  it('left sidebar contains 7 view mode switches', () => {
    const wrapper = mountEditor()
    const radios = wrapper.findAll('input[name="view-mode"]')
    expect(radios).toHaveLength(7)
  })

  it('right sidebar contains 4 tab panels', () => {
    const wrapper = mountEditor()
    expect(wrapper.find('[data-test="mask-panel"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="qpf-panel"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="ptype-panel"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="history-panel"]').exists()).toBe(true)
  })

  it('context bar shows metadata fields when not loading', async () => {
    const wrapper = mountEditor()
    const store = useEditorStore()
    store.loadingSession = false
    await wrapper.vm.$nextTick()

    const topbar = wrapper.find('[data-test="editor-topbar"]')
    const text = topbar.text()
    expect(text).toContain('起报')
    expect(text).toContain('累计')
    expect(text).toContain('时效')
  })
})

describe('EditorView – top bar buttons', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('save button calls editorStore.saveVersion', async () => {
    const wrapper = mountEditor()
    const store = useEditorStore()
    store.sessionId = 's-1'
    store.dirty = true
    store.saveLoading = false
    const spy = vi.spyOn(store, 'saveVersion').mockResolvedValue({ version_id: 'v-1', session_id: 's-1', before_image: null, after_image: null, review_image: null })
    await wrapper.vm.$nextTick()

    const btn = wrapper.find('[data-test="save-button"]')
    expect(btn.attributes('disabled')).toBeUndefined()
    await btn.trigger('click')
    expect(spy).toHaveBeenCalled()
  })

  it('submit button disabled without currentVersionId', () => {
    const wrapper = mountEditor()
    const store = useEditorStore()
    store.currentVersionId = null
    const btn = wrapper.find('[data-test="submit-button"]')
    expect(btn.attributes('disabled')).toBeDefined()
  })

  it('submit button disabled when dirty even with currentVersionId', async () => {
    const wrapper = mountEditor()
    const store = useEditorStore()
    await flushPromises()
    store.currentVersionId = 'v-1'
    store.dirty = true
    await flushPromises()

    const btn = wrapper.find('[data-test="submit-button"]')
    expect(btn.attributes('disabled')).toBeDefined()
  })

  it('submit button triggers confirmation dialog', async () => {
    const wrapper = mountEditor()
    const store = useEditorStore()
    // Wait for startSession to complete (it resets currentVersionId)
    await flushPromises()
    // Now set currentVersionId after session init
    store.currentVersionId = 'v-1'
    await flushPromises()

    const btn = wrapper.find('[data-test="submit-button"]')
    expect(btn.exists()).toBe(true)
    await btn.trigger('click')
    await flushPromises()

    const html = wrapper.html()
    expect(html).toContain('确认提交该版本进行审核')
  })

  it('undo button disabled when canUndo=false', () => {
    const wrapper = mountEditor()
    const store = useEditorStore()
    store.canUndo = false
    const btn = wrapper.find('[data-test="undo-button"]')
    expect(btn.attributes('disabled')).toBeDefined()
  })

  it('redo button disabled when canRedo=false', () => {
    const wrapper = mountEditor()
    const store = useEditorStore()
    store.canRedo = false
    const btn = wrapper.find('[data-test="redo-button"]')
    expect(btn.attributes('disabled')).toBeDefined()
  })

  it('undo button enabled when canUndo=true', async () => {
    const wrapper = mountEditor()
    const store = useEditorStore()
    store.sessionId = 's-1'
    store.canUndo = true
    store.applyLoading = false
    await wrapper.vm.$nextTick()

    const btn = wrapper.find('[data-test="undo-button"]')
    expect(btn.attributes('disabled')).toBeUndefined()
  })

  it('redo button enabled when canRedo=true', async () => {
    const wrapper = mountEditor()
    const store = useEditorStore()
    store.sessionId = 's-1'
    store.canRedo = true
    store.applyLoading = false
    await wrapper.vm.$nextTick()

    const btn = wrapper.find('[data-test="redo-button"]')
    expect(btn.attributes('disabled')).toBeUndefined()
  })

  it('undo button disabled during previewLoading', async () => {
    const wrapper = mountEditor()
    const store = useEditorStore()
    store.sessionId = 's-1'
    store.canUndo = true
    store.applyLoading = false
    store.previewLoading = true
    await wrapper.vm.$nextTick()

    const btn = wrapper.find('[data-test="undo-button"]')
    expect(btn.attributes('disabled')).toBeDefined()
  })

  it('redo button disabled during previewLoading', async () => {
    const wrapper = mountEditor()
    const store = useEditorStore()
    store.sessionId = 's-1'
    store.canRedo = true
    store.applyLoading = false
    store.previewLoading = true
    await wrapper.vm.$nextTick()

    const btn = wrapper.find('[data-test="redo-button"]')
    expect(btn.attributes('disabled')).toBeDefined()
  })
})

describe('EditorView – undo/redo marks dirty', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('undo marks session as dirty after save', async () => {
    const wrapper = mountEditor()
    const store = useEditorStore()
    await flushPromises()

    // Simulate saved state: dirty=false, currentVersionId set
    store.sessionId = 's-1'
    store.currentVersionId = 'v-1'
    store.dirty = false
    store.canUndo = true
    await wrapper.vm.$nextTick()

    // Mock editUndo to return success
    const mockedEditUndo = vi.mocked(editUndo)
    mockedEditUndo.mockResolvedValueOnce({
      can_undo: false,
      can_redo: true,
      operation_count: 0,
    })

    // Trigger undo via store (same as button click)
    await store.undoEdit()
    await flushPromises()

    expect(store.dirty).toBe(true)

    // Submit button should be disabled because dirty=true
    await wrapper.vm.$nextTick()
    const submitBtn = wrapper.find('[data-test="submit-button"]')
    expect(submitBtn.attributes('disabled')).toBeDefined()
  })

  it('redo marks session as dirty after save', async () => {
    const wrapper = mountEditor()
    const store = useEditorStore()
    await flushPromises()

    store.sessionId = 's-1'
    store.currentVersionId = 'v-1'
    store.dirty = false
    store.canRedo = true
    await wrapper.vm.$nextTick()

    const mockedEditRedo = vi.mocked(editRedo)
    mockedEditRedo.mockResolvedValueOnce({
      can_undo: true,
      can_redo: false,
      operation_count: 1,
    })

    await store.redoEdit()
    await flushPromises()

    expect(store.dirty).toBe(true)

    await wrapper.vm.$nextTick()
    const submitBtn = wrapper.find('[data-test="submit-button"]')
    expect(submitBtn.attributes('disabled')).toBeDefined()
  })
})
