package com.station.indb.strategy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.hutool.core.io.FileUtil;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.JSONObject;
import com.constants.DataTypeEnum;
import com.station.indb.inf.DataIndbStrategy;
import com.station.indb.util.DealDatasUtil;
import com.station.indb.util.LoggableStatementUtil;
import com.station.indb.util.ReadTableConfigMapUtils;
import com.util.ArrayReverse;
import com.util.DbUtils;
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
public class CldasP5DataIndbStrategy implements DataIndbStrategy {
	
	Map<String, Set<String>> configElementMap = ReadPropertiesUtil.getConfigMap("elements.properties");
	Map<String, double[]> stationLonlats;// = QueryStationsInfoFromDBUtil.getInstance().getStationsInfo();
	Map<String, String> elementMap = ReadPropertiesUtil.getUserConfigMap("cldas_elements_map.properties");
	Map<String, String> dataTableMap = ReadPropertiesUtil.getUserConfigMap("data_table.properties");
	Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config_indb.properties");
	private static Map<String, String> nameToElementMap = new HashMap<>();
	static
	{
		nameToElementMap.put("TEM", "temperature_height_above_ground");
		nameToElementMap.put("WIN", "wind");
		nameToElementMap.put("PRE", "prcp");
		nameToElementMap.put("VIS", "visibility_surface");
	}
	
	public void setStationLonlats(Map<String, double[]> stationLonlats)
    {
    	this.stationLonlats = stationLonlats;
    }
	
    @Override
    public Map<String, List<JSONObject>> getMapDataIndbList(String filePath, String tableName) {
        Map<String, List<JSONObject>> result = new HashMap<>();
//        Z_NAFP_C_BABJ_20250217091020_P_CLDAS_RT_ASI_0P0625_HOR-PRS-2025021709.nc
        String name = FileUtil.getName(filePath);
        String[] vtiDataTime = GribUtil.getVtiDataTime(filePath, DataTypeEnum.CLDAS.getDataType());
//        String vti = vtiDataTime[0];
        String dataTime = vtiDataTime[1];
        Date date = TimeUtil.dateTimeStr2date(dataTime, TimeUtil.DATE_FMT_YMDH, TimeUtil.DATE_FMT_YMDH);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR_OF_DAY, -8);
//        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        dataTime = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        Map<String, String> cldasMap = ReadTableConfigMapUtils.cldasMap;
        
        
        Map<String, Object> datasMap = GribFileReaderUtil.getDatasMap(filePath);
        if(datasMap.size() == 0)
        {
            return result;
        }
        long time = System.currentTimeMillis();
        Set<String> uvSet = configElementMap.get(DataTypeEnum.CLDAS.getDataType() + "@uv");
        Map<String, JSONObject> jsonMap = new HashMap<>();
        for(String station : stationLonlats.keySet())
        {
            jsonMap.put(station, new JSONObject());
            jsonMap.get(station).put(cldasMap.get("station"), station);
            jsonMap.get(station).put(cldasMap.get("dataTime"), dataTime);
            jsonMap.get(station).put(cldasMap.get("filename"), name);
            jsonMap.get(station).put(cldasMap.get("filepath"), filePath);
            jsonMap.get(station).put(cldasMap.get("inserttime"), TimeUtil.date2String(new Date(), TimeUtil.DEFAULT_DATETIME_FORMAT));
        }
        double[] lonLats = null;
        String[] lonLatName = NcReader.getLonLatName(datasMap);
        if(lonLatName == null)
        {
            return result;
        }
        double[] lons = ReadLonLatUtil.readLonLat(datasMap, lonLatName[0].toLowerCase());
        double[] lats = ReadLonLatUtil.readLonLat(datasMap, lonLatName[1].toLowerCase());
        if(lons == null || lats == null)
        {
            return result;
        }
        lats = ArrayReverse.reverse(lats);
        String lonlat = "70, 60, 140, 0";
        
        
        String ele = name.split("_")[10].split("-")[1];
//        Map<String, double[]> atMaxMin24h = null;
//        if("TEM".equals(ele))
//        {
//        	atMaxMin24h = getAtMaxMin24h(dataTime);
//        }
        
