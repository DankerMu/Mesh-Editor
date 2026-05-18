<script setup lang="ts">
import { reactive, ref } from 'vue'
import type { FormProps } from 'tdesign-vue-next'
import { BrowseIcon, BrowseOffIcon } from 'tdesign-icons-vue-next'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { SYSTEM_NAME } from '@/constants/navigation'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const errorMessage = ref('')
const passwordVisible = ref(false)

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
    const responseCode =
      typeof error === 'object' &&
      error !== null &&
      'response' in error &&
      typeof error.response === 'object' &&
      error.response !== null &&
      'data' in error.response &&
      typeof error.response.data === 'object' &&
      error.response.data !== null &&
      'code' in error.response.data &&
      typeof error.response.data.code === 'string'
        ? error.response.data.code
        : null

    errorMessage.value =
      responseCode === 'USER_DISABLED'
        ? '账号已被禁用，请联系管理员'
        : '登录失败，请检查用户名和密码'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <aside class="login-brand" aria-hidden="true">
      <div class="login-brand__content">
        <h1 class="login-brand__title">{{ SYSTEM_NAME }}</h1>
        <p class="login-brand__desc">专业的气象数据网格编辑与质量控制平台</p>
      </div>
    </aside>

    <section class="login-right">
      <div class="login-card" aria-labelledby="login-title">
        <h2 id="login-title" class="login-card__title">登录</h2>
        <p class="login-card__subtitle">请输入账号密码登录工作台</p>

        <t-alert v-if="errorMessage" class="login-alert" theme="error" :message="errorMessage" />

        <t-form :data="formData" :rules="rules" label-align="top" @submit="onSubmit">
          <t-form-item label="用户名" name="username">
            <t-input v-model="formData.username" placeholder="请输入用户名" clearable />
          </t-form-item>
          <t-form-item label="密码" name="password">
            <t-input
              v-model="formData.password"
              :type="passwordVisible ? 'text' : 'password'"
              placeholder="请输入密码"
              clearable
            >
              <template #suffixIcon>
                <span class="password-toggle" @click="passwordVisible = !passwordVisible">
                  <BrowseIcon v-if="passwordVisible" />
                  <BrowseOffIcon v-else />
                </span>
              </template>
            </t-input>
          </t-form-item>
          <t-form-item>
            <t-button block theme="primary" type="submit" :loading="loading">登录</t-button>
          </t-form-item>
        </t-form>
      </div>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
}

.login-brand {
  display: flex;
  flex: 0 0 50%;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1664FF 0%, #4080FF 100%);
  padding: 40px;
}

.login-brand__content {
  max-width: 400px;
  text-align: center;
}

.login-brand__title {
  margin: 0 0 16px;
  color: #fff;
  font-size: 32px;
  line-height: 40px;
  font-weight: 700;
}

.login-brand__desc {
  margin: 0;
  color: rgba(255, 255, 255, 0.85);
  font-size: 16px;
  line-height: 24px;
}

.login-right {
  display: flex;
  flex: 0 0 50%;
  align-items: center;
  justify-content: center;
  background: var(--page-bg);
  padding: 40px 24px;
}

.login-card {
  width: 100%;
  max-width: 400px;
  padding: 40px;
  border-radius: var(--radius-card);
  background: var(--card-bg);
  box-shadow: var(--shadow-card);
}

.login-card__title {
  margin: 0 0 8px;
  font-size: var(--font-size-title-lg);
  line-height: var(--line-height-title-lg);
  font-weight: 600;
}

.login-card__subtitle {
  margin: 0 0 24px;
  color: var(--text-secondary);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
}

.password-toggle {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  color: var(--text-placeholder);
  transition: color 0.2s;
}

.password-toggle:hover {
  color: var(--text-primary);
}

@media (max-width: 1024px) {
  .login-brand {
    display: none;
  }

  .login-right {
    flex: 1 1 100%;
  }
}
</style>
