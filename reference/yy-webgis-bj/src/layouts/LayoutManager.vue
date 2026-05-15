<template>
  <div class="layout-manager">
    <div v-if="slots.left" class="left-pane">
      <slot name="left" />
    </div>
    <div class="main-pane">
      <slot name="main" />
    </div>
    <div v-if="slots.right" class="right-pane">
      <slot name="right" />
    </div>
    <slot />
  </div>
</template>

<script setup lang="ts">
const slots = useSlots();
</script>

<style scoped>
.layout-manager {
  position: relative;
  width: 100%;
  height: 100%;

  display: flex;
}

.main-pane {

  position: relative;
  width: 100%;
}

.left-pane {
  position: relative;
  padding: 0.75rem;
  height: fit-content;
  box-sizing: border-box;
  z-index: 999;
}

.right-pane {
  position: relative;
  height: fit-content;
  box-sizing: border-box;
  z-index: 999;
  right: 0;
}
</style>
