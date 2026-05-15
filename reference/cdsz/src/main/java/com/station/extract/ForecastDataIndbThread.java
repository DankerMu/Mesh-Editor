package com.station.extract;

import java.util.List;
import java.util.Map;

import com.station.indb.DataIndbWork;
import com.util.ReadPropertiesUtil;

public class ForecastDataIndbThread implements Runnable{

	private List<String> filePathList;
	private String dataType;
	private Map<String, double[]> stationLonlats;
	public ForecastDataIndbThread(List<String> filePathList, String dataType, Map<String, double[]> stationLonlats) {
		this.filePathList = filePathList;
		this.dataType = dataType;
		this.stationLonlats = stationLonlats;
	}
	
	@Override
	public void run() {
		Map<String, String> dataTableMap = ReadPropertiesUtil.getUserConfigMap("data_table.properties");
		
		for(String filePath : filePathList)
		{
//			System.out.println(filePath);
			DataIndbWork.indb(filePath, dataType, dataTableMap.get(dataType), stationLonlats);
		}
	}

}
