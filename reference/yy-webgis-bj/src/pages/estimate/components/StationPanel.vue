<template>
  <div class="flex align-center mt-9" style="height: 60px;">
    <div>检验时段：</div>
    <div class="select-time-box" style="border-right: none;">
      <div class="blockbox bottom-border">
        <span class="labelText" :class="{ active: dateMode === '0' }" style="cursor: pointer;"
          @click="dateMode = '0'">常用</span>
      </div>
      <div class="blockbox">
        <span class="labelText" :class="{ active: dateMode === '1' }" style="cursor: pointer;"
          @click="dateMode = '1'">精确</span>
      </div>
    </div>

    <div v-show="dateMode === '1'" class="select-time-box" style="border-right: none;">
      <div class="blockbox bottom-border">
        <span class="labelText">起始时间：</span>
      </div>
      <div class="blockbox">
        <span class="labelText">结束时间：</span>
      </div>
    </div>

    <div v-show="dateMode === '1'" class="select-time-box mr-18">
      <div class="blockbox bottom-border">
        <t-date-picker v-model="startDate" format="YYYY-MM-DD" class="blockbox-datepicker" allow-input clearable
          style="margin-right: 10px;margin-left: 10px;" :popupProps="{ overlayClassName: 'yy-date-picker-popup' }">
          <template #suffixIcon>
            <img src="@/assets/icon/calendar.png" alt="">
          </template>
        </t-date-picker>
      </div>
      <div class="blockbox" style="border-right: none;">
        <t-date-picker v-model="endDate" format="YYYY-MM-DD" class="blockbox-datepicker" allow-input clearable
          style="margin-right: 10px;margin-left: 10px;" :popupProps="{ overlayClassName: 'yy-date-picker-popup' }">
          <template #suffixIcon>
            <img src="@/assets/icon/calendar.png" alt="">
          </template>
        </t-date-picker>
      </div>
    </div>

    <div v-show="dateMode === '0'" class="select-time-box" style="border-right: none;">
      <div class="blockbox bottom-border" @click="currentYear++" style="height: 16px;cursor: pointer;">
        <img src="@/assets/icon/arrow-up-bold.png" alt="">
      </div>
      <div class="blockbox bottom-border">
        <span style="font-size: 12px;padding: 0 4px;">{{ currentYear }}</span>
      </div>
      <div class="blockbox" @click="currentYear--" style="height: 16px;cursor: pointer;">
        <img src="@/assets/icon/arrow-down-bold.png">
      </div>
    </div>
    <div v-show="dateMode === '0'" class="select-time-box" style="border-right: none;">
      <div class="blockbox bottom-border">
        <span v-for="item in monthList" class="labelText right-border" :class="{ active: generalDateMode === item }"
          style="width: 25px;cursor: pointer;" @click="changeDateMode(item)">{{ item }}</span>
      </div>
      <div class="blockbox">
        <span v-for="item in seasonLsit" class="labelText right-border" :class="{ active: generalDateMode === item }"
          style="width: 75px;cursor: pointer;" @click="changeDateMode(item)">{{ item }}</span>
      </div>
    </div>
    <div v-show="dateMode === '0'" class="select-time-box mr-18">
      <div class="blockbox bottom-border">
        <span class="labelText" :class="{ active: generalDateMode === '近一月' }" style="cursor: pointer;"
          @click="changeDateMode('近一月')">近一月</span>
      </div>
      <div class="blockbox">
        <span class="labelText" :class="{ active: generalDateMode === '近一周' }" style="cursor: pointer;"
          @click="changeDateMode('近一周')">近一周</span>
      </div>
    </div>

    <!-- 时间选择器 -->
    <div>时次：</div>
    <t-select :popupProps="{ overlayClassName: 'pure-select-popup' }" v-model:value="timeValue" placeholder=""
      class="time-sel mr-18" style="width: 100px;">
      <t-option value="2008" label="全部">全部</t-option>
      <t-option value="8" label="08:00">08:00</t-option>
      <t-option value="20" label="20:00">20:00</t-option>
    </t-select>

    <RadioList ref="currentTimeValidRef" title="时效：" v-model:modelValue="currentTimeValid"
      :defaulValue="currentTimeValid" :options="timeValidList">
    </RadioList>
    <div v-show="currentTimeValid === 'day'">
      <div class="date-change-container">
        <ul class="date-list" ref="ulElement" @click="handleDateChange">
          <li v-for="(item, index) in dayList" :key="index" :data-date="item.value"
            :class="{ active: item.value == currentDay }">{{
              item.label
            }}</li>
        </ul>
      </div>
    </div>
  </div>
  <div class="flex align-center mt-5" style="height: 38px;">
    <RadioList ref="weatherFeatureRef" title="检验要素：" v-model:modelValue="weatherFeature" :options="weatherFeatureList"
      :defaulValue="weatherFeature" class="mr-18">
    </RadioList>
    <RadioList ref="methodRef" title="检验方法：" v-model:modelValue="method" :options="checkMethod" :defaulValue="method">
    </RadioList>
  </div>
  <div class="flex align-center mt-5" style="height: 38px;">
    <div>站点/区域：</div>
    <!-- <t-cascader v-model="selectedStation" :options="stationList" :load="handleLoad" multiple clearable class="mr-18"
      :show-all-levels="false" :min-collapsed-num="1" @change="handleSelectChange" filterable
      :popupProps="{ overlayClassName: 't-cascader-option-list' }">
      <template #collapsedItems="{ collapsedSelectedItems, count }">
        <t-popup>
          <template #content>
            <p v-for="(item, index) in collapsedSelectedItems" :key="index" style="padding: 10px">
              {{ item.toString() }}
            </p>
          </template>
          <span v-show="count > 0" style="color: #00a870; margin-left: 10px">+{{ count }}</span>
        </t-popup>
      </template>
    </t-cascader> -->
    <div class="mr-18"
      style="width: 150px;height: 38px;line-height: 38px;border: 1px solid #2D4098;padding: 0 10px;white-space: nowrap;overflow: hidden;text-overflow: ellipsis;cursor: pointer;"
      @click="openDialog">
      {{ selectedStation ? selectedStation.label : selectedArea?.label }}</div>
    <!-- <div class="operation-list">
      <t-button theme="primary" class="operation-list search mr-18" @click="openDialog">
        <img src="@/assets/icon/search.png" alt="">
        打开
      </t-button>
    </div> -->
    <CheckboxList ref="currentSourceRef" title="模式产品：" v-model:modelValue="currentSource" :options="sourceList">
    </CheckboxList>
  </div>
  <div class="operation-list mt-5">
    <t-button theme="primary" class="search mr-5" @click="search">
      <img src="@/assets/icon/search.png" alt="">
      查询
    </t-button>
    <t-button type="reset" class="reset" @click="reset">
      <img src="@/assets/icon/reset.png" alt="">
      重置
    </t-button>
  </div>
  <div class="operation-res flex-between mt-11">
    <div>
      <img src="" alt="">
      查询结果
    </div>
    <t-button theme="primary" class="search" @click="download">
      <!-- <add-icon slot="icon" /> -->
      <img src="@/assets/icon/search.png" alt="">
      下载数据
    </t-button>
  </div>
  <t-divider class="divider"></t-divider>

  <!-- <div v-show="tableData"> -->
  <t-tabs v-model="activeTab" class="estimate-tabs">
    <t-tab-panel value="bar" label="柱状图" :destroyOnHide="false">
      <EchartsVue v-show="series.length > 0" :chartType="'bar'" :xAxisData="xAxisData" :series="series"
        :title="chartTitle" :xUnit="currentTimeValid === 'hour' ? '小时' : '日期'" :yUnit="method.includes('率') ? '%' : ''">
      </EchartsVue>
      <div v-show="series.length === 0" class="default-content">
        <img src="@/assets/img/default.png" alt="">
        <div>暂无任何内容</div>
      </div>
    </t-tab-panel>
    <t-tab-panel value="line" label="折线图" :destroyOnHide="false">
      <EchartsVue v-show="series.length > 0" :chartType="'line'" :xAxisData="xAxisData" :series="series"
        :title="chartTitle" :xUnit="currentTimeValid === 'hour' ? '小时' : '日期'" :yUnit="method.includes('率') ? '%' : ''">
      </EchartsVue>
      <div v-show="series.length === 0" class="default-content">
        <img src="@/assets/img/default.png" alt="">
        <div>暂无任何内容</div>
      </div>
    </t-tab-panel>
    <t-tab-panel value="table" label="表格" :destroyOnHide="false">
      <ResultTable v-if="tableData.length > 0" :data="tableData" :columns="headers" style="margin-top: 10px;">
      </ResultTable>
      <div v-else="tableData.length === 0" class="default-content">
        <img src="@/assets/img/default.png" alt="">
        <div>暂无任何内容</div>
      </div>
    </t-tab-panel>
  </t-tabs>


  <!-- </div> -->

  <stationDialog ref="sdRef" v-model:selectedStation="selectedStation" v-model:selectedArea="selectedArea">
  </stationDialog>

  <t-loading :loading="isLoading" :fullscreen="true"></t-loading>
