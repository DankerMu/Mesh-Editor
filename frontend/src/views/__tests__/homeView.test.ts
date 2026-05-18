import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/authStore'
import { useWindowStore } from '@/stores/windowStore'
import HomeView from '@/views/HomeView.vue'
import type { WindowItem } from '@/api/data'

const pushMock = vi.fn()
vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue-router')>()
  return {
    ...actual,
    useRouter: () => ({
      push: pushMock,
      currentRoute: { value: { path: '/' } },
    }),
  }
})

function makeWindow(overrides: Partial<WindowItem> = {}): WindowItem {
  return {
    window_id: 'w-001',
    accum_hours: 24,
    start_lead: 0,
    end_lead: 24,
    status: 'available',
    qc_status: 'pass',
    negative_count: 0,
    negative_min_value: null,
    negative_abs_max: null,
    missing_count: 0,
    ptype_missing_leads: null,
    qpf_before_path: null,
    ptype_before_path: null,
    data_ready_at: null,
    updated_at: null,
    ...overrides,
  }
}

function mountHome(): VueWrapper {
  const pinia = createPinia()
  setActivePinia(pinia)
  return mount(HomeView, { global: { plugins: [pinia] } })
}

function mountHomeWithRole(role: string): { wrapper: VueWrapper; authStore: ReturnType<typeof useAuthStore>; windowStore: ReturnType<typeof useWindowStore> } {
  const pinia = createPinia()
  setActivePinia(pinia)
  const authStore = useAuthStore()
  authStore.user = { user_id: 'u1', username: 'test', display_name: 'Test', role }
  authStore.token = 'tok'
  const windowStore = useWindowStore()
  const wrapper = mount(HomeView, { global: { plugins: [pinia] } })
  return { wrapper, authStore, windowStore }
}

