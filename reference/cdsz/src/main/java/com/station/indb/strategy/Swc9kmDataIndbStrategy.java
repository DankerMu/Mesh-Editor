package com.station.indb.strategy;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alibaba.fastjson.JSONObject;
import com.constants.DataTypeEnum;
import com.station.indb.inf.DataIndbStrategy;
import com.station.indb.util.DealDatasUtil;
import com.station.indb.util.ReadTableConfigMapUtils;
import com.util.ArrayReverse;
import com.util.GribFileReaderUtil;
import com.util.GribUtil;
import com.util.NcReader;
import com.util.ReadLonLatUtil;
//import com.grib.NcReader;
//import com.indb.inf.DataIndbStrategy;
//import com.indb.util.QueryStationsInfoFromDBUtil;
//import com.indb.util.ReadTableConfigMapUtils;
import com.util.ReadPropertiesUtil;
import com.util.TimeUtil;

/**
 * @category
 * @date 2025/3/12 9:29
 * @description TODO
 */
public class Swc9kmDataIndbStrategy implements DataIndbStrategy {
    Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config_indb.properties");
//    Map<String, double[]> stationLonlats = ReadPropertiesUtil.getStationInfoConfigMap("stations_info.properties");
    Map<String, double[]> stationLonlats;// = QueryStationsInfoFromDBUtil.getInstance().getStationsInfo();
//    Map<String, double[]> stationLonlats = QueryStationsInfoFromDBUtil.getInstance().getStationsInfo();
    Map<String, String> dataTableMap = ReadPropertiesUtil.getUserConfigMap("data_table.properties");
    Map<String, Set<String>> configElementMap = ReadPropertiesUtil.getConfigMap("elements.properties");
    Map<String, String> elementMap = ReadPropertiesUtil.getUserConfigMap("swc9km_elements_map.properties");
    
    public void setStationLonlats(Map<String, double[]> stationLonlats)
    {
    	this.stationLonlats = stationLonlats;
    }
    
    @Override
    public Map<String, List<JSONObject>> getMapDataIndbList(String filePath, String tableName) {
        Map<String, List<JSONObject>> result = new HashMap<>();
//        SWCWARMS_20250218000000_F00_P10.grb
        String[] vtiDataTime = GribUtil.getVtiDataTime(filePath, DataTypeEnum.SWC9KM.getDataType());
        String vti = vtiDataTime[0];
        String dataTime = vtiDataTime[1];
        Date date = TimeUtil.dateTimeStr2date(dataTime, TimeUtil.DATE_FMT_YMDH, TimeUtil.DATE_FMT_YMDH);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR_OF_DAY, 8);
        dataTime = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        calendar.add(Calendar.HOUR_OF_DAY, Integer.parseInt(vti));
        String validDate = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT);
        Map<String, String> swc9kmMap = ReadTableConfigMapUtils.swc9kmMap;


        Map<String, Object> datasMap = GribFileReaderUtil.getDatasMap(filePath);
        if(datasMap.size() == 0)
        {
            return result;
        }
        Set<String> elements = configElementMap.get(DataTypeEnum.SWC9KM.getDataType());
        Set<String> uvSet = configElementMap.get(DataTypeEnum.SWC9KM.getDataType() + "@uv");
        Map<String, JSONObject> jsonMap = new HashMap<>();
        calendar.add(Calendar.HOUR_OF_DAY, Integer.parseInt(vti));
        for(String station : stationLonlats.keySet())
        {
            jsonMap.put(station, new JSONObject());
            jsonMap.get(station).put(swc9kmMap.get("station"), station);
            jsonMap.get(station).put(swc9kmMap.get("dataTime"), dataTime);
            jsonMap.get(station).put(swc9kmMap.get("validdate"), validDate);
            jsonMap.get(station).put(swc9kmMap.get("hour"), hour);
            jsonMap.get(station).put(swc9kmMap.get("vti"), Integer.parseInt(vti));
            jsonMap.get(station).put(swc9kmMap.get("filepath"), filePath);
            jsonMap.get(station).put(swc9kmMap.get("inserttime"), TimeUtil.date2String(new Date(), TimeUtil.DEFAULT_DATETIME_FORMAT));
        }
        double[] lonLats = null;
        int[] disVtiAndVti = GribUtil.getDisVtiAndVtiStation(filePath, DataTypeEnum.SWC9KM.getDataType());
        String[] lonLatName = NcReader.getLonLatName(datasMap);
        if(lonLatName == null)
        {
            return result;
        }
        double[] lons = ReadLonLatUtil.readLonLat(datasMap, lonLatName[0]);
        double[] lats = ReadLonLatUtil.readLonLat(datasMap, lonLatName[1]);
        if(lons == null || lats == null)
        {
            return result;
        }
        lats = ArrayReverse.reverse(lats);
        String lonlat = "60,50,140,10";
        for(String element : elements)
        {
            String elementField = element;
            if(element.startsWith("total_precipitation_surface"))
            {
                elementField = element + "_rain";
                element = element.replace("$", disVtiAndVti[1] + "");
            }
            double[][][] values = NcReader.readByElemNameLayerSlice(datasMap, element, null);
            if(values == null)
            {
                return result;
            }
            values[0] = ArrayReverse.transLevelLine(values[0]);
            for(String station : stationLonlats.keySet())
            {
                lonLats = stationLonlats.get(station);
                double[][] fstValue = DealDatasUtil.getFstValue(lons, lats, lonLats[0], lonLats[1], values[0], lonlat);
                double[] pointValues = fstValue[0];
                double[] pointLonLats = fstValue[1];
                jsonMap.get(station).put(elementMap.get(elementField), pointValues[4]);
                for(int i = 0; i < 4; i++)
                {
                    jsonMap.get(station).put(elementMap.get(elementField) + (i + 1), pointValues[i]);
                    jsonMap.get(station).put("lon" + (i + 1), pointLonLats[i * 2]);
                    jsonMap.get(station).put("lat" + (i + 1), pointLonLats[i * 2 + 1]);
                }
            }
        }
        if(uvSet != null)
        {
            for(String element : uvSet)
            {
                String elementV = element.replace(configMap.get(DataTypeEnum.SWC9KM.getDataType() + "_u"), configMap.get(DataTypeEnum.SWC9KM.getDataType() + "_v"));
                double[][][] datasU = NcReader.readByElemNameLayerSlice(datasMap, element, "10");
                double[][][] datasV = NcReader.readByElemNameLayerSlice(datasMap, elementV, "10");
                for(String station : stationLonlats.keySet())
                {
                    lonLats = stationLonlats.get(station);
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
        List<JSONObject> orgList = new ArrayList<>();
        for(String key : jsonMap.keySet())
        {
            orgList.add(jsonMap.get(key));
        }
        result.put(dataTableMap.get(DataTypeEnum.SWC9KM.getDataType() + "_value"), orgList);


        return result;
    }
}
