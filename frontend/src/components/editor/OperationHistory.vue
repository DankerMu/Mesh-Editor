<script setup lang="ts">
import { computed, onMounted } from 'vue'
import {
  BrushIcon,
  EditIcon,
  MapEditIcon,
  PenBrushIcon,
  RefreshIcon,
  RollbackIcon,
  SubwayLineIcon,
} from 'tdesign-icons-vue-next'
import { useEditorStore } from '@/stores/editorStore'
import type { OperationItem } from '@/api/edit'

const editorStore = useEditorStore()

onMounted(() => {
  if (editorStore.sessionId && editorStore.operations.length === 0) {
    editorStore.fetchOperations().catch(() => {})
  }
})

const toolMeta: Record<string, { label: string; icon: unknown }> = {
  polygon: { label: '多边形', icon: MapEditIcon },
  line_buffer: { label: '线缓冲', icon: SubwayLineIcon },
  brush_path: { label: '笔刷', icon: PenBrushIcon },
  brush: { label: '笔刷', icon: BrushIcon },
}

const operationLabels: Record<string, string> = {
  set_value: '设为指定值',
  increase: '增加',
  decrease: '减少',
  multiply: '倍率调整',
  clear: '清零',
  ptype_set: '设置相态',
  set_ptype: '设置相态',
  screen_clear: '小量清屏',
}

const variableLabels: Record<string, string> = {
  qpf: '降水',
  ptype: '相态',
}

const operations = computed(() => editorStore.operations)
const busy = computed(() => editorStore.applyLoading)

function getToolLabel(toolName: string): string {
  return toolMeta[toolName]?.label ?? toolName
}

function getToolIcon(toolName: string): unknown {
  return toolMeta[toolName]?.icon ?? EditIcon
}

function getOperationLabel(operationType: string): string {
  return operationLabels[operationType] ?? operationType
}

function getVariableLabel(variableName: string): string {
  return variableLabels[variableName] ?? variableName
}

function formatTime(value: string): string {
  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return value
  }

  const utc8 = new Date(date.getTime() + 8 * 60 * 60 * 1000)
  const pad = (part: number) => String(part).padStart(2, '0')

  return `${pad(utc8.getUTCMonth() + 1)}-${pad(utc8.getUTCDate())} ${pad(utc8.getUTCHours())}:${pad(
    utc8.getUTCMinutes(),
  )}`
}

function operationTitle(operation: OperationItem): string {
  return `${getVariableLabel(operation.variable_name)} ${getOperationLabel(operation.operation_type)}`
}

async function undo(): Promise<void> {
  if (!editorStore.canUndo || busy.value) {
    return
  }

  await editorStore.undoEdit()
}

async function redo(): Promise<void> {
  if (!editorStore.canRedo || busy.value) {
    return
  }

  await editorStore.redoEdit()
}
</script>

<template>
  <section class="operation-history" data-test="operation-history">
    <div class="operation-history__toolbar">
      <t-button
        size="small"
        :disabled="!editorStore.canUndo || busy"
        data-test="history-undo-button"
        @click="undo"
      >
        <template #icon><RollbackIcon /></template>
        撤销
      </t-button>
      <t-button
        size="small"
        :disabled="!editorStore.canRedo || busy"
        data-test="history-redo-button"
        @click="redo"
      >
        <template #icon><RefreshIcon /></template>
        重做
      </t-button>
    </div>

    <p v-if="operations.length === 0" class="operation-history__empty" data-test="operation-history-empty">
      暂无操作记录
    </p>

    <ol v-else class="operation-history__list" data-test="operation-history-list">
      <li
        v-for="operation in operations"
        :key="operation.sequence_no"
        class="operation-history__item"
        :class="{ 'operation-history__item--undone': operation.is_undone === 1 }"
        data-test="operation-history-item"
      >
        <div class="operation-history__icon" aria-hidden="true">
          <component :is="getToolIcon(operation.tool_name)" />
        </div>
        <div class="operation-history__content">
          <div class="operation-history__main">
            <span class="operation-history__sequence">#{{ operation.sequence_no }}</span>
            <span class="operation-history__title">{{ operationTitle(operation) }}</span>
            <t-tag v-if="operation.is_undone === 1" theme="default" variant="light">已撤销</t-tag>
          </div>
          <div class="operation-history__meta">
            <span>{{ getToolLabel(operation.tool_name) }}</span>
            <span>{{ operation.affected_grid_count }} 格点</span>
            <span>{{ formatTime(operation.created_at) }}</span>
          </div>
        </div>
      </li>
    </ol>
  </section>
</template>

<style scoped>
.operation-history {
  display: grid;
  gap: 12px;
}

.operation-history__toolbar {
  display: flex;
  gap: 8px;
}

.operation-history__empty {
  margin: 0;
  border: 1px dashed #d9e1ec;
  border-radius: 6px;
  background: #f7f8fa;
  padding: 18px 12px;
  color: #86909c;
  font-size: 14px;
  line-height: 22px;
  text-align: center;
}

.operation-history__list {
  display: grid;
  gap: 8px;
  max-height: 520px;
  margin: 0;
  padding: 0;
  overflow: auto;
  list-style: none;
}

.operation-history__item {
  display: grid;
  grid-template-columns: 28px 1fr;
  gap: 8px;
  min-height: 58px;
  border: 1px solid #e5e6eb;
  border-radius: 6px;
  background: #ffffff;
  padding: 9px 10px;
  color: #1d2129;
}

.operation-history__item--undone {
  background: #f7f8fa;
  color: #86909c;
}

.operation-history__item--undone .operation-history__title {
  text-decoration: line-through;
}

.operation-history__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: #e8f1ff;
  color: #1664ff;
  font-size: 16px;
}

.operation-history__item--undone .operation-history__icon {
  background: #f2f3f5;
  color: #86909c;
}

.operation-history__content {
  min-width: 0;
}

.operation-history__main,
.operation-history__meta {
  display: flex;
  align-items: center;
  min-width: 0;
}

.operation-history__main {
  gap: 6px;
  margin-bottom: 4px;
}

.operation-history__sequence {
  flex: 0 0 auto;
  color: #1664ff;
  font-size: 12px;
  line-height: 18px;
  font-weight: 600;
}

.operation-history__item--undone .operation-history__sequence {
  color: #86909c;
}

.operation-history__title {
  min-width: 0;
  overflow: hidden;
  font-size: 14px;
  line-height: 22px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.operation-history__meta {
  flex-wrap: wrap;
  gap: 6px 10px;
  color: #4e5969;
  font-size: 12px;
  line-height: 18px;
}

.operation-history__item--undone .operation-history__meta {
  color: #86909c;
}
</style>
