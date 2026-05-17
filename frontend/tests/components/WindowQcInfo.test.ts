import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import WindowQcInfo from '@/components/WindowQcInfo.vue'

describe('WindowQcInfo', () => {
  it('负值和缺失时效显示图标与 tooltip', () => {
    const wrapper = mount(WindowQcInfo, {
      props: {
        negativeCount: 2,
        negativeMinValue: -1.5,
        ptypeMissingLeads: [12, 15],
      },
    })

    expect(wrapper.text()).toContain('!')
    expect(wrapper.text()).toContain('i')
    expect(wrapper.html()).toContain('存在 2 个负值格点（最小值 -1.5mm）')
    expect(wrapper.html()).toContain('缺失时效: 12, 15')
  })

  it('clean window 不显示质控图标', () => {
    const wrapper = mount(WindowQcInfo, {
      props: {
        negativeCount: 0,
        negativeMinValue: null,
        ptypeMissingLeads: [],
      },
    })

    expect(wrapper.text()).toBe('')
  })
})
