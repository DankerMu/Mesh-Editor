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
const activeMenu = computed(
  () =>
    [...TOP_NAV_ITEMS, ...ADMIN_NAV_ITEMS].find((item) => item.path === router.currentRoute.value.path)
      ?.label ?? '网格编辑',
)
const showAdminNav = computed(() => authStore.role === 'admin')

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
      <div class="app-header__brand">{{ SYSTEM_NAME }}</div>
      <t-menu class="app-header__menu" theme="light" mode="horizontal" :value="activeMenu">
        <t-menu-item
          v-for="item in TOP_NAV_ITEMS"
          :key="item.label"
          :value="item.label"
          @click="navigate(item.path)"
        >
          {{ item.label }}
        </t-menu-item>
        <template v-if="showAdminNav">
          <t-menu-item value="系统管理" disabled class="app-header__menu-group">
            系统管理
          </t-menu-item>
          <t-menu-item
            v-for="item in ADMIN_NAV_ITEMS"
            :key="item.label"
            :value="item.label"
            @click="navigate(item.path)"
          >
            {{ item.label }}
          </t-menu-item>
        </template>
      </t-menu>
      <div class="app-header__user">
        <span>{{ displayName }}</span>
        <t-button theme="default" variant="outline" size="small" @click="handleLogout"
          >退出</t-button
        >
      </div>
    </div>
  </header>
</template>
