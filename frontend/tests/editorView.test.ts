import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import EditorView from '@/views/EditorView.vue'
import { useEditorStore } from '@/stores/editorStore'
import { useWindowStore } from '@/stores/windowStore'
import type { WindowItem } from '@/api/data'

vi.mock('@/components/map/BaseMap.vue', () => ({
  default: {
    emits: ['map-ready', 'grid-hover'],
    template: '<div class="base-map-stub" data-test="base-map"></div>',
  },
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

describe('EditorView', () => {
  beforeEach(() => {
    routeMock.path = '/editor/2026010108_ACC24_024_048'
    routeMock.params = { windowId: '2026010108_ACC24_024_048' }
    routerMock.push.mockReset()
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
    expect(drawButtons).toHaveLength(4)
    expect(drawButtons.every((button) => button.attributes('disabled') !== undefined)).toBe(true)

    expect(wrapper.find('[data-test="qpf-panel"]').text()).toContain('请先选择编辑区域')
    expect(wrapper.find('[data-test="ptype-panel"]').text()).toContain('请先选择编辑区域')
    expect(wrapper.find('[data-test="preview-button"]').attributes('disabled')).toBeDefined()
    expect(wrapper.find('[data-test="apply-button"]').attributes('disabled')).toBeDefined()
    expect(wrapper.find('[data-test="ptype-preview-button"]').attributes('disabled')).toBeDefined()
    expect(wrapper.find('[data-test="ptype-apply-button"]').attributes('disabled')).toBeDefined()
  })

  it('有 session 时启用 DrawTools，但 M2 操作按钮仍禁用', async () => {
    const { wrapper, editorStore } = mountEditor()
    editorStore.sessionId = 'session-1'
    await flushPromises()

    const drawButtons = wrapper.findAll('.draw-tools__button')
    expect(drawButtons.every((button) => button.attributes('disabled') === undefined)).toBe(true)
    expect(wrapper.find('[data-test="preview-button"]').attributes('disabled')).toBeDefined()
    expect(wrapper.find('[data-test="apply-button"]').attributes('disabled')).toBeDefined()
  })

  it('有 mask 时降水/相态 panel 显示 M3 placeholder', async () => {
    const { wrapper, editorStore } = mountEditor()
    editorStore.currentMaskGeometry = {
      type: 'polygon',
      coordinates: [
        [100, 30],
        [101, 30],
        [101, 31],
      ],
    }
    await flushPromises()

    expect(wrapper.find('[data-test="qpf-panel"]').text()).toContain('降水调整将在 M3 开放')
    expect(wrapper.find('[data-test="ptype-panel"]').text()).toContain('相态调整将在 M3 开放')
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
})
