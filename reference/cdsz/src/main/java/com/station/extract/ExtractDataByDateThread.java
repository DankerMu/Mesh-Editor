package com.station.extract;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import com.station.indb.DataIndbWork;
import com.util.DataTypeUtil;
import com.util.ReadPropertiesUtil;

public class ExtractDataByDateThread implements Runnable{

	private List<String> filePathMapList;
	private Map<String, double[]> stationInfo;
	private CountDownLatch latch;
	private Map<String, String> tableNameMap = ReadPropertiesUtil.getUserConfigMap("data_table.properties");
	public ExtractDataByDateThread(List<String> filePathMapList, Map<String, double[]> stationInfo, CountDownLatch latch) {
		this.stationInfo = stationInfo;
		this.filePathMapList = filePathMapList;
		this.latch = latch;
	}
	
	@Override
	public void run() {
		String dataType = null;
		String tableName = null;
		try {
			for(String filePath : filePathMapList)
			{
				dataType = DataTypeUtil.getDataType(filePath);
				tableName = tableNameMap.get(dataType);
				System.out.println(filePath + " 文件开始处理...");
				DataIndbWork.indb(filePath, dataType, tableName, stationInfo);
				if(filePath.contains("share-files"))
				{
					ExtractFileRecordIndbUtil.updateFileIndb1(filePath, dataType, tableName, stationInfo);
				}
				else
				{
					ExtractFileRecordIndbUtil.updateFileIndb(filePath, dataType, tableName, stationInfo);
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
