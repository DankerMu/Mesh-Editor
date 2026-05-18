import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { MessagePlugin } from 'tdesign-vue-next'
import { useEditorStore } from '@/stores/editorStore'
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
  loadSession: vi.fn(),
  fetchField: vi.fn(),
}))

vi.mock('@/api/edit', () => ({
  editPreview: vi.fn(),
  editApply: vi.fn(),
  editUndo: vi.fn(),
  editRedo: vi.fn(),
  getOperations: vi.fn().mockResolvedValue({ operations: [] }),
}))

vi.mock('@/api/version', () => ({
  saveVersion: vi.fn(),
  submitVersion: vi.fn(),
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

const MASK_GEOM = {
  type: 'polygon' as const,
  coordinates: [[100, 30], [101, 30], [101, 31], [100, 31], [100, 30]] as [number, number][],
}

function mountEditor(opts?: { withMask?: boolean }) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const store = useEditorStore()
  store.sessionId = 's-1'
  if (opts?.withMask) {
    store.currentMaskGeometry = MASK_GEOM
  }
  return { wrapper: mount(EditorView, { global: { plugins: [pinia] } }), store }
}

describe('QPF panel', () => {
  beforeEach(() => vi.clearAllMocks())

  it('shows hint when no mask selected', () => {
    const { wrapper } = mountEditor()
    const panel = wrapper.find('[data-test="qpf-panel"]')
    expect(panel.text()).toContain('请先选择编辑区域')
  })

  it('renders controls with active mask', async () => {
    const { wrapper } = mountEditor({ withMask: true })
    await wrapper.vm.$nextTick()
    const panel = wrapper.find('[data-test="qpf-panel"]')
    expect(panel.find('[data-test="qpf-operation"]').exists()).toBe(true)
    expect(panel.find('[data-test="preview-button"]').exists()).toBe(true)
  })

  it('operation dropdown shows 6 options', async () => {
    const { wrapper } = mountEditor({ withMask: true })
    await wrapper.vm.$nextTick()
    const select = wrapper.find('[data-test="qpf-operation"]')
    const options = select.findAll('option').filter((o) => o.attributes('value') !== '')
    expect(options).toHaveLength(6)
  })

  it('value input accepts negative numbers', async () => {
    const { wrapper } = mountEditor({ withMask: true })
    await wrapper.vm.$nextTick()
    // Select an operation that shows value input
    const select = wrapper.find('[data-test="qpf-operation"]')
    await select.setValue('increase')
    await wrapper.vm.$nextTick()

    const input = wrapper.find('[data-test="qpf-value"]')
    expect(input.exists()).toBe(true)
    await input.setValue(-5)
    expect(Number((input.element as HTMLInputElement).value)).toBe(-5)
  })

  it('clear operation hides value input', async () => {
    const { wrapper } = mountEditor({ withMask: true })
    await wrapper.vm.$nextTick()
    const select = wrapper.find('[data-test="qpf-operation"]')
    await select.setValue('clear')
    await wrapper.vm.$nextTick()

    expect(wrapper.find('[data-test="qpf-value"]').exists()).toBe(false)
  })

  it('screen_clear operation hides value input', async () => {
    const { wrapper } = mountEditor({ withMask: true })
    await wrapper.vm.$nextTick()
    const select = wrapper.find('[data-test="qpf-operation"]')
    await select.setValue('screen_clear')
    await wrapper.vm.$nextTick()

    expect(wrapper.find('[data-test="qpf-value"]').exists()).toBe(false)
  })

  it('preview button calls requestPreview with correct params for set_value', async () => {
    const { wrapper, store } = mountEditor({ withMask: true })
    const spy = vi.spyOn(store, 'requestPreview').mockResolvedValue({
      preview_id: 'p-1',
      affected_grid_count: 10,
      affected_area_km2: 25,
      before_stats: {},
      after_stats: {},
      op_ptype_transition: null,
      new_precip_needs_ptype: false,
      new_precip_count: 0,
      warnings: [],
    })
    await wrapper.vm.$nextTick()

    const select = wrapper.find('[data-test="qpf-operation"]')
    await select.setValue('set_value')
    await wrapper.vm.$nextTick()

    const btn = wrapper.find('[data-test="preview-button"]')
    await btn.trigger('click')

    expect(spy).toHaveBeenCalledWith(
      'polygon',
      'qpf',
      'set_value',
      expect.objectContaining({ type: 'polygon' }),
      { value: 0 },
    )
  })

  it('sends delta_mm param for increase operation', async () => {
    const { wrapper, store } = mountEditor({ withMask: true })
    const spy = vi.spyOn(store, 'requestPreview').mockResolvedValue({
      preview_id: 'p-1',
      affected_grid_count: 10,
      affected_area_km2: 25,
      before_stats: {},
      after_stats: {},
      op_ptype_transition: null,
      new_precip_needs_ptype: false,
      new_precip_count: 0,
      warnings: [],
    })
    await wrapper.vm.$nextTick()

    const select = wrapper.find('[data-test="qpf-operation"]')
    await select.setValue('increase')
    await wrapper.vm.$nextTick()

    const btn = wrapper.find('[data-test="preview-button"]')
    await btn.trigger('click')

    expect(spy).toHaveBeenCalledWith(
      'polygon',
      'qpf',
      'increase',
      expect.objectContaining({ type: 'polygon' }),
      { delta_mm: 0 },
    )
  })

  it('sends factor param for multiply operation', async () => {
    const { wrapper, store } = mountEditor({ withMask: true })
    const spy = vi.spyOn(store, 'requestPreview').mockResolvedValue({
      preview_id: 'p-1',
      affected_grid_count: 10,
      affected_area_km2: 25,
      before_stats: {},
      after_stats: {},
      op_ptype_transition: null,
      new_precip_needs_ptype: false,
      new_precip_count: 0,
      warnings: [],
    })
    await wrapper.vm.$nextTick()

    const select = wrapper.find('[data-test="qpf-operation"]')
    await select.setValue('multiply')
    await wrapper.vm.$nextTick()

    const btn = wrapper.find('[data-test="preview-button"]')
    await btn.trigger('click')

    expect(spy).toHaveBeenCalledWith(
      'polygon',
      'qpf',
      'multiply',
      expect.objectContaining({ type: 'polygon' }),
      { factor: 0 },
    )
  })

  it('uses mask geometry type as tool parameter', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const store = useEditorStore()
    store.sessionId = 's-1'
    store.currentMaskGeometry = {
      type: 'line_buffer' as const,
      coordinates: [[100, 30], [101, 31]] as [number, number][],
      width_grid: 3,
    }
    const spy = vi.spyOn(store, 'requestPreview').mockResolvedValue({
      preview_id: 'p-1',
      affected_grid_count: 10,
      affected_area_km2: 25,
      before_stats: {},
      after_stats: {},
      op_ptype_transition: null,
      new_precip_needs_ptype: false,
      new_precip_count: 0,
      warnings: [],
    })
    const wrapper = mount(EditorView, { global: { plugins: [pinia] } })
    await wrapper.vm.$nextTick()

    const select = wrapper.find('[data-test="qpf-operation"]')
    await select.setValue('set_value')
    await wrapper.vm.$nextTick()

    const btn = wrapper.find('[data-test="preview-button"]')
    await btn.trigger('click')

    expect(spy).toHaveBeenCalledWith(
      'line_buffer',
      'qpf',
      'set_value',
      expect.objectContaining({ type: 'line_buffer' }),
      { value: 0 },
    )
  })

  it('preview button shows loading state', async () => {
    const { wrapper, store } = mountEditor({ withMask: true })
    store.previewLoading = true
    await wrapper.vm.$nextTick()

    // Select operation first
    const select = wrapper.find('[data-test="qpf-operation"]')
    await select.setValue('set_value')
    await wrapper.vm.$nextTick()

    const btn = wrapper.find('[data-test="preview-button"]')
    expect(btn.attributes('disabled')).toBeDefined()
  })

  it('preview error shows toast', async () => {
    const { wrapper, store } = mountEditor({ withMask: true })
    vi.spyOn(store, 'requestPreview').mockRejectedValue(new Error('fail'))
    store.previewError = '预览失败'
    await wrapper.vm.$nextTick()

    const select = wrapper.find('[data-test="qpf-operation"]')
    await select.setValue('set_value')
    await wrapper.vm.$nextTick()

    const btn = wrapper.find('[data-test="preview-button"]')
    await btn.trigger('click')
    // Wait for async handler
    await wrapper.vm.$nextTick()
    await new Promise((r) => setTimeout(r, 0))

    expect(MessagePlugin.error).toHaveBeenCalled()
  })
})

describe('Ptype panel', () => {
  beforeEach(() => vi.clearAllMocks())

  it('shows hint when no mask selected', () => {
    const { wrapper } = mountEditor()
    const panel = wrapper.find('[data-test="ptype-panel"]')
    expect(panel.text()).toContain('请先选择编辑区域')
  })

  it('renders radio group with mask', async () => {
    const { wrapper } = mountEditor({ withMask: true })
    await wrapper.vm.$nextTick()
    const panel = wrapper.find('[data-test="ptype-panel"]')
    expect(panel.find('[data-test="ptype-radio"]').exists()).toBe(true)
    // 3 radio buttons: rain, snow, sleet
    const buttons = panel.find('[data-test="ptype-radio"]').findAll('button')
    expect(buttons).toHaveLength(3)
  })

  it('preview button disabled without ptype selection', async () => {
    const { wrapper } = mountEditor({ withMask: true })
    await wrapper.vm.$nextTick()
    const btn = wrapper.find('[data-test="ptype-preview-button"]')
    expect(btn.attributes('disabled')).toBeDefined()
  })

  it('selecting ptype enables preview button', async () => {
    const { wrapper } = mountEditor({ withMask: true })
    await wrapper.vm.$nextTick()

    // Click the first radio button (rain=1)
    const radioButtons = wrapper.find('[data-test="ptype-radio"]').findAll('button')
    await radioButtons[0].trigger('click')
    await wrapper.vm.$nextTick()

    const btn = wrapper.find('[data-test="ptype-preview-button"]')
    expect(btn.attributes('disabled')).toBeUndefined()
  })

  it('ptype preview calls requestPreview with set_ptype and target_ptype', async () => {
    const { wrapper, store } = mountEditor({ withMask: true })
    const spy = vi.spyOn(store, 'requestPreview').mockResolvedValue({
      preview_id: 'p-2',
      affected_grid_count: 5,
      affected_area_km2: 12.5,
      before_stats: {},
      after_stats: {},
      op_ptype_transition: null,
      new_precip_needs_ptype: false,
      new_precip_count: 0,
      warnings: [],
    })
    await wrapper.vm.$nextTick()

    // Select snow (value=2)
    const radioButtons = wrapper.find('[data-test="ptype-radio"]').findAll('button')
    await radioButtons[1].trigger('click')
    await wrapper.vm.$nextTick()

    const btn = wrapper.find('[data-test="ptype-preview-button"]')
    await btn.trigger('click')

    expect(spy).toHaveBeenCalledWith(
      'polygon',
      'ptype',
      'set_ptype',
      expect.objectContaining({ type: 'polygon' }),
      { target_ptype: 2 },
    )
  })
})
