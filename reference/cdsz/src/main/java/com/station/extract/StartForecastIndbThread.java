package com.station.extract;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import com.constants.DataTypeEnum;
import com.station.indb.util.QueryStationsInfoFromDBUtil;
import com.util.ReadPropertiesUtil;
import com.util.ThreadPoolUtil;

public class StartForecastIndbThread implements Runnable{

	private String dataType;
	private String dataTime;
	private static ExecutorService executorService = ThreadPoolUtil.getInstance();
	private static String[] vtis = new String[]{"000", "003", "006", "009", "012", "015", "018", "021", "024", "027", "030",
												"033", "036", "039", "042", "045", "048", "051", "054", "057", "060", "063", 
												"066", "069", "072", "078", "084", "090", "096", "102", "108", "114", "120", 
												"126", "132", "138", "144", "150", "156", "162", "168", "174", "180", "186", 
												"192", "198", "204", "210", "216", "222", "228", "234", "240"};
	public StartForecastIndbThread(String dataType, String dataTime) {
		this.dataType = dataType;
		this.dataTime = dataTime;
	}
	
	@Override
	public void run() {
		System.out.println("python start .....................");
		Map<String, double[]> stationLonlats = QueryStationsInfoFromDBUtil.queryAllStationsInfo();
		Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config_indb.properties");
		String dateStr = dataTime.substring(0, 8);
		List<String> filePathList = new ArrayList<>();
		String basePath = configMap.get(dataType + "_path");
		if(dataType.equals(DataTypeEnum.STATION.getDataType()))
		{
			for(String station : stationLonlats.keySet())
			{
				filePathList.add(basePath + station + File.separator + station + "-" + dataTime + ".csv");
			}
		}
		else if(dataType.equals(DataTypeEnum.MICAPS.getDataType()))
		{
//			filePathList.add(basePath + dateStr + File.separator + dataTime);
			filePathList.add(dataTime);
		}
		else if(dataType.equals(DataTypeEnum.FY4B.getDataType()))
		{
//			filePathList.add(basePath + dateStr + File.separator + dataTime);
			filePathList.add(dataTime);
		}
		else
		{
			String header = configMap.get(dataType + "_head");
			dataType = dataType + "_rain";
			for(String vti : vtis)
			{
				filePathList.add(basePath + dateStr + File.separator + header + dataTime + "_" + vti + ".txt");
			}
		}
		executorService.execute(new ForecastDataIndbThread(filePathList, dataType, stationLonlats));
	}
	
}
