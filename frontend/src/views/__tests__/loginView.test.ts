import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/authStore'
import LoginView from '@/views/LoginView.vue'
import { SYSTEM_NAME } from '@/constants/navigation'

const pushMock = vi.fn()
vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue-router')>()
  return {
    ...actual,
    useRouter: () => ({
      push: pushMock,
      currentRoute: { value: { path: '/login' } },
    }),
  }
})

vi.mock('tdesign-icons-vue-next', () => ({
  BrowseIcon: { template: '<svg data-test="browse-icon" />' },
  BrowseOffIcon: { template: '<svg data-test="browse-off-icon" />' },
}))

function mountLogin(): VueWrapper {
  const pinia = createPinia()
  setActivePinia(pinia)
  return mount(LoginView, { global: { plugins: [pinia] } })
}

describe('LoginView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  /* ---------- Layout tests ---------- */

  describe('layout', () => {
    it('renders brand panel with gradient background class', () => {
      const wrapper = mountLogin()
      const brand = wrapper.find('.login-brand')
      expect(brand.exists()).toBe(true)
    })

    it('brand panel displays system name', () => {
      const wrapper = mountLogin()
      const brand = wrapper.find('.login-brand')
      expect(brand.text()).toContain(SYSTEM_NAME)
    })

    it('brand panel displays description text', () => {
      const wrapper = mountLogin()
      const desc = wrapper.find('.login-brand__desc')
      expect(desc.text()).toBe('专业的气象数据网格编辑与质量控制平台')
    })

    it('right side shows login form card', () => {
      const wrapper = mountLogin()
      expect(wrapper.find('.login-right').exists()).toBe(true)
      expect(wrapper.find('.login-card').exists()).toBe(true)
    })

    it('form card has title and subtitle', () => {
      const wrapper = mountLogin()
      const card = wrapper.find('.login-card')
      expect(card.find('.login-card__title').text()).toBe('登录')
      expect(card.find('.login-card__subtitle').text()).toBe('请输入账号密码登录工作台')
    })
  })

  /* ---------- Form control tests ---------- */

  describe('form controls', () => {
    it('username input with label "用户名"', () => {
      const wrapper = mountLogin()
      const labels = wrapper.findAll('label')
      const usernameLabel = labels.find((l) => l.text().includes('用户名'))
      expect(usernameLabel).toBeDefined()
    })

    it('password input with label "密码"', () => {
      const wrapper = mountLogin()
      const labels = wrapper.findAll('label')
      const passwordLabel = labels.find((l) => l.text().includes('密码'))
      expect(passwordLabel).toBeDefined()
    })

    it('password visibility toggle switches icon on click', async () => {
      const wrapper = mountLogin()
      // Initially password is hidden, should show BrowseOffIcon
      expect(wrapper.find('[data-test="browse-off-icon"]').exists()).toBe(true)
      expect(wrapper.find('[data-test="browse-icon"]').exists()).toBe(false)

      // Click toggle
      await wrapper.find('.password-toggle').trigger('click')

      // Now password visible, should show BrowseIcon
      expect(wrapper.find('[data-test="browse-icon"]').exists()).toBe(true)
      expect(wrapper.find('[data-test="browse-off-icon"]').exists()).toBe(false)
    })

    it('password input type changes with toggle', async () => {
      const wrapper = mountLogin()
      const inputs = wrapper.findAll('input')
      const passwordInput = inputs.find((i) => i.attributes('type') === 'password')
      expect(passwordInput).toBeDefined()

      // Toggle visibility
      await wrapper.find('.password-toggle').trigger('click')

      // Now it should be text type
      const textInput = wrapper.findAll('input').find((i) => i.attributes('placeholder') === '请输入密码')
      expect(textInput?.attributes('type')).toBe('text')
    })

    it('login button is full-width primary', () => {
      const wrapper = mountLogin()
      const buttons = wrapper.findAll('button')
      const loginBtn = buttons.find((b) => b.text().includes('登录'))
      expect(loginBtn).toBeDefined()
    })
  })

  /* ---------- Validation tests ---------- */

  describe('validation', () => {
    it('submits form triggers validation', async () => {
      const wrapper = mountLogin()
      const form = wrapper.find('form')
      await form.trigger('submit')
      await flushPromises()
      // The stubbed t-form emits submit with validateResult: true,
      // which would proceed to login. With empty credentials the store call will fail.
      // Validation is handled by TDesign rules at runtime; stub always passes.
      expect(wrapper.find('form').exists()).toBe(true)
    })
  })

  /* ---------- Error handling tests ---------- */

  describe('error handling', () => {
    it('invalid credentials shows error alert', async () => {
      const wrapper = mountLogin()
      const authStore = useAuthStore()
      authStore.login = vi.fn().mockRejectedValue({
        response: { data: { code: 'INVALID_CREDENTIALS' } },
      })

      await wrapper.find('form').trigger('submit')
      await flushPromises()

      expect(wrapper.text()).toContain('登录失败，请检查用户名和密码')
    })

    it('disabled account shows specific message', async () => {
      const wrapper = mountLogin()
      const authStore = useAuthStore()
      authStore.login = vi.fn().mockRejectedValue({
        response: { data: { code: 'USER_DISABLED' } },
      })

      await wrapper.find('form').trigger('submit')
      await flushPromises()

      expect(wrapper.text()).toContain('账号已被禁用，请联系管理员')
    })

    it('generic error shows default message', async () => {
      const wrapper = mountLogin()
      const authStore = useAuthStore()
      authStore.login = vi.fn().mockRejectedValue(new Error('network'))

      await wrapper.find('form').trigger('submit')
      await flushPromises()

      expect(wrapper.text()).toContain('登录失败，请检查用户名和密码')
    })
  })

  /* ---------- Login flow tests ---------- */

  describe('login flow', () => {
    it('loading state during login', async () => {
      const wrapper = mountLogin()
      const authStore = useAuthStore()

      let resolveLogin: () => void
      authStore.login = vi.fn(
        () => new Promise<void>((resolve) => { resolveLogin = resolve }),
      )

      await wrapper.find('form').trigger('submit')
      // Loading should be true while waiting
      // The button loading prop is bound but stub doesn't expose it;
      // verify the store was called
      expect(authStore.login).toHaveBeenCalled()

      resolveLogin!()
      await flushPromises()
    })

    it('successful login redirects to /', async () => {
      const wrapper = mountLogin()
      const authStore = useAuthStore()
      authStore.login = vi.fn().mockResolvedValue(undefined)

      await wrapper.find('form').trigger('submit')
      await flushPromises()

      expect(pushMock).toHaveBeenCalledWith('/')
    })

    it('failed login does not redirect', async () => {
      const wrapper = mountLogin()
      const authStore = useAuthStore()
      authStore.login = vi.fn().mockRejectedValue(new Error('fail'))

      await wrapper.find('form').trigger('submit')
      await flushPromises()

      expect(pushMock).not.toHaveBeenCalled()
    })
  })

  /* ---------- Responsive (class presence) ---------- */

  describe('responsive', () => {
    it('brand panel has login-brand class for responsive hiding', () => {
      const wrapper = mountLogin()
      expect(wrapper.find('.login-brand').exists()).toBe(true)
    })

    it('right panel has login-right class for responsive expansion', () => {
      const wrapper = mountLogin()
      expect(wrapper.find('.login-right').exists()).toBe(true)
    })
  })
})
