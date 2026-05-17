import { defineStore } from 'pinia'
import { getScanStatus, getWindows, postScan } from '@/api/data'
import type { ScanStatusResponse, WindowItem } from '@/api/data'

const POLL_INTERVAL_MS = 2000

function getErrorMessage(scanStatus: ScanStatusResponse | null) {
  const firstError = scanStatus?.errors_json?.[0]
  const message = firstError?.message ?? firstError?.code
  return typeof message === 'string' ? message : '扫描失败'
}

export const useWindowStore = defineStore('window', {
  state: () => ({
    caseId: '',
    windows: [] as WindowItem[],
    scanStatus: null as ScanStatusResponse | null,
    selectedWindowId: null as string | null,
    scanPolling: false,
    pollingTimer: null as ReturnType<typeof setTimeout> | null,
    scanErrorMessage: null as string | null,
  }),
  getters: {
    selectedWindow: (state) =>
      state.windows.find((window) => window.window_id === state.selectedWindowId) ?? null,
    availableCount: (state) =>
      state.scanStatus?.available_count ??
      state.windows.filter((window) => window.status === 'available').length,
  },
  actions: {
    clearPollingTimer() {
      if (this.pollingTimer) {
        clearTimeout(this.pollingTimer)
        this.pollingTimer = null
      }
    },
    async triggerScan(caseId: string) {
      this.clearPollingTimer()
      this.caseId = caseId
      this.windows = []
      this.selectedWindowId = null
      this.scanStatus = null
      this.scanErrorMessage = null
      this.scanPolling = true

      const response = await postScan(caseId)
      await this.pollStatus(response.scan_id)
    },
    async pollStatus(scanId: string): Promise<void> {
      const status = await getScanStatus({ scan_id: scanId })
      this.scanStatus = status

      if (status.status === 'running') {
        this.scanPolling = true
        this.clearPollingTimer()
        this.pollingTimer = setTimeout(() => {
          void this.pollStatus(scanId)
        }, POLL_INTERVAL_MS)
        return
      }

      this.scanPolling = false
      this.clearPollingTimer()

      if (status.status === 'failed') {
        this.scanErrorMessage = getErrorMessage(status)
        return
      }

      await this.fetchWindows(status.case_id)
    },
    async fetchWindows(caseId: string) {
      this.caseId = caseId
      this.windows = await getWindows({ case_id: caseId })
    },
    selectWindow(windowId: string) {
      this.selectedWindowId = windowId
    },
  },
})
