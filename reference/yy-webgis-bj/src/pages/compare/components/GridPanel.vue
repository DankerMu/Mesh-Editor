<template>

  <TRow class="w-full list mt-6">
    <TCol :span="6">
      <span style="font-size: 16px;">站点</span>
      <div class="position-div">{{ position }}</div>
    </TCol>
    <TCol :span="6">
      <span style="font-size: 16px;">起报时间</span>
      <!-- 日期选择器 -->
      <DatePicker :popupProps="{ overlayClassName: 'yy-date-picker-popup' }" size='large' overlayClassName
        v-model:value="gridParams.qbtime" type="date" format="YYYY-MM-DD" class="date-sel" clearable>
        <template #valueDisplay="{ displayValue }">
          <span style="font-weight: bold;font-size: 13px;color:aliceblue"></span>{{ displayValue }}
        </template>
      </DatePicker>

      <!-- 时间选择器 -->
      <Select :popupProps="{ overlayClassName: 'gradient-select-popup' }" size='large' v-model:value="gridParams.time"
        @click.stop placeholder="" :disabled="!gridParams.qbtime" style="width: 30%;" class="time-sel">
        <Option value="8" label="08:00">08:00</Option>
        <Option value="20" label="20:00">20:00</Option>
      </Select>
    </TCol>
  </TRow>
  <CheckboxList title="模式产品：" v-model:modelValue="currentSource" :options="sourceList" style="margin-top: 4px;">
  </CheckboxList>
  <CheckboxList title="订正产品：" v-model:modelValue="currentType" :options="typeList" style="margin-top: 4px;">
  </CheckboxList>
  <div class="flex align-center" style="margin-bottom: 16px;margin-top: 4px;">
    气象要素：
    <ul class="date-list" @click="handleFeatureChange">
      <li class="active">降水量</li>
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
  <div v-show="dataGrid.length > 0">
    <Echarts ref="gChartRef"></Echarts>
  </div>
  <div v-show="dataGrid.length === 0">
    <div class="default-content">
      <img src="@/assets/img/default.png" alt="">
      <div>暂无任何内容</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onBeforeUnmount, nextTick } from 'vue'
import Echarts from './EchartsforGrid.vue'
import CheckboxList from '@/components/CheckboxList/index.vue'
import { CompareService, EstimateService } from '@/api'
import { DatePicker, Select, Option } from 'tdesign-vue-next';
import dayjs from 'dayjs';
import { MessagePlugin } from 'tdesign-vue-next'
import { colors } from '@/utils/echartsConfigColor'

const props = defineProps({
  position: {
    type: String,
    default: ''
  },
  positionId: {
    type: Number,
    default: 0
  }
});

// 格点查询参数
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
let gridParams = ref({
  qbtime: defaulDate,
  time: defaultTime,
  feature: 'precipitation',
})
const dataGrid = ref([])
let gChartRef = ref(null)

let currentSource = ref([])
let sourceList = ref([])

let currentType = ref([])
let currentTypeName = ref('')
let typeList = ref([])

const getDataSourceByDataType = async () => {
  try {
    // 模式
    const res = await CompareService.getDataSourceByDataType({
      dataType: 'cmpgrib'
    })
    sourceList.value = res.map(item => {
      return {
        value: item.dataSource,
        label: item.dataSourceName
      }
    })
    // 订正
    const res1 = await CompareService.getDataSourceByDataType({
      dataType: 'prc'
    })
    typeList.value = res1.map(item => {
      return {
        value: item.dataSource,
        label: item.dataSourceName
      }
    })
  } catch (error) {
    sourceList.value = []
    typeList.value = []
  }
}
getDataSourceByDataType()


const isAllEmptyObjects = (obj) => {
  return Object.values(obj).every(value =>
    typeof value === "object" && value !== null && Object.keys(value).length === 0
  );
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
    let dataTime = dayjs(gridParams.value.qbtime).hour(gridParams.value.time).format('YYYY-MM-DD HH:00:00')

   // const { dataSource, dataSourceName } = await EstimateService.queryGribUsedModel()
    let params = {
      station: props.positionId,
      dataTime: dataTime,
      //dataSource: currentType.value, 
      dataSource: Array.isArray(currentType.value)?[...currentType.value]:[currentType.value],
      // dataSource: [dataSource],
      dataSourceOrg: currentSource.value
    }

    const res = await CompareService.getGridComparisonData(params)
    if (res && !isAllEmptyObjects(res)) {
      dataGrid.value = []

      if (res['obs']) {
        dataGrid.value.push({
          label: '实况',
          data: res['obs'],
          color: colors['obs']
        })
      }
      // if (res['fst']) {
      //   dataGrid.value.push({
      //     label: '订正',
      //     data: res['fst'],
      //     color: colors['fst']
      //   })
      //}
      // if (res['deep']) {
      //   dataGrid.value.push({
      //     label: '智能订正',
      //     data: res['deep'],
      //     color: colors['deep']
      //   })
      // }
      // if (res['grapes']) {
      //   dataGrid.value.push({
      //     label: 'GRAPES_GFS订正',
      //     data: res['grapes'],
      //     color: colors['grapes']
      //   })
      // }
      delete res.obs
    
      for (let key in res) {
        // if (key === 'obs') {
        //   continue
        // } else if (key === 'fst') {
        //   continue
        // } else {

          let name = ''
          if (key.endsWith('org')) {
            // 原始
            let oriKey = key.split('_')[0]
            name = sourceList.value.find(x => x.value === oriKey)?.label
          } else {
            // 订正
            name = typeList.value.find(x => x.value === key)?.label
            //name = dataSourceName
          }

          dataGrid.value.push({
            label: name,
            color: colors[key],
            data: res[key]
          })
        // }
      }
      
      console.log('dataGrid.value', dataGrid.value)
      nextTick(() => {
        gChartRef.value?.initChart(dataGrid.value, {
          label: '降水量',
          unit: 'mm'
        }, 'grid')
      })
    } else {
      dataGrid.value = []
    }
  } catch (error) {
    dataGrid.value = []
  }

}

// 下载
// const download = async () => {
//   try {
//     let dataTime = dayjs(stationParams.value.qbtime).hour(stationParams.value.time).format('YYYY-MM-DD HH:00:00')
//     let params = {
//       station: "55026",
//       dataTime: dataTime,
//     }
//     const res = await CompareService.downloadGridData(params)

//     const blob = new Blob([res], { type: "text/csv;charset=utf-8;" });
//     const url = URL.createObjectURL(blob);
//     const link = document.createElement("a");
//     link.href = url;
//     link.setAttribute("download", "data.csv");
//     document.body.appendChild(link);
//     link.click();
//     // 释放资源
//     document.body.removeChild(link);
//     URL.revokeObjectURL(url);
//   } catch (error) {
//     MessagePlugin.error('下载失败')
//   }
// }

const clearChartData = () => {
  dataGrid.value = []
}

defineExpose({
  search,
  clearChartData
})

onBeforeUnmount(() => {
  dataGrid.value = []
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