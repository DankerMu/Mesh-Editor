import { defineRequest } from "../shared";
import type { Data } from "../types";

export default defineRequest((axios) => {
  return {
    // 获取站点对比数据
    getStationComparisonData(data: Data) {
      return axios.post("/compare/compareStationData", data)
    },

    // 获取气象要素
    getWeatherFeature(data: Data) {
      return axios.post('/config/getStationForecastCmpHeader', data)
    },

    // 获取格点对比数据
    getGridComparisonData(data: Data) {
      return axios.post("/compare/compareGribData", data)
    },

    // 下载站点数据
    downloadStationData(data: Data) {
      return axios.post('/downloadStationCompareData', data)
    },

    // 下载格点数据
    downloadGridData(data: Data) {
      return axios.post('/downloadGribCompareData', data)
    },

    // 获取数据源 cmpstation: 对比站点模式, cmpgrib: 对比格点模式, prc: prc
    getDataSourceByDataType(data: Data) {
      return axios.post('/config/getDataSourceByDataType', data)
    }
  };
});