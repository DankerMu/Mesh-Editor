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

  it('11.T1b TaskMonitor stat cards have colored backgrounds', async () => {
    const wrapper = mountWithPinia(TaskMonitorView)
    await flushPromises()

    const cards = wrapper.findAll('.ops-summary-card')
    expect(cards.length).toBe(4)

    // Pending card has blue background
    expect(cards[0].attributes('style')).toContain('--color-primary-bg')
    // Running card has blue background
    expect(cards[1].attributes('style')).toContain('--color-primary-bg')
    // Success card has green background
    expect(cards[2].attributes('style')).toContain('--color-success-bg')
    // Failed card has red background
    expect(cards[3].attributes('style')).toContain('--color-danger-bg')
  })

  it('11.T1c TaskMonitor stat cards have data-test attributes', async () => {
    const wrapper = mountWithPinia(TaskMonitorView)
    await flushPromises()

    expect(wrapper.find('[data-test="stat-card-等待中"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="stat-card-运行中"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="stat-card-成功"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="stat-card-失败"]').exists()).toBe(true)
  })

  it('11.T2 AuditLog renders filters and table', async () => {
    const wrapper = mountWithPinia(AuditLogView)
    await flushPromises()

    expect(wrapper.find('[data-test="audit-filters"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="audit-table"]').text()).toContain('admin')
    expect(wrapper.find('[data-test="audit-table"]').text()).toContain('submit')
    expect(wrapper.find('[data-test="audit-pagination"]').exists()).toBe(true)
  })

  it('11.T2b AuditLog expandable rows show JSON details', async () => {
    const wrapper = mountWithPinia(AuditLogView)
    await flushPromises()

    // Click expand button
    await wrapper.find('[data-test="audit-expand-1"]').trigger('click')
    await flushPromises()

    // Expanded row should show JSON content
    const expandedRow = wrapper.find('[data-test="audit-expanded-row"]')
    expect(expandedRow.exists()).toBe(true)
    expect(expandedRow.text()).toContain('version-1')

    // JSON highlight pre element should exist
    const pre = expandedRow.find('.json-highlight')
    expect(pre.exists()).toBe(true)
  })

  it('11.T2c AuditLog expandable rows toggle on second click', async () => {
    const wrapper = mountWithPinia(AuditLogView)
    await flushPromises()

    // Click expand
    await wrapper.find('[data-test="audit-expand-1"]').trigger('click')
    expect(wrapper.find('[data-test="audit-expanded-row"]').exists()).toBe(true)

    // Click again to collapse
    await wrapper.find('[data-test="audit-expand-1"]').trigger('click')
    expect(wrapper.find('[data-test="audit-expanded-row"]').exists()).toBe(false)
  })

  it('11.T3 StorageMonitor renders usage gauge', async () => {
    const wrapper = mountWithPinia(StorageMonitorView)
    await flushPromises()

    expect(wrapper.find('[data-test="storage-gauge"]').text()).toContain('40')
    expect(wrapper.find('[data-test="storage-breakdown"]').text()).toContain('archive')
  })

  it('11.T3b StorageMonitor progress bar shows percentage text', async () => {
    const wrapper = mountWithPinia(StorageMonitorView)
    await flushPromises()

    const usageText = wrapper.find('[data-test="storage-usage-text"]')
    expect(usageText.exists()).toBe(true)
    expect(usageText.text()).toContain('0.4 GB')
    expect(usageText.text()).toContain('1 GB')
    expect(usageText.text()).toContain('40%')
  })

  it('11.T3c StorageMonitor shows last scan timestamp', async () => {
    const wrapper = mountWithPinia(StorageMonitorView)
    await flushPromises()

    const scanTime = wrapper.find('[data-test="storage-scan-time"]')
    expect(scanTime.exists()).toBe(true)
    expect(scanTime.text()).toContain('上次扫描')
    expect(scanTime.text()).not.toContain('未扫描')
  })

  it('11.T3d StorageMonitor directory breakdown table shows type/size/count', async () => {
    const wrapper = mountWithPinia(StorageMonitorView)
    await flushPromises()

    const table = wrapper.find('[data-test="storage-breakdown"]')
    expect(table.text()).toContain('archive')
    expect(table.text()).toContain('0.4 GB')
    expect(table.text()).toContain('4')
  })

  it('11.T4 TaskMonitor and StorageMonitor pages use workspace-panel card style', async () => {
    const taskWrapper = mountWithPinia(TaskMonitorView)
    await flushPromises()
    expect(taskWrapper.find('.workspace-panel').exists()).toBe(true)

    const storageWrapper = mountWithPinia(StorageMonitorView)
    await flushPromises()
    expect(storageWrapper.find('.workspace-panel').exists()).toBe(true)
  })
})
