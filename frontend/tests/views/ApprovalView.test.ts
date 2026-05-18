import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ApprovalView from '@/views/ApprovalView.vue'
import VersionFieldMap from '@/components/approval/VersionFieldMap.vue'
import { useLinkedMaps } from '@/composables/useLinkedMaps'
import { useAuthStore } from '@/stores/authStore'
import { useVersionStore } from '@/stores/versionStore'
import { useWindowStore } from '@/stores/windowStore'
import { GRID_COLS, GRID_ROWS } from '@/constants/precipColors'
import { getVersionField } from '@/api/version'
import type { VersionDetail, VersionListItem } from '@/api/version'

const mapMock = vi.hoisted(() => ({
  addLayer: vi.fn(),
  removeLayer: vi.fn(),
  getView: vi.fn(() => ({
    on: vi.fn(() => ({ key: 'view-key' })),
    getCenter: vi.fn(() => [90.5, 37.5]),
    getResolution: vi.fn(() => 0.05),
    getRotation: vi.fn(() => 0),
    setCenter: vi.fn(),
    setResolution: vi.fn(),
    setRotation: vi.fn(),
  })),
}))

const layerMocks = vi.hoisted(() => {
  function makeLayerMock(this: {
    layer: { id: string }
    updateData: ReturnType<typeof vi.fn>
    clearData: ReturnType<typeof vi.fn>
    dispose: ReturnType<typeof vi.fn>
    getLayer: ReturnType<typeof vi.fn>
  }) {
    this.layer = { id: `layer-${Math.random()}` }
    this.updateData = vi.fn()
    this.clearData = vi.fn()
    this.dispose = vi.fn()
    this.getLayer = vi.fn(() => this.layer)
  }

  return { makeLayerMock }
})

vi.mock('@/api/version', () => ({
  getVersions: vi.fn(),
  getVersionDetail: vi.fn(),
  getVersionField: vi.fn(),
  saveVersion: vi.fn(),
  submitVersion: vi.fn(),
  reviewVersion: vi.fn(),
  releaseVersion: vi.fn(),
}))

vi.mock('@/components/map/BaseMap.vue', () => ({
  default: {
    name: 'BaseMap',
    emits: ['map-ready'],
    mounted(this: { $emit: (eventName: string, payload: unknown) => void }) {
      this.$emit('map-ready', mapMock)
    },
    template: '<div data-test="base-map"></div>',
  },
}))

vi.mock('@/components/map/PrecipPhaseGridLayer', () => ({
  PrecipPhaseGridLayer: vi.fn(layerMocks.makeLayerMock),
}))

vi.mock('@/components/map/MaskOverlayLayer', () => ({
  MaskOverlayLayer: vi.fn(layerMocks.makeLayerMock),
}))

