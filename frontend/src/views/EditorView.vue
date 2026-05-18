<script setup lang="ts">
import { computed, onBeforeUnmount, ref, shallowRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import Map from 'ol/Map'
import AppHeader from '@/components/AppHeader.vue'
import BaseMap from '@/components/map/BaseMap.vue'
import DrawTools from '@/components/map/DrawTools.vue'
import GridTooltip from '@/components/map/GridTooltip.vue'
import { MaskOverlayLayer } from '@/components/map/MaskOverlayLayer'
import { PrecipPhaseGridLayer, getGridDataValue } from '@/components/map/PrecipPhaseGridLayer'
import { SelectionOverlay } from '@/components/map/SelectionOverlay'
import OperationHistory from '@/components/editor/OperationHistory.vue'
import PreviewStatsPanel from '@/components/editor/PreviewStatsPanel.vue'
import { GRID_COLS } from '@/constants/precipColors'
import WindowSelector from '@/components/WindowSelector.vue'
import { useEditorStore } from '@/stores/editorStore'
import { useVersionStore } from '@/stores/versionStore'
import { useWindowStore } from '@/stores/windowStore'
import type { EditOperation } from '@/api/edit'
import type { GridHoverPayload, ViewMode } from '@/types/editor'
import type { WindowItem } from '@/api/data'

const route = useRoute()
const router = useRouter()
const editorStore = useEditorStore()
const versionStore = useVersionStore()
const windowStore = useWindowStore()

const leftCollapsed = ref(false)
const rightCollapsed = ref(false)
const activeRightTab = ref('mask')
const hoverPayload = ref<GridHoverPayload | null>(null)
const mapInstance = shallowRef<Map | null>(null)

let precipLayer: PrecipPhaseGridLayer | null = null
let invalidMaskLayer: MaskOverlayLayer | null = null
let viewMaskLayer: MaskOverlayLayer | null = null
let selectionOverlay: SelectionOverlay | null = null

const viewModes: Array<{ value: ViewMode; label: string; hint?: string }> = [
  { value: 'before', label: '订正前' },
  { value: 'after', label: '订正后' },
  { value: 'delta', label: '差值', hint: '无差异' },
  { value: 'change', label: '相态变化', hint: '无变化' },
  { value: 'touched', label: '已触达' },
  { value: 'changed', label: '已改变' },
  { value: 'review', label: '审核视图' },
]

const statusMeta: Record<string, { label: string; theme: string }> = {
  available: { label: '可用', theme: 'success' },
  partial: { label: '部分缺失', theme: 'warning' },
  invalid: { label: '异常', theme: 'danger' },
  pending: { label: '待处理', theme: 'default' },
  archived: { label: '已归档', theme: 'default' },
  unknown: { label: '未知', theme: 'default' },
}

const currentWindowId = computed(() => {
  const param = route.params.windowId
  return typeof param === 'string' ? param : null
})

const currentWindow = computed<WindowItem | null>(() => {
  const windowId = currentWindowId.value ?? editorStore.windowId

  if (!windowId) {
    return null
  }

  return windowStore.windows.find((window) => window.window_id === windowId) ?? null
})

const parsedWindow = computed(() => parseWindowId(currentWindowId.value ?? editorStore.windowId))

const hasWindowId = computed(() => currentWindowId.value !== null)
const drawToolsDisabled = computed(() => editorStore.sessionId === null)
const editPanelsDisabled = computed(() =>
  editorStore.sessionId === null || editorStore.currentMaskGeometry === null,
)
const maskTool = computed(() => {
  const geom = editorStore.currentMaskGeometry
  return geom?.type ?? 'polygon'
})
const panelBusy = computed(() => editorStore.previewLoading || editorStore.applyLoading)
const selectedMode = computed(() => viewModes.find((mode) => mode.value === editorStore.selectedViewMode))

// QPF panel state
const qpfOperation = ref<EditOperation | ''>('set_value')
const qpfValue = ref<number>(0)
const qpfOperationOptions = [
  { value: 'set_value', label: '设值' },
  { value: 'increase', label: '增加' },
  { value: 'decrease', label: '减少' },
  { value: 'multiply', label: '乘以' },
  { value: 'clear', label: '清除' },
  { value: 'screen_clear', label: '筛除清零' },
]
const qpfValueHidden = computed(() => qpfOperation.value === 'clear' || qpfOperation.value === 'screen_clear')
const qpfPreviewDisabled = computed(
  () => editPanelsDisabled.value || !qpfOperation.value || panelBusy.value,
)

// Ptype panel state
const ptypeTarget = ref<number | null>(null)
const ptypePreviewDisabled = computed(
  () => editPanelsDisabled.value || ptypeTarget.value === null || panelBusy.value,
)

// Submit dialog
const submitDialogVisible = ref(false)

function getQpfParams(): Record<string, unknown> {
  const op = qpfOperation.value
  if (op === 'clear' || op === 'screen_clear') return {}
  if (op === 'set_value') return { value: qpfValue.value }
  if (op === 'increase' || op === 'decrease') return { delta_mm: qpfValue.value }
  if (op === 'multiply') return { factor: qpfValue.value }
  return { value: qpfValue.value }
}

async function handleQpfPreview(): Promise<void> {
  if (!qpfOperation.value || !editorStore.currentMaskGeometry) return
  try {
    await editorStore.requestPreview(
      maskTool.value,
      'qpf',
      qpfOperation.value as EditOperation,
      editorStore.currentMaskGeometry as unknown as Record<string, unknown>,
      getQpfParams(),
    )
  } catch {
    if (editorStore.previewError) {
      MessagePlugin.error(editorStore.previewError)
    }
  }
}

async function handlePtypePreview(): Promise<void> {
  if (ptypeTarget.value === null || !editorStore.currentMaskGeometry) return
  try {
    await editorStore.requestPreview(
      maskTool.value,
      'ptype',
      'set_ptype',
      editorStore.currentMaskGeometry as unknown as Record<string, unknown>,
      { target_ptype: ptypeTarget.value },
    )
  } catch {
    if (editorStore.previewError) {
      MessagePlugin.error(editorStore.previewError)
    }
  }
}

async function handleSubmit(): Promise<void> {
  submitDialogVisible.value = true
}

async function confirmSubmit(): Promise<void> {
  if (!editorStore.currentVersionId) return
  await versionStore.submitVersion(editorStore.currentVersionId)
  submitDialogVisible.value = false
}

const topBarInfo = computed(() => {
  const window = currentWindow.value
  const parsed = parsedWindow.value

  return {
    initTime: parsed?.initTimeLabel ?? '-',
    accumHours: window?.accum_hours ?? parsed?.accumHours ?? null,
    startLead: window?.start_lead ?? parsed?.startLead ?? null,
    endLead: window?.end_lead ?? parsed?.endLead ?? null,
    status: window?.status ?? 'unknown',
  }
})

const statusTag = computed(() => statusMeta[topBarInfo.value.status] ?? statusMeta.unknown)
const leadRangeLabel = computed(() => {
  const { startLead, endLead } = topBarInfo.value

  if (startLead === null || endLead === null) {
    return '-'
  }

  return `+${String(startLead).padStart(3, '0')}h ~ +${String(endLead).padStart(3, '0')}h`
})

function parseWindowId(windowId: string | null): {
  initTimeLabel: string | null
  accumHours: number
  startLead: number
  endLead: number
} | null {
  if (!windowId) {
    return null
  }

  const match = windowId.match(/^(?:(\d{10})_)?ACC(\d+)_(\d{3})_(\d{3})$/)

  if (!match) {
    return null
  }

  const [, caseId, accumHours, startLead, endLead] = match

  return {
    initTimeLabel: caseId ? formatCaseIdUtc8(caseId) : null,
    accumHours: Number(accumHours),
    startLead: Number(startLead),
    endLead: Number(endLead),
  }
}

function formatCaseIdUtc8(caseId: string): string {
  const year = Number(caseId.slice(0, 4))
  const month = Number(caseId.slice(4, 6))
  const day = Number(caseId.slice(6, 8))
  const hour = Number(caseId.slice(8, 10))
  const date = new Date(Date.UTC(year, month - 1, day, hour + 8))
  const pad = (value: number) => String(value).padStart(2, '0')

  return `${date.getUTCFullYear()}-${pad(date.getUTCMonth() + 1)}-${pad(date.getUTCDate())} ${pad(
    date.getUTCHours(),
  )}`
}

function setViewMode(viewMode: ViewMode): void {
  editorStore.selectedViewMode = viewMode
}

function onMapReady(map: Map): void {
  disposeLayers()
  mapInstance.value = map
  precipLayer = new PrecipPhaseGridLayer()
  invalidMaskLayer = new MaskOverlayLayer()
  viewMaskLayer = new MaskOverlayLayer([22, 93, 255, 130])
  selectionOverlay = new SelectionOverlay(map)
  map.addLayer(precipLayer.getLayer())
  map.addLayer(invalidMaskLayer.getLayer())
  map.addLayer(viewMaskLayer.getLayer())
  syncMapLayers(editorStore.selectedViewMode)
  syncSelectionOverlay()
}

function onGridHover(payload: GridHoverPayload | null): void {
  if (!payload?.inBounds) {
    hoverPayload.value = payload
    return
  }

  const before = getGridDataValue(payload.lon, payload.lat, editorStore.qpfBefore, editorStore.ptypeBefore)
  const after = getGridDataValue(payload.lon, payload.lat, editorStore.qpfAfter, editorStore.ptypeAfter)
  const arrayIndex =
    before || after
      ? (before?.gridI ?? after?.gridI ?? 0) * GRID_COLS + (before?.gridJ ?? after?.gridJ ?? 0)
      : -1

  hoverPayload.value = {
    ...payload,
    gridI: before?.gridI ?? after?.gridI ?? payload.gridI,
    gridJ: before?.gridJ ?? after?.gridJ ?? payload.gridJ,
    qpfBefore: before?.qpf ?? null,
    qpfAfter: after?.qpf ?? null,
    ptypeBefore: before?.ptype ?? null,
    ptypeAfter: after?.ptype ?? null,
    isEdited:
      arrayIndex >= 0
        ? Boolean(editorStore.touchedMask?.[arrayIndex] || editorStore.changedMask?.[arrayIndex])
        : false,
  }
}

function updatePrecipData(qpf: Float32Array | null, ptype: Uint8Array | null): void {
  if (!precipLayer) {
    return
  }

  if (qpf && ptype) {
    precipLayer.updateData(qpf, ptype)
    return
  }

  precipLayer.clearData()
}

function updateMaskLayer(layer: MaskOverlayLayer | null, mask: Uint8Array | null): void {
  if (!layer) {
    return
  }

  if (mask) {
    layer.updateData(mask)
    return
  }

  layer.clearData()
}

function syncMapLayers(mode: ViewMode = editorStore.selectedViewMode): void {
  if (!precipLayer || !invalidMaskLayer || !viewMaskLayer) {
    return
  }

  updateMaskLayer(invalidMaskLayer, editorStore.invalidMask)
  viewMaskLayer.clearData()

  if (mode === 'before') {
    updatePrecipData(editorStore.qpfBefore, editorStore.ptypeBefore)
    return
  }

  if (mode === 'after') {
    updatePrecipData(editorStore.qpfAfter, editorStore.ptypeAfter)
    return
  }

  if (mode === 'review') {
    updatePrecipData(editorStore.qpfAfter, editorStore.ptypeAfter)
    updateMaskLayer(viewMaskLayer, editorStore.touchedMask)
    return
  }

  precipLayer.clearData()

  if (mode === 'touched') {
    updateMaskLayer(viewMaskLayer, editorStore.touchedMask)
  }

  if (mode === 'changed') {
    updateMaskLayer(viewMaskLayer, editorStore.changedMask)
  }
}

function syncSelectionOverlay(): void {
  selectionOverlay?.updateGeometry(editorStore.currentMaskGeometry)
}

function disposeLayers(): void {
  if (mapInstance.value) {
    if (precipLayer) {
      mapInstance.value.removeLayer(precipLayer.getLayer())
    }

    if (invalidMaskLayer) {
      mapInstance.value.removeLayer(invalidMaskLayer.getLayer())
    }

    if (viewMaskLayer) {
      mapInstance.value.removeLayer(viewMaskLayer.getLayer())
    }
  }

  selectionOverlay?.dispose()
  precipLayer?.dispose()
  invalidMaskLayer?.dispose()
  viewMaskLayer?.dispose()
  selectionOverlay = null
  precipLayer = null
  invalidMaskLayer = null
  viewMaskLayer = null
  mapInstance.value = null
}

watch(
  () => currentWindowId.value,
  (windowId) => {
    if (windowId) {
      void editorStore.startSession(windowId)
    } else {
      editorStore.reset()
    }
  },
  { immediate: true },
)

watch(
  () => windowStore.selectedWindowId,
  (windowId) => {
    if (windowId && route.path === '/editor') {
      void router.push(`/editor/${windowId}`)
    }
  },
)

watch(
  () => [
    editorStore.selectedViewMode,
    editorStore.qpfBefore,
    editorStore.ptypeBefore,
    editorStore.qpfAfter,
    editorStore.ptypeAfter,
    editorStore.invalidMask,
    editorStore.touchedMask,
    editorStore.changedMask,
  ],
  () => syncMapLayers(editorStore.selectedViewMode),
)

watch(
  () => editorStore.currentMaskGeometry,
  syncSelectionOverlay,
)

onBeforeUnmount(disposeLayers)
</script>

<template>
  <div class="editor-view-wrapper">
    <AppHeader />
    <div class="editor-view">
      <header class="editor-view__topbar" style="height: 56px" data-test="editor-topbar">
      <div class="editor-view__context">
        <span v-if="editorStore.loadingSession" class="editor-view__loading-session">
          正在加载会话...
        </span>
        <template v-else>
          <span>起报: {{ topBarInfo.initTime }}</span>
          <span>累计: {{ topBarInfo.accumHours ?? '-' }}h</span>
          <span>时效: {{ leadRangeLabel }}</span>
          <t-tag :theme="statusTag.theme" variant="light">{{ statusTag.label }}</t-tag>
        </template>
      </div>
      <div class="editor-view__actions">
        <t-button
          data-test="undo-button"
          :disabled="!editorStore.canUndo || editorStore.applyLoading"
          :loading="editorStore.applyLoading"
          @click="editorStore.undoEdit()"
        >
          撤销
        </t-button>
        <t-button
          data-test="redo-button"
          :disabled="!editorStore.canRedo || editorStore.applyLoading"
          :loading="editorStore.applyLoading"
          @click="editorStore.redoEdit()"
        >
          重做
        </t-button>
        <t-button
          data-test="save-button"
          :disabled="!editorStore.dirty || editorStore.saveLoading"
          :loading="editorStore.saveLoading"
          @click="editorStore.saveVersion()"
        >
          保存
        </t-button>
        <t-tooltip
          :content="editorStore.dirty ? '有未保存的更改，请先保存' : '请先保存版本'"
          :disabled="!!editorStore.currentVersionId && !editorStore.dirty"
        >
          <t-button
            data-test="submit-button"
            :disabled="!editorStore.currentVersionId || editorStore.dirty"
            @click="handleSubmit()"
          >
            提交
          </t-button>
        </t-tooltip>
      </div>
    </header>

    <div class="editor-view__body">
      <aside
        class="editor-view__sidebar editor-view__sidebar--left"
        :class="{ 'editor-view__sidebar--collapsed': leftCollapsed }"
        :style="{ width: leftCollapsed ? '0px' : '260px' }"
        data-test="left-sidebar"
      >
        <button
          class="editor-view__collapse-button"
          type="button"
          data-test="left-collapse"
          @click="leftCollapsed = !leftCollapsed"
        >
          {{ leftCollapsed ? '展开' : '收起' }}
        </button>
        <section v-if="!leftCollapsed" class="editor-panel" aria-labelledby="view-mode-title">
          <h2 id="view-mode-title" class="editor-panel__title">视图模式</h2>
          <label v-for="mode in viewModes" :key="mode.value" class="view-mode-option">
            <input
              type="radio"
              name="view-mode"
              :value="mode.value"
              :checked="editorStore.selectedViewMode === mode.value"
              @change="setViewMode(mode.value)"
            >
            <span>{{ mode.label }}</span>
            <small v-if="mode.hint && editorStore.selectedViewMode === mode.value">{{ mode.hint }}</small>
          </label>
        </section>
      </aside>

      <main class="editor-view__center" style="min-width: 600px" data-test="editor-center">
        <div v-if="!hasWindowId" class="editor-view__selector">
          <WindowSelector />
        </div>
        <div v-else class="editor-view__map-shell">
          <BaseMap @map-ready="onMapReady" @grid-hover="onGridHover" />
          <div v-if="editorStore.loadingFields" class="editor-view__map-overlay" data-test="field-loading-overlay">
            <t-loading />
            <span>字段加载中...</span>
          </div>
          <div v-if="editorStore.fieldLoadError" class="editor-view__error" data-test="field-error">
            {{ editorStore.fieldLoadError }}
          </div>
          <div v-if="selectedMode?.hint" class="editor-view__mode-hint" data-test="mode-hint">
            {{ selectedMode.hint }}
          </div>
        </div>
      </main>

      <aside
        class="editor-view__sidebar editor-view__sidebar--right"
        :class="{ 'editor-view__sidebar--collapsed': rightCollapsed }"
        :style="{ width: rightCollapsed ? '0px' : '340px' }"
        data-test="right-sidebar"
      >
        <button
          class="editor-view__collapse-button"
          type="button"
          data-test="right-collapse"
          @click="rightCollapsed = !rightCollapsed"
        >
          {{ rightCollapsed ? '展开' : '收起' }}
        </button>
        <section v-if="!rightCollapsed" class="editor-panel" aria-labelledby="edit-panel-title">
          <h2 id="edit-panel-title" class="editor-panel__title">编辑面板</h2>
          <t-tabs v-model="activeRightTab">
            <t-tab-panel value="mask" label="区域选择">
              <div class="editor-tab" data-test="mask-panel">
                <DrawTools :disabled="drawToolsDisabled" :map="mapInstance" />
              </div>
            </t-tab-panel>
            <t-tab-panel value="qpf" label="降水调整">
              <div
                class="editor-tab"
                :class="{ 'editor-tab--locked': editPanelsDisabled }"
                data-test="qpf-panel"
              >
                <p v-if="editPanelsDisabled" class="editor-tab__hint">请先选择编辑区域</p>
                <template v-else>
                  <div class="editor-tab__control">
                    <label class="editor-tab__label">操作类型</label>
                    <t-select
                      v-model="qpfOperation"
                      :options="qpfOperationOptions"
                      :disabled="panelBusy || editPanelsDisabled"
                      placeholder="选择操作"
                      data-test="qpf-operation"
                    />
                  </div>
                  <div v-if="!qpfValueHidden" class="editor-tab__control">
                    <label class="editor-tab__label">数值</label>
                    <t-input-number
                      v-model="qpfValue"
                      :disabled="panelBusy || editPanelsDisabled"
                      data-test="qpf-value"
                    />
                  </div>
                  <t-button
                    data-test="preview-button"
                    :disabled="qpfPreviewDisabled"
                    :loading="editorStore.previewLoading"
                    @click="handleQpfPreview()"
                  >
                    预览
                  </t-button>
                </template>
                <PreviewStatsPanel />
              </div>
            </t-tab-panel>
            <t-tab-panel value="ptype" label="相态调整">
              <div
                class="editor-tab"
                :class="{ 'editor-tab--locked': editPanelsDisabled }"
                data-test="ptype-panel"
              >
                <p v-if="editPanelsDisabled" class="editor-tab__hint">请先选择编辑区域</p>
                <template v-else>
                  <div class="editor-tab__control">
                    <label class="editor-tab__label">目标相态</label>
                    <t-radio-group v-model="ptypeTarget" :disabled="panelBusy || editPanelsDisabled" data-test="ptype-radio">
                      <t-radio-button :value="1">雨</t-radio-button>
                      <t-radio-button :value="2">雪</t-radio-button>
                      <t-radio-button :value="3">雨夹雪</t-radio-button>
                    </t-radio-group>
                  </div>
                  <t-button
                    data-test="ptype-preview-button"
                    :disabled="ptypePreviewDisabled"
                    :loading="editorStore.previewLoading"
                    @click="handlePtypePreview()"
                  >
                    预览
                  </t-button>
                </template>
                <PreviewStatsPanel />
              </div>
            </t-tab-panel>
            <t-tab-panel value="history" label="操作历史">
              <div class="editor-tab" data-test="history-panel">
                <OperationHistory />
              </div>
            </t-tab-panel>
          </t-tabs>
        </section>
      </aside>
    </div>

    <footer class="editor-view__statusbar" style="height: 36px" data-test="bottom-statusbar">
      <GridTooltip :payload="hoverPayload" :loading="editorStore.loadingFields" />
    </footer>
  </div>

  <t-dialog
    v-model:visible="submitDialogVisible"
    header="确认提交"
    data-test="submit-dialog"
    :close-on-overlay-click="false"
  >
    <p>确认提交该版本进行审核？</p>
    <template #footer>
      <t-button data-test="submit-cancel" @click="submitDialogVisible = false">取消</t-button>
      <t-button theme="primary" data-test="submit-confirm" @click="confirmSubmit()">确认</t-button>
    </template>
  </t-dialog>
  </div>
</template>

<style scoped>
.editor-view-wrapper {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

.editor-view {
  display: flex;
  flex-direction: column;
  min-width: 960px;
  height: calc(100vh - var(--top-nav-height));
  overflow: hidden;
  background: var(--page-bg);
}

.editor-view__topbar {
  display: flex;
  flex: 0 0 56px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  height: 56px;
  padding: 0 16px;
  border-bottom: 1px solid var(--color-border);
  background: var(--card-bg);
}

.editor-view__context,
.editor-view__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.editor-view__context {
  color: var(--text-primary);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
}

.editor-view__loading-session {
  color: var(--color-primary);
}

.editor-view__body {
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
}

.editor-view__sidebar {
  position: relative;
  flex: 0 0 auto;
  min-height: 0;
  border-right: 1px solid var(--color-border);
  background: var(--card-bg);
  transition: width 0.2s ease;
}

.editor-view__sidebar--left {
  width: var(--left-sidebar-width);
}

.editor-view__sidebar--right {
  width: var(--right-sidebar-width);
  border-right: 0;
  border-left: 1px solid var(--color-border);
}

.editor-view__sidebar--collapsed {
  width: 0;
  border-color: transparent;
}

.editor-view__collapse-button {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 2;
  min-width: 44px;
  height: 28px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-control);
  background: var(--card-bg);
  color: var(--text-secondary);
  cursor: pointer;
}

.editor-view__sidebar--collapsed .editor-view__collapse-button {
  right: -52px;
}

.editor-view__sidebar--right.editor-view__sidebar--collapsed .editor-view__collapse-button {
  right: auto;
  left: -52px;
}

.editor-panel {
  height: 100%;
  padding: 48px 14px 14px;
  overflow: auto;
}

.editor-panel__title {
  margin: 0 0 12px;
  font-size: 16px;
  line-height: 24px;
  font-weight: 600;
}

.view-mode-option {
  display: grid;
  grid-template-columns: 18px 1fr auto;
  align-items: center;
  gap: 8px;
  min-height: 34px;
  padding: 6px 4px;
  color: var(--text-primary);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
}

.view-mode-option small {
  color: var(--color-neutral);
  font-size: 12px;
}

.editor-view__center {
  position: relative;
  flex: 1 0 600px;
  min-width: 600px;
  min-height: 0;
  background: var(--page-bg);
}

.editor-view__selector,
.editor-view__map-shell {
  position: relative;
  width: 100%;
  height: 100%;
}

.editor-view__selector {
  overflow: auto;
  padding: 16px;
  background: var(--page-bg);
}

.editor-view__map-shell {
  overflow: hidden;
}

.editor-view__map-overlay,
.editor-view__error,
.editor-view__mode-hint {
  position: absolute;
  z-index: 5;
}

.editor-view__map-overlay {
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: rgb(255 255 255 / 70%);
  color: var(--text-primary);
  font-size: var(--font-size-body);
}

.editor-view__error {
  top: 16px;
  left: 16px;
  max-width: min(520px, calc(100% - 32px));
  border: 1px solid var(--color-danger-bg);
  border-radius: var(--radius-control);
  background: var(--color-danger-bg);
  padding: 10px 12px;
  color: var(--color-danger);
  font-size: 14px;
  line-height: 22px;
}

.editor-view__mode-hint {
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-control);
  background: var(--card-bg);
  padding: 6px 10px;
  color: var(--text-secondary);
  font-size: 13px;
}

.editor-tab {
  padding-top: 12px;
}

.editor-tab--disabled {
  display: grid;
  gap: 10px;
  color: var(--color-neutral);
}

.editor-tab--locked {
  opacity: 0.75;
}

.editor-tab__hint,
.editor-tab__empty {
  margin: 0 0 10px;
  color: var(--color-neutral);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
}

.editor-tab__control {
  display: grid;
  gap: 6px;
  margin-bottom: 10px;
}

.editor-tab__label {
  color: var(--text-secondary);
  font-size: var(--font-size-caption);
  line-height: var(--line-height-caption);
}

.editor-view__statusbar {
  flex: 0 0 var(--bottom-status-height);
  height: var(--bottom-status-height);
  border-top: 1px solid var(--color-border);
  background: var(--color-neutral-bg);
}
</style>
