import http from '@/api/http'

export interface SessionStartResponse {
  session_id: string
  window_id: string
  base_version_id: string | null
  status: string
  created_at: string
}

export interface SessionLoadResponse {
  session_id: string
  window_id: string
  base_version_id: string | null
  status: string
  grid_rows: number
  grid_cols: number
  operation_count: number
  can_undo: boolean
  can_redo: boolean
  field_urls: Record<string, string>
  before_image: string | null
  after_image: string | null
}

export async function startSession(windowId: string): Promise<SessionStartResponse> {
  const { data } = await http.post<{ data: SessionStartResponse }>('/session/start', {
    window_id: windowId,
  })
  return data.data
}

export async function loadSession(sessionId: string): Promise<SessionLoadResponse> {
  const { data } = await http.get<{ data: SessionLoadResponse }>(`/session/${sessionId}/load`)
  return data.data
}

export async function fetchField(
  url: string,
): Promise<{ buffer: ArrayBuffer; headers: Record<string, string> }> {
  const normalizedUrl = url.startsWith('/api/') ? url.slice(4) : url
  const response = await http.get<ArrayBuffer>(normalizedUrl, { responseType: 'arraybuffer' })
  const headers: Record<string, string> = {}

  for (const key of [
    'x-grid-rows',
    'x-grid-cols',
    'x-grid-dtype',
    'x-grid-order',
    'x-grid-byte-length',
    'x-grid-variable',
  ]) {
    const value = response.headers[key]
    if (value) {
      headers[key] = String(value)
    }
  }

  const rawLength = headers['x-grid-byte-length']
  if (!rawLength) {
    throw new Error('Binary transfer integrity error: missing X-Grid-Byte-Length header')
  }
  const expectedLength = parseInt(rawLength, 10)
  if (!Number.isFinite(expectedLength) || expectedLength <= 0) {
    throw new Error(`Binary transfer integrity error: invalid X-Grid-Byte-Length: ${rawLength}`)
  }
  if (response.data.byteLength !== expectedLength) {
    throw new Error(
      `Binary transfer integrity error: expected ${expectedLength} bytes, got ${response.data.byteLength}`,
    )
  }

  return { buffer: response.data, headers }
}
