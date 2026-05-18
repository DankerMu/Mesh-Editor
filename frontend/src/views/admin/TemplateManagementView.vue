<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import AppHeader from '@/components/AppHeader.vue'
import { getTemplate, getTemplates, updateTemplate } from '@/api/templates'
import type { ReviewTemplateDetail, ReviewTemplateSummary } from '@/api/templates'
import { useAuthStore } from '@/stores/authStore'

const authStore = useAuthStore()
const templates = ref<ReviewTemplateSummary[]>([])
const selectedTemplate = ref<ReviewTemplateDetail | null>(null)
const editorVisible = ref(false)
const editorText = ref('')
const error = ref('')
const canEdit = computed(() => authStore.role === 'admin')

async function loadTemplates() {
  const response = await getTemplates()
  templates.value = response.data
  if (!selectedTemplate.value && response.data[0]) {
    await selectTemplate(response.data[0].template_id)
  }
}

async function selectTemplate(templateId: string) {
  const response = await getTemplate(templateId)
  selectedTemplate.value = response.data
}

function openEditor() {
  if (!selectedTemplate.value) {
    return
  }
  editorText.value = JSON.stringify(selectedTemplate.value, null, 2)
  error.value = ''
  editorVisible.value = true
}

async function saveTemplate() {
  if (!selectedTemplate.value) {
    return
  }

  try {
    const payload = JSON.parse(editorText.value) as ReviewTemplateDetail
    await updateTemplate(selectedTemplate.value.template_id, payload)
    await selectTemplate(selectedTemplate.value.template_id)
    editorVisible.value = false
    MessagePlugin.success('模板已保存')
  } catch (err) {
    error.value = err instanceof SyntaxError ? 'JSON 格式不正确' : '模板保存失败'
  }
}

onMounted(async () => {
  await loadTemplates()
})
</script>

<template>
  <div class="page-shell">
    <AppHeader />
    <main class="content-wrap workspace-page">
      <section class="workspace-panel template-layout">
        <aside class="template-list">
          <h1 class="workspace-title">模板管理</h1>
          <button
            v-for="template in templates"
            :key="template.template_id"
            type="button"
            :data-test="`template-${template.template_id}`"
            @click="selectTemplate(template.template_id)"
          >
            <strong>{{ template.template_name }}</strong>
            <span>{{ template.panel_count }} 个面板</span>
          </button>
        </aside>

        <section v-if="selectedTemplate" class="template-detail" data-test="template-detail">
          <div class="workspace-panel__header">
            <div>
              <h2 class="workspace-title">{{ selectedTemplate.template_name }}</h2>
              <p class="workspace-desc">{{ selectedTemplate.template_id }}</p>
            </div>
            <t-button v-if="canEdit" data-test="template-edit" @click="openEditor">编辑</t-button>
          </div>

          <t-card title="必需字段" bordered>
            <p>{{ selectedTemplate.required_fields.join('、') }}</p>
          </t-card>
          <t-card title="可选字段" bordered>
            <p>{{ selectedTemplate.optional_fields.join('、') || '-' }}</p>
          </t-card>
          <t-card title="面板结构" bordered>
            <ul>
              <li v-for="panel in selectedTemplate.panels" :key="panel.id">
                {{ panel.id }} / {{ panel.type }} / {{ panel.fields.join('、') }}
              </li>
            </ul>
          </t-card>
        </section>
      </section>
    </main>

    <t-dialog v-model:visible="editorVisible" header="编辑模板">
      <t-textarea v-model="editorText" class="json-editor" data-test="template-json" />
      <p v-if="error" class="form-error">{{ error }}</p>
      <template #footer>
        <t-button data-test="template-save" @click="saveTemplate">保存</t-button>
      </template>
    </t-dialog>
  </div>
</template>
