import { beforeEach, describe, expect, it } from 'vitest'
import type { AxiosAdapter, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { AxiosHeaders } from 'axios'
import { createPinia, setActivePinia } from 'pinia'
import http from '@/api/http'
import { useAuthStore } from '@/stores/authStore'

describe('http client', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
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
})
