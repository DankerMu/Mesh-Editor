<script setup lang="ts">
import { computed, onBeforeUnmount, ref, shallowRef, watch } from 'vue'
import type Map from 'ol/Map'
import BaseMap from '@/components/map/BaseMap.vue'
import { MaskOverlayLayer } from '@/components/map/MaskOverlayLayer'
import { PrecipPhaseGridLayer } from '@/components/map/PrecipPhaseGridLayer'
import {
  FloatGridLayer,
  IntGridLayer,
  getChangePtypeColor,
  getDeltaQpfColor,
} from '@/components/map/RgbaGridLayer'
import { GRID_COLS, GRID_ROWS } from '@/constants/precipColors'
import { getVersionField } from '@/api/version'
import type { VersionFieldName } from '@/api/version'

type FieldNamePair = {
  qpf: VersionFieldName
  ptype: VersionFieldName
}

const props = defineProps<{
  versionId: string
  fieldName?: VersionFieldName
  fieldNames?: FieldNamePair
}>()

const emit = defineEmits<{
  'map-ready': [map: Map]
}>()

const expectedGridLength = GRID_ROWS * GRID_COLS
const loading = ref(false)
const error = ref('')
const loaded = ref(false)
const mapInstance = shallowRef<Map | null>(null)
const dataVersion = ref(0)

let activeLayer: PrecipPhaseGridLayer | MaskOverlayLayer | FloatGridLayer | IntGridLayer | null = null
let requestSeq = 0

const mode = computed(() => {
  if (props.fieldNames) {
    return 'precip'
  }

  if (props.fieldName === 'delta_qpf') {
    return 'delta'
  }

  if (props.fieldName === 'change_ptype') {
    return 'ptype-change'
  }

  if (props.fieldName === 'touched_mask' || props.fieldName === 'changed_mask') {
    return 'mask'
  }

  return 'unsupported'
})

function onMapReady(map: Map): void {
  mapInstance.value = map
  emit('map-ready', map)
  void loadFieldData()
}

async function loadFieldData(): Promise<void> {
  const map = mapInstance.value

  if (!map) {
    return
  }

  const seq = (requestSeq += 1)
  loading.value = true
  error.value = ''
  loaded.value = false

  try {
    const layer = await createLayer()

    if (seq !== requestSeq) {
      disposeLayer(layer)
      return
    }

    setActiveLayer(map, layer)
    loaded.value = true
    dataVersion.value += 1
  } catch (loadError) {
    if (seq !== requestSeq) {
      return
    }

    removeActiveLayer()
    error.value = loadError instanceof Error ? loadError.message : '字段数据加载失败'
  } finally {
    if (seq === requestSeq) {
      loading.value = false
    }
  }
}

async function createLayer(): Promise<PrecipPhaseGridLayer | MaskOverlayLayer | FloatGridLayer | IntGridLayer> {
  if (props.fieldNames) {
    const [qpfBuffer, ptypeBuffer] = await Promise.all([
      getVersionField(props.versionId, props.fieldNames.qpf),
      getVersionField(props.versionId, props.fieldNames.ptype),
    ])
    assertByteLength(qpfBuffer, expectedGridLength * Float32Array.BYTES_PER_ELEMENT, props.fieldNames.qpf)
    assertByteLength(ptypeBuffer, expectedGridLength * Uint8Array.BYTES_PER_ELEMENT, props.fieldNames.ptype)

    const layer = new PrecipPhaseGridLayer()
    layer.updateData(new Float32Array(qpfBuffer), new Uint8Array(ptypeBuffer))
    return layer
  }

  if (!props.fieldName) {
    throw new Error('缺少字段名称')
  }

  const buffer = await getVersionField(props.versionId, props.fieldName)

  if (props.fieldName === 'delta_qpf') {
    assertByteLength(buffer, expectedGridLength * Float32Array.BYTES_PER_ELEMENT, props.fieldName)
    const layer = new FloatGridLayer(getDeltaQpfColor)
    layer.updateData(new Float32Array(buffer))
    return layer
  }

  if (props.fieldName === 'change_ptype') {
    assertByteLength(buffer, expectedGridLength * Int8Array.BYTES_PER_ELEMENT, props.fieldName)
    const layer = new IntGridLayer(getChangePtypeColor)
    layer.updateData(new Int8Array(buffer))
    return layer
  }

  if (props.fieldName === 'touched_mask' || props.fieldName === 'changed_mask') {
    assertByteLength(buffer, expectedGridLength * Uint8Array.BYTES_PER_ELEMENT, props.fieldName)
    const color: [number, number, number, number] =
      props.fieldName === 'touched_mask' ? [22, 93, 255, 130] : [213, 73, 65, 150]
    const layer = new MaskOverlayLayer(color)
    layer.updateData(new Uint8Array(buffer))
    return layer
  }

  throw new Error(`不支持的字段：${props.fieldName}`)
}

function assertByteLength(buffer: ArrayBuffer, expectedLength: number, fieldName: VersionFieldName): void {
  if (buffer.byteLength !== expectedLength) {
    throw new Error(`${fieldName} 字节长度不匹配`)
  }
}

function setActiveLayer(
  map: Map,
  layer: PrecipPhaseGridLayer | MaskOverlayLayer | FloatGridLayer | IntGridLayer,
): void {
  removeActiveLayer()
  activeLayer = layer
  map.addLayer(layer.getLayer())
}

function removeActiveLayer(): void {
  if (mapInstance.value && activeLayer) {
    mapInstance.value.removeLayer(activeLayer.getLayer())
  }

  if (activeLayer) {
    disposeLayer(activeLayer)
  }
  activeLayer = null
}

function disposeLayer(layer: PrecipPhaseGridLayer | MaskOverlayLayer | FloatGridLayer | IntGridLayer): void {
  layer.dispose()
}

watch(
  () => [props.versionId, props.fieldName, props.fieldNames?.qpf, props.fieldNames?.ptype],
  () => {
    void loadFieldData()
  },
)

onBeforeUnmount(() => {
  requestSeq += 1
  removeActiveLayer()
})
</script>

<template>
  <div
    class="version-field-map"
    :data-test="fieldName ? `version-field-map-${fieldName}` : 'version-field-map-precip'"
    :data-loaded="loaded ? 'true' : 'false'"
    :data-mode="mode"
    :data-version="dataVersion"
  >
    <BaseMap @map-ready="onMapReady" />
    <div v-if="loading" class="version-field-map__overlay" data-test="version-field-loading">
      <t-loading />
      <span>字段加载中...</span>
    </div>
    <div v-if="error" class="version-field-map__error" data-test="version-field-error">
      {{ error }}
    </div>
  </div>
</template>

<style scoped>
.version-field-map {
  position: relative;
  width: 100%;
  min-height: 320px;
  overflow: hidden;
  border: 1px solid #d9e1ec;
  border-radius: 8px;
  background: #f7f8fa;
}

.version-field-map__overlay,
.version-field-map__error {
  position: absolute;
  inset: 0;
  z-index: 2;
  display: grid;
  place-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.78);
  color: #4e5969;
  font-size: 14px;
}

.version-field-map__error {
  color: #d54941;
}
</style>
