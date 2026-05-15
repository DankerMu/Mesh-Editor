<template>
  <div class="h-full">
    <LayoutManager>
      <template #main>
        <div class="content">
          <span>无权限</span>
        </div>
      </template>
    </LayoutManager>

  </div>
</template>

<script setup lang="ts">
import LayoutManager from "@/layouts/LayoutManager.vue"
</script>

<style lang="scss" scoped>
.content {
  position: relative;
  height: 100%;
  background-color: #fff;
  color: #000;
  z-index: 2;

  >span {
    font-size: 20px;
  }
}
</style>
