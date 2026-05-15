<template>
  <div class="operate-container">
    <SearchVue v-model:useIdIpt="currentRoleName"></SearchVue>
    <div class="operation-list" style="margin-left: 2.25rem">
      <t-button theme="primary" class="search" style="margin-right: 1.125rem;" @click="search">
        <img src="@/assets/icon/search.png" alt="">
        查询
      </t-button>
      <t-button class="new" @click="openNewUser">
        <img src="@/assets/icon/add.png" alt="">
        新增角色
      </t-button>
    </div>

  </div>
  <t-table row-key="index" :data="tableData" :columns="columns" bordered :max-height="height" headerAffixedTop
    :scroll="{ type: 'virtual' }" lazy-load>
    <template #permissions="{ row }">
      {{ formatPermissionByName(row.permissions) }}
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
          <p class="bold">确定要删除该角色吗？此操作无法撤销</p>
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

  <!-- 新增用户 -->
  <dialog-vue :visible="newVisible" header="新增角色" :customClass="'newrole-dialog custom-dialog-footer'"
    :close="newClose">
    <template #content>
      <t-form ref="formRef" :data="formData" class="item-container form-color-ipt" :rules="baseRules" label-width="auto"
        label-align="left" scrollToFirstError="smooth">
        <t-form-item label="角色名称" name="roleName">
          <t-input v-model="formData.roleName" clearable placeholder="请输入角色名称"></t-input>
        </t-form-item>
        <t-form-item label="权限范围" name="permissions">
          <CheckboxList title="" v-model:modelValue="formData.permissions" :options="extentList"></CheckboxList>
        </t-form-item>
        <t-form-item label="描述" name="roleDesc">
          <t-textarea v-model="formData.roleDesc" placeholder="请输入角色描述" :autosize="{ minRows: 4, maxRows: 4 }" />
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

  <!-- 修改用户信息 -->
  <dialog-vue :visible="updateVisible" :header="'修改角色信息'" :customClass="'newrole-dialog custom-dialog-footer'"
    :close="newClose">
    <template #content>
      <t-form ref="formUpdateRef" :data="formData" class="item-container form-color-ipt" :rules="baseRules"
        label-width="auto" label-align="left" scrollToFirstError="smooth">
        <t-form-item label="角色名称" name="roleName">
          <t-input v-model="formData.roleName" clearable placeholder="请输入角色名称"></t-input>
        </t-form-item>
        <t-form-item label="权限范围" name="permissions">
          <CheckboxList title="" v-model:modelValue="formData.permissions" :options="extentList"></CheckboxList>
        </t-form-item>
        <t-form-item label="描述" name="roleDesc">
          <t-textarea v-model="formData.roleDesc" placeholder="请输入角色描述" :autosize="{ minRows: 4, maxRows: 4 }" />
        </t-form-item>
      </t-form>
    </template>
    <template #button>
      <div class="btn-box" style="border-top: 1px solid #1b2c7c;">
        <t-button class="cancel-btn" @click="newClose">取消</t-button>
        <t-button class="confirm-btn" @click="update">确定</t-button>
      </div>
    </template>
  </dialog-vue>
</template>

<script setup lang="ts">
import { nextTick, ref, onMounted, reactive } from 'vue'
import DialogVue from '@/components/DialogVue.vue'
import CheckboxList from '@/components/CheckboxList/index.vue'
import SearchVue from './Search1.vue'
import { UserService } from '@/api'
import { MessagePlugin } from 'tdesign-vue-next'
import type { PageInfo, FormInstanceFunctions } from 'tdesign-vue-next'
import { useRoute, useRouter } from "vue-router";
import type { RolePermission, RoleFormData, UserRole } from '@/types/PagesType'
import { HiIcon } from "hoci";
import editSvg from '@/assets/icon/edit.png'
import deleteSvg from '@/assets/icon/delete.png'

const router = useRouter();

