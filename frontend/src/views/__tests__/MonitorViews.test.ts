import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { getAuditLogs } from '@/api/audit'
import { getStorageSummary, getTaskSummary, retryTask } from '@/api/monitor'
import AuditLogView from '@/views/admin/AuditLogView.vue'
import StorageMonitorView from '@/views/admin/StorageMonitorView.vue'
import TaskMonitorView from '@/views/admin/TaskMonitorView.vue'
import type { ApiResponse } from '@/api/admin'
import type { StorageSummary, TaskSummary } from '@/api/monitor'
import type { AuditLogListResponse } from '@/api/audit'

vi.mock('@/api/monitor', () => ({
  getTaskSummary: vi.fn(),
  getStorageSummary: vi.fn(),
  retryTask: vi.fn(),
}))

vi.mock('@/api/audit', () => ({
  getAuditLogs: vi.fn(),
}))

vi.mock('tdesign-vue-next', () => ({
  MessagePlugin: { success: vi.fn() },
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

const STORAGE: StorageSummary = {
  total_bytes: 1000,
  used_bytes: 400,
  free_bytes: 600,
  total_gb: 1,
  used_gb: 0.4,
  free_gb: 0.6,
  last_scan_at: '2026-05-18T00:00:00Z',
  breakdown: [{ type: 'archive', size_bytes: 400, size_gb: 0.4, file_count: 4 }],
}

const AUDIT: AuditLogListResponse = {
  items: [
    {
      id: 1,
      user_id: 1,
      username: 'admin',
      action: 'submit',
      resource_type: 'version',
      resource_id: 'version-1',
      detail_json: '{"version_id":"version-1"}',
      ip_address: '127.0.0.1',
      created_at: '2026-05-18T00:00:00Z',
    },
  ],
  total: 1,
  page: 1,
  page_size: 20,
}

function ok<T>(data: T): ApiResponse<T> {
  return { code: 'OK', message: '成功', data, trace_id: 'trace-1' }
}

function mountWithPinia(component: object) {
  const pinia = createPinia()
  setActivePinia(pinia)
  return mount(component, {
    global: {
      plugins: [pinia],
      stubs: { AppHeader: true },
    },
  })
}

describe('Monitor views', () => {
  beforeEach(() => {
    vi.useRealTimers()
    vi.mocked(getTaskSummary).mockReset()
    vi.mocked(getStorageSummary).mockReset()
    vi.mocked(retryTask).mockReset()
    vi.mocked(getAuditLogs).mockReset()
    vi.mocked(getTaskSummary).mockResolvedValue(ok(TASK_SUMMARY))
    vi.mocked(getStorageSummary).mockResolvedValue(ok(STORAGE))
    vi.mocked(retryTask).mockResolvedValue(ok({ review_id: 'review-1', plot_status: 'pending' }))
    vi.mocked(getAuditLogs).mockResolvedValue(ok(AUDIT))
  })

  it('11.T1 TaskMonitor renders cards and retry button', async () => {
    const wrapper = mountWithPinia(TaskMonitorView)
    await flushPromises()

    expect(wrapper.find('[data-test="task-summary-cards"]').text()).toContain('运行中')
    expect(wrapper.find('[data-test="failed-task-table"]').text()).toContain('review-1')

    await wrapper.find('[data-test="retry-review-1"]').trigger('click')
    await flushPromises()
    expect(retryTask).toHaveBeenCalledWith('review-1')
  })

  it('11.T2 AuditLog renders filters and table', async () => {
    const wrapper = mountWithPinia(AuditLogView)
    await flushPromises()

    expect(wrapper.find('[data-test="audit-filters"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="audit-table"]').text()).toContain('admin')
    expect(wrapper.find('[data-test="audit-table"]').text()).toContain('submit')
    expect(wrapper.find('[data-test="audit-pagination"]').exists()).toBe(true)
  })

  it('11.T3 StorageMonitor renders usage gauge', async () => {
    const wrapper = mountWithPinia(StorageMonitorView)
    await flushPromises()

    expect(wrapper.find('[data-test="storage-gauge"]').text()).toContain('40')
    expect(wrapper.find('[data-test="storage-breakdown"]').text()).toContain('archive')
  })
})
