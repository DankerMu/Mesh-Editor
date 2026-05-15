<template>
  <t-auto-complete v-model="keyword" clearable class="left-top" :options="filterOptions" placeholder="请输入站点名称或编号"
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
import { DisplayService } from '@/api'

const emit = defineEmits(['searchChange'])

const keyword = ref("");
// const options = ref([]);
let filterOptions = ref([])

const showDropdown = ref(false);

const handleInput = async (val) => {
  if (val.length > 0) {
    showDropdown.value = true
    try {
      // const res = await DisplayService.SearchStation({
      //   stationName: val
      // })
      const res = await DisplayService.MoreSearchStation({
        stationName: val
      })
      if (res.length > 0) {
        filterOptions.value = res.map(item => {
          let label = item.stationName + item.stationIdD
          if (!item.stationName || item.stationName == ' ') {
            label = item.stationIdD.toString()
          }
          return {
            label: label,
            ...item
          }
        })
      } else {
        filterOptions.value = []
      }
    } catch (error) { }
  } else {
    showDropdown.value = false
  }
};

const handleChange = (val) => {
  let result = filterOptions.value.find(x => x.label === val)
  emit('searchChange', result)
  showDropdown.value = false
}

// const filterMethod = (search, option) => {
//   debugger
//   console.log('search:', search, ', option:', option);
//   return options.value.label.indexOf(search) !== -1;
// }
const close = () => {
  keyword.value = ''
  showDropdown.value = false
}

defineExpose({
  close
})

</script>


<style scoped lang="less">
.left-top {
  position: absolute;
  z-index: 3;
  top: 1.5rem;
  left: 1.5rem;
  width: 223px;

  :deep(.t-input) {
    width: 223px;
    height: 43px;
    border-radius: 8px;
    border: 1px solid rgba(71, 80, 126, 0.9);
    background: linear-gradient(225deg, rgba(27, 44, 124, 0.98) 0%, rgba(21, 31, 96, 0.98) 100%);
  }
}
</style>
