<script setup lang="ts">
const props = defineProps<{
  negativeCount: number
  negativeMinValue: number | null
  ptypeMissingLeads: number[] | null
}>()

function formatNegativeMinValue(value: number | null) {
  return value === null ? '--' : value
}
</script>

<template>
  <span class="window-qc-info" aria-label="质控信息">
    <t-tooltip
      v-if="props.negativeCount > 0"
      :content="`存在 ${props.negativeCount} 个负值格点（最小值 ${formatNegativeMinValue(props.negativeMinValue)}mm）`"
    >
      <span class="window-qc-info__icon window-qc-info__icon--warning" aria-label="负值格点">!</span>
    </t-tooltip>
    <t-tooltip
      v-if="props.ptypeMissingLeads?.length"
      :content="`缺失时效: ${props.ptypeMissingLeads.join(', ')}`"
    >
      <span class="window-qc-info__icon window-qc-info__icon--info" aria-label="缺失时效">i</span>
    </t-tooltip>
  </span>
</template>

