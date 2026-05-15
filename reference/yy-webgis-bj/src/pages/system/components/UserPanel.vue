<template>
  <div class="operate-container">
    <SearchVue v-model:useIdIpt="username"></SearchVue>
    <div class="operation-list" style="margin-left: 2.25rem">
      <t-button theme="primary" class="search" style="margin-right: 1.125rem;" @click="search">
        <img src="@/assets/icon/search.png" alt="">
        查询
      </t-button>
      <t-button class="new" @click="openNewUser">
        <img src="@/assets/icon/add.png" alt="">
        新增用户
      </t-button>
    </div>

  </div>
  <t-table row-key="index" :data="tableData" :columns="columns" bordered :max-height="height" headerAffixedTop
    :scroll="{ type: 'virtual' }" lazy-load>
    <template #enabled="{ row }">
      <div class="flex align-center">
        <div style="width: 6px;height: 6px;border-radius: 3px;margin-right: 6px;"
          :style="{ 'background-color': row.enabled ? '#00D488' : '#9CA6CE' }"></div>
        <span :style="{ 'color': row.enabled ? '#00D488' : '#E9ECFA' }">{{ row.enabled ? '启用' : '未启用' }}</span>
      </div>
    </template>
    <template #operation="{ row }">
      <div class="table-btn">
        <t-button variant="text" class="replace-item" style="margin-right: 12px;" @click="updateUserInfo(row)">
          <HiIcon size="16px" :src="editSvg"></HiIcon>修改
        </t-button>
        <t-button variant="text" class="delete-item" @click="deleteUser(row)">
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
          <p class="bold">确定要删除该用户吗？此操作无法撤销</p>
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
  <dialog-vue :visible="newVisible" header="新增用户" :customClass="'newadd-dialog custom-dialog-footer'" :close="newClose">
    <template #content>
      <t-form ref="formRef" :data="formData" class="item-container form-color-ipt" layout="inline" :rules="baseRules"
        label-width="auto" label-align="left" scrollToFirstError="smooth">
        <t-form-item label="用户名称" name="username">
          <t-input v-model="formData.username" clearable placeholder="请输入用户名"></t-input>
        </t-form-item>
        <t-form-item label="登录名" name="loginname">
          <t-input v-model="formData.loginname" clearable placeholder="请输入登录名"></t-input>
        </t-form-item>
        <!-- <t-form-item label="邮箱" name="email">
          <t-input v-model="formData.email" clearable placeholder="请输入邮箱"></t-input>
        </t-form-item>
        <t-form-item label="电话" name="phone">
          <t-input v-model="formData.phone" clearable placeholder="请输入电话"></t-input>
        </t-form-item> -->
        <t-form-item label="角色" name="roles">
          <t-select v-model="formData.roles" :keys="roleKeys" :options="roleList" placeholder="请输入角色"
            :popupProps="{ overlayClassName: 'pure-select-popup' }" />
        </t-form-item>
        <t-form-item label="密码" name="password">
          <t-input v-model="formData.password" clearable placeholder="请输入密码"></t-input>
        </t-form-item>
        <t-form-item label="确认密码" name="pwdvalid">
          <t-input v-model="formData.pwdvalid" clearable placeholder="请再次输入密码"></t-input>
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
  <dialog-vue :visible="updateVisible" :header="'修改用户信息'" :customClass="'newadd-dialog custom-dialog-footer'"
    :close="newClose">
    <template #content>
      <t-form ref="formUpdateRef" :data="formData" class="item-container form-color-ipt" layout="inline"
        :rules="baseRules" label-width="auto" label-align="left" scrollToFirstError="smooth">
        <t-form-item label="用户名称" name="username">
          <t-input v-model="formData.username" disabled placeholder="请输入用户名"></t-input>
        </t-form-item>
        <t-form-item label="登录名" name="loginname">
          <t-input v-model="formData.loginname" clearable placeholder="请输入登录名"></t-input>
        </t-form-item>
        <!-- <t-form-item label="邮箱" name="email">
          <t-input v-model="formData.email" clearable placeholder="请输入邮箱"></t-input>
        </t-form-item>
        <t-form-item label="电话" name="phone">
          <t-input v-model="formData.phone" clearable placeholder="请输入电话"></t-input>
        </t-form-item> -->
        <t-form-item label="角色" name="roles">
          <t-select v-model="formData.roles" :keys="roleKeys" :options="roleList" placeholder="请输入角色"
            :popupProps="{ overlayClassName: 'pure-select-popup' }" />
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
import SearchVue from './Search.vue'
import { UserService } from '@/api'
import { MessagePlugin } from 'tdesign-vue-next'
import type { PageInfo, FormInstanceFunctions } from 'tdesign-vue-next'
import type { UserRole, UserRecords } from '@/types/PagesType'
import { HiIcon } from "hoci";
import editSvg from '@/assets/icon/edit.png'
import deleteSvg from '@/assets/icon/delete.png'

let height = ref(460 / window.devicePixelRatio)
window.onresize = () => {
  return (() => {
    height.value = 460 / window.devicePixelRatio
  })()
}

