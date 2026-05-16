import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/authStore'
import LoginView from '@/views/LoginView.vue'
import ForbiddenView from '@/views/ForbiddenView.vue'
import ErrorView from '@/views/ErrorView.vue'

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue-router')>()

  return {
    ...actual,
    useRouter: () => ({
      push: vi.fn(),
    }),
  }
})

describe('views', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('T5.10 LoginView 渲染中文文案', () => {
    const wrapper = mount(LoginView, {
      global: {
        plugins: [createPinia()],
      },
    })

    expect(wrapper.text()).toContain('降水相态网格编辑系统')
    expect(wrapper.text()).toContain('用户名')
    expect(wrapper.text()).toContain('密码')
    expect(wrapper.text()).toContain('登录')
  })

  it('T5.10b LoginView 不展示 backend 原始登录错误 message', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const authStore = useAuthStore()
    vi.spyOn(authStore, 'login').mockRejectedValue({
      response: {
        data: {
          code: 'AUTH_REQUIRED',
          message: '用户 admin 不存在或密码错误',
        },
      },
    })
    const wrapper = mount(LoginView, {
      global: {
        plugins: [pinia],
      },
    })

    await wrapper.find('form').trigger('submit')

    expect(wrapper.text()).toContain('登录失败，请检查用户名和密码')
    expect(wrapper.text()).not.toContain('用户 admin 不存在或密码错误')
  })

  it('T5.10c LoginView 对 USER_DISABLED 展示固定禁用提示', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const authStore = useAuthStore()
    vi.spyOn(authStore, 'login').mockRejectedValue({
      response: {
        data: {
          code: 'USER_DISABLED',
          message: '账号 admin 已被禁用',
        },
      },
    })
    const wrapper = mount(LoginView, {
      global: {
        plugins: [pinia],
      },
    })

    await wrapper.find('form').trigger('submit')

    expect(wrapper.text()).toContain('账号已被禁用，请联系管理员')
    expect(wrapper.text()).not.toContain('账号 admin 已被禁用')
  })

  it('T5.11 ForbiddenView 渲染中文权限不足', () => {
    const wrapper = mount(ForbiddenView, {
      global: {
        mocks: {
          $router: { push: () => undefined },
        },
      },
    })

    expect(wrapper.text()).toContain('权限不足')
  })

  it('T5.12 ErrorView 渲染中文服务错误', () => {
    const wrapper = mount(ErrorView, {
      global: {
        mocks: {
          $router: { push: () => undefined },
        },
      },
    })

    expect(wrapper.text()).toContain('服务器错误')
  })
})
