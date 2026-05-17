import { beforeEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import WindowSelector from '@/components/WindowSelector.vue'
import type { WindowItem } from '@/api/data'
import { useWindowStore } from '@/stores/windowStore'

function makeWindow(overrides: Partial<WindowItem>): WindowItem {
  return {
    window_id: 'ACC24_024_048',
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

describe('WindowSelector', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('渲染状态 badge', () => {
    const store = useWindowStore()
    store.windows = [
      makeWindow({ status: 'available' }),
      makeWindow({ window_id: 'ACC24_048_072', start_lead: 48, end_lead: 72, status: 'invalid' }),
    ]

    const wrapper = mount(WindowSelector)

    expect(wrapper.text()).toContain('可用')
    expect(wrapper.text()).toContain('异常')
  })

  it('available/partial 可点击，invalid 禁用', async () => {
    const store = useWindowStore()
    store.windows = [
      makeWindow({ status: 'available' }),
      makeWindow({ window_id: 'ACC24_048_072', start_lead: 48, end_lead: 72, status: 'invalid' }),
    ]

    const wrapper = mount(WindowSelector)
    const buttons = wrapper.findAll('button.window-selector__item')

    await buttons[0].trigger('click')

    expect(store.selectedWindowId).toBe('ACC24_024_048')
    expect(buttons[1].attributes('disabled')).toBeDefined()
    expect(wrapper.html()).toContain('数据异常，不可编辑')
  })
})
