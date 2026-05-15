<template>
  <t-auto-complete v-model="keyword" clearable :options="filterOptions" class="ipt pure-color-ipt" placeholder="请输入用户名"
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
import { UserService } from '@/api'

const props = defineProps({
  useIdIpt: {
    type: String,
    default: ''
  }
})
watch(() => props.useIdIpt, (newVal) => {
  if (!newVal) {
    keyword.value = newVal
    showDropdown.value = false
    // filterOptions.value = []
  }
})

const emit = defineEmits(['update:useIdIpt'])

const keyword = ref(props.useIdIpt);
let filterOptions = ref([])

const showDropdown = ref(false);

const handleInput = async (val) => {
  // filterOptions.value = []

  if (val.length > 0) {
    showDropdown.value = true
    emit('update:useIdIpt', val)
    try {
      const res = await UserService.searchUser({
        id: 0,
        username: val,
        loginname: '',
        role: '',
        enabled: false
      })
      if (res.length > 0) {
        filterOptions.value = res.map(item => {
          return {
            label: item.username,
            // ...item
          }
        })
      }
    } catch (error) { }
  } else {
    emit('update:useIdIpt', val)
    showDropdown.value = false
    // filterOptions.value = []
  }
};

const handleChange = (val) => {
  emit('update:useIdIpt', val)
  // filterOptions.value = []
  showDropdown.value = false
}

</script>


<style scoped lang="less">
.ipt {
  width: 26.25rem;

  :deep(.t-input) {
    width: 26.25rem;
    height: 3.75rem;
  }
}
</style>
