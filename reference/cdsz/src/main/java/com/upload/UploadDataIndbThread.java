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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import cn.hutool.core.io.FileUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;

import com.alibaba.fastjson2.JSONObject;
import com.constants.DataTypeEnum;
import com.model.pojo.ModelManagerDetailEntity;
import com.obs.dao.ObsDataMapper;
import com.station.dao.StationInfoMapper;
import com.station.extract.ExtractFileRecordIndbUtil;
import com.station.extract.StartExtractDataByDateThread;
import com.station.extract.StartExtractDataThread;
import com.station.pojo.StationInfoEntity;
import com.util.GribUtil;
import com.util.ReadPropertiesUtil;
import com.util.ReflectUtil;
import com.util.ThreadPoolUtil;
import com.util.TimeUtil;

public class UploadDataIndbThread implements Runnable{
//	站点名称	观测时间	气温	本站气压	相对湿度	二分钟平均风向	二分钟平均风速	极大风向	极大风速	小时降水量/翻斗式 	最低气温	最高气温	经度	纬度	能见度	总云量
//	站点名称	观测时间	气温	本站气压	相对湿度	二分钟平均风向	二分钟平均风速	极大风向	极大风速	分钟降水量/翻斗式		小时降水量/翻斗式	   	最低气温	最高气温

	private static Map<String, String> headerMap = new HashMap<>();
	private static Map<String, String> transMap = new HashMap<>();
	private static Map<String, String> indexMap = new HashMap<>();
	private static Map<String, double[]> lonlatsMap = new HashMap<>();
	
	static
	{
		headerMap.put("站点名称", "station_name");
		headerMap.put("观测时间", "datetime");
		headerMap.put("经度", "lon");
		headerMap.put("纬度", "lat");
		headerMap.put("气温", "tem");
		headerMap.put("本站气压", "prs");
		headerMap.put("相对湿度", "rhu");
		headerMap.put("二分钟平均风向", "win_d_avg_10mi");
		headerMap.put("二分钟平均风速", "win_s_avg_10mi");
		headerMap.put("极大风向", "win_d_max");
		headerMap.put("极大风速", "win_s_max");
		headerMap.put("小时降水量/翻斗式", "pre");
		headerMap.put("最低气温", "tem_min_24h");
		headerMap.put("最高气温", "tem_max_24h");
		headerMap.put("能见度", "vis");
		headerMap.put("总云量", "tcc");
		
		
		transMap.put("station_name", "stationName");
		transMap.put("datetime", "dateTime");
		transMap.put("lon", "lon");
		transMap.put("lat", "lat");
		transMap.put("tem", "tem");
		transMap.put("prs", "prs");
		transMap.put("rhu", "rhu");
		transMap.put("win_d_avg_10mi", "wd10mi");
		transMap.put("win_s_avg_10mi", "ws10mi");
		transMap.put("win_d_max", "wdMax");
		transMap.put("win_s_max", "wsMax");
		transMap.put("pre", "pre");
		transMap.put("tem_min_24h", "temMin");
		transMap.put("tem_max_24h", "temMax");
		transMap.put("tcc", "tcc");
		transMap.put("vis", "vis");
		
		lonlatsMap.put("五二四三（天文点）", new double[]{78.2883, 35.3069});
		lonlatsMap.put("加勒万河谷", new double[]{78.2456, 34.7506});
		lonlatsMap.put("噶尔县扎西岗", new double[]{79.6739, 32.5272});
		lonlatsMap.put("空喀山口_自组网", new double[]{79.1108, 34.3295});
		lonlatsMap.put("库尔那克堡", new double[]{78.985, 33.7517});
		lonlatsMap.put("拉马斯", new double[]{88.9336, 27.2996});
		lonlatsMap.put("墨脱", new double[]{95.3566, 29.2863});
		
//		天文点	78.2883	35.3068
//		加勒万	78.2456	34.7506
//		空喀山口	79.1333	34.34109
//		库尔那克堡	78.985	33.7517
//		扎西岗	79.6739	32.5272
//		岗巴	88.6083	28.1528
//		日挺布	91.7183	27.7932
//		陇	93.046	28.382
//		墨脱-自建站	95.3578	29.2878

		lonlatsMap.put("岗巴", new double[]{88.6083,28.1528});
		lonlatsMap.put("加勒万河谷", new double[]{78.2456, 34.7506});
		lonlatsMap.put("皮山县空喀山口", new double[]{79.1333, 34.34109});
		lonlatsMap.put("库尔那克堡", new double[]{78.985, 33.7517});
		lonlatsMap.put("陇", new double[]{93.046, 28.382});
		lonlatsMap.put("墨脱", new double[]{95.3578, 29.2878});
		lonlatsMap.put("日挺布", new double[]{91.7183, 27.7932});
		lonlatsMap.put("五二四三（天文点）", new double[]{78.2883, 35.3068});
		lonlatsMap.put("噶尔县扎西岗", new double[]{79.6739, 32.5272});
	}
	private String filePath;
	private int index;
	private ObsDataMapper obsDataMapper;
	private StationInfoMapper stationInfoMapper;
	private Map<String, String> uploadStationMap;
//	private ExecutorService executor = ThreadPoolUtil.getInstance();
	private List<StationInfoEntity> stationList;
	private Map<String, List<String>> dateListMap;
	private CountDownLatch latch;
	public UploadDataIndbThread(String filePath, Map<String, String> uploadStationMap, int index, ObsDataMapper obsDataMapper, 
			StationInfoMapper stationInfoMapper, List<StationInfoEntity> stationList, Map<String, List<String>> dateListMap, CountDownLatch latch) {
		this.filePath = filePath;
		this.uploadStationMap = uploadStationMap;
		this.index = index;
		this.obsDataMapper = obsDataMapper;
		this.stationInfoMapper = stationInfoMapper;
		this.stationList = stationList;
		this.dateListMap = dateListMap;
		this.latch = latch;
	}
	
