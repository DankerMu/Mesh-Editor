export type ToolType = 'polygon' | 'line_buffer' | 'brush_path' | 'lasso'
export type ViewMode = 'before' | 'after' | 'delta' | 'change' | 'touched' | 'changed' | 'review'

export interface PolygonGeometry {
  type: 'polygon'
  coordinates: [number, number][]
}

export interface LineBufferGeometry {
  type: 'line_buffer'
  coordinates: [number, number][]
  width_grid: number
}

export interface BrushPathGeometry {
  type: 'brush_path'
  points: [number, number][]
  radius_grid: number
}

export interface LassoGeometry {
  type: 'lasso'
  coordinates: [number, number][]
}

export type MaskGeometry = PolygonGeometry | LineBufferGeometry | BrushPathGeometry | LassoGeometry

export interface EditorState {
  sessionId: string | null
  windowId: string | null
  loadingSession: boolean
  loadingFields: boolean
  fieldLoadError: string | null
}

export interface GridHoverPayload {
  lon: number
  lat: number
  gridI: number
  gridJ: number
  qpfBefore: number | null
  qpfAfter: number | null
  ptypeBefore: number | null
  ptypeAfter: number | null
  isEdited: boolean
  inBounds: boolean
}

export interface EditStats {
  affectedCount: number
  meanDelta: number
  maxDelta: number
  minDelta: number
}

export interface EditOperationDTO {
  operationId: string
  toolType: ToolType
  createdAt: string
  affectedCount: number
}
