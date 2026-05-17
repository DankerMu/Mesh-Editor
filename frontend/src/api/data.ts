import http from '@/api/http'
import type { AccumHours, QcStatus, WindowStatus } from '@/types/enums'

interface ApiResponse<T> {
  code: string
  message: string
  data: T
  trace_id: string
}

export interface ScanResponse {
  scan_id: string
  status: string
}

export interface ScanStatusResponse {
  scan_id: string
  case_id: string
  status: string
  scan_started_at: string
  scan_finished_at: string | null
  tp_files_found: number
  ptype_files_found: number
  windows_created: number
  windows_updated: number
  errors_json: Array<Record<string, unknown>> | null
  total_windows: number
  available_count: number
  partial_count: number
  invalid_count: number
}

export interface WindowItem {
  window_id: string
  accum_hours: AccumHours
  start_lead: number
  end_lead: number
  status: WindowStatus
  qc_status: QcStatus
  negative_count: number
  negative_min_value: number | null
  negative_abs_max: number | null
  missing_count: number
  ptype_missing_leads: number[] | null
  qpf_before_path: string | null
  ptype_before_path: string | null
  data_ready_at: string | null
  updated_at: string | null
}

export async function postScan(caseId: string): Promise<ScanResponse> {
  const { data } = await http.post<ApiResponse<ScanResponse>>('/data/scan', {
    case_id: caseId,
  })

  return data.data
}

export async function getScanStatus(params: {
  scan_id?: string
  case_id?: string
}): Promise<ScanStatusResponse> {
  const { data } = await http.get<ApiResponse<ScanStatusResponse>>('/data/status', {
    params,
  })

  return data.data
}

export async function getWindows(params: {
  case_id: string
  accum_hours?: number
  status?: string
}): Promise<WindowItem[]> {
  const { data } = await http.get<ApiResponse<WindowItem[]>>('/windows', {
    params,
  })

  return data.data
}
