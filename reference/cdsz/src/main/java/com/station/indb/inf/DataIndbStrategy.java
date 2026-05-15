package com.station.indb.inf;

import com.alibaba.fastjson.JSONObject;

import java.util.List;
import java.util.Map;

public interface DataIndbStrategy {


    Map<String, List<JSONObject>> getMapDataIndbList(String filePath, String tableName);
    
    void setStationLonlats(Map<String, double[]> stationLonlats);
}
