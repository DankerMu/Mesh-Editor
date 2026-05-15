<template>
  <dialog-vue :visible="detailVisible" header="站点详情" :customClass="'detail-dialog custom-dialog-footer'" :close="close">
    <template #content>
      <div class="operate-container" style="padding: 0;">
        <SearchVue ref="searchRef" v-model:stationIpt="stationIpt" :modelId="curModelId" :placeholder="'请输入站点名称或编号'"
          :attributeName="'modelName'" @searchChange="searchChange" @showAllStation="showAllStation"></SearchVue>
        <!-- <div class="operation-list" style="margin-left: 2.25rem">
          <t-button theme="primary" class="search" style="margin-right: 1.125rem;" @click="search">
            <img src="@/assets/icon/search.png" alt="">
            搜索
          </t-button>
        </div> -->
      </div>
      <t-table :data="tableData" :columns="columns" class="table-pagination" row-key="index" :loading="isLoading"
        :loadingProps="{ totalContent: false }" :pagination="pagination" bordered :header-affix-props="{ offsetTop: 0 }"
        :scroll="{ type: 'virtual' }" lazy-load max-height="480" @page-change="onPageChange">
        <template #status="{ row }">
          <span :style="{ color: row.status === 1 ? '#fff' : 'yellow' }">{{ getDescription(row.status) }}</span>
        </template>
        <template #operation="{ row }">
          <div class="table-btn">
            <t-button  v-show="row.replaceflag === 1" :disabled="row.status === 0 || row.status === 2 || isUpdating" variant="text" class="replace-item"
              style="margin-right: 12px;" @click="replace(row)">
              <HiIcon size="16px" :src="replaceSvg"></HiIcon>替换
            </t-button>
            <t-button :disabled="row.status === 0 || row.status === 2 || isUpdating" variant="text" class="delete-item"
              @click="deleteModel(row)">
              <HiIcon size="16px" :src="deleteSvg"></HiIcon>删除
            </t-button>
          </div>
        </template>
      </t-table>
    </template>
  </dialog-vue>

  <!-- 模型替换 -->
  <dialog-vue :visible="replaceVisible" header="模型替换" :customClass="'replace-dialog custom-dialog-footer'"
    :close="replaceClose">
    <template #content>
      <t-form ref="formRef" :data="formData" class="item-container" :rules="baseRules" label-width="auto"
        label-align="left" scrollToFirstError="smooth">
        <t-form-item label="模型选择" name="model">
          <t-select v-model="formData.model" class="select" placeholder="请选择模型" :options="modelList"
            :popupProps="{ overlayClassName: 'pure-select-popup' }"></t-select>
        </t-form-item>
        <!-- <t-form-item label="时间周期" name="period">
          <t-date-range-picker v-model="formData.period" class="dateRangePicker" allow-input clearable
            :popupProps="{ overlayClassName: 'yy-date-picker-popup' }">
            <template #suffixIcon>
              <img src="@/assets/icon/calendar.png" alt="">
            </template>
  </t-date-range-picker>
  </t-form-item> -->
      </t-form>
    </template>
    <template #button>
      <div class="btn-box" style="border-top: 1px solid #1b2c7c;">
        <t-button class="cancel-btn" @click="replaceClose">取消</t-button>
        <t-button class="confirm-btn" @click="replaceConfirm">确认替换</t-button>
      </div>
    </template>
  </dialog-vue>

  <!-- 模型删除 -->
  <dialog-vue :visible="delVisible" header="确认删除" :customClass="'delete-dialog custom-dialog-footer'" :close="delClose">
    <template #content>
      <div class="warning-container">
        <img src="@/assets/img/delwarning.png" alt="">
        <div>
          <p class="bold">确定要删除该模型吗？此操作无法撤销</p>
          <p class="normal">删除后数据将无法恢复，请谨慎操作</p>
        </div>
      </div>
    </template>
    <template #button>
      <div class="btn-box">
        <t-button class="cancel-btn" @click="delClose">取消</t-button>
        <t-button class="delete-btn" @click="delConfirm">确认删除</t-button>
      </div>
    </template>
  </dialog-vue>
</template>

<script setup>
import { ref, onBeforeUnmount, reactive } from 'vue'
import DialogVue from '@/components/DialogVue.vue'
import SearchVue from './SearchStation.vue'
import { HiIcon } from "hoci";
import deleteSvg from '@/assets/icon/delete-btn.svg'
import replaceSvg from '@/assets/icon/replace.svg'
import { ModelService } from '@/api'
import { MessagePlugin } from 'tdesign-vue-next'

const emit = defineEmits(["update:selectedStation", "update:selectedArea", "closeDetail"]);

let detailVisible = ref(false)
let searchRef = ref(null)
let stationIpt = ref('')
let hasReplaceBtn = ref(true) // 是否有'替换'按钮
let hasDeleteBtn = ref(true) // 是否有'删除'按钮
let curModelId = ref() // 当前站点的模型id
let isUpdating = ref(false) // 是否正在升级

const open = (row) => {
  hasReplaceBtn.value = true
  hasDeleteBtn.value = true
  isUpdating.value = false
  curModelId.value = row.id
  search()
  // debugger
  if (row.modelName.includes('区域')) {
    hasReplaceBtn.value = false
    hasDeleteBtn.value = false
  }
  if (row.status === 0) {
    isUpdating.value = true
  }
  detailVisible.value = true
}
const close = () => {
  detailVisible.value = false
  searchRef && searchRef.value.close()
  pagination.value.current = 1
  emit('closeDetail')
}


