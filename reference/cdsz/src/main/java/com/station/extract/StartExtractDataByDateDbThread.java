package com.station.extract;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import com.alibaba.fastjson2.JSONObject;
import com.check.pojo.CheckDataParams;
import com.constants.DataTypeEnum;
import com.model.task.HttpTool;
import com.util.ReadPropertiesUtil;
import com.util.ThreadPoolUtil;
import com.util.TimeUtil;

public class StartExtractDataByDateDbThread implements Runnable{

	private Map<String, double[]> stationInfo;
	private List<String> dataTypes;
//	            dataType     date
//	private Map<String, Map<String, List<String>>> filePathMapList;
	private Map<String, Map<String, Set<String>>> dateFilePathStationMap;
	private Date startDate;
	private Date endDate;
    private ExecutorService executorService = ThreadPoolUtil.getInstance();
    private int count;
	public StartExtractDataByDateDbThread(Map<String, double[]> stationInfo, Map<String, Map<String, Set<String>>> dateFilePathStationMap, Date startDate, Date endDate, int count) {
		this.stationInfo = stationInfo;
		this.startDate = startDate;
		this.endDate = endDate;
//		this.filePathMapList = filePathMapList;
		this.dateFilePathStationMap = dateFilePathStationMap;
	}
	
	@Override
	public void run() {
		
		CountDownLatch latch = new CountDownLatch(count);
		dataTypes = new ArrayList<>();
		
//		for(String dataType : filePathMapList.keySet())
//		{
//			for(String date : filePathMapList.get(dataType).keySet())
//			{
//				if(!map.containsKey(date))
//				{
//					map.put(date, new ArrayList<>());
//				}
//				map.get(date).addAll(filePathMapList.get(dataType).get(date));
//			}
//		}
		
//		Set<String> dateSet = new HashSet<>();
//		for(String dataType : filePathMapList.keySet())
//		{
//			for(String date : filePathMapList.get(dataType).keySet())
//			{
//				dateSet.add(date);
//			}
//		}
//		     date       filepath
//		Map<String, List<String>> map = new HashMap<>();
//		for(String date : dateSet)
//		{
//			for(String dataType : filePathMapList.keySet())
//			{
//				List<String> list = filePathMapList.get(dataType).get(date);
//				if(list == null)
//				{
//					continue;
//				}
//				if(!map.containsKey(date))
//				{
//					map.put(date, new ArrayList<>());
//				}
//				map.get(date).addAll(filePathMapList.get(dataType).get(date));
//			}
//		}
		
//           date      filePath     station
//   	Map<String, Map<String, List<String>>> dateFilePathStationMap = new HashMap<>();
		
		int f = dateFilePathStationMap.keySet().size() % 10;
		int threadCount = f == 0 ? f : f + 1;
		latch = new CountDownLatch(threadCount);
		
		int ii = 0;
		Map<String, Set<String>> filePathStationMap = new HashMap<>();
		for(String date : dateFilePathStationMap.keySet())
		{
			if(ii != 0 && ii % 10 == 0)
			{
				executorService.execute(new ExtractDataByDateDbThread(filePathStationMap, stationInfo, latch));
				filePathStationMap = new HashMap<>();
				filePathStationMap.putAll(dateFilePathStationMap.get(date));
			}
			else
			{
				filePathStationMap.putAll(dateFilePathStationMap.get(date));
			}
			
			ii++;
		}
		executorService.execute(new ExtractDataByDateDbThread(filePathStationMap, stationInfo, latch));
		
		try {
			latch.await();
			System.out.println("历史数据抽取完成。");
			
//			if(filePathMapList.size() == 3)
			{
				System.out.println("开始计算新抽取的历史数据检验指标...");
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(new Date());
				calendar.add(Calendar.MONTH, -3);
				List<String> dateList = new ArrayList<>();
				for(int i = 0;; i++)
				{
					calendar.setTime(startDate);
					calendar.add(Calendar.DATE, i);
					if(calendar.getTime().after(endDate))
					{
						break;
					}
					dateList.add(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATE_FORMAT) + " 08:00:00");
					dateList.add(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATE_FORMAT) + " 20:00:00");
				}
				CheckDataParams param = new CheckDataParams();
				Set<String> set = stationInfo.keySet();
				String[] stations = new String[set.size()];
				int i = 0;
				for(String station : set)
				{
					stations[i] = station;
					i++;
				}
				param.setStations(stations);
				param.setElements(new String[]{"atmax","atmin","at","ws"});
				param.setDisVti(24);
				List<String> dataSourceList = new ArrayList<>();
				for(String dataSource : dataTypes)
				{
					if(dataSource.equals(DataTypeEnum.CLDAS.getDataType()))
					{
						continue;
					}
					dataSourceList.add(dataSource);
				}
				String[] dataSources = new String[dataSourceList.size()];
				for(int j = 0, count = dataSources.length; j < count; j++)
				{
					dataSources[j] = dataSourceList.get(j);
				}
				param.setDataSources(dataSources);
				
				Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
				String url = configMap.get("check_station_url");
				
				for(String date : dateList)
				{
					param.setStartValidDate(date);
					param.setEndValidDate(date);
					HttpTool.doPost(url, JSONObject.toJSONString(param));
				}
				System.out.println("新抽取的历史数据检验指标计算完成。");
			}
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			
		}
	}

	
}