</template>

<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import CheckboxList from '@/components/CheckboxList/index.vue'
import RadioList from '@/components/CheckboxList/RadioList.vue'
import ResultTable from './ResultTable.vue'
import EchartsVue from './EchartsVue.vue'
import stationDialog from './stationDialog.vue'
import { EstimateService } from '@/api'
import dayjs from 'dayjs'
import { MessagePlugin } from 'tdesign-vue-next'
import { colors } from '@/utils/echartsConfigColor'


const startDate = ref('')
const endDate = ref('')
let currentYear = ref(dayjs().year())
const monthList = ref(['1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12'])
const seasonLsit = ref(['第一季度', '第二季度', '第三季度', '第四季度'])
let dateMode = ref('0') // 0 常用，1 精确
let generalDateMode = ref('')
const timeValue = ref('2008')
let currentTimeValid = ref('day')
const currentTimeValidRef = ref(null)
const timeValidList = ref([
  {
    label: '逐时评分',
    value: 'hour',
  },
  {
    label: '逐日评分',
    value: 'day'
  }
])
let currentDay = ref('24')
const dayList = ref([
  {
    label: '24h',
    value: '24'
  },
  {
    label: '48h',
    value: '48'
  },
  {
    label: '72h',
    value: '72'
  },
  {
    label: '96h',
    value: '96'
  },
  {
    label: '120h',
    value: '120'
  },
  {
    label: '144h',
    value: '144'
  },
  {
    label: '168h',
    value: '168'
  },
  {
    label: '192h',
    value: '192'
  },
  {
    label: '216h',
    value: '216'
  },
  {
    label: '240h',
    value: '240'
  },
])
let selectedStation = ref(null)
let selectedArea = ref(null)
// const stationList = ref([
//   {
//     value: '1',
//     label: '1',
//     children: [
//       {
//         label: '子1',
//         value: '1.1'
//       }
//     ]
//   },
//   {
//     value: '2',
//     label: '2',
//   },
//   {
//     value: '3',
//     label: '3',
//   },
//   {
//     value: '4',
//     label: '4',
//   },
// ])
// let province = []
// let city = []
// let cnty = []
// let station = []

