<template>
  <t-auto-complete v-model="keyword" clearable :options="filterOptions" class="left-top  pure-color-ipt"
    :placeholder="placeholder"
    :popupProps="{ overlayClassName: 't-demo-autocomplete-option-list', visible: showDropdown }" @change="handleInput"
    @select="handleChange">
    <template #prefixIcon>
      <img src="@/assets/icon/search-station.png" alt="" style="width: 24px;">
    </template>
    <template #option="{ option }">
      {{ option.label }}
    </template>
  </t-auto-complete>
</template>

<script setup>

const props = defineProps({
  useIpt: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: ''
  },
  attributeName: {
    type: String,
    default: 'username'
  },
  searchMethod: {
    type: Function,
    default: () => { }
  }
})
watch(() => props.useIpt, (newVal) => {
  if (!newVal) {
    keyword.value = newVal
    showDropdown.value = false
  }
})

const emit = defineEmits(['update:useIpt'])

const keyword = ref(props.useIpt);
let filterOptions = ref([])

const showDropdown = ref(false);

const handleInput = async (val) => {

  if (val.length > 0) {
    showDropdown.value = true
    emit('update:useIpt', val)
    try {
      let params = {}
      params[props.attributeName] = val
      const res = await props.searchMethod(params)
      if (res.length > 0) {
        filterOptions.value = res.map(item => {
          return {
            label: item[props.attributeName],
          }
        })
      } else {
        filterOptions.value = []
      }
    } catch (error) { }
  } else {
    emit('update:useIpt', val)
    showDropdown.value = false
  }
};

const handleChange = (val) => {
  emit('update:useIpt', val)
  showDropdown.value = false
}

</script>


<style scoped lang="less">
.left-top {
  width: 26.25rem;

  :deep(.t-input) {
    width: 26.25rem;
    height: 3.75rem;
  }
}
</style>
<style></style>
