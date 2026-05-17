<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useEditorStore } from '@/stores/editorStore'

type PtypeValue = 0 | 1 | 2 | 3

const editorStore = useEditorStore()
const selectedTargetPtype = ref<1 | 2 | 3>(1)
const ptypeDialogVisible = ref(false)

const preview = computed(() => editorStore.previewResult)
const needsTargetPtype = computed(() => preview.value?.new_precip_needs_ptype === true)
const applying = computed(() => editorStore.applyLoading)

const statRows = [
  { key: 'min', label: '最小值', unit: 'mm' },
  { key: 'max', label: '最大值', unit: 'mm' },
  { key: 'mean', label: '平均值', unit: 'mm' },
  { key: 'sum', label: '总量', unit: 'mm' },
  { key: 'count', label: '格点数', unit: '格点' },
  { key: 'area_km2', label: '面积', unit: 'km²' },
] as const

const ptypeLabels: Array<{ value: PtypeValue; label: string }> = [
  { value: 0, label: '无降水(0)' },
  { value: 1, label: '雨(1)' },
  { value: 2, label: '雪(2)' },
  { value: 3, label: '雨夹雪(3)' },
]

const targetPtypeOptions = [
  { value: 1, label: '雨(1)' },
  { value: 2, label: '雪(2)' },
  { value: 3, label: '雨夹雪(3)' },
]

const matrixRows = computed(() =>
  ptypeLabels.map((from) => ({
    ...from,
    cells: ptypeLabels.map((to) => ({
      to: to.value,
      count: getTransitionCount(from.value, to.value),
    })),
  })),
)

watch(
  () => preview.value?.preview_id,
  () => {
    selectedTargetPtype.value = 1
  },
)

function getStatValue(stats: Record<string, number> | undefined, key: string): number | null {
  const value = stats?.[key]
  return typeof value === 'number' && Number.isFinite(value) ? value : null
}

function formatValue(value: number | null, key: string): string {
  if (value === null) {
    return '-'
  }

  if (key === 'count') {
    return `${Math.round(value)}`
  }

  if (key === 'area_km2') {
    return value >= 100 ? value.toFixed(0) : value.toFixed(1)
  }

  if (key === 'mean') {
    return value.toFixed(2)
  }

  return value.toFixed(1)
}

function getTransitionCount(from: PtypeValue, to: PtypeValue): number {
  const transition = preview.value?.op_ptype_transition

  if (!transition) {
    return 0
  }

  return transition[`${from}->${to}`] ?? transition[`${from}_${to}`] ?? transition[`${from},${to}`] ?? 0
}

function formatWarning(warning: { code: string; count: number }): string {
  return `${warning.code}: ${warning.count} 个格点`
}

async function applyPreview(): Promise<void> {
  if (needsTargetPtype.value) {
    ptypeDialogVisible.value = true
    return
  }

  await editorStore.applyEdit()
}

async function confirmApplyWithPtype(): Promise<void> {
  await editorStore.applyEdit(selectedTargetPtype.value)
  ptypeDialogVisible.value = false
}
</script>

