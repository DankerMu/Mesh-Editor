package com.check.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.hutool.core.date.DateUtil;

import com.util.TimeUtil;

public class DataCheckSqlUtil {

	public static Map<String, String> getSqlsByDate(String startDateStr, String endDateStr, String dataSource, String fieldsName, String stationConStr, String hourConStr)
	{
		Map<String, String> result = new HashMap<>();
		String sql = "";
		
		Calendar calendar = Calendar.getInstance();
		Date startDate = TimeUtil.String2Date(startDateStr.split(" ")[0], TimeUtil.DEFAULT_DATE_FORMAT);
		Date endDate = TimeUtil.String2Date(endDateStr.split(" ")[0], TimeUtil.DEFAULT_DATE_FORMAT);
		Map<String, List<Date>> map = new LinkedHashMap<>(); 
		for(int i = 0;; i++)
        {
            calendar.setTime(startDate);
            calendar.add(Calendar.DATE, i);
            int lastDayOfMonth = DateUtil.getLastDayOfMonth(calendar.getTime());
            String monthStr = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATE_FORMAT);
            monthStr = monthStr.substring(0, 7);
            if(calendar.getTime().after(endDate))
            {
                break;
            }
            if(!map.containsKey(monthStr))
            {
            	map.put(monthStr, new ArrayList<>());
            }
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            if(day == 1 || day == lastDayOfMonth)
            {
            	map.get(monthStr).add(calendar.getTime());
            }
        }
		List<String> monthSqlList = new ArrayList<>();
		List<String> daySqlList = new ArrayList<>();
		for(String key : map.keySet())
		{
			if(map.get(key).size() == 2)
			{
				monthSqlList.add(key);
				System.out.println(key);
			}
			else if(map.get(key).size() == 0)
			{
				continue;
			}
			else
			{
				daySqlList.add(TimeUtil.date2String(map.get(key).get(0), TimeUtil.DEFAULT_DATE_FORMAT));
				System.out.println(TimeUtil.date2String(map.get(key).get(0), TimeUtil.DEFAULT_DATE_FORMAT));
			}
		}
		int monthCount = monthSqlList.size();
		int dayCount = daySqlList.size();
		String tableName = "";
		if(monthCount > 0)
		{
			tableName = "public.station_check_value_month_tab";
			sql = "select station,datatime,datasource," + fieldsName + " from " + tableName + " where datatime >='" + 
				   monthSqlList.get(0) + "' and datatime <= '" + monthSqlList.get(monthSqlList.size() - 1) + "' " + stationConStr + " and datasource = '"+ 
				   dataSource + "' " + hourConStr;
			result.put("month", sql);
		}
		if(dayCount > 0 && monthCount > 0)
		{
			tableName = "public.station_check_value_tab";
			if(dayCount == 2)
			{
				sql = "select station,datatime,datasource," + fieldsName + " from " + tableName + 
					  " where ((datatime >='" + startDateStr.split(" ")[0] + "' and datatime <= '" + daySqlList.get(0) + "') or ("
					  + "datatime >='" + daySqlList.get(1) + "' and datatime <= '" + endDateStr.split(" ")[0] + "')) "
					  + stationConStr + " and datasource = '"+ dataSource + "' " + hourConStr;
				result.put("day", sql);
			}
			else if(dayCount == 1)
			{
				String startDates = startDateStr;
				String endDates = daySqlList.get(0).split(" ")[0];
				if(monthSqlList.get(monthCount - 1).compareTo(endDates.substring(0, 7)) < 0)
				{
					startDates = daySqlList.get(0).split(" ")[0];
					endDates = endDateStr;
				}
				
				sql = "select station,datatime,datasource," + fieldsName + " from " + tableName + " where datatime >='" + 
					   startDates + "' and datatime <= '" + endDates + "' " + stationConStr + " and datasource = '"+ 
					   dataSource + "' " + hourConStr;
				result.put("day", sql);
			}
		}
		if(dayCount == 0 && monthCount == 0)
		{
			tableName = "public.station_check_value_tab";
			sql = "select station,datatime,datasource," + fieldsName + " from " + tableName + " where datatime >='" + 
				   startDateStr.split(" ")[0] + "' and datatime <= '" + endDateStr.split(" ")[0] + "' " + stationConStr + " and datasource = '"+ 
				   dataSource + "' " + hourConStr;
				result.put("day", sql);
		}
		
		
		return result;
	}
	
	public static void main(String[] args) {
		Map<String, String> result = new HashMap<>();
		String startDateStr = "2025-10-01 20:00:00";
		String endDateStr = "2025-10-31 20:00:00";
		String dataSource = "";
		String stationsStr = "";
		String tableName = "";
		String fieldsName = "*";
		int hour = Integer.parseInt(startDateStr.split(" ")[1].split(":")[0]);
		String hourCon = "";
		if(hour == 2008)
		{
			hourCon = "in (8,20)";
		}
		else
		{
			hourCon = "= " + hour;
		}
		String stationConStr = "and station in (" + stationsStr + ") ";
		String hourConStr =  "and hour " + hourCon;
		dataSource = dataSource.replace("_org", "");
		
		Calendar calendar = Calendar.getInstance();
		Date startDate = TimeUtil.String2Date(startDateStr.split(" ")[0], TimeUtil.DEFAULT_DATE_FORMAT);
		Date endDate = TimeUtil.String2Date(endDateStr.split(" ")[0], TimeUtil.DEFAULT_DATE_FORMAT);
		Map<String, List<Date>> map = new LinkedHashMap<>(); 
		for(int i = 0;; i++)
        {
            calendar.setTime(startDate);
            calendar.add(Calendar.DATE, i);
            int lastDayOfMonth = DateUtil.getLastDayOfMonth(calendar.getTime());
//            int month = calendar.get(Calendar.MONTH) + 1;
            String monthStr = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATE_FORMAT);
            monthStr = monthStr.substring(0, 7);
            if(calendar.getTime().after(endDate))
            {
                break;
            }
            if(!map.containsKey(monthStr))
            {
            	map.put(monthStr, new ArrayList<>());
            }
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            if(day == 1 || day == lastDayOfMonth)
            {
            	map.get(monthStr).add(calendar.getTime());
            }
        }
		List<String> monthSqlList = new ArrayList<>();
		List<String> daySqlList = new ArrayList<>();
		for(String key : map.keySet())
		{
			if(map.get(key).size() == 2)
			{
				monthSqlList.add(key);
				System.out.println(key);
			}
			else if(map.get(key).size() == 0)
			{
				continue;
			}
			else
			{
				daySqlList.add(TimeUtil.date2String(map.get(key).get(0), TimeUtil.DEFAULT_DATE_FORMAT));
				System.out.println(TimeUtil.date2String(map.get(key).get(0), TimeUtil.DEFAULT_DATE_FORMAT));
			}
		}
		
		int monthCount = monthSqlList.size();
		int dayCount = daySqlList.size();
		String sql = null;
		if(monthCount > 0)
		{
			tableName = "public.station_check_value_month_tab";
			sql = "select station,datatime,datasource," + fieldsName + " from " + tableName + " where datatime >='" + 
				   monthSqlList.get(0) + "' and datatime <= '" + monthSqlList.get(monthSqlList.size() - 1) + "' " + stationConStr + " and datasource = '"+ 
				   dataSource + "' " + hourConStr;
			result.put("month", sql);
		}
		if(dayCount > 0 && monthCount > 0)
		{
			tableName = "public.station_check_value_tab";
			if(dayCount == 2)
			{
				sql = "select station,datatime,datasource," + fieldsName + " from " + tableName + 
					  " where (datatime >='" + startDateStr.split(" ")[0] + "' and datatime <= '" + daySqlList.get(0) + "') or ("
					  + "datatime >='" + daySqlList.get(1) + "' and datatime <= '" + endDateStr.split(" ")[0] + "') "
					  + "" + stationConStr + " and datasource = '"+ dataSource + "' " + hourConStr;
				result.put("day", sql);
			}
			else if(dayCount == 1)
			{
				String startDates = startDateStr.split(" ")[0];
				String endDates = daySqlList.get(0).split(" ")[0];
				if(monthSqlList.get(monthCount - 1).compareTo(endDates.substring(0, 7)) < 0)
				{
					startDates = daySqlList.get(0).split(" ")[0];
					endDates = endDateStr.split(" ")[0];
				}
				
				sql = "select station,datatime,datasource," + fieldsName + " from " + tableName + " where datatime >='" + 
					   startDates + "' and datatime <= '" + endDates + "' " + stationConStr + " and datasource = '"+ 
					   dataSource + "' " + hourConStr;
				result.put("day", sql);
			}
		}
		if(dayCount == 0 && monthCount == 0)
		{
			tableName = "public.station_check_value_tab";
			sql = "select station,datatime,datasource," + fieldsName + " from " + tableName + " where datatime >='" + 
				   startDateStr.split(" ")[0] + "' and datatime <= '" + endDateStr.split(" ")[0] + "' " + stationConStr + " and datasource = '"+ 
				   dataSource + "' " + hourConStr;
				result.put("day", sql);
		}
		
		System.out.println(result);
	}
}
