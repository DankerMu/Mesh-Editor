import http from '@/api/http'
import type { ApiResponse } from '@/api/admin'

export interface StorageBreakdownItem {
  type: string
  size_bytes: number
  size_gb: number
  file_count: number
}

export interface StorageSummary {
  total_bytes: number
  free_bytes: number
  used_bytes: number
  total_gb: number
  used_gb: number
  free_gb: number
  breakdown: StorageBreakdownItem[]
  last_scan_at: string
}

export interface TaskCounts {
  pending: number
  running: number
  success: number
  partial_success: number
  failed: number
  permanently_failed: number
  superseded: number
}

export interface FailedTaskItem {
  review_id: string
  window_id: string
  plot_status: string
  error_summary: string | null
  failed_at: string | null
}

export interface TaskSummary {
  counts: TaskCounts
  recent_failed: FailedTaskItem[]
}

export interface TaskRetryResponse {
  review_id: string
  plot_status: string
}

export async function getStorageSummary(): Promise<ApiResponse<StorageSummary>> {
  const { data } = await http.get<ApiResponse<StorageSummary>>('/monitor/storage')
  return data
}

export async function getTaskSummary(): Promise<ApiResponse<TaskSummary>> {
  const { data } = await http.get<ApiResponse<TaskSummary>>('/monitor/tasks')
  return data
}

export async function retryTask(
  reviewId: string,
): Promise<ApiResponse<TaskRetryResponse>> {
  const { data } = await http.post<ApiResponse<TaskRetryResponse>>(
    `/monitor/tasks/${reviewId}/retry`,
  )
  return data
}
