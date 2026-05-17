import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import GridTooltip from '@/components/map/GridTooltip.vue'
import type { GridHoverPayload } from '@/types/editor'

const validPayload: GridHoverPayload = {
  lon: 90.05,
  lat: 35.1,
  gridI: 202,
  gridJ: 401,
  qpfBefore: 8.25,
  qpfAfter: 10.5,
  ptypeBefore: 1,
  ptypeAfter: 3,
  isEdited: false,
  inBounds: true,
}

describe('GridTooltip', () => {
  it('renders loading text when loading=true', () => {
    const wrapper = mount(GridTooltip, {
      props: {
        payload: validPayload,
        loading: true,
      },
    })

    expect(wrapper.text()).toContain('数据加载中...')
  })

  it('renders out-of-area text when payload=null', () => {
    const wrapper = mount(GridTooltip, {
      props: {
        payload: null,
        loading: false,
      },
    })

    expect(wrapper.text()).toContain('区域外')
  })

  it('renders out-of-area text when payload.inBounds=false', () => {
    const wrapper = mount(GridTooltip, {
      props: {
        payload: {
          ...validPayload,
          inBounds: false,
        },
        loading: false,
      },
    })

    expect(wrapper.text()).toContain('区域外')
  })

  it('renders coordinate and values for valid payload', () => {
    const wrapper = mount(GridTooltip, {
      props: {
        payload: validPayload,
        loading: false,
      },
    })

    const text = wrapper.text()
    expect(text).toContain('经度: 90.05')
    expect(text).toContain('纬度: 35.10')
    expect(text).toContain('行: 202')
    expect(text).toContain('列: 401')
    expect(text).toContain('QPF原始: 8.25')
    expect(text).toContain('QPF订正: 10.50')
    expect(text).toContain('相态原始: 1')
    expect(text).toContain('相态订正: 3')
  })

  it('shows edited indicator when isEdited=true', () => {
    const wrapper = mount(GridTooltip, {
      props: {
        payload: {
          ...validPayload,
          isEdited: true,
        },
        loading: false,
      },
    })

    expect(wrapper.text()).toContain('已编辑')
  })
})