let height = ref(460 / window.devicePixelRatio)
window.onresize = () => {
  return (() => {
    height.value = 460 / window.devicePixelRatio
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
}

let columns = [
  { colKey: 'index', width: 70, title: '序号' },
  { colKey: 'roleName', width: 220, title: '角色名称' },
  { colKey: 'permissions', title: '权限范围' },
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
      roleName: currentRoleName.value,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    const res = await UserService.getRolePermissionList(params)
    pagination.total = res.total || 0;
    if (res && res.data) {
      tableData.value = res.data.map((item: UserRole, index: number) => {
        return {
          ...item,
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

const formatPermissionByName = (data: any) => {
  return [...data.map((obj: any) => obj.permName)].join('、')
}
const formatPermissionByKey = (data: any) => {
  return [...data.map((obj: any) => obj.permKey)]
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
  formData.roleName = row.roleName
  formData.permissions = formatPermissionByKey(row.permissions)
  formData.roleDesc = row.roleDesc
  updateId = row.id

  formUpdateRef.value?.clearValidate()
  updateVisible.value = true
}
const update = () => {
  formUpdateRef.value?.validate().then(async (res: any) => {
    if (res !== true) {
      throw new Error("表单校验失败")
    }

    let tempPermissions: RolePermission[] = []
    formData.permissions.forEach(p => {
      if (typeof p === 'string') {
        let obj = extentList.value.find((x: RolePermission) => x.permKey === p)
        let newObj = Object.assign({}, obj);
        delete newObj.label
        delete newObj.value
        tempPermissions.push(newObj)
      }
    })

    let params = {
      id: updateId,
      roleName: formData.roleName,
      permissions: tempPermissions,
      roleDesc: formData.roleDesc
    }

    try {
      await UserService.updateRole(params)

      // 修改当前登录角色
      const userInfo = sessionStorage.getItem("userInfo")
      if (userInfo) {
        const rolename = JSON.parse(userInfo).rolename
        if (rolename === formData.roleName) {
          MessagePlugin.success('修改成功，请重新登录')
          return router.push('/login')
        }
      }

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
  try {
    const res = await UserService.queryUserwithRole({
      id: row.id
    })
    if (res && res > 0) {
      MessagePlugin.warning('该角色有关联用户，请先取消关联再删除')
    } else {
      deleteId = row.id
      delVisible.value = true
    }
  } catch (error) { }
}

const delClose = () => {
  deleteId = ''
  delVisible.value = false
}

const delConfirm = async () => {
  try {
    let params = {
      id: deleteId,
    }
    await UserService.deleteRole(params)
    await getTableData()
    MessagePlugin.success('删除成功')
  } catch (error) {
    MessagePlugin.error('删除失败')
  } finally {
    deleteId = ''
    delVisible.value = false
  }
}

const formRef = ref<FormInstanceFunctions>()
const formData = reactive<RoleFormData>({
  roleName: '',
  permissions: [],
  roleDesc: ''
});
const baseRules = {
  roleName: [
    { required: true, message: "请输入角色名称", type: "error", trigger: 'blur' },
    {
      max: 20,
      message: '角色名称不超过20字符',
      trigger: 'blur',
    }
  ],
  permissions: [{ required: true, message: "请选择权限范围", type: "error" }],
  roleDesc: [
    {
      max: 500,
      message: '描述不超过500字符',
      trigger: 'blur',
    }
  ]
};

const extentList = ref<RolePermission[]>([])
const getPermissionList = async () => {
  try {
    const res = await UserService.getPermissionsList()
    if (res) {
      extentList.value = res.map((item: RolePermission) => {
        return {
          ...item,
          label: item.permName,
          value: item.permKey
        }
      })
    }
  } catch (error) { }
}

getPermissionList()

const newClose = () => {
  newVisible.value && (newVisible.value = false)
  updateVisible.value && (updateVisible.value = false)
  formData.roleName = ''
  formData.permissions = []
  formData.roleDesc = ''
}

const confirm = () => {
  formRef.value?.validate().then(async (res) => {
    if (res !== true) {
      throw new Error("表单校验失败")
    }

    // 校验通过
    let tempPermissions: RolePermission[] = []
    formData.permissions.forEach(p => {
      if (typeof p === 'string') {
        let obj = extentList.value.find((x: RolePermission) => x.permKey === p)
        let newObj = Object.assign({}, obj);
        delete newObj.label
        delete newObj.value
        tempPermissions.push(newObj)
      }
    })

    let params = {
      roleName: formData.roleName,
      permissions: tempPermissions,
      roleDesc: formData.roleDesc
    }
    try {
      await UserService.addNewRole(params)
      MessagePlugin.success('角色增加成功')
      newClose()
      await getTableData()
    } catch (error) {
      MessagePlugin.error('新增角色失败')
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

.newrole-dialog {
  width: 580px;
  height: 574px;

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
</style>