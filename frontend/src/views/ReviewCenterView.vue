<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import AppHeader from '@/components/AppHeader.vue'
import { exportReview } from '@/api/review'
import type { MissingField, ReviewProductDetail, ReviewProductListItem } from '@/api/review'
import { TERMINAL_PLOT_STATUSES, useReviewStore } from '@/stores/reviewStore'

const reviewStore = useReviewStore()
const selectedReviewId = ref<string | null>(null)

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '等待中', value: 'pending' },
  { label: '生成中', value: 'running' },
  { label: '成功', value: 'success' },
  { label: '部分成功', value: 'partial_success' },
  { label: '失败', value: 'failed' },
  { label: '永久失败', value: 'permanently_failed' },
  { label: '已替代', value: 'superseded' },
]

const statusMeta: Record<string, { label: string; theme: string }> = {
  pending: { label: '等待中', theme: 'warning' },
  running: { label: '生成中', theme: 'primary' },
  success: { label: '成功', theme: 'success' },
  partial_success: { label: '部分成功', theme: 'warning' },
  failed: { label: '失败', theme: 'danger' },
  permanently_failed: { label: '永久失败', theme: 'danger' },
  superseded: { label: '已替代', theme: 'default' },
}

const reasonLabels: Record<string, string> = {
  file_not_found: '文件缺失',
  read_error: '读取失败',
  dimension_mismatch: '维度不匹配',
}

const currentReview = computed(() => reviewStore.currentReview)
const canRegenerate = computed(() =>
  ['failed', 'partial_success'].includes(String(currentReview.value?.plot_status ?? '')),
)
const canExport = computed(() =>
  ['success', 'partial_success'].includes(String(currentReview.value?.plot_status ?? '')),
)

const caseOptions = computed(() => makeOptions(reviewStore.reviews.map((review) => caseIdFromWindow(review.window_id))))
const windowOptions = computed(() => makeOptions(reviewStore.reviews.map((review) => review.window_id)))
const missingFields = computed(() => parseMissingFields(currentReview.value?.missing_fields_json))
const durationText = computed(() => {
  const review = currentReview.value
  if (!review?.plot_started_at || !review.plot_finished_at) {
    return '-'
  }

  const durationMs =
    new Date(review.plot_finished_at).getTime() - new Date(review.plot_started_at).getTime()
  if (!Number.isFinite(durationMs) || durationMs < 0) {
    return '-'
  }

  return `${Math.round(durationMs / 1000)} 秒`
})

function makeOptions(values: Array<string | undefined>) {
  return Array.from(new Set(values.filter(Boolean) as string[])).map((value) => ({
    label: value,
    value,
  }))
}

function caseIdFromWindow(windowId: string) {
  return windowId.match(/\d{10}/)?.[0]
}

function statusLabel(status: string) {
  return statusMeta[status]?.label ?? status
}

function statusTheme(status: string) {
  return statusMeta[status]?.theme ?? 'default'
}

function reasonLabel(reason: string) {
  return reasonLabels[reason] ?? reason
}

function formatDate(value?: string | null) {
  if (!value) {
    return '-'
  }

  return new Date(value).toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai', hour12: false })
}

function levelText(field: MissingField) {
  if (!field.level_type && field.level_value === null) {
    return '-'
  }

  return [field.level_type, field.level_value].filter((value) => value !== null && value !== undefined).join(' ')
}

