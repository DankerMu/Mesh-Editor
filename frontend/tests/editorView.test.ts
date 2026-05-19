import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import EditorView from '@/views/EditorView.vue'
import { useEditorStore } from '@/stores/editorStore'
import { useWindowStore } from '@/stores/windowStore'
import type { WindowItem } from '@/api/data'

const layerMocks = vi.hoisted(() => {
  const precipInstances: Array<{
    layer: { id: string }
    updateData: ReturnType<typeof vi.fn>
    clearData: ReturnType<typeof vi.fn>
    dispose: ReturnType<typeof vi.fn>
    getLayer: ReturnType<typeof vi.fn>
  }> = []
  const maskInstances: Array<{
    layer: { id: string }
    updateData: ReturnType<typeof vi.fn>
    clearData: ReturnType<typeof vi.fn>
    dispose: ReturnType<typeof vi.fn>
    getLayer: ReturnType<typeof vi.fn>
  }> = []
  const selectionInstances: Array<{
    updateGeometry: ReturnType<typeof vi.fn>
    dispose: ReturnType<typeof vi.fn>
  }> = []

  return {
    precipInstances,
    maskInstances,
    selectionInstances,
  }
})

vi.mock('@/components/map/BaseMap.vue', () => ({
  default: {
    name: 'BaseMap',
    emits: ['map-ready', 'grid-hover'],
    template: '<div class="base-map-stub" data-test="base-map"></div>',
  },
}))

vi.mock('@/components/map/PrecipPhaseGridLayer', () => ({
  getGridDataValue: vi.fn(() => null),
  PrecipPhaseGridLayer: vi.fn(function MockPrecipPhaseGridLayer(this: {
    layer: { id: string }
    updateData: ReturnType<typeof vi.fn>
    clearData: ReturnType<typeof vi.fn>
    dispose: ReturnType<typeof vi.fn>
    getLayer: ReturnType<typeof vi.fn>
  }) {
    this.layer = { id: `precip-${layerMocks.precipInstances.length}` }
    this.updateData = vi.fn()
    this.clearData = vi.fn()
    this.dispose = vi.fn()
    this.getLayer = vi.fn(() => this.layer)
    layerMocks.precipInstances.push(this)
  }),
}))

vi.mock('@/components/map/MaskOverlayLayer', () => ({
  MaskOverlayLayer: vi.fn(function MockMaskOverlayLayer(this: {
    layer: { id: string }
    updateData: ReturnType<typeof vi.fn>
    clearData: ReturnType<typeof vi.fn>
    dispose: ReturnType<typeof vi.fn>
    getLayer: ReturnType<typeof vi.fn>
  }) {
    this.layer = { id: `mask-${layerMocks.maskInstances.length}` }
    this.updateData = vi.fn()
    this.clearData = vi.fn()
    this.dispose = vi.fn()
    this.getLayer = vi.fn(() => this.layer)
    layerMocks.maskInstances.push(this)
  }),
}))

vi.mock('@/components/map/SelectionOverlay', () => ({
  SelectionOverlay: vi.fn(function MockSelectionOverlay(this: {
    updateGeometry: ReturnType<typeof vi.fn>
    dispose: ReturnType<typeof vi.fn>
  }) {
    this.updateGeometry = vi.fn()
    this.dispose = vi.fn()
    layerMocks.selectionInstances.push(this)
  }),
}))

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue-router')>()

  return {
    ...actual,
    useRoute: () => routeMock,
    useRouter: () => routerMock,
  }
})

const routeMock = {
  path: '/editor/2026010108_ACC24_024_048',
  params: {
    windowId: '2026010108_ACC24_024_048',
  },
}

const routerMock = {
  push: vi.fn(),
}

function makeWindow(overrides: Partial<WindowItem> = {}): WindowItem {
  return {
    window_id: '2026010108_ACC24_024_048',
    accum_hours: 24,
    start_lead: 24,
    end_lead: 48,
    status: 'available',
    qc_status: 'pass',
    negative_count: 0,
    negative_min_value: null,
    negative_abs_max: null,
    missing_count: 0,
    ptype_missing_leads: [],
    qpf_before_path: 'qpf.npz',
    ptype_before_path: 'ptype.npz',
    data_ready_at: '2026-01-01T08:01:00Z',
    updated_at: '2026-01-01T08:01:00Z',
    ...overrides,
  }
}