	@Override
	public void run() {
		ExcelReader reader = ExcelUtil.getReader(filePath);
		List<Sheet> sheets = reader.getSheets();
		UploadStationData data = null;
		List<UploadStationData> list = new ArrayList<>();
		List<String> dateList = new ArrayList<>();
		Set<String> dateSet = new HashSet<>();
		Calendar calendar = Calendar.getInstance();
		StationInfoEntity station = stationInfoMapper.queryMaxUploadStationNum();
		int stationNum = 0;
		if(station == null)
        {
        	stationNum = 981000;
        }
		stationNum = station.getMaxNum() + index;
        
		for(Sheet sheet : sheets)
		{
			int firstRowNum = sheet.getFirstRowNum();
			int lastRowNum = sheet.getLastRowNum();
//			System.out.println("lastRowNum: " + lastRowNum);
			Row firstRow = sheet.getRow(firstRowNum);
			short firstCellNum = firstRow.getFirstCellNum();
			short lastCellNum = firstRow.getLastCellNum();
			for(int j = firstCellNum; j < lastCellNum; j++)
			{
				Cell cell = firstRow.getCell(j);
				String cellValue = cell.getStringCellValue();
				indexMap.put(j + "", headerMap.get(cellValue));
			}
			for(int i = firstRowNum + 1; i < lastRowNum; i++)
			{
				Row row = sheet.getRow(i);
				if(row == null)
				{
					continue;
				}
				firstCellNum = row.getFirstCellNum();
				lastCellNum = row.getLastCellNum();
				data = new UploadStationData();
				data.setFilePath(filePath);
				data.setFileName(FileUtil.getName(filePath));
				data.setInsertTime(TimeUtil.date2String(new Date(), TimeUtil.DEFAULT_DATETIME_FORMAT));
				for(int j = firstCellNum; j < lastCellNum; j++)
				{
					Cell cell = row.getCell(j);
					String cellValue = cell.getStringCellValue();
					if(cellValue == null || cellValue.length() == 0)
					{
						continue;
					}
//					System.out.println("j:" + j + " " + indexMap.get(j + ""));
//					System.out.println("indexMap.get(j):" + indexMap.get(j + "") + " " + transMap.get(indexMap.get(j + "")));
					ReflectUtil.setFieldValueAutoByType(data, cellValue, transMap.get(indexMap.get(j + "")));
				}
				if(firstRowNum + 1 == i)
				{
					System.out.println(JSONObject.toJSONString(uploadStationMap));
					System.out.println(data.getStationName());
					if(uploadStationMap.containsKey(data.getStationName()))
					{
						stationNum = Integer.parseInt(uploadStationMap.get(data.getStationName()));
					}
				}
				data.setStationNum(stationNum);
				
				if(data.getLon() == null)
				{
					data.setLon(lonlatsMap.get(data.getStationName())[0]);
					data.setLat(lonlatsMap.get(data.getStationName())[1]);
				}
				
				calendar.setTime(TimeUtil.dateTimeStr2date(data.getDateTime(), TimeUtil.DEFAULT_DATETIME_FORMAT, TimeUtil.DEFAULT_DATETIME_FORMAT));
				calendar.add(Calendar.HOUR_OF_DAY, -8);
				String dateTime = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT);
				data.setDateTime(dateTime);
				
				dateList.add(data.getDateTime());
				dateSet.add(data.getDateTime().split(" ")[0].replace("-", ""));
				list.add(data);
			}
			dateListMap.put(stationNum + "", dateList);
		}
		
