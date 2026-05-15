package com.station.extract;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import com.station.indb.DataIndbWork;
import com.util.ReadPropertiesUtil;

public class ExtractDataThread implements Runnable{

//	private String fileDir;
	private String dataType;
	private List<String> filePathList;
//	private Date startDate;
//	private Date endDate;
	
	private Map<String, double[]> stationInfo;
	private CountDownLatch latch;
	private Map<String, String> tableNameMap = ReadPropertiesUtil.getUserConfigMap("data_table.properties");
	public ExtractDataThread(String dataType, List<String> filePathList, Map<String, double[]> stationInfo, CountDownLatch latch) {
//		this.fileDir = fileDir;
		this.dataType = dataType;
//		this.startDate = startDate;
//		this.endDate = endDate;
		this.stationInfo = stationInfo;
		this.latch = latch;
		this.filePathList = filePathList;
	}
	
	@Override
	public void run() {
//		Calendar calendar = Calendar.getInstance();
//		calendar.setTime(new Date());
//		calendar.add(Calendar.MONTH, -3);
//		calendar.add(Calendar.DAY_OF_MONTH, -1);
//		int dataStartTime = Integer.parseInt(TimeUtil.date2String(startDate, TimeUtil.DATE_FMT_YMDH));
//		int dataEndTime = Integer.parseInt(TimeUtil.date2String(endDate, TimeUtil.DATE_FMT_YMDH));
		
//		DirFileVisitor visitor = new DirFileVisitor(Paths.get(fileDir), dataType, dataStartTime, dataEndTime, stationInfo);
		String tableName = tableNameMap.get(dataType);
		
		try {
			
			for(String filePath : filePathList)
			{
				System.out.println(filePath + " 文件开始处理...");
//				FileUtil.appendString(filePath + " 文件开始处理..." + "\r\n", "/data/extract.txt", "utf-8");
				DataIndbWork.indb(filePath, dataType, tableName, stationInfo);
				if(filePath.contains("share-files"))
				{
					ExtractFileRecordIndbUtil.updateFileIndb1(filePath, dataType, tableName, stationInfo);
				}
				else
				{
					ExtractFileRecordIndbUtil.updateFileIndb(filePath, dataType, tableName, stationInfo);
				}
//		    	FileUtil.appendString(filePath + " 文件处理完成。" + "\r\n", "/data/extract.txt", "utf-8");
		    	System.out.println(filePath + " 文件处理完成。");
			}
			
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			latch.countDown();
		}
	}

}
