import { describe, expect, it } from 'vitest'
import { getColorForCell, getGridDataValue, getGridIndex } from '@/components/map/PrecipPhaseGridLayer'
import { GRID_COLS, GRID_ROWS } from '@/constants/precipColors'

describe('PrecipPhaseGridLayer pure logic', () => {
  it('ptype=1 qpf=15 returns rain color band', () => {
    expect(getColorForCell(15, 1)).toEqual([0, 0, 225, 200])
  })

  it('ptype=2 qpf=8 returns snow color band', () => {
    expect(getColorForCell(8, 2)).toEqual([190, 190, 190, 200])
  })

  it('ptype=3 qpf=5 returns fixed mixed color', () => {
    expect(getColorForCell(5, 3)).toEqual([251, 201, 252, 255])
  })

  it('ptype=0 returns transparent', () => {
    expect(getColorForCell(20, 0)).toEqual([0, 0, 0, 0])
  })

  it('qpf below 0.1 returns transparent', () => {
    expect(getColorForCell(0.09, 1)).toEqual([0, 0, 0, 0])
  })

  it('getGridIndex returns expected in-bounds indices', () => {
    expect(getGridIndex(90.05, 35.1)).toEqual({ gridI: 202, gridJ: 401 })
  })

  it('getGridIndex returns null for out-of-bounds coordinates', () => {
    expect(getGridIndex(111.01, 30)).toBeNull()
    expect(getGridIndex(90, 24.99)).toBeNull()
  })

  it('getGridIndex handles boundary values', () => {
    expect(getGridIndex(70, 25)).toEqual({ gridI: 0, gridJ: 0 })
    expect(getGridIndex(111, 50)).toEqual({ gridI: 500, gridJ: 820 })
    expect(getGridIndex(70, 50)).toEqual({ gridI: 500, gridJ: 0 })
  })
  it('getGridDataValue returns qpf and ptype for an in-bounds coordinate', () => {
    const qpfArray = new Float32Array(GRID_ROWS * GRID_COLS)
    const ptypeArray = new Uint8Array(GRID_ROWS * GRID_COLS)
    const arrayIndex = 202 * GRID_COLS + 401

    qpfArray[arrayIndex] = 12.5
    ptypeArray[arrayIndex] = 1

    expect(getGridDataValue(90.05, 35.1, qpfArray, ptypeArray)).toEqual({
      qpf: 12.5,
      ptype: 1,
      gridI: 202,
      gridJ: 401,
    })
  })

  it('getGridDataValue returns null for out-of-bounds coordinate', () => {
    expect(getGridDataValue(111.01, 30, new Float32Array(0), new Uint8Array(0))).toBeNull()
  })
})
