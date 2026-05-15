<template>
  <div class="h-full relative">
    <SearchStation @searchChange="searchChange"></SearchStation>
    <!-- <div class="right-bottom"></div> -->
    <Comparision></Comparision>
  </div>

</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount } from 'vue'
// import LayoutManager from "@/layouts/LayoutManager.vue"
import Comparision from './components/Comparision.vue'
import SearchStation from './components/SearchStation.vue'
import StationLayer from '@/layers/stationLayer'
import { DisplayService } from '@/api'


let stationLayer: any = null

const addStationLayer = async () => {
  try {
    const res = await DisplayService.getDifferentStations()
    if (res && res.length > 0 && olMap) {
      // 去掉flag=1的站点
      let stations0_2 = res.filter(item => item.flag === 0 || item.flag === 2)
      
      // 创建站点图层并添加到地图
      stationLayer = new StationLayer(olMap.getMap(), stations0_2);
      olMap.getMap().addLayer(stationLayer);
    }
  } catch (error) { }
}

const removeStationLayer = () => {
  stationLayer && olMap && olMap.getMap().removeLayer(stationLayer)
  stationLayer = null
}

const searchChange = (n: any) => {
  stationLayer?.updateSource(n.stationIdD)
  window.$bus.emit('searchTable', n)
}


nextTick(() => {
  addStationLayer()
})

onBeforeUnmount(() => {
  removeStationLayer()
})

</script>

<style lang="less" scoped>
.content {
  height: 100%;
  background-color: #fff;
  position: relative;
  z-index: 2;

}

.right-bottom {
  position: absolute;
  z-index: 1;
  bottom: 26px;
  right: 15px;
  width: 264px;
  height: 56px;
  background: url('@/assets/img/rain-card.png') no-repeat center;
  background-size: contain;

  opacity: 0.98;

}
</style>
