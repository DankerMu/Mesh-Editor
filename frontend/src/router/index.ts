import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true },
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomeView.vue'),
  },
  {
    path: '/forbidden',
    name: 'Forbidden',
    component: () => import('@/views/ForbiddenView.vue'),
    meta: { public: true },
  },
  {
    path: '/error',
    name: 'Error',
    component: () => import('@/views/ErrorView.vue'),
    meta: { public: true },
  },
]

export function createAppRouter() {
  const router = createRouter({
    history: createWebHistory(),
    routes,
  })

  router.beforeEach((to) => {
    const authStore = useAuthStore()

    if (!to.meta.public && !authStore.isAuthenticated) {
      return '/login'
    }

    if (to.path === '/login' && authStore.isAuthenticated) {
      return '/'
    }

    return true
  })

  return router
}

const router = createAppRouter()

export default router