		for(UploadStationData value : list)
		{
			obsDataMapper.addUploadStationData(value);
		}
		try {
			station = new StationInfoEntity();
			station.setStationIdD(stationNum);
			station.setStationIdC(stationNum + "");
			UploadStationData stationData = list.get(0);
			station.setStationName(stationData.getStationName());
			station.setLon(stationData.getLon());
			station.setLat(list.get(0).getLat());
			station.setEnabled(0);
			stationList.add(station);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			latch.countDown();
		}
		
	
//		----------------------------------------------------------分界线---------------------------------------------------------------------------------
		
		//TODO 需要将站点信息入到自建站信息表中
//		int addUploadStation = stationInfoMapper.addUploadStation(station);
//		
//		if(addUploadStation > 0)
//		{
//	        Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
//	        String[] fileDirs = new String[2];
//			String[] dataTypes = new String[2];
//			fileDirs[0] = configMap.get("ecmf_his_path");
//			fileDirs[1] = configMap.get("grapes_his_path");
////			fileDirs[2] = configMap.get("swc9km_his_path");
//			dataTypes[0] = DataTypeEnum.ECMF.getDataType();
//			dataTypes[1] = DataTypeEnum.GRAPES.getDataType();
////			dataTypes[2] = DataTypeEnum.SWC9KM.getDataType();
//			Map<String, String> sufMap = new HashMap<>();
//			sufMap.put(dataTypes[0], "grib");
//			sufMap.put(dataTypes[1], "grib2");
////			sufMap.put(dataTypes[2], "grb");
//			Map<String, double[]> stationInfo = new HashMap<>();
//			stationInfo.put(station.getStationIdC(), new double[]{station.getLon(), station.getLat()});
//			
//			
//			List<String> list2 = dateList.stream().sorted().collect(Collectors.toList());
//			Date startDate = TimeUtil.String2Date(list2.get(0), TimeUtil.DEFAULT_DATETIME_FORMAT);
//			Date endDate = TimeUtil.String2Date(list2.get(list2.size() - 1), TimeUtil.DEFAULT_DATETIME_FORMAT);
//			
//			Map<String, List<String>> filePathMapList = new HashMap<>();
//			Map<String, Map<String, List<String>>> filePathTypeDateMap = new HashMap<>();
//	        for(String dataType : dataTypes)
//	        {
//	        	filePathMapList.put(dataType, new ArrayList<>());
//	        	filePathTypeDateMap.put(dataType, new HashMap<>());
//	        }
//	        int dateCount = 0;
//	        for(int i = 0, num = fileDirs.length; i < num; i++)
//	        {
//	        	for(String date : dateSet)
//				{
////	        		date = date.split(" ")[0].replace("-", "");
//	        		File file = new File(fileDirs[i] + File.separator + date);
//	        		if(!file.exists())
//	        		{
//	        			continue;
//	        		}
//	        		System.out.println("抽取数据路径: " + file.getAbsolutePath());
//	        		String suffix = sufMap.get(dataTypes[i]);
//	        		File[] list1 = file.listFiles(new FilenameFilter() {
//						
//						@Override
//						public boolean accept(File dir, String name) {
//							
//							return name.toLowerCase().endsWith(suffix);
//						}
//					});
//	        		if(list1 == null)
//	        		{
//	        			continue;
//	        		}
//	        		if(!filePathTypeDateMap.get(dataTypes[i]).containsKey(date))
//	        		{
//	        			filePathTypeDateMap.get(dataTypes[i]).put(date, new ArrayList<>());
//	        		}
//	        		for(File str : list1)
//	        		{
//	        			filePathMapList.get(dataTypes[i]).add(str.getAbsolutePath());
//	        			filePathTypeDateMap.get(dataTypes[i]).get(date).add(str.getAbsolutePath());
//	        		}
//	        		dateCount++;
//				}
//	        }
//			
//	        
//	        String[] fileDirs1 = new String[2];
//			String[] dataTypes1 = new String[2];
//			fileDirs1[0] = configMap.get("ecmf_path");
//			fileDirs1[1] = configMap.get("grapes_path");
//			dataTypes1[0] = DataTypeEnum.ECMF.getDataType();
//			dataTypes1[1] = DataTypeEnum.GRAPES.getDataType();
//			Map<String, List<String>> filePathMapList1 = new HashMap<>();
//			Map<String, Map<String, List<String>>> filePathTypeDateMap1 = new HashMap<>();
//	        for(String dataType : dataTypes1)
//	        {
//	        	filePathMapList1.put(dataType, new ArrayList<>());
//	        	filePathTypeDateMap1.put(dataType, new HashMap<>());
//	        }
//	        calendar.setTime(new Date());
//	        Date endDate1 = calendar.getTime();
//	        calendar.add(Calendar.DAY_OF_MONTH, -2);
//	        Date startDate1 = calendar.getTime();
//	        String startDate1Str = TimeUtil.date2String(startDate1, TimeUtil.DEFAULT_DATE_FORMAT);
//	        Set<String> dateSet1 = new HashSet<>();
//	        for(int i = 0, num = fileDirs1.length; i < num; i++)
//	        {
//	        	File file = new File(fileDirs1[i]);
//        		if(!file.exists())
//        		{
//        			continue;
//        		}
//        		String dataType = dataTypes1[i];
//        		System.out.println("抽取数据路径1: " + file.getAbsolutePath());
//        		String suffix = sufMap.get(dataTypes1[i]);
//        		File[] list1 = file.listFiles(new FilenameFilter() {
//					
//					@Override
//					public boolean accept(File dir, String name) {
//						boolean endsWith = name.toLowerCase().endsWith(suffix);
//						boolean result = false;
//						if(endsWith)
//						{
//							String[] vtiDataTime = GribUtil.getVtiDataTime(name, dataType);
//					        String dataTime = vtiDataTime[1];
//					        if(dataTime.compareTo(startDate1Str) > 0)
//					        {
//					        	dateSet1.add(dataTime);
//					        	result = true;
//					        }
//						}
//						
//						return result;
//					}
//				});
//        		if(list1 == null)
//        		{
//        			continue;
//        		}
//        		for(File str : list1)
//        		{
//        			filePathMapList1.get(dataTypes1[i]).add(str.getAbsolutePath());
//        			String[] vtiDataTime = GribUtil.getVtiDataTime(str.getName(), dataType);
//			        String date = vtiDataTime[1];
//        			if(!filePathTypeDateMap1.get(dataTypes[i]).containsKey(date))
//            		{
//        				filePathTypeDateMap1.get(dataTypes[i]).put(date, new ArrayList<>());
//            		}
//        			filePathTypeDateMap1.get(dataTypes[i]).get(date).add(str.getAbsolutePath());
//        		}
//	        }
//	        
//	        
//	        ExtractFileRecordIndbUtil.fileIndb(stationInfo, filePathMapList);
//	        ExtractFileRecordIndbUtil.fileIndb1(stationInfo, filePathMapList1);
//			
//			System.out.println("上传数据后，添加站点成功");
////			StartExtractDataThread extractData = new StartExtractDataThread(stationInfo, filePathMapList, dataTypes, startDate, endDate);
////			StartExtractDataThread extractData = new StartExtractDataThread(stationInfo, filePathMapList, startDate, endDate);
////			StartExtractDataThread extractData1 = new StartExtractDataThread(stationInfo, filePathMapList1, startDate1, endDate1);
//			StartExtractDataByDateThread extractData = new StartExtractDataByDateThread(stationInfo, filePathTypeDateMap, startDate, endDate, dateCount);
//			StartExtractDataByDateThread extractData1 = new StartExtractDataByDateThread(stationInfo, filePathTypeDateMap1, startDate1, endDate1, dateSet1.size());
//	        System.out.println("开始抽取已有站点历史数据...");
//	        executor.execute(extractData);
//	        executor.execute(extractData1);
//	        
//	        addModel(station, 6);
//		}
//		else
//		{
//			System.out.println("上传数据后，添加站点失败");
//		}
		
	}
	
    public int addModel(StationInfoEntity stationInfo, int managerId) {
        ModelManagerDetailEntity data = new ModelManagerDetailEntity();
        data.setStationNum(stationInfo.getStationIdD() + "");
        data.setStationName(stationInfo.getStationName());
        data.setLon(stationInfo.getLon());
        data.setLat(stationInfo.getLat());
        data.setAuthor(stationInfo.getAuthor());
        data.setStatus(1);
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
        data1.setStatus(1);
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

	public static void main(String[] args) {
//		String filePath = "E:/fl/导入观测/202510/excel/test2.xls";
//		ExcelReader reader = ExcelUtil.getReader(filePath);
//		UploadDataIndbThread upload = new UploadDataIndbThread(filePath, 0, null, null);
//		upload.run();
//		System.out.println(reader);
	}
}
