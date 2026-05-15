import { defineRequest } from "../shared";
import type { Data } from "../types";

export default defineRequest((request) => {
  return {
    // 查询用户
    getUser(data: Data) {
      return request.post('/user/queryUsers', data)
    },
    // 查询日志
    getLog(data: Data) {
      return request.post('/log/queryLogRecords', data)
    },
  };
});