<template>
  <div class="download-btn">
    <div @click="downloadWord" v-show="currentGroup !== 'all'&& isStationLayerShow">
      <img :src="downloadFilePng" alt="" title="导出预报单">
    </div>
    <div @click="() => updateVisible = true">
      <img :src="downloadGridPng" alt="" title="下载图片">
    </div>
  </div>
  <div class="right-top">
    <div class="map">
      <img src="@/assets/icon/map-active.png" alt="">
      <t-select v-model="currentLayer" :options="layerList" placeholder=""
        :popupProps="{ overlayClassName: 'gradient-select-popup' }" @change="changeBaseLayer">
        <template #valueDisplay>
          地图
        </template>
      </t-select>
    </div>

    <div class="map">
      <img :src="currentDistrict.length > 0 ? districtActiveImg : districtImg" alt="">
      <t-select v-model="currentDistrict" class="multi-sel" :options="districtList" placeholder="" multiple
        :popupProps="{ overlayClassName: 'gradient-select-popup' }" @change="changeDistrictLayer">
        <template #valueDisplay>
          <span
            :style="{ 'color': currentDistrict.length > 0 ? 'var(--app-text-color-normal) !important' : 'var(--app-text-color-purple) !important' }">行政</span>
        </template>
      </t-select>
    </div>
    <!-- @click.prevent="switchStationLayer" -->
    <div class="map" :class="{ active: isStationLayerShow }">
      <img :src="isStationLayerShow ? stationActiveImg : stationImg" alt="" @click="changeStationLayerStatus">
      <!-- <div>站点</div> -->
      <t-select v-model="currentGroup" :options="groupList" placeholder=""
        :popupProps="{ overlayClassName: 'gradient-select-popup' }" @change="changeGroup">
        <template #valueDisplay>
          站点
        </template>
      </t-select>
    </div>

    <!-- <div class="map" :class="{ active: isGridLayerShow }" @click="switchGridLayer()">
      <img :src="isGridLayerShow ? gridActiveImg : gridImg" alt="">
      <div>格点</div>
    </div> -->

    <div class="map" :class="{ active: isGridLayerShow }">
      <img :src="isGridLayerShow ? gridActiveImg : gridImg" alt="" @click="switchGridLayer()">
      <t-select v-model="currentSource" :options="sourceList" placeholder=""
        :popupProps="{ overlayClassName: 'gradient-select-popup' }" @change="changeGridSource">
        <template #valueDisplay>
          格点
        </template>
      </t-select>
    </div>
    <!-- 下载图片的标题和子标题 -->
    <dialog-vue :visible="updateVisible" header="下载图片" :customClass="'delete-dialog custom-dialog-footer'"
      :close="updateClose">
      <template #content>
        <div class="form-color-ipt mb-[20px]">
          <t-input v-model="title" clearable placeholder="请输入标题"></t-input>
        </div>
        <div class="form-color-ipt">
          <t-input v-model="subTitle" clearable placeholder="请输入子标题"></t-input>
        </div>
      </template>
      <template #button>
        <div class="btn-box">
          <t-button class="cancel-btn" @click="updateClose">取消</t-button>
          <t-button class="delete-btn" @click="updateConfirm">确认</t-button>
        </div>
      </template>
    </dialog-vue>
  </div>
</template>

<script setup>
import { onBeforeUnmount, ref, onMounted } from 'vue'
import { baseLayerList } from '@/components/OlMap/OlMap'
import downloadGridPng from '/image/downloadgrid.png'
import downloadFilePng from '/image/downloadfile.png'
import stationImg from '@/assets/icon/station.png'
import stationActiveImg from '@/assets/icon/station-active.png'
import gridImg from '@/assets/icon/grid.png'
import gridActiveImg from '@/assets/icon/grid-active.png'
import districtImg from '@/assets/icon/district.png'
import districtActiveImg from '@/assets/icon/district-active.png'
import StationLayer from '@/layers/stationLayer'
import GridLayer from '@/layers/gridLayer'
import { DisplayService, EstimateService } from '@/api'
import dayjs from 'dayjs'
import { Vector as VectorLayer } from 'ol/layer';
import { Vector as VectorSource } from 'ol/source';
import GeoJSON from 'ol/format/GeoJSON'
import { useTimelineStore } from '@/stores'
import { MessagePlugin } from 'tdesign-vue-next'
import { ModelService } from '@/api'
import DialogVue from '@/components/DialogVue.vue'
const tmStore = useTimelineStore()
let currentId = -1
let updateVisible = ref(false)

