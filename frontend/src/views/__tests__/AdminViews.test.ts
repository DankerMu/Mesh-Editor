import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createAppRouter, routes } from '@/router'
import { createUser, getUsers, updateUser } from '@/api/admin'
import { getConfig, getConfigHistory, updateConfig } from '@/api/config'
import { getTemplate, getTemplates } from '@/api/templates'
import { useAuthStore } from '@/stores/authStore'
import ConfigManagementView from '@/views/admin/ConfigManagementView.vue'
import UserManagementView from '@/views/admin/UserManagementView.vue'
import TemplateManagementView from '@/views/admin/TemplateManagementView.vue'
import type { ApiResponse, UserItem, UserListResponse } from '@/api/admin'

vi.mock('@/api/admin', () => ({
  getUsers: vi.fn(),
  createUser: vi.fn(),
  updateUser: vi.fn(),
}))

vi.mock('@/api/config', () => ({
  getConfig: vi.fn(),
  updateConfig: vi.fn(),
  getConfigHistory: vi.fn(),
}))

vi.mock('@/api/templates', () => ({
  getTemplates: vi.fn(),
  getTemplate: vi.fn(),
  updateTemplate: vi.fn(),
}))

vi.mock('tdesign-vue-next', () => ({
  MessagePlugin: { success: vi.fn() },
}))

const ADMIN_USER: UserItem = {
  id: 1,
  username: 'admin',
  display_name: '管理员',
  role: 'admin',
  is_active: true,
  created_at: '2026-05-18T00:00:00Z',
  updated_at: '2026-05-18T00:00:00Z',
  last_login_at: '2026-05-18T01:00:00Z',
}

const REVIEWER_USER: UserItem = {
  id: 2,
  username: 'reviewer1',
  display_name: '审核员A',
  role: 'reviewer',
  is_active: true,
  created_at: '2026-05-18T00:00:00Z',
  updated_at: '2026-05-18T00:00:00Z',
  last_login_at: null,
}

const FORECASTER_USER: UserItem = {
  id: 3,
  username: 'forecaster1',
  display_name: '预报员A',
  role: 'forecaster',
  is_active: true,
  created_at: '2026-05-18T00:00:00Z',
  updated_at: '2026-05-18T00:00:00Z',
  last_login_at: null,
}

