<script setup lang="ts">
import { computed, ref } from 'vue'
import WindowQcInfo from '@/components/WindowQcInfo.vue'
import { useWindowStore } from '@/stores/windowStore'
import type { WindowItem } from '@/api/data'

const windowStore = useWindowStore()
const activeTab = ref('24')

const tabs = [
  { value: '24', label: '24h', accumHours: 24 },
  { value: '48', label: '48h', accumHours: 48 },
  { value: '168', label: '168h', accumHours: 168 },
] as const

const statusMeta = {
  available: { label: '可用', theme: 'success' },
  partial: { label: '部分缺失', theme: 'warning' },
  invalid: { label: '异常', theme: 'danger' },
  pending: { label: '待处理', theme: 'default' },
  archived: { label: '已归档', theme: 'default' },
} as const

const groupedWindows = computed(() =>
  tabs.reduce(
    (result, tab) => {
      result[tab.value] = windowStore.windows.filter(
        (window) => window.accum_hours === tab.accumHours,
      )
      return result
    },
    {} as Record<(typeof tabs)[number]['value'], WindowItem[]>,
  ),
)

function canSelect(window: WindowItem) {
  return window.status === 'available' || window.status === 'partial'
}

function selectWindow(window: WindowItem) {
  if (canSelect(window)) {
    windowStore.selectWindow(window.window_id)
  }
}
</script>

<template>
  <section class="window-selector" aria-labelledby="window-selector-title">
    <div class="window-selector__header">
      <h2 id="window-selector-title" class="window-selector__title">产品窗口</h2>
      <span class="window-selector__count">{{ windowStore.windows.length }} 个窗口</span>
    </div>

    <t-tabs v-model="activeTab">
      <t-tab-panel v-for="tab in tabs" :key="tab.value" :value="tab.value" :label="tab.label">
        <div class="window-selector__list">
          <p v-if="groupedWindows[tab.value].length === 0" class="window-selector__empty">
            暂无{{ tab.label }}窗口
          </p>
          <t-tooltip
            v-for="window in groupedWindows[tab.value]"
            :key="window.window_id"
            :content="window.status === 'invalid' ? '数据异常，不可编辑' : window.status === 'partial' ? '部分数据缺失' : ''"
            :disabled="window.status !== 'invalid' && window.status !== 'partial'"
          >
            <button
              class="window-selector__item"
              :class="{ 'window-selector__item--selected': windowStore.selectedWindowId === window.window_id }"
              type="button"
              :aria-disabled="!canSelect(window)"
              @click="selectWindow(window)"
            >
              <span class="window-selector__range">
                {{ window.start_lead }}-{{ window.end_lead }}h
              </span>
              <WindowQcInfo
                :negative-count="window.negative_count"
                :negative-min-value="window.negative_min_value"
                :ptype-missing-leads="window.ptype_missing_leads"
              />
              <t-tag
                class="window-selector__tag"
                :theme="statusMeta[window.status].theme"
                variant="light"
              >
                {{ statusMeta[window.status].label }}
              </t-tag>
            </button>
          </t-tooltip>
        </div>
      </t-tab-panel>
    </t-tabs>
  </section>
</template>
