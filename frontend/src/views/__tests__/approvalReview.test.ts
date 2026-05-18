import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/authStore'
import { useVersionStore } from '@/stores/versionStore'
import { useReviewStore } from '@/stores/reviewStore'
import ApprovalView from '@/views/ApprovalView.vue'
import ReviewCenterView from '@/views/ReviewCenterView.vue'
import type { VersionDetail, VersionListItem } from '@/api/version'
import type { ReviewProductDetail, ReviewProductListItem } from '@/api/review'

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue-router')>()
  return {
    ...actual,
    useRouter: () => ({
      push: vi.fn(),
      currentRoute: { value: { path: '/approval' } },
    }),
  }
})

vi.mock('@/composables/useLinkedMaps', () => ({
  useLinkedMaps: () => ({
    registerMap: vi.fn(),
    cleanup: vi.fn(),
  }),
}))

// Polyfill ResizeObserver for jsdom (used by OpenLayers)
if (typeof globalThis.ResizeObserver === 'undefined') {
  globalThis.ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  } as unknown as typeof ResizeObserver
}

vi.mock('@/api/version', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/version')>()
  return {
    ...actual,
    getVersions: vi.fn().mockResolvedValue([]),
    getVersionDetail: vi.fn().mockResolvedValue(null),
    getVersionField: vi.fn().mockResolvedValue(new ArrayBuffer(0)),
  }
})

vi.mock('@/api/review', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/review')>()
  return {
    ...actual,
    getReviews: vi.fn(() => Promise.resolve({ code: 'ok', message: '', data: [], trace_id: '' })),
    getReviewDetail: vi.fn(() => Promise.resolve({ code: 'ok', message: '', data: null, trace_id: '' })),
    exportReview: vi.fn(() => Promise.resolve(undefined)),
  }
})

function makeVersionListItem(overrides: Partial<VersionListItem> = {}): VersionListItem {
  return {
    version_id: 'v-001',
    window_id: 'w-001',
    version_no: 1,
    base_version_id: null,
    status: 'submitted',
    has_images: false,
    created_by: 'user1',
    created_at: '2026-01-01T00:00:00Z',
    ...overrides,
  }
}

function makeVersionDetail(overrides: Partial<VersionDetail> = {}): VersionDetail {
  return {
    version_id: 'v-001',
    window_id: 'w-001',
    version_no: 1,
    base_version_id: null,
    status: 'submitted',
    has_images: false,
    created_by: 'user1',
    created_at: '2026-01-01T00:00:00Z',
    session_id: 's-001',
    image_paths: {},
    field_urls: {},
    operation_summary: { operation_count: 5, affected_grid_count: 100 },
    approval_history: [],
    before_image_path: null,
    after_image_path: null,
    delta_qpf_image_path: null,
    change_ptype_image_path: null,
    touched_mask_image_path: null,
    changed_mask_image_path: null,
    review_image_path: null,
    ...overrides,
  }
}

function makeReviewListItem(overrides: Partial<ReviewProductListItem> = {}): ReviewProductListItem {
  return {
    review_id: 'r-001',
    window_id: 'w-001',
    version_id: 'v-001',
    template_id: 'tpl-001',
    plot_status: 'success',
    image_path: null,
    total_panels: 4,
    success_panels: 4,
    skipped_panels: 0,
    created_at: '2026-01-01T00:00:00Z',
    ...overrides,
  }
}

function makeReviewDetail(overrides: Partial<ReviewProductDetail> = {}): ReviewProductDetail {
  return {
    review_id: 'r-001',
    window_id: 'w-001',
    version_id: 'v-001',
    template_id: 'tpl-001',
    image_path: '/img/composite.png',
    plot_status: 'success',
    total_panels: 4,
    success_panels: 4,
    skipped_panels: 0,
    created_at: '2026-01-01T00:00:00Z',
    plot_started_at: '2026-01-01T00:00:00Z',
    plot_finished_at: '2026-01-01T00:01:00Z',
    ...overrides,
  }
}

// Helper: query DOM directly to avoid VTU fragment issues
function q(selector: string): Element | null {
  return document.body.querySelector(selector)
}
function qAll(selector: string): NodeListOf<Element> {
  return document.body.querySelectorAll(selector)
}

let wrapper: VueWrapper

const extraStubs = {
  VersionFieldMap: { template: '<div data-test="version-field-map-stub"></div>' },
}

function mountApproval(): VueWrapper {
  const pinia = createPinia()
  setActivePinia(pinia)
  const el = document.createElement('div')
  document.body.appendChild(el)
  wrapper = mount(ApprovalView, {
    global: { plugins: [pinia], stubs: extraStubs },
    attachTo: el,
  })
  return wrapper
}