let weatherFeature = ref('')
let weatherFeatureRef = ref()
const weatherFeatureList = ref([]);
let method = ref('')
let methodRef = ref()
const checkMethod = ref([]);

let currentSource = ref([]) // 模式
let currentSourceRef = ref(null)
let sourceList = ref([])

let sdRef = ref(null)

let tableData = ref([])
let headers = ref([])

let isLoading = ref(false)

const handleDateChange = (event) => {
  const target = event.target;
  // 确保点击的是 li 标签
  if (target.tagName.toLowerCase() === 'li') {
    currentDay.value = target.dataset.date
    // updateTableData()
  }
}

const setInitDate = () => {
  let year = currentYear.value.toString()
  let month = dayjs().month() + 1
  let date = dayjs().date()
  startDate.value = dayjs(year).month(month - 1).format('YYYY-MM-01')
  endDate.value = dayjs(year).month(month).subtract(1, 'day').format('YYYY-MM-DD')
  generalDateMode.value = month.toString()
}
setInitDate()

let lastStartDate0 = ''
let lastEndDate0 = ''
let lastStartDate1 = ''
let lastEndDate1 = ''
watch(dateMode, (newval) => {
  if (newval === '0') {
    lastStartDate1 = startDate.value
    lastEndDate1 = endDate.value
    if (lastStartDate0) {
      startDate.value = lastStartDate0
      endDate.value = lastEndDate0
    }
  } else {
    lastStartDate0 = startDate.value
    lastEndDate0 = endDate.value
    if (lastStartDate1) {
      startDate.value = lastStartDate1
      endDate.value = lastEndDate1
    }
  }
})

