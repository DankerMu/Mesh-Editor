<template>
  <div class="h-full">
    <LayoutManager>
      <template #main>
        <div class="content">
          tab1
        </div>
      </template>
    </LayoutManager>
  </div>
</template>

<script setup lang="ts">
import LayoutManager from "@/layouts/LayoutManager.vue"
</script>

<style lang="less" scoped>
.content {
  height: 100%;
  background-color: #fff;
  position: relative;

}
</style>
