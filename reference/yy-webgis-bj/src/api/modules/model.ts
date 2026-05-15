import { defineRequest } from "../shared";
import type { Data } from "../types";

export default defineRequest((request) => {
  return {
    // 查询模型列表
    getModelList() {
      // return request.post('/model/queryModelInfo', data)
      return request.post('/model/queryModelManagerList')
    },
    // 查询模型
    searchModel(data: Data) {
      return request.post('/model/queryModelInfoLike', data)
    },
    // 删除模型
    deleteModel(data: Data) {
      return request.post('/model/deleteModelById', data)
    },
    // 获取模型选项
    getModelOption() {
      return request.get('/config/queryModelType')
    },
    // 创建模型
    createNewModel(data: Data) {
      return request.post('/model/addModelInfo', data)
    },
    // 升级模型
    updateModel(data: Data) {
      // return request.post('/model/upgradeModel', data)
      return request.post('/model/upgradeModel', data)
    },
    // 替换模型
    replaceModel(data: Data) {
      return request.post('/model/replaceModel', data)
    },
    // 替换模型的列表
    updateModelList(data: Data) {
      return request.post('/model/queryUseableModelList', data)
    },
    // 新建无站点
    addNoneStation(data: Data) {
      return request.post('/station/addZjStation', data)
    },
    // 对无建模的站点建模
    addNoModelStation(data: Data) {
      return request.post('/station/addStations', data)
    },
    // 查询无建模的站点
    getNoModelStation() {
      return request.get('/station/queryAllStationsNotModel')
    },
    // 模糊查询无建模的站点
    searchNoModelStation(data: Data) {
      return request.post('/station/queryStationInfoNotModel', data)
    },
    // 查询模型详情列表
    searchDetailStaion(data: Data) {
      return request.post('/model/queryModelManagerListDetail', data)
    },
    // 获取替换模型列表
    getReplaceModelList(data: Data) {
      return request.post('/model/queryModelReplaceList', data)
    },
    // 站点模糊查询
    searchStation(data: Data) {
      return request.post('/model/queryModelDetailListLike', data)
    },
    // 站点删除
    deleteStation(data: Data) {
      return request.post('/model/deleteModelByStationNum', data)
    },
    // 站点模型替换
    stationModelReplace(data: Data) {
      return request.post('/model/replaceModel', data)
    },
    testupdate(data: Data) {
      return request.post('/upgradeModel', data, { baseURL: '/testapi' })
    },
    // 检测无站点站名是否重复
    isDuplicatedStaionName(data: Data) {
      return request.post('/station/queryZjStationByStationName', data)
    },
    // 地图自建站点模糊查询
    queryCustomStation(data: Data) {
      return request.post('/station/queryStationInfoZd', data)
    },
    // 根据模型ID查询模型详情
    queryModelManagerDetailById(data: Data) {
      return request.post('/model/queryModelManagerDetailById', data)
    },
    // 根据模型ID查询模型详情
    queryGribModelAll() {
      return request.post('/model/queryGribModelAll')
    },

    // 站点分组
    queryTaskList(data) {
      return request.post('/station/task/queryTaskList', data)
    },
    queryTaskListAndStations(data) {
      return request.post('/station/task/queryTaskListAndStations', data)
    },
    queryTaskStations(data) {
      return request.post('/station/task/queryTaskStations', data)
    },
    addTaskList(data) {
      return request.post('/station/task/addTaskList', data)
    },
    addTaskStation(data) {
      return request.post('/station/task/addTaskStation', data)
    },
    deleteTaskStation(data) {
      return request.post('/station/task/deleteTaskStation', data)
    },
    deleteTask(data) {
      return request.post('/station/task/deleteTask', data)
    },
    modifyTaskList(data) {
      return request.post('/station/task/modifyTaskList', data)
    },
    modifyTaskName(data) {
      return request.post('/station/task/modifyTaskName', data)
    },
  };
});