const changeDateMode = (mode) => {
  generalDateMode.value = mode
  if (mode) {
    let year = currentYear.value.toString()
    if (!isNaN(mode * 1)) {
      let month = mode * 1
      startDate.value = dayjs(year).month(month - 1).format('YYYY-MM-DD')
      endDate.value = dayjs(year).month(month).subtract(1, 'day').format('YYYY-MM-DD')
    } else {
      if (mode === '近一月') {
        let lastmonth = dayjs().month()
        let date = dayjs().date()
        startDate.value = dayjs().month(lastmonth - 1).date(date).format(`${year}-MM-DD`)
        endDate.value = dayjs().format(`${year}-MM-DD`)
      } else if (mode === '近一周') {
        startDate.value = dayjs().subtract(7, 'day').format(`${year}-MM-DD`)
        endDate.value = dayjs().format(`${year}-MM-DD`)
      } else if (mode === '第一季度') {
        startDate.value = dayjs().format(`${year}-01-01`)
        endDate.value = dayjs().format(`${year}-03-31`)
      } else if (mode === '第二季度') {
        startDate.value = dayjs().format(`${year}-04-01`)
        endDate.value = dayjs().format(`${year}-06-30`)
      } else if (mode === '第三季度') {
        startDate.value = dayjs().format(`${year}-07-01`)
        endDate.value = dayjs().format(`${year}-09-30`)
      } else if (mode === '第四季度') {
        startDate.value = dayjs().format(`${year}-10-01`)
        endDate.value = dayjs().format(`${year}-12-31`)
      }
    }
  }
}

watch(currentYear, (val) => {
  startDate.value && (startDate.value = dayjs(startDate.value).year(val).format('YYYY-MM-DD'))
  endDate.value && (endDate.value = dayjs(endDate.value).year(val).format('YYYY-MM-DD'))
})

// 处理动态加载数据
// const handleLoad = async (node) => {
//   console.log('handleLoad', node);
//   let nodes = []
//   if (node.level === 0) {
//     const res = await EstimateService.getCities({
//       id: node.data.value
//     });
//     // 市节点
//     nodes = res.map(item => {
//       return {
//         value: node.data.value + '-' + item.id,
//         label: item.city,
//         provinceId: item.provinceId,
//         children: true
//       }
//     })
//   } else if (node.level === 1) {
//     let id = node.data.value.split('-')[node.level] * 1
//     const res = await EstimateService.getDistrict({
//       id: id,
//       provinceId: node.data.provinceId
//     });
//     // 县节点
//     nodes = res.map(item => {
//       return {
//         value: node.data.value + '-' + item.id,
//         label: item.cnty,
//         provinceId: item.provinceId,
//         cityId: item.cityId,
//         children: true
//       }
//     })
//   } else if (node.level === 2) {
//     let id = node.data.value.split('-')[node.level] * 1
//     const res = await EstimateService.getStations({
//       id: id,
//       cityId: node.data.cityId,
//       provinceId: node.data.provinceId
//     });
//     // 站点节点
//     nodes = res.map(item => {
//       return {
//         value: node.data.value + '-' + item.station,
//         label: item.stationName,
//         provinceId: item.provinceId,
//         cityId: item.cityId,
//         cntyId: item.cntyId
//       }
//     })
//   }
//   return nodes
// };

// const handleSelectChange = (value, context) => {
//   // console.log('handleSelectChange');
//   // 清空数组
//   // province = [];
//   // city = [];
//   // cnty = [];
//   station = [];
//   value.forEach(v => {
//     let arr = v.split('-')
//     if (arr[3] && !station.includes(arr[0])) {
//       station.push(arr[3])
//     }
//     // if (arr[2] && !cnty.includes(arr[2])) {
//     //   cnty.push(arr[2])
//     // }
//     // if (arr[1] && !city.includes(arr[1])) {
//     //   city.push(arr[1])
//     // }
//     // if (arr[0] && !province.includes(arr[0])) {
//     //   province.push(arr[0])
//     // }
//   })
// }


