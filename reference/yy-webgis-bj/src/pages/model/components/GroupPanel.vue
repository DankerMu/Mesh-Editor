<template>
  <div class="operate-container">
    <!-- <SearchVue v-model:useIdIpt="currentRoleName"></SearchVue> -->
    <div class="operation-list" >
      <!-- <t-button theme="primary" class="search" style="margin-right: 1.125rem;" @click="search">
        <img src="@/assets/icon/search.png" alt="">
        查询
      </t-button> -->
      <t-button class="new" @click="openNewUser">
        <img src="@/assets/icon/add.png" alt="">
        新增分组
      </t-button>
    </div>

  </div>
  <t-table row-key="index" :data="tableData" :columns="columns" bordered :max-height="height" headerAffixedTop
    :scroll="{ type: 'virtual' }" lazy-load>
    <template #stationList="{ row }">
      <t-tooltip :content="row.stationList.map(e=> `${e.stationName || ''}${e.stationIdD}`).join(',')" placement="top">
        <div class="ellipsis" style="max-width: 100%">{{row.stationList.map(e=> `${e.stationName || ''}${e.stationIdD}`).join(',')}}</div>
      </t-tooltip>
    </template>
    <template #operation="{ row }">
      <div class="table-btn">
        <t-button variant="text" class="replace-item" style="margin-right: 12px;" @click="updateRoleInfo(row)">
          <HiIcon size="16px" :src="editSvg"></HiIcon>修改
        </t-button>
        <t-button variant="text" class="delete-item" @click="deleteRole(row)">
          <HiIcon size="16px" :src="deleteSvg"></HiIcon>删除
        </t-button>
      </div>
    </template>
  </t-table>
  <t-pagination v-model="pagination.current" class="table-pagination" :total="pagination.total"
    :page-size.sync="pagination.pageSize" @page-size-change="onPageSizeChange" @current-change="onCurrentChange" />

  <dialog-vue :visible="delVisible" header="确认删除" :customClass="'delete-dialog custom-dialog-footer'" :close="delClose">
    <template #content>
      <div class="warning-container">
        <img src="@/assets/img/delwarning.png" alt="">
        <div>
          <p class="bold">确定要删除该分组吗？此操作无法撤销</p>
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

  <!-- 新增分组 -->
  <dialog-vue :visible="newVisible" header="新增分组" :customClass="'group-dialog custom-dialog-footer'"
    :close="newClose">
    <template #content>
      <t-form ref="formRef" :data="formData" class="item-container form-color-ipt" :rules="baseRules" label-width="auto"
        label-align="left" scrollToFirstError="smooth">
        <t-form-item label="分组名称" name="taskName">
          <t-input v-model="formData.taskName" clearable placeholder="请输入分组名称"></t-input>
        </t-form-item>
        <t-form-item label="组内站点" name="stationList" class="flex-columns" >
          <div>
            <SearchStation ref="searchRef" @searchChange="searchChange" style="position: static; margin-bottom: 10px"></SearchStation>
            <!-- 可拖拽容器 -->
            <div ref="tagListRef" class="tag-list">
              <span v-for="(item, i) in extentList" :key="item.stationIdD" class="tag">
                <span class="tag-text">{{ item.label }}</span>
                <span class="tag-close" @click="closeGroup(i)">×</span>
              </span>
            </div>
          </div>
        </t-form-item>
      </t-form>
    </template>
    <template #button>
      <div class="btn-box" style="border-top: 1px solid #1b2c7c;">
        <t-button class="cancel-btn" @click="newClose">取消</t-button>
        <t-button class="confirm-btn" @click="confirm">确定</t-button>
      </div>
    </template>
  </dialog-vue>

  <!-- 修改分组信息 -->
  <dialog-vue :visible="updateVisible" :header="'修改分组信息'" :customClass="'group-dialog custom-dialog-footer'"
    :close="newClose">
    <template #content>
      <t-form ref="formUpdateRef" :data="formData" class="item-container form-color-ipt" :rules="baseRules"
        label-width="auto" label-align="left" scrollToFirstError="smooth">
        <t-form-item label="分组名称" name="taskName">
          <t-input v-model="formData.taskName" placeholder="请输入分组名称"></t-input>
        </t-form-item>
        <t-form-item label="组内站点" name="stationList" class="block">
          <div>
            <SearchStation ref="searchRef1" @searchChange="searchChange" style="position: static; margin-bottom: 10px"></SearchStation>
            <!-- 可拖拽容器 -->
            <div ref="tagListRef1" class="tag-list">
              <span v-for="(item, i) in extentList" :key="item.stationIdD" class="tag">
                <span class="tag-text">{{ item.label }}</span>
                <span class="tag-close" @click="closeGroup(i)">×</span>
              </span>
            </div>
          </div>
        </t-form-item>
      </t-form>
    </template>
    <template #button>
      <div class="btn-box" style="border-top: 1px solid #1b2c7c;">
        <t-button class="cancel-btn" @click="newClose">取消</t-button>
        <t-button class="confirm-btn" @click="newconfirm">确定</t-button>
      </div>
    </template>
  </dialog-vue>
