package com.upload;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import com.constants.DataTypeEnum;
import com.model.pojo.ModelManagerDetailEntity;
import com.station.dao.StationInfoMapper;
import com.station.extract.ExtractFileRecordIndbUtil;
import com.station.extract.StartExtractDataByDateDbThread;
import com.station.pojo.StationInfoEntity;
import com.util.GribUtil;
import com.util.ReadPropertiesUtil;
import com.util.ThreadPoolUtil;
import com.util.TimeUtil;

public class ExtractUploadStationHisDataThread implements Runnable{

	private StationInfoMapper stationInfoMapper;
	private List<StationInfoEntity> stationList;
	private Map<String, List<String>> dateListMap;
	public ExtractUploadStationHisDataThread(StationInfoMapper stationInfoMapper, List<StationInfoEntity> stationList, Map<String, List<String>> dateListMap) {
		this.stationInfoMapper = stationInfoMapper;
		this.stationList = stationList;
		this.dateListMap = dateListMap;
	}
	
	@Override
	public void run() {
		Map<String, StationInfoEntity> stationMap = new HashMap<>();
		for(StationInfoEntity data : stationList)
		{
			stationMap.put(data.getStationIdC(), data);
		}
		StationInfoEntity station = null;
		Calendar calendar = Calendar.getInstance();
		Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
		for(String key : stationMap.keySet())
		{
			station = stationMap.get(key);
			//TODO 需要将站点信息入到自建站信息表中
			int addUploadStation = stationInfoMapper.addUploadStation(station);
			
			if(addUploadStation > 0)
			{
		        String[] fileDirs = new String[2];
				String[] dataTypes = new String[2];
				fileDirs[0] = configMap.get("ecmf_his_cut_path");
				fileDirs[1] = configMap.get("grapes_his_path");
				dataTypes[0] = DataTypeEnum.ECMF.getDataType();
				dataTypes[1] = DataTypeEnum.GRAPES.getDataType();
				Map<String, String> sufMap = new HashMap<>();
				sufMap.put(dataTypes[0], "grib");
				sufMap.put(dataTypes[1], "grib2");
				Map<String, double[]> stationInfo = new HashMap<>();
				stationInfo.put(station.getStationIdC(), new double[]{station.getLon(), station.getLat()});
				
				
				List<String> list2 = dateListMap.get(station.getStationIdC()).stream().sorted().collect(Collectors.toList());
//				Date startDate = TimeUtil.String2Date(list2.get(0), TimeUtil.DEFAULT_DATETIME_FORMAT);
//				Date endDate = TimeUtil.String2Date(list2.get(list2.size() - 1), TimeUtil.DEFAULT_DATETIME_FORMAT);
				
				Map<String, List<String>> filePathMapList = new HashMap<>();
				Map<String, Map<String, List<String>>> filePathTypeDateMap = new HashMap<>();
		        for(String dataType : dataTypes)
		        {
		        	filePathMapList.put(dataType, new ArrayList<>());
		        	filePathTypeDateMap.put(dataType, new HashMap<>());
		        }
		        
		        Set<String> dateSet = new HashSet<>();
		        for(String date : list2)
		        {
		        	dateSet.add(date);
		        }
//		        int dateCount = 0;
		        for(int i = 0, num = fileDirs.length; i < num; i++)
		        {
		        	for(String date : dateSet)
					{
				        date = date.split(" ")[0].replace("-", "");
		        		File file = new File(fileDirs[i] + File.separator + date);
		        		if(!file.exists())
		        		{
		        			continue;
		        		}
		        		System.out.println("抽取数据路径: " + file.getAbsolutePath());
		        		String suffix = sufMap.get(dataTypes[i]);
		        		File[] list1 = file.listFiles(new FilenameFilter() {
							
							@Override
							public boolean accept(File dir, String name) {
								
								return name.toLowerCase().endsWith(suffix);
							}
						});
		        		if(list1 == null)
		        		{
		        			continue;
		        		}
		        		if(!filePathTypeDateMap.get(dataTypes[i]).containsKey(date))
		        		{
		        			filePathTypeDateMap.get(dataTypes[i]).put(date, new ArrayList<>());
		        		}
		        		for(File str : list1)
		        		{
		        			filePathMapList.get(dataTypes[i]).add(str.getAbsolutePath());
		        			filePathTypeDateMap.get(dataTypes[i]).get(date).add(str.getAbsolutePath());
		        		}
//		        		dateCount++;
					}
		        }
				
		        
		        String[] fileDirs1 = new String[2];
				String[] dataTypes1 = new String[2];
				fileDirs1[0] = configMap.get("ecmf_path");
				fileDirs1[1] = configMap.get("grapes_path");
				dataTypes1[0] = DataTypeEnum.ECMF.getDataType();
				dataTypes1[1] = DataTypeEnum.GRAPES.getDataType();
				Map<String, List<String>> filePathMapList1 = new HashMap<>();
				Map<String, Map<String, List<String>>> filePathTypeDateMap1 = new HashMap<>();
		        for(String dataType : dataTypes1)
		        {
		        	filePathMapList1.put(dataType, new ArrayList<>());
		        	filePathTypeDateMap1.put(dataType, new HashMap<>());
		        }
		        
		        calendar.setTime(new Date());
//		        Date endDate1 = calendar.getTime();
		        calendar.add(Calendar.DAY_OF_MONTH, -2);
		        Date startDate1 = calendar.getTime();
		        String startDate1Str = TimeUtil.date2String(startDate1, TimeUtil.DEFAULT_DATE_FORMAT);
		        Set<String> dateSet1 = new HashSet<>();
		        for(int i = 0, num = fileDirs1.length; i < num; i++)
		        {
		        	File file = new File(fileDirs1[i]);
	        		if(!file.exists())
	        		{
	        			continue;
	        		}
	        		String dataType = dataTypes1[i];
	        		System.out.println("抽取数据路径1: " + file.getAbsolutePath());
	        		String suffix = sufMap.get(dataTypes1[i]);
	        		File[] list1 = file.listFiles(new FilenameFilter() {
						
						@Override
						public boolean accept(File dir, String name) {
							boolean endsWith = name.toLowerCase().endsWith(suffix);
							boolean result = false;
							if(endsWith)
							{
								String[] vtiDataTime = GribUtil.getVtiDataTime(name, dataType);
						        String dataTime = vtiDataTime[1];
						        if(dataTime.compareTo(startDate1Str) > 0)
						        {
						        	dateSet1.add(dataTime);
						        	result = true;
						        }
							}
							
							return result;
						}
					});
	        		if(list1 == null)
	        		{
	        			continue;
	        		}
	        		for(File str : list1)
	        		{
	        			filePathMapList1.get(dataTypes1[i]).add(str.getAbsolutePath());
	        			String[] vtiDataTime = GribUtil.getVtiDataTime(str.getName(), dataType);
				        String date = vtiDataTime[1];
	        			if(!filePathTypeDateMap1.get(dataTypes[i]).containsKey(date))
	            		{
	        				filePathTypeDateMap1.get(dataTypes[i]).put(date, new ArrayList<>());
	            		}
	        			filePathTypeDateMap1.get(dataTypes[i]).get(date).add(str.getAbsolutePath());
	        		}
		        }
		        
		        
		        ExtractFileRecordIndbUtil.fileIndb(stationInfo, filePathMapList);
		        ExtractFileRecordIndbUtil.fileIndb1(stationInfo, filePathMapList1);
				
				System.out.println(station.getStationIdC() + "站上传数据后，添加站点成功");
				addModel(station, 6);

			}
			else
			{
				System.out.println(station.getStationIdC() + "站上传数据后，添加站点失败");
			}
		}
		
//		StartExtractDataThread extractData = new StartExtractDataThread(stationInfo, filePathMapList, dataTypes, startDate, endDate);
//		StartExtractDataThread extractData = new StartExtractDataThread(stationInfo, filePathMapList, startDate, endDate);
//		StartExtractDataThread extractData1 = new StartExtractDataThread(stationInfo, filePathMapList1, startDate1, endDate1);
		
//		StartExtractDataByDateThread extractData = new StartExtractDataByDateThread(stationInfo, filePathTypeDateMap, startDate, endDate, dateCount);
//		StartExtractDataByDateThread extractData1 = new StartExtractDataByDateThread(stationInfo, filePathTypeDateMap1, startDate1, endDate1, dateSet1.size());
//		System.out.println("开始抽取已有站点历史数据...");
//		executor.execute(extractData);
//		executor.execute(extractData1);
//
//		addModel(station, 6);
		
		Map<String, double[]> stationInfo = new HashMap<>();
        Map<String, Date[]> startEndDate = new HashMap<>();
        Map<String, Map<String, Set<String>>> dateFilePathStationMap = new HashMap<>();
        ExtractFileRecordIndbUtil.queryFileIndb(stationInfo, startEndDate, dateFilePathStationMap);
		
        Date[] startEnd = null;
        ExecutorService executor = ThreadPoolUtil.getInstance();
        try {
        	startEnd = startEndDate.get("date");
        	if(startEnd != null)
        	{
        		StartExtractDataByDateDbThread extractData = new StartExtractDataByDateDbThread(stationInfo, dateFilePathStationMap, startEnd[0], startEnd[1], startEndDate.size());
        		executor.execute(extractData);
        	}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
    public int addModel(StationInfoEntity stationInfo, int managerId) {
        ModelManagerDetailEntity data = new ModelManagerDetailEntity();
        data.setStationNum(stationInfo.getStationIdD() + "");
        data.setStationName(stationInfo.getStationName());
        data.setLon(stationInfo.getLon());
        data.setLat(stationInfo.getLat());
        data.setAuthor(stationInfo.getAuthor());
//        data.setStatus(1);
        data.setStatus(2);//建模中
        data.setManagerId(managerId);
        data.setUsed(0);
        data.setEnabled(0);
        data.setModel("通用模型");
        int i = stationInfoMapper.addYzdModel(data);

        ModelManagerDetailEntity data1 = new ModelManagerDetailEntity();
        data1.setStationNum(stationInfo.getStationIdD() + "");
        data1.setStationName(stationInfo.getStationName());
        data1.setLon(stationInfo.getLon());
        data1.setLat(stationInfo.getLat());
        data1.setAuthor(stationInfo.getAuthor());
//        data1.setStatus(1);
        data.setStatus(2);//建模中
        data1.setManagerId(managerId);
        data1.setUsed(1);
        data1.setEnabled(0);
        data1.setModel("强拟合模型");
        int j = stationInfoMapper.addYzdModel(data1);
        int result = 0;
        if(i > 0 && j > 0)
        {
            result = 1;
        }

        return result;
    }

}
