<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type Map from 'ol/Map'
import VersionFieldMap from '@/components/approval/VersionFieldMap.vue'
import { useLinkedMaps } from '@/composables/useLinkedMaps'
import { useAuthStore } from '@/stores/authStore'
import { useVersionStore } from '@/stores/versionStore'
import { useWindowStore } from '@/stores/windowStore'
import type { VersionDetail, VersionFieldName, VersionListItem } from '@/api/version'

const versionStore = useVersionStore()
const windowStore = useWindowStore()
const authStore = useAuthStore()

const activeStatus = ref('')
const rejectDialogVisible = ref(false)
const releaseDialogVisible = ref(false)
const rejectComment = ref('')
const rejectError = ref('')
const previewImage = ref<{ src: string; label: string } | null>(null)
const linkedMaps = useLinkedMaps()

const statusTabs = [
  { value: '', label: '全部' },
  { value: 'submitted', label: '待审核' },
  { value: 'approved', label: '已通过' },
  { value: 'rejected', label: '已退回' },
  { value: 'released', label: '已发布' },
]

const statusMeta: Record<string, { label: string; theme: string }> = {
  draft: { label: '草稿', theme: 'default' },
  submitted: { label: '待审核', theme: 'warning' },
  approved: { label: '已通过', theme: 'success' },
  rejected: { label: '已退回', theme: 'danger' },
  released: { label: '已发布', theme: 'primary' },
  superseded: { label: '已替代', theme: 'default' },
}

const imageItems: Array<{ key: string; label: string }> = [
  { key: 'before_product', label: '订正前' },
  { key: 'after_product', label: '订正后' },
  { key: 'delta_qpf', label: '降水差值' },
  { key: 'change_ptype', label: '相态变化' },
  { key: 'touched_mask', label: '触达范围' },
  { key: 'changed_mask', label: '改变范围' },
]

const derivedFieldItems: Array<{ fieldName: VersionFieldName; label: string }> = [
  { fieldName: 'delta_qpf', label: '降水差值' },
  { fieldName: 'change_ptype', label: '相态变化' },
  { fieldName: 'touched_mask', label: '触达范围' },
  { fieldName: 'changed_mask', label: '改变范围' },
]

const canOperate = computed(() => ['admin', 'reviewer'].includes(authStore.role ?? ''))
const selectedVersion = computed(() => versionStore.currentVersion)
const selectedVersionId = computed(() => selectedVersion.value?.version_id ?? null)
const selectedStatus = computed(() => selectedVersion.value?.status ?? '')
const showReviewActions = computed(() => canOperate.value && selectedStatus.value === 'submitted')
const showReleaseAction = computed(() => canOperate.value && selectedStatus.value === 'approved')
const hasFieldUrls = computed(() => Boolean(Object.keys(selectedVersion.value?.field_urls ?? {}).length))
const hasComparisonFields = computed(() =>
  hasFields(['qpf_before', 'ptype_before', 'qpf_after', 'ptype_after']),
)
const visibleDerivedFields = computed(() =>
  derivedFieldItems.filter((item) => Boolean(selectedVersion.value?.field_urls?.[item.fieldName])),
)

const windowOptions = computed(() =>
  windowStore.windows.map((window) => ({
    label: window.window_id,
    value: window.window_id,
  })),
)

function statusLabel(status: string) {
  return statusMeta[status]?.label ?? status
}

function statusTheme(status: string) {
  return statusMeta[status]?.theme ?? 'default'
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai', hour12: false })
}

function imageSrc(detail: VersionDetail, key: string) {
  return detail.image_paths?.[key] ?? null
}

function hasFields(fieldNames: VersionFieldName[]) {
  const urls = selectedVersion.value?.field_urls
  return Boolean(urls && fieldNames.every((fieldName) => urls[fieldName]))
}

function registerLinkedMap(map: Map) {
  linkedMaps.registerMap(map)
}

function openImagePreview(src: string | null, label: string) {
  if (!src) {
    return
  }

  previewImage.value = { src, label }
}

function closeImagePreview() {
  previewImage.value = null
}

function onPreviewKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    closeImagePreview()
  }
}

async function applyFilters(status = activeStatus.value) {
  activeStatus.value = status
  await versionStore.fetchVersions({
    status: status || undefined,
    windowId: versionStore.filters.windowId,
  })

  if (
    versionStore.currentVersion &&
    !versionStore.versions.some(
      (version) => version.version_id === versionStore.currentVersion?.version_id,
    )
  ) {
    versionStore.currentVersion = null
  }
}

