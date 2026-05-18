import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import {
  generateReview,
  getPlotTaskStatus,
  getReviewDetail,
  getReviews,
} from '@/api/review'
import { useReviewStore } from '@/stores/reviewStore'
import type { ApiResponse, ReviewProductDetail, ReviewProductListItem } from '@/api/review'

vi.mock('@/api/review', () => ({
  getReviews: vi.fn(),
  getReviewDetail: vi.fn(),
  getPlotTaskStatus: vi.fn(),
  generateReview: vi.fn(),
  exportReview: vi.fn(),
}))

const REVIEW: ReviewProductListItem = {
  review_id: 'review-1',
  window_id: '2026051708_w024_048',
  version_id: 'version-1',
  template_id: 'snow_phase_review_v1',
  plot_status: 'pending',
  image_path: null,
  total_panels: null,
  success_panels: null,
  skipped_panels: null,
  created_at: '2026-05-17T00:00:00Z',
}

const DETAIL: ReviewProductDetail = {
  ...REVIEW,
  plot_status: 'success',
  image_path: '/images/review.png',
  attempt: 1,
  max_retries: 3,
  plot_started_at: '2026-05-17T00:00:00Z',
  plot_finished_at: '2026-05-17T00:00:10Z',
  total_panels: 6,
  success_panels: 6,
  skipped_panels: 0,
  missing_fields_json: null,
  error_log_path: null,
}

function ok<T>(data: T): ApiResponse<T> {
  return {
    code: 'OK',
    message: 'success',
    data,
    trace_id: 'trace-1',
  }
}

describe('reviewStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.useRealTimers()
    vi.mocked(getReviews).mockReset()
    vi.mocked(getReviewDetail).mockReset()
    vi.mocked(getPlotTaskStatus).mockReset()
    vi.mocked(generateReview).mockReset()
  })

  it('fetchReviews populates reviews list', async () => {
    vi.mocked(getReviews).mockResolvedValue(ok([REVIEW]))

    const store = useReviewStore()
    await store.fetchReviews({ case_id: '2026051708', plot_status: 'pending' })

    expect(getReviews).toHaveBeenCalledWith({
      case_id: '2026051708',
      window_id: undefined,
      plot_status: 'pending',
    })
    expect(store.reviews).toEqual([REVIEW])
    expect(store.loading).toBe(false)
  })

  it('setFilter updates filter state', () => {
    const store = useReviewStore()

    store.setFilter('window_id', 'window-1')
    store.setFilter('plot_status', '')

    expect(store.filters.window_id).toBe('window-1')
    expect(store.filters.plot_status).toBeUndefined()
  })

  it('polling starts and stops when terminal status is returned', async () => {
    vi.useFakeTimers()
    vi.mocked(getPlotTaskStatus).mockResolvedValue(ok(DETAIL))

    const store = useReviewStore()
    store.reviews = [REVIEW]
    store.startPolling('review-1')

    expect(store.pollingTimer).not.toBeNull()

    await vi.advanceTimersByTimeAsync(3000)

    expect(getPlotTaskStatus).toHaveBeenCalledWith('review-1')
    expect(store.currentReview?.plot_status).toBe('success')
    expect(store.reviews[0].image_path).toBe('/images/review.png')
    expect(store.pollingTimer).toBeNull()
  })

  it('stopPolling clears interval', () => {
    vi.useFakeTimers()

    const store = useReviewStore()
    store.startPolling('review-1')

    expect(store.pollingTimer).not.toBeNull()
    store.stopPolling()

    expect(store.pollingTimer).toBeNull()
    vi.advanceTimersByTime(3000)
    expect(getPlotTaskStatus).not.toHaveBeenCalled()
  })
})
