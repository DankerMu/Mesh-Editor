import axios from 'axios'
import type { InternalAxiosRequestConfig } from 'axios'
import { useAuthStore } from '@/stores/authStore'
import router from '@/router'

const http = axios.create({ baseURL: '/api' })

function createTraceId() {
  return globalThis.crypto?.randomUUID?.() ?? Date.now().toString(36)
}

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const authStore = useAuthStore()

  if (authStore.token) {
    config.headers.set('Authorization', `Bearer ${authStore.token}`)
  }

  config.headers.set('X-Trace-ID', createTraceId())
  return config
})

http.interceptors.response.use(
  (response) => response,
  async (error) => {
    const code = error.response?.data?.code

    if (code === 'TOKEN_EXPIRED' || code === 'AUTH_REQUIRED') {
      const authStore = useAuthStore()
      authStore.logout()
      await router.push('/login')
    }

    return Promise.reject(error)
  },
)

export default http
