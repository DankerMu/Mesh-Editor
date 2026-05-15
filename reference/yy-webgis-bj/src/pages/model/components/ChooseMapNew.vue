<template>
  <div style="position:absolute;top: 10px;right: 0;width: 72vw;height: 77vh;">
    <div id="chooseMapContainer">
    </div>

    <div class="bun-group" v-show="mapMode === 'station'">
      <div class="btn point" @click="changeCurTool(0)" title="选点">
        <!-- <img src="@/assets/icon/littlemap-position.svg" alt=""> -->
        <HiIcon :class="{ active: curTool === 0 }" class="icon" size="24px" :src="positionSvg"
          style="color: var(--app-text-color-purple)"></HiIcon>
      </div>
      <div class="btn rectangle" @click="changeCurTool(1)" title="框选矩形">
        <!-- <img src="@/assets/icon/littlemap-extent.svg" alt=""> -->
        <HiIcon :class="{ active: curTool === 1 }" size="24px" :src="recSvg"
          style="color: var(--app-text-color-purple)"></HiIcon>
      </div>
    </div>


    <!-- <div class="footer">
      站点选择
      <div class="operation-list">
        <t-button type="reset" class="reset mr-5" @click="resetStation">
          <img src="@/assets/icon/reset.png" alt="">
          重置
        </t-button>
        <t-button theme="primary" class="search" @click="confirmMap" style="width: 92px;">
          <HiIcon class="svg" size="16px" :src="okSvg"></HiIcon>
          完成选择
        </t-button>
      </div>
    </div> -->

    <SearchStation v-show="mapMode === 'station'" :curTool="curTool" @searchChange="searchChange"></SearchStation>
  </div>

  <Tooltip ref="toolRef"></Tooltip>

</template>

<script setup lang="ts">
import { onMounted, watch, ref } from 'vue';
import Tooltip from './overlay.vue'
import SearchStation from './SearchStationforNew.vue';
import OlMap, { baseLayerList } from '@/components/OlMap/OlMap'
import TileLayer from 'ol/layer/Tile'
import XYZ from 'ol/source/XYZ'
import { Draw } from 'ol/interaction';
import { Vector as VectorLayer } from 'ol/layer';
import { Vector, Vector as VectorSource } from 'ol/source';
import { Fill, Stroke, Style, Circle } from 'ol/style';
import { Polygon, Point } from 'ol/geom';
import { toLonLat } from 'ol/proj';
import type { Feature } from 'ol';
import type { Geometry } from 'ol/geom';
import SimpleGeometry from 'ol/geom/SimpleGeometry';
import type { SketchCoordType, GeometryFunction } from 'ol/interaction/Draw';
import { Coordinate } from 'ol/coordinate';
import { containsCoordinate } from 'ol/extent';
// import StationLayer from '@/layers/stationLayer'
import WebGLPointsStationLayer from '@/layers/webglStationLayer'
import { ModelService } from '@/api'
import { HiIcon } from "hoci";
import cancelSvg from '@/assets/icon/cancel.svg'
import okSvg from '@/assets/icon/ok.svg'
import positionSvg from '@/assets/icon/littlemap-position.svg'
import recSvg from '@/assets/icon/littlemap-extent.svg'
import resetSvg from '@/assets/icon/replace.svg'
import GeoJSON from 'ol/format/GeoJSON'

