import { defineStore } from 'pinia'
import {
  generateReview as generateReviewApi,
  getPlotTaskStatus,
  getReviewDetail,
  getReviews,
} from '@/api/review'
import type {
  ReviewGenerateParams,
  ReviewGenerateResponse,
  ReviewListParams,
  ReviewProductDetail,
  ReviewProductListItem,
} from '@/api/review'

export interface ReviewFilters {
  case_id?: string
  window_id?: string
  plot_status?: string
}

type PollingTimer = ReturnType<typeof setInterval> | null

export const TERMINAL_PLOT_STATUSES = new Set([
  'success',
  'partial_success',
  'failed',
  'permanently_failed',
  'superseded',
])

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : '复盘操作失败'
}

function toApiFilters(filters: ReviewFilters): ReviewListParams {
  return {
    case_id: filters.case_id || undefined,
    window_id: filters.window_id || undefined,
    plot_status: filters.plot_status || undefined,
  }
}

function mergeReview(
  reviews: ReviewProductListItem[],
  detail: ReviewProductDetail,
): ReviewProductListItem[] {
  return reviews.map((review) =>
    review.review_id === detail.review_id
      ? {
          ...review,
          plot_status: detail.plot_status,
          image_path: detail.image_path,
          total_panels: detail.total_panels,
          success_panels: detail.success_panels,
          skipped_panels: detail.skipped_panels,
        }
      : review,
  )
}

export const useReviewStore = defineStore('review', {
  state: () => ({
    reviews: [] as ReviewProductListItem[],
    currentReview: null as ReviewProductDetail | null,
    filters: {
      case_id: undefined,
      window_id: undefined,
      plot_status: undefined,
    } as ReviewFilters,
    loading: false,
    error: null as string | null,
    pollingTimer: null as PollingTimer,
  }),
  actions: {
    async fetchReviews(filters?: ReviewFilters) {
      if (filters) {
        this.filters = { ...this.filters, ...filters }
      }

      this.loading = true
      this.error = null

      try {
        const response = await getReviews(toApiFilters(this.filters))
        this.reviews = response.data
        return this.reviews
      } catch (error) {
        this.error = getErrorMessage(error)
        throw error
      } finally {
        this.loading = false
      }
    },
    async fetchReviewDetail(reviewId: string) {
      this.loading = true
      this.error = null

      try {
        const response = await getReviewDetail(reviewId)
        this.currentReview = response.data
        this.reviews = mergeReview(this.reviews, response.data)
        return this.currentReview
      } catch (error) {
        this.error = getErrorMessage(error)
        throw error
      } finally {
        this.loading = false
      }
    },
    setFilter(key: keyof ReviewFilters, value: string | undefined) {
      this.filters[key] = value || undefined
    },
    async generateReview(params: ReviewGenerateParams): Promise<ReviewGenerateResponse> {
      this.loading = true
      this.error = null

      try {
        const response = await generateReviewApi(params)
        await this.fetchReviews()
        return response.data
      } catch (error) {
        this.error = getErrorMessage(error)
        throw error
      } finally {
        this.loading = false
      }
    },
    startPolling(reviewId: string) {
      this.stopPolling()

      this.pollingTimer = setInterval(() => {
        void getPlotTaskStatus(reviewId)
          .then((response) => {
            const detail = response.data
            this.currentReview = detail
            this.reviews = mergeReview(this.reviews, detail)

            if (TERMINAL_PLOT_STATUSES.has(String(detail.plot_status))) {
              this.stopPolling()
            }
          })
          .catch((error: unknown) => {
            this.error = getErrorMessage(error)
            this.stopPolling()
          })
      }, 3000)
    },
    stopPolling() {
      if (this.pollingTimer) {
        clearInterval(this.pollingTimer)
        this.pollingTimer = null
      }
    },
  },
})