const updateClose = () => {
  currentId = -1
  updateVisible.value = false
}

const props = defineProps({
  startTime: {
    type: dayjs,
    required: true
  },
  currentPoint: {
    type: dayjs,
    default: '',
    required: true
  },
  currentIndex: {
    type: Number,
    required: true
  }
})

const title = ref('战区未来24小时天气趋势预报')
const subTitle = ref('')
subTitle.value = `${props.startTime.format('MM月DD日20时')}～${props.startTime.add(1, 'day').format('DD日20时')}`

let currentLayer = ref('topography')
let layerList = baseLayerList.map(item => {
  return {
    value: item.key,
    label: item.title
  }
})
const updateConfirm = async () => {
  if (!title.value) {
    MessagePlugin.error('请输入标题')
    return
  }
  if (!subTitle.value) {
    MessagePlugin.error('请输入子标题')
    return
  }
  updateVisible.value = false
  downloadGrid()
}

let provinceLayer_empty = null
let captitalLayer_empty = null

let changeBaseLayer = (val) => {
  olMap.switchBaseLayer(val)

  if (val === 'empty') {
    captitalLayer_empty = new VectorLayer({
      name: 'captitalLayer_empty',
      source: new VectorSource({
        url: `/data/capital.geojson`,
        format: new GeoJSON()
      }),
      style: {
        'circle-fill-color': '#000',
        'text-font': '16px sans-serif',
        'circle-radius': 3,
        'text-value': ['get', 'name'],
        'text-offset-y': -15,
        // 'text-stroke-color': 'rgb(255,255,255,0.8)',
        // 'text-stroke-color': '#000',
        'text-stroke-width': 0.5,
        // 'text-scale': 1.5
      },
      zIndex: 5
    })
    olMap.getMap().addLayer(captitalLayer_empty)

    provinceLayer_empty = new VectorLayer({
      name: 'provinceLayer_empty',
      source: new VectorSource({
        url: `/data/province.geojson`,
        format: new GeoJSON()
      }),
      style: {
        'stroke-color': '#474547',
        'stroke-width': 2
      },
      zIndex: 3
    })
    olMap.getMap().addLayer(provinceLayer_empty)

  } else {
    captitalLayer_empty && olMap.getMap().removeLayer(captitalLayer_empty)
    captitalLayer_empty = null

    provinceLayer_empty && olMap.getMap().removeLayer(provinceLayer_empty)
    provinceLayer_empty = null
  }

}

let currentDistrict = ref(['country', 'province'])
let districtList = ref([
  {
    label: '国界',
    value: 'country',
    color: '#ba67bc',
    width: 3,
    visible: true,
    zIndex: 4
  },
  {
    label: '省界',
    value: 'province',
    color: '#474547',
    width: 2,
    visible: false,
    zIndex: 3
  },
  {
    label: '市界',
    value: 'city',
    color: '#757171',
    width: 1,
    visible: false,
    zIndex: 2
  }
])

const changeDistrictLayer = (val) => {
  // districtList.value.forEach(obj => {
  //   let layer = olMap.getMap().getAllLayers().find(l => l.get('name') === (obj.value + '_layer'))
  //   if (val.includes(obj.value)) {
  //     if (layer) {
  //     layer.setVisible(true)
  //     } else {
  //     addDistricLayer(obj)
  //     }
  //   } else {
  //     if (layer) {
  //       layer.setVisible(false)
  //     }
  //   }
  // })

  districtList.value.forEach((obj) => {
    let layer = olMap.getMap().getAllLayers().find(l => l.get('name') === (obj.value + '_layer'))
    if (val.includes(obj.value)) {
      if (!layer) {
        addDistricLayer(obj)
      }
    } else {
      if (layer) {
        olMap.getMap().removeLayer(layer)
      }
    }
  })

}


