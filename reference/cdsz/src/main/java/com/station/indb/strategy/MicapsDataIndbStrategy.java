package com.station.indb.strategy;

import cn.hutool.core.io.FileUtil;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.JSONObject;
import com.constants.DataTypeEnum;
import com.station.indb.inf.DataIndbStrategy;
import com.station.indb.util.DbUtils;
import com.station.indb.util.LoggableStatementUtil;
import com.station.indb.util.ReadTableConfigMapUtils;
import com.util.NumberFormatUtil;
import com.util.TimeUtil;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * @category
 * @date 2025/6/4 10:12
 * @description TODO
 */
public class MicapsDataIndbStrategy implements DataIndbStrategy {
    @Override
    public Map<String, List<JSONObject>> getMapDataIndbList(String filePath, String tableName) {
        Map<String, List<JSONObject>> result = new HashMap<>();
        List<String> lines = FileUtil.readLines(filePath, "utf-8");
//        "station", "lon", "lat", "datatime", "year", "month", "day", "hour", "at", "rh", "wd", "ws", "slp", "vis", "n", "rain", "filename", "filepath", "inserttime"
//        "datetime", "station_id_d", "lat", "lon", "alti", "prs_sea", "tem", "dpt", "rhu", "pre_1h", "pre", "win_d_avg_10mi", "win_s_avg_10mi", "win_d_inst", "win_s_inst", "vis"
//        String prefix = FileUtil.getPrefix(filePath);
        if(lines.size() < 2)
        {
        	return result;
        }
        String dataTime = lines.get(1).replace("/", "-").split(",")[0];//2025/7/16 23:00:00
        
        String fmt1 = "yyyy-MM-dd HH:mm";
        if(dataTime.length() == 10)
        {
            fmt1 = "yyyy-MM-dd";
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(TimeUtil.String2Date(dataTime, fmt1));
        calendar.add(Calendar.HOUR_OF_DAY, -8);
        dataTime = TimeUtil.date2String(calendar.getTime(), "yyyy-MM-dd HH:mm");
        
        String header = lines.get(0);
        String[] headerArray = header.split(",");
        Map<String, String> stationMap = ReadTableConfigMapUtils.micapsStationMap;
        Map<String, Integer> elementIndexMap = new HashMap<>();
        for(int i = 0, count = headerArray.length; i < count; i++)
        {
            elementIndexMap.put(stationMap.get(headerArray[i]), i);
        }
        Map<String, double[]> atMaxMin24hTp = getAtMaxMin24hTp(dataTime);
        String[] datas = null;
        String dataTimeStr = null;
        List<JSONObject> list = new ArrayList<>();
        
        for(int i = 1, count = lines.size(); i < count; i++)
        {
            datas = lines.get(i).replace("/", "-").split(",");
            JSONObject json = new JSONObject();

            json.put(stationMap.get("station_id_d"), datas[elementIndexMap.get(stationMap.get("station_id_d"))]);
            dataTimeStr = datas[elementIndexMap.get(stationMap.get("datetime"))];
            String fmt = "yyyy-MM-dd HH:mm";
            if(dataTimeStr.length() == 10)
            {
                fmt = "yyyy-MM-dd";
            }
            calendar.setTime(TimeUtil.String2Date(dataTimeStr, fmt));
            calendar.add(Calendar.HOUR_OF_DAY, -8);
            dataTimeStr = TimeUtil.date2String(calendar.getTime(), "yyyy-MM-dd HH:mm:ss");
            json.put(stationMap.get("datetime"), dataTimeStr);
//            2025/1/1 8:00
            json.put(stationMap.get("lon"), Double.parseDouble(datas[elementIndexMap.get(stationMap.get("lon"))]));
            json.put(stationMap.get("lat"), Double.parseDouble(datas[elementIndexMap.get(stationMap.get("lat"))]));
//            json.put(stationMap.get("lcc"), Double.parseDouble(datas[elementIndexMap.get(stationMap.get("lcc"))]));
//            json.put(stationMap.get("tcc"), Double.parseDouble(datas[elementIndexMap.get(stationMap.get("tcc"))]));
            json.put(stationMap.get("prs_sea"), Double.parseDouble(datas[elementIndexMap.get(stationMap.get("prs_sea"))]));
            json.put(stationMap.get("pre_1h"), Double.parseDouble(datas[elementIndexMap.get(stationMap.get("pre_1h"))]));
            json.put(stationMap.get("tem"), Double.parseDouble(datas[elementIndexMap.get(stationMap.get("tem"))]));
            json.put(stationMap.get("vis"), NumberFormatUtil.numFormat(Double.parseDouble(datas[elementIndexMap.get(stationMap.get("vis"))]), 3));
            json.put(stationMap.get("rhu"), Double.parseDouble(datas[elementIndexMap.get(stationMap.get("rhu"))]));
            json.put(stationMap.get("dpt"), Double.parseDouble(datas[elementIndexMap.get(stationMap.get("dpt"))]));
            json.put(stationMap.get("win_s_avg_10mi"), Double.parseDouble(datas[elementIndexMap.get(stationMap.get("win_s_avg_10mi"))]));
            json.put(stationMap.get("win_d_avg_10mi"), Double.parseDouble(datas[elementIndexMap.get(stationMap.get("win_d_avg_10mi"))]));
            double[] values = atMaxMin24hTp.get(json.get(stationMap.get("station_id_d")));
            if(atMaxMin24hTp == null || json.get(stationMap.get("station_id_d")) == null || atMaxMin24hTp.get(json.get(stationMap.get("station_id_d"))) == null)
            {
                values = new double[]{9999, 9999, 9999};
            }
            json.put(stationMap.get("tp"), values[2]);
            json.put(stationMap.get("tmax_24h"), getMaxValue(atMaxMin24hTp, json.getString(stationMap.get("station_id_d")), Double.parseDouble(datas[elementIndexMap.get(stationMap.get("tem"))])));
            json.put(stationMap.get("tmin_24h"), getMinValue(atMaxMin24hTp, json.getString(stationMap.get("station_id_d")), Double.parseDouble(datas[elementIndexMap.get(stationMap.get("tem"))])));
//            json.put(stationMap.get("tp"), Double.parseDouble(datas[elementIndexMap.get(stationMap.get("tp"))]));
//            json.put(stationMap.get("tmax_24h"), Double.parseDouble(datas[elementIndexMap.get(stationMap.get("tmax_24h"))]));
//            json.put(stationMap.get("tmin_24h"), Double.parseDouble(datas[elementIndexMap.get(stationMap.get("tmin_24h"))]));
            json.put(stationMap.get("filename"), FileUtil.getName(filePath));
            json.put(stationMap.get("filepath"), filePath);
            json.put(stationMap.get("inserttime"), TimeUtil.date2String(new Date(), TimeUtil.DEFAULT_DATETIME_FORMAT));
            list.add(json);
        }
        result.put(tableName, list);

        List<JSONObject> fileManagerList = new ArrayList<>();
//      20251023150000.000.csv
        String name = FileUtil.getName(filePath);
        String suffix = FileUtil.getSuffix(filePath);
        String vtiDataTime = TimeUtil.dateTimeStr2Str(name.substring(0, 14), TimeUtil.DATE_FMT_YMDHMS, TimeUtil.DEFAULT_DATETIME_FORMAT);
        String dataTime1 = vtiDataTime.split(" ")[0] + " 00:00:00";
        Date date = TimeUtil.dateTimeStr2date(vtiDataTime, TimeUtil.DEFAULT_DATETIME_FORMAT, TimeUtil.DEFAULT_DATETIME_FORMAT);
        calendar.setTime(date);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        long fileSize = FileUtil.size(new File(filePath));
        String inserttime = TimeUtil.date2String(new Date(), TimeUtil.DEFAULT_DATETIME_FORMAT);
        Map<String, String> nafpMap = ReadTableConfigMapUtils.nafpMap;
        JSONObject json = new JSONObject();
        json.put(nafpMap.get("filename"), name);
        json.put(nafpMap.get("dataTime"), dataTime1);
        json.put(nafpMap.get("validdate"), vtiDataTime);
        json.put(nafpMap.get("hour"), hour);
        json.put(nafpMap.get("vti"), 1);
        json.put(nafpMap.get("datatype"), DataTypeEnum.MICAPS.getDataType());
        json.put(nafpMap.get("filepath"), filePath);
        json.put(nafpMap.get("inserttime"), inserttime);
        json.put(nafpMap.get("filesize"), fileSize);
        json.put(nafpMap.get("filetype"), suffix);
        fileManagerList.add(json);
        
        result.put("public.nafp_grib_tab", fileManagerList);

        return result;
    }

    private double getMaxValue(Map<String, double[]> valuesMap, String station, double value)
    {
        double[] values = valuesMap.get(station);
        if(values == null)
        {
            return 9999;
        }
        if(values[0] == 9999)
        {
            values[0] = -9999;
        }
        if(value == 9999)
        {
            value = -9999;
        }

        return values[0] > value ? values[0] : value;
    }

    private double getMinValue(Map<String, double[]> valuesMap, String station, double value)
    {
        double[] values = valuesMap.get(station);
        if(values == null)
        {
            return 9999;
        }
        if(values[1] == 9999)
        {
            value = 9999;
        }

        return values[1] < value ? values[1] : value;
    }

    private Map<String, double[]> getAtMaxMin24hTp(String dataTime)
    {
        Map<String, double[]> result = new HashMap<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(TimeUtil.String2Date(dataTime, "yyyy-MM-dd HH:mm"));
        calendar.add(Calendar.HOUR_OF_DAY, -24);
        String dataTimeStr = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT);
        String sql = "select station_id_d,datetime,tem,pre_1h from surf_chn_mul_hor_micaps where datetime >'"+dataTimeStr+"'" + " and datetime <='" + dataTime +"'";

        DruidDataSource dataSource = DbUtils.getInstance().getDataSource();
//        Connection conn = null;
//        ResultSet rs = null;
//        LoggableStatementUtil ps = null;
        try (Connection conn = dataSource.getConnection();
             LoggableStatementUtil ps = new LoggableStatementUtil(conn, sql);
             ResultSet rs = ps.executeQuery()){
//            conn = dataSource.getConnection();
//            ps = new LoggableStatementUtil(conn, sql);
//            rs = ps.executeQuery();
            String stationId = null;
//            String datetime = null;
            double tem;
            double pre_1h;
            while (rs.next())
            {
                stationId = rs.getString("station_id_d");
//                datetime = rs.getString("datetime");
                tem = rs.getDouble("tem");
                pre_1h = rs.getDouble("pre_1h");
                
                if(tem == 999999)
                {
                	tem = 9999;
                }
                if(pre_1h == 999999)
                {
                	pre_1h = 9999;
                }
                
                if(!result.containsKey(stationId))
                {
                    if(pre_1h == 9999)
                    {
                        pre_1h = 0;
                    }

                    if(tem == 9999)
                    {
                        result.put(stationId, new double[]{-9999, 9999, pre_1h});
                    }
                    else
                    {
                        result.put(stationId, new double[]{tem, tem, pre_1h});
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
                    if(pre_1h != 9999)
                    {
                        result.get(stationId)[2] += pre_1h;
                    }
                }
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

	@Override
	public void setStationLonlats(Map<String, double[]> stationLonlats) {
		
		
	}
}