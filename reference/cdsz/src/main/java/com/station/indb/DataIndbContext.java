package com.station.indb;

import com.constants.DataTypeEnum;
import com.station.indb.inf.DataIndbStrategy;
import com.station.indb.strategy.CldasDataIndbStrategy;
import com.station.indb.strategy.CldasP5DataIndbStrategy;
import com.station.indb.strategy.EcmwfDataIndbStrategy;
import com.station.indb.strategy.FY4BDataIndbStrategy;
import com.station.indb.strategy.GrapesDataIndbStrategy;
import com.station.indb.strategy.GribRainDataIndbStrategy;
import com.station.indb.strategy.MicapsDataIndbStrategy;
import com.station.indb.strategy.StationDataIndbStrategy;
import com.station.indb.strategy.Swc9kmDataIndbStrategy;

import java.util.HashMap;
import java.util.Map;

/**
 * @category
 * @date 2025/3/12 9:21
 * @description TODO
 */
public class DataIndbContext {
    private static Map<String, Class<? extends DataIndbStrategy>> map = new HashMap<>();
    static
    {
        map.put(DataTypeEnum.CLDAS.getDataType(), CldasDataIndbStrategy.class);
        map.put(DataTypeEnum.CLDAS.getDataType() + "_p5", CldasP5DataIndbStrategy.class);
        map.put(DataTypeEnum.ECMF.getDataType(), EcmwfDataIndbStrategy.class);
        map.put(DataTypeEnum.MICAPS.getDataType(), MicapsDataIndbStrategy.class);
        map.put(DataTypeEnum.GRAPES.getDataType(), GrapesDataIndbStrategy.class);
        map.put(DataTypeEnum.SWC9KM.getDataType(), Swc9kmDataIndbStrategy.class);
        map.put(DataTypeEnum.STATION.getDataType(), StationDataIndbStrategy.class);
        map.put(DataTypeEnum.FY4B.getDataType(), FY4BDataIndbStrategy.class);
        map.put(DataTypeEnum.ECMF.getDataType() + "_" + DataTypeEnum.RAIN.getDataType(), GribRainDataIndbStrategy.class);
        map.put(DataTypeEnum.GRAPES.getDataType() + "_" + DataTypeEnum.RAIN.getDataType(), GribRainDataIndbStrategy.class);
        map.put(DataTypeEnum.DEEP.getDataType() + "_" + DataTypeEnum.RAIN.getDataType(), GribRainDataIndbStrategy.class);
    }

    public static Class getDataIndbStrategy(String dataType)
    {
        return map.get(dataType);
    }
}
