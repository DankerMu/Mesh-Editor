import { config } from '@vue/test-utils'

config.global.stubs = {
  't-alert': {
    props: ['message'],
    template: '<div><slot />{{ message }}</div>',
  },
  't-button': {
    props: ['disabled', 'type'],
    template: '<button :type="type || \'button\'" :disabled="disabled"><slot name="icon" /><slot /></button>',
  },
  't-dialog': {
    props: ['visible', 'header'],
    emits: ['update:visible'],
    template: '<div v-if="visible" role="dialog"><h3>{{ header }}</h3><slot /></div>',
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
    template: '<span data-test="loading"></span>',
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
    template: '<section :data-tab="value"><slot /></section>',
  },
  't-tag': {
    props: ['theme', 'variant'],
    template: '<span :data-theme="theme"><slot /></span>',
  },
  't-tooltip': {
    props: ['content', 'disabled'],
    template: '<span :title="disabled ? undefined : content"><slot /></span>',
  },
}
