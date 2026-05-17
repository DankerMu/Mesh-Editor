<script setup lang="ts">
import { computed, ref } from 'vue'

const emit = defineEmits<{
  submit: [caseId: string]
}>()

const caseId = ref('')
const touched = ref(false)

function isValidCaseId(value: string) {
  if (!/^\d{10}$/.test(value)) {
    return false
  }

  const year = Number(value.slice(0, 4))
  const month = Number(value.slice(4, 6))
  const day = Number(value.slice(6, 8))
  const hour = value.slice(8, 10)

  if (hour !== '08' && hour !== '20') {
    return false
  }

  const date = new Date(Date.UTC(year, month - 1, day, Number(hour)))
  return (
    date.getUTCFullYear() === year &&
    date.getUTCMonth() === month - 1 &&
    date.getUTCDate() === day &&
    date.getUTCHours().toString().padStart(2, '0') === hour
  )
}

const valid = computed(() => isValidCaseId(caseId.value))
const showError = computed(() => touched.value && caseId.value.length > 0 && !valid.value)

function submitCaseId() {
  touched.value = true
  if (valid.value) {
    emit('submit', caseId.value)
  }
}
</script>

<template>
  <form class="case-id-input" @submit.prevent="submitCaseId">
    <div class="case-id-input__field">
      <label class="case-id-input__label" for="case-id">起报时次</label>
      <t-input
        id="case-id"
        v-model="caseId"
        class="case-id-input__control"
        clearable
        maxlength="10"
        placeholder="2026010108"
        @blur="touched = true"
      />
      <p v-if="showError" class="case-id-input__error">格式: YYYYMMDDHH（时次 08 或 20）</p>
    </div>
    <t-button theme="primary" type="submit" :disabled="!valid">扫描数据</t-button>
  </form>
</template>

