import { defineStore } from 'pinia'
import { getStorageSummary, getTaskSummary, retryTask as retryTaskApi } from '@/api/monitor'
import type { StorageSummary, TaskSummary } from '@/api/monitor'

const AUTO_REFRESH_MS = 10_000

type RefreshTimer = ReturnType<typeof setInterval> | null

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : '监控数据加载失败'
}

export const useMonitorStore = defineStore('monitor', {
  state: () => ({
    taskSummary: null as TaskSummary | null,
    storageSummary: null as StorageSummary | null,
    loading: false,
    error: null as string | null,
    refreshTimer: null as RefreshTimer,
  }),
  actions: {
    async fetchTaskSummary() {
      this.loading = true
      this.error = null

      try {
        const response = await getTaskSummary()
        this.taskSummary = response.data
        return response.data
      } catch (error) {
        this.error = getErrorMessage(error)
        throw error
      } finally {
        this.loading = false
      }
    },
    async fetchStorageSummary() {
      this.loading = true
      this.error = null

      try {
        const response = await getStorageSummary()
        this.storageSummary = response.data
        return response.data
      } catch (error) {
        this.error = getErrorMessage(error)
        throw error
      } finally {
        this.loading = false
      }
    },
    async retryTask(reviewId: string) {
      this.loading = true
      this.error = null

      try {
        const response = await retryTaskApi(reviewId)
        await this.fetchTaskSummary()
        return response.data
      } catch (error) {
        this.error = getErrorMessage(error)
        throw error
      } finally {
        this.loading = false
      }
    },
    startAutoRefresh(intervalMs = AUTO_REFRESH_MS) {
      this.stopAutoRefresh()
      this.refreshTimer = setInterval(() => {
        void this.fetchTaskSummary()
      }, intervalMs)
    },
    stopAutoRefresh() {
      if (this.refreshTimer) {
        clearInterval(this.refreshTimer)
        this.refreshTimer = null
      }
    },
  },
})
