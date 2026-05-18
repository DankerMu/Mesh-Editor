import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/authStore'
import AppHeader from '@/components/AppHeader.vue'
import { ADMIN_NAV_ITEMS, SYSTEM_NAME, TOP_NAV_ITEMS } from '@/constants/navigation'

// Mock vue-router – keep createRouter/createWebHistory so router/index.ts can initialise
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

function mountHeader(role = 'admin') {
  const pinia = createPinia()
  setActivePinia(pinia)

  const authStore = useAuthStore()
  authStore.token = 'jwt-token'
  authStore.user = {
    user_id: 'u-1',
    username: 'testuser',
    display_name: 'Test User',
    role,
  }

  return mount(AppHeader, {
    global: {
      plugins: [pinia],
    },
  })
}

describe('AppHeader', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders system name', () => {
    const wrapper = mountHeader()
    expect(wrapper.text()).toContain(SYSTEM_NAME)
  })

  it('renders all 5 navigation items', () => {
    const wrapper = mountHeader()
    const text = wrapper.text()

    for (const item of TOP_NAV_ITEMS) {
      expect(text).toContain(item.label)
    }
    // system-admin submenu title
    expect(text).toContain('系统管理')
  })

  it('renders logout button', () => {
    const wrapper = mountHeader()
    expect(wrapper.text()).toContain('退出')
  })

  it('admin user sees all 6 submenu items', () => {
    const wrapper = mountHeader('admin')
    const text = wrapper.text()

    for (const item of ADMIN_NAV_ITEMS) {
      expect(text).toContain(item.label)
    }
    expect(ADMIN_NAV_ITEMS).toHaveLength(6)
  })

  it('non-admin user sees no-access message', () => {
    const wrapper = mountHeader('forecaster')
    expect(wrapper.text()).toContain('无权限访问')
  })
})
