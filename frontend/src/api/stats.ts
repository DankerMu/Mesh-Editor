import http from '@/api/http'
import type { ApiResponse } from '@/api/admin'

export interface StatsQueryParams {
  start_date: string
  end_date: string
  user_id?: number
  window_id?: string
  accum_hours?: number
}

export interface OperationStats {
  period: Record<string, string>
  total_sessions: number
  total_operations: number
  total_versions_saved: number
  total_versions_released: number
  by_accum_hours: Record<string, { sessions: number; versions: number }>
  by_tool: Record<string, number>
  by_operation: Record<string, number>
}

export interface TopTransitionItem {
  transition: string
  count: number
  label: string
}

export interface PtypeTransitionStats {
  period: Record<string, string>
  total_operations_with_transitions: number
  matrix: Record<string, number>
  top_transitions: TopTransitionItem[]
}

export interface StatsExportPayload extends StatsQueryParams {
  format?: 'csv'
  include: Array<'operations' | 'ptype_transitions' | 'version_summary'>
}

export async function getOperationStats(
  params: StatsQueryParams,
): Promise<ApiResponse<OperationStats>> {
  const { data } = await http.get<ApiResponse<OperationStats>>('/stats/operations', { params })
  return data
}

export async function getPtypeTransitions(
  params: Omit<StatsQueryParams, 'accum_hours'>,
): Promise<ApiResponse<PtypeTransitionStats>> {
  const { data } = await http.get<ApiResponse<PtypeTransitionStats>>('/stats/ptype-transitions', {
    params,
  })
  return data
}

function filenameFromDisposition(disposition: string | undefined) {
  const match = disposition?.match(/filename="?([^"]+)"?/)
  return match?.[1] ?? 'operation_stats.csv'
}

export async function exportStats(payload: StatsExportPayload): Promise<void> {
  const response = await http.post<Blob>('/stats/export', payload, { responseType: 'blob' })
  const url = URL.createObjectURL(response.data)
  const link = document.createElement('a')
  link.href = url
  link.download = filenameFromDisposition(response.headers['content-disposition'])
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