describe('HomeView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  /* ---------- Layout ---------- */

  describe('layout', () => {
    it('renders AppHeader', () => {
      const wrapper = mountHome()
      expect(wrapper.find('[data-test="app-header-stub"]').exists()).toBe(true)
    })

    it('renders CaseIdInput within workspace panel', () => {
      const wrapper = mountHome()
      expect(wrapper.find('.case-id-input').exists()).toBe(true)
    })

    it('workspace panel has card styling class', () => {
      const wrapper = mountHome()
      expect(wrapper.find('.workspace-panel').exists()).toBe(true)
    })
  })

  /* ---------- Permission checks ---------- */

  describe('permission checks', () => {
    it('scan button is disabled for viewer role', () => {
      const { wrapper } = mountHomeWithRole('viewer')
      const buttons = wrapper.findAll('button')
      const scanBtn = buttons.find((b) => b.text().includes('扫描数据'))
      expect(scanBtn?.attributes('disabled')).toBeDefined()
    })

    it('scan button is disabled for forecaster role', () => {
      const { wrapper } = mountHomeWithRole('forecaster')
      const buttons = wrapper.findAll('button')
      const scanBtn = buttons.find((b) => b.text().includes('扫描数据'))
      expect(scanBtn?.attributes('disabled')).toBeDefined()
    })

    it('scan button is enabled for reviewer role with valid input', async () => {
      const { wrapper } = mountHomeWithRole('reviewer')
      const input = wrapper.find('.case-id-input__control input')
      await input.setValue('2026010108')
      await input.trigger('blur')

      const buttons = wrapper.findAll('button')
      const scanBtn = buttons.find((b) => b.text().includes('扫描数据'))
      expect(scanBtn?.attributes('disabled')).toBeUndefined()
    })

    it('scan button is enabled for admin role with valid input', async () => {
      const { wrapper } = mountHomeWithRole('admin')
      const input = wrapper.find('.case-id-input__control input')
      await input.setValue('2026010108')
      await input.trigger('blur')

      const buttons = wrapper.findAll('button')
      const scanBtn = buttons.find((b) => b.text().includes('扫描数据'))
      expect(scanBtn?.attributes('disabled')).toBeUndefined()
    })

    it('permission tooltip shows for viewer role', () => {
      const { wrapper } = mountHomeWithRole('viewer')
      const tooltip = wrapper.find('.case-id-input [title="无扫描权限"]')
      expect(tooltip.exists()).toBe(true)
    })

    it('permission tooltip shows for forecaster role', () => {
      const { wrapper } = mountHomeWithRole('forecaster')
      const tooltip = wrapper.find('.case-id-input [title="无扫描权限"]')
      expect(tooltip.exists()).toBe(true)
    })
  })

  /* ---------- WindowSelector ---------- */

  describe('WindowSelector', () => {
    it('renders tabs for 24h, 48h, 168h', () => {
      const wrapper = mountHome()
      const tabs = wrapper.find('[data-test="tabs"]')
      expect(tabs.exists()).toBe(true)
      expect(wrapper.text()).toContain('24h')
      expect(wrapper.text()).toContain('48h')
      expect(wrapper.text()).toContain('168h')
    })

    it('available window shows green (success) tag', () => {
      const pinia = createPinia()
      setActivePinia(pinia)
      const windowStore = useWindowStore()
      windowStore.windows = [makeWindow({ status: 'available' })]
      const wrapper = mount(HomeView, { global: { plugins: [pinia] } })
      const tag = wrapper.find('[data-theme="success"]')
      expect(tag.exists()).toBe(true)
      expect(tag.text()).toBe('可用')
    })

    it('partial window shows orange (warning) tag', () => {
      const pinia = createPinia()
      setActivePinia(pinia)
      const windowStore = useWindowStore()
      windowStore.windows = [makeWindow({ window_id: 'w-p', status: 'partial' })]
      const wrapper = mount(HomeView, { global: { plugins: [pinia] } })
      const tag = wrapper.find('[data-theme="warning"]')
      expect(tag.exists()).toBe(true)
      expect(tag.text()).toBe('部分缺失')
    })

    it('invalid window shows red (danger) tag with aria-disabled', () => {
      const pinia = createPinia()
      setActivePinia(pinia)
      const windowStore = useWindowStore()
      windowStore.windows = [makeWindow({ window_id: 'w-inv', status: 'invalid' })]
      const wrapper = mount(HomeView, { global: { plugins: [pinia] } })
      const tag = wrapper.find('[data-theme="danger"]')
      expect(tag.exists()).toBe(true)
      expect(tag.text()).toBe('异常')

      const item = wrapper.find('.window-selector__item')
      expect(item.attributes('aria-disabled')).toBe('true')
    })

    it('clicking available window navigates to /editor/{windowId}', async () => {
      const pinia = createPinia()
      setActivePinia(pinia)
      const windowStore = useWindowStore()
      windowStore.windows = [makeWindow({ window_id: 'w-nav', status: 'available' })]
      mount(HomeView, { global: { plugins: [pinia] } })

      // Simulate selectWindow which sets selectedWindowId triggering the watcher
      windowStore.selectWindow('w-nav')
      await flushPromises()

      expect(pushMock).toHaveBeenCalledWith('/editor/w-nav')
    })

    it('clicking invalid window does not navigate', async () => {
      const pinia = createPinia()
      setActivePinia(pinia)
      const windowStore = useWindowStore()
      windowStore.windows = [makeWindow({ window_id: 'w-bad', status: 'invalid' })]
      const wrapper = mount(HomeView, { global: { plugins: [pinia] } })

      const item = wrapper.find('.window-selector__item')
      await item.trigger('click')
      await flushPromises()

      expect(pushMock).not.toHaveBeenCalled()
    })
  })

  /* ---------- Scan progress ---------- */

  describe('scan progress', () => {
    it('shows progress bar during scanning', () => {
      const pinia = createPinia()
      setActivePinia(pinia)
      const windowStore = useWindowStore()
      windowStore.scanPolling = true
      windowStore.scanStatus = {
        scan_id: 's1',
        case_id: '2026010108',
        status: 'running',
        scan_started_at: '',
        scan_finished_at: null,
        tp_files_found: 0,
        ptype_files_found: 0,
        windows_created: 0,
        windows_updated: 0,
        errors_json: null,
        total_windows: 0,
        available_count: 0,
        partial_count: 0,
        invalid_count: 0,
      }
      const wrapper = mount(HomeView, { global: { plugins: [pinia] } })

      expect(wrapper.find('.scan-progress__running').exists()).toBe(true)
      expect(wrapper.text()).toContain('正在扫描')
    })

    it('shows error message on scan failure', () => {
      const pinia = createPinia()
      setActivePinia(pinia)
      const windowStore = useWindowStore()
      windowStore.scanPolling = false
      windowStore.scanStatus = {
        scan_id: 's2',
        case_id: '2026010108',
        status: 'failed',
        scan_started_at: '',
        scan_finished_at: null,
        tp_files_found: 0,
        ptype_files_found: 0,
        windows_created: 0,
        windows_updated: 0,
        errors_json: [{ message: '文件读取失败' }],
        total_windows: 0,
        available_count: 0,
        partial_count: 0,
        invalid_count: 0,
      }
      windowStore.scanErrorMessage = '文件读取失败'
      const wrapper = mount(HomeView, { global: { plugins: [pinia] } })

      expect(wrapper.find('.scan-progress__failed').exists()).toBe(true)
      expect(wrapper.text()).toContain('文件读取失败')
    })

    it('shows retry button on scan failure', () => {
      const pinia = createPinia()
      setActivePinia(pinia)
      const windowStore = useWindowStore()
      windowStore.scanPolling = false
      windowStore.scanStatus = {
        scan_id: 's3',
        case_id: '2026010108',
        status: 'failed',
        scan_started_at: '',
        scan_finished_at: null,
        tp_files_found: 0,
        ptype_files_found: 0,
        windows_created: 0,
        windows_updated: 0,
        errors_json: null,
        total_windows: 0,
        available_count: 0,
        partial_count: 0,
        invalid_count: 0,
      }
      const wrapper = mount(HomeView, { global: { plugins: [pinia] } })

      const retryBtn = wrapper.findAll('button').find((b) => b.text().includes('重新扫描'))
      expect(retryBtn).toBeDefined()
    })

    it('shows completion message after scan', () => {
      const pinia = createPinia()
      setActivePinia(pinia)
      const windowStore = useWindowStore()
      windowStore.scanPolling = false
      windowStore.scanStatus = {
        scan_id: 's4',
        case_id: '2026010108',
        status: 'complete',
        scan_started_at: '',
        scan_finished_at: '2026-01-01T00:10:00Z',
        tp_files_found: 5,
        ptype_files_found: 5,
        windows_created: 3,
        windows_updated: 0,
        errors_json: null,
        total_windows: 3,
        available_count: 2,
        partial_count: 1,
        invalid_count: 0,
      }
      const wrapper = mount(HomeView, { global: { plugins: [pinia] } })

      expect(wrapper.find('.scan-progress__complete').exists()).toBe(true)
      expect(wrapper.text()).toContain('扫描完成')
    })
  })
})
