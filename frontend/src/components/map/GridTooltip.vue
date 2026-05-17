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
  height: 36px;
  padding: 0 12px;
  overflow: hidden;
  color: #1f2937;
  font-size: 13px;
  line-height: 36px;
  white-space: nowrap;
  background: #f8fafc;
  border-top: 1px solid #d9e1ec;
}

.grid-tooltip > span:first-child {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.edited-indicator {
  flex: 0 0 auto;
  padding: 0 8px;
  color: #0052d9;
  font-size: 12px;
  line-height: 22px;
  background: #e8f1ff;
  border: 1px solid #bed9ff;
  border-radius: 4px;
}
</style>
