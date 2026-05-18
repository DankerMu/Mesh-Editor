<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { ADMIN_NAV_ITEMS, SYSTEM_NAME, TOP_NAV_ITEMS } from '@/constants/navigation'

const router = useRouter()
const authStore = useAuthStore()

const displayName = computed(
  () => authStore.user?.display_name ?? authStore.user?.username ?? '当前用户',
)

const activeMenu = computed(() => {
  const path = router.currentRoute.value.path

  const topMatch = TOP_NAV_ITEMS.find((item) => item.path === path)
  if (topMatch) {
    return topMatch.path
  }

  const adminMatch = ADMIN_NAV_ITEMS.find((item) => item.path === path)
  if (adminMatch) {
    return adminMatch.path
  }

  if (path === '/' || path.startsWith('/editor')) {
    return '/'
  }

  return ''
})

const isAdmin = computed(() => authStore.role === 'admin')

function navigate(path: string) {
  router.push(path)
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <header class="app-header">
    <div class="content-wrap app-header__inner">
      <div class="app-header__brand">
        <svg
          class="app-header__logo"
          viewBox="0 0 24 24"
          width="28"
          height="28"
          fill="none"
          aria-hidden="true"
        >
          <path
            d="M6.5 18.5C3.46 18.5 1 16.04 1 13c0-2.54 1.73-4.67 4.07-5.28A6.5 6.5 0 0 1 17.5 7c0 .13 0 .26-.02.39A5 5 0 0 1 19 17.5H6.5Z"
            fill="var(--color-primary)"
            opacity="0.18"
          />
          <path
            d="M6.5 18.5C3.46 18.5 1 16.04 1 13c0-2.54 1.73-4.67 4.07-5.28A6.5 6.5 0 0 1 17.5 7c0 .13 0 .26-.02.39A5 5 0 0 1 19 17.5H6.5Z"
            stroke="var(--color-primary)"
            stroke-width="1.5"
            stroke-linecap="round"
            stroke-linejoin="round"
            fill="none"
          />
        </svg>
        <span class="app-header__title">{{ SYSTEM_NAME }}</span>
      </div>

      <t-head-menu
        class="app-header__menu"
        :value="activeMenu"
        theme="light"
      >
        <t-menu-item
          v-for="item in TOP_NAV_ITEMS"
          :key="item.path"
          :value="item.path"
          @click="navigate(item.path)"
        >
          {{ item.label }}
        </t-menu-item>

        <t-submenu value="system-admin" title="系统管理">
          <template v-if="isAdmin">
            <t-menu-item
              v-for="item in ADMIN_NAV_ITEMS"
              :key="item.path"
              :value="item.path"
              @click="navigate(item.path)"
            >
              {{ item.label }}
            </t-menu-item>
          </template>
          <t-menu-item v-else value="no-access" disabled>
            无权限访问
          </t-menu-item>
        </t-submenu>
      </t-head-menu>

      <div class="app-header__user">
        <span>{{ displayName }}</span>
        <t-button theme="default" variant="outline" size="small" @click="handleLogout">
          退出
        </t-button>
      </div>
    </div>
  </header>
</template>
