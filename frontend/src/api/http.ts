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
    const status = error.response?.status

    if (code === 'TOKEN_EXPIRED' || code === 'AUTH_REQUIRED' || code === 'USER_DISABLED' || (status === 401 && !code)) {
      const authStore = useAuthStore()
      authStore.logout()
      await router.push('/login')
    } else if (code === 'PERMISSION_DENIED') {
      await router.push('/forbidden')
    } else if ((typeof status === 'number' && status >= 500) || code === 'INTERNAL_ERROR') {
      await router.push('/error')
    }

    return Promise.reject(error)
  },
)

export default http
