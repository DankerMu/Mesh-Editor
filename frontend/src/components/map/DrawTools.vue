<template>
  <section class="draw-tools" aria-label="选区工具">
    <div class="draw-tools__buttons">
      <button
        v-for="tool in DRAW_TOOLS"
        :key="tool.value"
        class="draw-tools__button"
        :class="{ 'draw-tools__button--active': activeTool === tool.value }"
        type="button"
        :disabled="disabled"
        :aria-pressed="activeTool === tool.value"
        :data-test="`tool-${tool.value}`"
        @click="activateTool(tool.value)"
      >
        {{ tool.label }}
      </button>
      <button
        class="draw-tools__button draw-tools__button--clear"
        type="button"
        :disabled="disabled"
        data-test="tool-clear-selection"
        @click="clearSelection"
      >
        清除选区
      </button>
    </div>

    <label v-if="activeTool === 'line_buffer'" class="draw-tools__field">
      <span>宽度格数</span>
      <input
        :value="widthGrid"
        class="draw-tools__number"
        type="number"
        min="1"
        max="50"
        step="1"
        data-test="width-grid-input"
        @input="onWidthInput"
        @blur="widthGrid = clampGridValue(widthGrid, 1, 50)"
      >
    </label>

    <div v-if="activeTool === 'brush_path'" class="draw-tools__field" data-test="radius-grid-display">
      <span>半径格数</span>
      <strong>{{ radiusGrid }}</strong>
    </div>

    <div
      v-if="activeTool === 'brush_path' && brushCursor.visible"
      class="draw-tools__brush-cursor"
      :style="brushCursorStyle"
      aria-hidden="true"
    />
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import Draw from 'ol/interaction/Draw'
import VectorSource from 'ol/source/Vector'
import type Map from 'ol/Map'
import type { Coordinate } from 'ol/coordinate'
import type { Pixel } from 'ol/pixel'
import type { EventsKey } from 'ol/events'
import type { DrawEvent } from 'ol/interaction/Draw'
import LineString from 'ol/geom/LineString'
import Polygon from 'ol/geom/Polygon'
import { unByKey } from 'ol/Observable'
import { useEditorStore } from '@/stores/editorStore'
import type { BrushPathGeometry, LineBufferGeometry, MaskGeometry, PolygonGeometry, ToolType } from '@/types/editor'

type LonLat = [number, number]

const DRAW_TOOLS: Array<{ value: ToolType; label: string }> = [
  { value: 'polygon', label: '多边形' },
  { value: 'line_buffer', label: '线缓冲' },
  { value: 'brush_path', label: '画刷' },
]

const props = withDefaults(
  defineProps<{
    disabled: boolean
    map?: Map | null
  }>(),
  {
    map: null,
  },
)

const emit = defineEmits<{
  'mask-created': [geometry: MaskGeometry]
}>()

const editorStore = useEditorStore()
const widthGrid = ref(5)
const radiusGrid = ref(3)
const isBrushing = ref(false)
const brushCursor = reactive({
  visible: false,
  x: 0,
  y: 0,
})

let drawSource: VectorSource | null = null
let drawInteraction: Draw | null = null
let drawInteractionMap: Map | null = null
let drawEndKey: EventsKey | null = null
let brushEventKeys: EventsKey[] = []
let brushPoints: LonLat[] = []
let lastBrushPixel: Pixel | null = null
let lastBrushPointAt = 0
let wheelTarget: HTMLElement | null = null
let contextMenuTarget: HTMLElement | null = null

const activeTool = computed(() => editorStore.activeTool)
const brushCursorStyle = computed(() => {
  const diameter = radiusGrid.value * 2 * 6

  return {
    width: `${diameter}px`,
    height: `${diameter}px`,
    transform: `translate(${brushCursor.x - diameter / 2}px, ${brushCursor.y - diameter / 2}px)`,
  }
})

function clampGridValue(value: number, min: number, max: number): number {
  if (!Number.isFinite(value)) {
    return min
  }

  return Math.min(max, Math.max(min, Math.round(value)))
}

function toLonLat(coordinate: Coordinate): LonLat {
  return [Number(coordinate[0]), Number(coordinate[1])]
}

