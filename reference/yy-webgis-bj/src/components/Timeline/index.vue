<template>
  <div
    class="z-999 absolute bottom-[0.75rem] m-x-[0.75rem] h-15 bg-[#ffffffCC]"
    :class="[
      mapPanelOptions.leftPanel ? 'left-[calc(24vw+0.75rem)]' : 'left-0',
      mapPanelOptions.rightPanel ? 'right-[calc(24vw+0.75rem+3.4rem)]' : 'right-[3.4rem]',
    ]"
  >
  <div class="timeline">
    <div class="controls">
      <PageFirstIcon @click="timelineStore.previous" />
      <PlayCircleStrokeIcon @click="timelineStore.play" v-if="!timelineStore.isPlaying" />
      <PauseCircleStrokeIcon @click="timelineStore.stop" v-else />
      <PageLastIcon @click="timelineStore.next" />
    </div>

    <PermanentTooltipSlider
      v-model="timelineStore.current"
      :min="timelineStore.config.min"
      :max="timelineStore.config.max"
      :step="timelineStore.config.step"
      alwaysShowLabel
      :format="formatDate"
    />
  </div>
</div>
</template>

<script setup lang="ts">
import PermanentTooltipSlider from './PermanentTooltipSlider.vue'
import {PageFirstIcon, PlayCircleStrokeIcon, PauseCircleStrokeIcon, PageLastIcon} from 'tdesign-icons-vue-next'
import dayjs from 'dayjs';


const mapStore = useMapStore();
const mapPanelOptions = computed(() => mapStore.mapPanelOptions);

const timelineStore = useTimelineStore()

const formatDate = (value: number) => {
  return dayjs(value).format('YYYY-MM-DD HH:mm');
};

onMounted(() => {
  console.log("timeline is mounted");
});
</script>

<style lang="less" scoped>
.timeline {
  display: flex;
  align-items: center;
  height: 100%;
  padding: 0.75rem 3rem 0.75rem 0.75rem;
  box-sizing: border-box;

  .controls {
    margin-right: 1rem;
    width: 8rem;

    .t-icon {
      font-size: 2rem;
      cursor: pointer;
    }
  }
}
</style>
