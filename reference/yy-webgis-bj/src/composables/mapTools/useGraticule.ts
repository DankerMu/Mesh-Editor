import { Graticule } from "ol"
import Stroke from "ol/style/Stroke"

export const useGraticuleLayer = () => {
  const lonlatShow = ref(false)
  let graticuleLayer: Graticule | null = null

  function toggleGraticule () {
    if (lonlatShow.value && graticuleLayer) {
      olMap?.removeLayer(graticuleLayer)
      graticuleLayer = null
    } else {
      graticuleLayer = new Graticule({
        strokeStyle: new Stroke({
          color: 'rgba(255,120,0,0.9)',
          width: 1,
          // lineDash: [0.5, 4],
        }),
        showLabels: true,
        lonLabelPosition: 0.95,
        // latLabelPosition: 0.95,
        wrapX: true,
        zIndex: 3,
      })
      console.log(graticuleLayer)
      olMap?.addLayer(graticuleLayer)
    }
    lonlatShow.value = !lonlatShow.value
  }

  return {
    lonlatShow,
    graticuleLayer,
    toggleGraticule
  }
}