function mountApprovalWithRole(role: string) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const authStore = useAuthStore()
  authStore.user = { user_id: 'u1', username: 'test', display_name: 'Test', role }
  authStore.token = 'tok'
  const versionStore = useVersionStore()
  const el = document.createElement('div')
  document.body.appendChild(el)
  wrapper = mount(ApprovalView, {
    global: { plugins: [pinia], stubs: extraStubs },
    attachTo: el,
  })
  return { wrapper, authStore, versionStore }
}

function mountReview(): VueWrapper {
  const pinia = createPinia()
  setActivePinia(pinia)
  const el = document.createElement('div')
  document.body.appendChild(el)
  wrapper = mount(ReviewCenterView, {
    global: { plugins: [pinia] },
    attachTo: el,
  })
  return wrapper
}

function mountReviewWithStore() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const reviewStore = useReviewStore()
  const el = document.createElement('div')
  document.body.appendChild(el)
  wrapper = mount(ReviewCenterView, {
    global: { plugins: [pinia] },
    attachTo: el,
  })
  return { wrapper, reviewStore }
}

afterEach(() => {
  wrapper?.unmount()
  document.body.innerHTML = ''
})

/* ===================================================================
   ApprovalView
   =================================================================== */
describe('ApprovalView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  /* ---------- 3-Column Layout (task 6.1) ---------- */

  describe('3-column layout', () => {
    it('renders 3-column structure with two asides and one main', () => {
      mountApproval()
      // t-aside -> aside, t-content -> main (per global stubs)
      const asides = qAll('aside')
      const mains = qAll('main')
      expect(asides.length).toBe(2)
      expect(mains.length).toBe(1)
    })

    it('left aside has approval-view__left class', () => {
      mountApproval()
      expect(q('.approval-view__left')).toBeTruthy()
    })

    it('center has approval-view__center class', () => {
      mountApproval()
      expect(q('.approval-view__center')).toBeTruthy()
    })

    it('right aside has approval-view__right class', () => {
      mountApproval()
      expect(q('.approval-view__right')).toBeTruthy()
    })
  })

  /* ---------- Left panel: version list ---------- */

  describe('left panel content', () => {
    it('renders version list in left panel', async () => {
      const { versionStore } = mountApprovalWithRole('reviewer')
      versionStore.versions = [makeVersionListItem()]
      await flushPromises()
      const left = q('.approval-view__left')
      expect(left?.querySelector('[data-test="version-list"]')).toBeTruthy()
    })

    it('renders status tabs in left panel', () => {
      mountApproval()
      const left = q('.approval-view__left')
      expect(left?.querySelector('[data-test="tabs"]')).toBeTruthy()
    })

    it('renders window filter in left panel', () => {
      mountApproval()
      const left = q('.approval-view__left')
      expect(left?.querySelector('[data-test="window-filter"]')).toBeTruthy()
    })
  })

  /* ---------- Center panel: map + images ---------- */

  describe('center panel content', () => {
    it('shows t-empty when no version selected', () => {
      mountApproval()
      const empty = q('[data-test="empty-detail"]')
      expect(empty).toBeTruthy()
      expect(empty?.textContent).toContain('请选择左侧版本查看详情')
    })

    it('shows map comparison when version selected with field URLs', async () => {
      const { versionStore } = mountApprovalWithRole('reviewer')
      versionStore.currentVersion = makeVersionDetail({
        field_urls: {
          qpf_before: '/f/qpf_b',
          ptype_before: '/f/pt_b',
          qpf_after: '/f/qpf_a',
          ptype_after: '/f/pt_a',
        },
      })
      await flushPromises()
      expect(q('[data-test="field-map-comparison"]')).toBeTruthy()
    })

    it('renders image thumbnails in center panel', async () => {
      const { versionStore } = mountApprovalWithRole('reviewer')
      versionStore.currentVersion = makeVersionDetail({
        image_paths: { before_product: '/img/before.png' },
      })
      await flushPromises()
      expect(q('[data-test="image-thumb-before_product"]')).toBeTruthy()
    })
  })

  /* ---------- Right panel: metadata + timeline + actions ---------- */

  describe('right panel content', () => {
    it('renders version detail in right panel', async () => {
      const { versionStore } = mountApprovalWithRole('reviewer')
      versionStore.currentVersion = makeVersionDetail()
      await flushPromises()
      const right = q('.approval-view__right')
      expect(right?.querySelector('[data-test="version-detail"]')).toBeTruthy()
    })

    it('renders approval timeline in right panel when history exists', async () => {
      const { versionStore } = mountApprovalWithRole('reviewer')
      versionStore.currentVersion = makeVersionDetail({
        approval_history: [
          {
            approval_id: 'a-1',
            version_id: 'v-001',
            reviewer_id: 'admin',
            action: 'approve',
            comment: null,
            reviewed_at: '2026-01-01T01:00:00Z',
          },
        ],
      })
      await flushPromises()
      const right = q('.approval-view__right')
      const timeline = right?.querySelector('ol')
      expect(timeline).toBeTruthy()
      expect(timeline?.textContent).toContain('admin')
    })

    it('shows t-empty when no approval history', async () => {
      const { versionStore } = mountApprovalWithRole('reviewer')
      versionStore.currentVersion = makeVersionDetail({ approval_history: [] })
      await flushPromises()
      const emptyHistory = q('[data-test="empty-history"]')
      expect(emptyHistory).toBeTruthy()
      expect(emptyHistory?.textContent).toContain('暂无审核记录')
    })
  })

  /* ---------- Action buttons (task 6.2) ---------- */

  describe('review action buttons', () => {
    it('shows approve and reject for submitted status with reviewer role', async () => {
      const { versionStore } = mountApprovalWithRole('reviewer')
      versionStore.currentVersion = makeVersionDetail({ status: 'submitted' })
      await flushPromises()
      expect(q('[data-test="approve-button"]')).toBeTruthy()
      expect(q('[data-test="reject-button"]')).toBeTruthy()
    })

    it('shows release for approved status with reviewer role', async () => {
      const { versionStore } = mountApprovalWithRole('reviewer')
      versionStore.currentVersion = makeVersionDetail({ status: 'approved' })
      await flushPromises()
      expect(q('[data-test="release-button"]')).toBeTruthy()
    })

    it('hides actions for non-reviewer roles', async () => {
      const { versionStore } = mountApprovalWithRole('forecaster')
      versionStore.currentVersion = makeVersionDetail({ status: 'submitted' })
      await flushPromises()
      expect(q('[data-test="approve-button"]')).toBeNull()
      expect(q('[data-test="reject-button"]')).toBeNull()
    })

    it('hides approve/reject for non-submitted status', async () => {
      const { versionStore } = mountApprovalWithRole('reviewer')
      versionStore.currentVersion = makeVersionDetail({ status: 'approved' })
      await flushPromises()
      expect(q('[data-test="approve-button"]')).toBeNull()
      expect(q('[data-test="reject-button"]')).toBeNull()
    })

    it('hides release for non-approved status', async () => {
      const { versionStore } = mountApprovalWithRole('reviewer')
      versionStore.currentVersion = makeVersionDetail({ status: 'submitted' })
      await flushPromises()
      expect(q('[data-test="release-button"]')).toBeNull()
    })
  })

  /* ---------- Reject dialog ---------- */

  describe('reject dialog', () => {
    it('requires comment when rejecting', async () => {
      const { versionStore } = mountApprovalWithRole('reviewer')
      versionStore.currentVersion = makeVersionDetail({ status: 'submitted' })
      await flushPromises()

      // Open reject dialog
      const rejectBtn = q('[data-test="reject-button"]') as HTMLElement
      rejectBtn.click()
      await flushPromises()

      // Confirm without comment
      const confirmBtn = q('[data-test="reject-confirm"]') as HTMLElement
      confirmBtn.click()
      await flushPromises()

      expect(q('[data-test="reject-error"]')).toBeTruthy()
      expect(q('[data-test="reject-error"]')?.textContent).toContain('退回必须填写审核意见')
    })
  })

  /* ---------- Empty states (task 6.4) ---------- */

  describe('empty states use t-empty', () => {
    it('uses t-empty with Chinese text for no version selected', () => {
      mountApproval()
      const emptyEl = q('[data-test="empty-detail"]')
      expect(emptyEl).toBeTruthy()
      expect(emptyEl?.textContent).toContain('请选择左侧版本查看详情')
    })

    it('uses t-empty for empty approval history', async () => {
      const { versionStore } = mountApprovalWithRole('reviewer')
      versionStore.currentVersion = makeVersionDetail({ approval_history: [] })
      await flushPromises()
      const historyEmpty = q('[data-test="empty-history"]')
      expect(historyEmpty).toBeTruthy()
      expect(historyEmpty?.textContent).toContain('暂无审核记录')
    })
  })
})

