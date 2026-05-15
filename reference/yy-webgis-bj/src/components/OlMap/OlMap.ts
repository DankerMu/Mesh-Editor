import Map from "ol/Map"
import TileLayer from "ol/layer/Tile"
// import OSM from "ol/source/OSM"
import View from "ol/View"
import { defaults as defaultControls } from 'ol/control'
import BaseLayer from 'ol/layer/Base'
import { fromLonLat } from 'ol/proj'
import { Coordinate } from "ol/coordinate"
import XYZ from 'ol/source/XYZ'
import electronicImg from '@/assets/icon/elec.png'
import photomapImg from '@/assets/icon/img.png'

interface OlMapOptions {
  center: Coordinate;
  zoom?: number;
  minZoom?: number;
  maxZoom?: number;
  projection?: string;
  layers?: BaseLayer[];
  extent?: number[];
}

const DEFAULT_OPTIONS: OlMapOptions = {
  center: [0, 0],
  zoom: 2,
  minZoom: 0,
  maxZoom: 20,
  projection: 'EPSG:3857'
}

export const baseLayerList = [
  // {
  //   title: '黑图',
  //   key: 'darkmap',
  //   iconUrl: electronicImg,
  //   url: 'http://192.168.2.46:19998/maps/darkmap/{z}/{x}/{y}.png',
  // },
  // {
  //   title: '灰底',
  //   key: 'grayland',
  //   iconUrl: electronicImg,
  //   url: '/maps/grayland/{z}/{x}/{y}.png',
  // },
  // {
  //   title: '卫星',
  //   key: 'photomap',
  //   iconUrl: photomapImg,
  //   // url: '/maps/fmap/{z}/{x}/{y}.png',
  //   url: 'http://webst0{1-4}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&style=6&x={x}&y={y}&z={z}',
  // },
  {
    title: '卫星地形图',
    key: 'topography',
    iconUrl: photomapImg,
    // url: '/maps/fmap/{z}/{x}/{y}.png',
    // url: 'http://webst0{1-4}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&style=6&x={x}&y={y}&z={z}',
    url: 'http://192.168.3.13:8091/server/map/google-terrain/{z}/{x}/{y}'
  },
  {
    title: '卫星影像图',
    key: 'satellite',
    iconUrl: electronicImg,
    url: 'http://192.168.3.13:8091/server/map/google-satellite/{z}/{x}/{y}',
  },
  {
    title: '行政地形图',
    key: 'google',
    iconUrl: electronicImg,
    url: 'http://192.168.3.13:8091/server/map/google-map/{z}/{x}/{y}',
  },
  {
    title: '空白地图',
    key: 'empty',
    iconUrl: electronicImg,
    url: ''
  }
  // {
  //   title: '矢量',
  //   key: 'electronic',
  //   iconUrl: electronicImg,
  //   url: 'http://wprd0{1-4}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&style=10&x={x}&y={y}&z={z}&ltype=11',
  // },
]

export default class OlMap {
  private map: Map
  private options: OlMapOptions
  private baseLayers: { [key: string]: TileLayer }
  private currentBaseLayer: string | null = null

  constructor(target: HTMLElement | string, options: Partial<OlMapOptions> = {}) {
    this.options = { ...DEFAULT_OPTIONS, ...options }
    this.map = this.createMap(target)
    this.baseLayers = {}
  }

  private createMap(target: HTMLElement | string): Map {

    var gaodeLayer = new TileLayer({
      source: new XYZ({
        url: 'https://webst0{1-4}.is.autonavi.com/appmaptile?style=6&x={x}&y={y}&z={z}'
      })
    });

    // const baseLayer = new TileLayer({
    //   source: new OSM()
    // })

    const layers = this.options.layers || [gaodeLayer]

    return new Map({
      target,
      layers,
      controls: defaultControls({
        attribution: false,
        zoom: false
      }),
      view: new View({
        center: fromLonLat(this.options.center),
        zoom: this.options.zoom,
        minZoom: this.options.minZoom,
        maxZoom: this.options.maxZoom,
        projection: this.options.projection,
        extent: this.options.extent
      })
    })
  }

  // 获取地图实例
  getMap(): Map {
    return this.map
  }

  // 获取视图
  getView(): View {
    return this.map.getView()
  }

  // 添加图层
  addLayer(layer: BaseLayer): void {
    this.map.addLayer(layer)
  }

  // 移除图层
  removeLayer(layer: BaseLayer): void {
    this.map.removeLayer(layer)
  }

  // 设置中心点
  setCenter(center: number[]): void {
    this.getView().setCenter(center)
  }

  // 设置缩放级别
  setZoom(zoom: number): void {
    this.getView().setZoom(zoom)
  }

  // getZoom
  getZoom(): number | undefined {
    return this.getView().getZoom()
  }

  // zoomOut
  zoomOut(): void {
    const zoom = this.getView().getZoom();
    if (zoom === 0) return;

    if (zoom) {
      this.getView().setZoom(zoom - 1);
    }
  }

  // zoomIn
  zoomIn(): void {
    const zoom = this.getView().getZoom();
    if (zoom === 20) return;

    if (zoom) {
      this.getView().setZoom(zoom + 1);
    }
  }

  // setRotation
  setRotation(rotation: number): void {
    this.getView().setRotation(rotation)
  }


  switchBaseLayer(layerKey: string): void {
    if (!this.baseLayers[layerKey]) {
      console.error(`Base layer with key "${layerKey}" does not exist.`)
      return
    }

    Object.keys(this.baseLayers).forEach(key => {
      this.baseLayers[key].setVisible(key === layerKey)
    })
    this.currentBaseLayer = layerKey
  }


  addBaseLayer(key: string, layer: TileLayer): void {
    if (this.baseLayers[key]) {
      this.map.removeLayer(this.baseLayers[key])
    }

    layer.setVisible(false)
    this.baseLayers[key] = layer
    this.map.addLayer(layer)

    if (this.currentBaseLayer === null) {
      this.switchBaseLayer(key)
    }
  }

  setView(view: View): void {
    this.map.setView(view)
  }

  getCurrentBaseLayer(): string | null {
    return this.currentBaseLayer
  }

  destroy(): void {
    if (this.map) {
      this.map.setTarget('')
      // @ts-ignore
      this.map = null
    }
  }
}
