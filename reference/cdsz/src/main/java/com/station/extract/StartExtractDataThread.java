package com.station.extract;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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

public class StartExtractDataThread implements Runnable{

	private Map<String, double[]> stationInfo;
//	private String[] fileDirs;
	private List<String> dataTypes;
	private Map<String, List<String>> filePathMapList;
	private Date startDate;
	private Date endDate;
    private ExecutorService executorService = ThreadPoolUtil.getInstance();
//    private static Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
	public StartExtractDataThread(Map<String, double[]> stationInfo, Map<String, List<String>> filePathMapList, Date startDate, Date endDate) {
		this.stationInfo = stationInfo;
//		this.fileDirs = fileDirs;
//		this.dataTypes = dataTypes;
		this.startDate = startDate;
		this.endDate = endDate;
		this.filePathMapList = filePathMapList;
	}
	
	@Override
	public void run() {
		
		CountDownLatch latch = new CountDownLatch(filePathMapList.size());
//		String[] fileDirs = new String[3];
//		String[] dataTypes = new String[3];
//		for(int i = 0, count = filePathMapList.size(); i < count; i++)
//		{
////			if(!FileUtil.exist(fileDirs[i]))
////			{
////				latch.countDown();
////				System.out.println(fileDirs[i] + " 文件不存在。");
////				continue;
////			}
//			executorService.execute(new ExtractDataThread(fileDirs[i], dataTypes[i], startDate, endDate, stationInfo, latch));
//			System.out.println(dataTypes[i] + " 抽取任务已开始...");
//		}
		dataTypes = new ArrayList<>();
		for(String dataType : filePathMapList.keySet())
		{
			dataTypes.add(dataType);
			executorService.execute(new ExtractDataThread(dataType, filePathMapList.get(dataType), stationInfo, latch));
			System.out.println(dataType + " 抽取任务已开始...");
		}
		
		try {
			latch.await();
			System.out.println("历史数据抽取完成。");
			
			if(filePathMapList.size() == 3)
			{
				System.out.println("开始计算新抽取的历史数据检验指标...");
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(new Date());
				calendar.add(Calendar.MONTH, -3);
//				Date startDate = calendar.getTime();
//				Date endDate = new Date();
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
				
//	        	JSONObject jsonStation = new JSONObject();
//				jsonStation.put("disVti", "24");
//				jsonStation.put("zone", "6");
//				jsonStation.put("dataSources", new String[]{"ecmf", "grapes", "swc9km"});
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
