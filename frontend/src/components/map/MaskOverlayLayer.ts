import WebGLTileLayer from 'ol/layer/WebGLTile'
import DataTileSource from 'ol/source/DataTile'
import TileGrid from 'ol/tilegrid/TileGrid'
import { get as getProjection } from 'ol/proj'
import { GRID_COLS, GRID_ROWS } from '@/constants/precipColors'

const GRID_EXTENT = [70, 25, 111, 50] as const
const GRID_STEP = 0.05
const SOURCE_TILE_SIZE: [number, number] = [GRID_COLS, GRID_ROWS]
const DEFAULT_MASK_COLOR: [number, number, number, number] = [128, 128, 128, 100]

export class MaskOverlayLayer {
  private maskArray: Uint8Array | null = null
  private hasInvalidCells = false
  private readonly maskColor: [number, number, number, number]
  private readonly source: DataTileSource
  private readonly layer: WebGLTileLayer

  constructor(maskColor: [number, number, number, number] = DEFAULT_MASK_COLOR) {
    this.maskColor = maskColor
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
      opacity: 1,
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

  updateData(maskArray: Uint8Array): void {
    const expectedLength = GRID_ROWS * GRID_COLS

    if (maskArray.length !== expectedLength) {
      throw new Error(`maskArray length mismatch: expected ${expectedLength}, got ${maskArray.length}`)
    }

    this.maskArray = maskArray
    this.hasInvalidCells = maskArray.some((value) => value > 0)
    this.layer.setVisible(this.hasInvalidCells)
    this.source.refresh()
  }

  clearData(): void {
    this.maskArray = null
    this.hasInvalidCells = false
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

    if (!this.maskArray || !this.hasInvalidCells) {
      return tileData
    }

    for (let tileRow = 0; tileRow < GRID_ROWS; tileRow += 1) {
      const gridI = GRID_ROWS - 1 - tileRow

      for (let gridJ = 0; gridJ < GRID_COLS; gridJ += 1) {
        const sourceIndex = gridI * GRID_COLS + gridJ

        if (this.maskArray[sourceIndex] === 0) {
          continue
        }

        const targetIndex = (tileRow * GRID_COLS + gridJ) * 4
        tileData[targetIndex] = this.maskColor[0]
        tileData[targetIndex + 1] = this.maskColor[1]
        tileData[targetIndex + 2] = this.maskColor[2]
        tileData[targetIndex + 3] = this.maskColor[3]
      }
    }

    return tileData
  }
}
