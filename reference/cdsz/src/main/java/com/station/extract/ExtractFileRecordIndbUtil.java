package com.station.extract;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson2.JSONObject;
import com.station.indb.util.DbUtils;
import com.station.indb.util.LoggableStatementUtil;
import com.util.GribUtil;
import com.util.ReadPropertiesUtil;
import com.util.TimeUtil;

@Slf4j
public class ExtractFileRecordIndbUtil {
	
	private static DruidDataSource dataSource = DbUtils.getInstance().getDataSource();
//    private static DataIndbStrategyContextService dataListService = new DataIndbStrategyContextService();
//    private static Set<String> persistEleSets = new HashSet<>();
    private static Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config_indb.properties");

	public static void fileIndb(Map<String, double[]> stationInfo, Map<String, List<String>> filePathMapList)
	{
		List<JSONObject> jsonList = new ArrayList<>();
		JSONObject json = null;
		String[] strs = null;
		for(String station : stationInfo.keySet())
		{
			for(String dataSource : filePathMapList.keySet())
			{
				for(String filePath : filePathMapList.get(dataSource))
				{
					json = new JSONObject();
					json.put("station", station);
					json.put("lon", stationInfo.get(station)[0]);
					json.put("lat", stationInfo.get(station)[1]);
					json.put("datasource", dataSource);
					json.put("filepath", filePath);
					json.put("extract", 1);
					json.put("inserttime", TimeUtil.date2String(new Date(), TimeUtil.DEFAULT_DATETIME_FORMAT));
					strs = filePath.split("/");
					json.put("datatime", TimeUtil.dateTimeStr2Str(strs[strs.length - 2], TimeUtil.DATE_FMT_YMD, TimeUtil.DEFAULT_DATE_FORMAT));
					jsonList.add(json);
				}
			}
		}
		
		insertUpdateDataToDb(jsonList, "public.extract_data_record_tab");
	}
	
	public static void fileIndb1(Map<String, double[]> stationInfo, Map<String, List<String>> filePathMapList)
	{
		List<JSONObject> jsonList = new ArrayList<>();
		JSONObject json = null;
//		String[] strs = null;
		for(String station : stationInfo.keySet())
		{
			for(String dataSource : filePathMapList.keySet())
			{
				for(String filePath : filePathMapList.get(dataSource))
				{
					json = new JSONObject();
					json.put("station", station);
					json.put("lon", stationInfo.get(station)[0]);
					json.put("lat", stationInfo.get(station)[1]);
					json.put("datasource", dataSource);
					json.put("filepath", filePath);
					json.put("extract", 1);
					json.put("inserttime", TimeUtil.date2String(new Date(), TimeUtil.DEFAULT_DATETIME_FORMAT));
//					strs = filePath.split("/");
					String[] vtiDataTime = GribUtil.getVtiDataTime(filePath, dataSource);
					json.put("datatime", vtiDataTime[1]);
					jsonList.add(json);
				}
			}
		}
		
		insertUpdateDataToDb(jsonList, "public.extract_data_record_tab");
	}
	
	public static void updateFileIndb(String filePath, String dataType, String tableName, Map<String, double[]> stationInfo)
	{
		List<JSONObject> jsonList = new ArrayList<>();
		JSONObject json = null;
		String[] strs = null;
		for(String station : stationInfo.keySet())
		{
			json = new JSONObject();
			json.put("station", station);
			json.put("lon", stationInfo.get(station)[0]);
			json.put("lat", stationInfo.get(station)[1]);
			json.put("datasource", dataType);
			json.put("filepath", filePath);
			strs = filePath.split("/");
			json.put("datatime", TimeUtil.dateTimeStr2Str(strs[strs.length - 2], TimeUtil.DATE_FMT_YMD, TimeUtil.DEFAULT_DATE_FORMAT));
			json.put("extract", 0);
			json.put("inserttime", TimeUtil.date2String(new Date(), TimeUtil.DEFAULT_DATETIME_FORMAT));
			jsonList.add(json);
		}
		insertUpdateDataToDb(jsonList, "public.extract_data_record_tab");
	}
	
