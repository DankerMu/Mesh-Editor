package com.station.indb;

import com.alibaba.fastjson.JSONObject;
import com.station.indb.inf.DataIndbStrategy;

import java.util.List;
import java.util.Map;

/**
 * @Package com.data.strategy
 * @Description: java类作用描述
 * @date 2024/2/29 10:11
 * @Version: 1.0
 */
public class DataIndbStrategyContextService {

    private DataIndbStrategy strategy;

    public Map<String, List<JSONObject>> getDataList(String filePath, String dataType, String tableName, Map<String, double[]> stationLonlats)
    {
        Class<? extends DataIndbStrategy> clazz = DataIndbContext.getDataIndbStrategy(dataType);

        strategy = DataIndbStrategyFactory.getInstance(clazz);
        
        strategy.setStationLonlats(stationLonlats);

        return strategy == null ? null : strategy.getMapDataIndbList(filePath, tableName);
    }
}
