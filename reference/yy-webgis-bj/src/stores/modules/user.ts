import type { UserInfo } from "@/types/interface"
import router from "@/router"
import { UserService } from '@/api'
import pinia from "../index";

import { defineStore } from "pinia"

interface UserState {
  token: string;
  userInfo: Record<string, any> | null; // 或者更具体的类型
  userName_: string;
}

interface LoginParams {
  u: string;
  p: string;
  // verifyCode: string;
}

interface ModifiedPwd {
  userName: string;
  oldPwd: string;
  newPwd: string;
}

export const useUserStore = defineStore("user", {
  state: (): UserState => ({
    token: "",
    userInfo: null,
    userName_: '',
  }),
  getters: {
    roles: state => state.userInfo?.roles,
    userName: state => state.userInfo?.username || '',
    isLogin: state => !!state.userInfo
  },
  actions: {
    async login(data: LoginParams) {
      const res = await UserService.userLogin(data)
      if (res && !res.token) {
        throw new Error(res.message); // 主动抛出异常
      }
      if (res) {
        this.token = res.token
        this.userName_ = data.u
        await this.getUserInfo();
      }
    },
    async getUserInfo() {
      const res = await UserService.getUserInfo({ username: this.userName_ })
      if (res) {
        this.userInfo = res
        sessionStorage.setItem("userInfo", JSON.stringify(res ?? ""));
      }
    },
    async checkLogin() {
      return !!sessionStorage.getItem("userInfo");
    },
    async logout() {
      await UserService.userLogout()
      this.token = ""
      this.userInfo = null
      sessionStorage.removeItem("userInfo");

      router.push("/login")
    },
    async modifyPassword(data: ModifiedPwd) {
      const res = await UserService.editPassword(data)
      if (res === 1) {
        await this.logout()
      } else {
        throw new Error('原密码输入错误')
      }
    }
  },
  persist: {
    afterHydrate: () => {
      console.log("afterHydrate")
    },
    key: "token",
    pick: ["token"],
  },
})

export const useUserStoreWithOut = () => useUserStore(pinia);