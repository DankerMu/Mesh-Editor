<template>
  <div class="h-full">
    <LayoutManager>
      <!-- <template #left>
        <leftPanel />
      </template> -->
    </LayoutManager>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: "CustomLeft"
})

import LayoutManager from "@/layouts/LayoutManager.vue";
import leftPanel from "./leftPanel.vue";
</script>
