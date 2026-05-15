<template>
  <div class="h-full">
    <LayoutManager>
      <template #main>
        <div class="content layout-bg">
          <div class="h-full content-bg pl-9 pr-9 pb-9">
            <div class="model-header">
              <HiIcon class="back" size="16px" :src="backstepSvg" @click="router.back()"></HiIcon>
              模型参数配置
            </div>
            <div class="model-content">
              <ul class="mode-list" ref="ulElement" @click="handleModeChange">
                <li v-for="(item, index) in modeList" :key="index" :data-mode="item.value"
                  :class="{ active: item.value == mode }">
                  {{ item.label }}</li>
              </ul>
              <div class="flex justify-between operate-container">
                <div class="choose-box">
                  <div class="title">模型名称：</div>
                  <t-input v-model="baseParams.modelName" class="ipt" placeholder="请输入模型名称" clearable></t-input>
                </div>
                <div class="choose-box">
                  <div class="title">版本号：</div>
                  <t-input v-model="baseParams.modelVersion" class="ipt" placeholder="请输入版本号" clearable></t-input>
                </div>
                <div class="choose-box">
                  <div class="title">模型选择：</div>
                  <t-select v-model="baseParams.model" class="select" :options="modelList"
                    :popupProps="{ overlayClassName: 'pure-select-popup' }" placeholder="请选择模型">
                  </t-select>
                </div>
                <div class="choose-box">
                  <div class="title">时间周期：</div>
                  <t-date-range-picker v-model="dateRangeIpt" class="dateRangePicker" allow-input clearable
                    :popupProps="{ overlayClassName: 'yy-date-picker-popup' }">
                    <template #suffixIcon>
                      <img src="@/assets/icon/calendar.png" alt="">
                    </template>
                  </t-date-range-picker>
                </div>
              </div>
              <div class="flex align-center" v-show="mode === 'nil'">
                <div class="choose-box operate-container" style="margin-right: 12px;padding: 0;">
                  <div class="title">站&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;号：</div>
                  <t-input v-model="nilStationId" class="ipt" placeholder="请输入站号" clearable></t-input>
                </div>
                <div class="title">站点选择：</div>
                <t-input-group class="choose-district">
                  <t-input v-model="position.lon" label="经度：" placeholder="请输入经度" clearable />
                  <t-input v-model="position.lat" label="纬度：" placeholder="请输入纬度" clearable />
                </t-input-group>
                <t-button class="choose-btn" @click="openMap">
                  <HiIcon size="20px" :src="chooseSvg"></HiIcon>
                  手动选择
                </t-button>
              </div>
              <div class="flex align-center" style="margin-bottom: 16px;" v-show="mode === 'single'">
                <div class="title">站点选择：</div>
                <t-cascader v-model="selectedStation" :options="stationList" clearable :show-all-levels="false"
                  :min-collapsed-num="1" @change="handleSingleSelectChange">
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
                </t-cascader>
                <t-button class="choose-btn" @click="openMap">
                  <HiIcon size="20px" :src="chooseSvg"></HiIcon>
                  手动选择
                </t-button>
              </div>
              <div class="flex align-center" style="margin-bottom: 16px;" v-show="mode === 'many'">
                <div class="title">站点选择：</div>
                <t-cascader v-model="selectedStations" :options="stationList" multiple clearable
                  :show-all-levels="false" :min-collapsed-num="2" @change="handleSelectChange">
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
                </t-cascader>
                <t-button class="choose-btn" @click="openMap">
                  <HiIcon size="20px" :src="chooseSvg"></HiIcon>
                  手动选择
                </t-button>
              </div>
              <div class="flex align-center" v-show="mode === 'zone'">
                <div class="title">站&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;号：</div>
                <t-tooltip :content="rectangleStations" placement="top">
                  <div class="extentStation">
                    {{ rectangleStations }}
                  </div>
                </t-tooltip>
                <t-button class="choose-btn" @click="openMap">
                  <HiIcon size="20px" :src="chooseSvg"></HiIcon>
                  手动选择
                </t-button>
              </div>
              <div class="operation-list" style="margin-top: 16px;">
                <t-button theme="primary" class="search mr-5" @click="createModel">
                  <HiIcon class="svg" size="16px" :src="okSvg"></HiIcon>
                  确认
                </t-button>
                <t-button type="reset" class="reset" @click="cancelCreate">
                  <HiIcon class="svg" size="16px" :src="cancelSvg"></HiIcon>
                  取消
                </t-button>
              </div>
            </div>
          </div>
        </div>
      </template>
    </LayoutManager>
  </div>

  <ChooseMap ref="choosemapRef" :mapMode="mode" @confirmStation="confirmStation"></ChooseMap>
