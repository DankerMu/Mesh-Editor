import { defineStore } from 'pinia'
import proj4 from 'proj4'
import { get as getProjection, transformExtent } from 'ol/proj'
import { register } from 'ol/proj/proj4'

interface ProjectionDefinition {
  title: string
  key: string
  def: string,
  options: any,
  extent?: number[]
}

type ProjectionDefs = {
  [key: string]: ProjectionDefinition
}

export const useProjectionStore = defineStore('projection', () => {
  const currentProjection = ref('EPSG:3857')
  const projectionDefs: ProjectionDefs = {
    'EPSG:lbt': {
      title: '兰伯特',
      key: 'EPSG:lbt',
      def: '+proj=lcc +lat_1=25 +lat_2=47 +lat_0=36 +lon_0=105 +x_0=0 +y_0=0 +ellps=WGS84 +datum=WGS84 +units=m +no_defs',
      options: {
        center: [116, 39],
        zoom: 4,
        maxZoom: 17,
        minZoom: 2,
        projection: 'EPSG:lbt',
        extent: [70, -10, 140, 58]
      },
      extent: [55, -30, 165, 70]
    },
    'EPSG:4326': {
      title: 'WGS84',
      key: 'EPSG:4326',
      def: '+proj=longlat +ellps=WGS84 +datum=WGS84 +no_defs',
      options: {
        center: [116, 39],
        zoom: 4,
        maxZoom: 17,
        minZoom: 2,
        projection: 'EPSG:4326',
      }
    },
    'EPSG:3857': {
      title: 'Mercator',
      key: 'EPSG:3857',
      def: '+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +wktext +no_defs',
      options: {
        center: [116, 39],
        zoom: 4,
        maxZoom: 17,
        minZoom: 2,
        projection: 'EPSG:3857',
      }
    }
  }

  // getters
  const currentProjectionDef = computed(() => 
    projectionDefs[currentProjection.value]
  )

  // actions
  function initProjections() {
    Object.entries(projectionDefs).forEach(([code, projection]) => {
      proj4.defs(code, projection.def)
      
      register(proj4)
      
      // 自定义的投影 需设置世界范围 & 投影范围
      if (projection.extent) {
        const proj = getProjection(code)
        if (proj) {
          proj.setWorldExtent(projection.extent)
          proj.setExtent(transformExtent(projection.extent, 'EPSG:4326', code))
        }
      }

    })
  }

  function setProjection(code: string) {
    if (projectionDefs[code]) {
      currentProjection.value = code
    } else {
      console.warn(`Projection ${code} not found`)
    }
  }

  return {
    currentProjection,
    projectionDefs,
    currentProjectionDef,
    initProjections,
    setProjection
  }
})