// const formatHearders = () => {
//   let obj = [{
//     label: '站点',
//     colKey: 'station'
//   }]
//   // weatherFeature.value.forEach(f => {
//   let valueforFirst = weatherFeature.value
//   let first = {
//     label: weatherFeatureList.value.find(x => x.value === f)?.label
//   }
//   timeValidity.value.forEach(time => {
//     let valueforSecond = time
//     let second = {
//       label: timeList.find(x => x.value === time)?.label
//     }
//     if (!first.children) {
//       first.children = []
//     }
//     first.children.push(second)
//     method.value.forEach(m => {
//       let valueforThird = m
//       let third = {
//         label: checkMethod.value.find(x => x.value === m)?.label,
//         colKey: valueforFirst + '_' + valueforSecond + '_' + valueforThird
//       }
//       if (!second.children) {
//         second.children = []
//       }
//       second.children.push(third)
//     })
//   })
//   obj.push(first)
//   // })
//   return obj
// }

// const formatTableData = (data) => {
//   return Object.entries(data).map(([station, elements]) => {
//     let formatted = { station };

//     Object.entries(elements).forEach(([element, times]) => {
//       Object.entries(times).forEach(([time, methods]) => {
//         Object.entries(methods).forEach(([method, value]) => {
//           formatted[`${element}_${time}_${method}`] = value;
//         });
//       });
//     });

//     return formatted;
//   });
// }

const formatHearders = () => {
  let obj = [{
    // label: currentTimeValid.value === 'hour' ? '预报时效' : '日期',
    label: '产品类型',
    colKey: 'source'
  }]
  xAxisData.value.forEach(item => {
    obj.push({
      label: item,
      colKey: item
    })
  })
  return obj
}

const formatTableData = (data) => {
  tableData.value = []
  for (let k in data) {
    let obj = {}

    if (k === 'fst') {
      obj['source'] = '订正'
    }
    let sourceLabel = sourceList.value.find(x => x.value === k)?.label
    if (sourceLabel) {
      obj['source'] = sourceLabel
    }

    // Object.assign(obj, data[k][weatherFeature.value][method.value])
    tableData.value.push({
      ...obj,
      ...data[k][weatherFeature.value][method.value]
    })
  }
}

const getWeatherFeature = async () => {
  try {
    const res = await EstimateService.getWeatherFeature1({
      dataType: 'station'
    })
    if (res && res.length > 0) {
      weatherFeatureList.value = res.map(item => {
        return {
          value: item.element,
          label: item.elementName
        }
      })
      weatherFeature.value = weatherFeatureList.value[0].value
    }
  } catch (error) { }
}
const getCheckMethod = async () => {
  try {
    const res = await EstimateService.getCheckMethodByFeature({
      element: weatherFeature.value
    })
    if (res && res.length > 0) {
      checkMethod.value = res.map(item => {
        return {
          value: item,
          label: item
        }
      })
      method.value = checkMethod.value[0].value
    } else {
      checkMethod.value = []
    }
  } catch (error) {
    checkMethod.value = []
  }
}

const getModeName = async () => {
  try {
    const res = await EstimateService.getModeName({
      dataType: 'chkstation'
    })
    if (res && res.length > 0) {
      sourceList.value = res.map(item => {
        return {
          value: item.dataSource,
          label: item.dataSourceName
        }
      })
    } else {
      sourceList.value = null
    }
  } catch (error) {
    sourceList.value = null
  }
}

// const getProvinceLists = async () => {
//   stationList.value = []
//   const res = await EstimateService.getProvinces()
//   if (res && res.length > 0) {
//     res.forEach(p => {
//       stationList.value.push({
//         value: p.id.toString(),
//         label: p.province,
//         children: true
//       })
//     })
//   }
// }

const openDialog = () => {
  sdRef.value?.open()
}

