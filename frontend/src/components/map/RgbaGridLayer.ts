import WebGLTileLayer from 'ol/layer/WebGLTile'
import DataTileSource from 'ol/source/DataTile'
import TileGrid from 'ol/tilegrid/TileGrid'
import { get as getProjection } from 'ol/proj'
import { GRID_COLS, GRID_ROWS } from '@/constants/precipColors'

const GRID_EXTENT = [70, 25, 111, 50] as const
const GRID_STEP = 0.05
const SOURCE_TILE_SIZE: [number, number] = [GRID_COLS, GRID_ROWS]
const EMPTY_COLOR: [number, number, number, number] = [0, 0, 0, 0]
const CHANGE_PTYPE_COLORS: Record<number, [number, number, number, number]> = {
  [-3]: [0, 100, 200, 190],
  [-2]: [80, 160, 255, 190],
  [-1]: [140, 200, 255, 190],
  0: EMPTY_COLOR,
  1: [255, 200, 140, 190],
  2: [255, 140, 60, 190],
  3: [200, 60, 0, 190],
}

type FloatColorMapper = (value: number) => [number, number, number, number]
type IntColorMapper = (value: number) => [number, number, number, number]

export class FloatGridLayer {
  private gridArray: Float32Array | null = null
  private readonly colorMapper: FloatColorMapper
  private readonly source: DataTileSource
  private readonly layer: WebGLTileLayer

  constructor(colorMapper: FloatColorMapper) {
    this.colorMapper = colorMapper
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
      opacity: 0.82,
      visible: false,
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

  updateData(gridArray: Float32Array): void {
    assertDataLength(gridArray.length, 'gridArray')
    this.gridArray = gridArray
    this.layer.setVisible(true)
    this.source.refresh()
  }

  clearData(): void {
    this.gridArray = null
    this.layer.setVisible(false)
    this.source.refresh()
  }

  dispose(): void {
    this.clearData()
    this.layer.setSource(null)
    this.source.clear()
  }

  private createTileData(): Uint8Array {
    const tileData = new Uint8Array(GRID_ROWS * GRID_COLS * 4)

    if (!this.gridArray) {
      return tileData
    }

    fillTileData(tileData, (sourceIndex) => this.colorMapper(this.gridArray?.[sourceIndex] ?? 0))
    return tileData
  }
}

export class IntGridLayer {
  private gridArray: Int8Array | Uint8Array | null = null
  private readonly colorMapper: IntColorMapper
  private readonly source: DataTileSource
  private readonly layer: WebGLTileLayer

  constructor(colorMapper: IntColorMapper) {
    this.colorMapper = colorMapper
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
      opacity: 0.78,
      visible: false,
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

  updateData(gridArray: Int8Array | Uint8Array): void {
    assertDataLength(gridArray.length, 'gridArray')
    this.gridArray = gridArray
    this.layer.setVisible(true)
    this.source.refresh()
  }

  clearData(): void {
    this.gridArray = null
    this.layer.setVisible(false)
    this.source.refresh()
  }

  dispose(): void {
    this.clearData()
    this.layer.setSource(null)
    this.source.clear()
  }

  private createTileData(): Uint8Array {
    const tileData = new Uint8Array(GRID_ROWS * GRID_COLS * 4)

    if (!this.gridArray) {
      return tileData
    }

    fillTileData(tileData, (sourceIndex) => this.colorMapper(this.gridArray?.[sourceIndex] ?? 0))
    return tileData
  }
}

export function getDeltaQpfColor(value: number): [number, number, number, number] {
  if (!Number.isFinite(value) || Math.abs(value) < 0.001) {
    return EMPTY_COLOR
  }

  const magnitude = Math.min(Math.abs(value) / 20, 1)
  const channel = Math.round(255 * (1 - magnitude))

  if (value < 0) {
    return [channel, channel, 255, 205]
  }

  return [255, channel, channel, 205]
}

export function getChangePtypeColor(value: number): [number, number, number, number] {
  const normalized = Math.max(-3, Math.min(3, Math.trunc(value)))
  return CHANGE_PTYPE_COLORS[normalized] ?? EMPTY_COLOR
}

function fillTileData(tileData: Uint8Array, getColor: (sourceIndex: number) => [number, number, number, number]) {
  for (let tileRow = 0; tileRow < GRID_ROWS; tileRow += 1) {
    const gridI = GRID_ROWS - 1 - tileRow

    for (let gridJ = 0; gridJ < GRID_COLS; gridJ += 1) {
      const sourceIndex = gridI * GRID_COLS + gridJ
      const targetIndex = (tileRow * GRID_COLS + gridJ) * 4
      const color = getColor(sourceIndex)

      tileData[targetIndex] = color[0]
      tileData[targetIndex + 1] = color[1]
      tileData[targetIndex + 2] = color[2]
      tileData[targetIndex + 3] = color[3]
    }
  }
}

function assertDataLength(length: number, label: string): void {
  const expectedLength = GRID_ROWS * GRID_COLS

  if (length !== expectedLength) {
    throw new Error(`${label} length mismatch: expected ${expectedLength}, got ${length}`)
  }
}
