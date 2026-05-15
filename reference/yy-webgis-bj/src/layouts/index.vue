<template>
  <div class="layout">
    <!-- 顶部 -->
    <Header />

    <!-- OpenLayers地图 -->
    <OlMap />

    <!-- MapToolsPanel右下 -->
    <MapToolsPanel v-if="mapPanelOptions.mapTools" />

    <!-- bottomPanel -->
    <!-- <Timeline /> -->
    <Timeline v-if="mapPanelOptions.timeline" :startTime="startTime" :endTime="endTime"
      v-model:currentPoint="currentPoint" />
    <!-- 内容 -->
    <div class="content">
      <router-view v-slot="{ Component }">
        <!-- <keep-alive :exclude="['CustomLeft']"> -->
          <component :is="Component" />
        <!-- </keep-alive> -->
      </router-view>
    </div>

    <!-- 一定存在的组件 -->
    <!-- <t-input size="large" class="yy left-top w-100" style="border-width: 0; border-radius: 8px;"
      placeholder="请输入要搜索的内容"></t-input> -->

    <!-- <div class="right-top">
      <div class="map">
        <div></div>
        <div>地图</div>
      </div>
      <div class="tag">
        <div></div>
        <div>站点</div>
      </div>

    </div> -->


  </div>
</template>

<script setup>
import OlMap from "@/components/OlMap/index.vue";

import Header from "@/layouts/components/Header.vue";
import MapToolsPanel from "@/components/MapToolsPanel/index.vue";
import RightAssistPanel from "@/components/RightAssistPanel/index.vue";
// import Timeline from "@/components/Timeline/index.vue";
import Timeline from "@/components/Timeline/Timeline.vue";
import dayjs from "dayjs";



const startTime = ref(dayjs('2025-03-13 08:00', 'YYYY-MM-DD HH:mm'));
const endTime = ref(dayjs('2025-03-23 07:00', 'YYYY-MM-DD HH:mm'));
const currentPoint = ref(dayjs('2025-03-13 08:00', 'YYYY-MM-DD HH:mm'));


const mapStore = useMapStore();
const mapPanelOptions = computed(() => mapStore.mapPanelOptions);
</script>

<style scoped>
.layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.content {
  background-color: #fff;
  flex: 1;
  overflow: hidden;
  /* z-index: -6; */
}



.left-top {
  position: absolute;
  z-index: 2;
  top: 15px;
  left: 15px;
}

.right-top {
  position: absolute;
  z-index: 1;
  top: 65px;
  right: 15px;
  width: 212px;
  height: 48px;
  background: linear-gradient(225deg, rgba(27, 44, 124, 0.98) 0%, rgba(21, 31, 96, 0.98) 100%);
  border-radius: 8px 8px 8px 8px;
  border: 1px solid rgba(71, 80, 126, 0.9);
  opacity: 0.98;

  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-left: 24px;
  padding-right: 24px;

  /* .map {
    height: 24px;
    min-width: 48px;
    color: rgba(188, 200, 255, 1);

    display: flex;
    align-items: center;
    justify-content: flex-start;

    div:first-child {
      height: 24px;
      width: 24px;
      background: url('@/assets/icon/earth-icon.png') no-repeat center;
      background-size: contain;
      margin-right: 6px;
    }

    div:nth-child(2) {
      font-size: 16px;
    }
  } */

  .tag {
    height: 24px;
    min-width: 48px;
    color: rgba(188, 200, 255, 1);
    display: flex;
    align-items: center;
    justify-content: flex-start;

    div:first-child {
      height: 24px;
      width: 24px;
      background: url('@/assets/icon/site-normal.png') no-repeat center;
      background-size: contain;
      margin-right: 6px;
    }

    div:nth-child(2) {
      color: rgba(30, 169, 255, 1);
      font-size: 16px;
    }

  }

}

.right-bottom {
  position: absolute;
  z-index: 1;
  bottom: 76px;
  right: 15px;
  width: 264px;
  height: 56px;
  background: url('@/assets/img/rain-card.png') no-repeat center;
  background-size: contain;

  opacity: 0.98;

}
</style>
