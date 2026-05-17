<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type Map from 'ol/Map'
import BaseMap from '@/components/map/BaseMap.vue'
import DrawTools from '@/components/map/DrawTools.vue'
import GridTooltip from '@/components/map/GridTooltip.vue'
import { MaskOverlayLayer } from '@/components/map/MaskOverlayLayer'
import { PrecipPhaseGridLayer, getGridDataValue } from '@/components/map/PrecipPhaseGridLayer'
import { SelectionOverlay } from '@/components/map/SelectionOverlay'
import { GRID_COLS } from '@/constants/precipColors'
import WindowSelector from '@/components/WindowSelector.vue'
import { useEditorStore } from '@/stores/editorStore'
import { useWindowStore } from '@/stores/windowStore'
import type { GridHoverPayload, ViewMode } from '@/types/editor'
import type { WindowItem } from '@/api/data'

const route = useRoute()
const router = useRouter()
const editorStore = useEditorStore()
const windowStore = useWindowStore()

const leftCollapsed = ref(false)
const rightCollapsed = ref(false)
const activeRightTab = ref('mask')
const hoverPayload = ref<GridHoverPayload | null>(null)
const mapInstance = ref<Map | null>(null)

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
const editPanelsDisabled = computed(() => editorStore.currentMaskGeometry === null)
const selectedMode = computed(() => viewModes.find((mode) => mode.value === editorStore.selectedViewMode))

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
        <t-button data-test="undo-button" disabled>撤销</t-button>
        <t-button data-test="redo-button" disabled>重做</t-button>
        <t-button data-test="save-button" disabled>保存</t-button>
        <t-button data-test="submit-button" disabled>提交</t-button>
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
            />
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
                class="editor-tab editor-tab--disabled"
                :class="{ 'editor-tab--locked': editPanelsDisabled }"
                data-test="qpf-panel"
              >
                <p v-if="editPanelsDisabled" class="editor-tab__hint">请先选择编辑区域</p>
                <p v-else class="editor-tab__hint">降水调整将在 M3 开放</p>
                <t-button data-test="preview-button" disabled>预览</t-button>
                <t-button data-test="apply-button" disabled>应用</t-button>
              </div>
            </t-tab-panel>
            <t-tab-panel value="ptype" label="相态调整">
              <div
                class="editor-tab editor-tab--disabled"
                :class="{ 'editor-tab--locked': editPanelsDisabled }"
                data-test="ptype-panel"
              >
                <p v-if="editPanelsDisabled" class="editor-tab__hint">请先选择编辑区域</p>
                <p v-else class="editor-tab__hint">相态调整将在 M3 开放</p>
                <t-button data-test="ptype-preview-button" disabled>预览</t-button>
                <t-button data-test="ptype-apply-button" disabled>应用</t-button>
              </div>
            </t-tab-panel>
            <t-tab-panel value="history" label="操作历史">
              <div class="editor-tab" data-test="history-panel">
                <p class="editor-tab__empty">暂无操作记录</p>
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
</template>

<style scoped>
.editor-view {
  display: flex;
  flex-direction: column;
  min-width: 960px;
  height: 100vh;
  overflow: hidden;
  background: #eef2f7;
}

.editor-view__topbar {
  display: flex;
  flex: 0 0 56px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  height: 56px;
  padding: 0 16px;
  border-bottom: 1px solid #d9e1ec;
  background: #ffffff;
}

.editor-view__context,
.editor-view__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.editor-view__context {
  color: #1d2129;
  font-size: 14px;
  line-height: 22px;
}

.editor-view__loading-session {
  color: #0052d9;
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
  border-right: 1px solid #d9e1ec;
  background: #ffffff;
  transition: width 0.2s ease;
}

.editor-view__sidebar--left {
  width: 260px;
}

.editor-view__sidebar--right {
  width: 340px;
  border-right: 0;
  border-left: 1px solid #d9e1ec;
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
  border: 1px solid #c9cdd4;
  border-radius: 4px;
  background: #ffffff;
  color: #4e5969;
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
  color: #1d2129;
  font-size: 14px;
  line-height: 22px;
}

.view-mode-option small {
  color: #86909c;
  font-size: 12px;
}

.editor-view__center {
  position: relative;
  flex: 1 0 600px;
  min-width: 600px;
  min-height: 0;
  background: #dfe7f1;
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
  background: #f5f7fa;
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
  color: #1d2129;
  font-size: 14px;
}

.editor-view__error {
  top: 16px;
  left: 16px;
  max-width: min(520px, calc(100% - 32px));
  border: 1px solid #f5c6c3;
  border-radius: 6px;
  background: #fff2f0;
  padding: 10px 12px;
  color: #d54941;
  font-size: 14px;
  line-height: 22px;
}

.editor-view__mode-hint {
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border: 1px solid #d9e1ec;
  border-radius: 4px;
  background: #ffffff;
  padding: 6px 10px;
  color: #4e5969;
  font-size: 13px;
}

.editor-tab {
  padding-top: 12px;
}

.editor-tab--disabled {
  display: grid;
  gap: 10px;
  color: #86909c;
}

.editor-tab--locked {
  opacity: 0.75;
}

.editor-tab__hint,
.editor-tab__empty {
  margin: 0 0 10px;
  color: #86909c;
  font-size: 14px;
  line-height: 22px;
}

.editor-view__statusbar {
  flex: 0 0 36px;
  height: 36px;
  border-top: 1px solid #d9e1ec;
  background: #f8fafc;
}
</style>
