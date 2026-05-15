import { defineRequest } from "../shared";
import type { Data } from "../types";

export default defineRequest((axios) => {
  return {
    // 获取站点评估数据
    getStationEstimate(data: Data) {
      return axios.post('/check/checkStationData', data)
    },

    // 获取站点逐时评估数据
    getStationEstimateByHour(data: Data) {
      return axios.post('/check/checkStationDataHour', data)
    },

    // 获取站点逐日评估数据
    getStationEstimateByDay(data: Data) {
      return axios.post('/check/checkStationDataDay', data)
    },

    // 气象要素
    // getWeatherFeature() {
    //   return axios.get('/config/getStationForecastCheckElements')
    // },

    getWeatherFeature1(data: Data) {
      return axios.post('/config/getElementsByDataType', data)
    },

    // 根据要素获取检验方法
    getCheckMethodByFeature(data: Data) {
      return axios.post('/config/getMethodByElement', data)
    },

    // 获取检验方法
    getCheckMethod() {
      return axios.get('/config/getStationForecastCheckMethods')
    },

    // 根据数据类型获取数据源名称
    // org: 原始产品, prc：订正产品
    getModeName(data: Data) {
      return axios.post('/config/getDataSourceByDataType', data)
    },

    getZone() {
      return axios.get('/config/getDataZone')
    },

    // 获取省列表
    getProvinces() {
      return axios.get('/station/queryProvince ')
    },

    // 获取市列表
    getCities(data: Data) {
      return axios.post('/station/queryCity', data)
    },

    // 获取县列表
    getDistrict(data: Data) {
      return axios.post('/station/queryCnty', data)
    },

    // 获取站点列表
    getStations(data: Data) {
      return axios.post('/station/queryStations', data)
    },

    // 获取数据源
    getSourceList() {
      return axios.post('/config/getDataSources')
    },

    // 获取格点评估数据
    getGridEstimate(data: Data) {
      return axios.post('/check/checkGribData', data)
    },

    // 获取格点逐时评估数据
    getGridEstimateByHour(data: Data) {
      return axios.post('/check/checkGribDataHour', data)
    },

    // 获取格点逐日评估数据
    getGridEstimateByDay(data: Data) {
      return axios.post('/check/checkGribDataDay', data)
    },

    // 站点下载
    downloadStationCheckData(data: Data) {
      return axios.post('/downloadStationCheckData', data)
    },

    // 格点下载
    downloadGridCheckData(data: Data) {
      return axios.post('/downloadGribCheckData', data)
    },
    // 下载csv文件
    downloadCSV(data: Data) {
      return axios.post('/downloadExcel', data, {responseType: 'blob'})
    },
    // 获取正在使用的订正模型
    queryGribUsedModel() {
      return axios.post('/model/queryGribUsedModel')
    }
  };
});