	public static void updateFileIndb1(String filePath, String dataType, String tableName, Map<String, double[]> stationInfo)
	{
		List<JSONObject> jsonList = new ArrayList<>();
		JSONObject json = null;
//		String[] strs = null;
		for(String station : stationInfo.keySet())
		{
			json = new JSONObject();
			json.put("station", station);
			json.put("lon", stationInfo.get(station)[0]);
			json.put("lat", stationInfo.get(station)[1]);
			json.put("datasource", dataType);
			json.put("filepath", filePath);
			String[] vtiDataTime = GribUtil.getVtiDataTime(filePath, dataType);
			json.put("datatime", vtiDataTime[1]);
			json.put("extract", 0);
			json.put("inserttime", TimeUtil.date2String(new Date(), TimeUtil.DEFAULT_DATETIME_FORMAT));
			jsonList.add(json);
		}
		insertUpdateDataToDb(jsonList, "public.extract_data_record_tab");
	}
//	                                                                        dataType     date
	public static void queryFileIndb(Map<String, double[]> stationInfo, Map<String, Date[]> startEndDate, Map<String, Map<String, Set<String>>> dateFilePathStationMap)
	{
		String sql = "select * from public.extract_data_record_tab where extract = 1";
		try (Connection conn = dataSource.getConnection();
			 LoggableStatementUtil ps = new LoggableStatementUtil(conn, sql);
			 ResultSet rs = ps.executeQuery()){
	        String station = null;
	        String dataSource = null;
	        String date = null;
	        String filePath = null;
	        List<String> dateList = new ArrayList<>();
//	        Map<String, List<String>> dateMap =new HashMap<>();
//	           1     2   3      4         5        6        7         8
//	        station,lon,lat,datasource,filepath,extract,inserttime,datatime
//	             date      filePath     station
//	        Map<String, Map<String, List<String>>> dateFilePathStationMap = new HashMap<>();
			while(rs.next())
	        {
	        	station = rs.getString(1);
	        	if(!stationInfo.containsKey(station))
	        	{
	        		stationInfo.put(station, new double[]{rs.getDouble(2), rs.getDouble(3)});
	        	}
	        	dataSource = rs.getString(4);
	        	date = rs.getString(8);
//	        	if(!filePathMapList.containsKey(dataSource))
//	        	{
//	        		filePathMapList.put(dataSource, new HashMap<>());
//	        	}
//	        	if(!filePathMapList.get(dataSource).containsKey(date))
//	        	{
//	        		filePathMapList.get(dataSource).put(date, new ArrayList<>());
//	        	}
//	        	filePathMapList.get(dataSource).get(date).add(filePath);
	        	filePath = rs.getString(5);
	        	
	        	if(!dateFilePathStationMap.containsKey(date))
	        	{
	        		dateFilePathStationMap.put(date, new HashMap<>());
	        	}
	        	if(!dateFilePathStationMap.get(date).containsKey(filePath))
	        	{
	        		dateFilePathStationMap.get(date).put(filePath, new HashSet<>());
	        	}
	        	dateFilePathStationMap.get(date).get(filePath).add(station);
	        	
//	        	if(!dateMap.containsKey(dataSource))
//	        	{
//	        		dateMap.put(dataSource, new ArrayList<>());
//	        	}
//	        	dateMap.get(dataSource).add(date);
	        	dateList.add(date);
	        }
			
//			for(String st : dateMap.keySet())
//			{
//				List<String> list = dateMap.get(st);
//				list = list.stream().sorted().collect(Collectors.toList());
//				startEndDate.put(st, new Date[]{TimeUtil.dateTimeStr2date(trans(list.get(0)), TimeUtil.DEFAULT_DATE_FORMAT, TimeUtil.DEFAULT_DATETIME_FORMAT), 
//												TimeUtil.dateTimeStr2date(trans(list.get(list.size() - 1)), TimeUtil.DEFAULT_DATE_FORMAT, TimeUtil.DEFAULT_DATETIME_FORMAT)});
//			}
			
			if(dateList.size() > 0)
			{
				dateList = dateList.stream().sorted().collect(Collectors.toList());
				startEndDate.put("date", new Date[]{TimeUtil.dateTimeStr2date(trans(dateList.get(0)), TimeUtil.DEFAULT_DATE_FORMAT, TimeUtil.DEFAULT_DATETIME_FORMAT), 
						TimeUtil.dateTimeStr2date(trans(dateList.get(dateList.size() - 1)), TimeUtil.DEFAULT_DATE_FORMAT, TimeUtil.DEFAULT_DATETIME_FORMAT)});
			}
			
		} catch (Exception e) {
		    e.printStackTrace();
	    }
	}
	
//	public static void queryFileIndb(Map<String, double[]> stationInfo, Map<String, Map<String, List<String>>> filePathMapList, Map<String, Date[]> startEndDate)
//	{
//		String sql = "select * from public.extract_data_record_tab where extract = 1";
//		try (Connection conn = dataSource.getConnection();
//			 LoggableStatementUtil ps = new LoggableStatementUtil(conn, sql);
//			 ResultSet rs = ps.executeQuery()){
//	        String station = null;
//	        String dataSource = null;
//	        String date = null;
//	        Map<String, List<String>> dateMap =new HashMap<>();
//			while(rs.next())
//	        {
//	        	station = rs.getString(1);
//	        	if(!stationInfo.containsKey(station))
//	        	{
//	        		stationInfo.put(station, new double[]{rs.getDouble(2), rs.getDouble(3)});
//	        	}
//	        	if(!filePathMapList.containsKey(station))
//	        	{
//	        		filePathMapList.put(station, new HashMap<>());
//	        	}
//	        	dataSource = rs.getString(4);
//	        	date = rs.getString(8);
//	        	if(!filePathMapList.get(station).containsKey(dataSource))
//	        	{
//	        		filePathMapList.get(station).put(dataSource, new ArrayList<>());
//	        	}
//	        	filePathMapList.get(station).get(dataSource).add(rs.getString(5));
//	        	if(!dateMap.containsKey(station))
//	        	{
//	        		dateMap.put(station, new ArrayList<>());
//	        	}
//	        	dateMap.get(station).add(date);
//	        }
//			
//			for(String st : dateMap.keySet())
//			{
//				List<String> list = dateMap.get(st);
//				list = list.stream().sorted().collect(Collectors.toList());
//				startEndDate.put(st, new Date[]{TimeUtil.dateTimeStr2date(trans(list.get(0)), TimeUtil.DEFAULT_DATE_FORMAT, TimeUtil.DEFAULT_DATETIME_FORMAT), 
//												TimeUtil.dateTimeStr2date(trans(list.get(list.size() - 1)), TimeUtil.DEFAULT_DATE_FORMAT, TimeUtil.DEFAULT_DATETIME_FORMAT)});
//			}
//			
//		} catch (Exception e) {
//		    e.printStackTrace();
//	    }
//	}
	