const addDistricLayer = (obj) => {
  const vectorLayer = new VectorLayer({
    name: obj.value + '_layer',
    source: new VectorSource({
      url: `/data/${obj.value}.geojson`,
      format: new GeoJSON()
    }),
    style: {
      'stroke-color': obj.color,
      'stroke-width': obj.width
    },
    zIndex: obj.zIndex
  })

  olMap.getMap().addLayer(vectorLayer);
}

let isStationLayerShow = ref(true)
const switchStationLayer = () => {
  isStationLayerShow.value = !isStationLayerShow.value
  if (differentTypeStationLayer) {
    removeStationLayer()
  } else {
    addStationLayer()
  }
};

let isGridLayerShow = ref(true)
const switchGridLayer = () => {
  isGridLayerShow.value = !isGridLayerShow.value
  if (!isGridLayerShow.value) {
    removeGridLayer()
  } else {
    addGridLayer()
  }
};

let currentSource = ref('deep')
let currentGroup = ref('all')
let lastGroup = ref('all')
let currentSourceName = ref('')
const groupList = ref([
  {
    label: '全部站点',
    value: 'all',
    id: '1'
  },
])
let sourceList = ref([
  // {
  //   label: '智能订正',
  //   value: 'deep',
  //   id: '1'
  // },
  // {
  //   label: 'ECMWF订正',
  //   value: 'ecmf',
  //   id: '2'
  // },
  // {
  //   label: 'GRAPES_GFS订正',
  //   value: 'grapes',
  //   id: '3'
  // },
])


const getGridModelAll = async () => {
  const res = await ModelService.queryGribModelAll()
  // console.log(res);
  sourceList.value = res.map(item => {

    return {
      label: item.model,
      value: item.dataType,
      id: item.id
    }
  })
}
getGridModelAll()
const getGroupAll = async () => {
  const res = await ModelService.queryTaskList()
  // console.log(res);

  groupList.value.push(...res.map(item => {
    return {
      label: item.taskName,
      value: item.id,
      id: item.id
    }
  }))
  // groupList.value.appendChild({
  //   value: 'all',
  //   label: '全部'
  // })
}
getGroupAll()
function changeStationLayerStatus() {
  isStationLayerShow.value = !isStationLayerShow.value
  if (differentTypeStationLayer) {
    removeStationLayer()
  } else {
    if (currentGroup.value === 'all') {
      // switchStationLayer()
      addStationLayer()
    } else {
      changeGroupLayer()
    }
  }
}
function changeGroup(e) {
  if (!isStationLayerShow.value) {
    return
  }
  console.log(e, 'e')
  if (e === 'all') {
    // switchStationLayer()
    addStationLayer()
  } else {
    changeGroupLayer()
  }
  lastGroup.value = e
}

const changeGroupLayer = async () => {
  removeStationLayer()
  // if (stationLayer) return;
  try {
    const userInfo = JSON.parse(sessionStorage.getItem("userInfo") ?? '{}')
    const uname = userInfo?.username ?? ""
    let id = groupList.value.find(x => x.value == currentGroup.value)?.id
    console.log('id', id, currentGroup.value, groupList.value)
    const res = await ModelService.queryTaskStations({
      taskId: id,
      // author: uname
    })

    if (res && res.length > 0 && olMap) {
      // 创建站点图层并添加到地图
      differentTypeStationLayer = new StationLayer(olMap.getMap(), res);
      olMap.getMap().addLayer(differentTypeStationLayer);
      MessagePlugin.success('切换成功')
    }
  } catch (error) {
    MessagePlugin.error('切换失败')
  }
}
const changeGridSource = async () => {
  if (!isGridLayerShow.value) {
    return
  }
  removeGridLayer()
  addGridLayer()
  // try {
  //   const userInfo = JSON.parse(sessionStorage.getItem("userInfo") ?? '{}')
  //   const uname = userInfo?.username ?? ""
  //   let id = sourceList.value.find(x => x.value == currentSource.value)?.id
  //   const res = await ModelService.stationModelReplace({
  //     id: id,
  //     author: uname
  //   })
  //   if (res > 0) {
  //     MessagePlugin.success('切换成功')
  //   } else {
  //     MessagePlugin.error('切换失败')
  //   }
  // } catch (error) {
  //   MessagePlugin.error('切换失败')
  // }
}
let stationLayer = null
let differentTypeStationLayer = null
let gridLayer = null
let imgList = []

