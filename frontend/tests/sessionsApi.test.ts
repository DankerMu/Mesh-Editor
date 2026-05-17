import { beforeEach, describe, expect, it, vi } from 'vitest'
import http from '@/api/http'
import { fetchField, loadSession, startSession } from '@/api/sessions'

vi.mock('@/api/http', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
  },
}))

describe('sessions API', () => {
  beforeEach(() => {
    vi.mocked(http.post).mockReset()
    vi.mocked(http.get).mockReset()
  })

  it('startSession 调用 POST /session/start 并返回 data.data', async () => {
    const response = {
      session_id: 'session-1',
      window_id: 'window-1',
      base_version_id: null,
      status: 'editing',
      created_at: '2026-05-17T00:00:00Z',
    }
    vi.mocked(http.post).mockResolvedValue({ data: { data: response } })

    await expect(startSession('window-1')).resolves.toEqual(response)

    expect(http.post).toHaveBeenCalledWith('/session/start', { window_id: 'window-1' })
  })

  it('loadSession 调用 GET /session/{id}/load 并返回 data.data', async () => {
    const response = {
      session_id: 'session-1',
      window_id: 'window-1',
      base_version_id: null,
      status: 'editing',
      grid_rows: 501,
      grid_cols: 821,
      operation_count: 0,
      can_undo: false,
      can_redo: false,
      field_urls: {},
      before_image: null,
      after_image: null,
    }
    vi.mocked(http.get).mockResolvedValue({ data: { data: response } })

    await expect(loadSession('session-1')).resolves.toEqual(response)

    expect(http.get).toHaveBeenCalledWith('/session/session-1/load')
  })

  it('fetchField 使用 arraybuffer 并提取 X-Grid headers', async () => {
    const buffer = new ArrayBuffer(8)
    vi.mocked(http.get).mockResolvedValue({
      data: buffer,
      headers: {
        'x-grid-rows': '501',
        'x-grid-cols': '821',
        'x-grid-dtype': 'float32',
        'x-grid-order': 'C',
        'x-grid-byte-length': '8',
        'x-grid-variable': 'qpf_before',
        'content-type': 'application/octet-stream',
      },
    })

    const result = await fetchField('/api/session/session-1/field/qpf_before')

    expect(http.get).toHaveBeenCalledWith('/api/session/session-1/field/qpf_before', {
      responseType: 'arraybuffer',
    })
    expect(result.buffer).toBe(buffer)
    expect(result.headers).toEqual({
      'x-grid-rows': '501',
      'x-grid-cols': '821',
      'x-grid-dtype': 'float32',
      'x-grid-order': 'C',
      'x-grid-byte-length': '8',
      'x-grid-variable': 'qpf_before',
    })
  })

  it('fetchField 在 byte-length 不匹配时抛错', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: new ArrayBuffer(4),
      headers: {
        'x-grid-byte-length': '8',
      },
    })

    await expect(fetchField('/field')).rejects.toThrow(
      'Binary transfer integrity error: expected 8 bytes, got 4',
    )
  })
})
