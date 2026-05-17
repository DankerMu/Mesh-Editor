import { defineStore } from 'pinia'
import { shallowRef, ref } from 'vue'
import { fetchField, loadSession as loadSessionApi, startSession as startSessionApi } from '@/api/sessions'
import type { EditOperationDTO, EditStats, MaskGeometry, ToolType, ViewMode } from '@/types/editor'
import { GRID_COLS, GRID_ROWS } from '@/constants/precipColors'

const FIELD_NAMES = [
  'qpf_before',
  'ptype_before',
  'qpf_after',
  'ptype_after',
  'touched_mask',
  'changed_mask',
  'invalid_mask',
] as const

type FieldName = (typeof FIELD_NAMES)[number]
type FieldBuffers = Record<FieldName, ArrayBuffer>

const UINT8_FIELD_LENGTH = GRID_ROWS * GRID_COLS
const FLOAT32_FIELD_LENGTH = UINT8_FIELD_LENGTH * Float32Array.BYTES_PER_ELEMENT

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : '字段加载失败'
}

function assertByteLength(fieldName: FieldName, buffer: ArrayBuffer) {
  const expectedLength = fieldName.startsWith('qpf_') ? FLOAT32_FIELD_LENGTH : UINT8_FIELD_LENGTH
  if (buffer.byteLength !== expectedLength) {
    throw new Error(
      `${fieldName} byte-length mismatch: expected ${expectedLength}, got ${buffer.byteLength}`,
    )
  }
}

export const useEditorStore = defineStore('editor', () => {
  const sessionId = ref<string | null>(null)
  const windowId = ref<string | null>(null)
  const baseVersionId = ref<string | null>(null)
  const currentVersionId = ref<string | null>(null)
  const qpfBefore = shallowRef<Float32Array | null>(null)
  const ptypeBefore = shallowRef<Uint8Array | null>(null)
  const qpfAfter = shallowRef<Float32Array | null>(null)
  const ptypeAfter = shallowRef<Uint8Array | null>(null)
  const touchedMask = shallowRef<Uint8Array | null>(null)
  const changedMask = shallowRef<Uint8Array | null>(null)
  const invalidMask = shallowRef<Uint8Array | null>(null)
  const activeTool = ref<ToolType | null>(null)
  const currentMaskGeometry = ref<MaskGeometry | null>(null)
  const selectedViewMode = ref<ViewMode>('after')
  const previewId = ref<string | null>(null)
  const previewStats = ref<EditStats | null>(null)
  const operations = ref<EditOperationDTO[]>([])
  const canUndo = ref(false)
  const canRedo = ref(false)
  const dirty = ref(false)
  const loadingSession = ref(false)
  const loadingFields = ref(false)
  const previewLoading = ref(false)
  const applyLoading = ref(false)
  const saveLoading = ref(false)
  const fieldLoadError = ref<string | null>(null)
  const previewError = ref<string | null>(null)

  function clearArrays() {
    qpfBefore.value = null
    ptypeBefore.value = null
    qpfAfter.value = null
    ptypeAfter.value = null
    touchedMask.value = null
    changedMask.value = null
    invalidMask.value = null
  }

  function reset() {
    sessionId.value = null
    windowId.value = null
    baseVersionId.value = null
    currentVersionId.value = null
    clearArrays()
    activeTool.value = null
    currentMaskGeometry.value = null
    selectedViewMode.value = 'after'
    previewId.value = null
    previewStats.value = null
    operations.value = []
    canUndo.value = false
    canRedo.value = false
    dirty.value = false
    loadingSession.value = false
    loadingFields.value = false
    previewLoading.value = false
    applyLoading.value = false
    saveLoading.value = false
    fieldLoadError.value = null
    previewError.value = null
  }

  async function startSession(windowIdToStart: string) {
    loadingSession.value = true
    fieldLoadError.value = null

    try {
      const session = await startSessionApi(windowIdToStart)
      sessionId.value = session.session_id
      windowId.value = session.window_id
      baseVersionId.value = session.base_version_id
      currentVersionId.value = null
      await loadSession(session.session_id)
    } catch (error) {
      fieldLoadError.value = getErrorMessage(error)
    } finally {
      loadingSession.value = false
    }
  }

  async function loadSession(sessionIdToLoad: string) {
    loadingFields.value = true
    fieldLoadError.value = null
    clearArrays()

    try {
      const session = await loadSessionApi(sessionIdToLoad)
      sessionId.value = session.session_id
      windowId.value = session.window_id
      baseVersionId.value = session.base_version_id
      canUndo.value = session.can_undo
      canRedo.value = session.can_redo
      operations.value = []

      const urls = session.field_urls
      for (const fieldName of FIELD_NAMES) {
        if (!urls[fieldName]) {
          throw new Error(`Missing field URL: ${fieldName}`)
        }
      }

      const entries = await Promise.all(
        FIELD_NAMES.map(async (fieldName) => {
          const { buffer } = await fetchField(urls[fieldName])
          assertByteLength(fieldName, buffer)
          return [fieldName, buffer] as const
        }),
      )
      const buffers = Object.fromEntries(entries) as FieldBuffers

      qpfBefore.value = new Float32Array(buffers.qpf_before)
      ptypeBefore.value = new Uint8Array(buffers.ptype_before)
      qpfAfter.value = new Float32Array(buffers.qpf_after)
      ptypeAfter.value = new Uint8Array(buffers.ptype_after)
      touchedMask.value = new Uint8Array(buffers.touched_mask)
      changedMask.value = new Uint8Array(buffers.changed_mask)
      invalidMask.value = new Uint8Array(buffers.invalid_mask)
    } catch (error) {
      clearArrays()
      fieldLoadError.value = getErrorMessage(error)
    } finally {
      loadingFields.value = false
    }
  }

  function setActiveTool(tool: ToolType | null) {
    if (tool === null) {
      activeTool.value = null
      return
    }

    if (sessionId.value === null) {
      return
    }

    activeTool.value = tool
  }

  function setMaskGeometry(geom: MaskGeometry) {
    currentMaskGeometry.value = geom
  }

  function clearMask() {
    currentMaskGeometry.value = null
    activeTool.value = null
  }

  return {
    sessionId,
    windowId,
    baseVersionId,
    currentVersionId,
    qpfBefore,
    ptypeBefore,
    qpfAfter,
    ptypeAfter,
    touchedMask,
    changedMask,
    invalidMask,
    activeTool,
    currentMaskGeometry,
    selectedViewMode,
    previewId,
    previewStats,
    operations,
    canUndo,
    canRedo,
    dirty,
    loadingSession,
    loadingFields,
    previewLoading,
    applyLoading,
    saveLoading,
    fieldLoadError,
    previewError,
    startSession,
    loadSession,
    setActiveTool,
    setMaskGeometry,
    clearMask,
    reset,
  }
})