const activeTab = ref('bar')

let chartTitle = ref('')
let xAxisData = ref([])
let series = ref([])

const getChartsData = (data) => {
  xAxisData.value = []
  series.value = []
  let allName = []

  for (let k in data) {
    if (data[k] && xAxisData.value.length === 0) {
      if (currentTimeValid.value === 'hour') {
        xAxisData.value = Object.keys(data[k][weatherFeature.value][method.value])
      } else {
        for (let d = dayjs(startDate.value); d.isBefore(dayjs(endDate.value)); d = d.add(1, 'day')) {
          xAxisData.value.push(d.format('YYYY-MM-DD'))
        }
        xAxisData.value.push(endDate.value)
      }
    }
    if (k === 'fst') continue
    let datasourceName = sourceList.value.find(x => x.value === k)?.label ?? ''
    series.value.push({
      name: datasourceName,
      data: Object.values(data[k][weatherFeature.value][method.value]),
      connectNulls: true,
      color: colors[k]
    })

    allName.push(datasourceName)
  }
  if (data['fst']) {
    series.value.push({
      name: '订正',
      data: Object.values(data['fst'][weatherFeature.value][method.value]),
      connectNulls: true,
      color: colors['fst']
    })
    allName.push('订正')
  }

  let weatherName = weatherFeatureList.value.find(x => x.value === weatherFeature.value)?.label ?? ''
  let methodName = checkMethod.value.find(x => x.value === method.value)?.label ?? ''
  chartTitle.value = allName.join('/') + '  ' + weatherName + '  ' + methodName

}

const checkParams = () => {
  if (!startDate.value || !endDate.value) {
    MessagePlugin.warning('请选择检验时段')
    return false
  }
  if (!weatherFeature.value) {
    MessagePlugin.warning('请选择检验要素')
    return false
  }
  if (!method.value) {
    MessagePlugin.warning('请选择检验方法')
    return false
  }
  if (!selectedStation.value && !selectedArea.value) {
    MessagePlugin.warning('请选择站点或区域')
    return false
  }
  // if (currentSource.value.length === 0) {
  //   MessagePlugin.warning('请选择模式')
  //   return false
  // }
  return true
}

// 查询
const search = async () => {
  try {
    if (!checkParams()) return
    isLoading.value = true

    let params
    let res

    let start = startDate.value + ' ' + dayjs().hour(timeValue.value).format('HH:00:00')
    let end = endDate.value + ' ' + dayjs().hour(timeValue.value).format('HH:00:00')
    if (timeValue.value === '2008') {
      start = startDate.value + ' ' + '2008:00:00'
      end = endDate.value + ' ' + '2008:00:00'
    }
    // 逐时
    if (currentTimeValid.value === 'hour') {
      params = {
        startValidDate: start,
        endValidDate: end,
        vtis: ["0", "24", "48", "72", "96", "120", "144", "168", "192", "216", "240"],
        elements: [weatherFeature.value],
        // elements: ['rain'],
        methods: [method.value],
        // stations: typeof selectedStation.value === 'string' ? [selectedStation.value] : selectedStation.value.split(','),
        stations: selectedStation.value ? [selectedStation.value.value] : [],
        zone: selectedArea.value ? selectedArea.value.value : '',
        dataSources: currentSource.value
      }
      res = await EstimateService.getStationEstimateByHour(params)
    } else {
      // 逐日
      params = {
        startValidDate: start,
        endValidDate: end,
        vti: currentDay.value, // 选的逐日小时标签
        disVti: '24',
        elements: [weatherFeature.value],
        methods: [method.value],
        // stations: typeof selectedStation.value === 'string' ? [selectedStation.value] : selectedStation.value.split(','),
        stations: selectedStation.value ? [selectedStation.value.value] : [],
        zone: selectedArea.value ? selectedArea.value.value : '',
        dataSources: currentSource.value
      }
      res = await EstimateService.getStationEstimateByDay(params)
    }

    if (res) {
      getChartsData(res)

      headers.value = formatHearders()
      formatTableData(res)

      // tableData.value = formatTableData(res)

    } else {
      tableData.value = []
      headers.value = []
      xAxisData.value = []
      series.value = []
      chartTitle.value = ''
    }
  } catch (error) {
    tableData.value = []
    headers.value = []
    xAxisData.value = []
    series.value = []
    chartTitle.value = ''
    MessagePlugin.error('查询失败')
  } finally {
    isLoading.value = false
  }
}

