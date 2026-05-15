<template>
  <div class="station-container pl-9 pr-9" v-show="visible">
    <div class="title flex align-center justify-between">{{ position }}气象要素预报
      <img src="@/assets/icon/close.png" alt="" @click="close" style="cursor: pointer;">
    </div>
    <div class="date-change-container pt-5">
      <div class="arrow left-arrow" @click="scrollLeft">
        <!-- <img src="@/assets/icon/left-arrow.png" alt="左箭头"> -->
      </div>
      <ul class="date-list" ref="ulElement" @click="handleDateChange">
        <li v-for="(item, index) in dateList" :key="index" :data-date="item" :class="{ active: item == date }">{{
          item
        }}</li>
      </ul>
      <div class="arrow right-arrow" @click="scrollRight">
        <!-- <img src="@/assets/icon/right-arrow.png" alt="右箭头"> -->
      </div>
    </div>

    <div class="flex" style="height: 36px;justify-content: space-between;align-items: center;margin-bottom: 8px;">
      <div style="font-size: 14px; color: var(--app-text-color-purple)">
        最高气温：{{ maxData[0] }}℃&nbsp;&nbsp;最低气温：{{ maxData[1] }}℃&nbsp;&nbsp;最大风速：{{ maxData[2] }}m/s&nbsp;&nbsp;近七天误差值：{{ maeValue }}
      </div>
      <div class="operation-res" v-show="tableData.length > 0">
        <t-button theme="primary" class="search" @click="download">
          <!-- <add-icon slot="icon" /> -->
          <img src="@/assets/icon/search.png" alt="">
          下载数据
        </t-button>
      </div>
    </div>

    <t-table v-if="tableData.length > 0" rowKey="index" :data="tableData" :columns="columns" :bordered="bordered"
      :max-height="height" :headerAffixedTop="true" :scroll="{ type: 'virtual' }" lazy-load
      cellEmptyContent="-"></t-table>

    <div v-else>
      <div style="border-bottom: 1px solid #213382;"></div>

      <div class="default-content">
        <img src="@/assets/img/default.png" alt="">
        <div>暂无任何内容</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="jsx">
import { ref, onBeforeUnmount, watch } from 'vue'
import { DisplayService, EstimateService } from '@/api'
import dayjs from 'dayjs'
import { useTimelineStore } from '@/stores'
const timelineStore = useTimelineStore()

const curDate = computed(() => timelineStore.startDate)
const curTime = computed(() => timelineStore.startTime)

let visible = ref(false)
const close = () => {
  visible.value = false
}

let height = ref(435 / window.devicePixelRatio)
window.onresize = () => {
  return (() => {
    height.value = 435 / window.devicePixelRatio
  })()
}

let position = ref('')
const tableData = ref([]);
const tableAllData = ref(null)
const maxData = ref([])
const maxAllData = ref(null)
const maeValue = ref("")
const size = ref('medium');
const tableLayout = ref(false);
const stripe = ref(true);
const bordered = ref(true);
const hover = ref(false);
const showHeader = ref(true);
const columns = ref();
let dateList = ref([])
let date = ref('0')
let maxTemp = ref('')
let minTemp = ref('')
let maxWindSpeed = ref('')

const ulElement = ref(null);
// 向左滚动
const scrollLeft = () => {
  if (ulElement.value) {
    ulElement.value.scrollLeft -= 100;  // 每次向左滚动100px
  }
};

// 向右滚动
const scrollRight = () => {
  if (ulElement.value) {
    ulElement.value.scrollLeft += 100;  // 每次向右滚动100px
  }
};

// 生成默认日期列表
const defaultDateList = () => {
  dateList.value = []
  for (let i = 0; i < 10; i++) {
    // debugger
    // if (i === 0) {
    //   dateList.value.push(dayjs().format('YYYY-MM-DD'))
    //   continue
    // }

    dateList.value.push(dayjs(curDate.value).add(i, 'day').format('YYYY-MM-DD'))
  }
}

