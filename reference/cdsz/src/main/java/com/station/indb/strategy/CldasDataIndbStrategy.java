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
public class CldasDataIndbStrategy implements DataIndbStrategy {
	
	Map<String, Set<String>> configElementMap = ReadPropertiesUtil.getConfigMap("elements.properties");
	Map<String, double[]> stationLonlats;// = QueryStationsInfoFromDBUtil.getInstance().getStationsInfo();
	Map<String, String> elementMap = ReadPropertiesUtil.getUserConfigMap("cldas_elements_map.properties");
	Map<String, String> dataTableMap = ReadPropertiesUtil.getUserConfigMap("data_table.properties");
	private static Map<String, String> nameToElementMap = new HashMap<>();
	static
	{
//		nameToElementMap.put("TEM", "tair");
//		nameToElementMap.put("WIN", "wind");
		nameToElementMap.put("PRE", "prcp");
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
        String lonlat = "60.031250, 64.968750, 159.968750, 0.031250";
        
        
        String ele = name.split("_")[10].split("-")[1];
        
        String element = nameToElementMap.get(ele);
        double[][][] values = NcReader.readByElemNameLayerSlice(datasMap, element, null);
        if(values == null)
        {
            return result;
        }
        
//        Map<String, double[]> rain24Map = get24hTp(dataTime);
        
        values[0] = ArrayReverse.transLevelLine(values[0]);
        for(String station : stationLonlats.keySet())
        {
            lonLats = stationLonlats.get(station);
            double[][] fstValue = DealDatasUtil.getFstValue(lons, lats, lonLats[0], lonLats[1], values[0], lonlat);
            double[] pointValues = fstValue[0];
            jsonMap.get(station).put(elementMap.get(element), pointValues[4]);
            jsonMap.get(station).put("lon", lonLats[0]);
            jsonMap.get(station).put("lat", lonLats[1]);
//            jsonMap.get(station).put("pre_24h", rain24Map.get(station) == null ? 9999 : rain24Map.get(station)[0]);
        }
        
        List<JSONObject> orgList = new ArrayList<>();
        for(String key : jsonMap.keySet())
        {
            orgList.add(jsonMap.get(key));
        }
        result.put(dataTableMap.get(DataTypeEnum.CLDAS.getDataType()), orgList);


        return result;
    }
    
    private Map<String, double[]> get24hTp(String dataTime)
    {
        Map<String, double[]> result = new HashMap<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(TimeUtil.String2Date(dataTime, "yyyy-MM-dd HH:mm"));
        calendar.add(Calendar.HOUR_OF_DAY, -24);
        String dataTimeStr = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT);
        String sql = "select station_id_d,datetime,pre_1h from surf_chn_mul_hor_micaps where datetime >'"+dataTimeStr+"'" + " and datetime <='" + dataTime +"'";

        DruidDataSource dataSource = DbUtils.getInstance().getDataSource();
        try (Connection conn = dataSource.getConnection();
             LoggableStatementUtil ps = new LoggableStatementUtil(conn, sql);
             ResultSet rs = ps.executeQuery()){
            String stationId = null;
            double pre_1h;
            while (rs.next())
            {
                stationId = rs.getString("station_id_d");
                pre_1h = rs.getDouble("pre_1h");
                if(!result.containsKey(stationId))
                {
                    if(pre_1h == 9999)
                    {
                        pre_1h = 0;
                    }

                    result.put(stationId, new double[]{pre_1h});
                }
                else
                {
                    if(pre_1h != 9999)
                    {
                        result.get(stationId)[0] += pre_1h;
                    }
                }
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }
}
