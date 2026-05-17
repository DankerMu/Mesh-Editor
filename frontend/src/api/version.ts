import http from '@/api/http'

interface ApiResponse<T> {
  code: string
  message: string
  data: T
  trace_id: string
}

export type VersionStatus = 'draft' | 'submitted' | 'approved' | 'rejected' | 'released' | 'superseded'
export type ReviewAction = 'approve' | 'reject'
export type VersionFieldName =
  | 'qpf_before'
  | 'ptype_before'
  | 'qpf_after'
  | 'ptype_after'
  | 'delta_qpf'
  | 'change_ptype'
  | 'touched_mask'
  | 'changed_mask'

export interface VersionListParams {
  status?: string
  window_id?: string
  created_by?: string
}

export interface VersionListItem {
  version_id: string
  window_id: string
  version_no: number
  base_version_id: string | null
  status: VersionStatus | string
  has_images: boolean
  created_by: string | null
  created_at: string
}

export interface VersionSaveResponse {
  session_id: string
  version_id: string
  before_image: string | null
  after_image: string | null
  review_image: string | null
}

export interface VersionMutationResponse {
  version_id: string
  status?: string
  version_status?: string
  approval_id?: string
  release_id?: string
  release_status?: string
}

export interface ApprovalHistoryItem {
  approval_id: string
  version_id: string
  reviewer_id: string
  action: ReviewAction | string
  comment: string | null
  reviewed_at: string
}

export interface OperationSummary {
  operation_count: number
  affected_grid_count: number
}

export interface VersionDetail extends VersionListItem {
  session_id: string | null
  image_paths: Record<string, string | null>
  field_urls: Partial<Record<VersionFieldName, string>>
  operation_summary: OperationSummary
  approval_history: ApprovalHistoryItem[]
  before_image_path: string | null
  after_image_path: string | null
  delta_qpf_image_path: string | null
  change_ptype_image_path: string | null
  touched_mask_image_path: string | null
  changed_mask_image_path: string | null
  review_image_path: string | null
}

export async function saveVersion(
  sessionId: string,
  generateReview = true,
): Promise<VersionSaveResponse> {
  const { data } = await http.post<ApiResponse<VersionSaveResponse>>('/version/save', {
    session_id: sessionId,
    generate_review: generateReview,
  })
  return data.data
}

export async function submitVersion(versionId: string): Promise<VersionMutationResponse> {
  const { data } = await http.post<ApiResponse<VersionMutationResponse>>('/version/submit', {
    version_id: versionId,
  })
  return data.data
}

export async function reviewVersion(
  versionId: string,
  action: ReviewAction,
  comment?: string,
): Promise<VersionMutationResponse> {
  const { data } = await http.post<ApiResponse<VersionMutationResponse>>('/version/review', {
    version_id: versionId,
    action,
    comment,
  })
  return data.data
}

export async function releaseVersion(versionId: string): Promise<VersionMutationResponse> {
  const { data } = await http.post<ApiResponse<VersionMutationResponse>>('/version/release', {
    version_id: versionId,
  })
  return data.data
}

export async function getVersions(params: VersionListParams = {}): Promise<VersionListItem[]> {
  const { data } = await http.get<ApiResponse<VersionListItem[]>>('/versions', {
    params,
  })
  return data.data
}

export async function getVersionDetail(versionId: string): Promise<VersionDetail> {
  const { data } = await http.get<ApiResponse<VersionDetail>>(`/versions/${versionId}`)
  return data.data
}

export async function getVersionField(
  versionId: string,
  fieldName: VersionFieldName,
): Promise<ArrayBuffer> {
  const { data } = await http.get<ArrayBuffer>(`/version/${versionId}/field/${fieldName}`, {
    responseType: 'arraybuffer',
  })
  return data
}
