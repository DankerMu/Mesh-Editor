<template>
  <t-auto-complete v-model="keyword" clearable :options="filterOptions" class="ipt pure-color-ipt" placeholder="请输入角色名称"
    :popupProps="{ overlayClassName: 't-demo-autocomplete-option-list', visible: showDropdown1 }"
    @change="handleInput" @select="handleChange">
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
    showDropdown1.value = false
    // filterOptions.value = []
  }
})

const emit = defineEmits(['update:useIdIpt'])

const keyword = ref(props.useIdIpt);
let filterOptions = ref([])

const showDropdown1 = ref(false);

const handleInput = async (val) => {
  // filterOptions.value = []

  if (val.length > 0) {
    showDropdown1.value = true
    emit('update:useIdIpt', val)

    try {
      const res = await UserService.searchRole({
        roleName: val,
      })
      if (res.length > 0) {
        filterOptions.value = res.map(item => {
          return {
            label: item.roleName,
            value: item.id,
            // ...item
          }
        })
      }
    } catch (error) { }
  } else {
    emit('update:useIdIpt', val)
    showDropdown1.value = false
    // filterOptions.value = []
  }
};

const handleChange = (val) => {
  emit('update:useIdIpt', val)
  // filterOptions.value = []
  showDropdown1.value = false
}

</script>


<style scoped lang="less">
.ipt {
  width: 26.25rem;

  :deep(.t-input) {
    width: 26.25rem;
    height: 3.75rem;
    border-color: #2E3C8A !important;
  }
}
</style>
