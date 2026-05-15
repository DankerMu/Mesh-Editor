import { defineRequest } from "../shared";

import type { Data } from "../types";

export interface User {
  id: number;
  name: string;
  phone: string;
  remark?: string;
  status: string;
  onlineStatus?: number;
  account: string;
  createId: number;
  createTime: string;
  createName: string;
  roleNames?: string;
  statusName: string;
}


export default defineRequest((request) => {
  return {
    // 登录
    userLogin(data: Data) {
      const { u, p, verifyCode } = data
      return request.post(`/login?u=${u}&p=${p}&verifyCode=${verifyCode}`)
    },
    // 获取验证码
    validateCode() {
      return request.get('/verifyCode', { responseType: 'arraybuffer' })
    },
    // 获取用户信息
    getUserInfo(data: Data) {
      return request.post('/user/findByUserName', data)
    },
    // 退出登录
    userLogout() {
      return request.post('/exit ')
    },
    // 修改密码
    editPassword(data: Data) {
      return request.post('/user/modifyPassword', data)
    },
    // 获取用户列表
    getUserList(data: Data) {
      return request.post('/user/queryUserByPage', data)
    },
    // 获取角色列表
    getRoleList() {
      return request.get('/user/queryAllRoles')
    },
    // 查询用户
    searchUser(data: Data) {
      return request.post('/user/queryUserLike', data)
    },
    // 新增用户
    addNewUser(data: Data) {
      return request.post('/user/addUser', data)
    },
    // 删除用户
    deleteUser(id: number | string) {
      return request.post('/user/deleteUser', id)
    },
    // 修改用户信息
    updateUserInfo(data: Data) {
      return request.post('/user/updateUser', data)
    },
    // 获取角色权限列表
    getRolePermissionList(data: Data) {
      return request.post('/user/queryRoleByPage', data)
    },
    // 新增角色
    addNewRole(data: Data) {
      return request.post('/user/addRole', data)
    },
    // 删除角色
    deleteRole(data: Data) {
      return request.post('/user/deleteRole', data)
    },
    // 查询角色下的用户个数
    queryUserwithRole(data: Data) {
      return request.post('/user/queryRoleUser', data)
    },
    // 修改角色
    updateRole(data: Data) {
      return request.post('/user/updateRole', data)
    },
    // 查询角色
    searchRole(data: Data) {
      return request.post('/user/queryRoles', data)
    },
    // 获取权限列表
    getPermissionsList() {
      return request.get('/user/queryAllPermisss')
    }
  };
});