function sameCoordinate(a: LonLat, b: LonLat): boolean {
  return a[0] === b[0] && a[1] === b[1]
}

function normalizePolygonCoordinates(coordinates: Coordinate[][]): LonLat[] {
  const ring = coordinates[0] ?? []
  const normalized = ring.map(toLonLat)

  if (normalized.length > 1 && sameCoordinate(normalized[0], normalized[normalized.length - 1])) {
    normalized.pop()
  }

  return normalized
}

function emitMask(geometry: MaskGeometry): void {
  editorStore.setMaskGeometry(geometry)
  emit('mask-created', geometry)
}

function emitPolygon(points: LonLat[]): void {
  if (points.length < 3) {
    return
  }

  const geometry: PolygonGeometry = {
    type: 'polygon',
    coordinates: points,
  }

  emitMask(geometry)
}

function emitLineBuffer(points: LonLat[]): void {
  if (points.length < 2) {
    return
  }

  const geometry: LineBufferGeometry = {
    type: 'line_buffer',
    coordinates: points,
    width_grid: clampGridValue(widthGrid.value, 1, 50),
  }

  emitMask(geometry)
}

function emitBrushPath(): void {
  if (brushPoints.length === 0) {
    return
  }

  const geometry: BrushPathGeometry = {
    type: 'brush_path',
    points: [...brushPoints],
    radius_grid: clampGridValue(radiusGrid.value, 1, 30),
  }

  emitMask(geometry)
}

function onWidthInput(event: Event): void {
  const target = event.target as HTMLInputElement
  widthGrid.value = clampGridValue(Number(target.value), 1, 50)
}

function activateTool(tool: ToolType): void {
  if (props.disabled) {
    return
  }

  const nextTool = activeTool.value === tool ? null : tool
  cancelDrawing()
  editorStore.setActiveTool(nextTool)
}

function clearSelection(): void {
  if (props.disabled) {
    return
  }

  cancelDrawing()
  editorStore.clearMask()
}

function removeDrawInteraction(): void {
  if (drawEndKey) {
    unByKey(drawEndKey)
    drawEndKey = null
  }

  if (drawInteraction && drawInteractionMap) {
    drawInteractionMap.removeInteraction(drawInteraction)
  }

  drawSource?.clear()
  drawSource = null
  drawInteraction = null
  drawInteractionMap = null
}

function setupDrawInteraction(tool: 'polygon' | 'line_buffer'): void {
  if (!props.map) {
    return
  }

  drawSource = new VectorSource({ wrapX: false })
  drawInteraction = new Draw({
    source: drawSource,
    type: tool === 'polygon' ? 'Polygon' : 'LineString',
    minPoints: tool === 'polygon' ? 3 : 2,
    stopClick: true,
    wrapX: false,
  })

  drawEndKey = drawInteraction.on('drawend', (event: DrawEvent) => {
    const geometry = event.feature.getGeometry()

    if (tool === 'polygon' && geometry instanceof Polygon) {
      emitPolygon(normalizePolygonCoordinates(geometry.getCoordinates()))
    }

    if (tool === 'line_buffer' && geometry instanceof LineString) {
      emitLineBuffer(geometry.getCoordinates().map(toLonLat))
    }
  })

  drawInteractionMap = props.map
  drawInteractionMap.addInteraction(drawInteraction)
}

function removeBrushHandlers(): void {
  if (brushEventKeys.length > 0) {
    unByKey(brushEventKeys)
    brushEventKeys = []
  }

  if (wheelTarget) {
    wheelTarget.removeEventListener('wheel', onBrushWheel)
    wheelTarget = null
  }

  isBrushing.value = false
  brushCursor.visible = false
}

function getOriginalEvent(event: unknown): Event | null {
  return typeof event === 'object' && event !== null && 'originalEvent' in event
    ? ((event as { originalEvent?: Event }).originalEvent ?? null)
    : null
}

function getMapCoordinate(event: unknown): LonLat | null {
  if (typeof event !== 'object' || event === null || !('coordinate' in event)) {
    return null
  }

  const coordinate = (event as { coordinate?: Coordinate }).coordinate
  return coordinate ? toLonLat(coordinate) : null
}

