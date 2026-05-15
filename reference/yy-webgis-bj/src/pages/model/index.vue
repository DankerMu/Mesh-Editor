<template>
  <div class="h-full">
    <LayoutManager>
      <template #main>
        <div class="content layout-bg">
          <div class="h-full content-bg pb-9 pl-9 pr-9 overflow-y-auto">
            <div class="operate-container">
              <div class="operation-list">
                <t-button type="reset" class="new" @click="createModel">
                  <img src="@/assets/icon/add.png" alt="">
                  新建站点
                </t-button>
                <!-- <t-button type="reset" class="new ml-[10px]" @click="addGroup">
                  <img src="@/assets/icon/add.png" alt="">
                  新建站点分组
                </t-button> -->
              </div>
            </div>
            <t-table row-key="id" :data="tableData" :columns="columns" bordered :max-height="height"
              :header-affix-props="{ offsetTop: 0 }" :scroll="{ type: 'virtual' }" :loading="isLoading" lazy-load>
              <template #status="{ row }">
                <span :style="{ color: row.status === 1 ? '#fff' : 'yellow' }">{{ row.status === 1 ? '正常' : '升级中'
                }}</span>
              </template>
              <template #operation="{ row }">
                <div class="table-btn">
                  <t-button v-show="row.detail === 1" variant="text" class="replace-item" style="margin-right: 12px;"
                    @click="detail(row)">
                    <HiIcon size="16px" :src="editSvg"></HiIcon>详情
                  </t-button>
                  <t-button v-show="row.upgrade === 1" variant="text" class="replace-item" style="margin-right: 12px;"
                    :disabled="row.status === 0 || row.hasReplacing === 0" @click="update(row)">
                    <HiIcon size="16px" :src="upgradeSvg"></HiIcon>升级
                  </t-button>
                </div>
              </template>
            </t-table>
            <GroupPanel ref="groupPanelRef" ></GroupPanel>
          </div>
        </div>
      </template>
    </LayoutManager>
  </div>


  <!-- 模型升级 -->
  <dialog-vue :visible="updateVisible" header="确认升级" :customClass="'delete-dialog custom-dialog-footer'"
    :close="updateClose">
    <template #content>
      <div class="warning-container">
        <p class="bold">确定要升级模型？</p>
      </div>
    </template>
    <template #button>
      <div class="btn-box">
        <t-button class="cancel-btn" @click="updateClose">取消</t-button>
        <t-button class="delete-btn" @click="updateConfirm">确认升级</t-button>
      </div>
    </template>
  </dialog-vue>

  <DetailVue ref="detailRef" @closeDetail="closeDetail"></DetailVue>

  <!-- 站点分组管理 -->
  <!-- <GroupAdd ref="groupAddRef" ></GroupAdd> -->
  

</template>

<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from "vue"
import LayoutManager from "@/layouts/LayoutManager.vue"
import DialogVue from '@/components/DialogVue.vue'
import DetailVue from './components/DetailVue.vue'
import GroupPanel from './components/GroupPanel.vue'
import { useRouter } from "vue-router"
import { ModelService } from "@/api"
import type { ModelList } from '@/types/PagesType'
import { MessagePlugin } from 'tdesign-vue-next'
import type { PageInfo, FormInstanceFunctions } from 'tdesign-vue-next'
import { HiIcon } from "hoci";
import editSvg from '@/assets/icon/choose.svg'
import upgradeSvg from '@/assets/icon/upgrade.svg'
import dayjs from 'dayjs'

let height = ref(460 / window.devicePixelRatio)
window.onresize = () => {
  return (() => {
    height.value = 460 / window.devicePixelRatio
  })()
}

const router = useRouter()
const groupPanelRef = ref('')
let useIpt = ref('')
const isLoading = ref(false);
const tableData = ref([])
const columns = ref([
  { colKey: 'index', width: 70, title: '序号' },
  {
    colKey: 'modelName',
    title: '模型名称',
  },
  {
    colKey: 'latestUpdateTime',
    title: '最新升级时间',
  },
  {
    colKey: 'author',
    title: '升级者',
    width: 200
  },
  {
    colKey: 'nextUpdateTime',
    title: '下次升级时间',
  },
  {
    colKey: 'status', // 0 升级中， 1 升级完成
    title: '状态',
    width: 200
  },
  {
    colKey: 'operation',
    title: '操作',
    width: 200
  },
  // hasReplacing: 0 有正在替换的，1 没有正在替换的
]);
// const pagination = reactive({
//   current: 1,
//   pageSize: 10,
//   total: 0,
// });

