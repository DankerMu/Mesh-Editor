// QPF threshold levels (mm) and corresponding RGBA colors
export const QPF_THRESHOLDS = [
  0.1, 2.5, 5, 10, 15, 20, 25, 30, 40, 50, 70, 100, 150, 200, 250,
] as const

export const QPF_COLORS: readonly [number, number, number, number][] = [
  [166, 242, 143, 200], // 0.1-2.5  浅绿
  [61, 186, 61, 200], // 2.5-5    绿
  [97, 184, 255, 200], // 5-10     浅蓝
  [0, 0, 225, 200], // 10-15    蓝
  [255, 0, 255, 200], // 15-20    品红
  [128, 0, 64, 200], // 20-25    深紫红
  [255, 128, 0, 200], // 25-30    橙
  [255, 0, 0, 200], // 30-40    红
  [153, 0, 0, 200], // 40-50    暗红
  [255, 255, 0, 200], // 50-70    黄
  [192, 192, 0, 200], // 70-100   暗黄
  [128, 0, 128, 200], // 100-150  紫
  [64, 0, 64, 200], // 150-200  暗紫
  [40, 40, 40, 200], // 200-250  深灰
  [10, 10, 10, 200], // 250+     黑
]

// Ptype value -> color (0=无, 1=雨, 2=雪, 3=雨夹雪)
export const PTYPE_COLORS: Record<number, [number, number, number, number]> = {
  0: [0, 0, 0, 0], // 无降水 - 透明
  1: [0, 128, 0, 180], // 雨 - 绿
  2: [0, 100, 255, 180], // 雪 - 蓝
  3: [255, 165, 0, 180], // 雨夹雪 - 橙
}

export const PHASE_QPF_THRESHOLDS = [0.1, 1, 5, 10, 25, 50, 100, 250] as const

export const RAIN_PHASE_COLORS: readonly [number, number, number, number][] = [
  [166, 242, 143, 200],
  [61, 186, 61, 200],
  [97, 184, 255, 200],
  [0, 0, 225, 200],
  [255, 0, 255, 200],
  [128, 0, 64, 200],
  [255, 128, 0, 200],
  [255, 0, 0, 200],
]

export const SNOW_PHASE_COLORS: readonly [number, number, number, number][] = [
  [240, 240, 240, 200],
  [220, 220, 220, 200],
  [190, 190, 190, 200],
  [160, 160, 160, 200],
  [130, 130, 130, 200],
  [100, 100, 100, 200],
  [70, 70, 70, 200],
  [40, 40, 40, 200],
]

export const MIXED_PHASE_COLOR: [number, number, number, number] = [251, 201, 252, 255]

export const GRID_ROWS = 501
export const GRID_COLS = 821
