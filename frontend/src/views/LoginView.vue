<script setup lang="ts">
import { reactive, ref } from 'vue'
import type { FormProps } from 'tdesign-vue-next'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { SYSTEM_NAME } from '@/constants/navigation'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const errorMessage = ref('')

const formData = reactive({
  username: '',
  password: '',
})

const rules: FormProps['rules'] = {
  username: [{ required: true, message: '请输入用户名', type: 'error' }],
  password: [{ required: true, message: '请输入密码', type: 'error' }],
}

const onSubmit: FormProps['onSubmit'] = async ({ validateResult }) => {
  if (validateResult !== true) {
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    await authStore.login(formData.username, formData.password)
    await router.push('/')
  } catch (error: unknown) {
    const responseMessage =
      typeof error === 'object' &&
      error !== null &&
      'response' in error &&
      typeof error.response === 'object' &&
      error.response !== null &&
      'data' in error.response &&
      typeof error.response.data === 'object' &&
      error.response.data !== null &&
      'message' in error.response.data &&
      typeof error.response.data.message === 'string'
        ? error.response.data.message
        : null

    errorMessage.value = responseMessage ?? '登录失败，请检查用户名和密码'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="center-page">
    <section class="login-panel" aria-labelledby="login-title">
      <h1 id="login-title" class="login-title">{{ SYSTEM_NAME }}</h1>
      <p class="login-subtitle">请输入账号密码登录工作台</p>

      <t-alert v-if="errorMessage" class="login-alert" theme="error" :message="errorMessage" />

      <t-form :data="formData" :rules="rules" label-align="top" @submit="onSubmit">
        <t-form-item label="用户名" name="username">
          <t-input v-model="formData.username" placeholder="请输入用户名" clearable />
        </t-form-item>
        <t-form-item label="密码" name="password">
          <t-input v-model="formData.password" type="password" placeholder="请输入密码" clearable />
        </t-form-item>
        <t-form-item>
          <t-button block theme="primary" type="submit" :loading="loading">登录</t-button>
        </t-form-item>
      </t-form>
    </section>
  </main>
</template>
