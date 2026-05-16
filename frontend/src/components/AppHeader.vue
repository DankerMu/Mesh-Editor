<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { SYSTEM_NAME, TOP_MENUS } from '@/constants/navigation'

const router = useRouter()
const authStore = useAuthStore()

const displayName = computed(
  () => authStore.user?.display_name ?? authStore.user?.username ?? '当前用户',
)

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <header class="app-header">
    <div class="content-wrap app-header__inner">
      <div class="app-header__brand">{{ SYSTEM_NAME }}</div>
      <t-menu class="app-header__menu" theme="light" mode="horizontal" value="网格编辑">
        <t-menu-item v-for="menu in TOP_MENUS" :key="menu" :value="menu">
          {{ menu }}
        </t-menu-item>
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
