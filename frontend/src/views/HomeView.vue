<script setup lang="ts">
import AppHeader from '@/components/AppHeader.vue'
import { TOP_MENUS } from '@/constants/navigation'
</script>

<template>
  <div class="page-shell">
    <AppHeader />
    <main class="content-wrap empty-workbench">
      <section class="workspace-panel" aria-labelledby="workspace-title">
        <h1 id="workspace-title" class="workspace-title">空白工作台</h1>
        <p class="workspace-desc">{{ TOP_MENUS.join(' / ') }} 模块已预留，后续阶段接入业务功能。</p>
      </section>
    </main>
  </div>
</template>
