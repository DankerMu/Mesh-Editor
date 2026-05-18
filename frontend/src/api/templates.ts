import http from '@/api/http'
import type { ApiResponse } from '@/api/admin'

export interface ReviewTemplatePanel {
  id: string
  type: string
  fields: string[]
}

export interface ReviewTemplateSummary {
  template_id: string
  template_name: string
  required_fields: string[]
  optional_fields: string[]
  allow_partial_success: boolean
  review_time_policy: string
  panel_count: number
}

export interface ReviewTemplateDetail extends Omit<ReviewTemplateSummary, 'panel_count'> {
  panels: ReviewTemplatePanel[]
}

export interface ReviewTemplateUpdateResponse {
  template_id: string
  snapshot_id: string
}

export async function getTemplates(): Promise<ApiResponse<ReviewTemplateSummary[]>> {
  const { data } = await http.get<ApiResponse<ReviewTemplateSummary[]>>('/templates')
  return data
}

export async function getTemplate(
  id: string,
): Promise<ApiResponse<ReviewTemplateDetail>> {
  const { data } = await http.get<ApiResponse<ReviewTemplateDetail>>(`/templates/${id}`)
  return data
}

export async function updateTemplate(
  id: string,
  payload: ReviewTemplateDetail,
): Promise<ApiResponse<ReviewTemplateUpdateResponse>> {
  const { data } = await http.put<ApiResponse<ReviewTemplateUpdateResponse>>(
    `/templates/${id}`,
    payload,
  )
  return data
}
