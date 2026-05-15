<template>
  <TRow class="w-full list mt-6">
    <TCol :span="6">
      <span style="font-size: 16px;">站点</span>
      <div class="position-div">{{ position }}</div>
    </TCol>
    <TCol :span="6">
      <span style="font-size: 16px;">起报时间</span>
      <!-- <t-select v-model="stationParams.time" :options="[]" placeholder="" /> -->
      <!-- 日期选择器 -->
      <DatePicker :popupProps="{ overlayClassName: 'yy-date-picker-popup' }" size='large'
        v-model:value="stationParams.qbtime" type="date" format="YYYY-MM-DD" class="date-sel" clearable>
        <template #valueDisplay="{ displayValue }">
          <span style="font-weight: bold;font-size: 13px;color:aliceblue"></span>{{ displayValue }}
        </template>
      </DatePicker>

      <!-- 时间选择器 -->
      <Select :popupProps="{ overlayClassName: 'gradient-select-popup' }" size='large'
        v-model:value="stationParams.time" @click.stop placeholder="" :disabled="!stationParams.qbtime"
        style="width: 30%;" class="time-sel">
        <Option value="8" label="08:00">08:00</Option>
        <Option value="20" label="20:00">20:00</Option>
      </Select>
    </TCol>
  </TRow>
  <CheckboxList title="模式产品：" v-model:modelValue="currentSource" :options="sourceList" style="margin-top: 4px;">
  </CheckboxList>
  <!-- <div style="height: 3.1875rem;">气象要素</div> -->
  <div class="flex align-center" style="margin-bottom: 16px;margin-top: 4px;">
    气象要素：
    <ul class="date-list" @click="handleFeatureChange">
      <li v-for="(item, index) in weatherFeatureListForStation" :key="item.value" :class="{ active: item.checked }"
        :data-value="item.value">{{ item.label }}</li>
    </ul>
  </div>
  <div class="operation-list">
    <t-button theme="primary" class="search mr-5" @click="search">
      <img src="@/assets/icon/search.png" alt="">
      查询
    </t-button>
    <!-- <t-button type="reset" class="reset" @click="download">
      <img src="@/assets/icon/download.png" alt="">
      下载
    </t-button> -->
  </div>
  <div style="border-bottom: 1px solid #213382;margin-top: 16px;"></div>
  <div v-show="dataStationAll.length > 0">
    <Echarts ref="sChartRef" v-show="dataStationAll.length > 0"></Echarts>
  </div>
  <div v-show="dataStationAll.length == 0">
    <div class="default-content">
      <img src="@/assets/img/default.png" alt="">
      <div>暂无任何内容</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onBeforeUnmount, nextTick } from 'vue'
import Echarts from './EchartsforStation.vue'
import CheckboxList from '@/components/CheckboxList/index.vue'
import { CompareService } from '@/api'
import { DatePicker, Select, Option } from 'tdesign-vue-next';
import dayjs from 'dayjs';
import { MessagePlugin } from 'tdesign-vue-next'
import { colors } from '@/utils/echartsConfigColor.ts'

const props = defineProps({
  position: {
    type: String,
    default: ''
  },
  positionId: {
    type: Number,
    default: 0
  },
  visible: {
    type: Boolean,
    default: false
  }
});

// 站点查询参数
const now = dayjs()
const target1 = dayjs().hour(8).minute(0).second(0)
const target2 = dayjs().hour(15).minute(20).second(0)
let defaulDate = dayjs().format('YYYY-MM-DD')
let defaultTime = '8'
if (now.isBefore(target1)) {
  defaulDate = dayjs().subtract(1, 'day').format('YYYY-MM-DD')
  defaultTime = '20'
} else if (now.isAfter(target2)) {
  defaultTime = '20'
}
let stationParams = ref({
  qbtime: defaulDate,
  time: defaultTime,
})
let dataStationAll = ref([])
let dataStation = ref([])
let sChartRef = ref()

let currentWeather = ref(null)
let weatherFeatureListForStation = ref([])

let currentSource = ref([])
let sourceList = ref([])

const getDataSourceByDataType = async () => {
  try {
    const res = await CompareService.getDataSourceByDataType({
      dataType: 'cmpstation'
    })
    sourceList.value = res.map(item => {
      return {
        value: item.dataSource,
        label: item.dataSourceName
      }
    })
  } catch (error) {
    sourceList.value = []
  }
}
getDataSourceByDataType()

