import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { RouterView } from 'vue-router'
import { createAppRouter } from '@/router'
import { useAuthStore } from '@/stores/authStore'
import { useEditorStore } from '@/stores/editorStore'
import { useWindowStore } from '@/stores/windowStore'
import type { WindowItem } from '@/api/data'

vi.mock('@/components/map/BaseMap.vue', () => ({
  default: {
    emits: ['map-ready', 'grid-hover'],
    template: '<div data-test="base-map"></div>',
  },
}))

function makeWindow(overrides: Partial<WindowItem> = {}): WindowItem {
  return {
    window_id: '2026010108_ACC24_024_048',
    accum_hours: 24,
    start_lead: 24,
    end_lead: 48,
    status: 'available',
    qc_status: 'pass',
    negative_count: 0,
    negative_min_value: null,
    negative_abs_max: null,
    missing_count: 0,
    ptype_missing_leads: [],
    qpf_before_path: 'qpf.npz',
    ptype_before_path: 'ptype.npz',
    data_ready_at: '2026-01-01T08:01:00Z',
    updated_at: '2026-01-01T08:01:00Z',
    ...overrides,
  }
}

async function mountWithRoute(path: string, role = 'forecaster') {
  const pinia = createPinia()
  setActivePinia(pinia)
  const authStore = useAuthStore()
  authStore.token = 'jwt-token'
  authStore.user = {
    user_id: 'u1',
    username: 'user',
    display_name: '用户',
    role,
  }
  const editorStore = useEditorStore()
  const startSessionSpy = vi.spyOn(editorStore, 'startSession').mockResolvedValue(undefined)
  const windowStore = useWindowStore()
  windowStore.windows = [makeWindow()]

  const router = createAppRouter()
  await router.push(path)
  await router.isReady()

  const wrapper = mount(
    {
      components: { RouterView },
      template: '<RouterView />',
    },
    {
      global: {
        plugins: [pinia, router],
      },
    },
  )
  await flushPromises()

  return { wrapper, router, startSessionSpy, windowStore }
}

describe('editor router integration', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('/editor 显示 WindowSelector', async () => {
    const { wrapper, startSessionSpy } = await mountWithRoute('/editor')

    expect(wrapper.text()).toContain('窗口')
    expect(startSessionSpy).not.toHaveBeenCalled()
  })

  it('/editor/:windowId 触发 startSession', async () => {
    const { startSessionSpy } = await mountWithRoute('/editor/2026010108_ACC24_024_048')

    expect(startSessionSpy).toHaveBeenCalledWith('2026010108_ACC24_024_048')
  })

  it('选择窗口后跳转到 /editor/:windowId', async () => {
    const { wrapper, router } = await mountWithRoute('/editor')

    await wrapper.find('button.window-selector__item').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/editor/2026010108_ACC24_024_048')
  })

  it('viewer 访问 /editor 被重定向到 /forbidden', async () => {
    const { router } = await mountWithRoute('/editor', 'viewer')

    expect(router.currentRoute.value.path).toBe('/forbidden')
  })
})
