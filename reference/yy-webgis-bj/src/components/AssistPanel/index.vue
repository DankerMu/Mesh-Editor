<template>
  <div class="panel-container">
    <div class="switch-group">
      <div class="switch-item">
        <span class="switch-label"> Map Tools </span>
        <t-switch
          :defaultValue="mapPanelOptions.mapTools"
          @change="mapStore.togglePanel('mapTools')"
          size="medium"
        />
      </div>

      <div class="switch-item">
        <span class="switch-label"> Timeline </span>
        <t-switch
          :defaultValue="mapPanelOptions.timeline"
          @change="mapStore.togglePanel('timeline')"
          size="medium"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const mapStore = useMapStore();
const mapPanelOptions = computed(() => mapStore.mapPanelOptions);
</script>

<style scoped>
.panel-container {
  padding: 0.75rem;
}

.switch-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem;
  border-radius: 0.175rem;
  transition: background-color 0.2s;

  span {
    margin-right: 0.375rem;
  }
}
</style>
