<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import CaseIdInput from '@/components/CaseIdInput.vue'
import ScanProgress from '@/components/ScanProgress.vue'
import WindowSelector from '@/components/WindowSelector.vue'
import { useWindowStore } from '@/stores/windowStore'
import { useAuthStore } from '@/stores/authStore'

const router = useRouter()
const windowStore = useWindowStore()
const authStore = useAuthStore()

const lastCaseId = ref('')

const scanning = computed(() => windowStore.scanPolling)
const permissionDenied = computed(() => {
  const role = authStore.user?.role
  // Backend allows admin + reviewer; deny viewer + forecaster
  return role !== 'admin' && role !== 'reviewer'
})

async function triggerScan(caseId: string) {
  lastCaseId.value = caseId
  try {
    await windowStore.triggerScan(caseId)
  } catch {
    // windowStore already handles error state internally
    // This catch prevents unhandled rejection
  }
}

function handleRetry() {
  if (lastCaseId.value && !permissionDenied.value) {
    triggerScan(lastCaseId.value)
  }
}

watch(
  () => windowStore.selectedWindowId,
  (windowId) => {
    if (windowId) {
      void router.push(`/editor/${windowId}`)
    }
  },
)
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
        <CaseIdInput :scanning="scanning" :permission-denied="permissionDenied" @submit="triggerScan" />
        <ScanProgress @retry="handleRetry" />
        <WindowSelector />
      </section>
    </main>
  </div>
</template>
