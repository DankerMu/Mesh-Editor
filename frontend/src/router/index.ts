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
  {
    path: '/approval',
    name: 'approval',
    component: () => import('@/views/ApprovalView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/review',
    name: 'review',
    component: () => import('@/views/ReviewCenterView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/analysis/operations',
    name: 'analysis-operations',
    component: () => import('@/views/StatsView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/admin/users',
    name: 'admin-users',
    component: () => import('@/views/admin/UserManagementView.vue'),
    meta: { roles: ['admin'] },
  },
  {
    path: '/admin/config',
    name: 'admin-config',
    component: () => import('@/views/admin/ConfigManagementView.vue'),
    meta: { roles: ['admin'] },
  },
  {
    path: '/admin/templates',
    name: 'admin-templates',
    component: () => import('@/views/admin/TemplateManagementView.vue'),
    meta: { roles: ['admin'] },
  },
  {
    path: '/admin/tasks',
    name: 'admin-tasks',
    component: () => import('@/views/admin/TaskMonitorView.vue'),
    meta: { roles: ['admin'] },
  },
  {
    path: '/admin/storage',
    name: 'admin-storage',
    component: () => import('@/views/admin/StorageMonitorView.vue'),
    meta: { roles: ['admin'] },
  },
  {
    path: '/admin/audit',
    name: 'admin-audit',
    component: () => import('@/views/admin/AuditLogView.vue'),
    meta: { roles: ['admin'] },
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
