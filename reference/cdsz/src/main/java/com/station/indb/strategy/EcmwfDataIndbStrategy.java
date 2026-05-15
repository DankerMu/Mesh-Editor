package com.station.indb.strategy;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.hutool.core.io.FileUtil;

import com.alibaba.fastjson.JSONObject;
import com.constants.DataTypeEnum;
import com.station.indb.inf.DataIndbStrategy;
import com.station.indb.util.DealDatasUtil;
import com.station.indb.util.ReadTableConfigMapUtils;
import com.util.GribFileReaderUtil;
import com.util.GribUtil;
import com.util.NcReader;
import com.util.ReadLonLatUtil;
import com.util.ReadPropertiesUtil;
import com.util.TimeUtil;

/**
 * @category
 * @date 2025/3/12 9:29
 * @description TODO
 */
public class EcmwfDataIndbStrategy implements DataIndbStrategy {
    Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config_indb.properties");
    Map<String, double[]> stationLonlats;// = QueryStationsInfoFromDBUtil.getInstance().getStationsInfo();
    Map<String, String> dataTableMap = ReadPropertiesUtil.getUserConfigMap("data_table.properties");
    Map<String, Set<String>> configElementMap = ReadPropertiesUtil.getConfigMap("elements.properties");
    Map<String, String> elementMap = ReadPropertiesUtil.getUserConfigMap("ecmf_elements_map.properties");

    
    public void setStationLonlats(Map<String, double[]> stationLonlats)
    {
    	this.stationLonlats = stationLonlats;
    }
    
    @Override
    public Map<String, List<JSONObject>> getMapDataIndbList(String filePath, String tableName) {
        Map<String, List<JSONObject>> result = new HashMap<>();
//        W_NAFP_C_ECMF_20240111054846_P_C1D01110000011106001.grib
//        String name = FileUtil.getName(filePath);
//        String prefix = FileUtil.getPrefix(filePath);
//        String suffix = FileUtil.getSuffix(filePath);
        System.out.println("ecmf111: " + filePath);
        long time = System.currentTimeMillis();
        long time1 = System.currentTimeMillis();
        String[] vtiDataTime = GribUtil.getVtiDataTime(filePath, DataTypeEnum.ECMF.getDataType());
        String vti = vtiDataTime[0];
        String dataTime = vtiDataTime[1];
        Date date = TimeUtil.dateTimeStr2date(dataTime, TimeUtil.DATE_FMT_YMDH, TimeUtil.DATE_FMT_YMDH);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR_OF_DAY, 8);
        dataTime = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
//        long fileSize = FileUtil.size(new File(filePath));
//        String inserttime = TimeUtil.date2String(new Date(), TimeUtil.DEFAULT_DATETIME_FORMAT);
        Map<String, String> ecmfMap = ReadTableConfigMapUtils.ecmfMap;
        calendar.add(Calendar.HOUR_OF_DAY, Integer.parseInt(vti));
        String validDate = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT);
//        JSONObject json = new JSONObject();
//        json.put(ecmfMap.get("filename"), name);
//        json.put(ecmfMap.get("dataTime"), dataTime);
//        json.put(ecmfMap.get("validdate"), validDate);
//        json.put(ecmfMap.get("hour"), hour);
//        json.put(ecmfMap.get("vti"), Integer.parseInt(vti));
//        json.put(ecmfMap.get("datatype"), DataTypeEnum.ECMF.getDataType());
//        json.put(ecmfMap.get("filepath"), filePath);
//        json.put(ecmfMap.get("inserttime"), inserttime);
//        json.put(ecmfMap.get("filesize"), fileSize);
//        json.put(ecmfMap.get("filetype"), suffix);
//        List<JSONObject> list = new ArrayList<>();
//        list.add(json);
//        result.put(tableName, list);
//        time = System.currentTimeMillis();
        Map<String, Object> datasMap = GribFileReaderUtil.getDatasMap(filePath);
//        System.out.println(filePath + "数据读取耗时:" + (System.currentTimeMillis() - time));
        if(datasMap.size() == 0)
        {
            return result;
        }
        time = System.currentTimeMillis();
        Set<String> elements = configElementMap.get(DataTypeEnum.ECMF.getDataType());
        Set<String> uvSet = configElementMap.get(DataTypeEnum.ECMF.getDataType() + "@uv");
        Map<String, JSONObject> jsonMap = new HashMap<>();
        for(String station : stationLonlats.keySet())
        {
            jsonMap.put(station, new JSONObject());
            jsonMap.get(station).put(ecmfMap.get("station"), station);
            jsonMap.get(station).put(ecmfMap.get("dataTime"), dataTime);
            jsonMap.get(station).put(ecmfMap.get("validdate"), validDate);
            jsonMap.get(station).put(ecmfMap.get("hour"), hour);
            jsonMap.get(station).put(ecmfMap.get("vti"), Integer.parseInt(vti));
            jsonMap.get(station).put(ecmfMap.get("filepath"), filePath);
            jsonMap.get(station).put(ecmfMap.get("inserttime"), TimeUtil.date2String(new Date(), TimeUtil.DEFAULT_DATETIME_FORMAT));
        }
        String prefix = "latlon_1441x2880-0p06s-180p00e:";
        if(filePath.contains("_cut"))
        {
        	prefix = "";
        }
        double[] lonLats = null;
        String[] lonLatName = NcReader.getLonLatName(datasMap);
        if(lonLatName == null)
        {
            return result;
        }
