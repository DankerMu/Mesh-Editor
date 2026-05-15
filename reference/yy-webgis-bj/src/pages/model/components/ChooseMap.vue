<template>
  <div v-show="mapVisible"
    style="position: absolute;top: 0;left: 0;width: 100vw;height: 100vh;background-color: rgba(0, 0, 0, 0.68);z-index: 9999;">
    <div id="chooseMapContainer">
      <div class="footer">
        站点选择
        <div class="operation-list">
          <t-button type="reset" class="reset mr-5" @click="closeMap">
            <HiIcon class="svg" size="16px" :src="cancelSvg"></HiIcon>
            取消
          </t-button>
          <t-button theme="primary" class="search" @click="confirmMap">
            <HiIcon class="svg" size="16px" :src="okSvg"></HiIcon>
            确认
          </t-button>
        </div>
      </div>
    </div>
  </div>

</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import OlMap, { baseLayerList } from '@/components/OlMap/OlMap'
import TileLayer from 'ol/layer/Tile'
import XYZ from 'ol/source/XYZ'
import { Draw } from 'ol/interaction';
import { Vector as VectorLayer } from 'ol/layer';
import { Vector as VectorSource } from 'ol/source';
import { Fill, Stroke, Style, Circle } from 'ol/style';
import { Polygon, Point } from 'ol/geom';
import { toLonLat } from 'ol/proj';
import type { Feature } from 'ol';
import type { Geometry } from 'ol/geom';
import SimpleGeometry from 'ol/geom/SimpleGeometry';
import type { SketchCoordType, GeometryFunction } from 'ol/interaction/Draw';
import { Coordinate } from 'ol/coordinate';
import { containsCoordinate } from 'ol/extent';
import StationLayer from '@/layers/stationLayer'
import { DisplayService } from '@/api'
import { HiIcon } from "hoci";
import cancelSvg from '@/assets/icon/cancel.svg'
import okSvg from '@/assets/icon/ok.svg'

const props = defineProps({
  mapMode: {
    type: String,
    default: 'single'
  }
})

const emit = defineEmits(['confirmStation'])

let map: OlMap | null = null
let mapVisible = ref(false)
let mapSations: string[] = []
let stationLayer: any = null

const openMap = () => {
  clearMap()
  mapVisible.value = true
}

const clearMap = () => {
  mapSations = []
  position = {
    lon: '',
    lat: ''
  }
  stationLayer?.clearSingleStation()
  stationLayer?.clearMultiStation()
  source?.clear();
}

const closeMap = () => {
  mapVisible.value = false
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
    const res = await DisplayService.getStations()
    if (res && res.length > 0 && map) {
      // 创建站点图层并添加到地图
      stationLayer = new StationLayer(map.getMap(), res);
      map.getMap().addLayer(stationLayer);
    }
  } catch (error) { }
}
const removeStationLayer = () => {
  stationLayer && map?.getMap().removeLayer(stationLayer)
  stationLayer = null
}

const addToStationList = (ss: any) => {

}

const confirmMap = () => {
  if (props.mapMode === 'zone') {
    emit('confirmStation', rectangleStations)
  } else if (props.mapMode === 'nil') {
    emit('confirmStation', position)
  } else {
    emit('confirmStation', mapSations)
  }
  mapVisible.value = false
}

// 创建矢量图层用于绘制图形
const source = new VectorSource();
const vector = new VectorLayer({
  source: source,
  style: new Style({
    image: new Circle({
      radius: 6,
      fill: new Fill({ color: '#FF312A' }),
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
let rectangleStations: string[] = []; // 存储矩形内的站点
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
    const geometry = event.feature.getGeometry(); // Polygon
    if (!geometry) return
    const extent = geometry.getExtent();

    const features = stationLayer?.getSource().getFeatures();
    rectangleStations = features.filter((feature: Feature<Point>) => {
      const coord = feature.getGeometry()?.getCoordinates();
      return coord ? containsCoordinate(extent, coord) : false;
    });
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
    const geometry = event.feature.getGeometry(); // Point
    if (!geometry) return
    const coordinates = (geometry as Point).getCoordinates(); // 矩形四个点和起点重复，即5个点
    let lonlat = toLonLat(coordinates)

    position.lon = lonlat[0].toFixed(6)
    position.lat = lonlat[1].toFixed(6)
  });
}


watch(() => props.mapMode, (mode) => {
  if (map) {
    drawInteraction && map.getMap().removeInteraction(drawInteraction);
    if (mode === 'zone') {
      if (!stationLayer) addStationLayer()
      enableRectangleDraw()
    } else if (mode === 'single') {
      if (!stationLayer) addStationLayer()
      stationLayer?.setMapMode(0)
    } else if (mode === 'many') {
      if (!stationLayer) addStationLayer()
      stationLayer?.setMapMode(1)
    } else if (mode === 'nil') {
      removeStationLayer()
      enablePointClick()
    }
  }
}, {
  immediate: true
})

onMounted(() => {
  initMap()
  enablePointClick()
  window.$bus.on('openTable', (val: Feature<Geometry>[]) => {
    mapSations = val.map((s: Feature<Geometry>) => s.get('stationIdD'));
  });
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
  position: absolute;
  left: 50%;
  top: 10%;
  transform: translateX(-50%);
  width: 78vw;
  height: 78vh;
  // background-color: #fff;
  // border-radius: 8px;
  border: 2px solid rgba(71, 80, 126, 0.9);
  z-index: 4;

  .footer {
    position: absolute;
    bottom: 0;
    width: 100%;
    height: 10%;
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
}
</style>