async function updateWindowFilter(value: string | undefined) {
  versionStore.filters.windowId = value || undefined
  await applyFilters()
}

async function selectVersion(version: VersionListItem) {
  await versionStore.fetchVersionDetail(version.version_id)
}

async function approveSelected() {
  if (!selectedVersionId.value) {
    return
  }

  await versionStore.reviewVersion(selectedVersionId.value, 'approve')
}

function openRejectDialog() {
  rejectComment.value = ''
  rejectError.value = ''
  rejectDialogVisible.value = true
}

async function confirmReject() {
  if (!selectedVersionId.value) {
    return
  }

  const comment = rejectComment.value.trim()
  if (!comment) {
    rejectError.value = '请填写退回意见'
    return
  }

  await versionStore.reviewVersion(selectedVersionId.value, 'reject', comment)
  rejectDialogVisible.value = false
}

function openReleaseDialog() {
  releaseDialogVisible.value = true
}

async function confirmRelease() {
  if (!selectedVersionId.value) {
    return
  }

  await versionStore.releaseVersion(selectedVersionId.value)
  releaseDialogVisible.value = false
}

onMounted(async () => {
  window.addEventListener('keydown', onPreviewKeydown)
  await versionStore.fetchVersions()
})

watch(selectedVersionId, () => {
  linkedMaps.cleanup()
  closeImagePreview()
})

onBeforeUnmount(() => {
  linkedMaps.cleanup()
  window.removeEventListener('keydown', onPreviewKeydown)
})
</script>

