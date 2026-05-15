<template>
  <div ref="tooltipRef" class="tooltip" v-show="tooltipVisible">
    <div>经度：{{ lon }}</div>
    <div>纬度：{{ lat }}</div>
  </div>
</template>

<script setup>
import { Overlay } from 'ol'
import { toLonLat } from 'ol/proj'
import {onBeforeUnmount, ref} from 'vue'

const tooltipRef = ref(null)
const tooltipVisible = ref(false)

let mapObj, overlay
let lon = ref('')
let lat = ref('')
const pointerMoveHandler = (evt)=>{
  const [longitude, latitude] = toLonLat(evt.coordinate)
  lon.value = longitude.toFixed(4)
  lat.value = latitude.toFixed(4)
  overlay.setPosition(evt.coordinate)
  tooltipVisible.value = true
}

const install = (map)=>{
  mapObj = map
  overlay = new Overlay({
    element: tooltipRef.value,
    offset: [10, -50],
    positioning: 'top-right'
  })
  map.addOverlay(overlay)
  map.on('pointermove', pointerMoveHandler)
}

onBeforeUnmount(()=>{
  if(mapObj) { 
    if(pointerMoveHandler) {
      mapObj.un('pointermove', pointerMoveHandler)
    }
    if(overlay) {
      mapObj.removeOverlay(overlay)
    }
    mapObj.setTarget(null)
    mapObj = null
  }
})


defineExpose({
  install
})

</script>

<style lang="less" scoped>
.tooltip {
  position: absolute;
  bottom: 2px;
  width: 200px;
  padding: 4px 8px;
  font-size: 12px;
  box-shadow: 0px 3px 6px 0px rgba(19, 29, 80, 0.4);
  background-color: rgba(0,0,0,0.2);
  border: 1px solid #ccc;
  white-space: nowrap;
}
</style>