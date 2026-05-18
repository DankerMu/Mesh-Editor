<template>
  <div ref="mapContainer" class="base-map" />
</template>

<script setup lang="ts">
import 'ol/ol.css'
import { onBeforeUnmount, onMounted, ref } from 'vue'
import Map from 'ol/Map'
import View from 'ol/View'
import TileLayer from 'ol/layer/Tile'
import OSM from 'ol/source/OSM'
import type { EventsKey } from 'ol/events'
import { unByKey } from 'ol/Observable'
import type { GridHoverPayload } from '@/types/editor'
import { getGridIndex } from './PrecipPhaseGridLayer'

const GRID_EXTENT = [70, 25, 111, 50] as const
const HOVER_THROTTLE_MS = 50

const emit = defineEmits<{
  'map-ready': [map: Map]
  'grid-hover': [payload: GridHoverPayload | null]
}>()

const mapContainer = ref<HTMLDivElement | null>(null)
let map: Map | null = null
let pointerMoveKey: EventsKey | null = null
let lastHoverAt = Number.NEGATIVE_INFINITY

function emitHoverPayload(coordinate: number[]): void {
  const [lon, lat] = coordinate
  const index = getGridIndex(lon, lat)

  if (!index) {
    emit('grid-hover', null)
    return
  }

  emit('grid-hover', {
    lon,
    lat,
    gridI: index.gridI,
    gridJ: index.gridJ,
    qpfBefore: null,
    qpfAfter: null,
    ptypeBefore: null,
    ptypeAfter: null,
    isEdited: false,
    inBounds: true,
  })
}

onMounted(() => {
  if (!mapContainer.value) {
    return
  }

  const baseLayer = new TileLayer({
    source: new OSM({
      wrapX: false,
    }),
  })

  map = new Map({
    target: mapContainer.value,
    layers: [baseLayer],
    view: new View({
      projection: 'EPSG:4326',
      center: [90.5, 37.5],
      extent: [...GRID_EXTENT],
      zoom: 5,
    }),
  })

  pointerMoveKey = map.on('pointermove', (event) => {
    const now = window.performance.now()

    if (now - lastHoverAt < HOVER_THROTTLE_MS) {
      return
    }

    lastHoverAt = now
    emitHoverPayload(event.coordinate)
  })

  emit('map-ready', map)
})

onBeforeUnmount(() => {
  if (!map) {
    return
  }

  if (pointerMoveKey) {
    unByKey(pointerMoveKey)
    pointerMoveKey = null
  }

  map.getLayers().clear()
  ;(map as { setTarget(target: HTMLElement | string | null): void }).setTarget(null)
  map = null
})
</script>

<style scoped>
.base-map {
  width: 100%;
  height: 100%;
  min-height: 360px;
}
</style>
