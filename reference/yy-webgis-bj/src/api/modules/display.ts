import { defineRequest } from "../shared";
import type { Data } from "../types";


export default defineRequest((axios) => {
  return {
    // 获取气象预报数据
    getStationPridctionData(data: Data) {
      return axios.post("/forecast/queryGribForecastDataMap", data)
    },

    // 获取气象要素
    getWeatherFeatures(data: Data) {
      // return axios.post('/config/getStationForecastHeader')
      return axios.post('/config/getStationForecastHeader', data)
    },

    // 站点搜索
    SearchStation(data: Data) {
      return axios.post('/station/queryStationInfo', data)
    },
    // 预报展示模糊查询
    MoreSearchStation(data: Data) {
      return axios.post('/station/queryStationInfoShow', data)
    },

    getDifferentStations() {
      return axios.get('/station/queryAllStationsFlag')
    },

    // 获取所有站点
    getStations() {
      return axios.get('/station/queryAllStations')
    },

    // 获取无站点
    getCustomStation() {
      return axios.get('/station/queryAllStationsZj')
    },

    // 获取格点数据
    getGridData(data: Data) {
      return axios.post('/forecast/queryGribForecast', data)
    },

    // 获取告警信息数量
    getWarningCount(data: Data) {
      return axios.post('/warn/queryWarnCount', data)
    },

    // 获取告警信息
    getWarningInfo() {
      return axios.post('/warn/queryWarnInfo')
    },

    // 获取服务器时间
    getServerTime() {
      return axios.post('/warn/queryServerDateTime')
    },

    // 下载图片
    downloadPng(data: Data) {
      return axios.post('/downloadImage', data, { responseType: 'blob' })
    },
    // 下载文档
    downloadWord(data: Data) {
      return axios.post('/downloadWord', data, { responseType: 'blob' })
    }
  };
});
