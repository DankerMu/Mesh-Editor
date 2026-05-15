<template>
  <div class="slider-container">
    <div
      v-if="alwaysShowLabel"
      class="permanent-tooltip"
      :style="{ left: `${tooltipPosition}%` }"
    >
      {{ formattedValue }}
    </div>
    <t-slider
      v-model="value"
      :min="min"
      :max="max"
      :step="step"
      :label="null"
      :marks="marks"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { Slider as TSlider } from 'tdesign-vue-next';
import dayjs from 'dayjs';

const props = defineProps({
  min: { type: Number, required: true },
  max: { type: Number, required: true },
  step: { type: Number, required: true },
  format: { type: Function, default: (val) => val },
  alwaysShowLabel: {
    type: Boolean, default: false
  },
  marks: { type: Object, default: () => ({}) },
  marksRender: { type: Function, default: null },
});

const marks = computed(() => {
  if (Object.keys(props.marks).length > 0) {
    return props.marks;
  }
  
  if (props.marksRender && typeof props.marksRender === 'function') {
    return props.marksRender(props.min, props.max, props.step);
  }

  const marks = {};
  for (let i = props.min; i <= props.max; i += props.step) {
    marks[i] = dayjs(i).format('HH:mm');
  }
  return marks;
});

const value = defineModel();

const tooltipPosition = computed(() => {
  return ((value.value - props.min) / (props.max - props.min)) * 100;
});

const formattedValue = computed(() => {
  return props.format(value.value);
});
</script>

<style lang="less" scoped>
.slider-container {
  position: relative;
  width: 100%;
  transform: translateY(-0.6rem);

  :deep(.t-slider) {
    .t-slider__mark {

      :first-child,
      :last-child {
        transform: translateX(-50%);
      }
    }
  }
}

.permanent-tooltip {
  position: absolute;
  top: -2.8rem;
  transform: translateX(-50%);
  background-color: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
  user-select: none;

  &::after {
    content: "";
    width: 0;
    height: 0;
    position: absolute;
    left: 50%;
    transform: translateX(-50%);
    bottom: -0.9rem;
    border: 0.5rem solid;
    border-color: transparent;
    border-top-color: rgba(0, 0, 0, 0.7);
  }
}
</style>
