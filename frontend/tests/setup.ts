import { config } from '@vue/test-utils'

config.global.stubs = {
  't-alert': {
    props: ['message'],
    template: '<div><slot />{{ message }}</div>',
  },
  't-button': {
    props: ['disabled', 'type', 'theme'],
    template: '<button :type="type || \'button\'" :disabled="disabled"><slot name="icon" /><slot /></button>',
  },
  't-card': {
    props: ['title', 'bordered'],
    template: '<section><h3 v-if="title">{{ title }}</h3><slot /></section>',
  },
  't-dialog': {
    props: ['visible', 'header'],
    emits: ['update:visible'],
    template: '<div v-if="visible" role="dialog"><h3>{{ header }}</h3><slot /><footer><slot name="footer" /></footer></div>',
  },
  't-layout': {
    template: '<div><slot /></div>',
  },
  't-aside': {
    template: '<aside><slot /></aside>',
  },
  't-content': {
    template: '<main><slot /></main>',
  },
  't-form': {
    template: '<form @submit.prevent="$emit(\'submit\', { validateResult: true })"><slot /></form>',
  },
  't-form-item': {
    props: ['label'],
    template: '<label>{{ label }}<slot /></label>',
  },
  't-input': {
    props: ['modelValue', 'maxlength', 'placeholder'],
    emits: ['update:modelValue', 'blur'],
    template:
      '<input :value="modelValue" :maxlength="maxlength" :placeholder="placeholder" @input="$emit(\'update:modelValue\', $event.target.value)" @blur="$emit(\'blur\')" />',
  },
  't-menu': {
    template: '<nav><slot /></nav>',
  },
  't-menu-item': {
    template: '<span><slot /></span>',
  },
  't-loading': {
    props: ['loading'],
    template: '<div><span v-if="loading" data-test="loading"></span><slot /></div>',
  },
  't-empty': {
    props: ['description'],
    template: '<div>{{ description }}<slot /></div>',
  },
  't-space': {
    props: ['direction', 'size'],
    template: '<div><slot /></div>',
  },
  't-progress': {
    props: ['percentage', 'status'],
    template: '<div data-test="progress">{{ percentage }}</div>',
  },
  't-radio-group': {
    props: ['modelValue'],
    emits: ['update:modelValue'],
    template: '<div><slot /></div>',
  },
  't-radio-button': {
    props: ['value'],
    template: '<button type="button" @click="$parent.$emit(\'update:modelValue\', value)"><slot /></button>',
  },
  't-tabs': {
    props: ['modelValue'],
    emits: ['update:modelValue'],
    template: '<div data-test="tabs"><slot /></div>',
  },
  't-tab-panel': {
    props: ['value', 'label'],
    template:
      '<section :data-tab="value"><button v-if="label" type="button" :data-test="`status-tab-${value || \'all\'}`" @click="$parent.$emit(\'update:modelValue\', value || \'\')">{{ label }}</button><slot /></section>',
  },
  't-tag': {
    props: ['theme', 'variant'],
    template: '<span :data-theme="theme"><slot /></span>',
  },
  't-select': {
    props: ['modelValue', 'options', 'placeholder', 'clearable'],
    emits: ['update:modelValue'],
    template:
      '<select :value="modelValue || \'\'" @change="$emit(\'update:modelValue\', $event.target.value || undefined)"><option value="">{{ placeholder }}</option><option v-for="option in options" :key="option.value" :value="option.value">{{ option.label }}</option></select>',
  },
  't-list': {
    template: '<ul><slot /></ul>',
  },
  't-list-item': {
    template: '<li><slot /></li>',
  },
  't-image': {
    props: ['src', 'fit'],
    template: '<img :src="src" />',
  },
  't-timeline': {
    template: '<ol><slot /></ol>',
  },
  't-timeline-item': {
    props: ['label'],
    template: '<li><span>{{ label }}</span><slot /></li>',
  },
  't-descriptions': {
    template: '<dl><slot /></dl>',
  },
  't-descriptions-item': {
    props: ['label'],
    template: '<div><dt>{{ label }}</dt><dd><slot /></dd></div>',
  },
  't-textarea': {
    props: ['modelValue', 'placeholder'],
    emits: ['update:modelValue'],
    template:
      '<textarea :value="modelValue" :placeholder="placeholder" @input="$emit(\'update:modelValue\', $event.target.value)" />',
  },
  't-tooltip': {
    props: ['content', 'disabled'],
    template: '<span :title="disabled ? undefined : content"><slot /></span>',
  },
}
