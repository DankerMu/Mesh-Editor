import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ApprovalView from '@/views/ApprovalView.vue'
import { useAuthStore } from '@/stores/authStore'
import { useVersionStore } from '@/stores/versionStore'
import { useWindowStore } from '@/stores/windowStore'
import type { VersionDetail, VersionListItem } from '@/api/version'

vi.mock('@/api/version', () => ({
  getVersions: vi.fn(),
  getVersionDetail: vi.fn(),
  saveVersion: vi.fn(),
  submitVersion: vi.fn(),
  reviewVersion: vi.fn(),
  releaseVersion: vi.fn(),
}))

const SUBMITTED_VERSION: VersionListItem = {
  version_id: 'window-1_v001',
  window_id: 'window-1',
  version_no: 1,
  base_version_id: null,
  status: 'submitted',
  has_images: true,
  created_by: 'forecaster',
  created_at: '2026-05-17T00:00:00Z',
}

const APPROVED_VERSION: VersionListItem = {
  ...SUBMITTED_VERSION,
  version_id: 'window-1_v002',
  version_no: 2,
  status: 'approved',
}

function makeDetail(version: VersionListItem): VersionDetail {
  return {
    ...version,
    session_id: 'session-1',
    image_paths: {
      before_product: '/images/before.png',
      after_product: '/images/after.png',
      delta_qpf: null,
      change_ptype: null,
      touched_mask: null,
      changed_mask: null,
    },
    field_urls: {},
    operation_summary: {
      operation_count: 3,
      affected_grid_count: 42,
    },
    approval_history: [
      {
        approval_id: 'approval-1',
        version_id: version.version_id,
        reviewer_id: 'reviewer',
        action: 'approve',
        comment: null,
        reviewed_at: '2026-05-17T01:00:00Z',
      },
    ],
    before_image_path: '/images/before.png',
    after_image_path: '/images/after.png',
    delta_qpf_image_path: null,
    change_ptype_image_path: null,
    touched_mask_image_path: null,
    changed_mask_image_path: null,
    review_image_path: null,
  }
}

function mountApproval(role = 'reviewer', detail: VersionDetail | null = makeDetail(SUBMITTED_VERSION)) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const versionStore = useVersionStore()
  const authStore = useAuthStore()
  const windowStore = useWindowStore()

  authStore.token = 'token'
  authStore.user = {
    user_id: 'user-1',
    username: role,
    display_name: role,
    role,
  }
  windowStore.windows = [
    {
      window_id: 'window-1',
      accum_hours: 24,
      start_lead: 24,
      end_lead: 48,
      status: 'available',
      qc_status: 'pass',
      negative_count: 0,
      negative_min_value: null,
      negative_abs_max: null,
      missing_count: 0,
      ptype_missing_leads: [],
      qpf_before_path: 'qpf.npz',
      ptype_before_path: 'ptype.npz',
      data_ready_at: '2026-05-17T00:00:00Z',
      updated_at: '2026-05-17T00:00:00Z',
    },
  ]

  vi.spyOn(versionStore, 'fetchVersions').mockImplementation(async (filters) => {
    if (filters) {
      versionStore.filters = { ...versionStore.filters, ...filters }
    }
    versionStore.versions = [SUBMITTED_VERSION, APPROVED_VERSION]
    return versionStore.versions
  })
  vi.spyOn(versionStore, 'fetchVersionDetail').mockImplementation(async (versionId) => {
    versionStore.currentVersion = makeDetail(
      versionId === APPROVED_VERSION.version_id ? APPROVED_VERSION : SUBMITTED_VERSION,
    )
    return versionStore.currentVersion
  })
  vi.spyOn(versionStore, 'reviewVersion').mockResolvedValue({
    version_id: SUBMITTED_VERSION.version_id,
    version_status: 'approved',
  })
  vi.spyOn(versionStore, 'releaseVersion').mockResolvedValue({
    version_id: APPROVED_VERSION.version_id,
    release_id: 'release-1',
  })

  versionStore.versions = [SUBMITTED_VERSION, APPROVED_VERSION]
  versionStore.currentVersion = detail

  const wrapper = mount(ApprovalView, {
    global: {
      plugins: [pinia],
    },
  })

  return { wrapper, versionStore }
}

describe('ApprovalView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('渲染版本列表', async () => {
    const { wrapper } = mountApproval()
    await flushPromises()

    expect(wrapper.find('[data-test="version-list"]').text()).toContain('v001')
    expect(wrapper.find('[data-test="version-list"]').text()).toContain('待审核')
    expect(wrapper.text()).toContain('window-1_v001')
  })

  it('点击状态 tab 触发筛选', async () => {
    const { wrapper, versionStore } = mountApproval()
    await flushPromises()

    await wrapper.find('[data-test="status-tab-submitted"]').trigger('click')

    expect(versionStore.fetchVersions).toHaveBeenLastCalledWith({
      status: 'submitted',
      windowId: undefined,
    })
  })

  it('reviewer 查看待审核版本时显示通过按钮', async () => {
    const { wrapper } = mountApproval('reviewer', makeDetail(SUBMITTED_VERSION))
    await flushPromises()

    expect(wrapper.find('[data-test="approve-button"]').exists()).toBe(true)
  })

  it('forecaster 查看待审核版本时隐藏通过按钮', async () => {
    const { wrapper } = mountApproval('forecaster', makeDetail(SUBMITTED_VERSION))
    await flushPromises()

    expect(wrapper.find('[data-test="approve-button"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="reject-button"]').exists()).toBe(false)
  })

  it('退回必须填写意见', async () => {
    const { wrapper, versionStore } = mountApproval('reviewer', makeDetail(SUBMITTED_VERSION))
    await flushPromises()

    await wrapper.find('[data-test="reject-button"]').trigger('click')
    await wrapper.find('[data-test="reject-confirm"]').trigger('click')

    expect(wrapper.find('[data-test="reject-error"]').text()).toContain('请填写退回意见')
    expect(versionStore.reviewVersion).not.toHaveBeenCalled()
  })

  it('发布按钮打开确认弹窗', async () => {
    const { wrapper } = mountApproval('reviewer', makeDetail(APPROVED_VERSION))
    await flushPromises()

    await wrapper.find('[data-test="release-button"]').trigger('click')

    expect(wrapper.text()).toContain('确认发布该版本？发布后旧版本将自动替代。')
  })
})
