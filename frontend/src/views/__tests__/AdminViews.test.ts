import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createAppRouter, routes } from '@/router'
import { createUser, getUsers, updateUser } from '@/api/admin'
import { getConfig, getConfigHistory, updateConfig } from '@/api/config'
import { useAuthStore } from '@/stores/authStore'
import ConfigManagementView from '@/views/admin/ConfigManagementView.vue'
import UserManagementView from '@/views/admin/UserManagementView.vue'
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

vi.mock('tdesign-vue-next', () => ({
  MessagePlugin: { success: vi.fn() },
}))

const USER: UserItem = {
  id: 1,
  username: 'admin',
  display_name: '管理员',
  role: 'admin',
  is_active: true,
  created_at: '2026-05-18T00:00:00Z',
  updated_at: '2026-05-18T00:00:00Z',
  last_login_at: '2026-05-18T01:00:00Z',
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
    const users: UserListResponse = { items: [USER], total: 1, page: 1, page_size: 20 }
    vi.mocked(getUsers).mockResolvedValue(ok(users))
    vi.mocked(createUser).mockResolvedValue(ok(USER))
    vi.mocked(updateUser).mockResolvedValue(ok(USER))
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
})
