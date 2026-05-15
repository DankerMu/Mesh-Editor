<template>
  <ul>
    <li
      v-for="(item, index) in projectionStore.projectionDefs"
      :data-name="item.title"
      :key="`base-layer-${index}`"
      @click="() => setProj(item.key)"
      :class="{ active: projectionStore.currentProjection === item.key }"
    >
      {{ item.title }}
    </li>
  </ul>
</template>

<script setup lang="ts">
const projectionStore = useProjectionStore()

const $emit = defineEmits(['close'])

function setProj(key: string) {
  projectionStore.setProjection(key)

  $emit('close')
}

onMounted(() => {
  projectionStore.initProjections()
});
</script>


<style lang="less" scoped>
ul {
  display: flex;
  justify-content: space-between;

  li {
    margin-right: 5px;
    font-size: 12px;
    cursor: pointer;
    position: relative;
  }

  li.active {
    color: var(--td-brand-color);
  }
}
</style>
