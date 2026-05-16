import { beforeEach, describe, expect, it } from 'vitest'
import type { AxiosAdapter, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { createPinia, setActivePinia } from 'pinia'
import http from '@/api/http'
import { useAuthStore } from '@/stores/authStore'

describe('authStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('T5.3 初始状态 isAuthenticated=false, token=null, user=null', () => {
    const authStore = useAuthStore()

    expect(authStore.isAuthenticated).toBe(false)
    expect(authStore.token).toBeNull()
    expect(authStore.user).toBeNull()
  })

  it('T5.4 login 后 isAuthenticated=true, token 有值, user/role 已设', async () => {
    const adapter: AxiosAdapter = async (config: InternalAxiosRequestConfig) =>
      ({
        data: {
          code: 'OK',
          message: 'success',
          data: {
            user_id: 'user-1',
            username: 'admin',
            display_name: '管理员',
            role: 'admin',
            token: 'jwt-token',
            expires_at: '2026-05-17T00:00:00+08:00',
          },
          trace_id: 'trace-id',
        },
        status: 200,
        statusText: 'OK',
        headers: {},
        config,
      }) satisfies AxiosResponse
    http.defaults.adapter = adapter

    const authStore = useAuthStore()
    await authStore.login('admin', 'password')

    expect(authStore.isAuthenticated).toBe(true)
    expect(authStore.token).toBe('jwt-token')
    expect(authStore.user).toEqual({
      user_id: 'user-1',
      username: 'admin',
      display_name: '管理员',
      role: 'admin',
    })
    expect(authStore.role).toBe('admin')
    expect(localStorage.getItem('token')).toBe('jwt-token')
    expect(JSON.parse(localStorage.getItem('user') ?? '{}')).toEqual({
      user_id: 'user-1',
      username: 'admin',
      display_name: '管理员',
      role: 'admin',
    })
  })

  it('T5.5 restoreSession 恢复 token 与 user 元数据', () => {
    localStorage.setItem('token', 'jwt-token')
    localStorage.setItem(
      'user',
      JSON.stringify({
        user_id: 'user-1',
        username: 'admin',
        display_name: '管理员',
        role: 'admin',
      }),
    )

    const authStore = useAuthStore()
    authStore.restoreSession()

    expect(authStore.isAuthenticated).toBe(true)
    expect(authStore.token).toBe('jwt-token')
    expect(authStore.user).toEqual({
      user_id: 'user-1',
      username: 'admin',
      display_name: '管理员',
      role: 'admin',
    })
    expect(authStore.role).toBe('admin')
  })

  it('T5.5b logout 后状态与持久化缓存清空', () => {
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

    authStore.logout()

    expect(authStore.isAuthenticated).toBe(false)
    expect(authStore.token).toBeNull()
    expect(authStore.user).toBeNull()
    expect(localStorage.getItem('token')).toBeNull()
    expect(localStorage.getItem('user')).toBeNull()
  })
})
