<template>
  <div class="h-full relative">
    <div style="position: absolute;bottom: 0; width: 100%;">
      <!-- <t-date-picker v-model:value="selectedDateTime" enableTimePicker format="YYYY-MM-DD HH:mm"
              style="width: 300px;" :disabled-time="disableSpecificTimes" /> -->
    </div>

    <SearchStation @searchChange="searchChange"></SearchStation>

    <RightTop :startTime="startTime" :currentPoint="currentPoint" :currentIndex="currentIndex"></RightTop>

    <StationWeather></StationWeather>

    <div class="right-bottom">
      <!-- <div class="rain"></div> -->
      <img src="/image/legend.png" alt="">
    </div>

    <Timeline :startTime="startTime" :endTime="endTime" v-model:currentPoint="currentPoint"
      v-model:currentIndex="currentIndex" @startDateChange="startDateChange" @startTimeChange="startTimeChange" />
  </div>

</template>


<script setup lang="ts">
import LayoutManager from "@/layouts/LayoutManager.vue"
// import Timeline from "@/components/Timeline/Timeline.vue"
import Timeline from "@/components/Timeline/TimelineForcast.vue"
import StationWeather from './components/StationWeather.vue'
import RightTop from './components/RightTop.vue'
import SearchStation from '@/components/SearchStation.vue'
import dayjs from "dayjs";
import { useTimelineStore } from '@/stores'

// const startTime = ref(dayjs().startOf('day'));
// const endTime = ref(dayjs().add(10, 'day').startOf('day'));

const timelineStore = useTimelineStore()

const now = dayjs()
const target1 = dayjs().hour(8).minute(0).second(0)
const target2 = dayjs().hour(15).minute(20).second(0)
let defaulDate = dayjs().format('YYYY-MM-DD')
let defaultTime = '8'
if (now.isBefore(target1)) {
  defaulDate = dayjs().subtract(1, 'day').format('YYYY-MM-DD')
  defaultTime = '20'
} else if (now.isAfter(target2)) {
  defaultTime = '20'
}
timelineStore.setStartDate(defaulDate)
timelineStore.setStartTime(defaultTime)

const startTime = ref(dayjs(defaulDate).hour(parseInt(defaultTime)).minute(0).second(0).millisecond(0));

const endTime = computed(() => startTime.value.add(10, 'day'))
const currentPoint = ref(dayjs(defaulDate).hour(parseInt(defaultTime)).minute(0).second(0).millisecond(0));
let currentIndex = ref(0)

watch(() => currentPoint.value, (val) => {
  // console.log(val);

})

const startDateChange = (val: any) => {
  let hour = startTime.value.hour()
  startTime.value = dayjs(val).hour(hour).minute(0).second(0).millisecond(0)
  currentPoint.value = dayjs(val).hour(hour).minute(0).second(0).millisecond(0)

  timelineStore.setStartDate(val)
}
const startTimeChange = (val: any) => {
  startTime.value = startTime.value.hour(val)
  currentPoint.value = startTime.value.hour(val)
  timelineStore.setStartTime(val)
}


const selectedDateTime = ref(new Date());

// const disableSpecificTimes = (time) => {
//   const hours = time.getHours();
//   const minutes = time.getMinutes();
//   // 仅允许 8:00 AM 和 8:00 PM
//   return !(hours === 8 && minutes === 0) && !(hours === 20 && minutes === 0);
// };

const searchChange = (n: any) => {
  // window.$bus.emit('updateStaionLayer', n.stationIdD)
  window.$bus.emit('updateCustomStaionLayer', n.stationIdD)
  window.$bus.emit('searchTable', n)
}

</script>
<style scoped lang="less">
.right-bottom {
  position: absolute;
  z-index: 3;
  bottom: 10px;
  right: 1.5rem;
  // opacity: 0.98;
}

.rain {
  width: 100px;
  height: 226px;
  background: url('/image/legend.png') no-repeat center;
  background-size: contain;
  margin-bottom: 4px;
}
</style>