function parseMissingFields(value: ReviewProductDetail['missing_fields_json']): MissingField[] {
  if (!value) {
    return []
  }

  if (Array.isArray(value)) {
    return value
  }

  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function updateFilter(key: 'case_id' | 'window_id' | 'plot_status', value: string | undefined) {
  reviewStore.setFilter(key, value)
  void reviewStore.fetchReviews()
}

async function selectReview(review: ReviewProductListItem) {
  selectedReviewId.value = review.review_id
  await reviewStore.fetchReviewDetail(review.review_id)
  syncPolling()
}

async function regenerateSelected() {
  const review = currentReview.value
  if (!review) {
    return
  }

  try {
    const response = await reviewStore.generateReview({
      window_id: review.window_id,
      version_id: review.version_id,
      template_id: review.template_id,
    })
    MessagePlugin.success('已提交重新生成任务')
    selectedReviewId.value = response.review_id
    await reviewStore.fetchReviewDetail(response.review_id)
    reviewStore.startPolling(response.review_id)
  } catch {
    // reviewStore exposes the error message for the page banner.
  }
}

async function exportSelected() {
  const review = currentReview.value
  if (!review) {
    return
  }

  try {
    await exportReview(review.review_id)
    MessagePlugin.success('复盘包已开始下载')
  } catch {
    reviewStore.error = '导出复盘包失败'
  }
}

function syncPolling() {
  const review = currentReview.value
  if (review && !TERMINAL_PLOT_STATUSES.has(String(review.plot_status))) {
    reviewStore.startPolling(review.review_id)
  } else {
    reviewStore.stopPolling()
  }
}

onMounted(async () => {
  await reviewStore.fetchReviews()
})

watch(
  () => currentReview.value?.plot_status,
  () => {
    syncPolling()
  },
)

onBeforeUnmount(() => {
  reviewStore.stopPolling()
})
</script>

<template>
  <AppHeader />
  <t-layout class="review-center">
    <t-aside class="review-center__list-panel">
      <t-card class="review-center__filters" bordered>
        <t-space direction="vertical" size="small">
          <t-select
            :model-value="reviewStore.filters.case_id"
            :options="caseOptions"
            clearable
            placeholder="全部起报时次"
            data-test="case-filter"
            @update:model-value="updateFilter('case_id', $event)"
          />
          <t-select
            :model-value="reviewStore.filters.window_id"
            :options="windowOptions"
            clearable
            placeholder="全部窗口"
            data-test="window-filter"
            @update:model-value="updateFilter('window_id', $event)"
          />
          <t-select
            :model-value="reviewStore.filters.plot_status"
            :options="statusOptions"
            clearable
            placeholder="全部状态"
            data-test="status-filter"
            @update:model-value="updateFilter('plot_status', $event)"
          />
        </t-space>
      </t-card>

      <t-loading :loading="reviewStore.loading">
        <t-list v-if="reviewStore.reviews.length > 0" class="review-center__list" data-test="review-list">
          <t-list-item
            v-for="review in reviewStore.reviews"
            :key="review.review_id"
            class="review-center__list-item"
            :class="{ 'review-center__list-item--active': review.review_id === selectedReviewId }"
          >
            <button
              class="review-center__review-button"
              type="button"
              :data-test="`review-item-${review.review_id}`"
              @click="selectReview(review)"
            >
              <span class="review-center__review-title">{{ review.template_id }}</span>
              <t-tag :theme="statusTheme(String(review.plot_status))" variant="light" data-test="status-badge">
                {{ statusLabel(String(review.plot_status)) }}
              </t-tag>
              <span class="review-center__review-meta">{{ review.window_id }}</span>
              <span class="review-center__review-meta">{{ review.version_id }}</span>
              <time>{{ formatDate(review.created_at) }}</time>
            </button>
          </t-list-item>
        </t-list>
        <t-empty v-else description="暂无复盘任务" data-test="review-empty" />
      </t-loading>
    </t-aside>

    <t-content class="review-center__detail-panel">
      <div v-if="reviewStore.error" class="review-center__error" data-test="review-error">
        {{ reviewStore.error }}
      </div>

      <section v-if="currentReview" class="review-detail" data-test="review-detail">
        <header class="review-detail__header">
          <div>
            <h1>{{ currentReview.review_id }}</h1>
            <p>{{ currentReview.window_id }} / {{ currentReview.version_id }}</p>
          </div>
          <t-tag :theme="statusTheme(String(currentReview.plot_status))" variant="light" data-test="detail-status">
            {{ statusLabel(String(currentReview.plot_status)) }}
          </t-tag>
        </header>

        <div class="review-detail__actions">
          <t-button
            v-if="canRegenerate"
            theme="warning"
            data-test="regenerate-button"
            @click="regenerateSelected"
          >
            重新生成
          </t-button>
          <t-button
            v-if="canExport"
            theme="primary"
            data-test="export-button"
            @click="exportSelected"
          >
            导出复盘包
          </t-button>
        </div>

        <t-card title="复盘合成图" bordered>
          <t-image
            v-if="currentReview.image_path"
            class="review-detail__image"
            :src="currentReview.image_path"
            fit="contain"
            data-test="review-image"
          />
          <t-empty v-else description="复盘图尚未生成" data-test="image-empty" />
        </t-card>

        <div class="review-detail__grid">
          <t-card title="面板统计" bordered>
            <dl class="review-detail__stats" data-test="panel-summary">
              <div>
                <dt>总面板</dt>
                <dd>{{ currentReview.total_panels ?? '-' }}</dd>
              </div>
              <div>
                <dt>成功</dt>
                <dd>{{ currentReview.success_panels ?? '-' }}</dd>
              </div>
              <div>
                <dt>跳过</dt>
                <dd>{{ currentReview.skipped_panels ?? '-' }}</dd>
              </div>
            </dl>
          </t-card>

          <t-card title="绘图耗时" bordered>
            <dl class="review-detail__timing" data-test="plot-timing">
              <div>
                <dt>开始</dt>
                <dd>{{ formatDate(currentReview.plot_started_at) }}</dd>
              </div>
              <div>
                <dt>结束</dt>
                <dd>{{ formatDate(currentReview.plot_finished_at) }}</dd>
              </div>
              <div>
                <dt>耗时</dt>
                <dd>{{ durationText }}</dd>
              </div>
            </dl>
          </t-card>
        </div>

        <t-card v-if="missingFields.length > 0" title="缺失字段" bordered>
          <table class="review-detail__missing" data-test="missing-fields">
            <thead>
              <tr>
                <th>变量</th>
                <th>层次</th>
                <th>时效</th>
                <th>原因</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="field in missingFields" :key="`${field.variable_name ?? field.name}-${field.lead_hour}-${field.reason}`">
                <td>{{ field.variable_name ?? field.name }}</td>
                <td>{{ levelText(field) }}</td>
                <td>{{ field.lead_hour ?? '-' }}</td>
                <td>{{ reasonLabel(field.reason) }}</td>
              </tr>
            </tbody>
          </table>
        </t-card>

        <t-card v-if="currentReview.error_log_path" title="错误日志" bordered>
          <a class="review-detail__log-link" :href="currentReview.error_log_path" target="_blank" rel="noreferrer">
            {{ currentReview.error_log_path }}
          </a>
        </t-card>
      </section>

      <t-empty v-else description="请选择左侧复盘任务" data-test="empty-detail" />
    </t-content>
  </t-layout>
</template>

<style scoped>
.review-center {
  display: grid;
  grid-template-columns: 380px minmax(0, 1fr);
  min-width: 980px;
  min-height: calc(100vh - var(--top-nav-height));
  background: var(--page-bg);
}

.review-center__list-panel,
.review-center__detail-panel {
  min-height: 0;
  background: var(--card-bg);
}

.review-center__list-panel {
  border-right: 1px solid var(--color-border);
  padding: 16px;
  overflow: auto;
}

.review-center__detail-panel {
  padding: 20px 24px;
  overflow: auto;
}

.review-center__filters {
  margin-bottom: 14px;
}

.review-center__list {
  display: grid;
  gap: 8px;
}

.review-center__list-item {
  list-style: none;
}

.review-center__review-button {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 6px 10px;
  width: 100%;
  min-height: 104px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  background: var(--card-bg);
  padding: 12px;
  color: var(--text-primary);
  cursor: pointer;
  text-align: left;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease;
}

.review-center__list-item--active .review-center__review-button,
.review-center__review-button:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-bg);
}