const addStationLayer = async () => {
  removeStationLayer()
  // if (stationLayer) return;
  try {
    // const res = await DisplayService.getStations()
    // if (res && res.length > 0 && olMap) {
    //   // 创建站点图层并添加到地图
    //   stationLayer = new StationLayer(olMap.getMap(), res, 'station');
    //   olMap.getMap().addLayer(stationLayer);
    // }
    const res1 = await DisplayService.getDifferentStations()
    if (res1 && res1.length > 0 && olMap) {
      // 创建站点图层并添加到地图
      differentTypeStationLayer = new StationLayer(olMap.getMap(), res1);
      olMap.getMap().addLayer(differentTypeStationLayer);
    }
  } catch (error) { }
}
const removeStationLayer = () => {
  // stationLayer && olMap.getMap().removeLayer(stationLayer)
  // stationLayer = null

  differentTypeStationLayer && olMap.getMap().removeLayer(differentTypeStationLayer)
  differentTypeStationLayer = null
}

const getDefaultMode = async () => {

  const { dataSource, dataSourceName } = await EstimateService.queryGribUsedModel()
  currentSource.value = dataSource
  currentSourceName.value = dataSourceName
}
// getDefaultMode()
const addGridLayer = async () => {
  imgList = []
  try {
    // const { dataSource, dataSourceName } = await EstimateService.queryGribUsedModel()
    // currentSource.value = dataSource
    // currentSourceName.value = dataSourceName
    let params = {
      dataTime: props.startTime.format('YYYY-MM-DD HH:mm:ss'),
      dataSource: currentSource.value,
      vtis: tmStore.labelList,
      rainVti: tmStore.validTime
    }
    const res = await DisplayService.getGridData(params)
    if (res && res.length > 0) {
      imgList = res.map(item => '/cdsz' + item.urlPath)
      gridLayer = new GridLayer(olMap.getMap(), imgList[props.currentIndex])
      olMap.getMap().addLayer(gridLayer);
    }
  } catch (error) { }
}

const removeGridLayer = () => {
  gridLayer && olMap.getMap().removeLayer(gridLayer)
  gridLayer = null
}

const downloadGrid = async () => {
  try {
    const res = await DisplayService.downloadPng({
      url: imgList[props.currentIndex],
      title: title.value,
      subTitle: subTitle.value
    })
    if (res) {
      const blob = new Blob([res], { type: "image/png;charset=utf-8;" });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", `${props.startTime.format('YYYY-MM-DD HH')} ${tmStore.labelList[props.currentIndex]} ${currentSourceName.value}`);
      document.body.appendChild(link);
      link.click();
      // 释放资源
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    }

  } catch (error) {

  }
}

const downloadWord = async () => {
  try {
    const res = await DisplayService.downloadWord({
      dateTime: props.startTime.format('YYYY-MM-DD HH:00:00'),
      taskId: currentGroup.value
    })
    if (res) {
      const blob = new Blob([res], { type: "application/vnd.openxmlformats-officedocument.wordprocessingml.document" });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", `${props.startTime.format('YYYY-MM-DD HH')}`);
      document.body.appendChild(link);
      link.click();
      // 释放资源
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    }

  } catch (error) { }
}

$bus.on('updateStaionLayer', (n) => {
  stationLayer?.updateSource(n)
})

$bus.on('updateCustomStaionLayer', (n) => {
  differentTypeStationLayer?.updateSource(n)
})

watch(() => props.startTime, (newVal) => {
  if (isGridLayerShow.value) {
    removeGridLayer()
    addGridLayer()
  }
})

