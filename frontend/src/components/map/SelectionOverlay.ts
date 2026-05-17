import Feature from 'ol/Feature'
import type Map from 'ol/Map'
import Circle from 'ol/geom/Circle'
import LineString from 'ol/geom/LineString'
import Polygon from 'ol/geom/Polygon'
import VectorLayer from 'ol/layer/Vector'
import VectorSource from 'ol/source/Vector'
import { Circle as CircleStyle, Fill, Stroke, Style } from 'ol/style'
import type { MaskGeometry } from '@/types/editor'

const GRID_STEP = 0.05

export class SelectionOverlay {
  private readonly source: VectorSource
  private readonly layer: VectorLayer<VectorSource>
  private readonly map: Map

  constructor(map: Map) {
    this.map = map
    this.source = new VectorSource({ wrapX: false })
    this.layer = new VectorLayer({
      source: this.source,
      zIndex: 20,
      style: [
        new Style({
          fill: new Fill({ color: 'rgba(22, 93, 255, 0.16)' }),
          stroke: new Stroke({ color: 'rgba(22, 93, 255, 0.9)', width: 2 }),
          image: new CircleStyle({
            radius: 4,
            fill: new Fill({ color: 'rgba(22, 93, 255, 0.35)' }),
            stroke: new Stroke({ color: 'rgba(22, 93, 255, 0.95)', width: 1 }),
          }),
        }),
      ],
    })

    this.map.addLayer(this.layer)
  }

  updateGeometry(geom: MaskGeometry | null): void {
    this.source.clear()

    if (!geom) {
      return
    }

    if (geom.type === 'polygon') {
      this.addPolygon(geom.coordinates)
      return
    }

    if (geom.type === 'line_buffer') {
      this.addLineBuffer(geom.coordinates, geom.width_grid)
      return
    }

    for (const point of geom.points) {
      this.source.addFeature(new Feature(new Circle(point, geom.radius_grid * GRID_STEP)))
    }
  }

  dispose(): void {
    this.source.clear()
    this.map.removeLayer(this.layer)
    this.layer.setSource(null)
  }

  private addPolygon(points: [number, number][]): void {
    if (points.length < 3) {
      return
    }

    this.source.addFeature(new Feature(new Polygon([[...points, points[0]]])))
  }

  private addLineBuffer(points: [number, number][], widthGrid: number): void {
    if (points.length < 2) {
      return
    }

    const widthDegree = widthGrid * GRID_STEP
    this.source.addFeature(new Feature(new LineString(points)))
    this.source.addFeature(new Feature(new Polygon([createBufferedLineRing(points, widthDegree)])))
  }
}

function createBufferedLineRing(points: [number, number][], widthDegree: number): [number, number][] {
  const halfWidth = widthDegree / 2
  const left: [number, number][] = []
  const right: [number, number][] = []

  for (let index = 0; index < points.length; index += 1) {
    const previous = points[Math.max(0, index - 1)]
    const next = points[Math.min(points.length - 1, index + 1)]
    const dx = next[0] - previous[0]
    const dy = next[1] - previous[1]
    const length = Math.hypot(dx, dy) || 1
    const offsetX = (-dy / length) * halfWidth
    const offsetY = (dx / length) * halfWidth
    const point = points[index]

    left.push([point[0] + offsetX, point[1] + offsetY])
    right.unshift([point[0] - offsetX, point[1] - offsetY])
  }

  const start = left[0]
  return [...left, ...right, start]
}
