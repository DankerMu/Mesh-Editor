import http from '@/api/http'

export interface ApiResponse<T> {
  code: string
  message: string
  data: T
  trace_id: string
}

export type UserRole = 'admin' | 'reviewer' | 'forecaster' | 'viewer'

export interface UserItem {
  id: number
  username: string
  display_name: string
  role: UserRole | string
  is_active: boolean
  created_at: string
  updated_at: string
  last_login_at: string | null
}

export interface UserListParams {
  role?: string
  is_active?: boolean
  page?: number
  page_size?: number
}

export interface UserListResponse {
  items: UserItem[]
  total: number
  page: number
  page_size: number
}

export interface UserCreatePayload {
  username: string
  display_name: string
  password: string
  role: UserRole
}

export interface UserUpdatePayload {
  display_name?: string
  role?: UserRole
  is_active?: boolean
}

export async function getUsers(
  params: UserListParams = {},
): Promise<ApiResponse<UserListResponse>> {
  const { data } = await http.get<ApiResponse<UserListResponse>>('/users', { params })
  return data
}

export async function createUser(
  payload: UserCreatePayload,
): Promise<ApiResponse<UserItem>> {
  const { data } = await http.post<ApiResponse<UserItem>>('/users', payload)
  return data
}

export async function updateUser(
  id: number,
  payload: UserUpdatePayload,
): Promise<ApiResponse<UserItem>> {
  const { data } = await http.put<ApiResponse<UserItem>>(`/users/${id}`, payload)
  return data
}
