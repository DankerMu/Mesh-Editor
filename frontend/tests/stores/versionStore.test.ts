import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import {
  getVersionDetail,
  getVersions,
  releaseVersion,
  reviewVersion,
  saveVersion,
  submitVersion,
} from '@/api/version'
import { useVersionStore } from '@/stores/versionStore'
import type { VersionDetail, VersionListItem } from '@/api/version'

vi.mock('@/api/version', () => ({
  getVersions: vi.fn(),
  getVersionDetail: vi.fn(),
  saveVersion: vi.fn(),
  submitVersion: vi.fn(),
  reviewVersion: vi.fn(),
  releaseVersion: vi.fn(),
}))

const VERSION: VersionListItem = {
  version_id: 'window-1_v001',
  window_id: 'window-1',
  version_no: 1,
  base_version_id: null,
  status: 'submitted',
  has_images: true,
  created_by: 'forecaster',
  created_at: '2026-05-17T00:00:00Z',
}

const DETAIL: VersionDetail = {
  ...VERSION,
  session_id: 'session-1',
  image_paths: {
    before_product: '/images/before.png',
    after_product: '/images/after.png',
  },
  field_urls: {},
  operation_summary: {
    operation_count: 2,
    affected_grid_count: 30,
  },
  approval_history: [],
  before_image_path: '/images/before.png',
  after_image_path: '/images/after.png',
  delta_qpf_image_path: null,
  change_ptype_image_path: null,
  touched_mask_image_path: null,
  changed_mask_image_path: null,
  review_image_path: null,
}

describe('versionStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(getVersions).mockReset()
    vi.mocked(getVersionDetail).mockReset()
    vi.mocked(saveVersion).mockReset()
    vi.mocked(submitVersion).mockReset()
    vi.mocked(reviewVersion).mockReset()
    vi.mocked(releaseVersion).mockReset()
  })

  it('fetchVersions 携带状态和窗口筛选', async () => {
    vi.mocked(getVersions).mockResolvedValue([VERSION])

    const store = useVersionStore()
    await store.fetchVersions({ status: 'submitted', windowId: 'window-1' })

    expect(getVersions).toHaveBeenCalledWith({
      status: 'submitted',
      window_id: 'window-1',
    })
    expect(store.versions).toEqual([VERSION])
    expect(store.filters).toEqual({ status: 'submitted', windowId: 'window-1' })
    expect(store.loading).toBe(false)
  })

  it('saveVersion 保存后记录结果并刷新列表', async () => {
    vi.mocked(saveVersion).mockResolvedValue({
      session_id: 'session-1',
      version_id: 'window-1_v001',
      before_image: null,
      after_image: null,
      review_image: null,
    })
    vi.mocked(getVersions).mockResolvedValue([VERSION])

    const store = useVersionStore()
    const response = await store.saveVersion('session-1')

    expect(saveVersion).toHaveBeenCalledWith('session-1')
    expect(getVersions).toHaveBeenCalledTimes(1)
    expect(response.version_id).toBe('window-1_v001')
    expect(store.lastSavedVersion?.version_id).toBe('window-1_v001')
    expect(store.versions).toEqual([VERSION])
  })

  it('reviewVersion 调用审核 API 并刷新列表和当前详情', async () => {
    vi.mocked(reviewVersion).mockResolvedValue({
      version_id: 'window-1_v001',
      version_status: 'approved',
      approval_id: 'approval-1',
    })
    vi.mocked(getVersions).mockResolvedValue([{ ...VERSION, status: 'approved' }])
    vi.mocked(getVersionDetail).mockResolvedValue({ ...DETAIL, status: 'approved' })

    const store = useVersionStore()
    store.currentVersion = DETAIL
    await store.reviewVersion('window-1_v001', 'approve')

    expect(reviewVersion).toHaveBeenCalledWith('window-1_v001', 'approve', undefined)
    expect(getVersions).toHaveBeenCalledTimes(1)
    expect(getVersionDetail).toHaveBeenCalledWith('window-1_v001')
    expect(store.currentVersion?.status).toBe('approved')
  })

  it('错误时写入 error 并恢复 loading', async () => {
    vi.mocked(getVersions).mockRejectedValue(new Error('版本列表失败'))

    const store = useVersionStore()

    await expect(store.fetchVersions()).rejects.toThrow('版本列表失败')
    expect(store.error).toBe('版本列表失败')
    expect(store.loading).toBe(false)
  })
})