const search = async () => {
  try {
    const res = await ModelService.getModelList()
    if (res) {
      tableData.value = res.map((item: ModelList, index: number) => {
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
          index: index + 1
        }
      })
      // tableData.value = res
      updateIdList.value = res.filter(x => x.status === 0).map(item => item.id)
    }
  } catch (error) {
    tableData.value = []
    MessagePlugin.error('查询失败')
  } finally {
    // isLoading.value = false;
  }
}


// 模型升级
let currentId = -1
let updateVisible = ref(false)
const updateIdList = ref([]) // 记录更新中的模型id
let isDetailBtnDisabled = ref(false)
const update = (row: ModelList) => {
  currentId = row.id
  updateVisible.value = true
}
const updateClose = () => {
  currentId = -1
  updateVisible.value = false
}
const updateConfirm = async () => {
  try {
    // debugger
    const userInfo = JSON.parse(sessionStorage.getItem("userInfo") ?? '{}')
    const uname = userInfo?.username ?? ""

    await ModelService.updateModel({
      id: currentId,
      author: uname
    })
    getNewUpdateList()
    MessagePlugin.success('升级中，请等待')

    // const res = await ModelService.testupdate({
    //   id: currentId,
    //   author: uname
    // })

  } catch (error) {
    MessagePlugin.error('升级失败')
  } finally {
    currentId = -1
    updateVisible.value = false
  }
}

// 开启定时器
let updateInterval = null
const startInterval = () => {
  // 开启定时器，请求模型升级状态
  if (!updateInterval) {
    updateInterval = setInterval(() => {
      // 更新升级状态
      getNewUpdateList()

    }, 1000 * 60 * 20);
  }
}
const getNewUpdateList = async () => {
  try {
    const res = await ModelService.getModelList()
    if (res) {
      // tableData.value = res
      tableData.value = res.map((item: ModelList, index: number) => {
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
          index: index + 1
        }
      })
      updateIdList.value = res.filter(x => x.status === 0).map(item => item.id)
    }

  } catch (error) { }
}


watch(() => updateIdList.value.length, (val) => {
  if (val > 0) {
    console.log('开启定时器');
    startInterval()
  } else {
    console.log('清除定时器');
    updateInterval && clearInterval(updateInterval)
    updateInterval = null
  }
}, {
  immediate: true
})



const createModel = () => {
  router.push({ name: 'create' })
}

// 打开详情弹窗
const detailRef = ref(null)
const detail = (row) => {
  detailRef.value?.open(row)
}
// 关闭详情弹窗，刷新列表
const closeDetail = () => {
  search()
}

// 打开分组弹窗
const groupAddRef = ref(null)
const addGroup = (row) => {
  groupAddRef.value?.open(row)
}



search()

onBeforeUnmount(() => {
  tableData.value = []
  console.log('清除定时器');
  updateInterval && clearInterval(updateInterval)
  updateInterval = null
})

</script>

<style lang="less" scoped>
.content {
  height: 100%;
  position: relative;
  z-index: 2;
  padding: var(--app-view-padding);
}

.operate-container {
  display: flex;
  align-items: center;
  padding-top: 2.25rem;
  padding-bottom: 1.5rem;

  :deep(.idipt) {
    width: 26.25rem;

    .t-input {
      width: 26.25rem;
      height: 3.75rem;
      border-color: #2E3C8A !important;
    }

    .t-input--focused {
      box-shadow: none;
    }
  }
}

// :deep(.t-table) {
//   .t-table__header--fixed:not(.t-table__header--multiple)>tr>th {
//     background-color: transparent !important;
//   }

//   .t-table__scroll-bar-divider {
//     border: none;
//   }
// }

// :deep(.t-table--bordered) {
//   th {
//     border-left: 1px solid var(--td-border-color) !important;
//   }
// }

// 表体滚动轴隐藏
:deep(.t-table__content::-webkit-scrollbar) {
  width: 0;
}
</style>
