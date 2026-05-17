import { defineStore } from 'pinia'
import { shallowRef, ref } from 'vue'
import {
  editApply,
  editPreview,
  editRedo,
  editUndo,
  getOperations,
} from '@/api/edit'
import type {
  EditOperation,
  EditPreviewResponse,
  EditTool,
  EditVariable,
  OperationItem,
} from '@/api/edit'
import { fetchField, loadSession as loadSessionApi, startSession as startSessionApi } from '@/api/sessions'
import type { MaskGeometry, ToolType, ViewMode } from '@/types/editor'
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
  const previewResult = ref<EditPreviewResponse | null>(null)
  const operations = ref<OperationItem[]>([])
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
    previewResult.value = null
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

  async function loadSessionField(fieldName: FieldName) {
    if (sessionId.value === null) {
      throw new Error('Missing active session')
    }

    const { buffer } = await fetchField(`/api/session/${sessionId.value}/field/${fieldName}`)
    assertByteLength(fieldName, buffer)

    if (fieldName === 'qpf_before') {
      qpfBefore.value = new Float32Array(buffer)
    } else if (fieldName === 'ptype_before') {
      ptypeBefore.value = new Uint8Array(buffer)
    } else if (fieldName === 'qpf_after') {
      qpfAfter.value = new Float32Array(buffer)
    } else if (fieldName === 'ptype_after') {
      ptypeAfter.value = new Uint8Array(buffer)
    } else if (fieldName === 'touched_mask') {
      touchedMask.value = new Uint8Array(buffer)
    } else if (fieldName === 'changed_mask') {
      changedMask.value = new Uint8Array(buffer)
    } else {
      invalidMask.value = new Uint8Array(buffer)
    }
  }

  async function refreshAfterEditFields() {
    loadingFields.value = true
    fieldLoadError.value = null

    try {
      await Promise.all([loadSessionField('qpf_after'), loadSessionField('ptype_after')])
    } catch (error) {
      fieldLoadError.value = getErrorMessage(error)
      throw error
    } finally {
      loadingFields.value = false
    }
  }

  function requireSessionId() {
    if (sessionId.value === null) {
      throw new Error('Missing active session')
    }
    return sessionId.value
  }

  async function fetchOperations() {
    const activeSessionId = requireSessionId()
    const response = await getOperations(activeSessionId)
    operations.value = response.operations
    canUndo.value = response.operations.some((operation) => operation.is_undone === 0)
    canRedo.value = response.operations.some((operation) => operation.is_undone === 1)
  }

  async function requestPreview(
    tool: EditTool,
    variable: EditVariable,
    operation: EditOperation,
    mask: Record<string, unknown>,
    parameters: Record<string, unknown>,
  ) {
    const activeSessionId = requireSessionId()
    previewLoading.value = true
    previewError.value = null

    try {
      const result = await editPreview({
        session_id: activeSessionId,
        tool,
        variable,
        operation,
        mask,
        parameters,
      })
      previewResult.value = result
      previewId.value = result.preview_id
      return result
    } catch (error) {
      previewError.value = getErrorMessage(error)
      throw error
    } finally {
      previewLoading.value = false
    }
  }

  function clearPreview() {
    previewResult.value = null
    previewId.value = null
  }

  async function applyEdit(targetPtype?: number) {
    const activeSessionId = requireSessionId()
    if (previewResult.value === null) {
      throw new Error('Missing preview result')
    }

    applyLoading.value = true
    previewError.value = null

    try {
      const response = await editApply({
        session_id: activeSessionId,
        preview_id: previewResult.value.preview_id,
        target_ptype: targetPtype,
      })
      canUndo.value = response.can_undo
      canRedo.value = response.can_redo
      dirty.value = true
      clearPreview()
      await refreshAfterEditFields()
      await fetchOperations()
      return response
    } catch (error) {
      previewError.value = getErrorMessage(error)
      throw error
    } finally {
      applyLoading.value = false
    }
  }

  async function undoEdit() {
    const activeSessionId = requireSessionId()
    applyLoading.value = true
    previewError.value = null

    try {
      const response = await editUndo({ session_id: activeSessionId })
      canUndo.value = response.can_undo
      canRedo.value = response.can_redo
      clearPreview()
      await refreshAfterEditFields()
      await fetchOperations()
      return response
    } catch (error) {
      previewError.value = getErrorMessage(error)
      throw error
    } finally {
      applyLoading.value = false
    }
  }

  async function redoEdit() {
    const activeSessionId = requireSessionId()
    applyLoading.value = true
    previewError.value = null

    try {
      const response = await editRedo({ session_id: activeSessionId })
      canUndo.value = response.can_undo
      canRedo.value = response.can_redo
      clearPreview()
      await refreshAfterEditFields()
      await fetchOperations()
      return response
    } catch (error) {
      previewError.value = getErrorMessage(error)
      throw error
    } finally {
      applyLoading.value = false
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
    previewResult,
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
    loadSessionField,
    requestPreview,
    applyEdit,
    undoEdit,
    redoEdit,
    fetchOperations,
    clearPreview,
    setActiveTool,
    setMaskGeometry,
    clearMask,
    reset,
  }
})
