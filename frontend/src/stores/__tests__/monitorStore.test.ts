import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { getStorageSummary, getTaskSummary, retryTask } from '@/api/monitor'
import { useMonitorStore } from '@/stores/monitorStore'
import type { TaskSummary } from '@/api/monitor'
import type { ApiResponse } from '@/api/admin'

vi.mock('@/api/monitor', () => ({
  getStorageSummary: vi.fn(),
  getTaskSummary: vi.fn(),
  retryTask: vi.fn(),
}))

const TASK_SUMMARY: TaskSummary = {
  counts: {
    pending: 1,
    running: 2,
    success: 3,
    partial_success: 0,
    failed: 1,
    permanently_failed: 0,
    superseded: 0,
  },
  recent_failed: [
    {
      review_id: 'review-1',
      window_id: 'window-1',
      plot_status: 'failed',
      error_summary: '绘图失败',
      failed_at: '2026-05-18T00:00:00Z',
    },
  ],
}

function ok<T>(data: T): ApiResponse<T> {
  return { code: 'OK', message: '成功', data, trace_id: 'trace-1' }
}

describe('monitorStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.useRealTimers()
    vi.mocked(getTaskSummary).mockReset()
    vi.mocked(getStorageSummary).mockReset()
    vi.mocked(retryTask).mockReset()
  })

  it('fetchTaskSummary stores task summary', async () => {
    vi.mocked(getTaskSummary).mockResolvedValue(ok(TASK_SUMMARY))

    const store = useMonitorStore()
    await store.fetchTaskSummary()

    expect(getTaskSummary).toHaveBeenCalled()
    expect(store.taskSummary?.counts.running).toBe(2)
  })

  it('retryTask calls API and refreshes task summary', async () => {
    vi.mocked(retryTask).mockResolvedValue(ok({ review_id: 'review-1', plot_status: 'pending' }))
    vi.mocked(getTaskSummary).mockResolvedValue(ok(TASK_SUMMARY))

    const store = useMonitorStore()
    await store.retryTask('review-1')

    expect(retryTask).toHaveBeenCalledWith('review-1')
    expect(getTaskSummary).toHaveBeenCalled()
  })

  it('auto-refresh starts and stops interval', async () => {
    vi.useFakeTimers()
    vi.mocked(getTaskSummary).mockResolvedValue(ok(TASK_SUMMARY))

    const store = useMonitorStore()
    store.startAutoRefresh(1000)
    expect(store.refreshTimer).not.toBeNull()

    await vi.advanceTimersByTimeAsync(1000)
    expect(getTaskSummary).toHaveBeenCalledTimes(1)

    store.stopAutoRefresh()
    expect(store.refreshTimer).toBeNull()
    await vi.advanceTimersByTimeAsync(1000)
    expect(getTaskSummary).toHaveBeenCalledTimes(1)
  })
})
