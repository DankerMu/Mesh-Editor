<template>
  <div class="checkbox-group-container flex align-center">
    <div class="select-title">{{ props.title }}</div>
    <t-checkbox-group v-model="selectedValues" @change="updateValue" class="checkbox-group">
      <t-checkbox v-for="item in options" :key="item.value" :value="item.value">
        {{ item.label }}
      </t-checkbox>
    </t-checkbox-group>
  </div>
</template>
<script setup>
import { ref } from "vue";


const props = defineProps({
  title: {
    type: String,
    default: '检查要素：'
  },
  modelValue: {
    type: Array,
    default: () => []
  },
  options: {
    type: Array,
    required: true,
    validator: (options) =>
      options.every((item) => "value" in item && "label" in item),
  },
});

const modelValueRef = toRef(props, 'modelValue'); // 把 props.modelValue 包装为 ref

const selectedValues = ref([...props.modelValue]);

watch(modelValueRef, (val) => {
  if(Array.isArray(val)){
  selectedValues.value = [...val];
  }else{
  selectedValues.value = [val];
  }
});

const emit = defineEmits(["update:modelValue"]);

const updateValue = (value) => {
  selectedValues.value = value;
  emit("update:modelValue", value);
};

const clear = () => {
  selectedValues.value = []
}

defineExpose({
  clear
})
</script>

<style lang="scss" scoped>
:deep(.t-checkbox-group) {}

:deep(.t-checkbox) {
  padding: 8px;
  background-color: var(--app-ui-bg-color);

  .t-checkbox__input {
    background-color: transparent;
    border: 1px solid #5568B0;
  }

  .t-checkbox__label {
    color: var(--app-text-color);
  }
}

.select-title {
  font-size: 16px;
}
</style>
