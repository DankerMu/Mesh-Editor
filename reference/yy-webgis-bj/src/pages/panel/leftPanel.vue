<template>
  <div class="left-panel" :class="{ collapsed: !mapPanelOptions.leftPanel }">
    <div class="panel-content" :class="{ '!hidden': !mapPanelOptions.leftPanel }">
      <p>currentTime: {{ dayjs(timelineStore.current).format("YYYY-MM-DD HH:mm") }}</p>
      <p>startTime: {{ dayjs(timelineStore.config.min).format("YYYY-MM-DD HH:mm") }}</p>
      <p>endTime: {{ dayjs(timelineStore.config.max).format("YYYY-MM-DD HH:mm") }}</p>

      <p>will be keepAlive:<t-input class="w-100" v-model="text"></t-input></p>
    </div>
    <div class="toggle-icon cursor-pointer" @click="mapStore.togglePanel('leftPanel')">
      <IndentLeftIcon v-if="mapPanelOptions.leftPanel" />
      <IndentRightIcon v-else />
    </div>
  </div>
</template>

<script setup lang="ts">
import { IndentLeftIcon, IndentRightIcon } from "tdesign-icons-vue-next";
import dayjs from "dayjs";

const mapStore = useMapStore();
const mapPanelOptions = computed(() => mapStore.mapPanelOptions);

const timelineStore = useTimelineStore();
const text = ref("default content");

function initTimeline() {
  timelineStore.config.max = new Date("2024-08-13 14:00").getTime();
  timelineStore.config.min = new Date("2024-08-12 08:00").getTime();
  timelineStore.config.step = 1000 * 60 * 60 * 6;

  timelineStore.setCurrentTime(new Date("2024-08-12 08:00").getTime());
}

onMountedOrActivated(() => {
  console.log("onMountedOrActivated run");
  initTimeline();
});

onUnmounted(() => {
  timelineStore.initialize();
});
</script>

<style scoped lang="less">
.left-panel {
  width: 24vw;
  height: calc(100vh - 4.375rem - 1.5rem);
  background-color: #ffffffcc;
  position: relative;

  &.collapsed {
    width: 0;
  }
}

.panel-content {
  width: 24vw;
  height: 100%;
  padding: 0.75rem;
  font-size: 1.3rem;

  p {
    line-height: 2rem;
  }
}

.toggle-icon {
  position: absolute;
  right: -1.6rem;
  top: 22rem;
  background-color: #ffffffcc;
  padding: 4px;
  border-radius: 0 4px 4px 0;
  box-shadow: 2px 0 5px rgba(0, 0, 0, 0.1);
  z-index: 10;
  display: flex;
  padding: 0.7rem 0.3rem;
}
</style>
