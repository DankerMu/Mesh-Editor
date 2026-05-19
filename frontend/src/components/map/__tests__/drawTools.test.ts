import { beforeEach, describe, expect, test, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import DrawTools, { simplifyDouglasPeucker } from '@/components/map/DrawTools.vue'
import { useEditorStore } from '@/stores/editorStore'

type DrawToolsVm = InstanceType<typeof DrawTools> & {
  activateTool: (tool: 'polygon' | 'line_buffer' | 'brush_path' | 'lasso') => void
  clearLassoPath: () => void
  completeLassoForTest: (points: [number, number][]) => void
  isLassoing: boolean
}

type MapHandler = (event: unknown) => void

function makeMapMock() {
  const handlers = new Map<string, MapHandler>()
  const viewport = document.createElement('div')

  return {
    handlers,
    map: {
      on: vi.fn((eventName: string, handler: MapHandler) => {
        handlers.set(eventName, handler)
        return { eventName, handler }
      }),
      getViewport: vi.fn(() => viewport),
      addInteraction: vi.fn(),
      removeInteraction: vi.fn(),
    },
  }
}

function mountDrawTools(options?: { map?: unknown }) {
  return mount(DrawTools, {
    props: {
      disabled: false,
      map: options?.map,
    },
  })
}

function makeLassoPoints(count: number): [number, number][] {
  return Array.from({ length: count }, (_, index) => {
    const angle = (Math.PI * 2 * index) / count
    return [100 + Math.cos(angle) * 0.1, 30 + Math.sin(angle) * 0.1]
  })
}

describe('DrawTools lasso', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.restoreAllMocks()
  })

  test('DRAW_TOOLS includes lasso button', () => {
    const wrapper = mountDrawTools()

    const toolButtons = wrapper.findAll('[data-test^="tool-"]').filter((button) => {
      return button.attributes('data-test') !== 'tool-clear-selection'
    })

    expect(toolButtons).toHaveLength(4)
    expect(wrapper.find('[data-test="tool-lasso"]').exists()).toBe(true)
    expect(toolButtons.map((button) => button.text())).toEqual(['多边形', '线缓冲', '画刷', '套索'])
  })

  test('lasso tool activates on click', async () => {
    const store = useEditorStore()
    store.sessionId = 'session-1'
    const wrapper = mountDrawTools()

    await wrapper.find('[data-test="tool-lasso"]').trigger('click')

    expect(store.activeTool).toBe('lasso')
    expect(wrapper.find('[data-test="tool-lasso"]').classes()).toContain('draw-tools__button--active')
  })

  test('L key activates lasso tool', async () => {
    const store = useEditorStore()
    store.sessionId = 'session-1'
    const activateSpy = vi.spyOn(store, 'setActiveTool')
    const wrapper = mountDrawTools()

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'L' }))
    await wrapper.vm.$nextTick()

    expect(activateSpy).toHaveBeenCalledWith('lasso')
    expect(store.activeTool).toBe('lasso')
  })

  test('Escape during lasso clears state', async () => {
    const store = useEditorStore()
    store.sessionId = 'session-1'
    const { map, handlers } = makeMapMock()
    const wrapper = mountDrawTools({ map })

    await wrapper.find('[data-test="tool-lasso"]').trigger('click')
    handlers.get('pointerdown')?.({
      coordinate: [100, 30],
      pixel: [10, 10],
      originalEvent: new PointerEvent('pointerdown', { button: 0 }),
    })
    expect((wrapper.vm as DrawToolsVm).isLassoing).toBe(true)
    expect(store.currentMaskGeometry).toEqual({ type: 'lasso', coordinates: [[100, 30]] })

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await wrapper.vm.$nextTick()

    expect((wrapper.vm as DrawToolsVm).isLassoing).toBe(false)
    expect(store.currentMaskGeometry).toBeNull()
    expect(store.activeTool).toBeNull()
  })

  test('simplifyDouglasPeucker reduces point count', () => {
    const points: [number, number][] = Array.from({ length: 40 }, (_, index) => [
      100 + index * 0.01,
      30 + Math.sin(index) * 0.001,
    ])

    const simplified = simplifyDouglasPeucker(points, 0.005)

    expect(simplified.length).toBeLessThan(points.length)
    expect(simplified[0]).toEqual(points[0])
    expect(simplified[simplified.length - 1]).toEqual(points[points.length - 1])
  })

  test('completeLassoForTest emits correct geometry', () => {
    const store = useEditorStore()
    store.sessionId = 'session-1'
    const wrapper = mountDrawTools()
    const points = makeLassoPoints(20)

    ;(wrapper.vm as DrawToolsVm).completeLassoForTest(points)

    const emitted = wrapper.emitted('mask-created')?.[0]?.[0]
    expect(emitted).toEqual(expect.objectContaining({ type: 'lasso' }))
    expect(store.currentMaskGeometry).toEqual(emitted)
    expect((emitted as { coordinates: [number, number][] }).coordinates[0]).toEqual(
      (emitted as { coordinates: [number, number][] }).coordinates.at(-1),
    )
  })
})
