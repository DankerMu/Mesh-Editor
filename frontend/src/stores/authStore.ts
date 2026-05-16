import { defineStore } from 'pinia'
import http from '@/api/http'

export interface User {
  user_id: string
  username: string
  display_name: string
  role: string
}

interface LoginResponse {
  user_id: string
  username: string
  display_name: string
  role: string
  token: string
  expires_at: string
}

interface ApiResponse<T> {
  code: string
  message: string
  data: T
  trace_id: string
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null as User | null,
    token: null as string | null,
  }),
  getters: {
    isAuthenticated: (state) => !!state.token,
    role: (state) => state.user?.role ?? null,
  },
  actions: {
    async login(username: string, password: string) {
      const { data } = await http.post<ApiResponse<LoginResponse>>('/auth/login', {
        username,
        password,
      })
      const loginData = data.data

      this.token = loginData.token
      this.user = {
        user_id: loginData.user_id,
        username: loginData.username,
        display_name: loginData.display_name,
        role: loginData.role,
      }
      localStorage.setItem('token', loginData.token)
      localStorage.setItem('user', JSON.stringify(this.user))
    },
    logout() {
      this.user = null
      this.token = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    },
    restoreSession() {
      const token = localStorage.getItem('token')
      const userStr = localStorage.getItem('user')

      if (token) {
        this.token = token
      }

      if (userStr) {
        try {
          this.user = JSON.parse(userStr)
        } catch {
          this.logout()
        }
      }
    },
  },
})
