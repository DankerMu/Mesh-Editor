import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createAppRouter, routes } from '@/router'
import { exportStats, getOperationStats, getPtypeTransitions } from '@/api/stats'
import { useAuthStore } from '@/stores/authStore'
import StatsView from '@/views/StatsView.vue'
import type { OperationStats, PtypeTransitionStats } from '@/api/stats'
import type { ApiResponse } from '@/api/admin'

vi.mock('vue-echarts', () => ({
  default: {
    props: ['option'],
    template: '<div data-test="echarts"></div>',
  },
}))

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
  by_tool: { brush: 2, polygon: 1 },
  by_operation: { replace: 2, set_ptype: 1 },
}

const PTYPE: PtypeTransitionStats = {
  period: { start_date: '2026-05-01', end_date: '2026-05-18' },
  total_operations_with_transitions: 2,
  matrix: { '0->1': 17, '1->3': 5 },
  top_transitions: [{ transition: '0->1', count: 17, label: 'none->rain' }],
}

function ok<T>(data: T): ApiResponse<T> {
  return { code: 'OK', message: '成功', data, trace_id: 'trace-1' }
}

function mountStatsView() {
  const pinia = createPinia()
  setActivePinia(pinia)
  return mount(StatsView, {
    global: {
      plugins: [pinia],
      stubs: { AppHeader: true },
    },
  })
}

describe('StatsView', () => {
  beforeEach(() => {
    vi.mocked(getOperationStats).mockReset()
    vi.mocked(getPtypeTransitions).mockReset()
    vi.mocked(exportStats).mockReset()
    vi.mocked(getOperationStats).mockResolvedValue(ok(OPERATIONS))
    vi.mocked(getPtypeTransitions).mockResolvedValue(ok(PTYPE))
  })

  it('9.T1 renders filters and statistics cards', async () => {
    const wrapper = mountStatsView()
    await flushPromises()

    expect(wrapper.find('[data-test="date-range-filter"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="user-filter"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="window-filter"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="total-sessions"]').text()).toContain('2')
    expect(wrapper.find('[data-test="total-operations"]').text()).toContain('3')
    expect(wrapper.find('[data-test="ptype-heatmap"]').exists()).toBe(true)
  })

  it('9.T2 export button triggers stats export API', async () => {
    vi.mocked(exportStats).mockResolvedValue(undefined)
    const wrapper = mountStatsView()
    await flushPromises()

    await wrapper.find('[data-test="stats-export"]').trigger('click')
    await flushPromises()

    expect(exportStats).toHaveBeenCalledWith(
      expect.objectContaining({
        format: 'csv',
        include: ['operations', 'ptype_transitions', 'version_summary'],
      }),
    )
  })

  it('9.T3 route /analysis/operations requires auth', async () => {
    const route = routes.find((item) => item.path === '/analysis/operations')
    expect(route?.meta?.requiresAuth).toBe(true)

    setActivePinia(createPinia())
    const router = createAppRouter()
    await router.push('/analysis/operations')
    expect(router.currentRoute.value.path).toBe('/login')

    const authStore = useAuthStore()
    authStore.token = 'jwt-token'
    authStore.user = {
      user_id: 'viewer-1',
      username: 'viewer',
      display_name: '观察员',
      role: 'viewer',
    }

    await router.push('/analysis/operations')
    expect(router.currentRoute.value.path).toBe('/analysis/operations')
  })
})
