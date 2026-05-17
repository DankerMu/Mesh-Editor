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
  {
    path: '/editor',
    name: 'Editor',
    component: () => import('@/views/EditorView.vue'),
    meta: { roles: ['admin', 'reviewer', 'forecaster'] },
  },
  {
    path: '/editor/:windowId',
    name: 'EditorWindow',
    component: () => import('@/views/EditorView.vue'),
    meta: { roles: ['admin', 'reviewer', 'forecaster'] },
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

    const roles = to.meta.roles as string[] | undefined
    if (roles && (!authStore.role || !roles.includes(authStore.role))) {
      return '/forbidden'
    }

    return true
  })

  return router
}

const router = createAppRouter()

export default router