function updateBrushCursor(event: unknown): void {
  if (typeof event !== 'object' || event === null || !('pixel' in event)) {
    brushCursor.visible = false
    return
  }

  const pixel = (event as { pixel?: [number, number] }).pixel
  if (!pixel) {
    brushCursor.visible = false
    return
  }

  brushCursor.x = pixel[0]
  brushCursor.y = pixel[1]
  brushCursor.visible = true
}

function getMapPixel(event: unknown): Pixel | null {
  if (typeof event !== 'object' || event === null || !('pixel' in event)) {
    return null
  }

  const pixel = (event as { pixel?: Pixel }).pixel
  return pixel ?? null
}

function appendBrushPoint(event: unknown): boolean {
  const coordinate = getMapCoordinate(event)
  if (!coordinate) {
    return false
  }

  brushPoints.push(coordinate)
  lastBrushPixel = getMapPixel(event)
  lastBrushPointAt = Date.now()
  return true
}

function shouldAppendBrushPoint(event: unknown): boolean {
  if (brushPoints.length === 0) {
    return true
  }

  const now = Date.now()
  if (now - lastBrushPointAt >= 16) {
    return true
  }

  const pixel = getMapPixel(event)
  if (!pixel || !lastBrushPixel) {
    return false
  }

  return Math.hypot(pixel[0] - lastBrushPixel[0], pixel[1] - lastBrushPixel[1]) >= 4
}

function isPrimaryPointerEvent(event: unknown): boolean {
  const originalEvent = getOriginalEvent(event)

  return !(originalEvent instanceof PointerEvent) || originalEvent.button === 0
}

function onBrushPointerDown(event: unknown): void {
  if (activeTool.value !== 'brush_path' || !isPrimaryPointerEvent(event)) {
    return
  }

  isBrushing.value = true
  appendBrushPoint(event)
  updateBrushCursor(event)
}

function onBrushPointerDrag(event: unknown): void {
  if (!isBrushing.value || activeTool.value !== 'brush_path') {
    return
  }

  if (shouldAppendBrushPoint(event)) {
    appendBrushPoint(event)
  }
  updateBrushCursor(event)
}

function onBrushPointerMove(event: unknown): void {
  if (activeTool.value === 'brush_path') {
    updateBrushCursor(event)
  }
}

function onBrushPointerUp(event: unknown): void {
  if (!isBrushing.value || activeTool.value !== 'brush_path') {
    return
  }

  appendBrushPoint(event)
  updateBrushCursor(event)
  isBrushing.value = false
  emitBrushPath()
}

function adjustBrushRadius(delta: number): void {
  radiusGrid.value = clampGridValue(radiusGrid.value + delta, 1, 30)
}

function onBrushWheel(event: WheelEvent): void {
  if (activeTool.value !== 'brush_path') {
    return
  }

  event.preventDefault()
  adjustBrushRadius(event.deltaY < 0 ? 1 : -1)
}