const props = defineProps({
  mapMode: {
    type: String,
    default: 'nil'
  }
})
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
const emit = defineEmits(['confirmStation'])
let country_layer = null
let province_layer = null
const addDistricLayer = (obj) => {
  
  try{
    const vectorLayer = new VectorLayer({
      // name: '_layer',
      // name: obj.value + '_layer',
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
    map.getMap().addLayer(vectorLayer);
  } catch(e) {
    console.log('e', e)
  }
}
let map: OlMap | null = null
// let mapVisible = ref(false)
let mapSations: string[] = []
let stationLayer: any = null
let curTool = ref(-1) // 0 选点,1 选矩形

const openMap = () => {
  clearMap()
  // mapVisible.value = true
}

const clearMap = () => {
  source?.clear();

  if (props.mapMode === 'station') {
    mapSations = []
    rectangleStations = []

    // stationLayer?.clearSingleStation()
    // stationLayer?.clearMultiStation()
    removeStationLayer()
    addStationLayer()
    emit('confirmStation', [])
  } else {
    position = {
      lon: '',
      lat: ''
    }
    emit('confirmStation', position)
  }
}

// const closeMap = () => {
//   mapVisible.value = false
// }

const resetStation = () => {
  if (curTool.value === 0) {
    stationLayer && stationLayer.clearMultiStation()
    mapSations = []
  } else {
    rectangleStations = []
    source?.clear();
  }
}

const initMap = () => {
  if (!map) {
    map = new OlMap(
      'chooseMapContainer',
      {
        layers: [],
        center: [90, 35.5],
        zoom: 5,
        minZoom: 2,
        maxZoom: 17
      }
    )
    initBaseLayer()

    map.addLayer(vector)

    toolRef.value?.install(map.getMap())
  }
}

const initBaseLayer = () => {
  baseLayerList.forEach(item => {
    map?.addBaseLayer(item.key, new TileLayer({
      source: new XYZ({
        url: item.url
      })
    }))
  })
}

const addStationLayer = async () => {
  try {
    const res = await ModelService.getNoModelStation()
    if (res && res.length > 0 && map) {
      // 创建站点图层并添加到地图
      // stationLayer = new StationLayer(map.getMap(), res, 'noModelStation');
      stationLayer = new WebGLPointsStationLayer(map.getMap(), res);
      map.getMap().addLayer(stationLayer);
    }
  } catch (error) { }
}
const removeStationLayer = () => {
  stationLayer && map?.getMap().removeLayer(stationLayer)
  stationLayer = null
}


// const confirmMap = () => {
//   if (props.mapMode === 'nil') {
//     emit('confirmStation', position)
//   } else if (props.mapMode === 'station') {
//     if (curTool.value === 0) {
//       emit('confirmStation', mapSations)
//     } else {
//       emit('confirmStation', rectangleStations.map((s: Feature<Geometry>) => s.get('stationIdD')))
//     }
//   }
//   // mapVisible.value = false
// }

// 创建矢量图层用于绘制图形
const source = new VectorSource();
const vector = new VectorLayer({
  source: source,
  style: new Style({
    image: new Circle({
      radius: 6,
      fill: new Fill({ color: 'rgb(0, 153, 255)' }),
      stroke: new Stroke({ color: '#fff', width: 1 }),
    }),
    stroke: new Stroke({
      color: 'blue',
      width: 2,
    }),
    fill: new Fill({
      color: 'rgba(0, 0, 255, 0.1)',
    }),
  }),
});

let drawInteraction: Draw | null = null
let position = {
  lon: '',
  lat: ''
}
let rectangleStations: Feature<Geometry>[] = []; // 存储矩形内的站点
const enableRectangleDraw = () => {
  drawInteraction = null
  drawInteraction = new Draw({
    source: source,
    type: 'Circle',
    geometryFunction: createBoxGeometryFunction,
  });
  map?.getMap().addInteraction(drawInteraction);


  // 每次绘制开始前清空旧图形
  drawInteraction.on('drawstart', () => {
    source.clear();
  });
  // 每次绘制完成后处理坐标
  drawInteraction.on('drawend', (event) => {
    rectangleStations = []

    const geometry = event.feature.getGeometry(); // Polygon
    if (!geometry) return
    const extent = geometry.getExtent();

    const features = stationLayer?.getSource().getFeatures();
    features.filter((feature: Feature<Point>) => {
      rectangleStations.push(...feature.get('features').filter((f: Feature<Point>) => {
        const coord = f.getGeometry()?.getCoordinates();
        return coord ? containsCoordinate(extent, coord) : false;
      }))
    });
    emit('confirmStation', rectangleStations.map((s: Feature<Geometry>) => s.get('stationIdD')))
  });
}

const createBoxGeometryFunction: GeometryFunction = (coordinates: SketchCoordType, geometry) => {
  const [start, end] = coordinates as [number[], number[]];
  const coords = [
    start,
    [start[0], end[1]],
    end,
    [end[0], start[1]],
    start,
  ];
  if (!geometry) {
    geometry = new Polygon([coords]);
  } else {
    geometry.setCoordinates([coords]);
  }
  return geometry;
}

const enablePointClick = () => {
  drawInteraction = null
  drawInteraction = new Draw({
    source: source,
    type: 'Point',
  });
  map?.getMap().addInteraction(drawInteraction);

  // 每次绘制开始前清空旧图形
  drawInteraction.on('drawstart', () => {
    source.clear();
  });
  // 每次绘制完成后处理坐标
  drawInteraction.on('drawend', (event) => {
    // debugger
    const geometry = event.feature.getGeometry(); // Point
    if (!geometry) return
    const coordinates = (geometry as Point).getCoordinates(); // 矩形四个点和起点重复，即5个点
    let lonlat = toLonLat(coordinates)

    position.lon = lonlat[0].toFixed(6)
    position.lat = lonlat[1].toFixed(6)
    emit('confirmStation', position)
  });
}

const changeCurTool = (type: number) => {
  if (props.mapMode === 'nil') return
  if (type === 0) {
    curTool.value = 0
    source.clear();
    map && drawInteraction && map.getMap().removeInteraction(drawInteraction);
    stationLayer && stationLayer.setCanMapChoose(true)
  } else if (type === 1) {
    curTool.value = 1
    map && drawInteraction && map.getMap().removeInteraction(drawInteraction);
    enableRectangleDraw()
    stationLayer && stationLayer.setCanMapChoose(false)
    stationLayer && stationLayer.clearMultiStation()
  }
}

const searchChange = (id: any) => {
  if (stationLayer) {
    // const feature = stationLayer.getSource().getFeatures().find(f => f.get('stationIdD') === id);
    // if (feature) {
    stationLayer?.updateSource(id)
    // const geometry = feature.getGeometry()
    // map?.getMap().getView().fit(geometry, {
    //   maxZoom: 9
    // })
    // }
  }
}

const toolRef = ref(null)

watch(() => props.mapMode, (mode) => {
  if (map) {
    drawInteraction && map.getMap().removeInteraction(drawInteraction);
    source?.clear()
    if (mode === 'station') {
      if (!stationLayer) addStationLayer()
      changeCurTool(0)
    } else if (mode === 'nil') {
      removeStationLayer()
      enablePointClick()
      curTool.value = -1
    }
  }
}, {
  immediate: true
})

onMounted(() => {
  initMap()
  enablePointClick()
  window.$bus.on('openModelTable', (val: Feature<Geometry>[]) => {
    mapSations = val.map((s: Feature<Geometry>) => s.get('stationIdD'));
    emit('confirmStation', mapSations)
  });
  console.log('districtList', districtList.value)
  addDistricLayer(districtList.value[0])
  addDistricLayer(districtList.value[1])
})

onBeforeUnmount(() => {
  console.log('地图手动选择unmounted');
})




defineExpose({
  openMap
})
</script>

<style lang="less" scoped>
#chooseMapContainer {
  width: 100%;
  height: 100%;
  // background-color: #fff;
  // border-radius: 8px;
  border: 2px solid rgba(71, 80, 126, 0.9);
  z-index: 4;
}


.bun-group {
  position: absolute;
  top: 1.5rem;
  left: 256px;
  background-color: rgba(27, 44, 124, 0.98);
  height: 42px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  padding: 0 6px;


  .btn {
    width: 32px;
    height: 32px;
    cursor: pointer;
    border-radius: 4px;
    // box-shadow: 0px 3px 6px 0px rgba(19, 29, 80, 0.4);
    padding: 4px;

    img {
      width: 24px;
      height: 24px;
    }
  }

  .point {
    // background: url('../../../assets/icon/littlemap-position.svg') no-repeat;
    // background-size: 85%;
    // background-position: center;
    margin-right: 8px;
  }

  .rectangle {
    // background: url('../../../assets/icon/littlemap-extent.svg') no-repeat;
  }

  .active {
    // background-color: rgba(0, 0, 0, 0.1);
    // border: 1px solid var(--app-text-color-normal);
    // background-color: #1ea9ff3d;
    // background-color: rgba(27, 44, 124, 0.98);
    color: var(--app-text-color-normal) !important;
  }

}

.footer {
  position: absolute;
  bottom: 0;
  width: 100%;
  height: 8%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  // border-radius: 0 0 8px 8px;
  background: linear-gradient(225deg, rgba(27, 44, 124, 0.84) 0%, rgba(21, 31, 96, 0.84) 100%);
  font-size: 16px;
  font-family: var(--app-text-family-bold);
  z-index: 1;
}
</style>