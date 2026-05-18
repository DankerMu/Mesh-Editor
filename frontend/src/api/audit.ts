import http from '@/api/http'
import type { ApiResponse } from '@/api/admin'

export interface AuditLogParams {
  user_id?: number
  action?: string
  resource_type?: string
  start_date?: string
  end_date?: string
  page?: number
  page_size?: number
}

export interface AuditLogItem {
  id: number
  user_id: number | null
  username: string
  action: string
  resource_type: string | null
  resource_id: string | null
  detail_json: string | null
  ip_address: string | null
  created_at: string
}

export interface AuditLogListResponse {
  items: AuditLogItem[]
  total: number
  page: number
  page_size: number
}

export async function getAuditLogs(
  params: AuditLogParams = {},
): Promise<ApiResponse<AuditLogListResponse>> {
  const { data } = await http.get<ApiResponse<AuditLogListResponse>>('/audit/logs', { params })
  return data
}
