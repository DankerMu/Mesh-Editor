<template>
  <div
    class="z-999 w-10 h-35 bg-[#ffffffCC] flex flex-col justify-around items-center absolute top-[5.15rem] rounded-1"
    :class="mapStore.mapPanelOptions.rightPanel ? 'opened' : 'closed'"
  ></div>
</template>

<script setup lang="ts">
const mapStore = useMapStore();
</script>

<style lang="less" scoped>
.opened {
  right: calc(24vw + 0.75rem);
}

.closed {
  right: 0.75rem;
}
</style>
