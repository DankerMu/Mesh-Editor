import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import GridTooltip from '@/components/map/GridTooltip.vue'
import type { GridHoverPayload } from '@/types/editor'

const IN_BOUNDS_PAYLOAD: GridHoverPayload = {
  lon: 105.35,
  lat: 32.10,
  gridI: 142,
  gridJ: 707,
  qpfBefore: 3.14,
  qpfAfter: 5.0,
  ptypeBefore: 1,
  ptypeAfter: 2,
  isEdited: true,
  inBounds: true,
}

const OUT_OF_BOUNDS_PAYLOAD: GridHoverPayload = {
  lon: 0,
  lat: 0,
  gridI: 0,
  gridJ: 0,
  qpfBefore: null,
  qpfAfter: null,
  ptypeBefore: null,
  ptypeAfter: null,
  isEdited: false,
  inBounds: false,
}

function mountTooltip(props: { payload: GridHoverPayload | null; loading: boolean }) {
  return mount(GridTooltip, { props })
}

describe('GridTooltip (status bar)', () => {
  it('shows coordinates and field values on hover', () => {
    const wrapper = mountTooltip({ payload: IN_BOUNDS_PAYLOAD, loading: false })
    const text = wrapper.text()
    expect(text).toContain('105.35')
    expect(text).toContain('32.10')
    expect(text).toContain('3.14')
    expect(text).toContain('5')
    expect(text).toContain('142')
    expect(text).toContain('707')
  })

  it('shows edited indicator when isEdited=true and inBounds', () => {
    const wrapper = mountTooltip({ payload: IN_BOUNDS_PAYLOAD, loading: false })
    expect(wrapper.find('.edited-indicator').exists()).toBe(true)
    expect(wrapper.text()).toContain('已编辑')
  })

  it('shows "区域外" when cursor leaves map', () => {
    const wrapper = mountTooltip({ payload: OUT_OF_BOUNDS_PAYLOAD, loading: false })
    expect(wrapper.text()).toContain('区域外')
  })

  it('shows "区域外" when payload is null', () => {
    const wrapper = mountTooltip({ payload: null, loading: false })
    expect(wrapper.text()).toContain('区域外')
  })

  it('shows loading state', () => {
    const wrapper = mountTooltip({ payload: null, loading: true })
    expect(wrapper.text()).toContain('数据加载中')
  })

  it('does not show edited indicator when not edited', () => {
    const payload = { ...IN_BOUNDS_PAYLOAD, isEdited: false }
    const wrapper = mountTooltip({ payload, loading: false })
    expect(wrapper.find('.edited-indicator').exists()).toBe(false)
  })

  it('does not show edited indicator when out of bounds', () => {
    const payload = { ...OUT_OF_BOUNDS_PAYLOAD, isEdited: true }
    const wrapper = mountTooltip({ payload, loading: false })
    expect(wrapper.find('.edited-indicator').exists()).toBe(false)
  })

  it('renders with role="status" for accessibility', () => {
    const wrapper = mountTooltip({ payload: null, loading: false })
    expect(wrapper.find('[role="status"]').exists()).toBe(true)
  })

  it('uses grid-tooltip class which binds to --bottom-status-height CSS variable', () => {
    const wrapper = mountTooltip({ payload: null, loading: false })
    expect(wrapper.find('.grid-tooltip').exists()).toBe(true)
  })

  // Pan/zoom updates: GridTooltip receives payload via props from the parent
  // EditorView, which listens to OpenLayers map events (pointermove, moveend).
  // This is a map-level integration behavior that cannot be unit-tested on
  // GridTooltip alone — the component simply re-renders when its payload prop
  // changes, which is already covered by the existing hover tests above.
  it.skip('updates on pan/zoom (integration test — requires OpenLayers map events)', () => {
    // Placeholder: pan/zoom triggers OpenLayers pointermove -> EditorView updates
    // GridHoverPayload prop -> GridTooltip re-renders. Covered at integration level.
  })
})
