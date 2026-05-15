<template>
  <div class="left-panel" :class="{ collapsed: !mapPanelOptions.leftPanel }">
    <div class="panel-content" :class="{ '!hidden': !mapPanelOptions.leftPanel }">
      <ul class="tabs">
        <li
          class="tab-item"
          :class="{ active: item.title === currentTab }"
          v-for="item in tabList"
          :key="item.title"
          @click="() => changeCurrentTab(item)"
        >
          {{ item.title }}
        </li>
      </ul>
      <div class="content">{{ currentTab }} will not keepAlive</div>
    </div>
    <div class="toggle-icon cursor-pointer" @click="mapStore.togglePanel('leftPanel')">
      <IndentLeftIcon v-if="mapPanelOptions.leftPanel" />
      <IndentRightIcon v-else />
    </div>
  </div>
</template>

<script setup lang="ts">
import { IndentLeftIcon, IndentRightIcon } from "tdesign-icons-vue-next";

const mapStore = useMapStore();
const mapPanelOptions = computed(() => mapStore.mapPanelOptions);

const currentTab = ref("tab1");

interface tabItemType {
  title: string;
}

const tabList: tabItemType[] = [
  {
    title: "tab1",
  },
  {
    title: "tab2",
  },
];

function changeCurrentTab(item: tabItemType) {
  currentTab.value = item.title;
}

onMounted(() => {
  console.log("panel/leftpanel is mounted");
});
</script>

<style scoped lang="less">
.left-panel {
  width: 24vw;
  height: calc(100vh - 4.375rem - 1.5rem);
  position: relative;

  &.collapsed {
    width: 0;
  }
}

.panel-content {
  height: 100%;

  display: flex;

  .tabs {
    .tab-item {
      width: 1rem;
      background-color: #ffffffcc;
      cursor: pointer;
      padding: 0.75rem;
      text-align: center;
      writing-mode: tb-rl;

      &:not(:last-child) {
        margin-bottom: 0.75rem;
      }

      &.active {
        background-color: #0000ffcc;
        color: white;
      }
    }
  }

  .content {
    background-color: #ffffffcc;
    margin-left: 0.75rem;
    flex: 1;
    padding: 0.75rem;
  }
}

.toggle-icon {
  position: absolute;
  right: -1.6rem;
  top: 22rem;
  background-color: #ffffffcc;
  padding: 4px;
  border-radius: 0 4px 4px 0;
  box-shadow: 2px 0 5px rgba(0, 0, 0, 0.1);
  z-index: 10;
  display: flex;
  padding: 0.7rem 0.3rem;
}
</style>