</template>

<script setup lang="ts">
import LayoutManager from "@/layouts/LayoutManager.vue"
import ChooseMap from './ChooseMap.vue'
import type { Feature } from 'ol';
import type { Geometry } from 'ol/geom';
import { useRouter } from "vue-router";
import { EstimateService, ModelService } from '@/api'
import type { Province, StationItem, City, Cnty, Station } from '@/types/PagesType'
import { type CascaderValue, type CascaderChangeContext, type TreeNodeModel, type TreeOptionData as CascaderOption, MessagePlugin } from 'tdesign-vue-next'
import { HiIcon } from "hoci";
import backstepSvg from '@/assets/icon/backstep.svg'
import chooseSvg from '@/assets/icon/choose.svg'
import okSvg from '@/assets/icon/ok.svg'
import cancelSvg from '@/assets/icon/cancel.svg'

const router = useRouter()

let mode = ref('nil')
const modeList = [
  {
    value: 'nil',
    label: '无站点'
  },
  {
    value: 'single',
    label: '单站点'
  },
  {
    value: 'many',
    label: '多站点'
  },
  {
    value: 'zone',
    label: '区域'
  },

]
// 切换模式
const handleModeChange = (event: MouseEvent) => {
  const target = event.target as HTMLElement;
  // 确保点击的是 li 标签
  if (target.tagName.toLowerCase() === 'li') {
    mode.value = target.dataset.mode as string
  }
};

watch(mode, (newval) => {
  if (newval === 'many') {
    isMultiStation.value = true
  } else {
    isMultiStation.value = false
  }
})

let modelList = ref([])
let dateRangeIpt = ref([])
let isMultiStation = ref(true)
let selectedStation = ref('')
let selectedStations = ref([])
let nilStationId = ref('')
let position = ref({
  lon: '',
  lat: ''
})
let rectangleStations = ref('')
let rectangleStationsCode: string[] = []
const stationList = ref<CascaderOption[]>([])
const stationPathMap = new Map<string, string>()
let station: string[] = []

const userInfo = JSON.parse(sessionStorage.getItem("userInfo") ?? '{}')
const uname = ref(userInfo?.username ?? "")
let baseParams = reactive({
  modelName: '',
  modelVersion: '',
  publisher: uname,
  model: '',
})


const getModelOption = async () => {
  const res = await ModelService.getModelOption()
  modelList.value = res.map((item: any) => {
    return {
      value: item.model,
      label: item.model
    }
  })
}

// 主动构造整棵树的方法
const buildFullOptions = async () => {
  const provinces = await EstimateService.getProvinces()
  const result: CascaderOption[] = []

  for (const p of provinces) {
    const provinceNode: CascaderOption = {
      value: p.id.toString(),
      label: p.province,
      children: []
    }

    const cities = await EstimateService.getCities({ id: p.id })
    for (const c of cities) {
      const cityNode: CascaderOption = {
        value: `${p.id}-${c.id}`,
        label: c.city,
        children: []
      }

      const counties = await EstimateService.getDistrict({ id: c.id, provinceId: p.id })
      for (const cnty of counties) {
        const countyNode: CascaderOption = {
          value: `${p.id}-${c.id}-${cnty.id}`,
          label: cnty.cnty,
          children: []
        }

        const stations = await EstimateService.getStations({ id: cnty.id, cityId: c.id, provinceId: p.id })
        for (const s of stations) {
          const stationPath = `${p.id}-${c.id}-${cnty.id}-${s.station}`
          stationPathMap.set(s.station, stationPath)

          if (Array.isArray(countyNode.children)) {
            countyNode.children.push({
              value: stationPath,
              label: s.stationName
            });
          }
        }

        if (Array.isArray(cityNode.children)) {
          cityNode.children.push(countyNode)
        }
      }

      if (Array.isArray(provinceNode.children)) {
        provinceNode.children.push(cityNode)
      }
    }

    result.push(provinceNode)
  }

  stationList.value = result
}