<template>
  <section v-if="preview" class="preview-stats-panel" data-test="preview-stats-panel">
    <div class="preview-stats-panel__header">
      <h3 class="preview-stats-panel__title">预览统计</h3>
      <div class="preview-stats-panel__summary" data-test="preview-summary">
        <span>{{ preview.affected_grid_count }} 格点</span>
        <span>{{ formatValue(preview.affected_area_km2, 'area_km2') }} km²</span>
      </div>
    </div>

    <div v-if="preview.warnings.length > 0" class="preview-stats-panel__warnings" data-test="preview-warnings">
      <p v-for="warning in preview.warnings" :key="warning.code">
        {{ formatWarning(warning) }}
      </p>
    </div>

    <div v-if="needsTargetPtype" class="preview-stats-panel__ptype-warning" data-test="new-precip-warning">
      <p>新增 {{ preview.new_precip_count }} 个降水格点需要选择相态</p>
    </div>

    <div class="preview-stats-panel__section">
      <h4 class="preview-stats-panel__subtitle">订正前后对比</h4>
      <table class="preview-stats-panel__table" data-test="stats-table">
        <thead>
          <tr>
            <th>指标</th>
            <th>编辑前</th>
            <th>编辑后</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in statRows" :key="row.key">
            <td>{{ row.label }}</td>
            <td>{{ formatValue(getStatValue(preview.before_stats, row.key), row.key) }} {{ row.unit }}</td>
            <td>{{ formatValue(getStatValue(preview.after_stats, row.key), row.key) }} {{ row.unit }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="preview-stats-panel__section">
      <h4 class="preview-stats-panel__subtitle">相态转移矩阵</h4>
      <table class="preview-stats-panel__table preview-stats-panel__matrix" data-test="ptype-matrix">
        <thead>
          <tr>
            <th>从 / 到</th>
            <th v-for="label in ptypeLabels" :key="label.value">{{ label.label }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in matrixRows" :key="row.value">
            <th>{{ row.label }}</th>
            <td v-for="cell in row.cells" :key="cell.to">
              {{ cell.count }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="preview-stats-panel__actions">
      <t-button
        theme="primary"
        :disabled="applying"
        :loading="applying"
        data-test="preview-apply-button"
        @click="applyPreview"
      >
        应用
      </t-button>
      <t-button :disabled="applying" data-test="preview-cancel-button" @click="editorStore.clearPreview()">
        取消
      </t-button>
    </div>

    <t-dialog
      v-if="needsTargetPtype"
      v-model:visible="ptypeDialogVisible"
      header="新增降水落区相态选择"
      :close-on-overlay-click="false"
      data-test="target-ptype-dialog"
    >
      <div class="preview-stats-panel__dialog">
        <p>本次操作将新增 {{ preview.new_precip_count }} 个降水格点，请选择这些格点的相态。</p>
        <t-radio-group v-model="selectedTargetPtype" class="preview-stats-panel__radio" data-test="target-ptype-radio">
          <t-radio-button v-for="option in targetPtypeOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </t-radio-button>
        </t-radio-group>
        <div class="preview-stats-panel__dialog-actions">
          <t-button :disabled="applying" data-test="target-ptype-cancel" @click="ptypeDialogVisible = false">
            取消
          </t-button>
          <t-button
            theme="primary"
            :disabled="applying"
            :loading="applying"
            data-test="target-ptype-confirm"
            @click="confirmApplyWithPtype"
          >
            确认并应用
          </t-button>
        </div>
      </div>
    </t-dialog>
  </section>
</template>

<style scoped>
.preview-stats-panel {
  display: grid;
  gap: 12px;
  color: #1d2129;
}

.preview-stats-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.preview-stats-panel__title,
.preview-stats-panel__subtitle {
  margin: 0;
  font-weight: 600;
}

.preview-stats-panel__title {
  font-size: 15px;
  line-height: 24px;
}

.preview-stats-panel__subtitle {
  margin-bottom: 8px;
  font-size: 13px;
  line-height: 20px;
}

.preview-stats-panel__summary {
  display: inline-flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
  color: #4e5969;
  font-size: 12px;
  line-height: 18px;
}

.preview-stats-panel__summary span {
  border-radius: 4px;
  background: #e8f1ff;
  padding: 2px 6px;
  color: #1664ff;
}

.preview-stats-panel__warnings,
.preview-stats-panel__ptype-warning {
  border: 1px solid #ffd8bf;
  border-radius: 6px;
  background: #fff3e8;
  padding: 8px 10px;
  color: #ad4e00;
  font-size: 13px;
  line-height: 20px;
}

.preview-stats-panel__warnings p,
.preview-stats-panel__ptype-warning p {
  margin: 0;
}

.preview-stats-panel__radio {
  margin-top: 8px;
}

.preview-stats-panel__dialog {
  display: grid;
  gap: 12px;
  color: #1d2129;
  font-size: 14px;
  line-height: 22px;
}

.preview-stats-panel__dialog p {
  margin: 0;
}

.preview-stats-panel__dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 4px;
}

.preview-stats-panel__table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  border: 1px solid #e5e6eb;
  border-radius: 6px;
  overflow: hidden;
  font-size: 12px;
  line-height: 18px;
}

.preview-stats-panel__table th,
.preview-stats-panel__table td {
  border-bottom: 1px solid #e5e6eb;
  padding: 7px 8px;
  text-align: right;
  vertical-align: middle;
}

.preview-stats-panel__table th:first-child,
.preview-stats-panel__table td:first-child {
  text-align: left;
}

.preview-stats-panel__table thead th {
  background: #fafafa;
  color: #4e5969;
  font-weight: 600;
}

.preview-stats-panel__table tbody tr:last-child th,
.preview-stats-panel__table tbody tr:last-child td {
  border-bottom: 0;
}

.preview-stats-panel__matrix th,
.preview-stats-panel__matrix td {
  padding: 6px 4px;
  text-align: center;
}

.preview-stats-panel__matrix th:first-child {
  width: 76px;
  text-align: left;
}

.preview-stats-panel__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 2px;
}
</style>
