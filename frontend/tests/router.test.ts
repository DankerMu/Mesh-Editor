import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { createAppRouter } from '@/router'
import { useAuthStore } from '@/stores/authStore'

describe('router guards', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('T5.8 未认证访问受保护路由重定向到 /login', async () => {
    const router = createAppRouter()

    await router.push('/')

    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('T5.9 已认证访问 /login 重定向到 /', async () => {
    const router = createAppRouter()
    const authStore = useAuthStore()
    authStore.token = 'jwt-token'

    await router.push('/login')

    expect(router.currentRoute.value.path).toBe('/')
  })
})
