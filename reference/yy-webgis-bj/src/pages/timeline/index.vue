<template>
  <div class="h-full"></div>
</template>

<script setup lang="ts">
const timelineStore = useTimelineStore()

function initTime(){
  timelineStore.config.min = new Date('2025-01-01').getTime()
  timelineStore.config.max = new Date('2025-01-02').getTime()
  timelineStore.setCurrentTime(new Date('2025-01-01').getTime())
  timelineStore.config.step = 1000 * 60 * 60
}

onMounted(()=>{
  initTime()
})

onActivated(()=>{
  initTime()
})

onUnmounted(()=>{
  timelineStore.initialize()
})
</script>
