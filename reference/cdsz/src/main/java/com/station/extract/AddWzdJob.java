package com.station.extract;

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

import com.constants.DataTypeEnum;
import com.util.GribUtil;
import com.util.ReadPropertiesUtil;
import com.util.ThreadPoolUtil;
import com.util.TimeUtil;

public class AddWzdJob implements Runnable{
	
	private Map<String, double[]> stationInfo;
	private ExecutorService executor = ThreadPoolUtil.getInstance();
	public AddWzdJob(Map<String, double[]> stationInfo) {
		this.stationInfo = stationInfo;
	}

	@Override
	public void run() {
		
		String[] fileDirs = new String[4];
		String[] dataTypes = new String[4];
		Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
		fileDirs[0] = configMap.get("ecmf_his_cut_path");
		fileDirs[1] = configMap.get("grapes_his_cut_path");
//		fileDirs[2] = configMap.get("swc9km_his_path");
		fileDirs[2] = configMap.get("cldas_his_path");
		fileDirs[3] = configMap.get("cldas_his_path");
//		fileDirs[0] = configMap.get("cldas_his_path");
//		fileDirs[1] = configMap.get("cldas_his_path");
		dataTypes[0] = DataTypeEnum.ECMF.getDataType();
		dataTypes[1] = DataTypeEnum.GRAPES.getDataType();
//		dataTypes[2] = DataTypeEnum.SWC9KM.getDataType();
		dataTypes[2] = DataTypeEnum.CLDAS.getDataType();
		dataTypes[3] = DataTypeEnum.CLDAS.getDataType() + "_p5";
//		dataTypes[0] = DataTypeEnum.CLDAS.getDataType() + "_p5";
//		dataTypes[1] = DataTypeEnum.CLDAS.getDataType();
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.MONTH, -3);
//		calendar.add(Calendar.DAY_OF_MONTH, -5);
		
		Map<String, String> sufMap = new HashMap<>();
		sufMap.put(dataTypes[0], "grib");
		sufMap.put(dataTypes[1], "grib2");
//		sufMap.put(dataTypes[2], "grb");
		sufMap.put(dataTypes[2], "nc");
		sufMap.put(dataTypes[3], "grb2");
//		sufMap.put(dataTypes[0], "grb2");
//		sufMap.put(dataTypes[1], "nc");
		Date startDate = calendar.getTime();
        Date endDate = new Date();
        List<String> dateList = new ArrayList<>();
        for(int i = 0;; i++)
        {
            calendar.setTime(startDate);
            calendar.add(Calendar.DATE, i);
            if(calendar.getTime().after(endDate))
            {
                break;
            }
            dateList.add(TimeUtil.date2String(calendar.getTime(), TimeUtil.DATE_FMT_YMD));
        }
        
        
        Map<String, List<String>> filePathMapList = new HashMap<>();
        Map<String, Map<String, List<String>>> filePathTypeDateMap = new HashMap<>();
        for(String dataType : dataTypes)
        {
        	filePathMapList.put(dataType, new ArrayList<>());
        	filePathTypeDateMap.put(dataType, new HashMap<>());
        }
        int dateCount = 0;
        for(int i = 0, num = fileDirs.length; i < num; i++)
        {
        	for(String date : dateList)
			{
        		File file = new File(fileDirs[i] + File.separator + date);
        		String[] ll = file.list();
        		if(!file.exists() || ll == null || ll.length == 0)
        		{
        			continue;
        		}
        		String suffix = sufMap.get(dataTypes[i]);
//        		System.out.println("file dir: " + file.getAbsolutePath());
        		File[] list = file.listFiles(new FilenameFilter() {
					
					@Override
					public boolean accept(File dir, String name) {
//						System.out.println("dir: " + dir + " name: " + name);
						if(name == null)
						{
							return false;
						}
						String lower = name.toLowerCase();
						
						return lower.endsWith(suffix);
					}
				});
        		if(list == null || list.length == 0)
        		{
        			System.out.println(dataTypes[i] + ":没符合条件的历史数据");
        			continue;
        		}
        		
        		if(!filePathTypeDateMap.get(dataTypes[i]).containsKey(date))
        		{
        			filePathTypeDateMap.get(dataTypes[i]).put(date, new ArrayList<>());
        		}
        		
        		for(File str : list)
        		{
        			filePathMapList.get(dataTypes[i]).add(str.getAbsolutePath());
        			filePathTypeDateMap.get(dataTypes[i]).get(date).add(str.getAbsolutePath());
        		}
        		dateCount++;
			}
        }
        
        extractRealData(configMap);
        
        ExtractFileRecordIndbUtil.fileIndb(stationInfo, filePathMapList);
        
        System.out.println("开始抽取历史数据......");
//        StartExtractDataThread extractData = new StartExtractDataThread(stationInfo, filePathMapList, calendar.getTime(), new Date());
//        executor.execute(extractData);
        StartExtractDataByDateThread extractData = new StartExtractDataByDateThread(stationInfo, filePathTypeDateMap, calendar.getTime(), new Date(), dateCount);
        executor.execute(extractData);
        System.out.println("路径文件生成完成。");
	}
	
	private void extractRealData(Map<String, String> configMap)
	{
		String[] fileDirs = new String[2];
		String[] dataTypes = new String[2];
		fileDirs[0] = configMap.get("ecmf_path");
		fileDirs[1] = configMap.get("grapes_path");
		dataTypes[0] = DataTypeEnum.ECMF.getDataType();
		dataTypes[1] = DataTypeEnum.GRAPES.getDataType();
		Map<String, String> sufMap = new HashMap<>();
		sufMap.put(dataTypes[0], "grib");
		sufMap.put(dataTypes[1], "grib2");
		Map<String, List<String>> filePathMapList = new HashMap<>();
        for(String dataType : dataTypes)
        {
        	filePathMapList.put(dataType, new ArrayList<>());
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        Date endDate = calendar.getTime();
        calendar.add(Calendar.DAY_OF_MONTH, -2);
        Date startDate1 = calendar.getTime();
        String startDate1Str = TimeUtil.date2String(startDate1, TimeUtil.DEFAULT_DATE_FORMAT);
        for(int i = 0, num = fileDirs.length; i < num; i++)
        {
        	File file = new File(fileDirs[i]);
    		if(!file.exists())
    		{
    			continue;
    		}
    		String dataType = dataTypes[i];
    		System.out.println("抽取实时数据路径: " + file.getAbsolutePath());
    		String suffix = sufMap.get(dataTypes[i]);
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
    			filePathMapList.get(dataTypes[i]).add(str.getAbsolutePath());
    		}
        }
        
        ExtractFileRecordIndbUtil.fileIndb1(stationInfo, filePathMapList);
        
        StartExtractDataThread extractData = new StartExtractDataThread(stationInfo, filePathMapList, startDate1, endDate);
        executor.execute(extractData);
	}
	
}