// 重置
const reset = () => {
  // generalDateMode.value = ''
  // startDate.value = ''
  // endDate.value = ''
  setInitDate()
  timeValue.value = '2008'
  currentTimeValid.value = 'day'
  currentDay.value = '24'
  weatherFeature.value = weatherFeatureList.value[0].value
  // weatherFeatureRef.value?.clear()
  method.value = ''
  // methodRef.value?.clear()
  getCheckMethod()
  // selectedStation.value = ''
  // selectedArea.value = ''
  sdRef.value?.clear()
  currentSource.value = []
  currentSourceRef.value?.clear()
}

// 下载
const download = async () => {
  try {
    // if (!checkParams()) return
    // let params = {
    // startTime: dayjs(dateValue.value[0]).format('YYYY-MM-DD HH:mm:ss'),
    // endTime: dayjs(dateValue.value[1]).format('YYYY-MM-DD HH:mm:ss'),
    // vtis: [...timeValidity.value],
    // elements: weatherFeature.value,
    // methods: [...method.value],
    // stations: station,
    // province: province,
    // city: city,
    // cnty: cnty
    // }

    if (tableData.value.length > 0) {
      const exportData = {}
      exportData['columns'] = headers.value.map(item => item.label)
      const newobj = new Map()
      const allVal = []
      tableData.value.forEach(t => {
        let sourceval = t.source
        delete t.source
        newobj.set('source',sourceval)
        Object.keys(t).forEach(k=>{newobj.set(k,t[k])})
        allVal.push(Array.from(newobj.values()).join(','))
      })
      exportData['data'] = allVal
      const res = await EstimateService.downloadCSV({
        contentsMap: exportData
      })
      if (res) {
        const blob = new Blob([res], { type: "text/csv;charset=utf-8;" });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.setAttribute("download", `${startDate.value}至${endDate.value} ${chartTitle.value}.csv`);
        document.body.appendChild(link);
        link.click();
        // 释放资源
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
      }
    }


  } catch (error) {
    MessagePlugin.error('下载失败')
  }
}

watch(weatherFeature, () => {
  method.value = ''
  methodRef.value?.clear()
  getCheckMethod()
})

getWeatherFeature()
getModeName()

onBeforeUnmount(() => {
  tableData.value = null
})

</script>

<style lang="less" scoped>
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

.select-time-box {
  display: flex;
  flex-direction: column;
  height: 60px;
  border: 1px solid #5d6069;

  .blockbox {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 30px;

    .labelText {
      display: inline-block;
      height: 30px;
      line-height: 30px;
      padding: 0 4px;
      text-align: center;
      white-space: nowrap;
      font-size: 12px;
    }

    .labelText:last-child {
      border-right: none;
    }

    :deep(.blockbox-datepicker) {
      width: 100%;

      .t-select-input .t-input {
        height: 30px;
        border-color: transparent !important;
        background: transparent !important;
      }
    }
  }

  .blockbox .active {
    color: #fff;
    background: linear-gradient(188deg, #21CFFF 0%, #3856E8 100%);
  }
}

.bottom-border {
  border-bottom: 1px solid #5d6069;
}

.right-border {
  border-right: 1px solid #5d6069;
}

.date-change-container {
  display: flex;
  align-items: center;
  height: 30px;
  padding: 0 5px;
  border: 1px solid #2D4098;
  border-radius: 8px;
}

.date-list {
  display: flex;
}

.date-list li {
  height: 18px;
  line-height: 18px;
  padding: 0 8px;
  margin-right: 0.75rem;
  font-size: 10px;
  color: rgba(188, 200, 255, 1);
  background: #2D4098;
  cursor: pointer;
  border-radius: 0.375rem;
  transition: background 0.3s;
}

.date-list li:last-child {
  margin-right: 0;
}

.date-list li.active {
  color: #fff;
  background: linear-gradient(188deg, #21CFFF 0%, #3856E8 100%);
}
</style>