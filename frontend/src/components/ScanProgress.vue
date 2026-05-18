<script setup lang="ts">
import { computed } from 'vue'
import { useWindowStore } from '@/stores/windowStore'

const emit = defineEmits<{
  retry: []
}>()

const windowStore = useWindowStore()

const isRunning = computed(
  () => windowStore.scanPolling || windowStore.scanStatus?.status === 'running',
)
const isFailed = computed(() => windowStore.scanStatus?.status === 'failed')
const isComplete = computed(
  () => !!windowStore.scanStatus && !isRunning.value && !isFailed.value,
)
const errorMessage = computed(() => windowStore.scanErrorMessage ?? '扫描失败')

function retry() {
  emit('retry')
}
</script>

<template>
  <section v-if="windowStore.scanStatus || windowStore.scanPolling" class="scan-progress" aria-live="polite">
    <div v-if="isRunning" class="scan-progress__running">
      <t-loading size="small" />
      <span>正在扫描...</span>
      <t-progress class="scan-progress__bar" :percentage="50" status="active" />
    </div>
    <p v-else-if="isComplete" class="scan-progress__complete">
      扫描完成：{{ windowStore.availableCount }} 个可用窗口
    </p>
    <div v-else-if="isFailed" class="scan-progress__failed">
      <p class="scan-progress__error-text">{{ errorMessage }}</p>
      <t-button size="small" theme="default" @click="retry">重新扫描</t-button>
    </div>
  </section>
</template>