const VIEWER_USER: UserItem = {
  id: 4,
  username: 'viewer1',
  display_name: '观察员A',
  role: 'viewer',
  is_active: false,
  created_at: '2026-05-18T00:00:00Z',
  updated_at: '2026-05-18T00:00:00Z',
  last_login_at: null,
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

describe('Admin views', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    const users: UserListResponse = {
      items: [ADMIN_USER, REVIEWER_USER, FORECASTER_USER, VIEWER_USER],
      total: 4,
      page: 1,
      page_size: 20,
    }
    vi.mocked(getUsers).mockResolvedValue(ok(users))
    vi.mocked(createUser).mockResolvedValue(ok(ADMIN_USER))
    vi.mocked(updateUser).mockResolvedValue(ok(ADMIN_USER))
    vi.mocked(getConfig).mockResolvedValue(ok({ max_lead_hours: 240 }))
    vi.mocked(getConfigHistory).mockResolvedValue(
      ok({
        items: [
          {
            snapshot_id: 'snapshot-1',
            config_type: 'product_config',
            changed_by: 'admin',
            created_at: '2026-05-18T00:00:00Z',
          },
        ],
        total: 1,
      }),
    )
    vi.mocked(updateConfig).mockResolvedValue(
      ok({
        snapshot_id: 'snapshot-2',
        config_type: 'product_config',
        changed_by: 'admin',
        created_at: '2026-05-18T01:00:00Z',
      }),
    )
  })

  it('10.T1 UserManagement renders user table with role tags', async () => {
    const wrapper = mountWithPinia(UserManagementView)
    await flushPromises()

    const table = wrapper.find('[data-test="user-table"]')
    expect(table.text()).toContain('admin')
    expect(table.text()).toContain('管理员')
  })

  it('10.T1b UserManagement has exactly one table (no dual table bug)', async () => {
    const wrapper = mountWithPinia(UserManagementView)
    await flushPromises()

    const tables = wrapper.findAll('[data-test="user-table"]')
    expect(tables).toHaveLength(1)
    // No t-table component rendered
    expect(wrapper.findAll('t-table-stub').length).toBe(0)
  })

  it('10.T1c role tags use correct semantic colors', async () => {
    const wrapper = mountWithPinia(UserManagementView)
    await flushPromises()

    const html = wrapper.html()

    // admin = danger
    expect(html).toContain('theme="danger"')
    // reviewer = warning
    expect(html).toContain('theme="warning"')
    // forecaster = primary
    expect(html).toContain('theme="primary"')
    // viewer = default
    expect(html).toContain('theme="default"')

    // Verify role labels rendered
    const table = wrapper.find('[data-test="user-table"]')
    expect(table.text()).toContain('管理员')
    expect(table.text()).toContain('审核员')
    expect(table.text()).toContain('预报员')
    expect(table.text()).toContain('观察员')
  })

  it('10.T2 create user dialog validates required fields', async () => {
    const wrapper = mountWithPinia(UserManagementView)
    await flushPromises()

    await wrapper.find('[data-test="create-user-button"]').trigger('click')
    await wrapper.find('[data-test="user-submit"]').trigger('click')

    expect(wrapper.find('[data-test="user-form-error"]').text()).toContain('请填写完整用户信息')
    expect(createUser).not.toHaveBeenCalled()
  })

  it('10.T3 admin routes redirect non-admin to forbidden', async () => {
    for (const path of ['/admin/users', '/admin/config', '/admin/templates', '/admin/tasks', '/admin/storage']) {
      const route = routes.find((item) => item.path === path)
      expect(route?.meta?.roles).toEqual(['admin'])
    }

    setActivePinia(createPinia())
    const router = createAppRouter()
    const authStore = useAuthStore()
    authStore.token = 'jwt-token'
    authStore.user = {
      user_id: 'forecaster-1',
      username: 'forecaster',
      display_name: '预报员',
      role: 'forecaster',
    }

    await router.push('/admin/users')
    expect(router.currentRoute.value.path).toBe('/forbidden')
  })

  it('10.T4 ConfigManagement renders config tabs and history', async () => {
    const wrapper = mountWithPinia(ConfigManagementView)
    await flushPromises()

    expect(wrapper.find('[data-test="config-tabs"]').text()).toContain('产品配置')
    expect(wrapper.find('[data-test="config-tabs"]').text()).toContain('绘图配置')
    expect(wrapper.find('[data-test="config-history"]').text()).toContain('snapshot-1')
  })

  it('10.T4b ConfigManagement shows error on invalid JSON save', async () => {
    const wrapper = mountWithPinia(ConfigManagementView)
    await flushPromises()

    // Set invalid JSON
    const textarea = wrapper.find('[data-test="config-json"]')
    await textarea.setValue('{ invalid json }')
    await wrapper.find('[data-test="config-save"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-test="config-error"]').text()).toContain('JSON 格式不正确')
    expect(updateConfig).not.toHaveBeenCalled()
  })

  it('10.T4c ConfigManagement save button has loading state', async () => {
    // Make updateConfig hang to test loading state
    let resolveUpdate!: (v: unknown) => void
    vi.mocked(updateConfig).mockReturnValue(new Promise((r) => { resolveUpdate = r }))

    const wrapper = mountWithPinia(ConfigManagementView)
    await flushPromises()

    await wrapper.find('[data-test="config-save"]').trigger('click')

    // Button should have loading attribute while saving
    const btn = wrapper.find('[data-test="config-save"]')
    expect(btn.attributes('loading')).toBeDefined()

    // Resolve the promise to clean up
    resolveUpdate(ok({
      snapshot_id: 'snapshot-2',
      config_type: 'product_config',
      changed_by: 'admin',
      created_at: '2026-05-18T01:00:00Z',
    }))
    await flushPromises()
  })

  it('10.T5 TemplateManagement renders left-right layout', async () => {
    vi.mocked(getTemplates).mockResolvedValue(ok([
      {
        template_id: 'tpl-1',
        template_name: '降水模板',
        required_fields: ['qpf'],
        optional_fields: ['ptype'],
        allow_partial_success: false,
        review_time_policy: 'rolling',
        panel_count: 3,
      },
    ]))
    vi.mocked(getTemplate).mockResolvedValue(ok({
      template_id: 'tpl-1',
      template_name: '降水模板',
      required_fields: ['qpf'],
      optional_fields: ['ptype'],
      allow_partial_success: false,
      review_time_policy: 'rolling',
      panels: [{ id: 'p1', type: 'map', fields: ['qpf'] }],
    }))

    const pinia = createPinia()
    setActivePinia(pinia)
    const authStore = useAuthStore()
    authStore.token = 'jwt-token'
    authStore.user = { user_id: '1', username: 'admin', display_name: '管理员', role: 'admin' }

    const wrapper = mount(TemplateManagementView, {
      global: { plugins: [pinia], stubs: { AppHeader: true } },
    })
    await flushPromises()

    // Left panel: template list with name and panel count
    expect(wrapper.find('.template-list').text()).toContain('降水模板')
    expect(wrapper.find('.template-list').text()).toContain('3 个面板')

    // Right panel: template detail
    expect(wrapper.find('[data-test="template-detail"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="template-detail"]').text()).toContain('qpf')

    // Edit button visible for admin
    expect(wrapper.find('[data-test="template-edit"]').exists()).toBe(true)
  })

  it('10.T5b TemplateManagement hides edit button for non-admin', async () => {
    vi.mocked(getTemplates).mockResolvedValue(ok([
      {
        template_id: 'tpl-1',
        template_name: '降水模板',
        required_fields: ['qpf'],
        optional_fields: [],
        allow_partial_success: false,
        review_time_policy: 'rolling',
        panel_count: 3,
      },
    ]))
    vi.mocked(getTemplate).mockResolvedValue(ok({
      template_id: 'tpl-1',
      template_name: '降水模板',
      required_fields: ['qpf'],
      optional_fields: [],
      allow_partial_success: false,
      review_time_policy: 'rolling',
      panels: [{ id: 'p1', type: 'map', fields: ['qpf'] }],
    }))

    const pinia = createPinia()
    setActivePinia(pinia)
    const authStore = useAuthStore()
    authStore.token = 'jwt-token'
    authStore.user = { user_id: '3', username: 'forecaster', display_name: '预报员', role: 'forecaster' }

    const wrapper = mount(TemplateManagementView, {
      global: { plugins: [pinia], stubs: { AppHeader: true } },
    })
    await flushPromises()

    expect(wrapper.find('[data-test="template-edit"]').exists()).toBe(false)
  })

  it('10.T6 UserManagement page uses card-style workspace-panel', async () => {
    const wrapper = mountWithPinia(UserManagementView)
    await flushPromises()

    const panel = wrapper.find('.workspace-panel')
    expect(panel.exists()).toBe(true)
  })
})
