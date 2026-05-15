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

    <RadioList title="时效：" v-model:modelValue="currentTimeValid" :defaulValue="currentTimeValid"
      :options="timeValidList">
    </RadioList>
    <div v-show="currentTimeValid === 'day'">
      <div class="date-change-container mr-18">
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
    <div>检验要素：</div>
    <div style="height: 38px;line-height: 22px;background-color: var(--app-ui-bg-color);padding: 8px;" class="mr-18">{{
      weatherFeature?.label }}
    </div>
    <RadioList ref="methodRef" title="检验方法：" v-model:modelValue="method" :options="checkMethod" :defaulValue="method"
      class="mr-18">
    </RadioList>
    <RadioList title="区域：" v-model:modelValue="currentAreaMode" :defaulValue="currentAreaMode" :options="areaModeList">
    </RadioList>
    <t-select v-show="currentAreaMode === 'select'" v-model="selectArea" :options="areaList" class="time-sel mr-18"
      style="width: 200px;" :popupProps="{ overlayClassName: 'pure-select-popup' }"></t-select>
    <t-input-group v-show="currentAreaMode === 'input'" class="choose-district mr-3">
      <t-input v-model="leftupPosition.lon" label="经度：" placeholder="请输入经度" clearable />
      <t-input v-model="leftupPosition.lat" label="纬度：" suffix="左上角" placeholder="请输入纬度" clearable />
    </t-input-group>
    <t-input-group v-show="currentAreaMode === 'input'" class="choose-district">
      <t-input v-model="RightdownPosition.lon" label="经度：" placeholder="请输入经度" clearable />
      <t-input v-model="RightdownPosition.lat" label="纬度：" suffix="右下角" placeholder="请输入纬度" clearable />
    </t-input-group>
    <div v-show="currentAreaMode === 'input'" class="iptTip">经度：[70, 111]<br />纬度：[25, 50]</div>
  </div>
  <div class="flex align-center mt-5" style="height: 38px;">
    <CheckboxList ref="currentSourceRef" class="mr-18" title="模式产品：" v-model:modelValue="currentSource"
      :options="sourceList">
    </CheckboxList>
    <CheckboxList ref="currentTypeRef" title="订正产品：" v-model:modelValue="currentType" :options="typeList">
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
      <ResultTable v-if="tableData.length > 0" :data="tableData" :columns="headers" style="margin-top: 10px;"></ResultTable>
      <div v-else="tableData.length === 0" class="default-content">
        <img src="@/assets/img/default.png" alt="">
        <div>暂无任何内容</div>
      </div>
    </t-tab-panel>
  </t-tabs>

  <t-loading :loading="isLoading" :fullscreen="true"></t-loading>
</template>

<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import CheckboxList from '@/components/CheckboxList/index.vue'
import RadioList from '@/components/CheckboxList/RadioList.vue'
import ResultTable from './ResultTable.vue'
import EchartsVue from './EchartsVue.vue'
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
let currentAreaMode = ref('select')
const areaModeList = ref([
  {
    label: '区域选择',
    value: 'select'
  },
  {
    label: '区域输入',
    value: 'input'
  }
])
let selectArea = ref('')
const areaList = ref([])
const leftupPosition = ref({
  lon: '',
  lat: ''
})
const RightdownPosition = ref({
  lon: '',
  lat: ''
})
watch(currentAreaMode, (newval) => {
  if (newval === 'select') {
    leftupPosition.value.lat = ''
    leftupPosition.value.lon = ''
    RightdownPosition.value.lat = ''
    RightdownPosition.value.lon = ''
    selectArea.value = areaList.value[0].value
  } else {
    selectArea.value = ''
  }
})

let weatherFeature = ref()
const weatherFeatureList = ref([]);
let method = ref('')
let methodRef = ref([])
const checkMethod = ref([]);
let currentSource = ref([]) // 模式
let currentSourceRef = ref(null)
let sourceList = ref([])
let currentType = ref([]) // 订正产品
let currentTypeName = ref('')
let currentTypeRef = ref(null)
const typeList = ref([])
// ref([
//   {
//     label: 'ECMWF订正',
//     value: 'ecmf'
//   },
//   {
//     label: 'GRAPES_GFS订正',
//     value: 'grapes'
//   },
//   {
//     label: '智能订正',
//     value: 'deep'
//   },
// ])

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


