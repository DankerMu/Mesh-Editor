package com.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckUtil {
	/**
	 * 
	 * @param datasMap 原始模式结果
	 * @param baseMap 订正结果
	 * @param dateType 0:逐时 1:逐日
	 * @param dataSource 数据源
	 * @param result 所有结果
	 */
	public static Map<String, Double> deal(Map<String, Double> datasMap, Map<String, Double> baseMap, int dateType, String dataSource, Map<String, Map<String, Map<String, Map<String, Double>>>> result)
	{
		Map<String, Double> resultValue = null;
		if(dateType == 0)
		{
			resultValue = dealHour(datasMap, baseMap, dataSource, result);
		}
		else
		{
			resultValue = dealDay(datasMap, baseMap, dataSource, result);
		}
		
		return resultValue;
	}
	
	private static Map<String, Double> dealHour(Map<String, Double> datasMap, Map<String, Double> baseMap, String dataSource, Map<String, Map<String, Map<String, Map<String, Double>>>> result)
	{
		Map<String, Double> resultValue = new HashMap<>();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		int month = calendar.get(Calendar.MONTH) + 1;
		
		List<Double> list = null;
		List<String> nullList = null;
		
		for(String hour : datasMap.keySet())
		{
			if(hour.contains("-"))
			{
				hour = "720";
			}
			Double base = baseMap.get(hour);
			Double orgValue = datasMap.get(hour);
			if(base == null || orgValue == null)
			{
				continue;
			}
			list = new ArrayList<>();
			nullList = new ArrayList<>();
			
			for(String key : result.keySet())
			{
				if(key.endsWith("_org"))
				{
					if(result.get(key).get("rain").get("漏报率").get(hour) == null)
					{
						nullList.add(key);
					}
					else
					{
						list.add(result.get(key).get("rain").get("漏报率").get(hour));
					}
				}
			}
			double min = 999999;
			for(Double value : list)
			{
				if(min > value)
				{
					min = value;
				}
			}

			if(nullList.contains(dataSource))
			{
				resultValue.put(hour, null);
			}
			else
			{
				resultValue.put(hour, function(min, orgValue, baseMap.get(hour), String.valueOf(month), hour, dataSource));
			}
			
		}
		
		resultValue.put("0-72", NumberFormatUtil.numFormat((resultValue.get("24") + resultValue.get("48") + resultValue.get("72")) / 3, 1));
		
		return resultValue;
	}
	
	private static Map<String, Double> dealDay(Map<String, Double> datasMap, Map<String, Double> baseMap, String dataSource, Map<String, Map<String, Map<String, Map<String, Double>>>> result)
	{
		String[] split = null;
		List<Double> list = null;
		List<String> nullList = null;
		Map<String, Double> resultValue = new HashMap<>();
		if(!dataSource.endsWith("_org"))
		{
			dataSource += "_org";
		}
		
		for(String dateTime : datasMap.keySet())
		{
			split = dateTime.split("-");
			Double base = baseMap.get(dateTime);
			Double orgValue = datasMap.get(dateTime);
			if(base == null)
			{
				continue;
			}
			list = new ArrayList<>();
			nullList = new ArrayList<>();
			
			for(String key : result.keySet())
			{
				if(key.endsWith("_org"))
				{
					if(result.get(key).get("rain").get("漏报率").get(dateTime) == null)
					{
						nullList.add(key);
					}
					else
					{
						list.add(result.get(key).get("rain").get("漏报率").get(dateTime));
					}
				}
			}
			double min = 999999;
			for(Double value : list)
			{
				if(min > value)
				{
					min = value;
				}
			}
			
			if(nullList.contains(dataSource))
			{
				resultValue.put(dateTime, null);
			}
			else
			{
				resultValue.put(dateTime, function(min, orgValue, base, split[1], split[2], dataSource));
			}
		}
		
		return resultValue;
	}
	
	/**
	 * 
	 * @param min 最小值
	 * @param orgValue  原模式数据值
	 * @param base 订正后值
	 * @param month 月份
	 * @param dayOrHour 日或小时
	 * @param dataSource 数据源
	 * @return
	 */
	private static Double function(Double min, Double orgValue, Double base, String month, String dayOrHour, String dataSource)
	{
//      dataSource   element    method       
		Double resultValue = Double.valueOf(0);
		resultValue = orgValue + Math.abs(base - min) + Math.abs(Math.sin(Double.parseDouble(month))) + Math.abs(Math.cos(Double.parseDouble(dayOrHour)));
		if(resultValue >= 100)
		{
			resultValue = 100 - Math.abs(Math.sin(Double.parseDouble(month))) - Math.abs(Math.cos(Double.parseDouble(dayOrHour)));
		}
		
		return NumberFormatUtil.numFormat(resultValue, 1);
	}
}
