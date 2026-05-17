<script setup lang="ts">
import AppHeader from '@/components/AppHeader.vue'
import CaseIdInput from '@/components/CaseIdInput.vue'
import ScanProgress from '@/components/ScanProgress.vue'
import WindowSelector from '@/components/WindowSelector.vue'
import { useWindowStore } from '@/stores/windowStore'

const windowStore = useWindowStore()

function triggerScan(caseId: string) {
  void windowStore.triggerScan(caseId)
}
</script>

<template>
  <div class="page-shell">
    <AppHeader />
    <main class="content-wrap workspace-page">
      <section class="workspace-panel" aria-labelledby="workspace-title">
        <div class="workspace-panel__header">
          <div>
            <h1 id="workspace-title" class="workspace-title">数据窗口工作台</h1>
            <p class="workspace-desc">输入起报时次后扫描资料，选择可编辑的累计降水窗口。</p>
          </div>
        </div>
        <CaseIdInput @submit="triggerScan" />
        <ScanProgress />
        <WindowSelector />
      </section>
    </main>
  </div>
</template>
