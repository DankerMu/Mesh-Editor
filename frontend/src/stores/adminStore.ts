import { defineStore } from 'pinia'
import {
  createUser as createUserApi,
  getUsers,
  updateUser as updateUserApi,
} from '@/api/admin'
import type {
  UserCreatePayload,
  UserItem,
  UserListParams,
  UserUpdatePayload,
} from '@/api/admin'

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : '用户管理操作失败'
}

export const useAdminStore = defineStore('admin', {
  state: () => ({
    users: [] as UserItem[],
    total: 0,
    page: 1,
    pageSize: 20,
    loading: false,
    error: null as string | null,
  }),
  actions: {
    async fetchUsers(params: UserListParams = {}) {
      this.loading = true
      this.error = null

      try {
        const response = await getUsers({
          page: this.page,
          page_size: this.pageSize,
          ...params,
        })
        this.users = response.data.items
        this.total = response.data.total
        this.page = response.data.page
        this.pageSize = response.data.page_size
        return response.data
      } catch (error) {
        this.error = getErrorMessage(error)
        throw error
      } finally {
        this.loading = false
      }
    },
    async createUser(payload: UserCreatePayload) {
      this.loading = true
      this.error = null

      try {
        const response = await createUserApi(payload)
        await this.fetchUsers()
        return response.data
      } catch (error) {
        this.error = getErrorMessage(error)
        throw error
      } finally {
        this.loading = false
      }
    },
    async updateUser(id: number, payload: UserUpdatePayload) {
      this.loading = true
      this.error = null

      try {
        const response = await updateUserApi(id, payload)
        this.users = this.users.map((user) => (user.id === id ? response.data : user))
        return response.data
      } catch (error) {
        this.error = getErrorMessage(error)
        throw error
      } finally {
        this.loading = false
      }
    },
  },
})
