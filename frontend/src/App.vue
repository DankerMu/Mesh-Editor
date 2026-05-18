<script setup lang="ts">
import zhCN from 'tdesign-vue-next/es/locale/zh_CN'
const globalLocale = { ...zhCN }
</script>

<template>
  <t-config-provider :global-locale="globalLocale">
    <RouterView />
  </t-config-provider>
</template>