const getModeName = async () => {
  try {
    const res = await EstimateService.getModeName({
      dataType: 'cmpgrib'
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
  try {
    const res = await EstimateService.getModeName({
      dataType: 'prc'
    })
    if (res && res.length > 0) {
      typeList.value = res.map(item => {
        return {
          value: item.dataSource,
          label: item.dataSourceName
        }
      })
    } else {
      typeList.value = null
    }
  } catch (error) {
    typeList.value = null
  }
}

const getWeatherFeature = async () => {
  try {
    const res = await EstimateService.getWeatherFeature1({
      dataType: 'grib'
    })
    if (res && res.length > 0) {
      weatherFeatureList.value = res.map(item => {
        return {
          value: item.element,
          label: item.elementName
        }
      })
      weatherFeature.value = weatherFeatureList.value[0]
      getCheckMethod()
    }
  } catch (error) { }
}
const getCheckMethod = async () => {
  try {
    const res = await EstimateService.getCheckMethodByFeature({
      element: weatherFeature.value.value
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

const getZone = async () => {
  try {
    const res = await EstimateService.getZone()
    if (res && res.length > 0) {
      areaList.value = res.map(item => {
        return {
          value: item.id.toString(),
          label: item.dataZone
        }
      })
      selectArea.value = areaList.value[0].value
    } else {
      areaList.value = []
    }
  } catch (error) {
    areaList.value = []
  }
}


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
      obj['source'] = '订正后'
    }

    if (k.endsWith('org')) {
      // 原始
      let oriKey = k.split('_')[0]
      obj['source'] = sourceList.value.find(x => x.value === oriKey)?.label ?? ''
    } else {
      // 订正
      // obj['source'] = currentTypeName.value
      obj['source'] = typeList.value.filter(e=> e.value===k)[0].label
    }

    // Object.assign(obj, data[k][method.value])
    // tableData.value.push(obj)
    tableData.value.push({
      ...obj,
      ...data[k][method.value]
    })
  }
}

const activeTab = ref('bar')

let chartTitle = ref('')
let xAxisData = ref([])
let series = ref([])

const getChartsData = (data) => {
  xAxisData.value = []
  series.value = []
  let allName = []

  if (data['fst']) {
    series.value.push({
      name: '订正后',
      data: Object.values(data['fst'][method.value]),
      color: colors['fst']
    })
    allName.push('订正后')
  }
  for (let k in data) {
    if (data[k] && xAxisData.value.length === 0) {
      if (currentTimeValid.value === 'hour') {
        xAxisData.value = Object.keys(data[k][method.value])
      } else {
        for (let d = dayjs(startDate.value); d.isBefore(dayjs(endDate.value)); d = d.add(1, 'day')) {
          xAxisData.value.push(d.format('YYYY-MM-DD'))
        }
        xAxisData.value.push(endDate.value)
      }
    }
    if (k === 'fst') continue

    let datasourceName = ''
    if (k.endsWith('org')) {
      // 原始
      let oriKey = k.split('_')[0]
      datasourceName = sourceList.value.find(x => x.value === oriKey)?.label ?? ''
    } else {
      // 订正
      // datasourceName = currentTypeName.value
      datasourceName = typeList.value.filter(e=> e.value===k)[0].label
    }
    series.value.push({
      name: datasourceName,
      data: Object.values(data[k][method.value]),
      color: colors[k]
    })
    allName.push(datasourceName)

  }

  let weatherName = weatherFeatureList.value.find(x => x.value === weatherFeature.value.value)?.label ?? ''
  let methodName = checkMethod.value.find(x => x.value === method.value)?.label ?? ''
  chartTitle.value = allName.join('/') + '  ' + weatherName + '  ' + methodName

}

const checkLonLat = (val, type) => {

  if (type === 'lon') {
    let iptLon = parseFloat(val)
    if (isNaN(iptLon)) {
      MessagePlugin.warning('经度请输入数字');
      return false
    }
    if (iptLon < 70 || iptLon > 111) {
      MessagePlugin.warning('经度范围在70至111之间');
      return false
    }
  } else {
    let iptLat = parseFloat(val)
    if (isNaN(iptLat)) {
      MessagePlugin.warning('纬度请输入数字');
      return false
    }
    if (iptLat < 25 || iptLat > 50) {
      MessagePlugin.warning('纬度范围在25至50之间');
      return false
    }
  }

  return true
}

const checkParams = () => {
  if (!startDate.value || !endDate.value) {
    MessagePlugin.warning('请选择检验时段')
    return false
  }
  if (!method.value) {
    MessagePlugin.warning('请选择检验方法')
    return false
  }
  if (currentAreaMode.value === 'select') {
    if (selectArea.value === '') {
      MessagePlugin.warning('请选择区域')
      return false
    }
  } else {
    if (leftupPosition.value.lon === '' || leftupPosition.value.lat === '' || RightdownPosition.value.lon === '' || RightdownPosition.value.lat === '') {
      MessagePlugin.warning('请输入完整区域范围')
      return false
    }
    if (!checkLonLat(leftupPosition.value.lon, 'lon') || !checkLonLat(RightdownPosition.value.lon, 'lon') || !checkLonLat(leftupPosition.value.lat, 'lat') || !checkLonLat(RightdownPosition.value.lat, 'lat')) {
      return false
    }
  }
  // if (currentSource.value.length === 0) {
  //   MessagePlugin.warning('请选择模式产品')
  //   return false
  // }
  return true
}

const getDefaultMode = async()=>{
  const { dataSource, dataSourceName } = await EstimateService.queryGribUsedModel()
    currentType.value = dataSource
    currentTypeName.value = dataSourceName
}

getDefaultMode()

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
        elements: [weatherFeature.value.value],
        methods: [method.value],
        dataSources: Array.isArray(currentType.value)?[...currentType.value]:[currentType.value],
        //dataSources: currentType.value,
        dataSourcesOrg: [...currentSource.value],
        zone: selectArea.value,
        lonLeft: leftupPosition.value.lon,
        latUp: leftupPosition.value.lat,
        lonRight: RightdownPosition.value.lon,
        latDown: RightdownPosition.value.lat
      }
      res = await EstimateService.getGridEstimateByHour(params)
    } else {
      // 逐日
      params = {
        startValidDate: start,
        endValidDate: end,
        vti: currentDay.value, // 选的逐日小时标签
        disVti: '24',
        elements: [weatherFeature.value.value],
        methods: [method.value],
        //dataSources: currentType.value,
        //dataSources: [...currentType.value],
        dataSources: Array.isArray(currentType.value)?[...currentType.value]:[currentType.value],
        dataSourcesOrg: [...currentSource.value],
        zone: selectArea.value,
        lonLeft: leftupPosition.value.lon,
        latUp: leftupPosition.value.lat,
        lonRight: RightdownPosition.value.lon,
        latDown: RightdownPosition.value.lat
      }
      res = await EstimateService.getGridEstimateByDay(params)
    }
    if (res) {
      // headers.value = formatHearders()
      // tableData.value = formatTableData(res)

      getChartsData(res)

      headers.value = formatHearders()
      formatTableData(res)
    } else {
      tableData.value = null
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
  generalDateMode.value = ''
  startDate.value = ''
  endDate.value = ''
  setInitDate()
  timeValue.value = '2008'
  currentTimeValid.value = 'day'
  currentDay.value = '24'
  weatherFeature.value = weatherFeatureList.value[0]
  method.value = ''
  //methodRef.value?.clear()
  getCheckMethod()
  currentAreaMode.value = 'select'
  selectArea.value = selectArea.value = areaList.value[0].value
  leftupPosition.value.lon = ''
  leftupPosition.value.lat = ''
  RightdownPosition.value.lon = ''
  RightdownPosition.value.lat = ''
  currentSource.value = []
  currentSourceRef.value?.clear()
  currentType.value = []
  // currentTypeRef.value?.clear()
}

// 下载
const download = async () => {
  try {
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

getWeatherFeature()
getModeName()
getZone()

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

:deep(.choose-district) {
  .t-input {
    height: 40px;
    border-color: #2E3C8A !important;
    background: var(--app-border-color-dark);

    .t-input__prefix {
      color: var(--app-text-color-purple) !important;
    }

    .t-input__prefix:not(:empty) {
      margin-right: 0;
    }

    &.t-input--suffix>.t-input__suffix {
      color: #5d6069;
      border-left: 1px solid #2E3C8A;
      padding-left: 8px;
    }
  }

  .t-input__wrap:first-child {
    .t-input {
      width: 140px;
      border-right: none;
    }
  }

  .t-input__wrap:last-child {
    margin-right: 8px;

    .t-input {
      width: 190px;
      border-left: none;
    }
  }
}

.iptTip {
  color: #b3a755;
  font-size: 10px;
  line-height: 1.5;
}
</style>