.review-center__review-title {
  min-width: 0;
  overflow: hidden;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.review-center__review-meta,
.review-center__review-button time {
  grid-column: 1 / -1;
  color: var(--text-secondary);
  font-size: var(--font-size-caption);
  line-height: 20px;
}

.review-center__review-button time {
  color: var(--color-neutral);
}

.review-center__error {
  margin-bottom: 16px;
  border: 1px solid var(--color-danger-bg);
  border-radius: var(--radius-card);
  background: var(--color-danger-bg);
  padding: 10px 12px;
  color: var(--color-danger);
  font-size: 14px;
}

.review-detail {
  display: grid;
  gap: 16px;
}

.review-detail__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid var(--color-border);
  padding-bottom: 16px;
}

.review-detail__header h1 {
  margin: 0 0 6px;
  font-size: var(--font-size-title);
  line-height: var(--line-height-title);
}

.review-detail__header p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 22px;
}

.review-detail__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.review-detail__image {
  display: block;
  width: 100%;
  max-height: 560px;
  background: var(--color-neutral-bg);
}

.review-detail__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.review-detail__stats,
.review-detail__timing {
  display: grid;
  gap: 10px;
  margin: 0;
}

.review-detail__stats div,
.review-detail__timing div {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 12px;
  align-items: baseline;
}

.review-detail__stats dt,
.review-detail__timing dt {
  color: var(--color-neutral);
  font-size: var(--font-size-caption);
  line-height: 20px;
}

.review-detail__stats dd,
.review-detail__timing dd {
  margin: 0;
  color: var(--text-primary);
  font-size: 14px;
  line-height: 22px;
}

.review-detail__missing {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.review-detail__missing th,
.review-detail__missing td {
  border-bottom: 1px solid var(--color-border);
  padding: 10px 8px;
  text-align: left;
}

.review-detail__missing th {
  color: var(--text-secondary);
  font-weight: 600;
}

.review-detail__log-link {
  color: var(--color-primary);
  font-size: 14px;
  word-break: break-all;
}
</style>