</template>
<script setup lang="ts">
import { nextTick, ref, onMounted, reactive } from 'vue'
import DialogVue from '@/components/DialogVue.vue'
import CheckboxList from '@/components/CheckboxList/index.vue'
// import SearchVue from './Search1.vue'
import SearchStation from '@/components/SearchStation.vue'
import { ModelService } from '@/api'
import { MessagePlugin } from 'tdesign-vue-next'
import type { PageInfo, FormInstanceFunctions } from 'tdesign-vue-next'
import { useRoute, useRouter } from "vue-router";
import type { RolePermission, RoleFormData, UserRole } from '@/types/PagesType'
import { HiIcon } from "hoci";
import editSvg from '@/assets/icon/edit.png'
import deleteSvg from '@/assets/icon/delete.png'
declare const Sortable: any

let searchRef = ref(null)
let searchRef1 = ref(null)
const router = useRouter();
const searchChange = async (n: any) => {
  // console.log('n', n)
  if (extentList.value.filter(e => e.stationIdD == n.stationIdD).length > 0) {
    return
  }
  n.value = n.stationIdD
  extentList.value.push(n)
  // 新增后 DOM 变化，确保 Sortable 正常
  await nextTick()
}
async function closeGroup(i) {
  extentList.value.splice(i, 1)
}
let height = ref(294 / window.devicePixelRatio)
window.onresize = () => {
  return (() => {
    height.value = 294 / window.devicePixelRatio
  })()
}

let currentRoleName = ref('')
const search = async () => {
  await getTableData()
}
let newVisible = ref(false)
const openNewUser = () => {
  formRef.value?.clearValidate()
  newVisible.value = true
  extentList.value = []
}

let columns = [
  { colKey: 'index', width: 70, title: '序号' },
  { colKey: 'taskName', width: 220, title: '分组名称' },
  { colKey: 'stationList', title: '组内站点' , },
  {
    colKey: 'operation',
    title: '操作',
    width: 200
  },
]
const isLoading = ref(false);
const tableData = ref([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
});

