package com;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.model.task.StartUpgradeModelAtStartUtil;
import com.station.extract.ExtractFileRecordIndbUtil;
import com.station.extract.StartExtractDataByDateDbThread;
import com.util.ThreadPoolUtil;

/**
 * @category
 * @date 2025/3/13 13:38
 * @description TODO
 */
@SpringBootApplication
@MapperScan("com.*.dao")
public class CdszApplication {
//	public static String userName = "";
    public static void main(String[] args)
    {
        SpringApplication springApplication = new SpringApplication(CdszApplication.class);
        springApplication.run(args);
        System.out.println("已启动");
        Map<String, double[]> stationInfo = new HashMap<>();
//        Map<String, Map<String, List<String>>> filePathMapList = new HashMap<>(); 
        Map<String, Date[]> startEndDate = new HashMap<>();
        Map<String, Map<String, Set<String>>> dateFilePathStationMap = new HashMap<>();
        ExtractFileRecordIndbUtil.queryFileIndb(stationInfo, startEndDate, dateFilePathStationMap);
        
        Date[] startEnd = null;
        ExecutorService executor = ThreadPoolUtil.getInstance();
//        for(String station : filePathMapList.keySet())
//        {
//        	startEnd = startEndDate.get(station);
//        	StartExtractDataThread extractData = new StartExtractDataThread(stationInfo, filePathMapList.get(station), startEnd[0], startEnd[1]);
//        	executor.execute(extractData);
//        }
        try {
//        	StartExtractDataByDateThread extractData = null;
//            for(String station : filePathMapList.keySet())
//            {
//            	startEnd = startEndDate.get(station);
//            	extractData = new StartExtractDataByDateThread(stationInfo, filePathMapList, startEnd[0], startEnd[1], startEndDate.size());
//            	executor.execute(extractData);
//            }
        	startEnd = startEndDate.get("date");
        	if(startEnd != null)
        	{
        		StartExtractDataByDateDbThread extractData = new StartExtractDataByDateDbThread(stationInfo, dateFilePathStationMap, startEnd[0], startEnd[1], startEndDate.size());
        		executor.execute(extractData);
        	}
		} catch (Exception e) {
			e.printStackTrace();
		}
        
        
        StartUpgradeModelAtStartUtil.start();
    }
}
