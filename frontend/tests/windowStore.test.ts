import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { getScanStatus, getWindows, postScan } from '@/api/data'
import type { ScanStatusResponse, WindowItem } from '@/api/data'
import { useWindowStore } from '@/stores/windowStore'

vi.mock('@/api/data', () => ({
  postScan: vi.fn(),
  getScanStatus: vi.fn(),
  getWindows: vi.fn(),
}))

function makeScanStatus(status: string): ScanStatusResponse {
  return {
    scan_id: 'scan-1',
    case_id: '2026010108',
    status,
    scan_started_at: '2026-01-01T08:00:00Z',
    scan_finished_at: status === 'running' ? null : '2026-01-01T08:01:00Z',
    tp_files_found: 10,
    ptype_files_found: 10,
    windows_created: 1,
    windows_updated: 0,
    errors_json: null,
    total_windows: 1,
    available_count: status === 'completed' ? 1 : 0,
    partial_count: 0,
    invalid_count: 0,
  }
}

function makeWindow(windowId = 'ACC24_024_048'): WindowItem {
  return {
    window_id: windowId,
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
  }
}

describe('windowStore', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    setActivePinia(createPinia())
    vi.mocked(postScan).mockReset()
    vi.mocked(getScanStatus).mockReset()
    vi.mocked(getWindows).mockReset()
  })

  afterEach(() => {
    useWindowStore().clearPollingTimer()
    vi.useRealTimers()
  })

  it('test_trigger_scan: postScan 后启动轮询状态', async () => {
    vi.mocked(postScan).mockResolvedValue({ scan_id: 'scan-1', status: 'running' })
    vi.mocked(getScanStatus).mockResolvedValue(makeScanStatus('running'))

    const store = useWindowStore()
    await store.triggerScan('2026010108')

    expect(postScan).toHaveBeenCalledWith('2026010108')
    expect(store.caseId).toBe('2026010108')
    expect(store.scanPolling).toBe(true)
  })

  it('test_poll_status: completed 后停止轮询并拉取窗口', async () => {
    const windows = [makeWindow()]
    vi.mocked(getScanStatus).mockResolvedValue(makeScanStatus('completed'))
    vi.mocked(getWindows).mockResolvedValue(windows)

    const store = useWindowStore()
    store.scanPolling = true
    await store.pollStatus('scan-1')

    expect(getScanStatus).toHaveBeenCalledWith({ scan_id: 'scan-1' })
    expect(store.scanPolling).toBe(false)
    expect(store.windows).toEqual(windows)
  })

  it('test_fetch_windows: getWindows 结果写入 windows', async () => {
    const windows = [makeWindow()]
    vi.mocked(getWindows).mockResolvedValue(windows)

    const store = useWindowStore()
    await store.fetchWindows('2026010108')

    expect(getWindows).toHaveBeenCalledWith({ case_id: '2026010108' })
    expect(store.windows).toEqual(windows)
  })

  it('test_select_window: selectedWindowId 更新', () => {
    const store = useWindowStore()

    store.selectWindow('ACC24_024_048')

    expect(store.selectedWindowId).toBe('ACC24_024_048')
  })
})