function setupBrushHandlers(): void {
  if (!props.map) {
    return
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any -- OL runtime dispatches these events but TS defs omit them
  const mapAny = props.map as any
  brushEventKeys = [
    mapAny.on('pointerdown', onBrushPointerDown),
    mapAny.on('pointerdrag', onBrushPointerDrag),
    mapAny.on('pointermove', onBrushPointerMove),
    mapAny.on('pointerup', onBrushPointerUp),
  ]

  wheelTarget = props.map.getViewport()
  wheelTarget.addEventListener('wheel', onBrushWheel, { passive: false })
}

function cancelDrawing(): void {
  drawInteraction?.abortDrawing()
  removeDrawInteraction()
  removeBrushHandlers()
  isBrushing.value = false
}

function clearBrushPath(): void {
  brushPoints = []
  lastBrushPixel = null
  lastBrushPointAt = 0
  isBrushing.value = false
  brushCursor.visible = false
}

function onKeydown(event: KeyboardEvent): void {
  if (event.ctrlKey && event.shiftKey && event.key.toLowerCase() === 'a') {
    event.preventDefault()
    clearSelection()
    return
  }

  if (props.disabled || activeTool.value === null) {
    return
  }

  if (event.key === 'Escape') {
    event.preventDefault()
    if (activeTool.value === 'brush_path') {
      clearBrushPath()
      editorStore.clearMask()
    } else {
      drawInteraction?.abortDrawing()
    }
    return
  }

  if (event.key === 'Backspace' && activeTool.value === 'polygon') {
    event.preventDefault()
    drawInteraction?.removeLastPoint()
    return
  }

  if (activeTool.value === 'brush_path' && event.key === '[') {
    event.preventDefault()
    adjustBrushRadius(-1)
  }

  if (activeTool.value === 'brush_path' && event.key === ']') {
    event.preventDefault()
    adjustBrushRadius(1)
  }
}

function onContextMenu(event: MouseEvent): void {
  if (activeTool.value !== 'polygon') {
    return
  }

  event.preventDefault()
  drawInteraction?.abortDrawing()
}

function bindContextMenu(): void {
  if (!props.map) {
    return
  }

  contextMenuTarget?.removeEventListener('contextmenu', onContextMenu)
  contextMenuTarget = props.map.getViewport()
  contextMenuTarget.addEventListener('contextmenu', onContextMenu)
}

watch(
  () => activeTool.value,
  (tool) => {
    cancelDrawing()
    clearBrushPath()

    if (tool === 'polygon' || tool === 'line_buffer') {
      setupDrawInteraction(tool)
    }

    if (tool === 'brush_path') {
      setupBrushHandlers()
    }
  },
)

watch(
  () => props.map,
  () => {
    cancelDrawing()
    bindContextMenu()

    if (activeTool.value === 'polygon' || activeTool.value === 'line_buffer') {
      setupDrawInteraction(activeTool.value)
    }

    if (activeTool.value === 'brush_path') {
      setupBrushHandlers()
    }
  },
)

watch(
  () => props.disabled,
  (disabled) => {
    if (disabled) {
      cancelDrawing()
      editorStore.setActiveTool(null)
    }
  },
)

onMounted(() => {
  window.addEventListener('keydown', onKeydown)
  bindContextMenu()
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  contextMenuTarget?.removeEventListener('contextmenu', onContextMenu)
  contextMenuTarget = null
  cancelDrawing()
})

function completePolygonForTest(points: LonLat[]): void {
  emitPolygon(points)
}

function completeLineBufferForTest(points: LonLat[]): void {
  emitLineBuffer(points)
}

function completeBrushStrokeForTest(points: LonLat[]): void {
  brushPoints.push(...points)
  emitBrushPath()
}

defineExpose({
  activateTool,
  adjustBrushRadius,
  cancelDrawing,
  clearSelection,
  completeBrushStrokeForTest,
  completeLineBufferForTest,
  completePolygonForTest,
  radiusGrid,
  widthGrid,
})
</script>

<style scoped>
.draw-tools {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.draw-tools__buttons {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.draw-tools__button {
  min-height: 34px;
  border: 1px solid #c9cdd4;
  border-radius: 6px;
  background: #ffffff;
  color: #1d2129;
  font-size: 13px;
  line-height: 20px;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease,
    color 0.2s ease;
}

.draw-tools__button:hover:not(:disabled) {
  border-color: #165dff;
  color: #165dff;
}

.draw-tools__button:disabled {
  color: #86909c;
  background: #f2f3f5;
  cursor: not-allowed;
}

.draw-tools__button--active {
  border-color: #165dff;
  background: #e8f3ff;
  color: #165dff;
  font-weight: 600;
}

.draw-tools__button--clear {
  grid-column: span 2;
}

.draw-tools__field {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 34px;
  color: #4e5969;
  font-size: 13px;
  line-height: 20px;
}

.draw-tools__number {
  width: 84px;
  height: 30px;
  border: 1px solid #c9cdd4;
  border-radius: 6px;
  padding: 0 8px;
  color: #1d2129;
}

.draw-tools__brush-cursor {
  position: absolute;
  top: 0;
  left: 0;
  pointer-events: none;
  border: 1px solid #165dff;
  border-radius: 50%;
  background: rgb(22 93 255 / 12%);
}
</style>