let username = ref('')
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
  { colKey: 'username', title: '用户名' },
  { colKey: 'loginname', title: '登录名' },
  { colKey: 'rolename', title: '角色' },
  // { colKey: 'enabled', title: '状态' },
  // { colKey: 'email', title: '邮箱' },
  // { colKey: 'phone', title: '电话' },
  {
    colKey: 'operation',
    title: '操作',
    width: 190
  },
]
const isLoading = ref(false);
const tableData = ref<UserRecords[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
});

const getTableData = async () => {
  try {
    isLoading.value = true;
    let params = {
      id: 0,
      username: username.value,
      loginname: "",
      role: "",
      enabled: false,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    const res = await UserService.getUserList(params)
    pagination.total = res.total || 0;
    if (res && res.data) {
      tableData.value = res.data.map((item: UserRecords, index: number) => {
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
const updateUserInfo = (row: any) => {
  formData.username = row.username
  formData.loginname = row.loginname
  formData.email = row.email
  formData.phone = row.phone
  formData.roles = row.roles?.[0]?.id ?? ''
  updateId = row.id

  formUpdateRef.value?.clearValidate()
  updateVisible.value = true
}
const update = () => {
  formUpdateRef.value?.validate().then(async (res) => {
    if (res !== true) {
      throw new Error("表单校验失败")
    }

    let role = roleList.value.find(x => x.id === formData.roles) || { permissions: [] }
    let newObj = { ...role, permissions: undefined }
    delete newObj.permissions

    let params = {
      id: updateId,
      username: formData.username,
      password: formData.password,
      loginname: formData.loginname,
      email: formData.email,
      phone: formData.phone,
      roles: [newObj]
    }

    try {
      await UserService.updateUserInfo(params)
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
const deleteUser = (row: any) => {
  deleteId = row.id
  delVisible.value = true
}

const delClose = () => {
  deleteId = ''
  delVisible.value = false
}

const delConfirm = async () => {
  try {
    await UserService.deleteUser(deleteId)
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
const formData = reactive({
  username: '',
  loginname: '',
  email: '',
  phone: '',
  roles: '',
  password: '',
  pwdvalid: ''
});

const baseRules = {
  username: [
    { required: true, message: "请输入用户名", type: "error", trigger: 'blur' },
    {
      max: 20,
      message: '用户名不超过20字符',
      trigger: 'blur',
    }
  ],
  loginname: [
    { required: true, message: "请输入登录名", type: "error", trigger: 'blur' },
    {
      max: 20,
      message: '登录名不超过20字符',
      trigger: 'blur',
    }
  ],
  // email: [{ required: true, message: "请输入邮箱", type: "error", trigger: 'blur' }],
  roles: [{ required: true, message: "请输入角色", type: "error" }],
  password: [
    { required: true, message: "请输入密码", type: "error", trigger: 'blur' },
    {
      max: 20,
      message: '密码不超过20字符',
      trigger: 'blur',
    }
  ],
  pwdvalid: [
    { required: true, message: "请再次输入密码", type: "error", trigger: 'blur' },
    {
      max: 20,
      message: '确认密码不超过20字符',
      trigger: 'blur',
    },
    {
      validator: (val: string) => {
        if (!val) return true;
        return val === formData.password;
      },
      message: '确认密码与新密码不一致',
      type: 'error',
      trigger: 'blur'
    }
  ],
};

const roleList = ref<UserRole[]>([])
const roleKeys = {
  value: 'id',
  label: 'roleName',
}
const getRoleList = async () => {
  try {
    const res = await UserService.getRoleList()
    res && (roleList.value = res)
  } catch (error) { }
}

getRoleList()

const newClose = () => {
  newVisible.value && (newVisible.value = false)
  updateVisible.value && (updateVisible.value = false)
  formData.username = ''
  formData.loginname = ''
  formData.email = ''
  formData.phone = ''
  formData.roles = ''
  formData.password = ''
  formData.pwdvalid = ''
}

const confirm = () => {
  formRef.value?.validate().then(async (res) => {
    if (res !== true) {
      throw new Error("表单校验失败")
    }

    // 校验通过
    let role = roleList.value.find(x => x.id === formData.roles) || { permissions: [] }
    let newObj = { ...role, permissions: undefined }
    delete newObj.permissions
    let params = {
      username: formData.username,
      password: formData.password,
      loginname: formData.loginname,
      email: formData.email,
      phone: formData.phone,
      enabled: false,
      roles: [newObj]
    }

    try {
      await UserService.addNewUser(params)
      MessagePlugin.success('用户增加成功')
      newClose()
      await getTableData()
    } catch (error) {
      MessagePlugin.error('新增用户失败')
    }
  }).catch((errors) => {
    // 校验失败
    console.log(errors);
  })
}

defineExpose({
  username,
  getTableData,
  getRoleList
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

.newadd-dialog {
  width: 712px;
  // height: 488px;

  .t-dialog__body {
    padding-top: 0;
    padding-bottom: 0;

    .item-container {
      margin-top: 20px;

      .t-form__item {
        width: 48%;
        margin-bottom: 0;
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

      .t-form__item:nth-child(odd) {
        margin-right: 20px;
      }

      .t-input__wrap {
        width: 100%;
        height: 36px;

        .t-input {
          width: 100%;
          height: 36px;
        }
      }
    }
  }
}
</style>