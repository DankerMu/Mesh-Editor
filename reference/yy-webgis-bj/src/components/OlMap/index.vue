<template>
  <div id="ol-map" ref="mapContainer" class="map-container" />

  <div class="position">经度：{{ lon }}，纬度：{{ lat }}</div>
</template>

<script setup lang="ts">
import "ol/ol.css";
import OlMap, { baseLayerList } from './OlMap'
import TileLayer from 'ol/layer/Tile'
import XYZ from 'ol/source/XYZ'
import { View } from 'ol'
import { toLonLat, fromLonLat, transformExtent } from "ol/proj";

const projectionStore = useProjectionStore()

const mapContainer = ref<HTMLElement | null>(null);
let olMap: OlMap | null = null
let lon = ref('')
let lat = ref('')

function initMap() {
  if (mapContainer.value) {
    olMap = new OlMap(
      mapContainer.value,
      {
        layers: [],
        center: [90, 37.5],
        zoom: 5,
        minZoom: 2,
        maxZoom: 17,
        extent: transformExtent([-180, -90, 180, 90], 'EPSG:4326', 'EPSG:3857')
      }
    )
    window.olMap = olMap

    initBaseLayer()

    olMap.getMap().on('pointermove', pointerMoveHandler)
  }
}

function initBaseLayer() {
  baseLayerList.forEach(item => {
    olMap?.addBaseLayer(item.key, new TileLayer({
      source: new XYZ({
        url: item.url
      })
    }))
  })
}

const pointerMoveHandler = (evt) => {
  const [longitude, latitude] = toLonLat(evt.coordinate)
  lon.value = longitude.toFixed(4)
  lat.value = latitude.toFixed(4)
}

watch(() => projectionStore.currentProjection, (newProjection) => {
  if (olMap && olMap.getView()) {
    const currentProj = olMap.getView().getProjection().getCode();
    if (currentProj !== newProjection) {
      const newViewOptions = projectionStore.projectionDefs[newProjection].options
      if (!newViewOptions) return

      const newView = new View({
        ...newViewOptions,
        center: fromLonLat(newViewOptions.center, newProjection),
        extent: (newViewOptions.extent && transformExtent(newViewOptions.extent, 'EPSG:4326', newProjection)) || undefined,
      })

      olMap.setView(newView)
    }
  }
});


onMounted(() => {
  initMap()
});

onUnmounted(() => {
  if (pointerMoveHandler) {
    olMap.getMap().un('pointermove', pointerMoveHandler)
  }
  olMap?.destroy()
  olMap = null
});
</script>

<style scoped>
.map-container {
  width: 100%;
  height: calc(100vh - 4.375rem);
  position: absolute;
  left: 0;
  top: 4.375rem;
  z-index: 1;
}

.position {
  position: absolute;
  bottom: 2px;
  left: 50%;
  width: 190px;
  transform: translateX(-50%);
  letter-spacing: 1px;
  z-index: 2;
  color: black;
}
</style>
