import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { createUser, getUsers, updateUser } from '@/api/admin'
import { useAdminStore } from '@/stores/adminStore'
import type { ApiResponse, UserItem, UserListResponse } from '@/api/admin'

vi.mock('@/api/admin', () => ({
  getUsers: vi.fn(),
  createUser: vi.fn(),
  updateUser: vi.fn(),
}))

const USER: UserItem = {
  id: 1,
  username: 'forecaster',
  display_name: '预报员',
  role: 'forecaster',
  is_active: true,
  created_at: '2026-05-18T00:00:00Z',
  updated_at: '2026-05-18T00:00:00Z',
  last_login_at: null,
}

function ok<T>(data: T): ApiResponse<T> {
  return { code: 'OK', message: '成功', data, trace_id: 'trace-1' }
}

describe('adminStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(getUsers).mockReset()
    vi.mocked(createUser).mockReset()
    vi.mocked(updateUser).mockReset()
  })

  it('fetchUsers populates users and pagination', async () => {
    const payload: UserListResponse = { items: [USER], total: 1, page: 1, page_size: 20 }
    vi.mocked(getUsers).mockResolvedValue(ok(payload))

    const store = useAdminStore()
    await store.fetchUsers({ role: 'forecaster' })

    expect(getUsers).toHaveBeenCalledWith({ page: 1, page_size: 20, role: 'forecaster' })
    expect(store.users).toEqual([USER])
    expect(store.total).toBe(1)
  })

  it('createUser calls API and refreshes list', async () => {
    vi.mocked(createUser).mockResolvedValue(ok(USER))
    vi.mocked(getUsers).mockResolvedValue(ok({ items: [USER], total: 1, page: 1, page_size: 20 }))

    const store = useAdminStore()
    await store.createUser({
      username: 'forecaster',
      display_name: '预报员',
      password: 'secret1',
      role: 'forecaster',
    })

    expect(createUser).toHaveBeenCalledWith({
      username: 'forecaster',
      display_name: '预报员',
      password: 'secret1',
      role: 'forecaster',
    })
    expect(store.users).toEqual([USER])
  })

  it('updateUser replaces user in local list', async () => {
    const updated = { ...USER, display_name: '高级预报员', is_active: false }
    vi.mocked(updateUser).mockResolvedValue(ok(updated))

    const store = useAdminStore()
    store.users = [USER]
    await store.updateUser(1, { display_name: '高级预报员', is_active: false })

    expect(updateUser).toHaveBeenCalledWith(1, {
      display_name: '高级预报员',
      is_active: false,
    })
    expect(store.users[0]).toEqual(updated)
  })
})
