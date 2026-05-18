import { defineStore } from 'pinia'
import {
  exportStats as exportStatsApi,
  getOperationStats,
  getPtypeTransitions,
} from '@/api/stats'
import type { OperationStats, PtypeTransitionStats, StatsQueryParams } from '@/api/stats'

export interface StatsFilters {
  dateRange: [string, string]
  user_id?: number
  window_id?: string
  accum_hours?: number
}

function formatDate(date: Date) {
  return date.toISOString().slice(0, 10)
}

function defaultDateRange(): [string, string] {
  const end = new Date()
  const start = new Date(end)
  start.setDate(end.getDate() - 29)
  return [formatDate(start), formatDate(end)]
}

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : '统计数据加载失败'
}

function toQuery(filters: StatsFilters): StatsQueryParams {
  return {
    start_date: filters.dateRange[0],
    end_date: filters.dateRange[1],
    user_id: filters.user_id,
    window_id: filters.window_id || undefined,
    accum_hours: filters.accum_hours,
  }
}

export const useStatsStore = defineStore('stats', {
  state: () => ({
    operationStats: null as OperationStats | null,
    ptypeTransitions: null as PtypeTransitionStats | null,
    filters: {
      dateRange: defaultDateRange(),
      user_id: undefined,
      window_id: undefined,
      accum_hours: undefined,
    } as StatsFilters,
    loading: false,
    exporting: false,
    error: null as string | null,
  }),
  actions: {
    setFilters(filters: Partial<StatsFilters>) {
      this.filters = { ...this.filters, ...filters }
    },
    setFilter<K extends keyof StatsFilters>(key: K, value: StatsFilters[K]) {
      this.filters[key] = value
    },
    async fetchOperationStats(params?: Partial<StatsFilters>) {
      if (params) {
        this.setFilters(params)
      }
      this.loading = true
      this.error = null

      try {
        const response = await getOperationStats(toQuery(this.filters))
        this.operationStats = response.data
        return response.data
      } catch (error) {
        this.error = getErrorMessage(error)
        throw error
      } finally {
        this.loading = false
      }
    },
    async fetchPtypeTransitions(params?: Partial<StatsFilters>) {
      if (params) {
        this.setFilters(params)
      }
      this.loading = true
      this.error = null

      try {
        const params = toQuery(this.filters)
        const response = await getPtypeTransitions({
          start_date: params.start_date,
          end_date: params.end_date,
          user_id: params.user_id,
          window_id: params.window_id,
        })
        this.ptypeTransitions = response.data
        return response.data
      } catch (error) {
        this.error = getErrorMessage(error)
        throw error
      } finally {
        this.loading = false
      }
    },
    async fetchAll(params?: Partial<StatsFilters>) {
      if (params) {
        this.setFilters(params)
      }
      await Promise.all([this.fetchOperationStats(), this.fetchPtypeTransitions()])
    },
    async exportStats() {
      this.exporting = true
      this.error = null

      try {
        await exportStatsApi({
          ...toQuery(this.filters),
          format: 'csv',
          include: ['operations', 'ptype_transitions', 'version_summary'],
        })
      } catch (error) {
        this.error = getErrorMessage(error)
        throw error
      } finally {
        this.exporting = false
      }
    },
  },
})
