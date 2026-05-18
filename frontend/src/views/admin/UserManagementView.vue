<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import type { BaseTableCol } from 'tdesign-vue-next'
import AppHeader from '@/components/AppHeader.vue'
import { useAdminStore } from '@/stores/adminStore'
import type { UserItem, UserRole } from '@/api/admin'

const adminStore = useAdminStore()
const dialogVisible = ref(false)
const editingUser = ref<UserItem | null>(null)
const formError = ref('')
const form = reactive({
  username: '',
  display_name: '',
  password: '',
  role: 'forecaster' as UserRole,
})

const roleOptions = [
  { label: '管理员', value: 'admin' },
  { label: '审核员', value: 'reviewer' },
  { label: '预报员', value: 'forecaster' },
  { label: '观察员', value: 'viewer' },
]

const userColumns: BaseTableCol[] = [
  { colKey: 'username', title: '用户名' },
  { colKey: 'display_name', title: '显示名' },
  { colKey: 'role', title: '角色', cell: 'role' },
  { colKey: 'is_active', title: '状态', cell: 'is_active' },
  { colKey: 'last_login_at', title: '最近登录', cell: 'last_login_at' },
  { colKey: 'op', title: '操作', cell: 'op' },
]

const dialogTitle = computed(() => (editingUser.value ? '编辑用户' : '创建用户'))

function roleLabel(role: string) {
  return roleOptions.find((option) => option.value === role)?.label ?? role
}

function roleTheme(role: string): 'danger' | 'warning' | 'primary' | 'default' {
  switch (role) {
    case 'admin': return 'danger'
    case 'reviewer': return 'warning'
    case 'forecaster': return 'primary'
    default: return 'default'
  }
}

function formatDate(value: string | null) {
  return value
    ? new Date(value).toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai', hour12: false })
    : '-'
}

function resetForm() {
  form.username = ''
  form.display_name = ''
  form.password = ''
  form.role = 'forecaster'
  formError.value = ''
}

function openCreateDialog() {
  editingUser.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(user: UserItem) {
  editingUser.value = user
  form.username = user.username
  form.display_name = user.display_name
  form.password = ''
  form.role = user.role as UserRole
  formError.value = ''
  dialogVisible.value = true
}

async function submitUser() {
  if (!form.display_name || !form.role || (!editingUser.value && (!form.username || !form.password))) {
    formError.value = '请填写完整用户信息'
    return
  }

  if (editingUser.value) {
    await adminStore.updateUser(editingUser.value.id, {
      display_name: form.display_name,
      role: form.role,
    })
    MessagePlugin.success('用户已更新')
  } else {
    await adminStore.createUser({
      username: form.username,
      display_name: form.display_name,
      password: form.password,
      role: form.role,
    })
    MessagePlugin.success('用户已创建')
  }

  dialogVisible.value = false
}

async function toggleActive(user: UserItem, value: boolean) {
  await adminStore.updateUser(user.id, { is_active: value })
}

onMounted(async () => {
  await adminStore.fetchUsers()
})
</script>

<template>
  <div class="page-shell">
    <AppHeader />
    <main class="content-wrap workspace-page">
      <section class="workspace-panel">
        <div class="workspace-panel__header">
          <div>
            <h1 class="workspace-title">用户管理</h1>
            <p class="workspace-desc">维护系统用户、角色和启停状态。</p>
          </div>
          <t-button theme="primary" data-test="create-user-button" @click="openCreateDialog">
            创建用户
          </t-button>
        </div>

        <t-table
          :data="adminStore.users"
          :columns="userColumns"
          row-key="id"
          data-test="user-table"
        >
          <template #role="{ row }">
            <t-tag :theme="roleTheme(row.role)" variant="light">{{ roleLabel(row.role) }}</t-tag>
          </template>
          <template #is_active="{ row }">
            <t-switch
              :model-value="row.is_active"
              :data-test="`user-active-${row.id}`"
              @update:model-value="toggleActive(row, Boolean($event))"
            />
          </template>
          <template #last_login_at="{ row }">
            {{ formatDate(row.last_login_at) }}
          </template>
          <template #op="{ row }">
            <t-button :data-test="`edit-user-${row.id}`" @click="openEditDialog(row)">
              编辑
            </t-button>
          </template>
        </t-table>
      </section>
    </main>

    <t-dialog v-model:visible="dialogVisible" :header="dialogTitle">
      <t-form data-test="user-form" @submit="submitUser">
        <t-form-item v-if="!editingUser" label="用户名">
          <t-input v-model="form.username" data-test="user-username" />
        </t-form-item>
        <t-form-item label="显示名">
          <t-input v-model="form.display_name" data-test="user-display-name" />
        </t-form-item>
        <t-form-item v-if="!editingUser" label="密码">
          <t-input v-model="form.password" type="password" data-test="user-password" />
        </t-form-item>
        <t-form-item label="角色">
          <t-select v-model="form.role" :options="roleOptions" data-test="user-role" />
        </t-form-item>
        <p v-if="formError" class="form-error" data-test="user-form-error">{{ formError }}</p>
      </t-form>
      <template #footer>
        <t-button data-test="user-submit" @click="submitUser">保存</t-button>
      </template>
    </t-dialog>
  </div>
</template>