<template>
  <t-layout class="approval-view">
    <t-aside class="approval-view__list-panel">
      <div class="approval-view__filters">
        <t-tabs :model-value="activeStatus" @update:model-value="applyFilters">
          <t-tab-panel
            v-for="tab in statusTabs"
            :key="tab.value"
            :value="tab.value"
            :label="tab.label"
          />
        </t-tabs>
        <t-select
          :model-value="versionStore.filters.windowId"
          :options="windowOptions"
          clearable
          placeholder="全部窗口"
          data-test="window-filter"
          @update:model-value="updateWindowFilter"
        />
      </div>

      <t-list class="approval-view__list" data-test="version-list">
        <t-list-item
          v-for="version in versionStore.versions"
          :key="version.version_id"
          class="approval-view__list-item"
          :class="{ 'approval-view__list-item--active': version.version_id === selectedVersionId }"
          @click="selectVersion(version)"
        >
          <button class="approval-view__version-button" type="button">
            <span class="approval-view__version-no">v{{ String(version.version_no).padStart(3, '0') }}</span>
            <t-tag :theme="statusTheme(version.status)" variant="light">
              {{ statusLabel(version.status) }}
            </t-tag>
            <span>{{ version.created_by ?? '未知用户' }}</span>
            <time>{{ formatDate(version.created_at) }}</time>
          </button>
        </t-list-item>
      </t-list>
    </t-aside>

    <t-content class="approval-view__detail-panel">
      <div v-if="versionStore.error" class="approval-view__error" data-test="version-error">
        {{ versionStore.error }}
      </div>

      <section v-if="selectedVersion" class="approval-detail" data-test="version-detail">
        <header class="approval-detail__header">
          <div>
            <h1>{{ selectedVersion.version_id }}</h1>
            <p>{{ selectedVersion.created_by ?? '未知用户' }} / {{ formatDate(selectedVersion.created_at) }}</p>
          </div>
          <t-tag :theme="statusTheme(selectedVersion.status)" variant="light">
            {{ statusLabel(selectedVersion.status) }}
          </t-tag>
        </header>

        <t-descriptions bordered :column="2">
          <t-descriptions-item label="版本号">
            {{ selectedVersion.version_no }}
          </t-descriptions-item>
          <t-descriptions-item label="窗口">
            {{ selectedVersion.window_id }}
          </t-descriptions-item>
          <t-descriptions-item label="操作数">
            {{ selectedVersion.operation_summary.operation_count }}
          </t-descriptions-item>
          <t-descriptions-item label="影响格点">
            {{ selectedVersion.operation_summary.affected_grid_count }}
          </t-descriptions-item>
        </t-descriptions>

        <section class="approval-detail__section">
          <h2>地图对比</h2>
          <div v-if="!hasFieldUrls" class="approval-detail__field-empty" data-test="field-data-empty">
            字段数据不可用
          </div>
          <template v-else>
            <div v-if="hasComparisonFields" class="approval-detail__comparison" data-test="field-map-comparison">
              <article class="approval-detail__map-card">
                <h3>订正前</h3>
                <VersionFieldMap
                  :key="`${selectedVersion.version_id}-before`"
                  :version-id="selectedVersion.version_id"
                  :field-names="{ qpf: 'qpf_before', ptype: 'ptype_before' }"
                  data-test="version-field-map-before"
                  @map-ready="registerLinkedMap"
                />
              </article>
              <article class="approval-detail__map-card">
                <h3>订正后</h3>
                <VersionFieldMap
                  :key="`${selectedVersion.version_id}-after`"
                  :version-id="selectedVersion.version_id"
                  :field-names="{ qpf: 'qpf_after', ptype: 'ptype_after' }"
                  data-test="version-field-map-after"
                  @map-ready="registerLinkedMap"
                />
              </article>
            </div>
            <div v-else class="approval-detail__field-empty" data-test="field-comparison-empty">
              字段数据不可用
            </div>

            <t-tabs v-if="visibleDerivedFields.length > 0" class="approval-detail__field-tabs">
              <t-tab-panel
                v-for="item in visibleDerivedFields"
                :key="item.fieldName"
                :value="item.fieldName"
                :label="item.label"
              >
                <VersionFieldMap
                  :key="`${selectedVersion.version_id}-${item.fieldName}`"
                  :version-id="selectedVersion.version_id"
                  :field-name="item.fieldName"
                  :data-test="`version-field-map-${item.fieldName}`"
                />
              </t-tab-panel>
            </t-tabs>
          </template>
        </section>

        <section class="approval-detail__section">
          <h2>审核图像</h2>
          <div class="approval-detail__images">
            <article v-for="item in imageItems" :key="item.key" class="approval-detail__image-card">
              <h3>{{ item.label }}</h3>
              <button
                v-if="imageSrc(selectedVersion, item.key)"
                class="approval-detail__image-button"
                type="button"
                :data-test="`image-thumb-${item.key}`"
                @click="openImagePreview(imageSrc(selectedVersion, item.key), item.label)"
              >
                <t-image :src="imageSrc(selectedVersion, item.key)" fit="contain" />
              </button>
              <div v-else class="approval-detail__image-empty">图片未生成</div>
            </article>
          </div>
        </section>

        <section class="approval-detail__section">
          <h2>审核历史</h2>
          <t-timeline v-if="selectedVersion.approval_history.length > 0">
            <t-timeline-item
              v-for="item in selectedVersion.approval_history"
              :key="item.approval_id"
              :label="formatDate(item.reviewed_at)"
            >
              {{ item.reviewer_id }} {{ item.action === 'approve' ? '通过' : '退回' }}
              <span v-if="item.comment">：{{ item.comment }}</span>
            </t-timeline-item>
          </t-timeline>
          <p v-else class="approval-detail__empty">暂无审核记录</p>
        </section>

        <footer class="approval-detail__actions">
          <t-button
            v-if="showReviewActions"
            theme="success"
            data-test="approve-button"
            @click="approveSelected"
          >
            通过
          </t-button>
          <t-button
            v-if="showReviewActions"
            theme="danger"
            data-test="reject-button"
            @click="openRejectDialog"
          >
            退回
          </t-button>
          <t-button
            v-if="showReleaseAction"
            theme="primary"
            data-test="release-button"
            @click="openReleaseDialog"
          >
            发布
          </t-button>
        </footer>
      </section>

      <div v-else class="approval-view__empty" data-test="empty-detail">请选择左侧版本</div>
    </t-content>

    <t-dialog :visible="rejectDialogVisible" header="退回版本" @update:visible="rejectDialogVisible = $event">
      <t-textarea v-model="rejectComment" placeholder="请输入退回意见" data-test="reject-comment" />
      <p v-if="rejectError" class="approval-view__dialog-error" data-test="reject-error">
        {{ rejectError }}
      </p>
      <template #footer>
        <t-button @click="rejectDialogVisible = false">取消</t-button>
        <t-button theme="danger" data-test="reject-confirm" @click="confirmReject">确认退回</t-button>
      </template>
    </t-dialog>

    <t-dialog :visible="releaseDialogVisible" header="发布版本" @update:visible="releaseDialogVisible = $event">
      <p>确认发布该版本？发布后旧版本将自动替代。</p>
      <template #footer>
        <t-button @click="releaseDialogVisible = false">取消</t-button>
        <t-button theme="primary" data-test="release-confirm" @click="confirmRelease">确认发布</t-button>
      </template>
    </t-dialog>

    <div
      v-if="previewImage"
      class="approval-preview"
      data-test="image-preview"
      role="dialog"
      aria-modal="true"
      @click.self="closeImagePreview"
    >
      <button class="approval-preview__close" type="button" data-test="image-preview-close" @click="closeImagePreview">
        关闭
      </button>
      <figure class="approval-preview__figure">
        <img :src="previewImage.src" :alt="previewImage.label" data-test="image-preview-img">
        <figcaption>{{ previewImage.label }}</figcaption>
      </figure>
    </div>
  </t-layout>
