import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { AxiosAdapter, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { AxiosHeaders } from 'axios'
import { createPinia, setActivePinia } from 'pinia'
import http from '@/api/http'
import { useAuthStore } from '@/stores/authStore'
import router from '@/router'

vi.mock('@/router', () => ({
  default: {
    push: vi.fn(),
  },
}))

describe('http client', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.mocked(router.push).mockReset()
  })

  it('T5.6 请求注入 Authorization Bearer header', async () => {
    const authStore = useAuthStore()
    authStore.token = 'jwt-token'

    const adapter: AxiosAdapter = async (config: InternalAxiosRequestConfig) =>
      ({
        data: {},
        status: 200,
        statusText: 'OK',
        headers: {},
        config,
      }) satisfies AxiosResponse
    http.defaults.adapter = adapter

    const response = await http.get('/health')
    const headers = AxiosHeaders.from(response.config.headers)

    expect(headers.get('Authorization')).toBe('Bearer jwt-token')
  })

  it('T5.7 请求附带 X-Trace-ID header', async () => {
    const adapter: AxiosAdapter = async (config: InternalAxiosRequestConfig) =>
      ({
        data: {},
        status: 200,
        statusText: 'OK',
        headers: {},
        config,
      }) satisfies AxiosResponse
    http.defaults.adapter = adapter

    const response = await http.get('/health')
    const headers = AxiosHeaders.from(response.config.headers)

    expect(headers.get('X-Trace-ID')).toBeTruthy()
  })

  it('T5.7b 403 PERMISSION_DENIED 路由到 /forbidden', async () => {
    const adapter: AxiosAdapter = async (config: InternalAxiosRequestConfig) =>
      Promise.reject({
        response: {
          data: { code: 'PERMISSION_DENIED', message: '权限不足' },
          status: 403,
          statusText: 'Forbidden',
          headers: {},
          config,
        },
      })
    http.defaults.adapter = adapter

    await expect(http.get('/protected')).rejects.toBeTruthy()

    expect(router.push).toHaveBeenCalledWith('/forbidden')
  })

  it('T5.7c 5xx 路由到 /error', async () => {
    const adapter: AxiosAdapter = async (config: InternalAxiosRequestConfig) =>
      Promise.reject({
        response: {
          data: { code: 'UNKNOWN', message: '服务器错误' },
          status: 500,
          statusText: 'Internal Server Error',
          headers: {},
          config,
        },
      })
    http.defaults.adapter = adapter

    await expect(http.get('/protected')).rejects.toBeTruthy()

    expect(router.push).toHaveBeenCalledWith('/error')
  })

  it('T5.7d USER_DISABLED 清空登录态并路由到 /login', async () => {
    const authStore = useAuthStore()
    authStore.token = 'jwt-token'
    authStore.user = {
      user_id: 'user-1',
      username: 'admin',
      display_name: '管理员',
      role: 'admin',
    }
    localStorage.setItem('token', 'jwt-token')
    localStorage.setItem('user', JSON.stringify(authStore.user))
    const adapter: AxiosAdapter = async (config: InternalAxiosRequestConfig) =>
      Promise.reject({
        response: {
          data: { code: 'USER_DISABLED', message: '账号已禁用' },
          status: 403,
          statusText: 'Forbidden',
          headers: {},
          config,
        },
      })
    http.defaults.adapter = adapter

    await expect(http.get('/protected')).rejects.toBeTruthy()

    expect(authStore.token).toBeNull()
    expect(authStore.user).toBeNull()
    expect(localStorage.getItem('token')).toBeNull()
    expect(localStorage.getItem('user')).toBeNull()
    expect(router.push).toHaveBeenCalledWith('/login')
  })
})
