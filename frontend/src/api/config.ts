import http from '@/api/http'
import type { ApiResponse } from '@/api/admin'

export type ConfigType = 'product_config' | 'plot_config' | 'template_config'

export interface ConfigSnapshot {
  snapshot_id: string
  config_type: ConfigType | string
  changed_by: string | null
  created_at: string
}

export interface ConfigHistoryResponse {
  items: ConfigSnapshot[]
  total: number
}

export async function getConfig(configType: ConfigType): Promise<ApiResponse<Record<string, unknown>>> {
  const { data } = await http.get<ApiResponse<Record<string, unknown>>>(`/config/${configType}`)
  return data
}

export async function updateConfig(
  configType: ConfigType,
  payload: Record<string, unknown>,
): Promise<ApiResponse<ConfigSnapshot>> {
  const { data } = await http.put<ApiResponse<ConfigSnapshot>>(`/config/${configType}`, payload)
  return data
}

export async function getConfigHistory(
  configType: ConfigType,
  params: { limit?: number } = {},
): Promise<ApiResponse<ConfigHistoryResponse>> {
  const { data } = await http.get<ApiResponse<ConfigHistoryResponse>>(
    `/config/${configType}/history`,
    { params },
  )
  return data
}
