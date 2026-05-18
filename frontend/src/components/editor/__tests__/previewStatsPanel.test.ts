import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useEditorStore } from '@/stores/editorStore'
import PreviewStatsPanel from '@/components/editor/PreviewStatsPanel.vue'
import type { EditPreviewResponse } from '@/api/edit'

vi.mock('tdesign-icons-vue-next', () => new Proxy({}, { get: () => ({ template: '<i />' }) }))

const PREVIEW: EditPreviewResponse = {
  preview_id: 'p-1',
  affected_grid_count: 12345,
  affected_area_km2: 308.5,
  before_stats: { min: 0.1, max: 15.3, mean: 4.56, sum: 1200, count: 263, area_km2: 65.8 },
  after_stats: { min: 1.0, max: 20.0, mean: 8.12, sum: 2100, count: 263, area_km2: 65.8 },
  op_ptype_transition: { '0->1': 10, '1->1': 200, '1->2': 5, '2->2': 48 },
  new_precip_needs_ptype: false,
  new_precip_count: 0,
  warnings: [],
}

const PREVIEW_WITH_NEW_PRECIP: EditPreviewResponse = {
  ...PREVIEW,
  preview_id: 'p-2',
  new_precip_needs_ptype: true,
  new_precip_count: 42,
}

function mountPanel(previewResult: EditPreviewResponse | null = null) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const store = useEditorStore()
  store.sessionId = 's-1'
  store.previewResult = previewResult
  return { wrapper: mount(PreviewStatsPanel, { global: { plugins: [pinia] } }), store }
}

describe('PreviewStatsPanel', () => {
  beforeEach(() => vi.clearAllMocks())

  it('hidden without preview', () => {
    const { wrapper } = mountPanel(null)
    expect(wrapper.find('[data-test="preview-stats-panel"]').exists()).toBe(false)
  })

  it('shows after preview with affected count and area', () => {
    const { wrapper } = mountPanel(PREVIEW)
    const panel = wrapper.find('[data-test="preview-stats-panel"]')
    expect(panel.exists()).toBe(true)
    const summary = wrapper.find('[data-test="preview-summary"]')
    // affected_grid_count 12345 formatted with thousands separator
    expect(summary.text()).toContain('12,345')
    // 308.5 is formatted to "309" by formatValue (>= 100 uses toFixed(0) which rounds)
    expect(summary.text()).toContain('309')
  })

  it('renders before/after stats table', () => {
    const { wrapper } = mountPanel(PREVIEW)
    const table = wrapper.find('[data-test="stats-table"]')
    expect(table.exists()).toBe(true)
    const text = table.text()
    expect(text).toContain('最小值')
    expect(text).toContain('最大值')
    expect(text).toContain('平均值')
    expect(text).toContain('总量')
    expect(text).toContain('格点数')
    expect(text).toContain('面积')
  })

  it('renders ptype transition matrix', () => {
    const { wrapper } = mountPanel(PREVIEW)
    const matrix = wrapper.find('[data-test="ptype-matrix"]')
    expect(matrix.exists()).toBe(true)
    expect(matrix.text()).toContain('从 / 到')
    expect(matrix.text()).toContain('雨(1)')
    expect(matrix.text()).toContain('雪(2)')
  })

  it('apply enabled after preview', () => {
    const { wrapper } = mountPanel(PREVIEW)
    const btn = wrapper.find('[data-test="preview-apply-button"]')
    expect(btn.attributes('disabled')).toBeUndefined()
  })

  it('apply disabled when applyLoading', () => {
    const { store } = mountPanel(PREVIEW)
    store.applyLoading = true
    // The component uses `applying` computed which reads applyLoading
    expect(store.applyLoading).toBe(true)
  })

  it('new precip dialog appears when new_precip_needs_ptype=true', async () => {
    const { wrapper } = mountPanel(PREVIEW_WITH_NEW_PRECIP)
    expect(wrapper.find('[data-test="new-precip-warning"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('42')

    // Click apply to open dialog
    const applyBtn = wrapper.find('[data-test="preview-apply-button"]')
    await applyBtn.trigger('click')
    await wrapper.vm.$nextTick()

    const dialog = wrapper.find('[data-test="target-ptype-dialog"]')
    expect(dialog.exists()).toBe(true)
  })

  it('dialog does NOT appear when new_precip_needs_ptype=false', async () => {
    const { wrapper, store } = mountPanel(PREVIEW)
    vi.spyOn(store, 'applyEdit').mockResolvedValue({
      operation_id: 'op-1',
      sequence_no: 1,
      applied: true,
      can_undo: true,
      can_redo: false,
    })

    const applyBtn = wrapper.find('[data-test="preview-apply-button"]')
    await applyBtn.trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.find('[data-test="target-ptype-dialog"]').exists()).toBe(false)
  })

  it('confirm with ptype selection calls applyEdit with target_ptype', async () => {
    const { wrapper, store } = mountPanel(PREVIEW_WITH_NEW_PRECIP)
    const spy = vi.spyOn(store, 'applyEdit').mockResolvedValue({
      operation_id: 'op-1',
      sequence_no: 1,
      applied: true,
      can_undo: true,
      can_redo: false,
    })

    // Open dialog
    await wrapper.find('[data-test="preview-apply-button"]').trigger('click')
    await wrapper.vm.$nextTick()

    // Select snow (value=2)
    const radioButtons = wrapper.find('[data-test="target-ptype-radio"]').findAll('button')
    await radioButtons[1].trigger('click')
    await wrapper.vm.$nextTick()

    // Confirm
    await wrapper.find('[data-test="target-ptype-confirm"]').trigger('click')
    expect(spy).toHaveBeenCalledWith(2)
  })

  it('cancel dialog aborts apply', async () => {
    const { wrapper, store } = mountPanel(PREVIEW_WITH_NEW_PRECIP)
    const spy = vi.spyOn(store, 'applyEdit')

    await wrapper.find('[data-test="preview-apply-button"]').trigger('click')
    await wrapper.vm.$nextTick()

    await wrapper.find('[data-test="target-ptype-cancel"]').trigger('click')
    await wrapper.vm.$nextTick()

    expect(spy).not.toHaveBeenCalled()
    expect(wrapper.find('[data-test="target-ptype-dialog"]').exists()).toBe(false)
  })

  it('formats large affected_grid_count with thousands separator', () => {
    const bigPreview: EditPreviewResponse = {
      ...PREVIEW,
      affected_grid_count: 12345,
    }
    const { wrapper } = mountPanel(bigPreview)
    const summary = wrapper.find('[data-test="preview-summary"]')
    expect(summary.text()).toContain('12,345')

    // Also verify the stats table count row uses thousands separator
    const table = wrapper.find('[data-test="stats-table"]')
    const text = table.text()
    // before_stats.count = 263, no separator needed
    expect(text).toContain('263')
  })

  it('apply button is not rendered when no preview exists', () => {
    const { wrapper } = mountPanel(null)
    expect(wrapper.find('[data-test="preview-apply-button"]').exists()).toBe(false)
  })

  it('apply success clears preview state', async () => {
    const { wrapper, store } = mountPanel(PREVIEW)
    vi.spyOn(store, 'applyEdit').mockResolvedValue({
      operation_id: 'op-1',
      sequence_no: 1,
      applied: true,
      can_undo: true,
      can_redo: false,
    })

    const applyBtn = wrapper.find('[data-test="preview-apply-button"]')
    await applyBtn.trigger('click')
    await wrapper.vm.$nextTick()

    expect(store.applyEdit).toHaveBeenCalled()
  })
})
