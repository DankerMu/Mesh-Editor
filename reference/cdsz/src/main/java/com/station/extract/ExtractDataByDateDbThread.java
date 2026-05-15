package com.station.extract;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import com.station.indb.DataIndbWork;
import com.util.DataTypeUtil;
import com.util.ReadPropertiesUtil;

public class ExtractDataByDateDbThread implements Runnable{

//	private List<String> filePathMapList;
//	            filePath    stations
	private Map<String, Set<String>> filePathStationMap;
	private Map<String, double[]> stationInfo;
	private CountDownLatch latch;
	private Map<String, String> tableNameMap = ReadPropertiesUtil.getUserConfigMap("data_table.properties");
	public ExtractDataByDateDbThread(Map<String, Set<String>> filePathStationMap, Map<String, double[]> stationInfo, CountDownLatch latch) {
		this.stationInfo = stationInfo;
		this.filePathStationMap = filePathStationMap;
		this.latch = latch;
	}
	
	@Override
	public void run() {
		String dataType = null;
		String tableName = null;
		Map<String, double[]> stationLonlatMap = null;
		try {
			for(String filePath : filePathStationMap.keySet())
			{
				dataType = DataTypeUtil.getDataType(filePath);
				tableName = tableNameMap.get(dataType);
				System.out.println(filePath + " 文件开始处理...");
				
				stationLonlatMap = new HashMap<>();
				for(String station : filePathStationMap.get(filePath))
				{
					stationLonlatMap.put(station, stationInfo.get(station));
				}
				
				DataIndbWork.indb(filePath, dataType, tableName, stationLonlatMap);
				if(filePath.contains("share-files"))
				{
					ExtractFileRecordIndbUtil.updateFileIndb1(filePath, dataType, tableName, stationLonlatMap);
				}
				else
				{
					ExtractFileRecordIndbUtil.updateFileIndb(filePath, dataType, tableName, stationLonlatMap);
				}
		    	System.out.println(filePath + " 文件处理完成。");
			}
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			latch.countDown();
		}
	}

}