// 切换日期
const handleDateChange = (event) => {
  const target = event.target;
  // 确保点击的是 li 标签
  if (target.tagName.toLowerCase() === 'li') {
    date.value = target.dataset.date
    updateTableData()
  }
};

// 获取表格列
const getTableColumn = async () => {
  try {
    const res = await DisplayService.getWeatherFeatures({
      mk: 'fst'
    })
    columns.value = []
    res.split(',').forEach(item => {
      let col = {
        colKey: item,
        title: item,
        width: '80px'
      }
      if (item === '日期') {
        col.width = '60px'
      }
      if (item === '气象要素') {
        col['className'] = 'first-col'
      }
      columns.value.push(col)
    })
  } catch (error) {

  }
}

// 更新表格数据
const updateTableData = () => {
  if (tableAllData.value) {
    tableData.value = tableAllData.value[date.value].map(row => {
      const values = row.split(",");
      return columns.value.reduce((acc, col, index) => {
        acc[col.colKey] = values[index];
        return acc;
      }, {});
    });
    maxData.value = maxAllData.value[date.value]
  }
}

let currentId = ref('')
// 获取所有表格数据
const getTableDataAll = async (id) => {
  if (!visible.value) return
  currentId.value = id
  tableAllData.value = null
  tableData.value = []
  maxAllData.value = null
  maeValue.value = ""
  maxData.value = []

  let params = {
    station: id,
    // dataTime: dayjs(curDate.value).format('YYYY-MM-DD 00:00:00'),
    dataTime: dayjs(curDate.value).hour(curTime.value).format('YYYY-MM-DD HH:00:00'),
  }

  try {
    const res = await DisplayService.getStationPridctionData(params)
    // 无数据
    if (Object.keys(res.hour).length === 0) {
      defaultDateList()
      date.value = dateList.value[0]
    } else {
      tableAllData.value = res.hour
      maxAllData.value = res.day 
      maeValue.value = res.mae.mae[0] ?? "-1"
      if(parseFloat(maeValue.value)<0){
        maeValue.value = "-"
      }
      dateList.value = Object.keys(res.hour).sort() // 获取日期列表
      date.value = dateList.value[0]
      updateTableData()
    }
    date.value = dateList.value[0]
  } catch (error) {

  }
}

// 下载所有表格数据
const download = async () => {
  const exportData = {}
  exportData['columns'] = columns.value.map(item => item.title)
  for (let key in tableAllData.value) {
    // const allVal = []
    // tableAllData.value[key].forEach(v => {
    //   allVal.push(v.split(',').join(','))
    // })
    // exportData[key] = allVal
    exportData[key] = tableAllData.value[key]
  }

  const res = await EstimateService.downloadCSV({
    contentsMap: exportData
    // contentsMap: JSON.stringify(exportData)
  })
  if (res) {
    const blob = new Blob([res], { type: "text/csv;charset=utf-8;" });

    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.setAttribute("download", `${position.value} ${dateList.value[0]}至${dateList.value[dateList.value.length - 1]}气象要素预报`);
    document.body.appendChild(link);
    link.click();
    // 释放资源
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  }

}

$bus.on('openTable', f => {
  position.value = f[0].get('stationName') + f[0].get('stationIdD')
  if (!visible.value) {
    visible.value = true
    getTableColumn()
  }
  getTableDataAll(f[0].get('stationIdD'))
})

$bus.on('searchTable', obj => {
  position.value = obj.stationName + obj.stationIdD
  if (!visible.value) {
    visible.value = true
    getTableColumn()
  }
  getTableDataAll(obj.stationIdD)
})

watch(curDate, () => {
  getTableDataAll(currentId.value)
})

watch(curTime, () => {
  getTableDataAll(currentId.value)
})

onBeforeUnmount(() => {
  tableAllData.value = null
  tableData.value = null
  maxAllData.value = null
  maeValue.value = ""
  maxData.value = []
  columns.value = null
  $bus.off('openTable')
  $bus.off('searchTable')
})
</script>

