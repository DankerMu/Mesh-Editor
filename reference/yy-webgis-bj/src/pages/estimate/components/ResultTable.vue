<template>
  <t-table v-if="tableData.length>0" row-key="index" :data="tableData" :columns="tableColumns" bordered
    :max-height="height" headerAffixedTop :scroll="{ type: 'virtual' }" :row-class-name="rowClassName"
    lazy-load></t-table>
</template>

<script setup>
import { watch, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  data: {
    type: Object,
    default: null
  },
  // features: {
  //   type: Array,
  //   default: []
  // },
  columns: {
    type: Array,
    default: []
  }
})

let tableData = ref([])


const height = ref(380 / window.devicePixelRatio)
function updateHeight() {
  height.value = 380 / window.devicePixelRatio
}

const fixedHeader = ref(true);

const formatData = (data) => {
  tableData.value = data.map(item => {
    let formatted = {}
    Object.entries(item).map(([time, value]) => {
      if (!value && value !== 0) {
        formatted[time] = '-'
      } else {
        formatted[time] = value
      }
    })
    return formatted
  })
}

watch(() => props.data, (newval) => {
  // tableData.value = newval
  formatData(newval)
}, {
  immediate: true
})

// 生成 t-table 需要的 columns 结构
const generateColumns = (headers) => {
  return headers.map(header => ({
    title: header.label,
    children: header.children ? generateColumns(header.children) : undefined,
    colKey: header.colKey || undefined, // 叶子节点才有 colKey
    align: "center",
  }));
};

// 计算表头
const tableColumns = computed(() => generateColumns(props.columns));

const rowClassName = ({ row, rowIndex, type }) => {
  if (row.station === 'total' || row.station === '合计') return 'last-row'
};

onMounted(() => {
  window.addEventListener('resize', updateHeight)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateHeight)
})

</script>

<style lang="scss" scoped>
// :deep(th) {
//   background-color: rgba(20, 38, 108, 1) !important;
//   border-color: gray;
// }


// :deep(td) {
//   background-color: rgba(34, 58, 153, 1) !important;
//   border-color: gray;
// }

// 表体滚动轴隐藏
:deep(.t-table__content::-webkit-scrollbar) {
  width: 0;
}

// :deep(.t-table__scroll-bar-divider) {
//   border: none;
// }

::v-deep(.last-row) {
  background: var(--app-table-even-th) !important;
}
</style>