// 单选级联
const handleSingleSelectChange = (value: CascaderValue<CascaderOption>, context: CascaderChangeContext<CascaderOption>) => {
  station = [];

  let arr = (value as string).split('-')
  if (arr[3] && !station.includes(arr[0])) {
    station.push(arr[3])
  }
}

// 多选级联
const handleSelectChange = (value: CascaderValue<CascaderOption>, context: CascaderChangeContext<CascaderOption>) => {
  station = [];

  const values = Array.isArray(value) ? value : [value];
  values.forEach((v) => {
    const str = String(v);
    let arr = str.split('-')
    if (arr[3] && !station.includes(arr[0])) {
      station.push(arr[3])
    }
  })
}

let choosemapRef = ref()
const openMap = () => {
  choosemapRef.value?.openMap()
}

const confirmStation = (val: string[] | any | Feature<Geometry>[]) => {
  console.log('手动选择的站点', val);
  if (mode.value === 'nil') {
    position.value.lon = val.lon
    position.value.lat = val.lat
  } else if (mode.value === 'single') {
    selectedStation.value = stationPathMap.get(val[0]) || val[0]
    station = val
  } else if (mode.value === 'many') {
    selectedStations.value = val
      .map((id: string) => stationPathMap.get(id))
      .filter(Boolean) // 去除 undefined 或空字符串等
    if (selectedStations.value.length > 0) {
      station = val
    } else {
      station = []
    }
  } else if (mode.value === 'zone') {
    rectangleStations.value = val.map((s: Feature<Geometry>) => {
      return s.get('stationIdD') + '(' + s.get('stationName') + ')'
    }).join(',')
    rectangleStationsCode = val.map((s: Feature<Geometry>) => s.get('stationIdD'));
  }
}

const allValuesNotEmpty = (obj: any) => {
  return Object.values(obj).every(value => value !== null && value !== undefined && value !== '');
}

const createModel = async () => {
  // debugger
  let params = {}
  switch (mode.value) {
    case 'nil': {
      params = {
        ...baseParams,
        ...position.value,
        modelType: mode.value,
        params: nilStationId.value,
        validTime: dateRangeIpt.value.join(',')
      }
      if (!allValuesNotEmpty(params)) {
        MessagePlugin.warning('请填写完整模型信息')
        return
      }
    }
    case 'single': {
      params = {
        ...baseParams,
        modelType: mode.value,
        params: station[0] ?? '',
        validTime: dateRangeIpt.value.join(',')
      }
      if (!allValuesNotEmpty(params)) {
        MessagePlugin.warning('请填写完整模型信息')
        return
      }
    }
    case 'many': {
      params = {
        ...baseParams,
        modelType: mode.value,
        params: station.join(','),
        validTime: dateRangeIpt.value.join(',')
      }
      if (!allValuesNotEmpty(params)) {
        MessagePlugin.warning('请填写完整模型信息')
        return
      }
    }
    case 'zone': {
      params = {
        ...baseParams,
        modelType: mode.value,
        params: rectangleStationsCode.join(','),
        validTime: dateRangeIpt.value.join(',')
      }
      if (!allValuesNotEmpty(params)) {
        MessagePlugin.warning('请填写完整模型信息')
        return
      }
    }
  }

  try {
    const res = await ModelService.createNewModel(params)
    if (res.code === 200) {
      MessagePlugin.success('创建成功！')
      cancelCreate()
    }
  } catch (error) {
    MessagePlugin.error('创建失败！')
  }

}