function mountEditor() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const editorStore = useEditorStore()
  vi.spyOn(editorStore, 'startSession').mockResolvedValue(undefined)
  const windowStore = useWindowStore()
  windowStore.windows = [makeWindow()]

  const wrapper = mount(EditorView, {
    global: {
      plugins: [pinia],
    },
  })

  return { wrapper, editorStore, windowStore }
}

function makeMapMock() {
  return {
    addLayer: vi.fn(),
    removeLayer: vi.fn(),
    getViewport: vi.fn(() => document.createElement('div')),
  }
}

async function mountEditorWithMap() {
  const mounted = mountEditor()
  await flushPromises()
  const map = makeMapMock()
  mounted.wrapper.findComponent({ name: 'BaseMap' }).vm.$emit('map-ready', map)
  await flushPromises()

  return { ...mounted, map }
}

describe('EditorView', () => {
  beforeEach(() => {
    routeMock.path = '/editor/2026010108_ACC24_024_048'
    routeMock.params = { windowId: '2026010108_ACC24_024_048' }
    routerMock.push.mockReset()
    layerMocks.precipInstances.length = 0
    layerMocks.maskInstances.length = 0
    layerMocks.selectionInstances.length = 0
  })

  it('渲染顶部窗口信息和 M2 禁用按钮', async () => {
    const { wrapper } = mountEditor()
    await flushPromises()

    expect(wrapper.find('[data-test="editor-topbar"]').attributes('style')).toContain('height: 56px')
    expect(wrapper.text()).toContain('起报: 2026-01-01 16')
    expect(wrapper.text()).toContain('累计: 24h')
    expect(wrapper.text()).toContain('时效: +024h ~ +048h')
    expect(wrapper.text()).toContain('可用')

    for (const testId of ['undo-button', 'redo-button', 'save-button', 'submit-button']) {
      expect(wrapper.find(`[data-test="${testId}"]`).attributes('disabled')).toBeDefined()
    }
  })

  it('loadingSession 时顶部显示加载提示', async () => {
    const { wrapper, editorStore } = mountEditor()
    editorStore.loadingSession = true
    await flushPromises()

    expect(wrapper.text()).toContain('正在加载会话...')
  })

  it('DrawTools 无 session 时禁用，降水/相态无 mask 时禁用', async () => {
    const { wrapper } = mountEditor()
    await flushPromises()

    const drawButtons = wrapper.findAll('.draw-tools__button')
    expect(drawButtons).toHaveLength(5)
    expect(drawButtons.every((button) => button.attributes('disabled') !== undefined)).toBe(true)

    expect(wrapper.find('[data-test="qpf-panel"]').text()).toContain('请先选择编辑区域')
    expect(wrapper.find('[data-test="ptype-panel"]').text()).toContain('请先选择编辑区域')
    // When panels are disabled (no mask), preview/apply buttons are not rendered
    // The hint text "请先选择编辑区域" is shown instead
  })

  it('有 session 时启用 DrawTools，但无 mask 时面板显示提示', async () => {
    const { wrapper, editorStore } = mountEditor()
    editorStore.sessionId = 'session-1'
    await flushPromises()

    const drawButtons = wrapper.findAll('.draw-tools__button')
    expect(drawButtons.every((button) => button.attributes('disabled') === undefined)).toBe(true)
    // Without mask, panels show hint instead of controls
    expect(wrapper.find('[data-test="qpf-panel"]').text()).toContain('请先选择编辑区域')
  })

  it('有 mask 时降水/相态 panel 显示编辑控件', async () => {
    const { wrapper, editorStore } = mountEditor()
    editorStore.sessionId = 'session-1'
    editorStore.currentMaskGeometry = {
      type: 'polygon',
      coordinates: [
        [100, 30],
        [101, 30],
        [101, 31],
      ],
    }
    await flushPromises()

    // QPF panel shows operation dropdown and preview button
    expect(wrapper.find('[data-test="qpf-operation"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="preview-button"]').exists()).toBe(true)
    // Ptype panel shows radio group and preview button
    expect(wrapper.find('[data-test="ptype-radio"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="ptype-preview-button"]').exists()).toBe(true)
  })

  it('五区域尺寸和侧栏折叠行为正确', async () => {
    const { wrapper } = mountEditor()
    await flushPromises()

    expect(wrapper.find('[data-test="editor-topbar"]').attributes('style')).toContain('height: 56px')
    expect(wrapper.find('[data-test="left-sidebar"]').attributes('style')).toContain('width: 260px')
    expect(wrapper.find('[data-test="right-sidebar"]').attributes('style')).toContain('width: 340px')
    expect(wrapper.find('[data-test="bottom-statusbar"]').attributes('style')).toContain('height: 36px')
    expect(wrapper.find('[data-test="editor-center"]').attributes('style')).toContain('min-width: 600px')

    await wrapper.find('[data-test="left-collapse"]').trigger('click')
    await wrapper.find('[data-test="right-collapse"]').trigger('click')

    expect(wrapper.find('[data-test="left-sidebar"]').classes()).toContain('editor-view__sidebar--collapsed')
    expect(wrapper.find('[data-test="right-sidebar"]').classes()).toContain('editor-view__sidebar--collapsed')
  })

  it('切换差异模式写入 selectedViewMode 并显示 M2 提示', async () => {
    const { wrapper, editorStore } = mountEditor()
    await flushPromises()

    await wrapper.find('input[value="delta"]').setValue()

    expect(editorStore.selectedViewMode).toBe('delta')
    expect(wrapper.find('[data-test="mode-hint"]').text()).toContain('无差异')
  })

  it('loadingFields 和 fieldLoadError 显示地图状态', async () => {
    const { wrapper, editorStore } = mountEditor()
    editorStore.loadingFields = true
    editorStore.fieldLoadError = '字段加载失败'
    await flushPromises()

    expect(wrapper.find('[data-test="field-loading-overlay"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="field-error"]').text()).toContain('字段加载失败')
  })

  it('before 和 after 模式使用不同字段数据', async () => {
    const { wrapper, editorStore } = await mountEditorWithMap()
    const precipLayer = layerMocks.precipInstances[0]
    const qpfBefore = new Float32Array([1])
    const ptypeBefore = new Uint8Array([1])
    const qpfAfter = new Float32Array([2])
    const ptypeAfter = new Uint8Array([2])

    editorStore.qpfBefore = qpfBefore
    editorStore.ptypeBefore = ptypeBefore
    editorStore.qpfAfter = qpfAfter
    editorStore.ptypeAfter = ptypeAfter
    await flushPromises()

    expect(precipLayer.updateData).toHaveBeenLastCalledWith(qpfAfter, ptypeAfter)

    await wrapper.find('input[value="before"]').setValue()
    await flushPromises()

    expect(precipLayer.updateData).toHaveBeenLastCalledWith(qpfBefore, ptypeBefore)
  })

  it('delta 和 change 模式隐藏 precipitation raster', async () => {
    const { wrapper, editorStore } = await mountEditorWithMap()
    const precipLayer = layerMocks.precipInstances[0]
    editorStore.qpfAfter = new Float32Array([2])
    editorStore.ptypeAfter = new Uint8Array([2])
    await flushPromises()

    await wrapper.find('input[value="delta"]').setValue()
    await flushPromises()

    expect(precipLayer.clearData).toHaveBeenCalled()
    expect(wrapper.find('[data-test="mode-hint"]').text()).toContain('无差异')

    precipLayer.clearData.mockClear()
    await wrapper.find('input[value="change"]').setValue()
    await flushPromises()

    expect(precipLayer.clearData).toHaveBeenCalled()
    expect(wrapper.find('[data-test="mode-hint"]').text()).toContain('无变化')
  })

  it('字段数组变为 null 时清理 stale layer data', async () => {
    const { editorStore } = await mountEditorWithMap()
    const precipLayer = layerMocks.precipInstances[0]
    const invalidMaskLayer = layerMocks.maskInstances[0]
    const viewMaskLayer = layerMocks.maskInstances[1]

    editorStore.qpfAfter = new Float32Array([2])
    editorStore.ptypeAfter = new Uint8Array([2])
    editorStore.invalidMask = new Uint8Array([1])
    editorStore.touchedMask = new Uint8Array([1])
    editorStore.selectedViewMode = 'review'
    await flushPromises()

    precipLayer.clearData.mockClear()
    invalidMaskLayer.clearData.mockClear()
    viewMaskLayer.clearData.mockClear()

    editorStore.qpfAfter = null
    editorStore.ptypeAfter = null
    editorStore.invalidMask = null
    editorStore.touchedMask = null
    await flushPromises()

    expect(precipLayer.clearData).toHaveBeenCalled()
    expect(invalidMaskLayer.clearData).toHaveBeenCalled()
    expect(viewMaskLayer.clearData).toHaveBeenCalled()
  })
})
