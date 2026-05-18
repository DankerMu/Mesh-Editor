<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import AppHeader from '@/components/AppHeader.vue'
import { getAuditLogs } from '@/api/audit'
import type { AuditLogItem } from '@/api/audit'

const logs = ref<AuditLogItem[]>([])
const total = ref(0)
const expandedId = ref<number | null>(null)
const filters = reactive({
  user_id: '',
  action: '',
  resource_type: '',
  dateRange: [] as string[],
  page: 1,
  page_size: 20,
})

function formatDate(value: string) {
  return new Date(value).toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai', hour12: false })
}

function detailText(value: string | null) {
  if (!value) {
    return '-'
  }
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

async function fetchLogs() {
  const response = await getAuditLogs({
    user_id: filters.user_id ? Number(filters.user_id) : undefined,
    action: filters.action || undefined,
    resource_type: filters.resource_type || undefined,
    start_date: filters.dateRange[0],
    end_date: filters.dateRange[1],
    page: filters.page,
    page_size: filters.page_size,
  })
  logs.value = response.data.items
  total.value = response.data.total
}

onMounted(async () => {
  await fetchLogs()
})
</script>

<template>
  <div class="page-shell">
    <AppHeader />
    <main class="content-wrap workspace-page">
      <section class="workspace-panel">
        <div class="workspace-panel__header">
          <div>
            <h1 class="workspace-title">审计日志</h1>
            <p class="workspace-desc">按用户、动作、资源类型和时间筛选操作记录。</p>
          </div>
        </div>

        <t-card bordered>
          <div class="ops-filter-grid" data-test="audit-filters">
            <t-input v-model="filters.user_id" placeholder="用户ID" data-test="audit-user-filter" />
            <t-input v-model="filters.action" placeholder="动作" data-test="audit-action-filter" />
            <t-input
              v-model="filters.resource_type"
              placeholder="资源类型"
              data-test="audit-resource-filter"
            />
            <t-date-range-picker v-model="filters.dateRange" data-test="audit-date-filter" />
            <t-button data-test="audit-search" @click="fetchLogs">查询</t-button>
          </div>
        </t-card>

        <table class="admin-table" data-test="audit-table">
          <thead>
            <tr>
              <th>时间</th>
              <th>用户</th>
              <th>动作</th>
              <th>资源</th>
              <th>摘要</th>
            </tr>
          </thead>
          <tbody>
            <template v-for="log in logs" :key="log.id">
              <tr>
                <td>{{ formatDate(log.created_at) }}</td>
                <td>{{ log.username }}</td>
                <td>{{ log.action }}</td>
                <td>{{ log.resource_type ?? '-' }}</td>
                <td>
                  <button type="button" :data-test="`audit-expand-${log.id}`" @click="expandedId = expandedId === log.id ? null : log.id">
                    查看详情
                  </button>
                </td>
              </tr>
              <tr v-if="expandedId === log.id">
                <td colspan="5">
                  <pre>{{ detailText(log.detail_json) }}</pre>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
        <t-pagination
          :total="total"
          :page-size="filters.page_size"
          :current="filters.page"
          data-test="audit-pagination"
        />
      </section>
    </main>
  </div>
</template>
