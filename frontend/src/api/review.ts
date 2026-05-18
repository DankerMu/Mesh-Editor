import http from '@/api/http'

export interface ApiResponse<T> {
  code: string
  message: string
  data: T
  trace_id: string
}

export type PlotStatus =
  | 'pending'
  | 'running'
  | 'success'
  | 'partial_success'
  | 'failed'
  | 'permanently_failed'
  | 'superseded'
  | string

export interface ReviewGenerateParams {
  window_id: string
  version_id: string
  template_id: string
}

export interface ReviewGenerateResponse {
  review_id: string
  plot_status: string
  message: string
}

export interface MissingField {
  variable_name?: string
  name?: string
  level_type: string | null
  level_value: number | null
  lead_hour: number | null
  reason: 'file_not_found' | 'read_error' | 'dimension_mismatch' | string
}

export interface ReviewProductDetail {
  review_id: string
  window_id: string
  version_id: string
  template_id: string
  image_path: string | null
  plot_config_path?: string | null
  plot_input_manifest_path?: string | null
  plot_code_version?: string | null
  plot_status: PlotStatus
  attempt?: number
  max_retries?: number
  locked_by?: string | null
  locked_at?: string | null
  next_retry_at?: string | null
  plot_started_at?: string | null
  plot_finished_at?: string | null
  total_panels: number | null
  success_panels: number | null
  skipped_panels: number | null
  missing_fields_json?: string | MissingField[] | null
  error_log_path?: string | null
  created_at: string
}

export interface ReviewProductListItem {
  review_id: string
  window_id: string
  version_id: string
  template_id: string
  plot_status: PlotStatus
  image_path: string | null
  total_panels: number | null
  success_panels: number | null
  skipped_panels: number | null
  created_at: string
}

export interface ReviewProductVersionItem extends ReviewProductListItem {
  version_no?: number | null
  version_status?: string | null
  version_created_by?: string | null
  version_created_at?: string | null
}

export interface ReviewListParams {
  case_id?: string
  window_id?: string
  plot_status?: string
  page?: number
  page_size?: number
}

interface ReviewListResponse {
  items: ReviewProductListItem[]
  total: number
  page: number
  page_size: number
}

export async function generateReview(
  params: ReviewGenerateParams,
): Promise<ApiResponse<ReviewGenerateResponse>> {
  const { data } = await http.post<ApiResponse<ReviewGenerateResponse>>('/review/generate', params)
  return data
}

export async function getPlotTaskStatus(
  reviewId: string,
): Promise<ApiResponse<ReviewProductDetail>> {
  const { data } = await http.get<ApiResponse<ReviewProductDetail>>(`/tasks/plot/${reviewId}`)
  return data
}

export async function getReviewDetail(
  reviewId: string,
): Promise<ApiResponse<ReviewProductDetail>> {
  const { data } = await http.get<ApiResponse<ReviewProductDetail>>(`/review/${reviewId}`)
  return data
}

export async function getReviews(
  params: ReviewListParams = {},
): Promise<ApiResponse<ReviewProductListItem[]>> {
  const { data } = await http.get<ApiResponse<ReviewListResponse | ReviewProductListItem[]>>(
    '/reviews',
    { params },
  )
  const list = Array.isArray(data.data) ? data.data : data.data.items

  return {
    ...data,
    data: list,
  }
}

export async function getReviewsByCase(
  caseId: string,
): Promise<ApiResponse<ReviewProductListItem[]>> {
  const { data } = await http.get<ApiResponse<ReviewProductListItem[]>>(`/review/case/${caseId}`)
  return data
}

export async function getReviewsByWindow(
  windowId: string,
): Promise<ApiResponse<ReviewProductVersionItem[]>> {
  const { data } = await http.get<ApiResponse<ReviewProductVersionItem[]>>(
    `/review/window/${windowId}/versions`,
  )
  return data
}

export async function exportReview(reviewId: string): Promise<void> {
  const { data } = await http.post<Blob>(
    '/review/export',
    { review_id: reviewId },
    { responseType: 'blob' },
  )
  const url = URL.createObjectURL(data)
  const link = document.createElement('a')
  link.href = url
  link.download = `review_package_${reviewId}.zip`
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
