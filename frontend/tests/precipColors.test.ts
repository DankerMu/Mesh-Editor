import { describe, expect, it } from 'vitest'
import { GRID_COLS, GRID_ROWS, PTYPE_COLORS, QPF_COLORS, QPF_THRESHOLDS } from '@/constants/precipColors'

describe('precipColors', () => {
  it('QPF 阈值和色标数量一致', () => {
    expect(QPF_THRESHOLDS).toHaveLength(15)
    expect(QPF_COLORS).toHaveLength(15)
  })

  it('每个 QPF 色标都是 0-255 RGBA', () => {
    for (const color of QPF_COLORS) {
      expect(color).toHaveLength(4)
      for (const channel of color) {
        expect(channel).toBeGreaterThanOrEqual(0)
        expect(channel).toBeLessThanOrEqual(255)
      }
    }
  })

  it('PTYPE 色标覆盖 0/1/2/3', () => {
    expect(Object.keys(PTYPE_COLORS).sort()).toEqual(['0', '1', '2', '3'])
  })

  it('网格尺寸固定为 501x821', () => {
    expect(GRID_ROWS).toBe(501)
    expect(GRID_COLS).toBe(821)
  })
})
