import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import OperationHistory from '@/components/editor/OperationHistory.vue'
import { useEditorStore } from '@/stores/editorStore'
import type { OperationItem } from '@/api/edit'

function makeOperation(overrides: Partial<OperationItem> = {}): OperationItem {
  return {
    sequence_no: 1,
    tool_name: 'polygon',
    operation_type: 'increase',
    variable_name: 'qpf',
    affected_grid_count: 12,
    is_undone: 0,
    created_at: '2026-05-17T00:00:00Z',
    ...overrides,
  }
}

function mountHistory(operations: OperationItem[] = [makeOperation()]) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const editorStore = useEditorStore()
  editorStore.sessionId = 'session-1'
  editorStore.operations = operations
  editorStore.canUndo = operations.some((operation) => operation.is_undone === 0)
  editorStore.canRedo = operations.some((operation) => operation.is_undone === 1)

  const wrapper = mount(OperationHistory, {
    global: {
      plugins: [pinia],
      stubs: {
        MapEditIcon: { template: '<span data-test="map-edit-icon"></span>' },
        SubwayLineIcon: { template: '<span data-test="line-icon"></span>' },
        PenBrushIcon: { template: '<span data-test="brush-icon"></span>' },
        BrushIcon: { template: '<span data-test="brush-icon"></span>' },
        EditIcon: { template: '<span data-test="edit-icon"></span>' },
        RollbackIcon: { template: '<span data-test="rollback-icon"></span>' },
        RefreshIcon: { template: '<span data-test="refresh-icon"></span>' },
        HistoryIcon: { template: '<span data-test="history-icon"></span>' },
      },
    },
  })

  return { wrapper, editorStore }
}

describe('OperationHistory', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('渲染操作列表和中文标签', () => {
    const { wrapper } = mountHistory([
      makeOperation({ sequence_no: 1, tool_name: 'polygon', operation_type: 'increase' }),
      makeOperation({
        sequence_no: 2,
        tool_name: 'brush_path',
        operation_type: 'ptype_set',
        variable_name: 'ptype',
        affected_grid_count: 7,
      }),
    ])

    const items = wrapper.findAll('[data-test="operation-history-item"]')
    expect(items).toHaveLength(2)
    expect(wrapper.text()).toContain('#1')
    expect(wrapper.text()).toContain('降水 增加')
    expect(wrapper.text()).toContain('多边形')
    expect(wrapper.text()).toContain('12 格点')
    expect(wrapper.text()).toContain('#2')
    expect(wrapper.text()).toContain('相态 设置相态')
    expect(wrapper.text()).toContain('笔刷')
    expect(wrapper.text()).toContain('7 格点')
  })

  it('已撤销操作显示灰显 class 和标签', () => {
    const { wrapper } = mountHistory([makeOperation({ is_undone: 1 })])
    const item = wrapper.find('[data-test="operation-history-item"]')

    expect(item.classes()).toContain('operation-history__item--undone')
    expect(item.text()).toContain('已撤销')
  })

  it('undo/redo 按钮 disabled 状态绑定 canUndo/canRedo', async () => {
    const { wrapper, editorStore } = mountHistory([makeOperation({ is_undone: 0 })])

    expect(wrapper.find('[data-test="history-undo-button"]').attributes('disabled')).toBeUndefined()
    expect(wrapper.find('[data-test="history-redo-button"]').attributes('disabled')).toBeDefined()

    editorStore.canUndo = false
    editorStore.canRedo = true
    await flushPromises()

    expect(wrapper.find('[data-test="history-undo-button"]').attributes('disabled')).toBeDefined()
    expect(wrapper.find('[data-test="history-redo-button"]').attributes('disabled')).toBeUndefined()
  })

  it('undo/redo 点击调用 store 方法', async () => {
    const { wrapper, editorStore } = mountHistory([
      makeOperation({ sequence_no: 1, is_undone: 0 }),
      makeOperation({ sequence_no: 2, is_undone: 1 }),
    ])
    const undoSpy = vi.spyOn(editorStore, 'undoEdit').mockResolvedValue({
      can_undo: false,
      can_redo: true,
      operation_count: 2,
    })
    const redoSpy = vi.spyOn(editorStore, 'redoEdit').mockResolvedValue({
      can_undo: true,
      can_redo: false,
      operation_count: 2,
    })

    await wrapper.find('[data-test="history-undo-button"]').trigger('click')
    await wrapper.find('[data-test="history-redo-button"]').trigger('click')

    expect(undoSpy).toHaveBeenCalled()
    expect(redoSpy).toHaveBeenCalled()
  })
})