//        time = System.currentTimeMillis();
        double[] lons = ReadLonLatUtil.readLonLat(datasMap, prefix + lonLatName[0]);
        double[] lats = ReadLonLatUtil.readLonLat(datasMap, prefix + lonLatName[1]);
//        System.out.println(filePath + "数据读取经纬度耗时:" + (System.currentTimeMillis() - time));
        if(lons == null || lats == null)
        {
            return result;
        }
        String lonlat = "0, 90, 360, -90";
        for(String element : elements)
        {
//        	time = System.currentTimeMillis();
            double[][][] values = NcReader.readByElemNameLayerSlice(datasMap, prefix + element, null);
//            System.out.println(filePath + "数据读取三维数组耗时:" + (System.currentTimeMillis() - time));
            if(values == null)
            {
                return result;
            }
            if(element.startsWith("total_precipitation_surface"))
            {
                element = element + "_rain";
            }
            for(String station : stationLonlats.keySet())
            {
                lonLats = stationLonlats.get(station);
//                time = System.currentTimeMillis();
                double[][] fstValue = DealDatasUtil.getFstValue(lons, lats, lonLats[0], lonLats[1], values[0], lonlat);
//                System.out.println(filePath + "数据插值耗时:" + (System.currentTimeMillis() - time));
                double[] pointValues = fstValue[0];
                double[] pointLonLats = fstValue[1];
                jsonMap.get(station).put(elementMap.get(element), pointValues[4]);
                for(int i = 0; i < 4; i++)
                {
                    jsonMap.get(station).put(elementMap.get(element) + (i + 1), pointValues[i]);
                    jsonMap.get(station).put("lon" + (i + 1), pointLonLats[i * 2]);
                    jsonMap.get(station).put("lat" + (i + 1), pointLonLats[i * 2 + 1]);
                }
            }
        }
        if(uvSet != null)
        {
            for(String element : uvSet)
            {
                String elementV = element.replace(configMap.get(DataTypeEnum.ECMF.getDataType() + "_u"), configMap.get(DataTypeEnum.ECMF.getDataType() + "_v"));
                double[][][] datasU = NcReader.readByElemNameLayerSlice(datasMap, prefix + element, null);
                double[][][] datasV = NcReader.readByElemNameLayerSlice(datasMap, prefix + elementV, null);
                for(String station : stationLonlats.keySet())
                {
                    lonLats = stationLonlats.get(station);
                    if(datasU == null || datasV == null || lonLats == null)
                    {
                        continue;
                    }
                    double[][] fstValueWsWd = DealDatasUtil.getFstValueWsWd(lons, lats, lonLats[0], lonLats[1], datasU[0], datasV[0], lonlat);
                    double[] pointValuesWs = fstValueWsWd[0];
                    double[] pointValuesWd = fstValueWsWd[1];
                    double[] pointLonLats = fstValueWsWd[2];
                    jsonMap.get(station).put("ws10", pointValuesWs[4]);
                    jsonMap.get(station).put("wd10", pointValuesWd[4]);
                    for(int i = 0; i < 4; i++)
                    {
                        jsonMap.get(station).put("ws10" + (i + 1), pointValuesWs[i]);
                        jsonMap.get(station).put("wd10" + (i + 1), pointValuesWd[i]);
                        jsonMap.get(station).put("lon" + (i + 1), pointLonLats[i * 2]);
                        jsonMap.get(station).put("lat" + (i + 1), pointLonLats[i * 2 + 1]);
                    }
                }
            }
        }
        System.out.println(filePath + "数据处理耗时:" + (System.currentTimeMillis() - time));
        List<JSONObject> orgList = new ArrayList<>();
        for(String key : jsonMap.keySet())
        {
            orgList.add(jsonMap.get(key));
        }
        result.put(dataTableMap.get(DataTypeEnum.ECMF.getDataType() + "_value"), orgList);
        
        
        System.out.println(filePath + "数据解析耗时:" + (System.currentTimeMillis() - time1));

        return result;
    }

}
