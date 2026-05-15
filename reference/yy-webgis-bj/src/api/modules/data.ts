import { defineRequest } from "../shared";
import type { Data } from "../types";

export default defineRequest((request) => {
  return {
    // 查询数据列表
    getDataList(data: Data) {
      return request.post('/original/queryOriginalNafpDataCount', data)
    },
    // 查询数据类型
    getDataType(data: Data) {
      // return request.get('/config/queryDataQcDataType')
      return request.post('/config/getDataSourceByDataType', data)
    },
    // 上传文件
    uploadFiles(data: Data) {
      return request.post('/upload/uploadFile', data)
    },
    // 上传进度
    uploadStatus() {
      return request.get('/upload/uploadStatus')
    },
    // 删除文件
    deleteFiles(data: Data) {
      return request.post('/original/updateNafpDataByDateTimeDataSource', data)
    },
    // 导出数据
    exportData(data: Data) {
      return request.post('/downloadDataManagerData', data, { responseType: 'blob' })
    },
    // 模板下载
    downloadTemplate() {
      return request.post('/downloadModelFile', {}, { responseType: 'blob' })
    },
  };
});