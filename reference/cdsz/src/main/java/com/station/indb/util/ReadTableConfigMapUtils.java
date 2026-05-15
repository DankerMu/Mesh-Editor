package com.station.indb.util;

import com.util.ReadPropertiesUtil;

import java.util.Map;

/**
 * @Package com.util
 * @Description: java类作用描述
 * @date 2024/2/29 14:19
 * @Version: 1.0
 */
public class ReadTableConfigMapUtils {

    public static Map<String, String> tableMap;
    public static Map<String, String> ecmfMap;
    public static Map<String, String> grapesMap;
    public static Map<String, String> cldasMap;
    public static Map<String, String> scmocMap;
    public static Map<String, String> swc3kmMap;
    public static Map<String, String> swc9kmMap;
    public static Map<String, String> cmpaMap;
    public static Map<String, String> ktMap;
    public static Map<String, String> t1279Map;
    public static Map<String, String> stationMap;
    public static Map<String, String> micapsStationMap;
    public static Map<String, String> fy4bMap;
    public static Map<String, String> rainMap;
    public static Map<String, String> ptypeMap;
    public static Map<String, String> nafpMap;

    static
    {
        try {
            tableMap = ReadPropertiesUtil.getTableConfigMap("data_table.properties");
            ecmfMap = ReadPropertiesUtil.getTableConfigMap("ecmf_fields.properties");
            grapesMap = ReadPropertiesUtil.getTableConfigMap("grapes_fields.properties");
            cldasMap = ReadPropertiesUtil.getTableConfigMap("cldas_fields.properties");
//            scmocMap = ReadPropertiesUtil.getTableConfigMap("scmoc_fields.properties");
//            swc3kmMap = ReadPropertiesUtil.getTableConfigMap("swc3km_fields.properties");
            swc9kmMap = ReadPropertiesUtil.getTableConfigMap("swc9km_fields.properties");
//            cmpaMap = ReadPropertiesUtil.getTableConfigMap("cmpa_fields.properties");
//            ktMap = ReadPropertiesUtil.getTableConfigMap("kt_fields.properties");
//            t1279Map = ReadPropertiesUtil.getTableConfigMap("t1279_fields.properties");
            stationMap = ReadPropertiesUtil.getTableConfigMap("station_fields.properties");
            micapsStationMap = ReadPropertiesUtil.getTableConfigMap("micaps_station_fields.properties");
            fy4bMap = ReadPropertiesUtil.getTableConfigMap("fy4b_fields.properties");
            rainMap = ReadPropertiesUtil.getTableConfigMap("rain_fields.properties");
            ptypeMap = ReadPropertiesUtil.getTableConfigMap("ptype_fields.properties");
            nafpMap = ReadPropertiesUtil.getTableConfigMap("nafp_fields.properties");
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public static Map<String, String> getTableMap()
    {
        tableMap = ReadPropertiesUtil.getTableConfigMap("data_table.properties");
        return tableMap;
    }
}
