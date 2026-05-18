import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PreviewStatsPanel from '@/components/editor/PreviewStatsPanel.vue'
import { useEditorStore } from '@/stores/editorStore'
import type { EditPreviewResponse } from '@/api/edit'

function makePreview(overrides: Partial<EditPreviewResponse> = {}): EditPreviewResponse {
  return {
    preview_id: 'preview-1',
    affected_grid_count: 12,
    affected_area_km2: 34.5,
    before_stats: {
      min: 0,
      max: 8.2,
      mean: 2.25,
      sum: 27,
      count: 12,
      area_km2: 34.5,
    },
    after_stats: {
      min: 1,
      max: 10.4,
      mean: 3.5,
      sum: 42,
      count: 12,
      area_km2: 34.5,
    },
    op_ptype_transition: {
      '0->1': 3,
      '1->1': 6,
      '1->2': 2,
      '2->2': 1,
    },
    new_precip_needs_ptype: false,
    new_precip_count: 0,
    warnings: [],
    ...overrides,
  }
}

function mountPanel(preview = makePreview()) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const editorStore = useEditorStore()
  editorStore.sessionId = 'session-1'
  editorStore.previewResult = preview
  editorStore.previewId = preview.preview_id

  const wrapper = mount(PreviewStatsPanel, {
    global: {
      plugins: [pinia],
    },
  })

  return { wrapper, editorStore }
}

describe('PreviewStatsPanel', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('渲染 preview stats 对比和相态矩阵', () => {
    const { wrapper } = mountPanel()

    expect(wrapper.find('[data-test="preview-stats-panel"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="preview-summary"]').text()).toContain('12 格点')
    expect(wrapper.find('[data-test="preview-summary"]').text()).toContain('34.5 km²')

    const statsText = wrapper.find('[data-test="stats-table"]').text()
    expect(statsText).toContain('最小值')
    expect(statsText).toContain('编辑前')
    expect(statsText).toContain('编辑后')
    expect(statsText).toContain('8.2 mm')
    expect(statsText).toContain('10.4 mm')
    expect(statsText).toContain('42.0 mm')

    const matrixText = wrapper.find('[data-test="ptype-matrix"]').text()
    expect(matrixText).toContain('无降水(0)')
    expect(matrixText).toContain('雨(1)')
    expect(matrixText).toContain('雪(2)')
    expect(matrixText).toContain('雨夹雪(3)')
    expect(matrixText).toContain('3')
    expect(matrixText).toContain('2')
  })

  it('new_precip_needs_ptype 为 true 时显示警告', () => {
    const { wrapper } = mountPanel(
      makePreview({
        new_precip_needs_ptype: true,
        new_precip_count: 5,
      }),
    )

    expect(wrapper.find('[data-test="new-precip-warning"]').text()).toContain(
      '新增 5 个降水格点需要选择相态',
    )
    expect(wrapper.find('[data-test="target-ptype-dialog"]').exists()).toBe(false)
  })

  it('点击应用后显示相态选择弹窗', async () => {
    const { wrapper } = mountPanel(
      makePreview({
        new_precip_needs_ptype: true,
        new_precip_count: 5,
      }),
    )

    await wrapper.find('[data-test="preview-apply-button"]').trigger('click')

    expect(wrapper.find('[data-test="target-ptype-dialog"]').text()).toContain('新降水需要指定降水类型')
    expect(wrapper.find('[data-test="target-ptype-radio"]').text()).toContain('雨(1)')
    expect(wrapper.find('[data-test="target-ptype-radio"]').text()).toContain('雪(2)')
    expect(wrapper.find('[data-test="target-ptype-radio"]').text()).toContain('雨夹雪(3)')
  })

  it('应用和取消按钮调用 editorStore 方法', async () => {
    const { wrapper, editorStore } = mountPanel(
      makePreview({
        new_precip_needs_ptype: true,
        new_precip_count: 5,
      }),
    )
    const applySpy = vi.spyOn(editorStore, 'applyEdit').mockResolvedValue({
      operation_id: 'operation-1',
      sequence_no: 1,
      applied: true,
      can_undo: true,
      can_redo: false,
    })
    const clearSpy = vi.spyOn(editorStore, 'clearPreview')

    await wrapper.find('[data-test="preview-apply-button"]').trigger('click')
    await wrapper.find('[data-test="target-ptype-radio"] button:nth-child(2)').trigger('click')
    await wrapper.find('[data-test="target-ptype-confirm"]').trigger('click')
    await flushPromises()

    expect(applySpy).toHaveBeenCalledWith(2)

    await wrapper.find('[data-test="preview-cancel-button"]').trigger('click')

    expect(clearSpy).toHaveBeenCalled()
  })
})
