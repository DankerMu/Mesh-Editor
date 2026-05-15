<template>
  <div class="panel-content">
    <ul>
      <li :class="{ active: item.title === currentTab }" v-for="item in tabList" :key="item.title" class="item"
        @click="() => changeCurrentTab(item)">
        {{ item.title }}
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
interface TabItemType {
  title: string;
}

const tabList: TabItemType[] = [
  {
    title: "tab1",
  },
  {
    title: "tab2",
  },
];
const currentTab = ref("tab1");

function changeCurrentTab(item: TabItemType) {
  currentTab.value = item.title;
}
</script>

<style scoped lang="less">
.panel-content {
  padding: 0.75rem 0;
  border-radius: 0.175rem;
  z-index: 999;
  position: absolute;
  right: 0;
  transition: opacity 0.3s ease;

  .item {
    background-color: #ffffffcc;
    padding: 0.75rem 1.5rem;
    cursor: pointer;
    border-radius: 0.175rem;

    &:not(:last-child) {
      margin-bottom: 0.75rem;
    }

    &.active {
      background-color: #0000ffcc;
      color: white;
    }
  }
}
</style>