const tableData = ref([])
const columns = ref([
  { colKey: 'index', width: 70, title: '序号' },
  {
    colKey: 'stationName',
    title: '站点/区域名称', // 站点名称站点编号
  },
  {
    colKey: 'replaceTime',
    title: '替换时间',
  },
  {
    colKey: 'author',
    title: '替换者',
    width: 100
  },
  {
    colKey: 'model',
    title: '当前模型',
  },
  {
    colKey: 'status', // 0 替换中，1 已完成，2 建模中
    title: '状态',
    width: 100
  },
  {
    colKey: 'operation',
    title: '操作',
    width: 185
  },
]);
const isLoading = ref(false);
const pagination = ref({
  total: 100,
  current: 1,
  pageSize: 10
})
const onPageChange = async (pageInfo, context) => {
  pagination.value.current = pageInfo.current
  pagination.value.pageSize = pageInfo.pageSize
  await search()
}

const search = async () => {
  try {
    isLoading.value = true;
    let params = {
      id: curModelId.value,
      pageNum: pagination.value.current,
      pageSize: pagination.value.pageSize
    }

    const res = await ModelService.searchDetailStaion(params)
    pagination.value.total = res.total || 0;
    if (res && res.data) {
      tableData.value = res.data.map((item, index) => {
        let formatted = {}
        Object.entries(item).map(([key, value]) => {
          if (!value && value !== 0) {
            formatted[key] = '-'
          } else {
            formatted[key] = value
          }
        })
        return {
          ...formatted,
          stationName: item.stationName + item.stationNum,
          index: index + 1
        }
      })
    }
  } catch (error) {
    tableData.value = []
    MessagePlugin.error('查询失败')
  } finally {
    isLoading.value = false;
  }
}

const formRef = ref()
const formData = reactive({
  model: '',
  // period: []
})
const baseRules = {
  model: [{ required: true, message: "请选择模型", type: "error" }],
  // period: [{ required: true, message: "请选择时间周期", type: "error" }],
}


const modelList = ref([])
const getUpdateModelList = async (num) => {
  const res = await ModelService.getReplaceModelList({
    stationNum: num
  })
  modelList.value = res.map((item) => {
    return {
      ...item,
      label: item.model,
      value: item.id
    }
  })
}

// 显示文字：0 替换中，1 正常，2 建模中
const getDescription = (status) => {
  if (status === 1) return '正常'
  else if (status === 0) return '替换中'
  else return '建模中'
}


// 替换模型
let replaceVisible = ref(false)
const replace = (row) => {
  formData.model = ''
  formRef.value?.clearValidate()
  currentStationId = row.id
  currentStationNum = row.stationNum === '-' ? '' : row.stationNum
  replaceVisible.value = true
  // 修改当前站点使用模型
  getUpdateModelList(currentStationNum)
}
const replaceClose = () => {
  replaceVisible.value = false
  formData.model = ''
  // formData.period = []
  formRef.value?.clearValidate()
}
const replaceConfirm = async () => {
  formRef.value?.validate().then(async (result) => {
    if (result === true) {
      try {
        const userInfo = JSON.parse(sessionStorage.getItem("userInfo") ?? '{}')
        const uname = userInfo?.username ?? ""

        const res = await ModelService.stationModelReplace({
          id: formData.model,
          stationNum: currentStationNum,
          author: uname
        })
        if (res > 0) {
          await search()
          MessagePlugin.success('替换中，请等待')
        } else {
          MessagePlugin.error('替换失败')
        }
      } catch (error) {
        MessagePlugin.error('替换失败')
      } finally {
        formData.model = ''
        // formData.period = []
        currentStationId = -1
        currentStationNum = ''
        replaceVisible.value = false
      }
    }
  })
}

let delVisible = ref(false)
let currentStationId = -1
let currentStationNum = ''
const deleteModel = (row) => {
  currentStationId = row.id
  currentStationNum = row.stationNum === '-' ? '' : row.stationNum
  delVisible.value = true
}

const delClose = () => {
  currentStationId = ''
  delVisible.value = false
}

const delConfirm = async () => {
  try {
    let params = {
      id: currentStationId,
      stationNum: currentStationNum,
      managerId: curModelId.value
    }
    const res = await ModelService.deleteStation(params)
    if (res > 0) {
      await search()
      MessagePlugin.success('删除成功')
    } else {
      MessagePlugin.error('删除失败')
    }

  } catch (error) {
    MessagePlugin.error('删除失败')
  } finally {
    currentStationId = -1
    currentStationNum = ''
    delVisible.value = false
  }
}

const searchChange = async (obj) => {
  try {
    const res = await ModelService.queryModelManagerDetailById({
      id: obj.id
    })
    tableData.value = res.map((item, index) => {
      let formatted = {}
      Object.entries(item).map(([key, value]) => {
        if (!value && value !== 0) {
          formatted[key] = '-'
        } else {
          formatted[key] = value
        }
      })

      let joinName = item.stationName ? item.stationName + item.stationNum : item.stationNum

      return {
        ...formatted,
        stationName: joinName,
        index: index + 1
      }
    })
    pagination.value.current = 1
    pagination.value.total = tableData.value.length
  } catch (error) { }
}

const showAllStation = () => {
  pagination.value.current = 1
  search()
}


onBeforeUnmount(() => {
  tableData.value = []
})

defineExpose({
  open
})

</script>

<style lang="less">
.detail-dialog {
  width: 1100px;
  height: 700px;

  .t-dialog__body {
    display: flex;
    flex-direction: column;
    height: calc(100% - 53px);

    ul {
      height: 100%;
      overflow-y: auto;
    }

    .t-pagination {
      color: var(--app-text-color-purple);
    }
  }
}
</style>