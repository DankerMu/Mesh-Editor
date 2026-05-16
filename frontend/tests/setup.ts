import { config } from '@vue/test-utils'

config.global.stubs = {
  't-alert': {
    props: ['message'],
    template: '<div><slot />{{ message }}</div>',
  },
  't-button': {
    template: '<button><slot /></button>',
  },
  't-form': {
    template: '<form @submit.prevent="$emit(\'submit\', { validateResult: true })"><slot /></form>',
  },
  't-form-item': {
    props: ['label'],
    template: '<label>{{ label }}<slot /></label>',
  },
  't-input': {
    template: '<input />',
  },
  't-menu': {
    template: '<nav><slot /></nav>',
  },
  't-menu-item': {
    template: '<span><slot /></span>',
  },
}
