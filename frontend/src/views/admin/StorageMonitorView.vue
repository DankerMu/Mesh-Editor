<script setup lang="ts">
import { computed, onMounted } from 'vue'
import AppHeader from '@/components/AppHeader.vue'
import { useMonitorStore } from '@/stores/monitorStore'

const monitorStore = useMonitorStore()
const usagePercent = computed(() => {
  const summary = monitorStore.storageSummary
  if (!summary || summary.total_bytes <= 0) {
    return 0
  }
  return Math.round((summary.used_bytes / summary.total_bytes) * 100)
})

function formatDate(value?: string) {
  return value
    ? new Date(value).toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai', hour12: false })
    : '-'
}

onMounted(async () => {
  await monitorStore.fetchStorageSummary()
})
</script>

<template>
  <div class="page-shell">
    <AppHeader />
    <main class="content-wrap workspace-page">
      <section class="workspace-panel">
        <div class="workspace-panel__header">
          <div>
            <h1 class="workspace-title">存储监控</h1>
            <p class="workspace-desc">查看归档、临时、复盘等目录空间占用。</p>
          </div>
        </div>

        <t-card title="空间使用率" bordered data-test="storage-gauge">
          <t-progress :percentage="usagePercent" />
          <p>
            已用 {{ monitorStore.storageSummary?.used_gb ?? 0 }} GB /
            总量 {{ monitorStore.storageSummary?.total_gb ?? 0 }} GB
          </p>
          <p>最近扫描：{{ formatDate(monitorStore.storageSummary?.last_scan_at) }}</p>
        </t-card>

        <t-card title="目录明细" bordered>
          <table class="admin-table" data-test="storage-breakdown">
            <thead>
              <tr>
                <th>类型</th>
                <th>大小</th>
                <th>文件数</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in monitorStore.storageSummary?.breakdown ?? []" :key="item.type">
                <td>{{ item.type }}</td>
                <td>{{ item.size_gb }} GB</td>
                <td>{{ item.file_count }}</td>
              </tr>
            </tbody>
          </table>
        </t-card>
      </section>
    </main>
  </div>
</template>