const cancelCreate = () => {
  router.back()
}

getModelOption()
buildFullOptions()

</script>

<style lang="less" scoped>
.content {
  height: 100%;
  position: relative;
  z-index: 3;
  padding: var(--app-view-padding);
}

.model-header {
  display: flex;
  align-items: center;
  height: 54px;
  line-height: 54px;
  font-size: 16px;
  border-bottom: 1px solid #263D98;

  .back {
    color: var(--app-text-color-purple);
    margin-right: 4px;
    cursor: pointer;
  }
}

.model-content {
  font-size: 16px;

  .mode-list {
    display: flex;
    list-style: none;
    overflow-x: auto;
    flex: 1;
    padding: 24px 0 16px 0;
    white-space: nowrap;
    scroll-behavior: smooth;
  }

  .mode-list li {
    width: 76px;
    height: 32px;
    line-height: 32px;
    text-align: center;
    padding: 0 1.125rem;
    margin-right: 12px;
    font-size: 1.3125rem;
    color: rgba(188, 200, 255, 1);
    background: #2D4098;
    ;
    cursor: pointer;
    border-radius: 0.375rem;
    transition: background 0.3s;
  }

  .mode-list li.active {
    color: #fff;
    background: linear-gradient(188deg, #21CFFF 0%, #3856E8 100%);
  }

  .choose-box {
    display: flex;
    align-items: center;
  }

  .title {
    line-height: 40px;
    white-space: nowrap;
  }

  :deep(.ipt),
  :deep(.select) {
    width: 280px;

    .t-input {
      width: 280px;
      height: 40px;
      background: var(--app-border-color-dark);
    }
  }

  :deep(.dateRangePicker) {
    .t-range-input {
      width: 280px;
      height: 40px;
    }
  }

  :deep(.choose-district) {
    .t-input {
      width: 140px;
      height: 40px;
      border-color: #2E3C8A !important;
      background: var(--app-border-color-dark);

      .t-input__prefix {
        color: var(--app-text-color-purple) !important;
      }

      .t-input__prefix:not(:empty) {
        margin-right: 0;
      }
    }

    .t-input__wrap:first-child {
      .t-input {
        border-right: none;
      }
    }

    .t-input__wrap:last-child {
      margin-right: 8px;

      .t-input {
        border-left: none;
      }
    }
  }

  :deep(.t-cascader) {
    width: 280px;
    height: 40px;
    margin-right: 8px;

    .t-input {
      height: 40px;
      border: 1px solid #2E3C8A !important;
      background: var(--app-border-color-dark);

      .t-input__prefix {
        .t-tag {
          background: #2741A8;
          color: #fff;
        }
      }
    }

    .t-input--focused {
      box-shadow: none;
    }
  }

  :deep(.choose-btn) {
    width: 102px;
    height: 40px;
    background: var(--app-ui-bg-color);
    border-radius: 4px 4px 4px 4px;
    border: 1px solid #2E3C8A;

    .t-button__text {
      display: flex;
      align-items: center;
      color: var(--app-text-color-normal);

      >div {
        margin-right: 4px;
      }
    }

    &:hover {
      background: linear-gradient(188deg, #2692DF 0%, #3252D2 100%) !important;

      .t-button__text {
        color: #fff;
      }
    }
  }

  .extentStation {
    width: 280px;
    height: 40px;
    line-height: 40px;
    border: 1px solid #2E3C8A !important;
    background: var(--app-border-color-dark);
    margin-right: 8px;
    border-radius: 4px;
    padding: 0 8px;
    white-space: nowrap;
    /* 不换行 */
    overflow: hidden;
    /* 超出隐藏 */
    text-overflow: ellipsis;
    /* 显示省略号 */
  }
}
</style>