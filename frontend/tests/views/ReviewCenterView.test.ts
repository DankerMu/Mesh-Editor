import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createAppRouter, routes } from '@/router'
import { exportReview } from '@/api/review'
import { useAuthStore } from '@/stores/authStore'
import { useReviewStore } from '@/stores/reviewStore'
import ReviewCenterView from '@/views/ReviewCenterView.vue'
import type { ReviewProductDetail, ReviewProductListItem } from '@/api/review'

vi.mock('@/api/review', () => ({
  getReviews: vi.fn(),
  getReviewDetail: vi.fn(),
  getPlotTaskStatus: vi.fn(),
  generateReview: vi.fn(),
  exportReview: vi.fn(),
}))

vi.mock('tdesign-vue-next', () => ({
  MessagePlugin: {
    success: vi.fn(),
  },
}))

const SUCCESS_REVIEW: ReviewProductListItem = {
  review_id: 'review-success',
  window_id: '2026051708_w024_048',
  version_id: 'version-1',
  template_id: 'snow_phase_review_v1',
  plot_status: 'success',
  image_path: '/images/review.png',
  total_panels: 6,
  success_panels: 6,
  skipped_panels: 0,
  created_at: '2026-05-17T00:00:00Z',
}

const FAILED_REVIEW: ReviewProductListItem = {
  ...SUCCESS_REVIEW,
  review_id: 'review-failed',
  version_id: 'version-2',
  plot_status: 'failed',
  image_path: null,
  total_panels: null,
  success_panels: null,
  skipped_panels: null,
}

const PARTIAL_REVIEW: ReviewProductListItem = {
  ...SUCCESS_REVIEW,
  review_id: 'review-partial',
  version_id: 'version-3',
  plot_status: 'partial_success',
  success_panels: 5,
  skipped_panels: 1,
}

function makeDetail(review: ReviewProductListItem): ReviewProductDetail {
  return {
    ...review,
    attempt: 1,
    max_retries: 3,
    plot_started_at: '2026-05-17T00:00:00Z',
    plot_finished_at: '2026-05-17T00:00:10Z',
    missing_fields_json:
      review.plot_status === 'partial_success'
        ? JSON.stringify([
            {
              variable_name: 'rh',
              level_type: 'isobaric',
              level_value: 700,
              lead_hour: 48,
              reason: 'file_not_found',
            },
          ])
        : null,
    error_log_path: review.plot_status === 'failed' ? '/logs/plot_log.txt' : null,
  }
}

function mountReviewCenter(current: ReviewProductDetail | null = makeDetail(SUCCESS_REVIEW)) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const reviewStore = useReviewStore()

  reviewStore.reviews = [SUCCESS_REVIEW, FAILED_REVIEW, PARTIAL_REVIEW]
  reviewStore.currentReview = current

  vi.spyOn(reviewStore, 'fetchReviews').mockImplementation(async () => {
    reviewStore.reviews = [SUCCESS_REVIEW, FAILED_REVIEW, PARTIAL_REVIEW]
    return reviewStore.reviews
  })
  vi.spyOn(reviewStore, 'fetchReviewDetail').mockImplementation(async (reviewId) => {
    const review = [SUCCESS_REVIEW, FAILED_REVIEW, PARTIAL_REVIEW].find(
      (item) => item.review_id === reviewId,
    )
    reviewStore.currentReview = review ? makeDetail(review) : null
    return reviewStore.currentReview as ReviewProductDetail
  })
  vi.spyOn(reviewStore, 'generateReview').mockResolvedValue({
    review_id: 'review-new',
    plot_status: 'pending',
    message: '已创建复盘任务',
  })
  vi.spyOn(reviewStore, 'startPolling').mockImplementation(() => undefined)
  vi.spyOn(reviewStore, 'stopPolling').mockImplementation(() => undefined)

  const wrapper = mount(ReviewCenterView, {
    global: {
      plugins: [pinia],
    },
  })

  return { wrapper, reviewStore }
}

describe('ReviewCenterView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('9.T1 renders review list with status badges', async () => {
    const { wrapper } = mountReviewCenter()
    await flushPromises()

    expect(wrapper.find('[data-test="review-list"]').text()).toContain('snow_phase_review_v1')
    expect(wrapper.find('[data-test="review-list"]').text()).toContain('成功')
    expect(wrapper.find('[data-test="review-list"]').text()).toContain('失败')
    expect(wrapper.findAll('[data-test="status-badge"]').length).toBeGreaterThanOrEqual(3)
  })

  it('9.T2 click review shows detail panel with composite image', async () => {
    const { wrapper, reviewStore } = mountReviewCenter(null)
    await flushPromises()

    await wrapper.find('[data-test="review-item-review-success"]').trigger('click')
    await flushPromises()

    expect(reviewStore.fetchReviewDetail).toHaveBeenCalledWith('review-success')
    expect(wrapper.find('[data-test="review-detail"]').text()).toContain('review-success')
    expect(wrapper.find('[data-test="review-image"]').attributes('src')).toBe('/images/review.png')
  })

  it('9.T3 failed review shows regenerate button and click triggers generateReview', async () => {
    const { wrapper, reviewStore } = mountReviewCenter(makeDetail(FAILED_REVIEW))
    await flushPromises()

    const button = wrapper.find('[data-test="regenerate-button"]')
    expect(button.exists()).toBe(true)

    await button.trigger('click')
    await flushPromises()

    expect(reviewStore.generateReview).toHaveBeenCalledWith({
      window_id: FAILED_REVIEW.window_id,
      version_id: FAILED_REVIEW.version_id,
      template_id: FAILED_REVIEW.template_id,
    })
  })

  it('9.T4 success review shows export package button', async () => {
    const { wrapper } = mountReviewCenter(makeDetail(SUCCESS_REVIEW))
    await flushPromises()

    const button = wrapper.find('[data-test="export-button"]')
    expect(button.exists()).toBe(true)

    await button.trigger('click')
    expect(exportReview).toHaveBeenCalledWith('review-success')
  })

  it('9.T5 partial_success shows missing fields list', async () => {
    const { wrapper } = mountReviewCenter(makeDetail(PARTIAL_REVIEW))
    await flushPromises()

    const missingFields = wrapper.find('[data-test="missing-fields"]')
    expect(missingFields.text()).toContain('rh')
    expect(missingFields.text()).toContain('isobaric 700')
    expect(missingFields.text()).toContain('文件缺失')
  })

  it('9.T6 route /review requires auth', async () => {
    const reviewRoute = routes.find((route) => route.path === '/review')
    expect(reviewRoute?.meta?.requiresAuth).toBe(true)

    setActivePinia(createPinia())
    const router = createAppRouter()
    await router.push('/review')
    expect(router.currentRoute.value.path).toBe('/login')

    const authStore = useAuthStore()
    authStore.token = 'jwt-token'
    authStore.user = {
      user_id: 'viewer-1',
      username: 'viewer',
      display_name: 'Viewer',
      role: 'viewer',
    }

    await router.push('/review')
    expect(router.currentRoute.value.path).toBe('/review')
  })
})