</template>

<style scoped>
.approval-view {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  min-width: 960px;
  min-height: 100vh;
  background: #eef2f7;
}

.approval-view__list-panel,
.approval-view__detail-panel {
  min-height: 0;
  background: #ffffff;
}

.approval-view__list-panel {
  border-right: 1px solid #d9e1ec;
  padding: 16px;
  overflow: auto;
}

.approval-view__detail-panel {
  padding: 20px 24px;
  overflow: auto;
}

.approval-view__filters {
  display: grid;
  gap: 12px;
  margin-bottom: 16px;
}

.approval-view__list {
  display: grid;
  gap: 8px;
}

.approval-view__list-item {
  list-style: none;
}

.approval-view__version-button {
  display: grid;
  grid-template-columns: auto auto 1fr;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-height: 72px;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  background: #ffffff;
  padding: 10px;
  color: #1d2129;
  cursor: pointer;
  text-align: left;
}

.approval-view__list-item--active .approval-view__version-button,
.approval-view__version-button:hover {
  border-color: #0052d9;
  background: #f7faff;
}

.approval-view__version-no {
  font-weight: 600;
}

.approval-view__version-button time {
  grid-column: 1 / -1;
  color: #86909c;
  font-size: 12px;
}

.approval-detail {
  display: grid;
  gap: 20px;
}

.approval-detail__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #e5e6eb;
  padding-bottom: 16px;
}

.approval-detail__header h1 {
  margin: 0 0 6px;
  font-size: 20px;
  line-height: 28px;
}

.approval-detail__header p,
.approval-detail__empty,
.approval-view__empty {
  margin: 0;
  color: #86909c;
  font-size: 14px;
  line-height: 22px;
}

.approval-detail__section h2 {
  margin: 0 0 12px;
  font-size: 16px;
  line-height: 24px;
}

.approval-detail__comparison {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.approval-detail__map-card {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.approval-detail__map-card h3 {
  margin: 0;
  color: #4e5969;
  font-size: 14px;
  line-height: 22px;
}

.approval-detail__field-tabs {
  margin-top: 12px;
}

.approval-detail__field-empty {
  display: grid;
  place-items: center;
  min-height: 160px;
  border: 1px dashed #c9cdd4;
  border-radius: 8px;
  background: #f7f8fa;
  color: #86909c;
  font-size: 14px;
}

.approval-detail__images {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.approval-detail__image-card {
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  padding: 10px;
}

.approval-detail__image-card h3 {
  margin: 0 0 8px;
  font-size: 14px;
  line-height: 22px;
}

.approval-detail__image-button {
  display: block;
  width: 100%;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  border: 0;
  border-radius: 4px;
  background: #f7f8fa;
  padding: 0;
  cursor: pointer;
}

.approval-detail__image-button :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.approval-detail__image-empty {
  display: grid;
  place-items: center;
  aspect-ratio: 4 / 3;
  border: 1px dashed #c9cdd4;
  border-radius: 4px;
  background: #f7f8fa;
  color: #86909c;
  font-size: 13px;
}

.approval-detail__actions {
  display: flex;
  gap: 10px;
  border-top: 1px solid #e5e6eb;
  padding-top: 16px;
}

.approval-view__error,
.approval-view__dialog-error {
  color: #d54941;
}

.approval-view__dialog-error {
  margin: 8px 0 0;
  font-size: 13px;
}

.approval-preview {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: grid;
  place-items: center;
  background: rgba(0, 0, 0, 0.82);
  padding: 32px;
}

.approval-preview__close {
  position: absolute;
  top: 20px;
  right: 24px;
  border: 1px solid rgba(255, 255, 255, 0.42);
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.48);
  padding: 6px 12px;
  color: #ffffff;
  cursor: pointer;
}

.approval-preview__figure {
  display: grid;
  gap: 10px;
  max-width: min(1120px, 92vw);
  max-height: 88vh;
  margin: 0;
  color: #ffffff;
  text-align: center;
}

.approval-preview__figure img {
  max-width: 100%;
  max-height: 80vh;
  object-fit: contain;
}
</style>