	private static String trans(String date)
	{
		String result = "";
		if(date.contains("-"))
		{
			result = date;
		}
		else
		{
			result = date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8);
		}
		
		return result;
	}
	
	private static void insertUpdateDataToDb(List<JSONObject> dataList, String tableName) {

        Connection conn = null;
        LoggableStatementUtil ps = null;
        int batchSize = 250;
        List<String> sqlList = new ArrayList<>();
        try {
            conn = dataSource.getConnection();
            // 不自动提交事务
            conn.setAutoCommit(false);
            // 获取所有全面的字段的key
            Set<String> fieldNames = new HashSet<>();
            for (JSONObject jsonObject : dataList) {
                fieldNames.addAll(jsonObject.keySet());
            }
            if(fieldNames.size() == 0)
            {
                return ;
            }
            StringBuffer fieldSb = new StringBuffer();
            StringBuffer valueSb = new StringBuffer();
            StringBuffer setSb = new StringBuffer();
            for (String field : fieldNames) {
                fieldSb.append(field).append(",");
                setSb.append(field).append(" =").append(" ? ").append(",");
                valueSb.append(" ? ").append(",");
            }
//            System.out.println("============: " + configMap.get(tableName));
            String sql = "insert into " + tableName + "(" + fieldSb.substring(0, fieldSb.length() - 1) + ") values("
                    + valueSb.substring(0, valueSb.length() - 1) + ")"
                    + " on conflict(" + configMap.get(tableName) + ") do update set " + setSb.substring(0, setSb.length() - 1);
//            ps = conn.prepareStatement(sql);
            ps = new LoggableStatementUtil(conn, sql);
//            log.info("预前置sql:{}", sql);
            int count = 0;
            for (JSONObject jsonObject : dataList) {
                int valueIndex = 0;
                for (String fieldName : fieldNames) {
                    Object obj = (jsonObject.containsKey(fieldName) && jsonObject.get(fieldName) != null) ? jsonObject.get(fieldName) : null;
//                    System.out.println("fieldName:" + fieldName + " obj:" + obj);
                    ps.setObject(valueIndex + 1, obj);
                    ps.setObject(fieldNames.size() + valueIndex + 1, obj);
                    valueIndex++;
                }
                count++;
                ps.addBatch();
                sqlList.add(ps.getQueryString());
//                System.out.println("完整sql: " + ps.getQueryString());
                if (count % batchSize == 0) {
//                    log.info("提交一次{}", count);
                    ps.executeBatch();
                    ps.clearBatch();
                }
            }
            // 提交最后一次
            if (dataList.size() % batchSize != 0) {
                ps.executeBatch();
//                log.info("提交最后一次{}", dataList.size() % batchSize);
                ps.clearBatch();
            }
            log.info("该批次处理完毕!");
            conn.commit();
            
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                conn.close();
//                conn.rollback();
                conn = dataSource.getConnection();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            log.info("开始逐条入库>>>>>>>");
            Statement st = null;
            try {
                st = conn.createStatement();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            if(st != null)
            {
                for(String sql : sqlList)
                {
                    try {
                    	System.out.println("===============>sql: " + sql);
                        st.executeUpdate(sql);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
            }
            sqlList.clear();
            try {
                st.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