/* ===================================================================
   ReviewCenterView
   =================================================================== */
describe('ReviewCenterView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  /* ---------- 3-Column Layout (task 6.3) ---------- */

  describe('3-column layout', () => {
    it('renders two asides and one main', () => {
      mountReview()
      expect(qAll('aside').length).toBe(2)
      expect(qAll('main').length).toBe(1)
    })

    it('left panel has review-center__left class', () => {
      mountReview()
      expect(q('.review-center__left')).toBeTruthy()
    })

    it('center panel has review-center__center class', () => {
      mountReview()
      expect(q('.review-center__center')).toBeTruthy()
    })

    it('right panel has review-center__right class', () => {
      mountReview()
      expect(q('.review-center__right')).toBeTruthy()
    })
  })

  /* ---------- Left panel: filter card ---------- */

  describe('left panel content', () => {
    it('renders filter selects in left panel', () => {
      mountReview()
      const left = q('.review-center__left')
      expect(left?.querySelector('[data-test="case-filter"]')).toBeTruthy()
      expect(left?.querySelector('[data-test="window-filter"]')).toBeTruthy()
      expect(left?.querySelector('[data-test="status-filter"]')).toBeTruthy()
    })

    it('renders review list in left panel', async () => {
      const { reviewStore } = mountReviewWithStore()
      // Wait for onMounted fetchReviews to complete first
      await flushPromises()
      // Then set reviews and let reactivity update the DOM
      reviewStore.reviews = [makeReviewListItem()]
      await flushPromises()
      const left = q('.review-center__left')
      expect(left?.querySelector('[data-test="review-list"]')).toBeTruthy()
    })
  })

  /* ---------- Center panel: 2x2 image grid ---------- */

  describe('center panel content', () => {
    it('shows t-empty when no review selected', () => {
      mountReview()
      const empty = q('[data-test="empty-detail"]')
      expect(empty).toBeTruthy()
      expect(empty?.textContent).toContain('请选择左侧复盘任务')
    })

    it('renders 2x2 image grid with 4 cells when review selected', async () => {
      const { reviewStore } = mountReviewWithStore()
      reviewStore.currentReview = makeReviewDetail()
      await flushPromises()
      const grid = q('[data-test="review-image-grid"]')
      expect(grid).toBeTruthy()
      expect(grid?.querySelectorAll('.review-center__image-cell').length).toBe(4)
    })

    it('image grid shows placeholder for missing images', async () => {
      const { reviewStore } = mountReviewWithStore()
      reviewStore.currentReview = makeReviewDetail()
      await flushPromises()
      const grid = q('[data-test="review-image-grid"]')
      expect(grid?.textContent).toContain('图片未生成')
    })

    it('renders composite image in center', async () => {
      const { reviewStore } = mountReviewWithStore()
      reviewStore.currentReview = makeReviewDetail({ image_path: '/img/comp.png' })
      await flushPromises()
      expect(q('[data-test="review-image"]')).toBeTruthy()
    })
  })

  /* ---------- Right panel: review info ---------- */

  describe('right panel content', () => {
    it('renders review detail in right panel', async () => {
      const { reviewStore } = mountReviewWithStore()
      reviewStore.currentReview = makeReviewDetail()
      await flushPromises()
      const right = q('.review-center__right')
      expect(right?.querySelector('[data-test="review-detail"]')).toBeTruthy()
    })

    it('renders panel summary in right panel', async () => {
      const { reviewStore } = mountReviewWithStore()
      reviewStore.currentReview = makeReviewDetail()
      await flushPromises()
      const right = q('.review-center__right')
      expect(right?.querySelector('[data-test="panel-summary"]')).toBeTruthy()
    })

    it('renders plot timing in right panel', async () => {
      const { reviewStore } = mountReviewWithStore()
      reviewStore.currentReview = makeReviewDetail()
      await flushPromises()
      const right = q('.review-center__right')
      expect(right?.querySelector('[data-test="plot-timing"]')).toBeTruthy()
    })

    it('renders regenerate button for failed status', async () => {
      const { reviewStore } = mountReviewWithStore()
      reviewStore.currentReview = makeReviewDetail({ plot_status: 'failed' })
      await flushPromises()
      const right = q('.review-center__right')
      expect(right?.querySelector('[data-test="regenerate-button"]')).toBeTruthy()
    })

    it('renders export button for success status', async () => {
      const { reviewStore } = mountReviewWithStore()
      reviewStore.currentReview = makeReviewDetail({ plot_status: 'success' })
      await flushPromises()
      expect(q('[data-test="export-button"]')).toBeTruthy()
    })
  })

  /* ---------- Status tags (task 6.5) ---------- */

  describe('status tag themes', () => {
    it('uses correct theme for various statuses', async () => {
      const { reviewStore } = mountReviewWithStore()
      await flushPromises()

      // Pending -> warning
      reviewStore.reviews = [makeReviewListItem({ plot_status: 'pending' })]
      await flushPromises()
      let badge = q('[data-test="status-badge"]')
      expect(badge).toBeTruthy()
      expect(badge!.getAttribute('data-theme')).toBe('warning')

      // Success -> success
      reviewStore.reviews = [makeReviewListItem({ plot_status: 'success' })]
      await flushPromises()
      badge = q('[data-test="status-badge"]')
      expect(badge!.getAttribute('data-theme')).toBe('success')

      // Failed -> danger
      reviewStore.reviews = [makeReviewListItem({ plot_status: 'failed' })]
      await flushPromises()
      badge = q('[data-test="status-badge"]')
      expect(badge!.getAttribute('data-theme')).toBe('danger')
    })
  })
})
