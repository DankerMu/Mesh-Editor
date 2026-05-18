<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import AppHeader from '@/components/AppHeader.vue'
import { getConfig, getConfigHistory, updateConfig } from '@/api/config'
import type { ConfigSnapshot, ConfigType } from '@/api/config'

const configTypes: Array<{ label: string; value: ConfigType }> = [
  { label: '产品配置', value: 'product_config' },
  { label: '绘图配置', value: 'plot_config' },
  { label: '模板配置', value: 'template_config' },
]

const activeType = ref<ConfigType>('product_config')
const editorText = ref('{}')
const history = ref<ConfigSnapshot[]>([])
const error = ref('')

function formatDate(value: string) {
  return new Date(value).toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai', hour12: false })
}

async function loadConfig(type = activeType.value) {
  activeType.value = type
  error.value = ''
  const [configResponse, historyResponse] = await Promise.all([
    getConfig(type),
    getConfigHistory(type, { limit: 20 }),
  ])
  editorText.value = JSON.stringify(configResponse.data, null, 2)
  history.value = historyResponse.data.items
}

async function saveConfig() {
  try {
    const payload = JSON.parse(editorText.value) as Record<string, unknown>
    await updateConfig(activeType.value, payload)
    await loadConfig(activeType.value)
    MessagePlugin.success('配置已保存')
  } catch (err) {
    error.value = err instanceof SyntaxError ? 'JSON 格式不正确' : '配置保存失败'
  }
}

onMounted(async () => {
  await loadConfig()
})
</script>

<template>
  <div class="page-shell">
    <AppHeader />
    <main class="content-wrap workspace-page">
      <section class="workspace-panel">
        <div class="workspace-panel__header">
          <div>
            <h1 class="workspace-title">配置管理</h1>
            <p class="workspace-desc">维护产品、绘图和模板配置快照。</p>
          </div>
        </div>

        <t-tabs v-model="activeType" data-test="config-tabs" @update:model-value="loadConfig">
          <t-tab-panel
            v-for="item in configTypes"
            :key="item.value"
            :value="item.value"
            :label="item.label"
          />
        </t-tabs>

        <div class="config-editor">
          <t-textarea v-model="editorText" class="json-editor" data-test="config-json" />
          <p v-if="error" class="form-error" data-test="config-error">{{ error }}</p>
          <t-button theme="primary" data-test="config-save" @click="saveConfig">保存配置</t-button>
        </div>

        <t-card title="历史版本" bordered>
          <ul class="history-list" data-test="config-history">
            <li v-for="item in history" :key="item.snapshot_id">
              <strong>{{ item.snapshot_id }}</strong>
              <span>{{ item.changed_by ?? '-' }}</span>
              <time>{{ formatDate(item.created_at) }}</time>
            </li>
          </ul>
        </t-card>
      </section>
    </main>
  </div>
</template>
