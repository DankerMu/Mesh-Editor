<template>
  <div class="layer-manager">
    <t-button @click="() => layerManagerStore.setVisible([], !allLayerVisible)">
      显/隐ALL
    </t-button>
    <t-button @click="() => layerManagerStore.removeAllLayers()"> 删除所有 </t-button>
    <div v-for="(item, key, index) in layerManagerStore.layerStates" :key="key">
      {{ key }}
      <t-checkbox
        v-model="item.visible"
        @change="(e) => layerManagerStore.setVisible(key, e)"
      />
      <t-button @click="() => layerManagerStore.removeLayerById(key)"> 删除 </t-button>

      <t-slider
        v-model="item.opacity"
        :show-tooltip="true"
        :max="1"
        :min="0"
        :step="0.01"
        @change="(e) => layerManagerStore.setOpacity(key, e)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
const layerManagerStore = useLayerManagerStore();

const allLayerVisible = computed(() => {
  return Object.values(layerManagerStore.layerStates).every((item) => item.visible);
});
</script>

<style lang="less" scoped></style>
