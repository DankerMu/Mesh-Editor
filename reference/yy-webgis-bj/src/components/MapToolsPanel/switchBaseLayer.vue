<template>
  <ul>
    <li
      v-for="(item, index) in baseLayerList"
      :data-name="item.title"
      :key="`base-layer-${index}`"
      @click="switchBaseLayer(item.key)"
    >
      <!-- <img :src="item.iconUrl" :alt="item.title" /> -->
      <span class="name">{{ item.title }}</span>
    </li>
  </ul>
</template>

<script setup lang="ts">
import { baseLayerList } from '@/components/OlMap/OlMap'
function switchBaseLayer(key: string) {
  olMap?.switchBaseLayer(key)
}
</script>


<style lang="less" scoped>
ul {
  display: flex;
  justify-content: space-between;

  li {
    margin-right: 5px;
    font-size: 12px;
    height: 60px;
    cursor: pointer;
    position: relative;

    img {
      width: 92px;
      height: 60px;
      object-fit: cover;
      position: relative;
    }

    &:hover {
      &::after {
        content: '';
        position: absolute;
        left: 0;
        top: 0;
        width: 100%;
        height: 100%;
        background-color: rgba(68, 65, 65, 0.6);
        z-index: 9;
      }
    }

    .name {
      position: absolute;
      right: 5px;
      bottom: 6px;
      z-index: 10;
      color: #00a0e9;
    }
  }

  li.active {
    background-color: rgb(68, 65, 65);
  }
}
</style>
