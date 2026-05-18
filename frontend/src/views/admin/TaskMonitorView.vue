<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import AppHeader from '@/components/AppHeader.vue'
import { useMonitorStore } from '@/stores/monitorStore'

const monitorStore = useMonitorStore()

const cards = computed(() => [
  { label: '等待中', value: monitorStore.taskSummary?.counts.pending ?? 0, bg: 'var(--color-primary-bg)', color: 'var(--color-primary)' },
  { label: '运行中', value: monitorStore.taskSummary?.counts.running ?? 0, bg: 'var(--color-primary-bg)', color: 'var(--color-primary)' },
  { label: '成功', value: monitorStore.taskSummary?.counts.success ?? 0, bg: 'var(--color-success-bg)', color: 'var(--color-success)' },
  {
    label: '失败',
    value:
      (monitorStore.taskSummary?.counts.failed ?? 0) +
      (monitorStore.taskSummary?.counts.permanently_failed ?? 0),
    bg: 'var(--color-danger-bg)',
    color: 'var(--color-danger)',
  },
])

function formatDate(value: string | null) {
  return value
    ? new Date(value).toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai', hour12: false })
    : '-'
}

async function retry(reviewId: string) {
  await monitorStore.retryTask(reviewId)
  MessagePlugin.success('任务已重试')
}

onMounted(async () => {
  await monitorStore.fetchTaskSummary()
  monitorStore.startAutoRefresh()
})

onBeforeUnmount(() => {
  monitorStore.stopAutoRefresh()
})
</script>

<template>
  <div class="page-shell">
    <AppHeader />
    <main class="content-wrap workspace-page">
      <section class="workspace-panel">
        <div class="workspace-panel__header">
          <div>
            <h1 class="workspace-title">任务监控</h1>
            <p class="workspace-desc">每 10 秒自动刷新绘图任务状态。</p>
          </div>
        </div>

        <section class="ops-summary" data-test="task-summary-cards">
          <t-card
            v-for="card in cards"
            :key="card.label"
            class="ops-summary-card"
            :style="{ background: card.bg }"
            bordered
            :data-test="`stat-card-${card.label}`"
          >
            <span>{{ card.label }}</span>
            <strong :style="{ color: card.color }">{{ card.value }}</strong>
          </t-card>
        </section>

        <t-card title="失败任务" bordered>
          <table class="admin-table" data-test="failed-task-table">
            <thead>
              <tr>
                <th>任务ID</th>
                <th>窗口</th>
                <th>错误</th>
                <th>失败时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="task in monitorStore.taskSummary?.recent_failed ?? []" :key="task.review_id">
                <td>{{ task.review_id }}</td>
                <td>{{ task.window_id }}</td>
                <td>{{ task.error_summary ?? '-' }}</td>
                <td>{{ formatDate(task.failed_at) }}</td>
                <td>
                  <t-button :data-test="`retry-${task.review_id}`" @click="retry(task.review_id)">
                    重试
                  </t-button>
                </td>
              </tr>
            </tbody>
          </table>
        </t-card>
      </section>
    </main>
  </div>
</template>
