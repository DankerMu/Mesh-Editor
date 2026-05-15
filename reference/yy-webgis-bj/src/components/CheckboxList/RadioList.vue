<template>
  <div class="checkbox-group-container flex align-center">
    <div class="select-title">{{ props.title }}</div>
    <t-radio-group :value="selectedValues" @change="updateValue" class="checkbox-group">
      <t-radio v-for="item in options" :key="item.value" :value="item.value">
        {{ item.label }}
      </t-radio>
    </t-radio-group>
  </div>
</template>
<script setup>
import { ref, watch } from "vue";

const props = defineProps({
  title: {
    type: String,
    default: '检查要素：'
  },
  modelValue: {
    type: String,
  },
  defaulValue: {
    type: String
  },
  options: {
    type: Array,
    required: true,
    validator: (options) =>
      options.every((item) => "value" in item && "label" in item),
  },
});

const emit = defineEmits(["update:modelValue"]);
const selectedValues = ref();

const updateValue = (value) => {
  selectedValues.value = value;
  emit("update:modelValue", value);
};

const clear = () => {
  selectedValues.value = ''
}

watch(() => props.defaulValue, (val) => {
  if (val) selectedValues.value = val
}, {
  immediate: true
})

defineExpose({
  clear
})
</script>

<style lang="scss" scoped>
:deep(.t-radio) {
  display: flex;
  align-items: center;
  height: 38px;
  padding: 8px;
  margin-right: 8px !important;
  background-color: var(--app-ui-bg-color);

  .t-radio__input {
    background-color: transparent;
    border: 1px solid #5568B0;
  }

  .t-radio__label {
    color: var(--app-text-color);
  }
}
</style>
