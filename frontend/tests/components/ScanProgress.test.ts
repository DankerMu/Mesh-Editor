import { beforeEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ScanProgress from '@/components/ScanProgress.vue'
import { useWindowStore } from '@/stores/windowStore'

describe('ScanProgress', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('running 状态显示扫描中', () => {
    const store = useWindowStore()
    store.scanPolling = true

    const wrapper = mount(ScanProgress)

    expect(wrapper.text()).toContain('正在扫描...')
  })

  it('completed 状态显示可用窗口数量', () => {
    const store = useWindowStore()
    store.scanStatus = {
      scan_id: 'scan-1',
      case_id: '2026010108',
      status: 'completed',
      scan_started_at: '2026-01-01T08:00:00Z',
      scan_finished_at: '2026-01-01T08:01:00Z',
      tp_files_found: 1,
      ptype_files_found: 1,
      windows_created: 1,
      windows_updated: 0,
      errors_json: null,
      total_windows: 1,
      available_count: 3,
      partial_count: 0,
      invalid_count: 0,
    }

    const wrapper = mount(ScanProgress)

    expect(wrapper.text()).toContain('扫描完成：3 个可用窗口')
  })
})