const validTime = computed(() => tmStore.validTime)
watch(validTime, (val) => {
  if (val) {
    if (isGridLayerShow.value) {
      removeGridLayer()
      addGridLayer()
    }
  }
})


watch(() => props.currentIndex, (newVal) => {
  gridLayer?.updateSource(imgList[props.currentIndex])
}, {
  immediate: true
})

onMounted(async() => {
  addStationLayer()
  addDistricLayer(districtList.value[0])
  addDistricLayer(districtList.value[1])
  await getDefaultMode()
  addGridLayer()
})

onBeforeUnmount(() => {
  tmStore.setValidTime('24')
  removeStationLayer()
  removeGridLayer()
  changeBaseLayer('topography')
  changeDistrictLayer([])
})


</script>
<style>
.gradient-select-popup {
  width: 142px;
}
</style>
<style lang="less" scoped>
.right-top {
  position: absolute;
  z-index: 3;
  top: 1.5rem;
  right: 1.5rem;
  // width: 19.875rem;
  height: 4.5rem;
  background: linear-gradient(225deg, rgba(27, 44, 124, 0.98) 0%, rgba(21, 31, 96, 0.98) 100%);
  border-radius: 0.75rem 0.75rem 0.75rem 0.75rem;
  border: 1px solid rgba(71, 80, 126, 0.9);
  opacity: 0.98;

  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;

  .map {
    height: 2.25rem;
    width: 90px;
    color: rgba(188, 200, 255, 1);

    display: flex;
    align-items: center;
    justify-content: center;

    cursor: pointer;

    >img {
      width: 22px;
      height: 22px;
      margin-right: 7px;
    }

    div:nth-child(2) {
      font-size: 1.5rem;
    }

    :deep(.t-select__wrap) {
      width: 53px;

      .t-input {
        // width: 4.5rem;
        width: 53px;
        padding: 0;
        background-color: transparent;
        border: none;
        font-size: 16px;

        .t-input__prefix {
          color: var(--app-text-color-normal) !important;
        }

        .t-input__inner {
          font-size: 16px;
        }

        .t-input__prefix:not(:empty),
        .t-input__suffix:not(:empty) {
          margin: 0;
        }

        .t-fake-arrow {
          color: var(--app-text-color-purple);
        }
      }

      .t-input--focused {
        box-shadow: none;
      }

    }


    :deep(.multi-sel) {
      width: 54px;

      .t-input {
        // width: 4.5rem;
        display: flex;
        width: 54px;
        padding: 0;
        background-color: transparent;
        border: none;
        font-size: 16px;

        .t-input__prefix {
          color: var(--app-text-color-normal) !important;
        }

        .t-input__inner {
          font-size: 16px;
        }

        .t-input__prefix:not(:empty),
        .t-input__suffix:not(:empty) {
          margin: 0;
        }

        .t-input__suffix-icon {
          right: 0;
        }

        .t-fake-arrow {
          color: var(--app-text-color-purple);
        }
      }

      .t-input--focused {
        box-shadow: none;
      }
    }

    :deep(.t-tag-input .t-input--focused .t-input__inner:not(.t-input--soft-hidden)) {
      min-width: 0 !important;
    }

    :deep(.t-tag-input:hover .t-input__inner:not(.t-input--soft-hidden)) {
      min-width: 0 !important;
    }
  }

  .map.active {
    color: var(--app-text-color-normal);
  }
}


.download-btn {
  position: absolute;
  z-index: 3;
  top: 1.5rem;
  right: 40rem;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;

  >div {
    width: 4.5rem;
    height: 4.5rem;
    display: flex;
    justify-content: center;
    align-items: center;
    background: linear-gradient(225deg, rgba(27, 44, 124, 0.9604) 0%, rgba(21, 31, 96, 0.9604) 100%);
    border-radius: 0.75rem 0.75rem 0.75rem 0.75rem;
    border: 1px solid rgba(71, 80, 126, 0.9);
    cursor: pointer;

    img {
      width: 24px;
      height: 24px;
    }
  }
}
</style>