<style lang="scss" scoped>
.station-container {
  width: 40vw;
  height: 68vh;
  z-index: 3;
  position: absolute;
  top: 7rem;
  left: 1.5rem;
  border-radius: 0.75rem 0.75rem 0.75rem 0.75rem;
  border: 1px solid rgba(71, 80, 126, 0.9);
  background: linear-gradient(225deg, rgba(27, 44, 124, 0.98) 0%, rgba(21, 31, 96, 0.98) 100%);
}

.title {
  height: 5rem;
  font-size: 1.5rem;
  border-bottom: 1px solid #2a3674;
}

.date-change-container {
  display: flex;
  align-items: center;
  overflow: hidden;
}

.arrow {
  width: 24px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: transparent;
}

/* 预加载 hover 图片 */
.left-arrow::before,
.right-arrow::before {
  content: "";
  position: absolute;
  width: 0;
  height: 0;
  background: url(../../../assets/icon/left-arrow-active.png) no-repeat,
    url(../../../assets/icon/right-arrow-active.png) no-repeat;
  visibility: hidden;
}

.left-arrow {
  background-image: url(../../../assets/icon/left-arrow.png);
}

.left-arrow:hover {
  background-image: url(../../../assets/icon/left-arrow-active.png);
}

.right-arrow {
  background-image: url(../../../assets/icon/right-arrow.png);
}

.right-arrow:hover {
  background-image: url(../../../assets/icon/right-arrow-active.png);
}

.arrow img {
  width: 24px;
  height: 40px;
}

.date-list {
  display: flex;
  height: 60px;
  list-style: none;
  overflow-x: auto;
  flex: 1;
  padding: 0.9375rem 0;
  margin: 0 10px;
  white-space: nowrap;
  scroll-behavior: smooth;
}

.date-list li {
  height: 3.75rem;
  line-height: 3.75rem;
  padding: 0 1.125rem;
  margin-right: 0.75rem;
  font-size: 1.3125rem;
  color: rgba(188, 200, 255, 1);
  background: #2D4098;
  ;
  cursor: pointer;
  border-radius: 0.375rem;
  transition: background 0.3s;
}

.date-list li.active {
  color: #fff;
  background: linear-gradient(188deg, #21CFFF 0%, #3856E8 100%);
}

.date-list::-webkit-scrollbar {
  display: none;
  /* 隐藏滚动条 */
}

:deep(.t-table) {
  background: #021043;

  // .t-table__header--fixed:not(.t-table__header--multiple)>tr>th {
  //   background-color: var(--app-border-color-dark);
  // }

  .t-table__content::-webkit-scrollbar {
    background: #021043;
    width: 0.375rem;
    // padding: 0 2px;
  }

  .t-table__content::-webkit-scrollbar-thumb {
    width: 0.375rem;
    height: 5.25rem;
    background: #5266CB;
  }

  .first-col {
    background-color: var(--app-border-color-dark);
  }

  .t-table__body {
    td:nth-child(n+2) {
      border-left: 1px solid var(--app-table-content-bg) !important;
    }
  }

  th,
  td {
    padding: 12px 4px;
  }

  // .t-table__scroll-bar-divider {
  //   border: none;
  // }


}

// :deep(.t-table--bordered th) {
//   border-left: 1px solid var(--td-border-color) !important;
//   color: #fff;
//   border-bottom: 1px solid var(--td-border-color) !important;
// }

// :deep(.t-table--bordered th:first-child) {
//   border-left-width: 0 !important;
// }

.default-content {
  height: 60%;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;

  div {
    font-size: 1.5rem;
  }

  img {
    width: 17.625rem;
    height: 12.75rem;
  }
}

.operation-res {
  :deep(.t-button) {
    width: 11.0625rem;
    height: 3.375rem;
    background: linear-gradient(188deg, #21CFFF 0%, #3856E8 100%);
  }

  :deep(.t-button__text) {
    display: flex;
    align-items: center;

    >img {
      width: 1.5rem;
      height: 1.5rem;
      margin-right: 0.375rem;
    }
  }
}
</style>