const getTableData = async () => {
  try {
    isLoading.value = true;
    let params = {
      // taskName: currentRoleName.value,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    const res = await ModelService.queryTaskListAndStations(params)
    pagination.total = res.total || 0;
    if (res) {
      tableData.value = res.data.map((item, index: number) => {
        return {
          ...item,
          // stationList: item.stations,
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

const onPageSizeChange = async (size: number, pageInfo: PageInfo) => {
  pagination.current = pageInfo.current;
  pagination.pageSize = size;
  await getTableData()
};
const onCurrentChange = async (current: number) => {
  pagination.current = current;
  await getTableData()
};

let formUpdateRef = ref<FormInstanceFunctions>()
let updateVisible = ref(false)
let updateId = ''
const updateRoleInfo = (row: any) => {
  formData.taskName = row.taskName
  // formData.stationList = formatPermissionByKey(row.stationList)
  // formData.stationList = row.stationList
  extentList.value = JSON.parse(JSON.stringify(row.stationList))
  extentList.value.forEach(e => {
    e.label = `${e.stationName||''}${e.stationIdD}`
  })
  updateId = row.id

  formUpdateRef.value?.clearValidate()
  updateVisible.value = true
}
const update = () => {
  formUpdateRef.value?.validate().then(async (res: any) => {
    if (res !== true) {
      throw new Error("表单校验失败")
    }

    // let tempPermissions: RolePermission[] = []
    // formData.stationList.forEach(p => {
    //   if (typeof p === 'string') {
    //     let obj = extentList.value.find((x: RolePermission) => x.permKey === p)
    //     let newObj = Object.assign({}, obj);
    //     delete newObj.label
    //     delete newObj.value
    //     tempPermissions.push(newObj)
    //   }
    // })

    let params = {
      id: updateId,
      taskName: formData.taskName,
      stationList: extentList.value,
    }

    try {
      await ModelService.modifyTaskList(params)
      MessagePlugin.success('修改成功')
      newClose()
      await getTableData()
    } catch (error) {
      MessagePlugin.error('修改失败')
    }
  }).catch((errors) => {
    // 校验失败
    console.log(errors);
  })
}


let delVisible = ref(false)
let deleteId = ''
const deleteRole = async (row: any) => {
  // try {
  //   const res = await ModelService.deleteTask({
  //     taskId: row.id
  //   })
    
  //   deleteId = row.id
  //   // delVisible.value = true
  //   getTableData()
  // } catch (error) { }
  deleteId=row.id
  delVisible.value = true
}

const delClose = () => {
  deleteId = ''
  delVisible.value = false
}

const delConfirm = async () => {
  try {
    let params = {
      taskId: deleteId,
    }
    await ModelService.deleteTask(params)
    //await getTableData()
    MessagePlugin.success('删除成功')
  } catch (error) {
    MessagePlugin.error('删除失败')
  } finally {
    deleteId = ''
    delVisible.value = false
    getTableData()
  }
}

const formRef = ref<FormInstanceFunctions>()
const formData = reactive({
  taskName: '',
  stationList: []
});
const baseRules = {
  taskName: [
    { required: true, message: "请输入分组名称", type: "error", trigger: 'blur' },
    {
      max: 20,
      message: '分组名称不超过20字符',
      trigger: 'blur',
    }
  ],
  // stationList: [{ required: true, message: "请选择组内站点", type: "error" }],
};

const extentList = ref([
  // {
  //   label: '111',
  //   stationIdD: '111'
  // },
  // {
  //   label: '222',
  //   stationIdD: '222'
  // },
])
const tagListRef = ref(null)
const tagListRef1 = ref(null)

const newClose = async () => {
  // if(updateVisible.value) {//编辑名称
  //   let params = {
  //     id: updateId,
  //     taskName: formData.taskName,
  //   }

  //   try {
  //     await ModelService.modifyTaskName(params)
  //     MessagePlugin.success('修改成功')
  //   } catch (error) {
  //     MessagePlugin.error('修改失败')
  //   }
  // }
  newVisible.value && (newVisible.value = false)
  updateVisible.value && (updateVisible.value = false)
  formData.taskName = ''
  formData.stationList = []
  searchRef && searchRef.value.close()
  searchRef1 && searchRef1.value.close()
  getTableData()

}

const confirm = () => {
  formRef.value?.validate().then(async (res) => {
    if (res !== true) {
      throw new Error("表单校验失败")
    }

    // 校验通过
    let tempPermissions = []
    // formData.stationList.forEach(p => {
    //   if (typeof p === 'string') {
    //     let obj = extentList.value.find((x) => x.permKey === p)
    //     let newObj = Object.assign({}, obj);
    //     delete newObj.label
    //     delete newObj.value
    //     tempPermissions.push(newObj)
    //   }
    // })

    extentList.value.forEach((item, i) => {
      item.index = i
    })
    let params = {
      taskName: formData.taskName,
      // stationList: tempPermissions,
      stationList: extentList.value,
    }
    try {
      const res = await ModelService.addTaskList(params)
      if(res === -1) {
        MessagePlugin.error('分组名重复，请修改后提交')
      } else {
        MessagePlugin.success('分组增加成功')
        newClose()
        await getTableData()
      }
      
    } catch (error) {
      MessagePlugin.error('新增分组失败')
    }
  }).catch((errors) => {
    // 校验失败
    console.log(errors);
  })
}

const newconfirm = () => {
  formRef.value?.validate().then(async (res) => {
    if (res !== true) {
      throw new Error("表单校验失败")
    }

    // 校验通过
    let tempPermissions = []
    // formData.stationList.forEach(p => {
    //   if (typeof p === 'string') {
    //     let obj = extentList.value.find((x) => x.permKey === p)
    //     let newObj = Object.assign({}, obj);
    //     delete newObj.label
    //     delete newObj.value
    //     tempPermissions.push(newObj)
    //   }
    // })

    extentList.value.forEach((item, i) => {
      item.index = i
    })
    let params = {
      id: updateId,
      taskName: formData.taskName,
      // stationList: tempPermissions,
      stationList: extentList.value,
    }
    try {
      const res = await ModelService.modifyTaskList(params)
      if(res === -1) {
        MessagePlugin.error('分组名重复，请修改后提交')
      } else {
        MessagePlugin.success('分组增加成功')
        newClose()
        await getTableData()
      }
      
    } catch (error) {
      MessagePlugin.error('新增分组失败')
    }
  }).catch((errors) => {
    // 校验失败
    console.log(errors);
  })
}

defineExpose({
  currentRoleName,
  search
})

search()

onMounted(() => {
  if (tagListRef.value) {
    new Sortable(tagListRef.value!, {
      animation: 150,
      draggable: '.tag', // ⭐ 非常重要
      ghostClass: 'sortable-ghost',

      onEnd(evt) {
        const { oldIndex, newIndex } = evt
        if (oldIndex === newIndex) return

        const moved = extentList.value.splice(oldIndex, 1)[0]
        extentList.value.splice(newIndex, 0, moved)
      }
    })
  }

  if (tagListRef1.value) {
    new Sortable(tagListRef1.value!, {
      animation: 150,
      draggable: '.tag', // ⭐ 非常重要
      ghostClass: 'sortable-ghost',

      onEnd(evt) {
        const { oldIndex, newIndex } = evt
        if (oldIndex === newIndex) return

        const moved = extentList.value.splice(oldIndex, 1)[0]
        extentList.value.splice(newIndex, 0, moved)
      }
    })
  }
})
</script>

<style lang="less" scoped></style>
<style lang="less">
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

.group-dialog {
  width: 580px;
  height: auto;

  .t-dialog__body {
    padding-top: 0;
    padding-bottom: 0;

    .item-container {
      margin-top: 16px;

      .t-form__item {
        width: 100%;
        margin-bottom: 16px;
        margin-right: 0;

        .t-form__label {
          float: none;
          color: var(--app-text-color);
          font-size: 16px;
        }

        .t-form__controls {
          margin-left: 0 !important;
        }
      }

      .t-form__item:last-child {
        margin-bottom: 0;
      }

      .t-input__wrap {
        width: 100%;
        height: 36px;

        .t-input {
          width: 100%;
          height: 36px;
        }
      }

      .t-textarea {
        .t-textarea__inner {
          width: 100%;
          height: 96px !important;
        }
      }

      .checkbox-group {
        .t-checkbox {
          width: 160px;
        }
      }
    }

  }
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  max-height: 104px;
  overflow-y: auto;
}

.tag {
  display: inline-flex;
  align-items: center;
  padding: 0 8px;
  height: 24px;
  font-size: 12px;
  background: #0052d9;
  color: #fff;
  border-radius: 4px;
  margin: 0 10px 10px 0;
  cursor: move;
}

.tag-close {
  margin-left: 6px;
  cursor: pointer;
  font-weight: bold;
}

.tag-close:hover {
  opacity: 0.8;
}

.sortable-ghost {
  opacity: 0.4;
}
</style>