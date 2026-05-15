<template>
  <div class="station-container pl-9 pr-9" v-show="visible">
    <div class="close" @click="close">
      <img src="@/assets/icon/close.png" alt="">
    </div>
    <t-tabs v-model="tabValue" class="transparent-tabs">
      <t-tab-panel value="station" label="站点" :destroyOnHide="false">
        <StationPanel ref="stationRef" :position="position" :positionId="positionId"></StationPanel>
      </t-tab-panel>
      <t-tab-panel value="grid" label="格点" :destroyOnHide="false">
        <GridPanel ref="gridRef" :position="position" :positionId="positionId"></GridPanel>
      </t-tab-panel>
    </t-tabs>
  </div>
</template>

<script setup>
import { ref, onBeforeUnmount, nextTick } from 'vue'
import StationPanel from './StationPanel.vue'
import GridPanel from './GridPanel.vue'
import { Vector as VectorLayer } from 'ol/layer';
import { Vector as VectorSource } from 'ol/source';
import GeoJSON from 'ol/format/GeoJSON'

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

let visible = ref(false)
const close = () => {
  visible.value = false
  stationRef && stationRef.value.clearChartData()
  gridRef && gridRef.value.clearChartData()
}

let tabValue = ref('station')
let position = ref('')
let positionId = ref(0)

let stationRef = ref(null)
let gridRef = ref(null)

watch(tabValue, (newval) => {
  if (visible.value) {
    if (newval === 'station') {
      nextTick(() => { stationRef && stationRef.value.search() })
    } else {
      nextTick(() => { gridRef && gridRef.value.search() })
    }
  }
})

$bus.on('openTable', f => {
  position.value = f[0].get('stationName') + f[0].get('stationIdD')
  positionId.value = f[0].get('stationIdD')
  if (tabValue.value === 'station') {
    nextTick(() => { stationRef && stationRef.value.search() })
  } else {
    nextTick(() => { gridRef && gridRef.value.search() })
  }
  if (!visible.value) {
    visible.value = true
  }
})

$bus.on('searchTable', obj => {
  position.value = obj.stationName + obj.stationIdD
  positionId.value = obj.stationIdD
  visible.value = true
})

onMounted(() => {
  addDistricLayer(districtList.value[0])
  addDistricLayer(districtList.value[1])
})

onBeforeUnmount(() => {
  changeDistrictLayer([])
  $bus.off('openTable')
  $bus.off('searchTable')
})

</script>

<style lang="scss" scoped>
.close {
  position: absolute;
  right: 2.0625rem;
  top: 1.5rem;
  cursor: pointer;
  z-index: 4;
}

.station-container {
  width: 40vw;
  height: 75vh;
  z-index: 3;
  position: absolute;
  top: 1.5rem;
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

:deep(.list) {
  height: 3.75rem;

  .t-col {
    display: flex;
    align-items: center;
    color: #fff;
    height: 100%;

    .position-div {
      width: 80%;
      height: 100%;
      border-radius: 0.375rem 0.375rem 0.375rem 0.375rem;
      border: 1px solid #2E3C8A;
      padding-left: 1.125rem;
      line-height: 3.75rem;
    }

    >span {
      white-space: nowrap;
      margin-right: 0.75rem;
    }

  }
}

:deep(.date-sel) {
  width: 47% !important;

  .t-input__prefix {
    margin-right: 0;
  }
}

:deep(.time-sel) {
  .t-input {
    padding-right: 5px;
  }
}

.arrow {
  width: 2.25rem;
  height: 3.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: #f0f0f0;
}

.left-arrow {
  background: url(../../../assets/icon/left-arrow.png) no-repeat 0 0;
}

.right-arrow {
  background: url(../../../assets/icon/right-arrow.png) no-repeat 0 0;
}

.arrow img {
  width: 1.875rem;
  height: 1.875rem;
}

:deep(.t-tabs) {
  .t-tabs__nav-scroll {
    height: 5.0625rem;
  }
}

:deep(.t-tabs__nav-item-text-wrapper) {
  font-size: 1.5rem;
}

:deep(.t-date-picker) {
  width: 40%;
  margin-right: 10px;

  .t-input {
    width: 100%;
    padding: 5px;
  }
}

:deep(.t-select__wrap) {
  width: 40%;

  .t-input {
    width: 100%;
    height: 3.75rem;
    background-color: transparent;
    border-radius: 0.375rem 0.375rem 0.375rem 0.375rem;
    border: 1px solid #2E3C8A;

    .t-fake-arrow {
      color: var(--app-text-color-purple);
    }
  }
}

:deep(.t-input) {
  background-color: transparent;
  border-radius: 0.375rem 0.375rem 0.375rem 0.375rem;
  border: 1px solid #2E3C8A;
  color: #fff;

  .t-input__inner {
    color: #fff;

  }
}


:deep(.t-input--focused) {
  box-shadow: none;
}
</style>