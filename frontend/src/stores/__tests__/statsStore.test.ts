import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { exportStats, getOperationStats, getPtypeTransitions } from '@/api/stats'
import { useStatsStore } from '@/stores/statsStore'
import type { OperationStats, PtypeTransitionStats } from '@/api/stats'
import type { ApiResponse } from '@/api/admin'

vi.mock('@/api/stats', () => ({
  getOperationStats: vi.fn(),
  getPtypeTransitions: vi.fn(),
  exportStats: vi.fn(),
}))

const OPERATIONS: OperationStats = {
  period: { start_date: '2026-05-01', end_date: '2026-05-18' },
  total_sessions: 2,
  total_operations: 3,
  total_versions_saved: 2,
  total_versions_released: 1,
  by_accum_hours: { '24': { sessions: 1, versions: 1 } },
  by_tool: { brush: 2 },
  by_operation: { replace: 2 },
}

const PTYPE: PtypeTransitionStats = {
  period: { start_date: '2026-05-01', end_date: '2026-05-18' },
  total_operations_with_transitions: 2,
  matrix: { '0->1': 17 },
  top_transitions: [{ transition: '0->1', count: 17, label: 'none->rain' }],
}

function ok<T>(data: T): ApiResponse<T> {
  return { code: 'OK', message: '成功', data, trace_id: 'trace-1' }
}

describe('statsStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(getOperationStats).mockReset()
    vi.mocked(getPtypeTransitions).mockReset()
    vi.mocked(exportStats).mockReset()
  })

  it('fetchOperationStats sends filters and stores response', async () => {
    vi.mocked(getOperationStats).mockResolvedValue(ok(OPERATIONS))

    const store = useStatsStore()
    await store.fetchOperationStats({
      dateRange: ['2026-05-01', '2026-05-18'],
      user_id: 1,
      window_id: 'window-1',
      accum_hours: 24,
    })

    expect(getOperationStats).toHaveBeenCalledWith({
      start_date: '2026-05-01',
      end_date: '2026-05-18',
      user_id: 1,
      window_id: 'window-1',
      accum_hours: 24,
    })
    expect(store.operationStats?.total_operations).toBe(3)
  })

  it('fetchPtypeTransitions omits accum_hours', async () => {
    vi.mocked(getPtypeTransitions).mockResolvedValue(ok(PTYPE))

    const store = useStatsStore()
    await store.fetchPtypeTransitions({
      dateRange: ['2026-05-01', '2026-05-18'],
      accum_hours: 48,
    })

    expect(getPtypeTransitions).toHaveBeenCalledWith({
      start_date: '2026-05-01',
      end_date: '2026-05-18',
      user_id: undefined,
      window_id: undefined,
    })
    expect(store.ptypeTransitions?.matrix['0->1']).toBe(17)
  })

  it('setFilter updates filters and export uses current filters', async () => {
    vi.mocked(exportStats).mockResolvedValue(undefined)

    const store = useStatsStore()
    store.setFilter('dateRange', ['2026-05-01', '2026-05-18'])
    store.setFilter('window_id', 'window-1')
    await store.exportStats()

    expect(store.filters.window_id).toBe('window-1')
    expect(exportStats).toHaveBeenCalledWith({
      start_date: '2026-05-01',
      end_date: '2026-05-18',
      user_id: undefined,
      window_id: 'window-1',
      accum_hours: undefined,
      format: 'csv',
      include: ['operations', 'ptype_transitions', 'version_summary'],
    })
  })
})