vi.mock('@/components/map/RgbaGridLayer', () => ({
  FloatGridLayer: vi.fn(layerMocks.makeLayerMock),
  IntGridLayer: vi.fn(layerMocks.makeLayerMock),
  getChangePtypeColor: vi.fn(),
  getDeltaQpfColor: vi.fn(),
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

function makeDetailWithFields(version: VersionListItem): VersionDetail {
  return {
    ...makeDetail(version),
    field_urls: {
      qpf_before: '/api/version/window-1_v001/field/qpf_before',
      ptype_before: '/api/version/window-1_v001/field/ptype_before',
      qpf_after: '/api/version/window-1_v001/field/qpf_after',
      ptype_after: '/api/version/window-1_v001/field/ptype_after',
      delta_qpf: '/api/version/window-1_v001/field/delta_qpf',
      change_ptype: '/api/version/window-1_v001/field/change_ptype',
      touched_mask: '/api/version/window-1_v001/field/touched_mask',
      changed_mask: '/api/version/window-1_v001/field/changed_mask',
    },
  }
}

function makeDetailWithoutImages(version: VersionListItem): VersionDetail {
  return {
    ...makeDetail(version),
    image_paths: {
      before_product: null,
      after_product: null,
      delta_qpf: null,
      change_ptype: null,
      touched_mask: null,
      changed_mask: null,
    },
    before_image_path: null,
    after_image_path: null,
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
    vi.mocked(getVersionField).mockImplementation(async (_versionId, fieldName) => {
      if (fieldName === 'qpf_before' || fieldName === 'qpf_after' || fieldName === 'delta_qpf') {
        return new ArrayBuffer(GRID_ROWS * GRID_COLS * Float32Array.BYTES_PER_ELEMENT)
      }

      return new ArrayBuffer(GRID_ROWS * GRID_COLS)
    })
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

  it('VersionFieldMap 加载字段并进入 loaded 状态', async () => {
    const wrapper = mount(VersionFieldMap, {
      props: {
        versionId: 'window-1_v001',
        fieldName: 'delta_qpf',
      },
    })
    await flushPromises()

    expect(getVersionField).toHaveBeenCalledWith('window-1_v001', 'delta_qpf')
    expect(wrapper.find('[data-test="version-field-loading"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="version-field-error"]').exists()).toBe(false)
    expect(wrapper.attributes('data-loaded')).toBe('true')
  })

  it('useLinkedMaps 同步视图并可清理监听', () => {
    const callbacks: Record<string, () => void> = {}
    const removeSourceListener = vi.fn()
    const removeTargetListener = vi.fn()
    const sourceView = {
      on: vi.fn((eventName: string, callback: () => void) => {
        callbacks[eventName] = callback
        return { target: { removeEventListener: removeSourceListener }, type: eventName, listener: callback }
      }),
      getCenter: vi.fn(() => [100, 30]),
      getResolution: vi.fn(() => 0.1),
      getRotation: vi.fn(() => 0.2),
      setCenter: vi.fn(),
      setResolution: vi.fn(),
      setRotation: vi.fn(),
    }
    const targetView = {
      on: vi.fn((eventName: string) => ({
        target: { removeEventListener: removeTargetListener },
        type: eventName,
        listener: vi.fn(),
      })),
      getCenter: vi.fn(() => [90, 35]),
      getResolution: vi.fn(() => 0.2),
      getRotation: vi.fn(() => 0),
      setCenter: vi.fn(),
      setResolution: vi.fn(),
      setRotation: vi.fn(),
    }
    const sourceMap = { getView: () => sourceView }
    const targetMap = { getView: () => targetView }
    const linkedMaps = useLinkedMaps()

    linkedMaps.registerMap(sourceMap as never)
    linkedMaps.registerMap(targetMap as never)
    callbacks['change:center']()

    expect(targetView.setCenter).toHaveBeenCalledWith([100, 30])
    expect(targetView.setResolution).toHaveBeenCalledWith(0.1)
    expect(targetView.setRotation).toHaveBeenCalledWith(0.2)

    linkedMaps.cleanup()
    expect(sourceView.on).toHaveBeenCalledTimes(3)
    expect(targetView.on).toHaveBeenCalledTimes(3)
    expect(removeSourceListener).toHaveBeenCalledTimes(3)
    expect(removeTargetListener).toHaveBeenCalledTimes(3)
  })

  it('字段 URL 完整时渲染 before/after VersionFieldMap', async () => {
    const { wrapper } = mountApproval('reviewer', makeDetailWithFields(SUBMITTED_VERSION))
    await flushPromises()

    expect(wrapper.find('[data-test="field-map-comparison"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="version-field-map-before"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="version-field-map-after"]').exists()).toBe(true)
  })

  it('点击缩略图打开全屏预览', async () => {
    const { wrapper } = mountApproval('reviewer', makeDetail(SUBMITTED_VERSION))
    await flushPromises()

    await wrapper.find('[data-test="image-thumb-before_product"]').trigger('click')

    expect(wrapper.find('[data-test="image-preview"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="image-preview-img"]').attributes('src')).toBe('/images/before.png')
  })

  it('无字段和无图片时显示降级占位', async () => {
    const { wrapper } = mountApproval('reviewer', makeDetailWithoutImages(SUBMITTED_VERSION))
    await flushPromises()

    expect(wrapper.find('[data-test="field-data-empty"]').text()).toContain('字段数据不可用')
    expect(wrapper.text()).toContain('图片未生成')
    expect(wrapper.findAll('.approval-detail__image-empty')).toHaveLength(6)
  })
})
