<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import VChart from 'vue-echarts'
import { BarChart, HeatmapChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TooltipComponent,
  VisualMapComponent,
} from 'echarts/components'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import AppHeader from '@/components/AppHeader.vue'
import { useStatsStore } from '@/stores/statsStore'

use([BarChart, HeatmapChart, GridComponent, LegendComponent, TooltipComponent, VisualMapComponent, CanvasRenderer])

const statsStore = useStatsStore()
const userIdInput = ref('')
const windowIdInput = ref('')

const accumOptions = [
  { label: '全部累计长度', value: '' },
  { label: '24小时', value: '24' },
  { label: '48小时', value: '48' },
  { label: '72小时', value: '72' },
]

const ptypeLabels = ['无降水', '雨', '雨夹雪', '雪']

const summaryCards = computed(() => [
  { label: '编辑会话', value: statsStore.operationStats?.total_sessions ?? 0, test: 'total-sessions' },
  { label: '编辑操作', value: statsStore.operationStats?.total_operations ?? 0, test: 'total-operations' },
  {
    label: '保存版本',
    value: statsStore.operationStats?.total_versions_saved ?? 0,
    test: 'versions-saved',
  },
  {
    label: '发布版本',
    value: statsStore.operationStats?.total_versions_released ?? 0,
    test: 'versions-released',
  },
])

const byToolOption = computed(() => makeBarOption('工具使用次数', statsStore.operationStats?.by_tool ?? {}))
const byOperationOption = computed(() =>
  makeBarOption('操作类型次数', statsStore.operationStats?.by_operation ?? {}),
)
const heatmapOption = computed(() => {
  const matrix = statsStore.ptypeTransitions?.matrix ?? {}
  const data = ptypeLabels.flatMap((_, from) =>
    ptypeLabels.map((__, to) => [to, from, matrix[`${from}->${to}`] ?? 0]),
  )
  const max = Math.max(1, ...data.map((item) => Number(item[2])))

  return {
    tooltip: {
      formatter(params: { data: [number, number, number] }) {
        const [to, from, count] = params.data
        return `${ptypeLabels[from]} → ${ptypeLabels[to]}：${count}`
      },
    },
    grid: { top: 24, right: 20, bottom: 48, left: 72 },
    xAxis: { type: 'category', data: ptypeLabels, name: '编辑后' },
    yAxis: { type: 'category', data: ptypeLabels, name: '编辑前' },
    visualMap: { min: 0, max, orient: 'horizontal', left: 'center', bottom: 0 },
    series: [{ type: 'heatmap', data }],
  }
})

function makeBarOption(title: string, data: Record<string, number>) {
  const entries = Object.entries(data)
  return {
    tooltip: {},
    grid: { top: 24, right: 20, bottom: 36, left: 48 },
    xAxis: { type: 'category', data: entries.map(([key]) => key || '未命名') },
    yAxis: { type: 'value' },
    series: [{ name: title, type: 'bar', data: entries.map(([, value]) => value) }],
  }
}

function toNumber(value: string) {
  const parsed = Number(value)
  return Number.isFinite(parsed) && value !== '' ? parsed : undefined
}

async function searchStats() {
  statsStore.setFilters({
    user_id: toNumber(userIdInput.value),
    window_id: windowIdInput.value || undefined,
  })
  await statsStore.fetchAll()
}

function updateAccumHours(value: string | undefined) {
  statsStore.setFilter('accum_hours', value ? Number(value) : undefined)
}

onMounted(async () => {
  await statsStore.fetchAll()
})
</script>

<template>
  <div class="page-shell">
    <AppHeader />
    <main class="content-wrap workspace-page ops-page">
      <section class="workspace-panel">
        <div class="workspace-panel__header">
          <div>
            <h1 class="workspace-title">历史分析</h1>
            <p class="workspace-desc">按时间、用户、窗口和累计长度查看编辑操作统计。</p>
          </div>
          <t-button theme="primary" data-test="stats-export" @click="statsStore.exportStats">
            导出 CSV
          </t-button>
        </div>

        <t-card class="ops-filters" bordered>
          <div class="ops-filter-grid">
            <label>
              时间范围
              <t-date-range-picker
                v-model="statsStore.filters.dateRange"
                data-test="date-range-filter"
              />
            </label>
            <label>
              用户ID
              <t-input v-model="userIdInput" placeholder="全部用户" data-test="user-filter" />
            </label>
            <label>
              窗口ID
              <t-input v-model="windowIdInput" placeholder="全部窗口" data-test="window-filter" />
            </label>
            <label>
              累计长度
              <t-select
                :model-value="String(statsStore.filters.accum_hours ?? '')"
                :options="accumOptions"
                data-test="accum-filter"
                @update:model-value="updateAccumHours"
              />
            </label>
            <t-button data-test="stats-search" @click="searchStats">查询</t-button>
          </div>
        </t-card>

        <div v-if="statsStore.error" class="ops-error" data-test="stats-error">
          {{ statsStore.error }}
        </div>

        <section class="ops-summary" aria-label="统计汇总">
          <t-card
            v-for="card in summaryCards"
            :key="card.test"
            class="ops-summary-card"
            bordered
            :data-test="card.test"
          >
            <span>{{ card.label }}</span>
            <strong>{{ card.value }}</strong>
          </t-card>
        </section>

        <section class="ops-charts">
          <t-card title="按工具统计" bordered>
            <VChart class="ops-chart" :option="byToolOption" autoresize data-test="by-tool-chart" />
          </t-card>
          <t-card title="按操作类型统计" bordered>
            <VChart
              class="ops-chart"
              :option="byOperationOption"
              autoresize
              data-test="by-operation-chart"
            />
          </t-card>
          <t-card title="相态转换矩阵" bordered>
            <VChart
              class="ops-chart ops-chart--heatmap"
              :option="heatmapOption"
              autoresize
              data-test="ptype-heatmap"
            />
          </t-card>
        </section>
      </section>
    </main>
  </div>
</template>
