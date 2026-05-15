<template>
  <div class="h-full">
    <LayoutManager>
      <template #main>
        <Panel />
      </template>
    </LayoutManager>
  </div>
</template>

<script setup lang="ts">
import LayoutManager from "@/layouts/LayoutManager.vue";
import Panel from "./rightTopPanel.vue";
</script>
