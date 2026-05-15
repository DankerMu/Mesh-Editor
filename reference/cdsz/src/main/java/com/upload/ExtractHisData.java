package com.upload;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.constants.DataTypeEnum;
import com.station.extract.StartExtractDataThread;
import com.util.ReadPropertiesUtil;
import com.util.TimeUtil;

@RestController
@RequestMapping("/extract")
public class ExtractHisData {
	
	private ExecutorService executor = Executors.newSingleThreadExecutor();
	@PostMapping("/extractData")
	public void extractData(@RequestBody ExtractHisDataParam params)
	{
		Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
        String[] fileDirs = new String[2];
		String[] dataTypes = new String[2];
		fileDirs[0] = configMap.get("ecmf_his_path");
		fileDirs[1] = configMap.get("grapes_his_path");
//		fileDirs[2] = configMap.get("swc9km_his_path");
		dataTypes[0] = DataTypeEnum.ECMF.getDataType();
		dataTypes[1] = DataTypeEnum.GRAPES.getDataType();
//		dataTypes[2] = DataTypeEnum.SWC9KM.getDataType();
		Map<String, String> sufMap = new HashMap<>();
		sufMap.put(dataTypes[0], "grib");
		sufMap.put(dataTypes[1], "grib2");
//		sufMap.put(dataTypes[2], "grb");
		Map<String, double[]> stationInfo = ReadPropertiesUtil.getStationInfoConfigMap("add_station.properties");
		
		Date startDate = TimeUtil.String2Date(params.getStartDate(), TimeUtil.DEFAULT_DATETIME_FORMAT);
		Date endDate = TimeUtil.String2Date(params.getEndDate(), TimeUtil.DEFAULT_DATETIME_FORMAT);
		
		Calendar calendar = Calendar.getInstance();
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
        for(String dataType : dataTypes)
        {
        	filePathMapList.put(dataType, new ArrayList<>());
        }
        for(int i = 0, num = fileDirs.length; i < num; i++)
        {
        	for(String date : dateList)
			{
        		File file = new File(fileDirs[i] + File.separator + date);
        		String suffix = sufMap.get(dataTypes[i]);
        		String[] list = file.list(new FilenameFilter() {
					
					@Override
					public boolean accept(File dir, String name) {
						
						return name.endsWith(suffix);
					}
				});
        		for(String str : list)
        		{
        			filePathMapList.get(dataTypes[i]).add(str);
        		}
			}
        }
        
        
        
//        List<String> filePathList = new ArrayList<>();
//        List<String> dataTypeList = new ArrayList<>();
//		for(int i = 0, num = fileDirs.length; i < num; i++)
//		{
//			for(String date : dateList)
//			{
//				filePathList.add(fileDirs[i] + File.separator + date);
//				dataTypeList.add(dataTypes[i]);
//			}
//		}
//		
//		String[] filePathes = new String[filePathList.size()];
//		String[] dataType = new String[dataTypeList.size()];
//		for(int i = 0, num = filePathList.size(); i < num; i++)
//		{
//			filePathes[i] = filePathList.get(i);
//			dataType[i] = dataTypeList.get(i);
//		}
		
//		StartExtractDataThread extractData = new StartExtractDataThread(stationInfo, filePathMapList, dataTypes, startDate, endDate);
		StartExtractDataThread extractData = new StartExtractDataThread(stationInfo, filePathMapList, startDate, endDate);
		executor.execute(extractData);
		System.out.println("抽取任务已开始......");
	}
}
