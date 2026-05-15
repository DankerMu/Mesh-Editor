<template>
  <t-dialog :visible.sync="visible" :dialogClassName="classArray" :header="header" :footer="false" top="15vh">
    <template #closeBtn>
      <img src="@/assets/icon/close.png" alt="" @click="close">
    </template>
    <template #default>
      <slot name="content"></slot>
      <slot name="button"></slot>
    </template>
    <!-- <template #footer>
      <slot name="button"></slot>
    </template> -->
  </t-dialog>
</template>

<script setup>

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  close: {
    type: Function,
    default: () => { }
  },
  header: {
    type: String,
    default: ''
  },
  customClass: {
    type: String,
    default: ''
  }
})

const classArray = computed(() => 'custom-dialog ' + props.customClass)

const close = () => {
  props.close()
}
</script>

<style lang="less">
.custom-dialog {

  background-color: var(--app-layout-bg-color);
  border-radius: 8px 8px 8px 8px;
  border: 1px solid rgba(71, 80, 126, 0.9);
  padding: 0 24px;
  overflow-y: auto;

  .t-dialog__header {
    border-bottom: 1px solid #1b2c7c;

    .t-dialog__header-content {
      height: 52px;
      line-height: 52px;
      color: #fff;

    }

    .t-dialog__close {
      padding: 0;
    }
  }

  .t-dialog__body {
    // .t-form {
    // .t-input {
    //   width: 320px;
    //   .t-input__inner {
    //     width: 320px;
    //   }
    // }
    // }
  }
}
</style>