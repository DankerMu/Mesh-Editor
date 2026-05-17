import WebGLTileLayer from 'ol/layer/WebGLTile'
import DataTileSource from 'ol/source/DataTile'
import TileGrid from 'ol/tilegrid/TileGrid'
import { get as getProjection } from 'ol/proj'
import {
  GRID_COLS,
  GRID_ROWS,
  MIXED_PHASE_COLOR,
  PHASE_QPF_THRESHOLDS,
  RAIN_PHASE_COLORS,
  SNOW_PHASE_COLORS,
} from '@/constants/precipColors'

const GRID_EXTENT = [70, 25, 111, 50] as const
const GRID_STEP = 0.05
const TRANSPARENT: [number, number, number, number] = [0, 0, 0, 0]
const SOURCE_TILE_SIZE: [number, number] = [GRID_COLS, GRID_ROWS]

export interface GridIndex {
  gridI: number
  gridJ: number
}

export function getGridIndex(lon: number, lat: number): GridIndex | null {
  if (lon < GRID_EXTENT[0] || lon > GRID_EXTENT[2] || lat < GRID_EXTENT[1] || lat > GRID_EXTENT[3]) {
    return null
  }

  const gridI = Math.round((lat - GRID_EXTENT[1]) / GRID_STEP)
  const gridJ = Math.round((lon - GRID_EXTENT[0]) / GRID_STEP)

  if (gridI < 0 || gridI >= GRID_ROWS || gridJ < 0 || gridJ >= GRID_COLS) {
    return null
  }

  return { gridI, gridJ }
}

function getBandIndex(qpf: number): number {
  for (let index = PHASE_QPF_THRESHOLDS.length - 1; index >= 0; index -= 1) {
    if (qpf >= PHASE_QPF_THRESHOLDS[index]) {
      return index
    }
  }

  return 0
}

export function getColorForCell(qpf: number, ptype: number): [number, number, number, number] {
  if (ptype === 0 || qpf < PHASE_QPF_THRESHOLDS[0]) {
    return TRANSPARENT
  }

  const bandIndex = getBandIndex(qpf)

  if (ptype === 1) {
    return RAIN_PHASE_COLORS[bandIndex]
  }

  if (ptype === 2) {
    return SNOW_PHASE_COLORS[bandIndex]
  }

  if (ptype === 3) {
    return MIXED_PHASE_COLOR
  }

  return TRANSPARENT
}

export function getGridDataValue(
  lon: number,
  lat: number,
  qpfArray: Float32Array | null,
  ptypeArray: Uint8Array | null,
): { qpf: number; ptype: number; gridI: number; gridJ: number } | null {
  const index = getGridIndex(lon, lat)

  if (!index || !qpfArray || !ptypeArray) {
    return null
  }

  const arrayIndex = index.gridI * GRID_COLS + index.gridJ

  return {
    qpf: qpfArray[arrayIndex],
    ptype: ptypeArray[arrayIndex],
    gridI: index.gridI,
    gridJ: index.gridJ,
  }
}

export class PrecipPhaseGridLayer {
  private qpfArray: Float32Array | null = null
  private ptypeArray: Uint8Array | null = null
  private readonly source: DataTileSource
  private readonly layer: WebGLTileLayer

  constructor() {
    const projection = getProjection('EPSG:4326')

    if (!projection) {
      throw new Error('EPSG:4326 projection is not available')
    }

    const tileGrid = new TileGrid({
      extent: [...GRID_EXTENT],
      origin: [GRID_EXTENT[0], GRID_EXTENT[3]],
      resolutions: [GRID_STEP],
      sizes: [[1, 1]],
      tileSize: SOURCE_TILE_SIZE,
    })

    this.source = new DataTileSource({
      projection,
      tileGrid,
      tileSize: SOURCE_TILE_SIZE,
      wrapX: false,
      interpolate: false,
      bandCount: 4,
      loader: () => this.createTileData(),
    })

    this.layer = new WebGLTileLayer({
      source: this.source,
      opacity: 0.7,
      extent: [...GRID_EXTENT],
      style: {
        color: [
          'color',
          ['*', ['band', 1], 255],
          ['*', ['band', 2], 255],
          ['*', ['band', 3], 255],
          ['*', ['band', 4], 1],
        ],
      },
    })
  }

  getLayer(): WebGLTileLayer {
    return this.layer
  }

  updateData(qpfArray: Float32Array, ptypeArray: Uint8Array): void {
    this.assertDataLength(qpfArray.length, 'qpfArray')
    this.assertDataLength(ptypeArray.length, 'ptypeArray')
    this.qpfArray = qpfArray
    this.ptypeArray = ptypeArray
    this.source.refresh()
  }

  getDataValue(
    lon: number,
    lat: number,
  ): { qpf: number; ptype: number; gridI: number; gridJ: number } | null {
    return getGridDataValue(lon, lat, this.qpfArray, this.ptypeArray)
  }

  dispose(): void {
    this.qpfArray = null
    this.ptypeArray = null
    this.layer.setSource(null)
    this.source.clear()
  }

  private createTileData(): Uint8Array {
    const tileData = new Uint8Array(GRID_ROWS * GRID_COLS * 4)

    if (!this.qpfArray || !this.ptypeArray) {
      return tileData
    }

    for (let tileRow = 0; tileRow < GRID_ROWS; tileRow += 1) {
      const gridI = GRID_ROWS - 1 - tileRow

      for (let gridJ = 0; gridJ < GRID_COLS; gridJ += 1) {
        const sourceIndex = gridI * GRID_COLS + gridJ
        const targetIndex = (tileRow * GRID_COLS + gridJ) * 4
        const color = getColorForCell(this.qpfArray[sourceIndex], this.ptypeArray[sourceIndex])

        tileData[targetIndex] = color[0]
        tileData[targetIndex + 1] = color[1]
        tileData[targetIndex + 2] = color[2]
        tileData[targetIndex + 3] = color[3]
      }
    }

    return tileData
  }

  private assertDataLength(length: number, label: string): void {
    const expectedLength = GRID_ROWS * GRID_COLS

    if (length !== expectedLength) {
      throw new Error(`${label} length mismatch: expected ${expectedLength}, got ${length}`)
    }
  }
}
