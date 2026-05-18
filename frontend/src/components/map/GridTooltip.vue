<template>
  <div class="grid-tooltip" role="status">
    <span>{{ displayText }}</span>
    <span v-if="payload?.inBounds && payload.isEdited && !loading" class="edited-indicator">已编辑</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { GridHoverPayload } from '@/types/editor'

const props = defineProps<{
  payload: GridHoverPayload | null
  loading: boolean
}>()

function formatNumber(value: number | null): string {
  if (value === null) {
    return '-'
  }

  return Number.isInteger(value) ? String(value) : value.toFixed(2)
}

const displayText = computed(() => {
  if (props.loading) {
    return '数据加载中...'
  }

  if (!props.payload || !props.payload.inBounds) {
    return '区域外'
  }

  return [
    `经度: ${props.payload.lon.toFixed(2)}`,
    `纬度: ${props.payload.lat.toFixed(2)}`,
    `行: ${props.payload.gridI}`,
    `列: ${props.payload.gridJ}`,
    `QPF原始: ${formatNumber(props.payload.qpfBefore)}`,
    `QPF订正: ${formatNumber(props.payload.qpfAfter)}`,
    `相态原始: ${formatNumber(props.payload.ptypeBefore)}`,
    `相态订正: ${formatNumber(props.payload.ptypeAfter)}`,
  ].join(' | ')
})
</script>

<style scoped>
.grid-tooltip {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  height: var(--bottom-status-height);
  padding: 0 12px;
  overflow: hidden;
  color: var(--text-primary);
  font-size: 13px;
  line-height: var(--bottom-status-height);
  white-space: nowrap;
  background: var(--page-bg);
  border-top: 1px solid var(--color-border);
}

.grid-tooltip > span:first-child {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.edited-indicator {
  flex: 0 0 auto;
  padding: 0 8px;
  color: var(--color-primary);
  font-size: var(--font-size-caption);
  line-height: var(--line-height-body);
  background: var(--color-primary-bg);
  border: 1px solid var(--color-primary-bg);
  border-radius: 4px;
}
</style>
