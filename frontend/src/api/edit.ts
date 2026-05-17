import http from '@/api/http'

interface ApiResponse<T> {
  code: string
  message: string
  data: T
  trace_id: string
}

export type EditTool = 'polygon' | 'line_buffer' | 'brush_path'
export type EditVariable = 'qpf' | 'ptype'
export type EditOperation =
  | 'set_value'
  | 'increase'
  | 'decrease'
  | 'multiply'
  | 'clear'
  | 'ptype_set'
  | 'set_ptype'
  | 'screen_clear'

export interface EditPreviewRequest {
  session_id: string
  tool: EditTool
  variable: EditVariable
  operation: EditOperation
  mask: Record<string, unknown>
  parameters: Record<string, unknown>
}

export interface EditPreviewResponse {
  preview_id: string
  affected_grid_count: number
  affected_area_km2: number
  area_mode?: string
  before_stats: Record<string, number>
  after_stats: Record<string, number>
  op_ptype_transition: Record<string, number> | null
  new_precip_needs_ptype: boolean
  new_precip_count: number
  warnings: Array<{ code: string; count: number }>
  preview_image?: string | null
}

export interface EditApplyRequest {
  session_id: string
  preview_id: string
  target_ptype?: number
}

export interface EditApplyResponse {
  operation_id: string
  sequence_no: number
  applied: boolean
  can_undo: boolean
  can_redo: boolean
}

export interface UndoRedoRequest {
  session_id: string
}

export interface UndoRedoResponse {
  can_undo: boolean
  can_redo: boolean
  operation_count: number
}

export interface OperationItem {
  sequence_no: number
  tool_name: string
  operation_type: string
  variable_name: string
  affected_grid_count: number
  is_undone: number
  created_at: string
}

export interface OperationListResponse {
  operations: OperationItem[]
}

export async function editPreview(params: EditPreviewRequest): Promise<EditPreviewResponse> {
  const { data } = await http.post<ApiResponse<EditPreviewResponse>>('/edit/preview', params)
  return data.data
}

export async function editApply(params: EditApplyRequest): Promise<EditApplyResponse> {
  const { data } = await http.post<ApiResponse<EditApplyResponse>>('/edit/apply', params)
  return data.data
}

export async function editUndo(params: UndoRedoRequest): Promise<UndoRedoResponse> {
  const { data } = await http.post<ApiResponse<UndoRedoResponse>>('/edit/undo', params)
  return data.data
}

export async function editRedo(params: UndoRedoRequest): Promise<UndoRedoResponse> {
  const { data } = await http.post<ApiResponse<UndoRedoResponse>>('/edit/redo', params)
  return data.data
}

export async function getOperations(sessionId: string): Promise<OperationListResponse> {
  const { data } = await http.get<ApiResponse<OperationListResponse>>('/edit/operations', {
    params: { session_id: sessionId },
  })
  return data.data
}