        if(!"WIN".equals(ele))
        {
        	String element = nameToElementMap.get(ele);
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
                if(element.equals("temperature_height_above_ground"))
                {
                	jsonMap.get(station).put(elementMap.get(element), pointValues[4] - 273.15);
                }
                else
                {
                	jsonMap.get(station).put(elementMap.get(element), pointValues[4]);
                }
                jsonMap.get(station).put("lon", lonLats[0]);
                jsonMap.get(station).put("lat", lonLats[1]);
//                if("TEM".equals(ele) && atMaxMin24h != null && atMaxMin24h.size() != 0)
//                {
//                	jsonMap.get(station).put("tem_max_24h", atMaxMin24h.get(station) == null ? 9999 : atMaxMin24h.get(station)[0]);
//                    jsonMap.get(station).put("tem_min_24h", atMaxMin24h.get(station) == null ? 9999 : atMaxMin24h.get(station)[1]);
//                }
            }
        }
        else
        {
        	for(String elemt : uvSet)
            {
                String elementV = elemt.replace(configMap.get(DataTypeEnum.CLDAS.getDataType() + "_u"), configMap.get(DataTypeEnum.CLDAS.getDataType() + "_v"));
                double[][][] datasU = NcReader.readByElemNameLayerSlice(datasMap, elemt, null);
                datasU[0] = ArrayReverse.transLevelLine(datasU[0]);
                double[][][] datasV = NcReader.readByElemNameLayerSlice(datasMap, elementV, null);
                datasV[0] = ArrayReverse.transLevelLine(datasV[0]);
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
                    jsonMap.get(station).put("win_s_avg_10mi", pointValuesWs[4]);
                    jsonMap.get(station).put("win_d_avg_10mi", pointValuesWd[4]);
                    jsonMap.get(station).put("lon", lonLats[0]);
                    jsonMap.get(station).put("lat", lonLats[1]);
                }
            }
        }
        
        System.out.println(filePath + "数据处理耗时:" + (System.currentTimeMillis() - time));
        List<JSONObject> orgList = new ArrayList<>();
        for(String key : jsonMap.keySet())
        {
            orgList.add(jsonMap.get(key));
        }
        result.put(dataTableMap.get(DataTypeEnum.CLDAS.getDataType()), orgList);


        return result;
    }

    private Map<String, double[]> getAtMaxMin24h(String dataTime)
    {
        Map<String, double[]> result = new HashMap<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(TimeUtil.String2Date(dataTime, "yyyy-MM-dd HH:mm"));
        calendar.add(Calendar.HOUR_OF_DAY, -24);
        String dataTimeStr = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT);
        String sql = "select station_id_d,datetime,tem from surf_chn_mul_hor_micaps where datetime >'"+dataTimeStr+"'" + " and datetime <='" + dataTime +"'";

        DruidDataSource dataSource = DbUtils.getInstance().getDataSource();
        try (Connection conn = dataSource.getConnection();
             LoggableStatementUtil ps = new LoggableStatementUtil(conn, sql);
             ResultSet rs = ps.executeQuery()){
            String stationId = null;
            double tem;
            while (rs.next())
            {
                stationId = rs.getString("station_id_d");
                tem = rs.getDouble("tem");
                if(!result.containsKey(stationId))
                {

                    if(tem == 9999)
                    {
                        result.put(stationId, new double[]{-9999, 9999});
                    }
                    else
                    {
                        result.put(stationId, new double[]{tem, tem});
                    }
                }
                else
                {
                    if(tem != 9999)
                    {
                        if(tem > result.get(stationId)[0])
                        {
                            result.get(stationId)[0] = tem;
                        }
                        if(tem < result.get(stationId)[1])
                        {
                            result.get(stationId)[1] = tem;
                        }
                    }
                }
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }
}
