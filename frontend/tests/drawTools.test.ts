import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import DrawTools from '@/components/map/DrawTools.vue'
import { useEditorStore } from '@/stores/editorStore'

type DrawToolsVm = InstanceType<typeof DrawTools> & {
  activateTool: (tool: 'polygon' | 'line_buffer' | 'brush_path' | 'lasso') => void
  completePolygonForTest: (points: [number, number][]) => void
  completeLineBufferForTest: (points: [number, number][]) => void
  completeBrushStrokeForTest: (points: [number, number][]) => void
  widthGrid: number
}

function mountDrawTools(disabled = false) {
  return mount(DrawTools, {
    props: {
      disabled,
    },
  })
}

describe('DrawTools', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('disabled=true 时禁用所有工具按钮', () => {
    const wrapper = mountDrawTools(true)

    const buttons = wrapper.findAll('button')
    expect(buttons).toHaveLength(5)
    expect(buttons.every((button) => button.attributes('disabled') !== undefined)).toBe(true)
  })

  it('工具互斥：激活一个工具会替换另一个工具', async () => {
    const store = useEditorStore()
    store.sessionId = 'session-1'
    const wrapper = mountDrawTools()

    await wrapper.find('[data-test="tool-polygon"]').trigger('click')
    expect(store.activeTool).toBe('polygon')
    expect(wrapper.find('[data-test="tool-polygon"]').classes()).toContain('draw-tools__button--active')

    await wrapper.find('[data-test="tool-brush_path"]').trigger('click')
    expect(store.activeTool).toBe('brush_path')
    expect(wrapper.find('[data-test="tool-polygon"]').classes()).not.toContain('draw-tools__button--active')
    expect(wrapper.find('[data-test="tool-brush_path"]').classes()).toContain('draw-tools__button--active')
  })

  it('点击当前激活工具会取消激活', async () => {
    const store = useEditorStore()
    store.sessionId = 'session-1'
    const wrapper = mountDrawTools()

    await wrapper.find('[data-test="tool-line_buffer"]').trigger('click')
    expect(store.activeTool).toBe('line_buffer')

    await wrapper.find('[data-test="tool-line_buffer"]').trigger('click')
    expect(store.activeTool).toBeNull()
  })

  it('polygon 完成后输出 MaskGeometry', () => {
    const store = useEditorStore()
    store.sessionId = 'session-1'
    const wrapper = mountDrawTools()

    const points: [number, number][] = [
      [100, 30],
      [101, 30],
      [101, 31],
    ]
    ;(wrapper.vm as DrawToolsVm).completePolygonForTest(points)

    expect(wrapper.emitted('mask-created')?.[0]).toEqual([
      {
        type: 'polygon',
        coordinates: points,
      },
    ])
    expect(store.currentMaskGeometry).toEqual({
      type: 'polygon',
      coordinates: points,
    })
  })

  it('line_buffer 完成后输出坐标和 width_grid', async () => {
    const store = useEditorStore()
    store.sessionId = 'session-1'
    const wrapper = mountDrawTools()

    await wrapper.find('[data-test="tool-line_buffer"]').trigger('click')
    await wrapper.find('[data-test="width-grid-input"]').setValue('10')

    const points: [number, number][] = [
      [100, 30],
      [101, 30],
      [102, 31],
    ]
    ;(wrapper.vm as DrawToolsVm).completeLineBufferForTest(points)

    expect(wrapper.emitted('mask-created')?.[0]).toEqual([
      {
        type: 'line_buffer',
        coordinates: points,
        width_grid: 10,
      },
    ])
  })

  it('line_buffer width_grid 会限制在 1-50', async () => {
    const store = useEditorStore()
    store.sessionId = 'session-1'
    const wrapper = mountDrawTools()

    await wrapper.find('[data-test="tool-line_buffer"]').trigger('click')
    await wrapper.find('[data-test="width-grid-input"]').setValue('99')

    ;(wrapper.vm as DrawToolsVm).completeLineBufferForTest([
      [100, 30],
      [101, 30],
    ])

    expect(wrapper.emitted('mask-created')?.[0]).toEqual([
      {
        type: 'line_buffer',
        coordinates: [
          [100, 30],
          [101, 30],
        ],
        width_grid: 50,
      },
    ])
  })

  it('brush_path 完成后输出 points 和 radius_grid', () => {
    const store = useEditorStore()
    store.sessionId = 'session-1'
    const wrapper = mountDrawTools()
    const vm = wrapper.vm as DrawToolsVm

    vm.activateTool('brush_path')
    const firstStroke: [number, number][] = [
      [100, 30],
      [100.1, 30.2],
    ]
    const secondStroke: [number, number][] = [[100.3, 30.4]]

    vm.completeBrushStrokeForTest(firstStroke)
    vm.completeBrushStrokeForTest(secondStroke)

    expect(wrapper.emitted('mask-created')?.[0]).toEqual([
      {
        type: 'brush_path',
        points: firstStroke,
        radius_grid: 3,
      },
    ])
    expect(wrapper.emitted('mask-created')?.[1]).toEqual([
      {
        type: 'brush_path',
        points: [...firstStroke, ...secondStroke],
        radius_grid: 3,
      },
    ])
  })

  it('clear_selection 调用 editorStore.clearMask()', async () => {
    const store = useEditorStore()
    store.sessionId = 'session-1'
    store.setActiveTool('polygon')
    store.setMaskGeometry({
      type: 'polygon',
      coordinates: [
        [100, 30],
        [101, 30],
        [101, 31],
      ],
    })
    const clearMaskSpy = vi.spyOn(store, 'clearMask')
    const wrapper = mountDrawTools()

    await wrapper.find('[data-test="tool-clear-selection"]').trigger('click')

    expect(clearMaskSpy).toHaveBeenCalledOnce()
    expect(store.currentMaskGeometry).toBeNull()
    expect(store.activeTool).toBeNull()
  })

  it('brush_path 完成后按 Esc 清除 store geometry', async () => {
    const store = useEditorStore()
    store.sessionId = 'session-1'
    const wrapper = mountDrawTools()
    const vm = wrapper.vm as DrawToolsVm

    vm.activateTool('brush_path')
    vm.completeBrushStrokeForTest([
      [100, 30],
      [100.1, 30.2],
    ])

    expect(store.currentMaskGeometry).toEqual({
      type: 'brush_path',
      points: [
        [100, 30],
        [100.1, 30.2],
      ],
      radius_grid: 3,
    })

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await wrapper.vm.$nextTick()

    expect(store.currentMaskGeometry).toBeNull()
    expect(store.activeTool).toBeNull()
  })
})