// 获取气象要素
const getWeatherFeature = async () => {
  weatherFeatureListForStation.value = []

  try {
    const res = await CompareService.getWeatherFeature({
      mk: 'cmp'
    })
    if (res && res.length > 0) {
      weatherFeatureListForStation.value = res.map(item => {
        return {
          value: item.element,
          label: item.elementName,
          unit: item.elementUnit,
          checked: false
        }
      })
      weatherFeatureListForStation.value[0].checked = true
      currentWeather.value = weatherFeatureListForStation.value[0]
    }
  } catch (error) { }
}

// 切换气象要素
const handleFeatureChange = (event) => {
  weatherFeatureListForStation.value.map(item => item.checked = false)
  const target = event.target;
  // 确保点击的是 li 标签
  if (target.tagName.toLowerCase() === 'li') {
    const val = target.dataset.value;  // 通过 dataset 获取索引
    const i = weatherFeatureListForStation.value.findIndex(item => item.value === val)
    weatherFeatureListForStation.value[i].checked = true
    currentWeather.value = weatherFeatureListForStation.value[i]
    // if (dataStationAll.value) {
    //   dataStation.value = dataStationAll.value[currentWeather.value.value]
    //   sChartRef.value?.initChart(dataStation.value, currentWeather.value, 'station')
    // }
  }
};

// 查询
const search = async () => {
  try {
    let dataTime = dayjs(stationParams.value.qbtime).hour(stationParams.value.time).format('YYYY-MM-DD HH:00:00')
    // if (currentSource.value.length <= 0) {
    //   MessagePlugin.warning('请选择模式')
    //   return
    // }
    let params = {
      station: props.positionId,
      dataTime: dataTime,
      dataSource: currentSource.value,
    }
    const res = await CompareService.getStationComparisonData(params)
    if (res && Object.keys(res).length > 0) {
      // dataStationAll.value = res
      // dataStation.value = res[currentWeather.value.value]
      // nextTick(() => {
      //   sChartRef.value?.initChart(dataStation.value, currentWeather.value, 'station')
      // })
      dataStationAll.value = []

      if (res['obs']) {
        dataStationAll.value.push({
          label: '实况',
          data: res['obs'],
          // color: '#1f77b4'
          color: colors['obs']
        })
      }
      if (res['fst']) {
        dataStationAll.value.push({
          label: '订正',
          data: res['fst'],
          // color: '#ff7f0e'
          color: colors['fst']
        })
      }

      for (let key in res) {
        if (key === 'obs') {
          // dataStationAll.value.push({
          //   label: '实况',
          //   data: res['obs'],
          //   color: '#1f77b4'
          // })
          continue
        } else if (key === 'fst') {
          // dataStationAll.value.push({
          //   label: '订正',
          //   data: res['fst'],
          //   color: '#ff7f0e'
          // })
          continue
        } else {
          const name = sourceList.value.find(x => x.value === key)?.label
          // const color = sourceList.value.find(x => x.value === key)?.color
          dataStationAll.value.push({
            label: name,
            // color: color,
            color: colors[key],
            data: res[key]
          })
        }
      }
      nextTick(() => {
        sChartRef.value?.initChart(dataStationAll.value, currentWeather.value, 'station')
      })
    } else {
      dataStationAll.value = []
      // dataStation.value = []
    }
  } catch (error) {
    dataStationAll.value = []
    // dataStation.value = []
  }
}

// 下载
const download = async () => {
  try {
    let dataTime = dayjs(stationParams.value.qbtime).hour(stationParams.value.time).format('YYYY-MM-DD HH:00:00')
    let params = {
      station: "55026",
      dataTime: dataTime,
    }
    const res = await CompareService.downloadStationData(params)

    const blob = new Blob([res], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.setAttribute("download", "data.csv");
    document.body.appendChild(link);
    link.click();
    // 释放资源
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  } catch (error) {
    MessagePlugin.error('下载失败')
  }
}

getWeatherFeature()

const clearChartData = () => {
  dataStationAll.value = []
}

defineExpose({
  search,
  clearChartData
})

onBeforeUnmount(() => {
  dataStationAll.value = []
  dataStation.value = []
})
</script>

<style lang="less" scoped>
.date-list {
  display: flex;
  list-style: none;
  overflow-x: auto;
  flex: 1;
  white-space: nowrap;
  scroll-behavior: smooth;

  &::-webkit-scrollbar {
    background: #021043;
    width: 0.375rem;
  }

  &::-webkit-scrollbar-thumb {
    width: 0.375rem;
    height: 5.25rem;
    background: #5266CB;
  }
}

.date-list li {
  // width: 7rem;
  height: 3rem;
  line-height: 3rem;
  text-align: center;
  margin-right: 0.75rem;
  padding: 0 10px;
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
</style>