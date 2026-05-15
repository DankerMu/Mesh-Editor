<template>





  <div class="h-full">
    <LayoutManager>
      <div style="position: absolute;bottom: 0; width: 100%;">
        <!-- <t-date-picker v-model:value="selectedDateTime" enableTimePicker format="YYYY-MM-DD HH:mm"
              style="width: 300px;" :disabled-time="disableSpecificTimes" /> -->
        <Timeline :startTime="startTime" :endTime="endTime" v-model:currentPoint="currentPoint" />
      </div>
    </LayoutManager>
  </div>
</template>

<script setup lang="ts">
import LayoutManager from "@/layouts/LayoutManager.vue"
import Timeline from "@/components/Timeline/Timeline.vue"
import dayjs from "dayjs";

// const startTime = ref(dayjs().startOf('day'));
// const endTime = ref(dayjs().add(10, 'day').startOf('day'));


const startTime = ref(dayjs('2025-03-13 08:00', 'YYYY-MM-DD HH:mm'));
const endTime = ref(dayjs('2025-03-23 07:00', 'YYYY-MM-DD HH:mm'));
const currentPoint = ref(dayjs('2025-03-13 08:00', 'YYYY-MM-DD HH:mm'));




const selectedDateTime = ref(new Date());

// const disableSpecificTimes = (time) => {
//   const hours = time.getHours();
//   const minutes = time.getMinutes();
//   // 仅允许 8:00 AM 和 8:00 PM
//   return !(hours === 8 && minutes === 0) && !(hours === 20 && minutes === 0);
// };

</script>
