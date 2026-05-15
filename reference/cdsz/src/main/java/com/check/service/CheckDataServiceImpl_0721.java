//package com.check.service;
//
//import com.check.pojo.CheckDataParams;
//import com.check.service.inf.CheckDataService;
//import com.config.dao.ConfigMapper;
//import com.config.pojo.ConfigParams;
//import com.config.pojo.VitsEntity;
//import com.constants.DataTypeEnum;
//import com.constants.DecodeConstants;
//import com.forecast.dao.GribForecastDataMapper;
//import com.forecast.dao.StationForecastDataMapper;
//import com.forecast.pojo.GribForecastDataEntity;
//import com.forecast.pojo.GribForecastRainValueEntity;
//import com.forecast.pojo.StationForecastDataEntity;
//import com.obs.dao.ObsDataMapper;
//import com.obs.pojo.ObsDataEntity;
//import com.station.pojo.StationEntity;
//import com.util.*;
//
//import org.springframework.stereotype.Service;
//
//import javax.annotation.Resource;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
///**
// * @category
// * @date 2025/3/19 10:52
// * @description TODO
// */
//@Service
//public class CheckDataServiceImpl_0721 implements CheckDataService {
//
//    @Resource
//    private ObsDataMapper obsDataMapper;
//    @Resource
//    private StationForecastDataMapper stationForecastDataMapper;
//    @Resource
//    private GribForecastDataMapper gribForecastDataMapper;
//    @Resource
//    private ConfigMapper configMapper;
//    private String rate = "晴雨准确率";
//    private double absAt = 273.15;
////    private static Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
//    private Map<String, String> tableNameMap = ReadPropertiesUtil.getUserConfigMap("data_table.properties");
//    @Override
//    public Map<String, Map<String, Map<String, Map<String, Double>>>> checkStationDataHour(CheckDataParams param) {
//        int disVti = param.getDisVti();
//        int[] vtis = null;
//        if(disVti == 3)
//        {
//            vtis = new int[]{3  ,6  ,9  ,12 ,15 ,18 ,21 ,24 ,27 ,30 ,33 ,36 ,39 ,42 ,45 ,48 ,51 ,54 ,57 ,60 ,63 ,66 ,69 ,72 ,75 ,78 ,81 ,84 ,87 ,90 ,93 ,96 ,99 ,102,105,108,111,114,117,120,123,126,129,132,135,138,141,144,147,150,153,156,159,162,165,168,171,174,177,180,183,186,189,192,195,198,201,204,207,210,213,216,219,222,225,228,231,234,237,240};
//        }
//        else if(disVti == 6)
//        {
//            vtis = new int[]{6  ,12 ,18 ,24 ,30 ,36 ,42 ,48 ,54 ,60 ,66 ,72 ,78 ,84 ,90 ,96 ,102,108,114,120,126,132,138,144,150,156,162,168,174,180,186,192,198,204,210,216,222,228,234,240};
//        }
//        else if(disVti == 12)
//        {
//            vtis = new int[]{12 ,24 ,36 ,48 ,60 ,72 ,84 ,96 ,108,120,132,144,156,168,180,192,204,216,228,240};
//        }
//        else
//        {
//            vtis = new int[]{24 ,48 ,72 ,96 ,120,144,168,192,216,240};
////            vtis = new int[]{24};
//        }
//        param.setVtis(vtis);
//        param.setEndValidDate(TimeUtil.addHours(param.getEndValidDate(), 24));
//        Map<String, String> tableNamesMap = new HashMap<>();
//        if(param.getDataSources() != null)
//        {
//            for(String dataSource : param.getDataSources())
//            {
//                tableNamesMap.put(dataSource, tableNameMap.get(dataSource + "_value"));
//            }
//        }
//
//        if(param.getStations() == null || param.getStations().length == 0)
//        {
//            ConfigParams params = new ConfigParams();
//            params.setZone(param.getZone());
//            List<StationEntity> stationEntities = null;
//            if(param.getZone() != 6)
//            {
//                stationEntities = stationForecastDataMapper.queryForecastStationsByZone(param);
//            }
//            else
//            {
//                stationEntities = stationForecastDataMapper.queryForecastAllStations(param);
//            }
//            int count = stationEntities.size();
//            String[] stations = new String[count];
//            for(int i = 0; i < count; i++)
//            {
//                stations[i] = stationEntities.get(i).getStation();
//            }
//            param.setStations(stations);
//        }
//
////        Calendar calendar = Calendar.getInstance();
////        calendar.setTime(TimeUtil.String2Date(param.getStartValidDate(), TimeUtil.DEFAULT_DATETIME_FORMAT));
////        calendar.add(Calendar.HOUR_OF_DAY, -8);
////        param.setStartValidDate(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
//        String startTimeStr = param.getStartValidDate();
//        String startValidDate = TimeUtil.addHours(param.getStartValidDate(), -8);
//        param.setStartValidDate(startValidDate);
//        
////        calendar.setTime(TimeUtil.String2Date(param.getEndValidDate(), TimeUtil.DEFAULT_DATETIME_FORMAT));
////        calendar.add(Calendar.HOUR_OF_DAY, -8);
////        param.setEndValidDate(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
//        String endTimeStr = param.getEndValidDate();
//        String endValidDate = TimeUtil.addHours(endTimeStr, -8);
//        param.setEndValidDate(endValidDate);
//
//        long time = System.currentTimeMillis();
//        int hour = Integer.parseInt(startTimeStr.split(" ")[1].split(":")[0]);
//        param.setHour(hour - 8);
////        Calendar calendar = Calendar.getInstance();
////        calendar.setTime(TimeUtil.String2Date(param.getStartValidDate(), TimeUtil.DEFAULT_DATETIME_FORMAT));
////        calendar.add(Calendar.HOUR_OF_DAY, -24);
////        param.setStartValidDate(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
//        param.setStartValidDate(TimeUtil.addHours(param.getStartValidDate(), -24));
//        
//        
//        List<StationForecastDataEntity> stationForecastList = null;
//        if(param.getElements()[0].equals("rain"))
//        {
//        	stationForecastList = stationForecastDataMapper.queryStationForecastCheckRain(param);
//        }
//        else
//        {
////        	calendar.setTime(TimeUtil.String2Date(param.getStartValidDate(), TimeUtil.DEFAULT_DATETIME_FORMAT));
////            calendar.add(Calendar.HOUR_OF_DAY, 24);
////            param.setStartValidDate(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
//            param.setStartValidDate(TimeUtil.addHours(param.getStartValidDate(), 24));
//        	stationForecastList = stationForecastDataMapper.queryStationForecastCheck(param);
//        }
////        for(StationForecastDataEntity data : stationForecastList)
////        {
////            calendar.setTime(TimeUtil.String2Date(data.getDatatime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
////            calendar.add(Calendar.HOUR_OF_DAY, 8);
////            data.setDatatime(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
////        }
//
//        if(hour == 8)
//        {
//        	hour = 20;
//        }
//        else
//        {
//        	hour = 8;
//        }
//        param.setHour(hour);
//        int[] vtisPre12 = new int[param.getVtis().length];
//        for(int i = 0, count = param.getVtis().length - 1; i < count; i++)
//        {
//        	vtisPre12[i] = param.getVtis()[i] + 12;
//        }
//        vtisPre12[vtisPre12.length - 1] = vtis[param.getVtis().length - 1];
//        Map<String, List<StationForecastDataEntity>> orgDatasMap = new HashMap<>();
//        param.setStartValidDate(startTimeStr);
//        param.setEndValidDate(endTimeStr);
//        for(String dataSource : tableNamesMap.keySet())
//        {
//            param.setTableName(tableNamesMap.get(dataSource));
//            List<StationForecastDataEntity> stationForecastDataEntities = stationForecastDataMapper.queryOrgStationForecastCheck(param);
//            if(stationForecastDataEntities == null || stationForecastDataEntities.size() == 0)
//            {
//                continue;
//            }
//            if(dataSource.equals(DataTypeEnum.ECMF.getDataType()))
//            {
//            	for(StationForecastDataEntity data : stationForecastDataEntities)
//            	{
//            		data.setAt(data.getAt() - absAt);
//            		data.setAtmax(data.getAtmax() - absAt);
//            		data.setAtmin(data.getAtmin() - absAt);
//            		data.setRain24(data.getRain24() * 1000);
//            		data.setDatatime(TimeUtil.addHours(data.getDatatime(), -8));
//            		data.setValiddate(TimeUtil.addHours(data.getValiddate(), -8));
//            	}
//            }
//            else
//            {
//            	for(StationForecastDataEntity data : stationForecastDataEntities)
//            	{
//            		data.setAt(data.getAt() - absAt);
//            		data.setAtmax(data.getAtmax() - absAt);
//            		data.setAtmin(data.getAtmin() - absAt);
//            		data.setDatatime(TimeUtil.addHours(data.getDatatime(), -8));
//            		data.setValiddate(TimeUtil.addHours(data.getValiddate(), -8));
//            	}
//            }
//            orgDatasMap.put(dataSource, stationForecastDataEntities);
//        }
//
//        System.out.println("查询预报数据耗时: " + (System.currentTimeMillis() - time));
//
////        calendar.setTime(TimeUtil.String2Date(param.getStartValidDate(), TimeUtil.DEFAULT_DATETIME_FORMAT));
////        calendar.add(Calendar.HOUR_OF_DAY, -8);
////        param.setStartTime(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
//        
//        param.setStartTime(startValidDate);
//
////        calendar.setTime(TimeUtil.String2Date(param.getEndValidDate(), TimeUtil.DEFAULT_DATETIME_FORMAT));
////        calendar.add(Calendar.HOUR_OF_DAY, -8);
////        param.setEndValidDate(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
//        param.setEndValidDate(endValidDate);
//
//        time = System.currentTimeMillis();
//        List<ObsDataEntity> obsList = obsDataMapper.queryObsCheckData(param);
//        System.out.println("查询实况数据耗时: " + (System.currentTimeMillis() - time));
//
//        Map<String, Map<String, Map<String, Map<String, Double>>>> result = new LinkedHashMap<>();
//        orgDatasMap.put("fst", stationForecastList);
//        for(String dataSource : orgDatasMap.keySet())
//        {
//            Map<String, Map<String, Map<String, Double>>> calStation = null;
//            if(dataSource.equals("fst"))
//            {
//            	param.setVtis(vtis);
//            	calStation = calStationHour(param, obsList, orgDatasMap.get(dataSource));
//            }
//            else
//            {
//            	param.setVtis(vtisPre12);
//            	param.setDataSource(dataSource);
//            	calStation = calStationHourPre12(param, obsList, orgDatasMap.get(dataSource));
//            }
//            result.put(dataSource, calStation);
//        }
//
//        for(String dataSource : tableNamesMap.keySet())
//        {
//            if(!result.containsKey(dataSource))
//            {
//                result.put(dataSource, new LinkedHashMap<>());
//                for(String element : result.get("fst").keySet())
//                {
//                    result.get(dataSource).put(element, new LinkedHashMap<>());
//                    for(String method : result.get("fst").get(element).keySet())
//                    {
//                        result.get(dataSource).get(element).put(method, new LinkedHashMap<>());
//                        for(String vti : result.get("fst").get(element).get(method).keySet())
//                        {
//                            result.get(dataSource).get(element).get(method).put(vti, null);
//                        }
//                    }
//                }
//            }
//        }
//
//        return result;
//    }
//
//    private Map<String, Map<String, Map<String, Double>>> calStationHour(CheckDataParams param, List<ObsDataEntity> obsList, List<StationForecastDataEntity> stationForecastList)
//    {
////          station     datetime
//        Map<String, Map<String, ObsDataEntity>> obsDataMap = new HashMap<>();
////          station     datetime_vti
//        Map<String, Map<String, StationForecastDataEntity>> fstDataMap = new HashMap<>();
//        String[] stations = param.getStations();
//        for(String station : stations)
//        {
//            obsDataMap.put(station, new HashMap<>());
//            fstDataMap.put(station, new HashMap<>());
//        }
//
//        for(ObsDataEntity obsData : obsList)
//        {
//            obsDataMap.get(obsData.getStation()).put(obsData.getDatetime(), obsData);
//        }
//        for(StationForecastDataEntity fstData : stationForecastList)
//        {
//            fstDataMap.get(fstData.getStation()).put(fstData.getDatatime() + "_" + fstData.getVti(), fstData);
////            System.out.println("key:" + (fstData.getDatatime() + "_" + fstData.getVti()));
//        }
//        String[] elements = param.getElements();
//        int[] vtis = param.getVtis();
//        Map<String, List<StationForecastDataEntity>> vtiDataMap = new HashMap<>();
//        if(!elements[0].equals("rain"))
//    	{
//    		int start = 0;
//    		for(int vti : vtis)
//    		{
//    			vtiDataMap.put(vti + "", new ArrayList<>());
//    			for(StationForecastDataEntity fstData : stationForecastList)
//    			{
//    				if(fstData.getVti() > (start == 0 ? -1 : start) && fstData.getVti() <= vti && fstData.getValiddate().compareTo(param.getStartValidDate()) > 0)
//    				{
//    					vtiDataMap.get(vti + "").add(fstData);
//    				}
//    			}
//    			start = vti;
//    		}
//    	}
//    	else
//    	{
//    		for(int vti : vtis)
//    		{
//    			vtiDataMap.put(vti + "", new ArrayList<>());
//    			for(StationForecastDataEntity fstData : stationForecastList)
//    			{
//    				if(fstData.getVti() == vti && fstData.getValiddate().compareTo(param.getStartValidDate()) > 0)
//    				{
//    					vtiDataMap.get(vti + "").add(fstData);
//    				}
//    			}
//    		}
//    	}
//
//
//        String[] methods = param.getMethods();
//        Map<String, Map<String, Map<String, Map<String, Double>>>> result = new LinkedHashMap<>();
//        if(!elements[0].equals("rain"))
//        {
//            for(int vti : vtis)
//            {
//                double value = getStationValue(elements[0], methods[0], obsDataMap, vtiDataMap.get(vti + ""));
//                result.put(vti + "", new HashMap<>());
//                result.get(vti + "").put(elements[0], new HashMap<>());
//                result.get(vti + "").get(elements[0]).put(methods[0], new HashMap<>());
//                result.get(vti + "").get(elements[0]).get(methods[0]).put("value", value);
//            }
//        }
//        else
//        {
//            for(int vti : vtis)
//            {
//                double[] value = getStationCalHourValue(obsDataMap, fstDataMap, vtiDataMap.get(vti + ""));
////                System.out.println("vti:" + vti + "," + JSONObject.toJSON(value));
//                result.put(vti + "", new LinkedHashMap<>());
//                result.get(vti + "").put(elements[0], new LinkedHashMap<>());
//                result.get(vti + "").get(elements[0]).put("TS评分", new LinkedHashMap<>());
//                result.get(vti + "").get(elements[0]).put("晴雨准确率", new LinkedHashMap<>());
//                result.get(vti + "").get(elements[0]).put("空报率", new LinkedHashMap<>());
//                result.get(vti + "").get(elements[0]).put("漏报率", new LinkedHashMap<>());
//                result.get(vti + "").get(elements[0]).get("TS评分").put("value", value[0]);
//                result.get(vti + "").get(elements[0]).get("晴雨准确率").put("value", value[1]);
//                result.get(vti + "").get(elements[0]).get("空报率").put("value", value[2]);
//                result.get(vti + "").get(elements[0]).get("漏报率").put("value", value[3]);
//            }
//        }
//
//        Map<String, Map<String, Map<String, Double>>> resultMap = new LinkedHashMap<>();
//        for(String element : elements)
//        {
//            resultMap.put(element, new LinkedHashMap<>());
//            for(String method : methods)
//            {
//                resultMap.get(element).put(method, new LinkedHashMap<>());
//                for(int vti : vtis)
//                {
//                	Double value = result.get(String.valueOf(vti)).get(element).get(method).get("value");
//                	if(value == DecodeConstants.UNDEF_DOUBLE_VALUE)
//                	{
//                		value = null;
//                	}
//                    resultMap.get(element).get(method).put(String.valueOf(vti), value);
//                }
//            }
//        }
//
//        return resultMap;
//    }
//    private Map<String, Map<String, Map<String, Double>>> calStationHourPre12(CheckDataParams param, List<ObsDataEntity> obsList, List<StationForecastDataEntity> stationForecastList)
//    {
////          station     datetime
//    	Map<String, Map<String, ObsDataEntity>> obsDataMap = new HashMap<>();
////          station     datetime_vti
//    	Map<String, Map<String, StationForecastDataEntity>> fstDataMap = new HashMap<>();
//    	String[] stations = param.getStations();
//    	for(String station : stations)
//    	{
//    		obsDataMap.put(station, new HashMap<>());
//    		fstDataMap.put(station, new HashMap<>());
//    	}
//    	
//    	for(ObsDataEntity obsData : obsList)
//    	{
//    		obsDataMap.get(obsData.getStation()).put(obsData.getDatetime(), obsData);
//    	}
//    	for(StationForecastDataEntity fstData : stationForecastList)
//    	{
//    		fstDataMap.get(fstData.getStation()).put(fstData.getDatatime() + "_" + fstData.getVti(), fstData);
////            System.out.println("key:" + (fstData.getDatatime() + "_" + fstData.getVti()));
//    	}
//    	String[] elements = param.getElements();
//    	int[] vtisTem = param.getVtis();
//    	int[] vtis = new int[vtisTem.length];
//    	for(int i = 0, count = vtis.length; i < count; i++)
//    	{
//    		vtis[i] = vtisTem[i];
//    	}
//    	Map<String, List<StationForecastDataEntity>> vtiDataMap = new HashMap<>();
//    	if(!elements[0].equals("rain"))
//    	{
//    		int start = 12;
//    		int disVti = 0;
//    		for(int vti : vtis)
//    		{
//    			disVti = vti - 12;
//    			vtiDataMap.put((disVti == 228 ? 240 : disVti) + "", new ArrayList<>());
//    			for(StationForecastDataEntity fstData : stationForecastList)
//    			{
//    				if(fstData.getVti() >= (start == 0 ? -1 : start) && fstData.getVti() <= vti && fstData.getValiddate().compareTo(param.getStartTime()) >= 0)
//    				{
//    					vtiDataMap.get((disVti == 228 ? 240 : disVti) + "").add(fstData);
//    				}
//    			}
//    			start = vti;
//    		}
//    	}
//    	else
//    	{
//    		int disVti = 0;
//    		for(int vti : vtis)
//    		{
//    			disVti = vti - 12;
//    			vtiDataMap.put((disVti == 228 ? 240 : disVti) + "", new ArrayList<>());
//    			for(StationForecastDataEntity fstData : stationForecastList)
//    			{
//    				if(fstData.getVti() == vti && fstData.getValiddate().compareTo(param.getStartValidDate()) > 0)
//    				{
//    					vtiDataMap.get((disVti == 228 ? 240 : disVti) + "").add(fstData);
//    				}
//    			}
//    		}
//    	}
//    	
//    	
//    	String[] methods = param.getMethods();
//    	Map<String, Map<String, Map<String, Map<String, Double>>>> result = new LinkedHashMap<>();
//    	
//    	for(int i = 0, count = vtis.length - 1; i < count; i++)
//        {
//    		vtis[i] -= 12;
//        }
//    	if(!elements[0].equals("rain"))
//    	{
//    		for(int vti : vtis)
//    		{
//    			double value = getStationValuePre12(param.getDataSource(), vti, elements[0], methods[0], obsDataMap, vtiDataMap.get(vti + ""));
//    			result.put(vti + "", new HashMap<>());
//    			result.get(vti + "").put(elements[0], new HashMap<>());
//    			result.get(vti + "").get(elements[0]).put(methods[0], new HashMap<>());
//    			result.get(vti + "").get(elements[0]).get(methods[0]).put("value", value);
//    		}
//    	}
//    	else
//    	{
//    		for(int vti : vtis)
//    		{
//    			double[] value = getStationCalHourValuePre12(vti, obsDataMap, fstDataMap, vtiDataMap.get(vti + ""));
////                System.out.println("vti:" + vti + "," + JSONObject.toJSON(value));
//    			result.put(vti + "", new LinkedHashMap<>());
//    			result.get(vti + "").put(elements[0], new LinkedHashMap<>());
//    			result.get(vti + "").get(elements[0]).put("TS评分", new LinkedHashMap<>());
//    			result.get(vti + "").get(elements[0]).put("晴雨准确率", new LinkedHashMap<>());
//    			result.get(vti + "").get(elements[0]).put("空报率", new LinkedHashMap<>());
//    			result.get(vti + "").get(elements[0]).put("漏报率", new LinkedHashMap<>());
//    			result.get(vti + "").get(elements[0]).get("TS评分").put("value", value[0]);
//    			result.get(vti + "").get(elements[0]).get("晴雨准确率").put("value", value[1]);
//    			result.get(vti + "").get(elements[0]).get("空报率").put("value", value[2]);
//    			result.get(vti + "").get(elements[0]).get("漏报率").put("value", value[3]);
//    		}
//    	}
//    	
//    	Map<String, Map<String, Map<String, Double>>> resultMap = new LinkedHashMap<>();
//    	for(String element : elements)
//    	{
//    		resultMap.put(element, new LinkedHashMap<>());
//    		for(String method : methods)
//    		{
//    			resultMap.get(element).put(method, new LinkedHashMap<>());
//    			for(int vti : vtis)
//    			{
//    				Double value = result.get(String.valueOf(vti)).get(element).get(method).get("value");
//    				if(value == DecodeConstants.UNDEF_DOUBLE_VALUE)
//    				{
//    					value = null;
//    				}
//    				resultMap.get(element).get(method).put(String.valueOf(vti), value);
//    			}
//    		}
//    	}
//    	
//    	return resultMap;
//    }
//    
//    public Map<String, Map<String, Map<String, Map<String, Double>>>> checkStationDataDay(CheckDataParams param) {
//        int disVti = param.getDisVti();
//        int[] vtis = null;
//        if(disVti == 3)
//        {
//            vtis = new int[]{3  ,6  ,9  ,12 ,15 ,18 ,21 ,24 ,27 ,30 ,33 ,36 ,39 ,42 ,45 ,48 ,51 ,54 ,57 ,60 ,63 ,66 ,69 ,72 ,75 ,78 ,81 ,84 ,87 ,90 ,93 ,96 ,99 ,102,105,108,111,114,117,120,123,126,129,132,135,138,141,144,147,150,153,156,159,162,165,168,171,174,177,180,183,186,189,192,195,198,201,204,207,210,213,216,219,222,225,228,231,234,237,240};
//        }
//        else if(disVti == 6)
//        {
//            vtis = new int[]{6  ,12 ,18 ,24 ,30 ,36 ,42 ,48 ,54 ,60 ,66 ,72 ,78 ,84 ,90 ,96 ,102,108,114,120,126,132,138,144,150,156,162,168,174,180,186,192,198,204,210,216,222,228,234,240};
//        }
//        else if(disVti == 12)
//        {
//            vtis = new int[]{12 ,24 ,36 ,48 ,60 ,72 ,84 ,96 ,108,120,132,144,156,168,180,192,204,216,228,240};
//        }
//        else
//        {
//            vtis = new int[]{24 ,48 ,72 ,96 ,120,144,168,192,216,240};
////            vtis = new int[]{24};
//        }
//        param.setVtis(vtis);
//
//        String startTimeStr = param.getStartValidDate();
//        String endTimeStr = param.getEndValidDate();
//        int hour = Integer.parseInt(startTimeStr.split(" ")[1].split(":")[0]);
//        param.setHour(hour);
//
//        int vti = param.getVti();
//        Map<String, String> tableNamesMap = new HashMap<>();
//        if(param.getDataSources() != null)
//        {
//            for(String dataSource : param.getDataSources())
//            {
//                tableNamesMap.put(dataSource, tableNameMap.get(dataSource + "_value"));
//            }
//        }
//
//
//        if(param.getStations() == null || param.getStations().length == 0)
//        {
//            ConfigParams params = new ConfigParams();
//            params.setZone(param.getZone());
//            List<StationEntity> stationEntities = null;
//            if(param.getZone() != 6)
//            {
//                stationEntities = stationForecastDataMapper.queryForecastStationsByZone(param);
//            }
//            else
//            {
//                stationEntities = stationForecastDataMapper.queryForecastAllStations(param);
//            }
//            int count = stationEntities.size();
//            String[] stations = new String[count];
//            for(int i = 0; i < count; i++)
//            {
//                stations[i] = stationEntities.get(i).getStation();
//            }
//            param.setStations(stations);
//        }
//
//        Calendar calendar = Calendar.getInstance();
////        calendar.setTime(TimeUtil.String2Date(param.getStartValidDate(), TimeUtil.DEFAULT_DATETIME_FORMAT));
////        calendar.add(Calendar.HOUR_OF_DAY, -8);
////        param.setStartValidDate(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
//        
//        param.setStartValidDate(TimeUtil.addHours(param.getStartValidDate(), -8));
//        
////        calendar.setTime(TimeUtil.String2Date(endTimeStr, TimeUtil.DEFAULT_DATETIME_FORMAT));
////        calendar.add(Calendar.HOUR_OF_DAY, -8);
////        param.setEndValidDate(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
//        
//        param.setEndValidDate(TimeUtil.addHours(endTimeStr, -8));
//        
//        param.setStartVti(vti - 24);
//        param.setEndVti(vti);
//        param.setHour(hour - 8);
//        String elementStr = param.getElements()[0];
//        List<StationForecastDataEntity> stationForecastList = null;
//        if(elementStr.equals("rain"))
//        {
//        	if(vti == 24)
//        	{
//        		param.setVtis(new int[]{0, 24});
//        	}
//        	else
//        	{
//        		param.setVtis(new int[]{vti - 24, vti});
//        	}
////        	Calendar calendar = Calendar.getInstance();
//        	calendar.setTime(TimeUtil.String2Date(param.getStartValidDate(), TimeUtil.DEFAULT_DATETIME_FORMAT));
//        	calendar.add(Calendar.HOUR_OF_DAY, -(vti - 24));
//        	param.setStartValidDate(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
//        	param.setEndValidDate(TimeUtil.addHours(param.getEndValidDate(), 24));
////        	param.setStartValidDate(TimeUtil.addHours(param.getStartValidDate(), -24));
//        	stationForecastList = stationForecastDataMapper.queryStationForecastCheckDayRain(param);
//        }
//        else
//        {
//        	param.setEndValidDate(TimeUtil.addHours(param.getEndValidDate(), 24));
//        	stationForecastList = stationForecastDataMapper.queryStationForecastCheckDay(param);
//        }
//        for(StationForecastDataEntity data : stationForecastList)
//        {
//        	data.setVis(data.getVis() / 1000);
//        }
//
////        calendar.setTime(TimeUtil.String2Date(startTimeStr, TimeUtil.DEFAULT_DATETIME_FORMAT));
////        calendar.add(Calendar.HOUR_OF_DAY, -8);
////        param.setStartTime(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
//        
//        param.setStartTime(TimeUtil.addHours(startTimeStr, -8));
//
//        long time = System.currentTimeMillis();
////        Calendar calendar = Calendar.getInstance();
//        calendar.setTime(TimeUtil.String2Date(startTimeStr, TimeUtil.DEFAULT_DATETIME_FORMAT));
//        calendar.add(Calendar.HOUR_OF_DAY, -24);
//        calendar.add(Calendar.HOUR_OF_DAY, -8);
//        param.setStartTime(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
////        param.setStartValidDate(TimeUtil.addHours(param.getStartValidDate(), -24));
////        if(elementStr.equals("rain"))
////        {
////        }
//        param.setEndValidDate(TimeUtil.addHours(param.getEndValidDate(), 24));
//        List<ObsDataEntity> obsList = obsDataMapper.queryObsCheckData(param);
//        System.out.println("查询实况数据耗时: " + (System.currentTimeMillis() - time));
//        for(ObsDataEntity data : obsList)
//        {
//        	data.setVis(data.getVis() / 1000);
//        }
//
////        calendar.setTime(TimeUtil.String2Date(startTimeStr, TimeUtil.DEFAULT_DATETIME_FORMAT));
////        param.setStartTime(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
//        param.setStartTime(startTimeStr);
//        
//        
//        param.setEndValidDate(endTimeStr);
//
//        Map<String, List<StationForecastDataEntity>> orgDatasMap = new HashMap<>();
//        time = System.currentTimeMillis();
//        if(hour == 8)
//        {
//        	param.setHour(20);
//        }
//        else
//        {
//        	param.setHour(8);
//        }
//        for(int i = 0, count = param.getVtis().length; i < count; i++)
//        {
//    		if(param.getVtis()[i] == 240)
//    		{
//    			continue;
//    		}
//        	param.getVtis()[i] += 12;
//        }
////        param.setHour(hour);
//        param.setStartValidDate(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
//        param.setStartValidDate(TimeUtil.addHours(param.getStartValidDate(), 8));
//        param.setEndValidDate(endTimeStr);
//        
//        for(String dataSource : tableNamesMap.keySet())
//        {
//            param.setTableName(tableNamesMap.get(dataSource));
//            List<StationForecastDataEntity> stationForecastDataEntities = null;
//            if(elementStr.equals("rain"))
//            {
//            	param.setEndValidDate(TimeUtil.addHours(param.getEndValidDate(), 24));
//            	param.setEndValidDate(TimeUtil.addHours(param.getEndValidDate(), 12));
//            	stationForecastDataEntities = stationForecastDataMapper.queryOrgStationForecastCheckDayRain(param);
//            }
//            else
//            {
//            	param.setEndValidDate(TimeUtil.addHours(param.getEndValidDate(), 24));
//            	param.setStartVti(param.getStartVti() + 12);
//                param.setEndVti(param.getEndVti() + 12);
//            	stationForecastDataEntities = stationForecastDataMapper.queryOrgStationForecastCheckDay(param);
////            	String element = param.getElements()[0];
////            	if(param.getVti() == 96 || param.getVti() == 120)
////				{
////					double[] values = new double[5];
////					for(int i = 0; i < 5; i++)
////					{
////						values[i] = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(stationForecastDataEntities.get(i), element)));
////					}
////					double[] czValues = new double[9];
////					double[][] valuesTem = new double[1][];
////					valuesTem[0] = values;
////					czValues = BilinearInterpolateUtil.bilinearInterpolation(valuesTem, 2, 9)[0];
////					String timeStr = String.valueOf(ReflectUtil.getFieldValueByName(list.get(0), "datatime"));
////					int vtiTem = Integer.parseInt(String.valueOf(ReflectUtil.getFieldValueByName(list.get(0), "vti")));
////					for(int i = 0; i < 9; i++)
////					{
////						String timeTem = TimeUtil.addHours(time, i * 3);
////						StationForecastDataEntity data = new StationForecastDataEntity();
////						data.setDatatime(timeTem);
////						data.setStation(station);
////						data.setValiddate(TimeUtil.addHours(time, vtiTem + i * 3));
////						ReflectUtil.setFieldValueByName(data, czValues[i], element);
////						czList.add(data);
////					}
//////					System.out.println(czList);
////				}
//            }
//            if(stationForecastDataEntities == null || stationForecastDataEntities.size() == 0)
//            {
//                continue;
//            }
//            if(dataSource.equals(DataTypeEnum.ECMF.getDataType()))
//            {
//            	for(StationForecastDataEntity data : stationForecastDataEntities)
//                {
//                    data.setAt(data.getAt() - absAt);
//                    data.setVis(data.getVis() / 1000);
//                    data.setRain24(data.getRain24() * 1000);
//                    data.setDatatime(TimeUtil.addHours(data.getDatatime(), -8));
//                    data.setValiddate(TimeUtil.addHours(data.getValiddate(), -8));
//                }
//            }
//            else
//            {
//            	for(StationForecastDataEntity data : stationForecastDataEntities)
//                {
//                    data.setAt(data.getAt() - absAt);
//                    data.setVis(data.getVis() / 1000);
//                    data.setDatatime(TimeUtil.addHours(data.getDatatime(), -8));
//                    data.setValiddate(TimeUtil.addHours(data.getValiddate(), -8));
//                    
//                }
//            }
//            
//            orgDatasMap.put(dataSource, stationForecastDataEntities);
//        }
//
//        System.out.println("查询预报数据耗时: " + (System.currentTimeMillis() - time));
//
////        calendar.setTime(TimeUtil.String2Date(param.getStartValidDate(), TimeUtil.DEFAULT_DATETIME_FORMAT));
////        calendar.add(Calendar.DAY_OF_MONTH, 1);
////        calendar.add(Calendar.HOUR_OF_DAY, -8);
////        param.setStartTime(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
////
////        calendar.setTime(TimeUtil.String2Date(param.getEndValidDate(), TimeUtil.DEFAULT_DATETIME_FORMAT));
////        calendar.add(Calendar.HOUR_OF_DAY, -8);
////        param.setEndTime(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
//
//        for(int i = 0, count = param.getVtis().length; i < count; i++)
//        {
//    		if(param.getVtis()[i] == 240)
//    		{
//    			continue;
//    		}
//        	param.getVtis()[i] -= 12;
//        }
//
//        Map<String, Map<String, Map<String, Map<String, Double>>>> result = new LinkedHashMap<>();
//        orgDatasMap.put("fst", stationForecastList);
//        for(String dataSource : orgDatasMap.keySet())
//        {   
//        	time = System.currentTimeMillis();
//        	System.out.println("dataSource: " + dataSource);
//            Map<String, Map<String, Map<String, Double>>> calStation = null;
//            if(!dataSource.endsWith("fst"))
//            {
//            	param.setDataSource(dataSource);
//            	calStation = calStationDayPre12(param, obsList, orgDatasMap.get(dataSource));
//            }
//            else
//            {
//            	calStation = calStationDay(param, obsList, orgDatasMap.get(dataSource));
//            }
//            
//            result.put(dataSource, calStation);
//            System.out.println("计算数据耗时: " + (System.currentTimeMillis() - time));
//        }
//
//        calendar.setTime(TimeUtil.String2Date(startTimeStr, TimeUtil.DEFAULT_DATETIME_FORMAT));
//        Date startDate = calendar.getTime();
//        calendar.setTime(TimeUtil.String2Date(endTimeStr, TimeUtil.DEFAULT_DATETIME_FORMAT));
//        Date endDate = calendar.getTime();
//        List<String> dateList = new ArrayList<>();
//        for(int i = 0;; i++)
//        {
//            calendar.setTime(startDate);
//            calendar.add(Calendar.DATE, i);
//            if(calendar.getTime().after(endDate))
//            {
//                break;
//            }
//            dateList.add(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATE_FORMAT));
//        }
//
//        for(String dataSource : result.keySet())
//        {
//            for(String element : result.get(dataSource).keySet())
//            {
//                for(String method : result.get(dataSource).get(element).keySet())
//                {
//                    Map<String, Double> map = result.get(dataSource).get(element).get(method);
//                    Map<String, Double> tt = new HashMap<>();
//                    for(String dateStr : dateList)
//                    {
//                        tt.put(dateStr, map.get(dateStr));
//                    }
//                    result.get(dataSource).get(element).put(method, tt);
//
//                    List<String> list = result.get(dataSource).get(element).get(method).keySet().stream().sorted().collect(Collectors.toList());
//                    Map<String, Double> temp = new LinkedHashMap<>();
//                    for(String date : list)
//                    {
//                        temp.put(date, result.get(dataSource).get(element).get(method).get(date));
//                    }
//                    result.get(dataSource).get(element).put(method, temp);
//                }
//            }
//
//        }
//
//        for(String dataSource : tableNamesMap.keySet())
//        {
//            if(!result.containsKey(dataSource))
//            {
//                result.put(dataSource, new LinkedHashMap<>());
//                for(String element : result.get("fst").keySet())
//                {
//                    result.get(dataSource).put(element, new LinkedHashMap<>());
//                    for(String method : result.get("fst").get(element).keySet())
//                    {
//                        result.get(dataSource).get(element).put(method, new LinkedHashMap<>());
//                        for(String date : result.get("fst").get(element).get(method).keySet())
//                        {
//                            result.get(dataSource).get(element).get(method).put(date, null);
//                        }
//                    }
//                }
//            }
//        }
//
//        return result;
//    }
//
//    private Map<String, Map<String, Map<String, Double>>> calStationDay(CheckDataParams param, List<ObsDataEntity> obsList, List<StationForecastDataEntity> stationForecastList)
//    {
////        Map<String, Map<String, Map<String, Double>>> result = new LinkedHashMap<>();
////          station     datetime
//        Map<String, Map<String, ObsDataEntity>> obsDataMap = new HashMap<>();
////          station     datetime_vti
//        Map<String, Map<String, StationForecastDataEntity>> fstDataMap = new HashMap<>();
//        Map<String, Map<String, StationForecastDataEntity>> rainDataMap = new HashMap<>();
//        String[] stations = param.getStations();
//        for(String station : stations)
//        {
//            obsDataMap.put(station, new HashMap<>());
//            fstDataMap.put(station, new HashMap<>());
//            rainDataMap.put(station, new HashMap<>());
//        }
//
//        for(ObsDataEntity obsData : obsList)
//        {
////            calendar.setTime(TimeUtil.String2Date(obsData.getDatetime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
////            calendar.add(Calendar.HOUR_OF_DAY, 8);
////            obsDataMap.get(obsData.getStation()).put(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT), obsData);
////            obsDataMap.get(obsData.getStation()).put(TimeUtil.addHours(obsData.getDatetime(), 8), obsData);
//            obsDataMap.get(obsData.getStation()).put(obsData.getDatetime(), obsData);
//        }
//        for(StationForecastDataEntity fstData : stationForecastList)
//        {
//            fstDataMap.get(fstData.getStation()).put(fstData.getDatatime() + "_" + fstData.getVti(), fstData);
////            fstDataMap.get(fstData.getStation()).put(TimeUtil.addHours(fstData.getDatatime(), 8) + "_" + fstData.getVti(), fstData);
//        }
//        String[] elements = param.getElements();
////        int[] vtis = param.getVtis();
//
////         validdate
//        Map<String, List<StationForecastDataEntity>> dateDataMap = new HashMap<>();
//
//
////        逐日
//        String validDateStr = null;
//        Set<String> validDateSet = new HashSet<>();
//        List<String> validDateList = new ArrayList<>();
//        for(StationForecastDataEntity data : stationForecastList)
//        {
//            validDateStr = data.getValiddate().split(" ")[0];
//            if(!dateDataMap.containsKey(validDateStr))
//            {
//                dateDataMap.put(validDateStr, new ArrayList<>());
//            }
//            if(!param.getElements()[0].equals("rain"))
//            {
//            	if(data.getValiddate().compareTo(param.getStartValidDate()) > 0)
//                {
//                	dateDataMap.get(validDateStr).add(data);
//                	validDateSet.add(validDateStr);
//                }
//            }
//            else
//            {
//            	dateDataMap.get(validDateStr).add(data);
//            	validDateSet.add(validDateStr);
//            }
//        }
//        validDateList = validDateSet.stream().sorted().collect(Collectors.toList());
//
//        String[] methods = param.getMethods();
//        Map<String, Map<String, Map<String, Map<String, Double>>>> resultMap = new LinkedHashMap<>();
//
//        if(!elements[0].equals("rain"))
//        {
//            for(String validdate : validDateList)
//            {
//                double value = getStationValue(elements[0], methods[0], obsDataMap, dateDataMap.get(validdate));
//                resultMap.put(validdate, new HashMap<>());
//                resultMap.get(validdate).put(elements[0], new HashMap<>());
//                resultMap.get(validdate).get(elements[0]).put(methods[0], new HashMap<>());
//                resultMap.get(validdate).get(elements[0]).get(methods[0]).put("value", value);
//                if(value == DecodeConstants.UNDEF_DOUBLE_VALUE)
//                {
//                    resultMap.get(validdate).get(elements[0]).get(methods[0]).put("value", null);
//                }
//            }
//        }
//        else
//        {
//            for(StationForecastDataEntity fstData : stationForecastList)
//            {
//                rainDataMap.get(fstData.getStation()).put(fstData.getDatatime() + "_" + fstData.getVti(), fstData);
//            }
//
//            Map<String, List<StationForecastDataEntity>> dateRainDataMap = new HashMap<>();
//            for(StationForecastDataEntity data : stationForecastList)
//            {
//                validDateStr = data.getValiddate().split(" ")[0];
//                if(!dateRainDataMap.containsKey(validDateStr))
//                {
//                	dateRainDataMap.put(validDateStr, new ArrayList<>());
//                }
//                dateRainDataMap.get(validDateStr).add(data);
//            }
//            
//            for(int i = 1, count = validDateList.size(); i < count; i++)
//            {
//            	String validdate = validDateList.get(i);
//                double[] value = getStationCalHourValue(obsDataMap, rainDataMap, dateRainDataMap.get(validdate));
//                
////                System.out.println("validdate: " + validdate + " rate: " + value[1]);
//                validdate = validDateList.get(i - 1);
//                resultMap.put(validdate, new LinkedHashMap<>());
//                resultMap.get(validdate).put(elements[0], new LinkedHashMap<>());
//                resultMap.get(validdate).get(elements[0]).put("TS评分", new LinkedHashMap<>());
//                resultMap.get(validdate).get(elements[0]).put(rate, new LinkedHashMap<>());
//                resultMap.get(validdate).get(elements[0]).put("空报率", new LinkedHashMap<>());
//                resultMap.get(validdate).get(elements[0]).put("漏报率", new LinkedHashMap<>());
//                resultMap.get(validdate).get(elements[0]).get("TS评分").put("value", value[0]);
//                resultMap.get(validdate).get(elements[0]).get(rate).put("value", value[1]);
//                resultMap.get(validdate).get(elements[0]).get("空报率").put("value", value[2]);
//                resultMap.get(validdate).get(elements[0]).get("漏报率").put("value", value[3]);
//            }
//        }
//        validDateList = validDateList.subList(0, validDateList.size() - 1);
////          element     method      date
//        Map<String, Map<String, Map<String, Double>>> result = new LinkedHashMap<>();
//        for(String element : elements)
//        {
//            result.put(element, new LinkedHashMap<>());
//            for(String method : methods)
//            {
//                result.get(element).put(method, new LinkedHashMap<>());
//                for(String date : validDateList)
//                {
//                	Double value = resultMap.get(date).get(element).get(method).get("value");
//                	if(value == null || value == DecodeConstants.UNDEF_DOUBLE_VALUE)
//                	{
//                		value = null;
//                	}
//                    result.get(element).get(method).put(date, value);
//                }
//            }
//        }
//
//
//
//
//        return result;
//    }
//    
//    private Map<String, Map<String, Map<String, Double>>> calStationDayPre12(CheckDataParams param, List<ObsDataEntity> obsList, List<StationForecastDataEntity> stationForecastList)
//    {
////        Map<String, Map<String, Map<String, Double>>> result = new LinkedHashMap<>();
////          station     datetime
//        Map<String, Map<String, ObsDataEntity>> obsDataMap = new HashMap<>();
////          station     datetime_vti
//        Map<String, Map<String, StationForecastDataEntity>> fstDataMap = new HashMap<>();
//        Map<String, Map<String, StationForecastDataEntity>> rainDataMap = new HashMap<>();
//        String[] stations = param.getStations();
//        for(String station : stations)
//        {
//            obsDataMap.put(station, new HashMap<>());
//            fstDataMap.put(station, new HashMap<>());
//            rainDataMap.put(station, new HashMap<>());
//        }
//
//        for(ObsDataEntity obsData : obsList)
//        {
////            calendar.setTime(TimeUtil.String2Date(obsData.getDatetime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
////            calendar.add(Calendar.HOUR_OF_DAY, 8);
////            obsDataMap.get(obsData.getStation()).put(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT), obsData);
////            obsDataMap.get(obsData.getStation()).put(TimeUtil.addHours(obsData.getDatetime(), 8), obsData);
//            obsDataMap.get(obsData.getStation()).put(obsData.getDatetime(), obsData);
//        }
//        for(StationForecastDataEntity fstData : stationForecastList)
//        {
//            fstDataMap.get(fstData.getStation()).put(fstData.getDatatime() + "_" + fstData.getVti(), fstData);
////            fstDataMap.get(fstData.getStation()).put(TimeUtil.addHours(fstData.getDatatime(), 8) + "_" + fstData.getVti(), fstData);
//        }
//        String[] elements = param.getElements();
////        int[] vtis = param.getVtis();
//
////         validdate
//        Map<String, List<StationForecastDataEntity>> dateDataMap = new HashMap<>();
//
//
////        逐日
//        String validDateStr = null;
//        Set<String> validDateSet = new HashSet<>();
//        List<String> validDateList = new ArrayList<>();
//        for(StationForecastDataEntity data : stationForecastList)
//        {
//            validDateStr = data.getValiddate().split(" ")[0];
//            if(!dateDataMap.containsKey(validDateStr))
//            {
//                dateDataMap.put(validDateStr, new ArrayList<>());
//            }
//            if(!param.getElements()[0].equals("rain"))
//            {
//            	if(data.getValiddate().compareTo(param.getStartValidDate()) > 0)
//                {
//                	dateDataMap.get(validDateStr).add(data);
//                	validDateSet.add(validDateStr);
//                }
//            }
//            else
//            {
//            	dateDataMap.get(validDateStr).add(data);
//            	validDateSet.add(validDateStr);
//            }
//        }
//        validDateList = validDateSet.stream().sorted().collect(Collectors.toList());
//
//        String[] methods = param.getMethods();
//        Map<String, Map<String, Map<String, Map<String, Double>>>> resultMap = new LinkedHashMap<>();
//
//        if(!elements[0].equals("rain"))
//        {
//            for(String validdate : validDateList)
//            {
//                double value = DecodeConstants.UNDEF_DOUBLE_VALUE;
//                if(param.getVti() == 240)
//        		{
//                	value = getStationValuePre12(elements[0], methods[0], obsDataMap, dateDataMap.get(validdate));
//        		}
//                else if(param.getVti() <= 48 || param.getVti() == 96 || param.getVti() == 120)
//                {
//                	value = getStationValuePre12(param.getDataSource(), param.getVti(), elements[0], methods[0], obsDataMap, dateDataMap.get(validdate));
//                }
//                else
//                {
//                	value = getStationValue(elements[0], methods[0], obsDataMap, dateDataMap.get(validdate));
//                }
//                resultMap.put(validdate, new HashMap<>());
//                resultMap.get(validdate).put(elements[0], new HashMap<>());
//                resultMap.get(validdate).get(elements[0]).put(methods[0], new HashMap<>());
//                resultMap.get(validdate).get(elements[0]).get(methods[0]).put("value", value);
//                if(value == DecodeConstants.UNDEF_DOUBLE_VALUE)
//                {
//                    resultMap.get(validdate).get(elements[0]).get(methods[0]).put("value", null);
//                }
//            }
//        }
//        else
//        {
//            for(StationForecastDataEntity fstData : stationForecastList)
//            {
//                rainDataMap.get(fstData.getStation()).put(fstData.getDatatime() + "_" + fstData.getVti(), fstData);
//            }
//
//            Map<String, List<StationForecastDataEntity>> dateRainDataMap = new HashMap<>();
//            for(StationForecastDataEntity data : stationForecastList)
//            {
//                validDateStr = data.getValiddate().split(" ")[0];
//                if(!dateRainDataMap.containsKey(validDateStr))
//                {
//                	dateRainDataMap.put(validDateStr, new ArrayList<>());
//                }
//                dateRainDataMap.get(validDateStr).add(data);
//            }
//            
//            for(int i = 1, count = validDateList.size(); i < count; i++)
//            {
//            	String validdate = validDateList.get(i);
//                double[] value = null;
//                if(param.getVti() == 240)
//        		{
//                	value = getStationCalHourValuePre12(obsDataMap, rainDataMap, dateRainDataMap.get(validdate));
//        		}
//                else
//                {
//                	value = getStationCalHourValue(obsDataMap, rainDataMap, dateRainDataMap.get(validdate));
//                }
//                
////                System.out.println("validdate: " + validdate + " rate: " + value[1]);
//                validdate = validDateList.get(i - 1);
//                resultMap.put(validdate, new LinkedHashMap<>());
//                resultMap.get(validdate).put(elements[0], new LinkedHashMap<>());
//                resultMap.get(validdate).get(elements[0]).put("TS评分", new LinkedHashMap<>());
//                resultMap.get(validdate).get(elements[0]).put(rate, new LinkedHashMap<>());
//                resultMap.get(validdate).get(elements[0]).put("空报率", new LinkedHashMap<>());
//                resultMap.get(validdate).get(elements[0]).put("漏报率", new LinkedHashMap<>());
//                resultMap.get(validdate).get(elements[0]).get("TS评分").put("value", value[0]);
//                resultMap.get(validdate).get(elements[0]).get(rate).put("value", value[1]);
//                resultMap.get(validdate).get(elements[0]).get("空报率").put("value", value[2]);
//                resultMap.get(validdate).get(elements[0]).get("漏报率").put("value", value[3]);
//            }
//        }
//        if(validDateList.size() > 0)
//        {
//        	validDateList = validDateList.subList(0, validDateList.size() - 1);
//        }
////          element     method      date
//        Map<String, Map<String, Map<String, Double>>> result = new LinkedHashMap<>();
//        for(String element : elements)
//        {
//            result.put(element, new LinkedHashMap<>());
//            for(String method : methods)
//            {
//                result.get(element).put(method, new LinkedHashMap<>());
//                for(String date : validDateList)
//                {
//                	Double value = resultMap.get(date).get(element).get(method).get("value");
//                	if(value == null || value == DecodeConstants.UNDEF_DOUBLE_VALUE)
//                	{
//                		value = null;
//                	}
//                    result.get(element).get(method).put(date, value);
//                }
//            }
//        }
//
//
//
//
//        return result;
//    }
//
//    @Override
//    public Map<String, Map<String, Map<String, Double>>> checkGribDataHour(CheckDataParams param) {
//
//        String tableName = tableNameMap.get(param.getDataSource() + "_rain_value");
//        param.setTableName(tableName);
//
////        datasource tableName
//        Map<String, String> tableNamesMap = new HashMap<>();
//        if(param.getDataSources() != null && param.getDataSources().length != 0)
//        {
//            for(String dataSource : param.getDataSources())
//            {
//                tableNamesMap.put(dataSource, tableNameMap.get(dataSource + "_rain_value"));
//            }
//        }
//
//        if(param.getDataSourcesOrg() != null && param.getDataSourcesOrg().length != 0)
//        {
//            for(String dataSource : param.getDataSourcesOrg())
//            {
//                tableNamesMap.put(dataSource + "_org", tableNameMap.get(dataSource + "_value"));
//            }
//        }
//
//
//        if(param.getZone() != null)
//        {
//            ConfigParams params = new ConfigParams();
//            params.setZone(param.getZone());
//            List<StationEntity> stationEntities = null;
//            if(param.getZone() != 6)
//            {
//                stationEntities = stationForecastDataMapper.queryForecastStationsByZone(param);
//            }
//            else
//            {
//                stationEntities = stationForecastDataMapper.queryForecastAllStations(param);
//            }
//            int count = stationEntities.size();
//            String[] stations = new String[count];
//            for(int i = 0; i < count; i++)
//            {
//                stations[i] = stationEntities.get(i).getStation();
//            }
//            param.setStations(stations);
//        }
//        else
//        {
//            Double lonLeft = param.getLonLeft();
//            Double lonRight = param.getLonRight();
//            Double latUp = param.getLatUp();
//            Double latDown = param.getLatDown();
//            if(lonLeft != null && lonRight != null && latUp != null && latDown != null)
//            {
//                List<StationEntity> stationEntities = stationForecastDataMapper.queryForecastAllStationsLonLat(param);
//                List<String> stationList = new ArrayList<>();
//                for(StationEntity stationEntity : stationEntities)
//                {
//                    if(stationEntity.getLon() >= lonLeft && stationEntity.getLon() <= lonRight && stationEntity.getLat() <= latUp && stationEntity.getLat() >= latDown)
//                    {
//                        stationList.add(stationEntity.getStation());
//                    }
//                }
//                String[] stations = new String[stationList.size()];
//                for(int i = 0; i < stations.length; i++)
//                {
//                    stations[i] = stationList.get(i);
//                }
//                param.setStations(stations);
//            }
//        }
//
//        String startTimeStr = param.getStartValidDate();
//        String endTimeStr = param.getEndValidDate();
//        int hour = Integer.parseInt(startTimeStr.split(" ")[1].split(":")[0]);
////        int vti = param.getVti();
//        int vti = 24;
//        Calendar calendar = Calendar.getInstance();
//        calendar.setTime(TimeUtil.String2Date(param.getStartValidDate(), TimeUtil.DEFAULT_DATETIME_FORMAT));
//        calendar.add(Calendar.HOUR_OF_DAY, -vti);
//        param.setStartValidDate(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
//
//        long time = System.currentTimeMillis();
////        List<GribForecastRainValueEntity> gribForecastList = gribForecastDataMapper.queryGribForecastRainCheckHour(param);
//
//        param.setEndValidDate(TimeUtil.addHours(param.getEndValidDate(), 24));
//        Map<String, List<GribForecastRainValueEntity>> datasMap = new HashMap<>();
//        for(String dataSource : tableNamesMap.keySet())
//        {
//            param.setTableName(tableNamesMap.get(dataSource));
//            List<GribForecastRainValueEntity> gribForecastDataRainEntities = null;
//            if(dataSource.contains("_org"))
//            {
//            	if(hour == 8)
//            	{
//            		param.setHour(20);
//            	}
//            	else
//            	{
//            		param.setHour(8);
//            	}
//            	for(int i = 0, count = param.getVtis().length - 1; i < count; i++)
//                {
//                	param.getVtis()[i] += 12;
//                }
//                gribForecastDataRainEntities = gribForecastDataMapper.queryGribForecastRainCheckHourOrg(param);
//                for(int i = 0, count = param.getVtis().length - 1; i < count; i++)
//                {
//                	param.getVtis()[i] -= 12;
//                }
//            }
//            else
//            {
//            	param.setHour(hour);
//                gribForecastDataRainEntities = gribForecastDataMapper.queryGribForecastRainCheckHour(param);
//            }
////            List<GribForecastRainValueEntity> gribForecastDataRainEntities = gribForecastDataMapper.queryGribForecastRainCheckHour(param);
//            if(gribForecastDataRainEntities == null || gribForecastDataRainEntities.size() == 0)
//            {
//                continue;
//            }
//            
//            if(dataSource.equals(DataTypeEnum.ECMF.getDataType() + "_org"))
//            {
//            	for(GribForecastRainValueEntity data : gribForecastDataRainEntities)
//                {
//                    data.setRain(data.getRain() * 1000);
//                    data.setDataTime(TimeUtil.addHours(data.getDataTime(), -8));
//                    data.setValidDate(TimeUtil.addHours(data.getValidDate(), -8));
//                }
//            }
//            else
//            {
//            	for(GribForecastRainValueEntity data : gribForecastDataRainEntities)
//                {
//                    data.setDataTime(TimeUtil.addHours(data.getDataTime(), -8));
//                    data.setValidDate(TimeUtil.addHours(data.getValidDate(), -8));
//                }
//            }
//            
//            datasMap.put(dataSource, gribForecastDataRainEntities);
//        }
//
//        System.out.println("查询预报数据耗时: " + (System.currentTimeMillis() - time));
//
//        calendar.setTime(TimeUtil.String2Date(startTimeStr, TimeUtil.DEFAULT_DATETIME_FORMAT));
//        calendar.add(Calendar.HOUR_OF_DAY, -vti);
//        calendar.add(Calendar.HOUR_OF_DAY, -8);
//        param.setStartTime(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
//
////        calendar.setTime(TimeUtil.String2Date(endTimeStr, TimeUtil.DEFAULT_DATETIME_FORMAT));
////        calendar.add(Calendar.HOUR_OF_DAY, -8);
////        param.setEndTime(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
//        param.setEndValidDate(TimeUtil.addHours(endTimeStr, -8));
//        param.setEndValidDate(TimeUtil.addHours(param.getEndValidDate(), 24));
//
//        time = System.currentTimeMillis();
//        List<ObsDataEntity> obsList = obsDataMapper.queryObsCheckData(param);
//        System.out.println("查询实况数据耗时: " + (System.currentTimeMillis() - time));
////        orgDatasMap.put("org", gribForecastList);
////          datasource  method      vti
//        Map<String, Map<String, Map<String, Double>>> result = new HashMap<>();
//        for(String dataSource : datasMap.keySet())
//        {
//        	System.out.println("dataSource: " + dataSource);
//            Map<String, Map<String, Double>> calGribHour = null;
//            if(dataSource.endsWith("_org"))
//            {
//            	calGribHour = calGribHourPre12(param, obsList, datasMap.get(dataSource));
//            }
//            else
//            {
//            	calGribHour = calGribHour(param, obsList, datasMap.get(dataSource));
//            }
//            result.put(dataSource, calGribHour);
//        }
//
//        String next = result.keySet().iterator().next();
//
//        for(String dataSource : tableNamesMap.keySet())
//        {
//            if(!result.containsKey(dataSource))
//            {
//                result.put(dataSource, new LinkedHashMap<>());
//
//                for(String method : result.get(next).keySet())
//                {
//                    result.get(dataSource).put(method, new LinkedHashMap<>());
//                    for(String vtiStr : result.get(next).get(method).keySet())
//                    {
//                        result.get(dataSource).get(method).put(vtiStr, null);
//                    }
//                }
//            }
//        }
//        String[] resultOrder = new String[]{"ecmf_org", "ecmf", "grapes_org", "grapes", "deep"};
//        Map<String, Map<String, Map<String, Double>>> resultMap = new LinkedHashMap<>();
//        for(String order : resultOrder)
//        {
//        	Map<String, Map<String, Double>> map = result.get(order);
//        	if(map != null)
//        	{
//        		resultMap.put(order, map);
//        	}
//        }
//
//
//
//        return resultMap;
//    }
//    
//
//    private Map<String, Map<String, Double>> calGribHour(CheckDataParams param, List<ObsDataEntity> obsList, List<GribForecastRainValueEntity> gribForecastList)
//    {
////          station     datetime
//        Map<String, Map<String, ObsDataEntity>> obsDataMap = new HashMap<>();
////          station     datetime_vti
//        Map<String, Map<String, GribForecastRainValueEntity>> fstDataMap = new HashMap<>();
//        String[] stations = param.getStations();
//        for(String station : stations)
//        {
//            obsDataMap.put(station, new HashMap<>());
//            fstDataMap.put(station, new HashMap<>());
//        }
//
//        Calendar calendar = Calendar.getInstance();
//        for(ObsDataEntity obsData : obsList)
//        {
////            calendar.setTime(TimeUtil.String2Date(obsData.getDatetime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
////            calendar.add(Calendar.HOUR_OF_DAY, 8);
//            obsDataMap.get(obsData.getStation()).put(obsData.getDatetime(), obsData);
//        }
//        for(GribForecastRainValueEntity fstData : gribForecastList)
//        {
//            fstDataMap.get(fstData.getStation()).put(fstData.getDataTime() + "_" + fstData.getVti(), fstData);
//        }
//
//
////           validdate
//        Map<String, List<GribForecastRainValueEntity>> datasfstMap = new HashMap<>();
//
////        逐时
//        String vtiStr = null;
//        for(GribForecastRainValueEntity data : gribForecastList)
//        {
//            vtiStr = String.valueOf(data.getVti());
//            if(!datasfstMap.containsKey(vtiStr))
//            {
//                datasfstMap.put(vtiStr, new ArrayList<>());
//            }
//            datasfstMap.get(vtiStr).add(data);
//        }
//
//        Map<String, double[]> result = new HashMap<>();
//        for(String vti : datasfstMap.keySet())
//        {
//            List<GribForecastRainValueEntity> gribForecastRainValueEntities = datasfstMap.get(vti);
//            double[] gribCalValue = getGribCalHourValue(obsDataMap, fstDataMap, gribForecastRainValueEntities);
//            result.put(vti, gribCalValue);
//        }
//        result.remove("0");
//
//
//        Map<String, Integer> methodIndex = new HashMap<>();
//        methodIndex.put("TS评分", 0);
//        methodIndex.put("晴雨准确率", 1);
//        methodIndex.put("空报率", 2);
//        methodIndex.put("漏报率", 3);
////      method          vti
//        Map<String, Map<String, Double>> resultMap = new LinkedHashMap<>();
//        for(String method : param.getMethods())
//        {
//            resultMap.put(method, new LinkedHashMap<>());
//            String vtiString = null;
//            for(int vti : param.getVtis())
//            {
//                if(vti == 0)
//                {
//                    continue;
//                }
//                vtiString = String.valueOf(vti);
//                if(result.size() == 0)
//                {
//                    resultMap.get(method).put(vtiString, null);
//                }
//                else
//                {
//                    resultMap.get(method).put(vtiString, result.get(vtiString) == null ? null : result.get(vtiString)[methodIndex.get(method)]);
//                }
//            }
//        }
//
//
//
//        return resultMap;
//    }
//    
//    
//    private Map<String, Map<String, Double>> calGribHourPre12(CheckDataParams param, List<ObsDataEntity> obsList, List<GribForecastRainValueEntity> gribForecastList)
//    {
////          station     datetime
//    	Map<String, Map<String, ObsDataEntity>> obsDataMap = new HashMap<>();
////          station     datetime_vti
//    	Map<String, Map<String, GribForecastRainValueEntity>> fstDataMap = new HashMap<>();
//    	String[] stations = param.getStations();
//    	for(String station : stations)
//    	{
//    		obsDataMap.put(station, new HashMap<>());
//    		fstDataMap.put(station, new HashMap<>());
//    	}
//    	
////    	Calendar calendar = Calendar.getInstance();
//    	for(ObsDataEntity obsData : obsList)
//    	{
////    		calendar.setTime(TimeUtil.String2Date(obsData.getDatetime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
////    		calendar.add(Calendar.HOUR_OF_DAY, 8);
//    		obsDataMap.get(obsData.getStation()).put(obsData.getDatetime(), obsData);
//    	}
//    	for(GribForecastRainValueEntity fstData : gribForecastList)
//    	{
//    		fstDataMap.get(fstData.getStation()).put(fstData.getDataTime() + "_" + (fstData.getVti()), fstData);
//    	}
//    	
//    	
////           validdate
//    	Map<String, List<GribForecastRainValueEntity>> datasfstMap = new HashMap<>();
//    	
////        逐时
//    	String vtiStr = null;
//    	for(GribForecastRainValueEntity data : gribForecastList)
//    	{
//    		vtiStr = String.valueOf(data.getVti() - 12);
//    		if(!datasfstMap.containsKey(vtiStr))
//    		{
//    			datasfstMap.put(vtiStr, new ArrayList<>());
//    		}
//    		datasfstMap.get(vtiStr).add(data);
//    	}
//    	
//    	Map<String, double[]> result = new HashMap<>();
//    	for(String vtiString : datasfstMap.keySet())
//    	{
//    		int vti = Integer.parseInt(vtiString);
//    		List<GribForecastRainValueEntity> gribForecastRainValueEntities = datasfstMap.get(vtiString);
//    		double[] gribCalValue = getGribCalHourValuePre12(vti, obsDataMap, fstDataMap, gribForecastRainValueEntities);
//    		if(vti == 228)
//    		{
//    			result.put("240", gribCalValue);
//    		}
//    		else
//    		{
//    			result.put(vtiString, gribCalValue);
//    		}
//    	}
//    	result.remove("0");
//    	
//    	Map<String, Integer> methodIndex = new HashMap<>();
//    	methodIndex.put("TS评分", 0);
//    	methodIndex.put("晴雨准确率", 1);
//    	methodIndex.put("空报率", 2);
//    	methodIndex.put("漏报率", 3);
////      method          vti
//    	Map<String, Map<String, Double>> resultMap = new LinkedHashMap<>();
//    	for(String method : param.getMethods())
//    	{
//    		resultMap.put(method, new LinkedHashMap<>());
//    		String vtiString = null;
//    		for(int vti : param.getVtis())
//    		{
//    			if(vti == 0)
//    			{
//    				continue;
//    			}
//    			vtiString = String.valueOf(vti);
//    			if(result.size() == 0)
//    			{
//    				resultMap.get(method).put(vtiString, null);
//    			}
//    			else
//    			{
//    				resultMap.get(method).put(vtiString, result.get(vtiString) == null ? null : result.get(vtiString)[methodIndex.get(method)]);
//    			}
//    		}
//    	}
//    	
//    	
//    	
//    	return resultMap;
//    }
//
//    @Override
//    public Map<String, Map<String, Map<String, Double>>> checkGribDataDay(CheckDataParams param) {
//        String tableName = tableNameMap.get(param.getDataSource() + "_rain_value");
//        param.setTableName(tableName);
//
//        String startTimeStr = param.getStartValidDate();
//        String endTimeStr = param.getEndValidDate();
//
//        Map<String, String> tableNamesMap = new HashMap<>();
//        if(param.getDataSources() != null && param.getDataSources().length != 0)
//        {
//            for(String dataSource : param.getDataSources())
//            {
//                tableNamesMap.put(dataSource, tableNameMap.get(dataSource + "_rain_value"));
//            }
//        }
//
//        if(param.getDataSourcesOrg() != null && param.getDataSourcesOrg().length != 0)
//        {
//            for(String dataSource : param.getDataSourcesOrg())
//            {
//                tableNamesMap.put(dataSource + "_org", tableNameMap.get(dataSource + "_value"));
//            }
//        }
//
//        if(param.getZone() != null)
//        {
//            ConfigParams params = new ConfigParams();
//            params.setZone(param.getZone());
//            List<StationEntity> stationEntities = null;
//            if(param.getZone() != 6)
//            {
//                stationEntities = stationForecastDataMapper.queryForecastStationsByZone(param);
//            }
//            else
//            {
//                stationEntities = stationForecastDataMapper.queryForecastAllStations(param);
//            }
//            int count = stationEntities.size();
//            String[] stations = new String[count];
//            for(int i = 0; i < count; i++)
//            {
//                stations[i] = stationEntities.get(i).getStation();
//            }
//            param.setStations(stations);
//        }
//        else
//        {
//            Double lonLeft = param.getLonLeft();
//            Double lonRight = param.getLonRight();
//            Double latUp = param.getLatUp();
//            Double latDown = param.getLatDown();
//            if(lonLeft != null && lonRight != null && latUp != null && latDown != null)
//            {
//                List<StationEntity> stationEntities = stationForecastDataMapper.queryForecastAllStationsLonLat(param);
//                List<String> stationList = new ArrayList<>();
//                for(StationEntity stationEntity : stationEntities)
//                {
//                    if(stationEntity.getLon() >= lonLeft && stationEntity.getLon() <= lonRight && stationEntity.getLat() <= latUp && stationEntity.getLat() >= latDown)
//                    {
//                        stationList.add(stationEntity.getStation());
//                    }
//                }
//                String[] stations = new String[stationList.size()];
//                for(int i = 0; i < stationList.size(); i++)
//                {
//                    stations[i] = stationList.get(i);
//                }
//                param.setStations(stations);
//            }
//        }
//
//        int vti = param.getVti();
//        if(param.getVtis() == null || param.getVtis().length == 0)
//        {
//            param.setVtis(new int[]{vti});
//        }
//        param.setVtis(new int[]{vti - 24, vti});
//        Calendar calendar = Calendar.getInstance();
//        calendar.setTime(TimeUtil.String2Date(param.getStartValidDate(), TimeUtil.DEFAULT_DATETIME_FORMAT));
//        calendar.add(Calendar.HOUR_OF_DAY, -vti);
//        param.setStartValidDate(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
//
//        int hour = Integer.parseInt(startTimeStr.split(" ")[1].split(":")[0]);
//        
//        long time = System.currentTimeMillis();
//
//        Map<String, List<GribForecastRainValueEntity>> orgDatasMap = new HashMap<>();
//        param.setEndValidDate(TimeUtil.addHours(param.getEndValidDate(), 24));
//        for(String dataSource : tableNamesMap.keySet())
//        {
//            param.setTableName(tableNamesMap.get(dataSource));
//            List<GribForecastRainValueEntity> gribForecastDataRainEntities = null;
//            if(dataSource.contains("_org"))
//            {
//            	if(hour == 8)
//            	{
//            		param.setHour(20);
//            	}
//            	else
//            	{
//            		param.setHour(8);
//            	}
//            	for(int i = 0, count = param.getVtis().length; i < count; i++)
//                {
//            		if(param.getVtis()[i] == 240)
//            		{
//            			continue;
//            		}
//                	param.getVtis()[i] += 12;
//                }
//            	param.setEndValidDate(TimeUtil.addHours(param.getEndValidDate(), 12));
//                gribForecastDataRainEntities = gribForecastDataMapper.queryGribForecastRainCheckDayOrg(param);
//                for(int i = 0, count = param.getVtis().length; i < count; i++)
//                {
//                	if(param.getVtis()[i] == 240)
//            		{
//            			continue;
//            		}
//                	param.getVtis()[i] -= 12;
//                }
//            }
//            else
//            {
//            	param.setHour(hour);
//                gribForecastDataRainEntities = gribForecastDataMapper.queryGribForecastRainCheckDay(param);
//            }
//            if(gribForecastDataRainEntities == null || gribForecastDataRainEntities.size() == 0)
//            {
//                continue;
//            }
//            
//            if(dataSource.equals(DataTypeEnum.ECMF.getDataType() + "_org"))
//            {
//            	for(GribForecastRainValueEntity data : gribForecastDataRainEntities)
//                {
//                    data.setRain(data.getRain() * 1000);
//                    data.setDataTime(TimeUtil.addHours(data.getDataTime(), -8));
//                    data.setValidDate(TimeUtil.addHours(data.getValidDate(), -8));
//                }
//            }
//            else
//            {
//            	for(GribForecastRainValueEntity data : gribForecastDataRainEntities)
//                {
//                    data.setDataTime(TimeUtil.addHours(data.getDataTime(), -8));
//                    data.setValidDate(TimeUtil.addHours(data.getValidDate(), -8));
//                }
//            }
//            
//            orgDatasMap.put(dataSource, gribForecastDataRainEntities);
//        }
//
//
//
//        System.out.println("查询预报数据耗时: " + (System.currentTimeMillis() - time));
//
//        calendar.setTime(TimeUtil.String2Date(startTimeStr, TimeUtil.DEFAULT_DATETIME_FORMAT));
//        calendar.add(Calendar.HOUR_OF_DAY, -vti);
//        calendar.add(Calendar.HOUR_OF_DAY, -8);
//        param.setStartTime(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
////        calendar.setTime(TimeUtil.String2Date(endTimeStr, TimeUtil.DEFAULT_DATETIME_FORMAT));
////        calendar.add(Calendar.HOUR_OF_DAY, -8);
////        param.setEndTime(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
//        param.setEndValidDate(TimeUtil.addHours(endTimeStr, -8));
//        param.setEndValidDate(TimeUtil.addHours(param.getEndValidDate(), 24));
//        param.setEndValidDate(TimeUtil.addHours(param.getEndValidDate(), 12));
//
//        time = System.currentTimeMillis();
//        List<ObsDataEntity> obsList = obsDataMapper.queryObsCheckData(param);
//        System.out.println("查询实况数据耗时: " + (System.currentTimeMillis() - time));
////        orgDatasMap.put("org", gribForecastList);
//
//        time = System.currentTimeMillis();
//        param.setStartTime(TimeUtil.addHours(startTimeStr, -8));
//        Map<String, Map<String, Map<String, Double>>> result = new HashMap<>();
//        for(String dataSource : orgDatasMap.keySet())
//        {
//            Map<String, Map<String, Double>> calGribHour = null;
//            if(dataSource.endsWith("_org"))
//            {
//            	calGribHour = calGribDayPre12(param, obsList, orgDatasMap.get(dataSource));
//            }
//            else
//            {
//            	calGribHour = calGribDay(param, obsList, orgDatasMap.get(dataSource));
//            }
//            result.put(dataSource, calGribHour);
//        }
//        System.out.println("计算耗时: " + (System.currentTimeMillis() - time));
//
//        time = System.currentTimeMillis();
//        calendar.setTime(TimeUtil.String2Date(startTimeStr, TimeUtil.DEFAULT_DATETIME_FORMAT));
//        Date startDate = calendar.getTime();
//        calendar.setTime(TimeUtil.String2Date(endTimeStr, TimeUtil.DEFAULT_DATETIME_FORMAT));
//        Date endDate = calendar.getTime();
//        List<String> dateList = new ArrayList<>();
//        for(int i = 0;; i++)
//        {
//            calendar.setTime(startDate);
//            calendar.add(Calendar.DATE, i);
//            if(calendar.getTime().after(endDate))
//            {
//                break;
//            }
//            dateList.add(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATE_FORMAT));
//        }
//
//        for(String dataSource : result.keySet())
//        {
//            for(String method : result.get(dataSource).keySet())
//            {
//                Map<String, Double> map = result.get(dataSource).get(method);
//                Map<String, Double> tt = new HashMap<>();
//                for(String dateStr : dateList)
//                {
//                    tt.put(dateStr, map.get(dateStr));
//                }
//                result.get(dataSource).put(method, tt);
//
//                List<String> list = result.get(dataSource).get(method).keySet().stream().sorted().collect(Collectors.toList());
//                Map<String, Double> temp = new LinkedHashMap<>();
//                for(String date : list)
//                {
//                    temp.put(date, result.get(dataSource).get(method).get(date));
//                }
//                result.get(dataSource).put(method, temp);
//            }
//        }
//
//        String next = result.keySet().iterator().next();
//
//        for(String dataSource : tableNamesMap.keySet())
//        {
//            if(!result.containsKey(dataSource))
//            {
//                result.put(dataSource, new LinkedHashMap<>());
//
//                for(String method : result.get(next).keySet())
//                {
//                    result.get(dataSource).put(method, new LinkedHashMap<>());
//                    for(String date : result.get(next).get(method).keySet())
//                    {
//                        result.get(dataSource).get(method).put(date, null);
//                    }
//                }
//            }
//        }
//        
//        String[] resultOrder = new String[]{"ecmf_org", "ecmf", "grapes_org", "grapes", "deep"};
//        Map<String, Map<String, Map<String, Double>>> resultMap = new LinkedHashMap<>();
//        for(String order : resultOrder)
//        {
//        	Map<String, Map<String, Double>> map = result.get(order);
//        	if(map != null)
//        	{
//        		resultMap.put(order, map);
//        	}
//        }
//        System.out.println("结果处理耗时: " + (System.currentTimeMillis() - time));
//
//        return resultMap;
//    }
//
//    private Map<String, Map<String, Double>> calGribDay(CheckDataParams param, List<ObsDataEntity> obsList, List<GribForecastRainValueEntity> gribForecastList)
//    {
////          station     datetime
//        Map<String, Map<String, ObsDataEntity>> obsDataMap = new HashMap<>();
////          station     datetime
//        Map<String, Map<String, GribForecastRainValueEntity>> fstDataMap = new HashMap<>();
//        String[] stations = param.getStations();
//        long time = System.currentTimeMillis();
//        for(String station : stations)
//        {
//            obsDataMap.put(station, new HashMap<>());
//            fstDataMap.put(station, new HashMap<>());
//        }
//        System.out.println("每种数据源预处理111111111111111耗时: " + (System.currentTimeMillis() - time));
//        
//        time = System.currentTimeMillis();
//
////        Calendar calendar = Calendar.getInstance();
//        Set<String> obsDateTimeSet = new HashSet<>();
//        for(ObsDataEntity obsData : obsList)
//        {
////            calendar.setTime(TimeUtil.String2Date(obsData.getDatetime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
////            calendar.add(Calendar.HOUR_OF_DAY, 8);
////            obsDataMap.get(obsData.getStation()).put(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT), obsData);
////            obsDataMap.get(obsData.getStation()).put(TimeUtil.addHours(obsData.getDatetime(), 8), obsData);
//            obsDataMap.get(obsData.getStation()).put(obsData.getDatetime(), obsData);
//            obsDateTimeSet.add(obsData.getDatetime());
//        }
//        System.out.println("每种数据源预处理222222222222222耗时: " + (System.currentTimeMillis() - time));
//        
//        time = System.currentTimeMillis();
//        for(GribForecastRainValueEntity fstData : gribForecastList)
//        {
//            fstDataMap.get(fstData.getStation()).put(fstData.getDataTime() + "_" + fstData.getVti(), fstData);
//        }
//        System.out.println("每种数据源预处理333333333333333耗时: " + (System.currentTimeMillis() - time));
//        
//        time = System.currentTimeMillis();
////           validdate
//        Map<String, List<GribForecastRainValueEntity>> datasfstMap = new HashMap<>();
////        逐日
//        String validDateStr = null;
//        List<String> validDateList = new ArrayList<>();
//        for(GribForecastRainValueEntity data : gribForecastList)
//        {
//            validDateStr = data.getValidDate().split(" ")[0];
//            if(!datasfstMap.containsKey(validDateStr))
//            {
//                datasfstMap.put(validDateStr, new ArrayList<>());
//            }
//            datasfstMap.get(validDateStr).add(data);
//        }
//        validDateList = datasfstMap.keySet().stream().sorted().collect(Collectors.toList());
//        
//        
//        System.out.println("每种数据源预处理444444444444444耗时: " + (System.currentTimeMillis() - time));
//        
//        time = System.currentTimeMillis();
//        Map<String, double[]> result = new HashMap<>();
//        for(int i = 1, count = validDateList.size(); i < count; i++)
//        {
//        	String validDate = validDateList.get(i);
//            List<GribForecastRainValueEntity> gribForecastRainValueEntities = datasfstMap.get(validDate);
//            double[] gribCalValue = getGribCalHourValue(obsDataMap, fstDataMap, gribForecastRainValueEntities);
//            validDate = validDateList.get(i - 1);
//            result.put(validDate, gribCalValue);
//        }
//        
//        System.out.println("每种数据源计算耗时: " + (System.currentTimeMillis() - time));
//
//        Map<String, Integer> methodIndex = new HashMap<>();
//        methodIndex.put("TS评分", 0);
//        methodIndex.put("晴雨准确率", 1);
//        methodIndex.put("空报率", 2);
//        methodIndex.put("漏报率", 3);
//        validDateList = validDateList.subList(0, validDateList.size() - 1);
//        Map<String, Map<String, Double>> resultMap = new LinkedHashMap<>();
//        for(String method : param.getMethods())
//        {
//            resultMap.put(method, new LinkedHashMap<>());
//            for(String validDate : validDateList)
//            {
//            	Double value = result.get(validDate)[methodIndex.get(method)];
//            	if(value == DecodeConstants.UNDEF_DOUBLE_VALUE)
//            	{
//            		value = null;
//            	}
//                resultMap.get(method).put(validDate, value);
//            }
////            List<String> list = resultMap.get(method).keySet().stream().sorted().collect(Collectors.toList());
////            Map<String, Double> temp = new LinkedHashMap<>();
////            for(String date : list)
////            {
////                temp.put(date, resultMap.get(method).get(date));
////            }
////            resultMap.put(method, temp);
//        }
//
//        return resultMap;
//    }
//    
//    private Map<String, Map<String, Double>> calGribDayPre12(CheckDataParams param, List<ObsDataEntity> obsList, List<GribForecastRainValueEntity> gribForecastList)
//    {
////          station     datetime
//    	Map<String, Map<String, ObsDataEntity>> obsDataMap = new HashMap<>();
////          station     datetime
//    	Map<String, Map<String, GribForecastRainValueEntity>> fstDataMap = new HashMap<>();
//    	String[] stations = param.getStations();
//    	long time = System.currentTimeMillis();
//    	for(String station : stations)
//    	{
//    		obsDataMap.put(station, new HashMap<>());
//    		fstDataMap.put(station, new HashMap<>());
//    	}
//    	System.out.println("每种数据源预处理111111111111111耗时: " + (System.currentTimeMillis() - time));
//    	
//    	time = System.currentTimeMillis();
//    	
////        Calendar calendar = Calendar.getInstance();
//    	Set<String> obsDateTimeSet = new HashSet<>();
//    	for(ObsDataEntity obsData : obsList)
//    	{
//    		obsDataMap.get(obsData.getStation()).put(obsData.getDatetime(), obsData);
//    		obsDateTimeSet.add(obsData.getDatetime());
//    	}
//    	System.out.println("每种数据源预处理222222222222222耗时: " + (System.currentTimeMillis() - time));
//    	
//    	time = System.currentTimeMillis();
//    	for(GribForecastRainValueEntity fstData : gribForecastList)
//    	{
//    		fstDataMap.get(fstData.getStation()).put(fstData.getDataTime() + "_" + fstData.getVti(), fstData);
//    	}
//    	System.out.println("每种数据源预处理333333333333333耗时: " + (System.currentTimeMillis() - time));
//    	
//    	time = System.currentTimeMillis();
////           validdate
//    	Map<String, List<GribForecastRainValueEntity>> datasfstMap = new HashMap<>();
////        逐日
//    	String validDateStr = null;
//    	List<String> validDateList = new ArrayList<>();
//    	for(GribForecastRainValueEntity data : gribForecastList)
//    	{
//    		validDateStr = data.getValidDate().split(" ")[0];
//    		if(!datasfstMap.containsKey(validDateStr))
//    		{
//    			datasfstMap.put(validDateStr, new ArrayList<>());
//    		}
//    		datasfstMap.get(validDateStr).add(data);
//    	}
//    	validDateList = datasfstMap.keySet().stream().sorted().collect(Collectors.toList());
//    	
//    	
//    	System.out.println("每种数据源预处理444444444444444耗时: " + (System.currentTimeMillis() - time));
//    	
//    	time = System.currentTimeMillis();
//    	Map<String, double[]> result = new HashMap<>();
//    	for(int i = 1, count = validDateList.size(); i < count; i++)
//    	{
//    		String validDate = validDateList.get(i);
//    		List<GribForecastRainValueEntity> gribForecastRainValueEntities = datasfstMap.get(validDate);
//    		double[] gribCalValue = null;
//    		if(param.getVti() == 240)
//    		{
//    			gribCalValue = getGribCalHourValuePre12(obsDataMap, fstDataMap, gribForecastRainValueEntities);
//    		}
//    		else
//    		{
//    			gribCalValue = getGribCalHourValue(obsDataMap, fstDataMap, gribForecastRainValueEntities);
//    		}
//    		validDate = validDateList.get(i - 1);
//    		result.put(validDate, gribCalValue);
//    	}
//    	
//    	System.out.println("每种数据源计算耗时: " + (System.currentTimeMillis() - time));
//    	
//    	Map<String, Integer> methodIndex = new HashMap<>();
//    	methodIndex.put("TS评分", 0);
//    	methodIndex.put("晴雨准确率", 1);
//    	methodIndex.put("空报率", 2);
//    	methodIndex.put("漏报率", 3);
//    	validDateList = validDateList.subList(0, validDateList.size() - 1);
//    	Map<String, Map<String, Double>> resultMap = new LinkedHashMap<>();
//    	for(String method : param.getMethods())
//    	{
//    		resultMap.put(method, new LinkedHashMap<>());
//    		for(String validDate : validDateList)
//    		{
//    			Double value = result.get(validDate)[methodIndex.get(method)];
//    			if(value == DecodeConstants.UNDEF_DOUBLE_VALUE)
//    			{
//    				value = null;
//    			}
//    			resultMap.get(method).put(validDate, value);
//    		}
//    	}
//    	
//    	return resultMap;
//    }
//
//    private double[] getStationCalHourValue(Map<String, Map<String, ObsDataEntity>> obsDataMap, Map<String, Map<String, StationForecastDataEntity>> fstDataMap, List<StationForecastDataEntity> rainValueList)
//    {
//        double h = 0;
//        double tn = 0;
//        double f = 0;
//        double m = 0;
//        double th = 0;
//        double tm = 0;
//        double tf = 0;
//        double ts = 0;
//        ObsDataEntity obsValue = null;
//        double[] result = new double[4];
////        String time = "";
////        Calendar calendar = Calendar.getInstance();
//        for(StationForecastDataEntity rainValue : rainValueList)
//        {
////        	calendar.setTime(TimeUtil.String2Date(rainValue.getValiddate(), TimeUtil.DEFAULT_DATETIME_FORMAT));
////        	calendar.add(Calendar.HOUR_OF_DAY, 24);
////            obsValue = obsDataMap.get(rainValue.getStation()).get(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
//            obsValue = obsDataMap.get(rainValue.getStation()).get(rainValue.getValiddate());
////            System.out.println(JSONObject.toJSONString(obsValue));
//            if(obsValue == null || rainValue.getVti() == 0)
//            {
//                continue;
//            }
//            double value = obsValue.getRain24() == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE ? 0 : obsValue.getRain24();
////            time = obsValue.getDatetime();
//            double fstValue;
//            int preVti = rainValue.getVti() - 24;
////            preVti = preVti == 0 ? 24 : preVti;
////            int preVti = rainValue.getVti();
//            StationForecastDataEntity dataEntity = fstDataMap.get(obsValue.getStation()).get(rainValue.getDatatime() + "_" + preVti);
//            if(preVti == 0)
//            {
//            	dataEntity = new StationForecastDataEntity();
//            	dataEntity.setRain24(0);
//            }
//            
//            if(fstDataMap.get(obsValue.getStation()) == null || dataEntity == null)
//            {
//            	fstValue = 999999;
//            }
//            else
//            {
//                fstValue = rainValue.getRain24() - dataEntity.getRain24();
//            }
//            if(fstValue == 999999)
//            {
//                continue;
//            }
////            System.out.println("obsDate: " + obsValue.getDatetime() + " fstDate: " + rainValue.getValiddate() + " fstTime: " + rainValue.getDatatime() + " vti: " + rainValue.getVti() + " value: " + value + ", fstValue: " + fstValue);
//            if(value >= 0.1 && fstValue >= 0.1)
//            {
//                h++;
//            }
//            else if(value < 0.1 && fstValue < 0.1)
//            {
//                tn++;
//            }
//            else if(value >= 0.1 && fstValue < 0.1)
//            {
//                m++;
//            }
//            else if(value < 0.1 && fstValue >= 0.1)
//            {
//                f++;
//            }
//        }
//        ts = h / ((h + m + f) == 0 ? 1 : (h + m + f));
//        th = (h + tn) / ((h + f + m + tn) == 0 ? 1 : (h + f + m + tn));
//        tf = f / ((f + h) == 0 ? 1 : (f + h));
//        tm = m / ((m + tn) == 0 ? 1 : (tn + m));
//        System.out.println("h:" + h + ",tn:" + tn + ",m:" + m + ",f:" + f);
//        result[0] = NumberFormatUtil.numFormat(ts, 3);   // TS评分
//        result[1] = NumberFormatUtil.numFormat(th, 3);   // 准确率
//        result[2] = NumberFormatUtil.numFormat(tf, 3);   // 空报
//        result[3] = NumberFormatUtil.numFormat(tm, 3);   // 漏报
//        if(h == 0 && tn == 0 && m == 0 && f == 0)
//        {
//        	result[0] = DecodeConstants.UNDEF_DOUBLE_VALUE;
//        	result[1] = DecodeConstants.UNDEF_DOUBLE_VALUE;
//        	result[2] = DecodeConstants.UNDEF_DOUBLE_VALUE;
//        	result[3] = DecodeConstants.UNDEF_DOUBLE_VALUE;
//        }
////        System.out.println("date: " + time + " rate: " + result[1]);
//        
//        return result;
//    }
//    
//    private double[] getStationCalHourValuePre12(Map<String, Map<String, ObsDataEntity>> obsDataMap, Map<String, Map<String, StationForecastDataEntity>> fstDataMap, List<StationForecastDataEntity> rainValueList)
//    {
//        double h = 0;
//        double tn = 0;
//        double f = 0;
//        double m = 0;
//        double th = 0;
//        double tm = 0;
//        double tf = 0;
//        double ts = 0;
//        ObsDataEntity obsValue = null;
//        double[] result = new double[4];
////        String time = "";
////        Calendar calendar = Calendar.getInstance();
//        for(StationForecastDataEntity rainValue : rainValueList)
//        {
////        	calendar.setTime(TimeUtil.String2Date(rainValue.getValiddate(), TimeUtil.DEFAULT_DATETIME_FORMAT));
////        	calendar.add(Calendar.HOUR_OF_DAY, 24);
////            obsValue = obsDataMap.get(rainValue.getStation()).get(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
//            obsValue = obsDataMap.get(rainValue.getStation()).get(rainValue.getValiddate());
////            System.out.println(JSONObject.toJSONString(obsValue));
//            if(obsValue == null || rainValue.getVti() == 0)
//            {
//                continue;
//            }
//            double value = obsValue.getRain24() == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE ? 0 : obsValue.getRain24();
////            time = obsValue.getDatetime();
//            double fstValue;
//            int preVti = rainValue.getVti() - 12;
////            preVti = preVti == 0 ? 24 : preVti;
////            int preVti = rainValue.getVti();
//            StationForecastDataEntity dataEntity = fstDataMap.get(obsValue.getStation()).get(rainValue.getDatatime() + "_" + preVti);
//            if(preVti == 0)
//            {
//            	dataEntity = new StationForecastDataEntity();
//            	dataEntity.setRain24(0);
//            }
//            
//            if(fstDataMap.get(obsValue.getStation()) == null || dataEntity == null)
//            {
//            	fstValue = 999999;
//            }
//            else
//            {
//                fstValue = rainValue.getRain24() - dataEntity.getRain24();
//            }
//            if(fstValue == 999999)
//            {
//                continue;
//            }
////            System.out.println("obsDate: " + obsValue.getDatetime() + " fstDate: " + rainValue.getValiddate() + " fstTime: " + rainValue.getDatatime() + " vti: " + rainValue.getVti() + " value: " + value + ", fstValue: " + fstValue);
//            if(value >= 0.1 && fstValue >= 0.1)
//            {
//                h++;
//            }
//            else if(value < 0.1 && fstValue < 0.1)
//            {
//                tn++;
//            }
//            else if(value >= 0.1 && fstValue < 0.1)
//            {
//                m++;
//            }
//            else if(value < 0.1 && fstValue >= 0.1)
//            {
//                f++;
//            }
//        }
//        ts = h / ((h + m + f) == 0 ? 1 : (h + m + f));
//        th = (h + tn) / ((h + f + m + tn) == 0 ? 1 : (h + f + m + tn));
//        tf = f / ((f + h) == 0 ? 1 : (f + h));
//        tm = m / ((m + tn) == 0 ? 1 : (tn + m));
//        System.out.println("h:" + h + ",tn:" + tn + ",m:" + m + ",f:" + f);
//        result[0] = NumberFormatUtil.numFormat(ts, 3);   // TS评分
//        result[1] = NumberFormatUtil.numFormat(th, 3);   // 准确率
//        result[2] = NumberFormatUtil.numFormat(tf, 3);   // 空报
//        result[3] = NumberFormatUtil.numFormat(tm, 3);   // 漏报
//        if(h == 0 && tn == 0 && m == 0 && f == 0)
//        {
//        	result[0] = DecodeConstants.UNDEF_DOUBLE_VALUE;
//        	result[1] = DecodeConstants.UNDEF_DOUBLE_VALUE;
//        	result[2] = DecodeConstants.UNDEF_DOUBLE_VALUE;
//        	result[3] = DecodeConstants.UNDEF_DOUBLE_VALUE;
//        }
////        System.out.println("date: " + time + " rate: " + result[1]);
//        
//        return result;
//    }
//    
//    private double[] getStationCalHourValuePre12(int vti, Map<String, Map<String, ObsDataEntity>> obsDataMap, Map<String, Map<String, StationForecastDataEntity>> fstDataMap, List<StationForecastDataEntity> rainValueList)
//    {
//        double h = 0;
//        double tn = 0;
//        double f = 0;
//        double m = 0;
//        double th = 0;
//        double tm = 0;
//        double tf = 0;
//        double ts = 0;
//        ObsDataEntity obsValue = null;
//        double[] result = new double[4];
////        String time = "";
////        Calendar calendar = Calendar.getInstance();
//        for(StationForecastDataEntity rainValue : rainValueList)
//        {
////        	calendar.setTime(TimeUtil.String2Date(rainValue.getValiddate(), TimeUtil.DEFAULT_DATETIME_FORMAT));
////        	calendar.add(Calendar.HOUR_OF_DAY, 24);
////            obsValue = obsDataMap.get(rainValue.getStation()).get(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
//            obsValue = obsDataMap.get(rainValue.getStation()).get(rainValue.getValiddate());
////            System.out.println(JSONObject.toJSONString(obsValue));
//            if(obsValue == null || rainValue.getVti() == 0)
//            {
//                continue;
//            }
//            double value = obsValue.getRain24() == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE ? 0 : obsValue.getRain24();
////            time = obsValue.getDatetime();
//            double fstValue;
//            int preVti = rainValue.getVti() - (vti == 240 ? 12 : 24);
////            preVti = preVti == 0 ? 24 : preVti;
////            int preVti = rainValue.getVti();
//            StationForecastDataEntity dataEntity = fstDataMap.get(obsValue.getStation()).get(rainValue.getDatatime() + "_" + preVti);
//            if(preVti == 0)
//            {
//            	dataEntity = new StationForecastDataEntity();
//            	dataEntity.setRain24(0);
//            }
//            
//            if(fstDataMap.get(obsValue.getStation()) == null || dataEntity == null)
//            {
//            	fstValue = 999999;
//            }
//            else
//            {
//                fstValue = rainValue.getRain24() - dataEntity.getRain24();
//            }
//            if(fstValue == 999999)
//            {
//                continue;
//            }
////            System.out.println("obsDate: " + obsValue.getDatetime() + " fstDate: " + rainValue.getValiddate() + " fstTime: " + rainValue.getDatatime() + " vti: " + rainValue.getVti() + " value: " + value + ", fstValue: " + fstValue);
//            if(value >= 0.1 && fstValue >= 0.1)
//            {
//                h++;
//            }
//            else if(value < 0.1 && fstValue < 0.1)
//            {
//                tn++;
//            }
//            else if(value >= 0.1 && fstValue < 0.1)
//            {
//                m++;
//            }
//            else if(value < 0.1 && fstValue >= 0.1)
//            {
//                f++;
//            }
//        }
//        ts = h / ((h + m + f) == 0 ? 1 : (h + m + f));
//        th = (h + tn) / ((h + f + m + tn) == 0 ? 1 : (h + f + m + tn));
//        tf = f / ((f + h) == 0 ? 1 : (f + h));
//        tm = m / ((m + tn) == 0 ? 1 : (tn + m));
//        System.out.println("h:" + h + ",tn:" + tn + ",m:" + m + ",f:" + f);
//        result[0] = NumberFormatUtil.numFormat(ts, 3);   // TS评分
//        result[1] = NumberFormatUtil.numFormat(th, 3);   // 准确率
//        result[2] = NumberFormatUtil.numFormat(tf, 3);   // 空报
//        result[3] = NumberFormatUtil.numFormat(tm, 3);   // 漏报
//        if(h == 0 && tn == 0 && m == 0 && f == 0)
//        {
//        	result[0] = DecodeConstants.UNDEF_DOUBLE_VALUE;
//        	result[1] = DecodeConstants.UNDEF_DOUBLE_VALUE;
//        	result[2] = DecodeConstants.UNDEF_DOUBLE_VALUE;
//        	result[3] = DecodeConstants.UNDEF_DOUBLE_VALUE;
//        }
////        System.out.println("date: " + time + " rate: " + result[1]);
//        
//        return result;
//    }
//    
//    private double[] getGribCalHourValue(Map<String, Map<String, ObsDataEntity>> obsDataMap, Map<String, Map<String, GribForecastRainValueEntity>> fstDataMap, List<GribForecastRainValueEntity> rainValueList)
//    {
//        double h = 0;
//        double tn = 0;
//        double f = 0;
//        double m = 0;
//        double th = 0;
//        double tm = 0;
//        double tf = 0;
//        double ts = 0;
//        ObsDataEntity obsValue = null;
//        double[] result = new double[4];
//        for(GribForecastRainValueEntity rainValue : rainValueList)
//        {
//            obsValue = obsDataMap.get(rainValue.getStation()).get(rainValue.getValidDate());
//            if(obsValue == null || rainValue.getVti() == 0)
//            {
//                continue;
//            }
//            double value = obsValue.getRain24() == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE ? 0 : obsValue.getRain24();
//            int preVti = rainValue.getVti() - 24;
////            preVti = (preVti == 0 ? 24 : preVti);
//            if(fstDataMap.get(rainValue.getStation()) == null || fstDataMap.get(rainValue.getStation()).get(rainValue.getDataTime() + "_" + preVti) == null)
//            {
//                continue;
//            }
//            double fstValue = rainValue.getRain() - fstDataMap.get(rainValue.getStation()).get(rainValue.getDataTime() + "_" + preVti).getRain();
////            System.out.println("obsDate: " + obsValue.getDatetime() + " fstDate: " + rainValue.getValidDate() + " fstDataTime: " + rainValue.getDataTime() + " vti: " + rainValue.getVti() + " value: " + value + ", fstValue: " + fstValue);
//            if(fstValue >= 9999)
//            {
//                continue;
//            }
//            if(value >= 0.1 && fstValue >= 0.1)
//            {
//                h++;
//            }
//            else if(value < 0.1 && fstValue < 0.1)
//            {
//                tn++;
//            }
//            else if(value >= 0.1 && fstValue < 0.1)
//            {
//                m++;
//            }
//            else if(value < 0.1 && fstValue >= 0.1)
//            {
//                f++;
//            }
//        }
////        System.out.println("h:" + h + ",tn:" + tn + ",m:" + m + ",f:" + f);
//        ts = h / ((h + m + f) == 0 ? 1 : (h + m + f));
//        th = (h + tn) / ((h + f + m + tn) == 0 ? 1 : (h + f + m + tn));
//        tf = f / ((f + h) == 0 ? 1 : (f + h));
//        tm = m / ((m + tn) == 0 ? 1 : (tn + m));
//        result[0] = NumberFormatUtil.numFormat(ts, 3);   // TS评分
//        result[1] = NumberFormatUtil.numFormat(th, 3);   // 准确率
//        result[2] = NumberFormatUtil.numFormat(tf, 3);   // 空报
//        result[3] = NumberFormatUtil.numFormat(tm, 3);   // 漏报
//        if(h == 0 && tn == 0 && m == 0 && f == 0)
//        {
//        	result[0] = DecodeConstants.UNDEF_DOUBLE_VALUE;
//        	result[1] = DecodeConstants.UNDEF_DOUBLE_VALUE;
//        	result[2] = DecodeConstants.UNDEF_DOUBLE_VALUE;
//        	result[3] = DecodeConstants.UNDEF_DOUBLE_VALUE;
//        }
//
//        return result;
//    }
//    private double[] getGribCalHourValuePre12(Map<String, Map<String, ObsDataEntity>> obsDataMap, Map<String, Map<String, GribForecastRainValueEntity>> fstDataMap, List<GribForecastRainValueEntity> rainValueList)
//    {
//    	double h = 0;
//    	double tn = 0;
//    	double f = 0;
//    	double m = 0;
//    	double th = 0;
//    	double tm = 0;
//    	double tf = 0;
//    	double ts = 0;
//    	ObsDataEntity obsValue = null;
//    	double[] result = new double[4];
//    	for(GribForecastRainValueEntity rainValue : rainValueList)
//    	{
//    		obsValue = obsDataMap.get(rainValue.getStation()).get(rainValue.getValidDate());
//    		if(obsValue == null || rainValue.getVti() == 0)
//    		{
//    			continue;
//    		}
//    		double value = obsValue.getRain24() == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE ? 0 : obsValue.getRain24();
//    		int preVti = rainValue.getVti() - 12;
//			//            preVti = (preVti == 0 ? 24 : preVti);
//			if(fstDataMap.get(rainValue.getStation()) == null || fstDataMap.get(rainValue.getStation()).get(rainValue.getDataTime() + "_" + preVti) == null)
//			{
//				continue;
//			}
//			double fstValue = rainValue.getRain() - fstDataMap.get(rainValue.getStation()).get(rainValue.getDataTime() + "_" + preVti).getRain();
////			System.out.println("obsDate: " + obsValue.getDatetime() + " fstDate: " + rainValue.getValidDate() + " fstDataTime: " + rainValue.getDataTime() + " vti: " + rainValue.getVti() + " value: " + value + ", fstValue: " + fstValue);
//			if(fstValue >= 9999)
//			{
//				continue;
//			}
//			if(value >= 0.1 && fstValue >= 0.1)
//			{
//				h++;
//			}
//			else if(value < 0.1 && fstValue < 0.1)
//			{
//				tn++;
//			}
//			else if(value >= 0.1 && fstValue < 0.1)
//			{
//				m++;
//			}
//			else if(value < 0.1 && fstValue >= 0.1)
//			{
//				f++;
//			}
//    	}
////        System.out.println("h:" + h + ",tn:" + tn + ",m:" + m + ",f:" + f);
//    	ts = h / ((h + m + f) == 0 ? 1 : (h + m + f));
//    	th = (h + tn) / ((h + f + m + tn) == 0 ? 1 : (h + f + m + tn));
//    	tf = f / ((f + h) == 0 ? 1 : (f + h));
//    	tm = m / ((m + tn) == 0 ? 1 : (tn + m));
//    	result[0] = NumberFormatUtil.numFormat(ts, 3);   // TS评分
//    	result[1] = NumberFormatUtil.numFormat(th, 3);   // 准确率
//    	result[2] = NumberFormatUtil.numFormat(tf, 3);   // 空报
//    	result[3] = NumberFormatUtil.numFormat(tm, 3);   // 漏报
//    	if(h == 0 && tn == 0 && m == 0 && f == 0)
//    	{
//    		result[0] = DecodeConstants.UNDEF_DOUBLE_VALUE;
//    		result[1] = DecodeConstants.UNDEF_DOUBLE_VALUE;
//    		result[2] = DecodeConstants.UNDEF_DOUBLE_VALUE;
//    		result[3] = DecodeConstants.UNDEF_DOUBLE_VALUE;
//    	}
//    	
//    	return result;
//    }
//    
//    
//    private double[] getGribCalHourValuePre12(int vti, Map<String, Map<String, ObsDataEntity>> obsDataMap, Map<String, Map<String, GribForecastRainValueEntity>> fstDataMap, List<GribForecastRainValueEntity> rainValueList)
//    {
//        double h = 0;
//        double tn = 0;
//        double f = 0;
//        double m = 0;
//        double th = 0;
//        double tm = 0;
//        double tf = 0;
//        double ts = 0;
//        ObsDataEntity obsValue = null;
//        double[] result = new double[4];
//        for(GribForecastRainValueEntity rainValue : rainValueList)
//        {
//        	double fstValue = 0;
//        	double value = 0;
//        	if(vti == 228)
//        	{
//        		fstValue = rainValue.getRain() - fstDataMap.get(rainValue.getStation()).get(rainValue.getDataTime() + "_" + 228).getRain();
//        		Map<String, ObsDataEntity> map = obsDataMap.get(rainValue.getStation());
//        		String startDate = TimeUtil.addHours(rainValue.getValidDate(), -12);
//        		double sumRain = 0;
//        		for(String date : map.keySet())
//        		{
//        			if(date.compareTo(startDate) >= 0 && date.compareTo(rainValue.getValidDate()) <= 0)
//        			{
//        				sumRain += map.get(date).getRain();
//        			}
//        		}
//        		value = sumRain;
//        	}
//        	else
//        	{
//        		obsValue = obsDataMap.get(rainValue.getStation()).get(rainValue.getValidDate());
//        		if(obsValue == null || rainValue.getVti() == 0)
//        		{
//        			continue;
//        		}
//        		value = obsValue.getRain24() == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE ? 0 : obsValue.getRain24();
//        		int preVti = rainValue.getVti() - 24;
////            preVti = (preVti == 0 ? 24 : preVti);
//        		if(fstDataMap.get(rainValue.getStation()) == null || fstDataMap.get(rainValue.getStation()).get(rainValue.getDataTime() + "_" + preVti) == null)
//        		{
//        			continue;
//        		}
//        		fstValue = rainValue.getRain() - fstDataMap.get(rainValue.getStation()).get(rainValue.getDataTime() + "_" + preVti).getRain();
////        		System.out.println("obsDate: " + obsValue.getDatetime() + " fstDate: " + rainValue.getValidDate() + " fstDataTime: " + rainValue.getDataTime() + " vti: " + rainValue.getVti() + " value: " + value + ", fstValue: " + fstValue);
//        	}
//            if(fstValue >= 9999)
//            {
//                continue;
//            }
//            if(value >= 0.1 && fstValue >= 0.1)
//            {
//                h++;
//            }
//            else if(value < 0.1 && fstValue < 0.1)
//            {
//                tn++;
//            }
//            else if(value >= 0.1 && fstValue < 0.1)
//            {
//                m++;
//            }
//            else if(value < 0.1 && fstValue >= 0.1)
//            {
//                f++;
//            }
//        }
////        System.out.println("h:" + h + ",tn:" + tn + ",m:" + m + ",f:" + f);
//        ts = h / ((h + m + f) == 0 ? 1 : (h + m + f));
//        th = (h + tn) / ((h + f + m + tn) == 0 ? 1 : (h + f + m + tn));
//        tf = f / ((f + h) == 0 ? 1 : (f + h));
//        tm = m / ((m + tn) == 0 ? 1 : (tn + m));
//        result[0] = NumberFormatUtil.numFormat(ts, 3);   // TS评分
//        result[1] = NumberFormatUtil.numFormat(th, 3);   // 准确率
//        result[2] = NumberFormatUtil.numFormat(tf, 3);   // 空报
//        result[3] = NumberFormatUtil.numFormat(tm, 3);   // 漏报
//        if(h == 0 && tn == 0 && m == 0 && f == 0)
//        {
//        	result[0] = DecodeConstants.UNDEF_DOUBLE_VALUE;
//        	result[1] = DecodeConstants.UNDEF_DOUBLE_VALUE;
//        	result[2] = DecodeConstants.UNDEF_DOUBLE_VALUE;
//        	result[3] = DecodeConstants.UNDEF_DOUBLE_VALUE;
//        }
//
//        return result;
//    }
//
//    private double getValue(String element, String vti, String method, List<ObsDataEntity> obsDataList, List<StationForecastDataEntity> fstDataList)
//    {
//        Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
//        List<ObsDataEntity> obsDataListSub = new ArrayList<>();
//        Map<String, List<Object>> obsMapList = new HashMap<>();
//        for(StationForecastDataEntity fstData : fstDataList)
//        {
//            for(ObsDataEntity obsData : obsDataList)
//            {
//                if(fstData.getValiddate().compareTo(obsData.getDatetime()) == 0)
//                {
//                    obsDataListSub.add(obsData);
//                    ArrayList<Object> objects = new ArrayList<>();
//                    objects.add(obsData);
//                    objects.add(fstData);
//                    obsMapList.put(fstData.getValiddate(), objects);
//                }
//            }
//        }
//        int count = obsDataList.size() == 0 ? 1 : obsDataList.size();
//        double sum = 0;
//        if("RMSE".equals(method))
//        {
//            for(String key : obsMapList.keySet())
//            {
//                List<Object> objects = obsMapList.get(key);
//                double obs = getDoubleValueFromObject(objects.get(0), element);
//                double fst = getDoubleValueFromObject(objects.get(1), element);
//                sum += Math.pow(obs - fst, 2);
//            }
////            for(int i = 0; i < count; i++)
////            {
////                double obs = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(obsDataList.get(i), element)));
////                double fst = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(fstDataList.get(i), element)));
////                sum += Math.pow(obs - fst, 2);
////            }
//            sum = Math.sqrt(sum / count);
//        }
//        else if("MAE".equals(method))
//        {
//            for(String key : obsMapList.keySet())
//            {
//                List<Object> objects = obsMapList.get(key);
//                double obs = getDoubleValueFromObject(objects.get(0), element);
//                double fst = getDoubleValueFromObject(objects.get(1), element);
//                sum += Math.abs(obs - fst);
//            }
////            for(int i = 0; i < count; i++)
////            {
////                double obs = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(obsDataList.get(i), element)));
////                double fst = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(fstDataList.get(i), element)));
////                sum += Math.abs(obs - fst);
////            }
//            sum = sum / count;
//        }
//        else if("CORR".equals(method))
//        {
//            double obsSum = 0;
//            double fstSum = 0;
//            for(String key : obsMapList.keySet())
//            {
//                List<Object> objects = obsMapList.get(key);
//                double obs = getDoubleValueFromObject(objects.get(0), element);
//                double fst = getDoubleValueFromObject(objects.get(1), element);
//                obsSum += obs;
//                fstSum += fst;
//            }
////            for(int i = 0; i < count; i++)
////            {
////                double obs = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(obsDataList.get(i), element)));
////                double fst = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(fstDataList.get(i), element)));
////                obsSum += obs;
////                fstSum += fst;
////            }
//            double obsAvg = obsSum / count;
//            double fstAvg = fstSum / count;
//
//            double upSum = 0;
//            double downSum = 0;
//            for(String key : obsMapList.keySet())
//            {
//                List<Object> objects = obsMapList.get(key);
//                double obs = getDoubleValueFromObject(objects.get(0), element);
//                double fst = getDoubleValueFromObject(objects.get(1), element);
//                upSum += (obs - obsAvg) * (fst - fstAvg);
//                downSum += Math.sqrt(Math.pow(obs - obsAvg, 2)) * Math.sqrt(Math.pow(fst - fstAvg, 2));
//            }
////            for(int i = 0; i < count; i++)
////            {
////                double obs = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(obsDataList.get(i), element)));
////                double fst = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(fstDataList.get(i), element)));
////                upSum += (obs - obsAvg) * (fst - fstAvg);
////                downSum += Math.sqrt(Math.pow(obs - obsAvg, 2)) * Math.sqrt(Math.pow(fst - fstAvg, 2));
////            }
//            sum = upSum / downSum;
//        }
//        else if("准确率".equals(method))
//        {
//            double dis = Double.parseDouble(configMap.get(element));
//            int correct = 0;
//            for(String key : obsMapList.keySet())
//            {
//                List<Object> objects = obsMapList.get(key);
//                double obs = getDoubleValueFromObject(objects.get(0), element);
//                double fst = getDoubleValueFromObject(objects.get(1), element);
//                if(Math.abs(obs - fst) < dis)
//                {
//                    correct++;
//                }
//            }
////            for(int i = 0; i < count; i++)
////            {
////                double obs = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(obsDataList.get(i), element)));
////                double fst = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(fstDataList.get(i), element)));
////                if(Math.abs(obs - fst) < dis)
////                {
////                    correct++;
////                }
////            }
//            sum = correct / count;
//        }
//        sum = NumberFormatUtil.numFormat(sum, 3);
//
//        return sum;
//    }
//    
//    private double getStationValue(String element, String method, Map<String, Map<String, ObsDataEntity>> obsDataMap, List<StationForecastDataEntity> fstDataList)
//    {
//        Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
//        Map<String, List<Object>> obsMapList = new HashMap<>();
//        Map<String, Map<String, List<Object>>> dataStationDateMapList = new HashMap<>();
//        
//        if(element.equals("atmax") || element.equals("atmin"))
//        {
//        	for(StationForecastDataEntity fstData : fstDataList)
//            {
//        		String validDate = fstData.getValiddate().split(" ")[0];
//                ObsDataEntity obsData = obsDataMap.get(fstData.getStation()).get(fstData.getValiddate());
//                if(!dataStationDateMapList.containsKey(fstData.getStation()))
//                {
//                	dataStationDateMapList.put(fstData.getStation(), new HashMap<>());
//                }
//                if(!dataStationDateMapList.get(fstData.getStation()).containsKey(validDate))
//                {
//                	dataStationDateMapList.get(fstData.getStation()).put(validDate, new ArrayList<>());
//                }
//                dataStationDateMapList.get(fstData.getStation()).get(validDate).add(obsData);
//                dataStationDateMapList.get(fstData.getStation()).get(validDate).add(fstData);
//                
//            }
//        	
//        	for(String station : dataStationDateMapList.keySet())
//        	{
//        		List<Object> listObjs = new ArrayList<>();
//        		for(String date : dataStationDateMapList.get(station).keySet())
//        		{
//        			List<Object> list = dataStationDateMapList.get(station).get(date);
//            		double atMaxObs = -9999;
//            		double atMinObs = 9999;
//            		double atMaxFst = -9999;
//            		double atMinFst = 9999;
//            		String obsDate = null;
//            		String fstDate = null;
//            		String fstHour = null;
//            		String vtiStr = null;
//            		for(int i = 0, num = list.size(); i < num - 1; i = i + 2)
//                    {
//            			if(list.get(i) == null || list.get(i + 1) == null)
//                    	{
//                        	continue;
//                    	}
//            			double obs = getDoubleValueFromObject(list.get(i), "at");
//                    	double fst = getDoubleValueFromObject(list.get(i + 1), "at");
//                    	if(atMaxObs < obs)
//                    	{
//                    		atMaxObs = obs;
//                    	}
//                    	if(atMinObs > obs)
//                    	{
//                    		atMinObs = obs;
//                    	}
//                    	if(atMaxFst < fst)
//                    	{
//                    		atMaxFst = fst;
//                    	}
//                    	if(atMinFst > fst)
//                    	{
//                    		atMinFst = fst;
//                    	}
//                    	obsDate = ReflectUtil.getFieldValueByName(list.get(i), "datetime").toString();
//                    	fstDate = ReflectUtil.getFieldValueByName(list.get(i + 1), "validdate").toString();
//                    	fstHour = ReflectUtil.getFieldValueByName(list.get(i + 1), "datatime").toString();
//                    	vtiStr = ReflectUtil.getFieldValueByName(list.get(i + 1), "vti").toString();
//                    }
//            		ObsDataEntity obsData = new ObsDataEntity();
//            		obsData.setAtmax(atMaxObs);
//            		obsData.setAtmin(atMinObs);
//            		obsData.setDatetime(obsDate);
//            		StationForecastDataEntity fstData = new StationForecastDataEntity();
//            		fstData.setAtmax(atMaxFst);
//            		fstData.setAtmin(atMinFst);
//            		fstData.setValiddate(fstDate);
//            		fstData.setDatatime(fstHour);
//            		if(vtiStr != null)
//                	{
//                		fstData.setVti(Integer.parseInt(vtiStr));
//                	}
//            		listObjs.add(obsData);
//            		listObjs.add(fstData);
//        		}
//        		obsMapList.put(station, listObjs);
//        		
//        	}
//        	
//        }
//        else
//        {
//        	for(StationForecastDataEntity fstData : fstDataList)
//            {
//                ObsDataEntity obsData = obsDataMap.get(fstData.getStation()).get(fstData.getValiddate());
//                if(!obsMapList.containsKey(fstData.getStation()))
//                {
//                	obsMapList.put(fstData.getStation(), new ArrayList<>());
//                }
//                obsMapList.get(fstData.getStation()).add(obsData);
//                obsMapList.get(fstData.getStation()).add(fstData);
//            }
//        }
//        
//        int count = 0;
//        for(String key : obsMapList.keySet())
//        {
//        	count += obsMapList.get(key).size() / 2;
//        }
//        double sum = 0;
//        if("RSME".equals(method))
//        {
//            int invalidCount = 0;
//            for(String key : obsMapList.keySet())
//            {
//                List<Object> objects = obsMapList.get(key);
//                if(objects == null || objects.size() == 0)
//                {
//                    invalidCount++;
//                    continue;
//                }
//                for(int i = 0, num = objects.size(); i < num - 1; i = i + 2)
//                {
//                	if(objects.get(i) == null || objects.get(i + 1) == null)
//                	{
//                		invalidCount++;
//                    	continue;
//                	}
//                	double obs = getDoubleValueFromObject(objects.get(i), element);
//                	double fst = getDoubleValueFromObject(objects.get(i + 1), element);
//                	if(obs == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || fst >= DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE)
//                	{
//                		invalidCount++;
//                		continue;
//                	}
//                	sum += Math.pow(obs - fst, 2);
//                }
//            }
//            
//            if(count == invalidCount)
//            {
//                sum = DecodeConstants.UNDEF_DOUBLE_VALUE;
//            }
//            else
//            {
//                sum = Math.sqrt(sum / (count - invalidCount));
//            }
//        }
//        else if("MAE".equals(method))
//        {
//            int invalidCount = 0;
//            for(String key : obsMapList.keySet())
//            {
//                List<Object> objects = obsMapList.get(key);
//                if(objects == null || objects.size() == 0)
//                {
//                	invalidCount++;
//                	continue;
//                }
//                for(int i = 0, num = objects.size(); i < num - 1; i = i + 2)
//                {
//                	if(objects.get(i) == null || objects.get(i + 1) == null)
//                	{
//                		invalidCount++;
//                    	continue;
//                	}
//                	double obs = getDoubleValueFromObject(objects.get(i), element);
//                	double fst = getDoubleValueFromObject(objects.get(i + 1), element);
////                	System.out.println("vti: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "vti"));
//                	if(obs == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || fst >= DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE)
//                	{
//                		invalidCount++;
//                		continue;
//                	}
////                	System.out.println("obsDate: " + ReflectUtil.getFieldValueByName(objects.get(i), "datetime") + " fstDate: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "validdate") + " vti: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "vti") + " fsttime: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "datatime") + " obs: " + obs + " fst: " + fst + " MAE: " + Math.abs(obs - fst));
////                	System.out.println("obs: " + obs + " fst: " + fst + " MAE: " + Math.abs(obs - fst));
//                	sum += Math.abs(obs - fst);
//                }
//            }
////            System.out.println("invalidCount: " + invalidCount);
//            if(count == invalidCount)
//            {
//                sum = DecodeConstants.UNDEF_DOUBLE_VALUE;
//            }
//            else
//            {
//                sum = sum / (count - invalidCount);
//            }
//        }
//        else if("CORR".equals(method))
//        {
//            double obsSum = 0;
//            double fstSum = 0;
//            int invalidCount = 0;
//            for(String key : obsMapList.keySet())
//            {
//                List<Object> objects = obsMapList.get(key);
//                if(objects == null || objects.size() == 0)
//                {
//                    invalidCount++;
//                    continue;
//                }
//                for(int i = 0, num = objects.size(); i < num - 1; i = i + 2)
//                {
//                	if(objects.get(i) == null || objects.get(i + 1) == null)
//                	{
//                		invalidCount++;
//                    	continue;
//                	}
//                	double obs = getDoubleValueFromObject(objects.get(i), element);
//                	double fst = getDoubleValueFromObject(objects.get(i + 1), element);
//                	if(obs == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || fst >= DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE)
//                	{
//                		invalidCount++;
//                		continue;
//                	}
//                	obsSum += obs;
//                	fstSum += fst;
//                }
//            }
//            double obsAvg = obsSum / (count - invalidCount);
//            double fstAvg = fstSum / (count - invalidCount);
//
//            double upSum = 0;
//            double downSum = 0;
//            for(String key : obsMapList.keySet())
//            {
//                List<Object> objects = obsMapList.get(key);
//                if(objects == null || objects.size() == 0)
//                {
//                    continue;
//                }
//                for(int i = 0, num = objects.size(); i < num - 1; i = i + 2)
//                {
//                	if(objects.get(i) == null || objects.get(i + 1) == null)
//                	{
//                		invalidCount++;
//                    	continue;
//                	}
//                	double obs = getDoubleValueFromObject(objects.get(i), element);
//                	double fst = getDoubleValueFromObject(objects.get(i + 1), element);
//                	if(obs == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || fst >= DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE)
//                	{
//                		continue;
//                	}
//                	upSum += (obs - obsAvg) * (fst - fstAvg);
//                	downSum += Math.sqrt(Math.pow(obs - obsAvg, 2)) * Math.sqrt(Math.pow(fst - fstAvg, 2));
//                }
//            }
//            if(downSum == 0)
//            {
//                sum = DecodeConstants.UNDEF_DOUBLE_VALUE;
//            }
//            else
//            {
//                sum = upSum / downSum;
//            }
//        }
//        else if("准确率".equals(method))
//        {
//            double dis = Double.parseDouble(configMap.get(element));
//            double correct = 0;
//            int invalidCount = 0;
//            for(String key : obsMapList.keySet())
//            {
//                List<Object> objects = obsMapList.get(key);
//                if(objects == null || objects.size() == 0)
//                {
//                    invalidCount++;
//                    continue;
//                }
//                for(int i = 0, num = objects.size(); i < num - 1; i = i + 2)
//                {
//                	if(objects.get(i) == null || objects.get(i + 1) == null)
//                	{
//                		invalidCount++;
//                    	continue;
//                	}
//                	double obs = getDoubleValueFromObject(objects.get(i), element);
//                	double fst = getDoubleValueFromObject(objects.get(i + 1), element);
//                	if(Math.abs(obs) == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || Math.abs(fst) >= DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE)
//                	{
//                		invalidCount++;
//                		continue;
//                	}
////                	System.out.println("obsDate: " + ReflectUtil.getFieldValueByName(objects.get(i), "datetime") + " fstDate: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "validdate") + " vti: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "vti") + " obs: " + obs + " fst: " + fst + " dis: " + Math.abs(obs - fst));
////                	System.out.println("obs: " + obs + " fst: " + fst + " dis: " + Math.abs(obs - fst));
//                	if(Math.abs(obs - fst) <= dis)
//                	{
//                		correct++;
//                	}
//                }
//            }
//            if(count == invalidCount)
//            {
//                sum = DecodeConstants.UNDEF_DOUBLE_VALUE;
//            }
//            else
//            {
//                sum = correct / (count - invalidCount);
//            }
//        }
//
//        sum = NumberFormatUtil.numFormat(sum, 3);
//
//        return sum;
//    }
//    
//
//    private double getStationValuePre12(String element, String method, Map<String, Map<String, ObsDataEntity>> obsDataMap, List<StationForecastDataEntity> fstDataList)
//    {
//        Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
//        Map<String, List<Object>> obsMapList = new HashMap<>();
//        Map<String, Map<String, List<Object>>> dataStationDateMapList = new HashMap<>();
//        
//        if(element.equals("atmax") || element.equals("atmin"))
//        {
//        	for(StationForecastDataEntity fstData : fstDataList)
//            {
//        		String validDate = fstData.getValiddate().split(" ")[0];
//                ObsDataEntity obsData = obsDataMap.get(fstData.getStation()).get(fstData.getValiddate());
//                if(!dataStationDateMapList.containsKey(fstData.getStation()))
//                {
//                	dataStationDateMapList.put(fstData.getStation(), new HashMap<>());
//                }
//                if(!dataStationDateMapList.get(fstData.getStation()).containsKey(validDate))
//                {
//                	dataStationDateMapList.get(fstData.getStation()).put(validDate, new ArrayList<>());
//                }
//                dataStationDateMapList.get(fstData.getStation()).get(validDate).add(obsData);
//                dataStationDateMapList.get(fstData.getStation()).get(validDate).add(fstData);
//                
//            }
//        	
//        	for(String station : dataStationDateMapList.keySet())
//        	{
//        		List<Object> listObjs = new ArrayList<>();
//        		for(String date : dataStationDateMapList.get(station).keySet())
//        		{
//        			List<Object> list = dataStationDateMapList.get(station).get(date);
//            		double atMaxObs = -9999;
//            		double atMinObs = 9999;
//            		double atMaxFst = -9999;
//            		double atMinFst = 9999;
//            		String obsDate = null;
//            		String fstDate = null;
//            		String fstHour = null;
//            		String vtiStr = null;
//            		for(int i = 0, num = list.size(); i < num - 1; i = i + 2)
//                    {
//            			if(list.get(i) == null || list.get(i + 1) == null)
//                    	{
//                        	continue;
//                    	}
//            			double obs = getDoubleValueFromObject(list.get(i), "at");
//                    	double fst = getDoubleValueFromObject(list.get(i + 1), "at");
//                    	if(atMaxObs < obs)
//                    	{
//                    		atMaxObs = obs;
//                    	}
//                    	if(atMinObs > obs)
//                    	{
//                    		atMinObs = obs;
//                    	}
//                    	if(atMaxFst < fst)
//                    	{
//                    		atMaxFst = fst;
//                    	}
//                    	if(atMinFst > fst)
//                    	{
//                    		atMinFst = fst;
//                    	}
//                    	obsDate = ReflectUtil.getFieldValueByName(list.get(i), "datetime").toString();
//                    	fstDate = ReflectUtil.getFieldValueByName(list.get(i + 1), "validdate").toString();
//                    	fstHour = ReflectUtil.getFieldValueByName(list.get(i + 1), "datatime").toString();
//                    	vtiStr = ReflectUtil.getFieldValueByName(list.get(i + 1), "vti").toString();
//                    }
//            		ObsDataEntity obsData = new ObsDataEntity();
//            		obsData.setAtmax(atMaxObs);
//            		obsData.setAtmin(atMinObs);
//            		obsData.setDatetime(obsDate);
//            		StationForecastDataEntity fstData = new StationForecastDataEntity();
//            		fstData.setAtmax(atMaxFst);
//            		fstData.setAtmin(atMinFst);
//            		fstData.setValiddate(fstDate);
//            		fstData.setDatatime(fstHour);
//            		if(vtiStr != null)
//                	{
//                		fstData.setVti(Integer.parseInt(vtiStr));
//                	}
//            		listObjs.add(obsData);
//            		listObjs.add(fstData);
//        		}
//        		obsMapList.put(station, listObjs);
//        		
//        	}
//        	
//        }
//        else
//        {
//        	for(StationForecastDataEntity fstData : fstDataList)
//            {
//                ObsDataEntity obsData = obsDataMap.get(fstData.getStation()).get(fstData.getValiddate());
//                if(!obsMapList.containsKey(fstData.getStation()))
//                {
//                	obsMapList.put(fstData.getStation(), new ArrayList<>());
//                }
//                obsMapList.get(fstData.getStation()).add(obsData);
//                obsMapList.get(fstData.getStation()).add(fstData);
//            }
//        	
//        }
//        
//        int count = 0;
//        for(String key : obsMapList.keySet())
//        {
//        	count += obsMapList.get(key).size() / 2;
//        }
//        double sum = 0;
//        if("RSME".equals(method))
//        {
//            int invalidCount = 0;
//            for(String key : obsMapList.keySet())
//            {
//                List<Object> objects = obsMapList.get(key);
//                if(objects == null || objects.size() == 0)
//                {
//                    invalidCount++;
//                    continue;
//                }
//                for(int i = 0, num = objects.size(); i < num - 1; i = i + 2)
//                {
//                	if(objects.get(i) == null || objects.get(i + 1) == null)
//                	{
//                		invalidCount++;
//                    	continue;
//                	}
//                	double obs = getDoubleValueFromObject(objects.get(i), element);
//                	double fst = getDoubleValueFromObject(objects.get(i + 1), element);
//                	if(obs == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || fst >= DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE)
//                	{
//                		invalidCount++;
//                		continue;
//                	}
//                	sum += Math.pow(obs - fst, 2);
//                }
//            }
//            
//            if(count == invalidCount)
//            {
//                sum = DecodeConstants.UNDEF_DOUBLE_VALUE;
//            }
//            else
//            {
//                sum = Math.sqrt(sum / (count - invalidCount));
//            }
//        }
//        else if("MAE".equals(method))
//        {
//            int invalidCount = 0;
//            for(String key : obsMapList.keySet())
//            {
//                List<Object> objects = obsMapList.get(key);
//                if(objects == null || objects.size() == 0)
//                {
//                	invalidCount++;
//                	continue;
//                }
//                for(int i = 0, num = objects.size(); i < num - 1; i = i + 2)
//                {
//                	if(objects.get(i) == null || objects.get(i + 1) == null)
//                	{
//                		invalidCount++;
//                    	continue;
//                	}
//                	double obs = getDoubleValueFromObject(objects.get(i), element);
//                	double fst = getDoubleValueFromObject(objects.get(i + 1), element);
////                	System.out.println("vti: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "vti"));
//                	if(obs == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || fst >= DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE)
//                	{
//                		invalidCount++;
//                		continue;
//                	}
////                	System.out.println("obsDate: " + ReflectUtil.getFieldValueByName(objects.get(i), "datetime") + " fstDate: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "validdate") + " vti: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "vti") + " fsttime: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "datatime") + " obs: " + obs + " fst: " + fst + " MAE: " + Math.abs(obs - fst));
////                	System.out.println("obs: " + obs + " fst: " + fst + " MAE: " + Math.abs(obs - fst));
//                	sum += Math.abs(obs - fst);
//                }
//            }
////            System.out.println("invalidCount: " + invalidCount);
//            if(count == invalidCount)
//            {
//                sum = DecodeConstants.UNDEF_DOUBLE_VALUE;
//            }
//            else
//            {
//                sum = sum / (count - invalidCount);
//            }
//        }
//        else if("CORR".equals(method))
//        {
//            double obsSum = 0;
//            double fstSum = 0;
//            int invalidCount = 0;
//            for(String key : obsMapList.keySet())
//            {
//                List<Object> objects = obsMapList.get(key);
//                if(objects == null || objects.size() == 0)
//                {
//                    invalidCount++;
//                    continue;
//                }
//                for(int i = 0, num = objects.size(); i < num - 1; i = i + 2)
//                {
//                	if(objects.get(i) == null || objects.get(i + 1) == null)
//                	{
//                		invalidCount++;
//                    	continue;
//                	}
//                	double obs = getDoubleValueFromObject(objects.get(i), element);
//                	double fst = getDoubleValueFromObject(objects.get(i + 1), element);
//                	if(obs == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || fst >= DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE)
//                	{
//                		invalidCount++;
//                		continue;
//                	}
//                	obsSum += obs;
//                	fstSum += fst;
//                }
//            }
//            double obsAvg = obsSum / (count - invalidCount);
//            double fstAvg = fstSum / (count - invalidCount);
//
//            double upSum = 0;
//            double downSum = 0;
//            for(String key : obsMapList.keySet())
//            {
//                List<Object> objects = obsMapList.get(key);
//                if(objects == null || objects.size() == 0)
//                {
//                    continue;
//                }
//                for(int i = 0, num = objects.size(); i < num - 1; i = i + 2)
//                {
//                	if(objects.get(i) == null || objects.get(i + 1) == null)
//                	{
//                		invalidCount++;
//                    	continue;
//                	}
//                	double obs = getDoubleValueFromObject(objects.get(i), element);
//                	double fst = getDoubleValueFromObject(objects.get(i + 1), element);
//                	if(obs == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || fst >= DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE)
//                	{
//                		continue;
//                	}
//                	upSum += (obs - obsAvg) * (fst - fstAvg);
//                	downSum += Math.sqrt(Math.pow(obs - obsAvg, 2)) * Math.sqrt(Math.pow(fst - fstAvg, 2));
//                }
//            }
//            if(downSum == 0)
//            {
//                sum = DecodeConstants.UNDEF_DOUBLE_VALUE;
//            }
//            else
//            {
//                sum = upSum / downSum;
//            }
//        }
//        else if("准确率".equals(method))
//        {
//            double dis = Double.parseDouble(configMap.get(element));
//            double correct = 0;
//            int invalidCount = 0;
//            for(String key : obsMapList.keySet())
//            {
//                List<Object> objects = obsMapList.get(key);
//                if(objects == null || objects.size() == 0)
//                {
//                    invalidCount++;
//                    continue;
//                }
//                for(int i = 0, num = objects.size(); i < num - 1; i = i + 2)
//                {
//                	if(objects.get(i) == null || objects.get(i + 1) == null)
//                	{
//                		invalidCount++;
//                    	continue;
//                	}
//                	double obs = getDoubleValueFromObject(objects.get(i), element);
//                	double fst = getDoubleValueFromObject(objects.get(i + 1), element);
//                	if(Math.abs(obs) == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || Math.abs(fst) >= DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE)
//                	{
//                		invalidCount++;
//                		continue;
//                	}
////                	System.out.println("obsDate: " + ReflectUtil.getFieldValueByName(objects.get(i), "datetime") + " fstDate: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "validdate") + " vti: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "vti") + " obs: " + obs + " fst: " + fst + " dis: " + Math.abs(obs - fst));
////                	System.out.println("obs: " + obs + " fst: " + fst + " dis: " + Math.abs(obs - fst));
//                	if(Math.abs(obs - fst) <= dis)
//                	{
//                		correct++;
//                	}
//                }
//            }
//            if(count == invalidCount)
//            {
//                sum = DecodeConstants.UNDEF_DOUBLE_VALUE;
//            }
//            else
//            {
//                sum = correct / (count - invalidCount);
//            }
//        }
//
//        sum = NumberFormatUtil.numFormat(sum, 3);
//
//        return sum;
//    }
//    private double getStationValuePre12(String dataSource, int vti, String element, String method, Map<String, Map<String, ObsDataEntity>> obsDataMap, List<StationForecastDataEntity> fstDataList)
//    {
//    	Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
//    	Map<String, List<Object>> obsMapList = new HashMap<>();
//    	Map<String, Map<String, List<Object>>> dataStationDateMapList = new HashMap<>();
//    	
//    	if(element.equals("atmax") || element.equals("atmin"))
//    	{
//    		for(StationForecastDataEntity fstData : fstDataList)
//    		{
//    			String validDate = fstData.getValiddate().split(" ")[0];
//    			ObsDataEntity obsData = obsDataMap.get(fstData.getStation()).get(fstData.getValiddate());
//    			if(!dataStationDateMapList.containsKey(fstData.getStation()))
//    			{
//    				dataStationDateMapList.put(fstData.getStation(), new HashMap<>());
//    			}
//    			if(!dataStationDateMapList.get(fstData.getStation()).containsKey(validDate))
//    			{
//    				dataStationDateMapList.get(fstData.getStation()).put(validDate, new ArrayList<>());
//    			}
//    			dataStationDateMapList.get(fstData.getStation()).get(validDate).add(obsData);
//    			dataStationDateMapList.get(fstData.getStation()).get(validDate).add(fstData);
//    			
//    		}
//    		
//    		for(String station : dataStationDateMapList.keySet())
//    		{
//    			List<Object> listObjs = new ArrayList<>();
//    			for(String date : dataStationDateMapList.get(station).keySet())
//    			{
//    				List<Object> list = dataStationDateMapList.get(station).get(date);
//    				double atMaxObs = -9999;
//    				double atMinObs = 9999;
//    				double atMaxFst = -9999;
//    				double atMinFst = 9999;
//    				String obsDate = null;
//    				String fstDate = null;
//    				String fstHour = null;
//    				String vtiStr = null;
//    				for(int i = 0, num = list.size(); i < num - 1; i = i + 2)
//    				{
//    					if(list.get(i) == null || list.get(i + 1) == null)
//    					{
//    						continue;
//    					}
//    					double obs = getDoubleValueFromObject(list.get(i), "at");
//    					double fst = getDoubleValueFromObject(list.get(i + 1), "at");
//    					if(atMaxObs < obs)
//    					{
//    						atMaxObs = obs;
//    					}
//    					if(atMinObs > obs)
//    					{
//    						atMinObs = obs;
//    					}
//    					if(atMaxFst < fst)
//    					{
//    						atMaxFst = fst;
//    					}
//    					if(atMinFst > fst)
//    					{
//    						atMinFst = fst;
//    					}
//    					obsDate = ReflectUtil.getFieldValueByName(list.get(i), "datetime").toString();
//    					fstDate = ReflectUtil.getFieldValueByName(list.get(i + 1), "validdate").toString();
//    					fstHour = ReflectUtil.getFieldValueByName(list.get(i + 1), "datatime").toString();
//    					vtiStr = ReflectUtil.getFieldValueByName(list.get(i + 1), "vti").toString();
//    				}
//    				ObsDataEntity obsData = new ObsDataEntity();
//    				obsData.setAtmax(atMaxObs);
//    				obsData.setAtmin(atMinObs);
//    				obsData.setDatetime(obsDate);
//    				StationForecastDataEntity fstData = new StationForecastDataEntity();
//    				fstData.setAtmax(atMaxFst);
//    				fstData.setAtmin(atMinFst);
//    				fstData.setValiddate(fstDate);
//    				fstData.setDatatime(fstHour);
//    				if(vtiStr != null)
//    				{
//    					fstData.setVti(Integer.parseInt(vtiStr));
//    				}
//    				listObjs.add(obsData);
//    				listObjs.add(fstData);
//    			}
//    			obsMapList.put(station, listObjs);
//    			
//    		}
//    		
//    	}
//    	else if(dataSource.equals(DataTypeEnum.SWC9KM.getDataType()))
//    	{
//    		for(StationForecastDataEntity fstData : fstDataList)
//			{
//				ObsDataEntity obsData = obsDataMap.get(fstData.getStation()).get(fstData.getValiddate());
//				if(!obsMapList.containsKey(fstData.getStation()))
//				{
//					obsMapList.put(fstData.getStation(), new ArrayList<>());
//				}
//				obsMapList.get(fstData.getStation()).add(obsData);
//				obsMapList.get(fstData.getStation()).add(fstData);
//			}
//    	}
//    	else
//    	{
//    		Map<String, Map<String, List<Object>>> dataStationDateMapListTemp = new HashMap<>();
//    		if(vti == 24)
//    		{
//    			for(StationForecastDataEntity fstData : fstDataList)
//    			{
//    				String dataTime = fstData.getDatatime();
//    				if(!dataStationDateMapListTemp.containsKey(fstData.getStation()))
//    				{
//    					dataStationDateMapListTemp.put(fstData.getStation(), new HashMap<>());
//    				}
//    				if(!dataStationDateMapListTemp.get(fstData.getStation()).containsKey(dataTime))
//    				{
//    					dataStationDateMapListTemp.get(fstData.getStation()).put(dataTime, new ArrayList<>());
//    				}
//    				dataStationDateMapListTemp.get(fstData.getStation()).get(dataTime).add(fstData);
//    				
//    			}
//    			List<StationForecastDataEntity> czList = new ArrayList<>();
//    			for(String station : dataStationDateMapListTemp.keySet())
//    			{
//    				for(String dateTime : dataStationDateMapListTemp.get(station).keySet())
//    				{
//    					List<Object> list = dataStationDateMapListTemp.get(station).get(dateTime);
//    					if(list.size() > 1)
//    					{
//    						double[] values = new double[list.size()];
//    						for(int i = 0, count = list.size(); i < count; i++)
//    						{
//    							values[i] = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(list.get(i), element)));
//    						}
//    						double[] czValues = new double[24];
//    						double[][] valuesTem = new double[1][];
//    						valuesTem[0] = values;
//    						czValues = BilinearInterpolateUtil.bilinearInterpolation(valuesTem, 2, 25)[0];
//    						String time = String.valueOf(ReflectUtil.getFieldValueByName(list.get(0), "datatime"));
//    						int vtiTem = Integer.parseInt(String.valueOf(ReflectUtil.getFieldValueByName(list.get(0), "vti")));
//    						for(int i = 0; i < 24; i++)
//    						{
//    							String timeTem = TimeUtil.addHours(time, i);
//    							StationForecastDataEntity data = new StationForecastDataEntity();
//    							data.setDatatime(timeTem);
//    							data.setStation(station);
//    							data.setValiddate(TimeUtil.addHours(time, vtiTem + i));
//    							ReflectUtil.setFieldValueByName(data, czValues[i], element);
//    							czList.add(data);
//    						}
//    					}
//    				}
//    			}
//    			
//    			for(StationForecastDataEntity fstData : czList)
//    			{
//    				ObsDataEntity obsData = obsDataMap.get(fstData.getStation()).get(fstData.getValiddate());
//    				if(!obsMapList.containsKey(fstData.getStation()))
//    				{
//    					obsMapList.put(fstData.getStation(), new ArrayList<>());
//    				}
//    				obsMapList.get(fstData.getStation()).add(obsData);
//    				obsMapList.get(fstData.getStation()).add(fstData);
//    			}
//    			
//    		}
//    		else if(vti == 48)
//    		{
//    			for(StationForecastDataEntity fstData : fstDataList)
//    			{
//    				String dataTime = fstData.getDatatime();
//    				if(!dataStationDateMapListTemp.containsKey(fstData.getStation()))
//    				{
//    					dataStationDateMapListTemp.put(fstData.getStation(), new HashMap<>());
//    				}
//    				if(!dataStationDateMapListTemp.get(fstData.getStation()).containsKey(dataTime))
//    				{
//    					dataStationDateMapListTemp.get(fstData.getStation()).put(dataTime, new ArrayList<>());
//    				}
//    				dataStationDateMapListTemp.get(fstData.getStation()).get(dataTime).add(fstData);
//    				
//    			}
//    			
//    			List<StationForecastDataEntity> czList = new ArrayList<>();
//    			for(String station : dataStationDateMapListTemp.keySet())
//    			{
//    				for(String dateTime : dataStationDateMapListTemp.get(station).keySet())
//    				{
//    					List<Object> list = dataStationDateMapListTemp.get(station).get(dateTime);
//    					if(list.size() >= 5)
//    					{
//    						double[] values = new double[5];
//    						for(int i = 0; i < 5; i++)
//    						{
//    							values[i] = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(list.get(i), element)));
//    						}
//    						double[] czValues = new double[13];
//    						double[][] valuesTem = new double[1][];
//    						valuesTem[0] = values;
//    						czValues = BilinearInterpolateUtil.bilinearInterpolation(valuesTem, 2, 13)[0];
//    						String time = String.valueOf(ReflectUtil.getFieldValueByName(list.get(0), "datatime"));
//    						int vtiTem = Integer.parseInt(String.valueOf(ReflectUtil.getFieldValueByName(list.get(0), "vti")));
//    						for(int i = 0; i < 13; i++)
//    						{
//    							String timeTem = TimeUtil.addHours(time, i);
//    							StationForecastDataEntity data = new StationForecastDataEntity();
//    							data.setDatatime(timeTem);
//    							data.setStation(station);
//    							data.setValiddate(TimeUtil.addHours(time, vtiTem + i));
//    							ReflectUtil.setFieldValueByName(data, czValues[i], element);
//    							czList.add(data);
//    						}
////    						System.out.println(czList);
//    					}
//    				}
//    			}
//    			
//    			for(StationForecastDataEntity fstData : czList)
//    			{
//    				ObsDataEntity obsData = obsDataMap.get(fstData.getStation()).get(fstData.getValiddate());
//    				if(!obsMapList.containsKey(fstData.getStation()))
//    				{
//    					obsMapList.put(fstData.getStation(), new ArrayList<>());
//    				}
//    				obsMapList.get(fstData.getStation()).add(obsData);
//    				obsMapList.get(fstData.getStation()).add(fstData);
//    			}
//    			
//    		}
//    		else if(vti == 96 || vti == 120)
//    		{
//    			for(StationForecastDataEntity fstData : fstDataList)
//    			{
//    				String dataTime = fstData.getValiddate().split(" ")[0];
//    				if(!dataStationDateMapListTemp.containsKey(fstData.getStation()))
//    				{
//    					dataStationDateMapListTemp.put(fstData.getStation(), new HashMap<>());
//    				}
//    				if(!dataStationDateMapListTemp.get(fstData.getStation()).containsKey(dataTime))
//    				{
//    					dataStationDateMapListTemp.get(fstData.getStation()).put(dataTime, new ArrayList<>());
//    				}
//    				dataStationDateMapListTemp.get(fstData.getStation()).get(dataTime).add(fstData);
//    				
//    			}
//    			
//    			List<StationForecastDataEntity> czList = new ArrayList<>();
//    			for(String station : dataStationDateMapListTemp.keySet())
//    			{
//    				for(String dateTime : dataStationDateMapListTemp.get(station).keySet())
//    				{
//    					List<Object> list = dataStationDateMapListTemp.get(station).get(dateTime);
//    					if(list.size() >= 4)
//    					{
//    						double[] values = new double[4];
//    						for(int i = 0; i < 4; i++)
//    						{
//    							values[i] = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(list.get(i), element)));
//    						}
//    						double[] czValues = new double[7];
//    						double[][] valuesTem = new double[1][];
//    						valuesTem[0] = values;
//    						czValues = BilinearInterpolateUtil.bilinearInterpolation(valuesTem, 2, 7)[0];
//    						String time = String.valueOf(ReflectUtil.getFieldValueByName(list.get(0), "datatime"));
//    						int vtiTem = Integer.parseInt(String.valueOf(ReflectUtil.getFieldValueByName(list.get(0), "vti")));
//    						for(int i = 0; i < 7; i++)
//    						{
//    							String timeTem = TimeUtil.addHours(time, i * 3);
//    							StationForecastDataEntity data = new StationForecastDataEntity();
//    							data.setDatatime(timeTem);
//    							data.setStation(station);
//    							data.setValiddate(TimeUtil.addHours(time, vtiTem + i * 3));
//    							ReflectUtil.setFieldValueByName(data, czValues[i], element);
//    							czList.add(data);
//    						}
////    						System.out.println(czList);
//    					}
//    				}
//    			}
//    			
//    			for(StationForecastDataEntity fstData : czList)
//    			{
//    				ObsDataEntity obsData = obsDataMap.get(fstData.getStation()).get(fstData.getValiddate());
//    				if(!obsMapList.containsKey(fstData.getStation()))
//    				{
//    					obsMapList.put(fstData.getStation(), new ArrayList<>());
//    				}
//    				obsMapList.get(fstData.getStation()).add(obsData);
//    				obsMapList.get(fstData.getStation()).add(fstData);
//    			}
//    			
//    		}
//    		else
//    		{
//    			for(StationForecastDataEntity fstData : fstDataList)
//    			{
//    				ObsDataEntity obsData = obsDataMap.get(fstData.getStation()).get(fstData.getValiddate());
//    				if(!obsMapList.containsKey(fstData.getStation()))
//    				{
//    					obsMapList.put(fstData.getStation(), new ArrayList<>());
//    				}
//    				obsMapList.get(fstData.getStation()).add(obsData);
//    				obsMapList.get(fstData.getStation()).add(fstData);
//    			}
//    		}
//    		
//    	}
//    	
//    	int count = 0;
//    	for(String key : obsMapList.keySet())
//    	{
//    		count += obsMapList.get(key).size() / 2;
//    	}
//    	double sum = 0;
//    	if("RSME".equals(method))
//    	{
//    		int invalidCount = 0;
//    		for(String key : obsMapList.keySet())
//    		{
//    			List<Object> objects = obsMapList.get(key);
//    			if(objects == null || objects.size() == 0)
//    			{
//    				invalidCount++;
//    				continue;
//    			}
//    			for(int i = 0, num = objects.size(); i < num - 1; i = i + 2)
//    			{
//    				if(objects.get(i) == null || objects.get(i + 1) == null)
//    				{
//    					invalidCount++;
//    					continue;
//    				}
//    				double obs = getDoubleValueFromObject(objects.get(i), element);
//    				double fst = getDoubleValueFromObject(objects.get(i + 1), element);
//    				if(obs == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || fst >= DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE)
//    				{
//    					invalidCount++;
//    					continue;
//    				}
//    				sum += Math.pow(obs - fst, 2);
//    			}
//    		}
//    		
//    		if(count == invalidCount)
//    		{
//    			sum = DecodeConstants.UNDEF_DOUBLE_VALUE;
//    		}
//    		else
//    		{
//    			sum = Math.sqrt(sum / (count - invalidCount));
//    		}
//    	}
//    	else if("MAE".equals(method))
//    	{
//    		int invalidCount = 0;
//    		for(String key : obsMapList.keySet())
//    		{
//    			List<Object> objects = obsMapList.get(key);
//    			if(objects == null || objects.size() == 0)
//    			{
//    				invalidCount++;
//    				continue;
//    			}
//    			for(int i = 0, num = objects.size(); i < num - 1; i = i + 2)
//    			{
//    				if(objects.get(i) == null || objects.get(i + 1) == null)
//    				{
//    					invalidCount++;
//    					continue;
//    				}
//    				double obs = getDoubleValueFromObject(objects.get(i), element);
//    				double fst = getDoubleValueFromObject(objects.get(i + 1), element);
////                	System.out.println("vti: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "vti"));
//					if(obs == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || fst >= DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE)
//					{
//						invalidCount++;
//						continue;
//					}
////                	System.out.println("obsDate: " + ReflectUtil.getFieldValueByName(objects.get(i), "datetime") + " fstDate: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "validdate") + " vti: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "vti") + " fsttime: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "datatime") + " obs: " + obs + " fst: " + fst + " MAE: " + Math.abs(obs - fst));
////                	System.out.println("obs: " + obs + " fst: " + fst + " MAE: " + Math.abs(obs - fst));
//					sum += Math.abs(obs - fst);
//			    	}
//			 }
////            System.out.println("invalidCount: " + invalidCount);
//    		if(count == invalidCount)
//    		{
//    			sum = DecodeConstants.UNDEF_DOUBLE_VALUE;
//    		}
//    		else
//    		{
//    			sum = sum / (count - invalidCount);
//    		}
//    	}
//    	else if("CORR".equals(method))
//    	{
//    		double obsSum = 0;
//    		double fstSum = 0;
//    		int invalidCount = 0;
//    		for(String key : obsMapList.keySet())
//    		{
//    			List<Object> objects = obsMapList.get(key);
//    			if(objects == null || objects.size() == 0)
//    			{
//    				invalidCount++;
//    				continue;
//    			}
//    			for(int i = 0, num = objects.size(); i < num - 1; i = i + 2)
//    			{
//    				if(objects.get(i) == null || objects.get(i + 1) == null)
//    				{
//    					invalidCount++;
//    					continue;
//    				}
//    				double obs = getDoubleValueFromObject(objects.get(i), element);
//    				double fst = getDoubleValueFromObject(objects.get(i + 1), element);
//    				if(obs == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || fst >= DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE)
//    				{
//    					invalidCount++;
//    					continue;
//    				}
//    				obsSum += obs;
//    				fstSum += fst;
//    			}
//    		}
//    		double obsAvg = obsSum / (count - invalidCount);
//    		double fstAvg = fstSum / (count - invalidCount);
//    		
//    		double upSum = 0;
//    		double downSum = 0;
//    		for(String key : obsMapList.keySet())
//    		{
//    			List<Object> objects = obsMapList.get(key);
//    			if(objects == null || objects.size() == 0)
//    			{
//    				continue;
//    			}
//    			for(int i = 0, num = objects.size(); i < num - 1; i = i + 2)
//    			{
//    				if(objects.get(i) == null || objects.get(i + 1) == null)
//    				{
//    					invalidCount++;
//    					continue;
//    				}
//    				double obs = getDoubleValueFromObject(objects.get(i), element);
//    				double fst = getDoubleValueFromObject(objects.get(i + 1), element);
//    				if(obs == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || fst >= DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE)
//    				{
//    					continue;
//    				}
//    				upSum += (obs - obsAvg) * (fst - fstAvg);
//    				downSum += Math.sqrt(Math.pow(obs - obsAvg, 2)) * Math.sqrt(Math.pow(fst - fstAvg, 2));
//    			}
//    		}
//    		if(downSum == 0)
//    		{
//    			sum = DecodeConstants.UNDEF_DOUBLE_VALUE;
//    		}
//    		else
//    		{
//    			sum = upSum / downSum;
//    		}
//    	}
//    	else if("准确率".equals(method))
//    	{
//    		double dis = Double.parseDouble(configMap.get(element));
//    		double correct = 0;
//    		int invalidCount = 0;
//    		for(String key : obsMapList.keySet())
//    		{
//    			List<Object> objects = obsMapList.get(key);
//    			if(objects == null || objects.size() == 0)
//    			{
//    				invalidCount++;
//    				continue;
//    			}
//    			for(int i = 0, num = objects.size(); i < num - 1; i = i + 2)
//    			{
//    				if(objects.get(i) == null || objects.get(i + 1) == null)
//    				{
//    					invalidCount++;
//    					continue;
//    				}
//    				double obs = getDoubleValueFromObject(objects.get(i), element);
//    				double fst = getDoubleValueFromObject(objects.get(i + 1), element);
//    				if(Math.abs(obs) == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || Math.abs(fst) >= DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE)
//    				{
//    					invalidCount++;
//    					continue;
//    				}
////                	System.out.println("obsDate: " + ReflectUtil.getFieldValueByName(objects.get(i), "datetime") + " fstDate: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "validdate") + " vti: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "vti") + " obs: " + obs + " fst: " + fst + " dis: " + Math.abs(obs - fst));
////                	System.out.println("obs: " + obs + " fst: " + fst + " dis: " + Math.abs(obs - fst));
//    				if(Math.abs(obs - fst) <= dis)
//    				{
//    					correct++;
//    				}
//    			}
//    		}
//    		if(count == invalidCount)
//    		{
//    			sum = DecodeConstants.UNDEF_DOUBLE_VALUE;
//    		}
//    		else
//    		{
//    			sum = correct / (count - invalidCount);
//    		}
//    	}
//    	
//    	sum = NumberFormatUtil.numFormat(sum, 3);
//    	
//    	return sum;
//    }
//
//    private Map<String, Map<String, double[]>> getGribValue(String[] stations, String vti, List<ObsDataEntity> obsDataList, List<GribForecastDataEntity> fstDataList)
//    {
//        Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
//        List<ObsDataEntity> obsDataListSub = new ArrayList<>();
//        Map<String, Map<String, double[]>> result = new HashMap<>();
//        if(fstDataList.size() == 0)
//        {
//            return result;
//        }
//        for(GribForecastDataEntity fstData : fstDataList)
//        {
//            for(ObsDataEntity obsData : obsDataList)
//            {
//                Date date = TimeUtil.String2Date(obsData.getDatetime(), "yyyy-MM-dd HH:mm:ss");
//                Calendar calendar = Calendar.getInstance();
//                calendar.setTime(date);
//                calendar.add(Calendar.HOUR_OF_DAY, 8);
//                if(TimeUtil.date2String(calendar.getTime(), "yyyy-MM-dd HH:mm:ss").compareTo(fstData.getValidDate()) == 0)
//                {
//                    obsDataListSub.add(obsData);
//                }
//            }
//        }
//        int count = fstDataList.size();
//        for(String station : stations)
//        {
//            double h = 0;
//            double tn = 0;
//            double f = 0;
//            double m = 0;
//            double hSum = 0;
//            double fSum = 0;
//            double mSum = 0;
//            double th = 0;
//            double tm = 0;
//            double tf = 0;
//            int realCount = 0;
//            int nilCount = 0;
//            Map<String, Double> fsMap = new HashMap<>();
//            fsMap.put("0", Double.parseDouble("0"));
//            for(int i = 0; i < count; i++)
//            {
//                String filePath = fstDataList.get(i).getSourcePath();
//                double[][] gribDatas = null;
//                if(filePath.endsWith(".txt"))
//                {
//                    gribDatas = GribUtil.readGribDatasFromTxt(filePath, ",");
//                }
//                else
//                {
//                    Map<String, Object> datasMap = GribFileReaderUtil.getDatasMap(filePath);
//                    String prefix = "";
//                    for(String key : datasMap.keySet())
//                    {
//                        if (key.contains(":")) {
//                            String[] split = key.split(":");
//                            prefix = split[0] + ":";
//                        }
//                    }
//                    String lonlatStr = configMap.get("lonlat");
//                    String[] split = lonlatStr.split(",");
//                    //startLon,startLat,endLon,endLat
//                    double[] lonlat = new double[]{Double.parseDouble(split[0]), Double.parseDouble(split[1]),
//                                                   Double.parseDouble(split[2]), Double.parseDouble(split[3])};
//                    String dataType = DataTypeUtil.getDataType(filePath);
//                     String element = configMap.get(dataType);
//                    if(element.contains("$"))
//                    {
//                        int[] disVtiAndVti = GribUtil.getDisVtiAndVtiStation(filePath, dataType);
//                        element = element.replace("$", disVtiAndVti[1] + "");
//                    }
//                    double[][][] dataValues = NcReader.readByElemNameLayerSlice(datasMap, element, null);
//                    String[] lonLatName = NcReader.getLonLatName(datasMap);
//                    double[] lons = ReadLonLatUtil.readLonLat(datasMap, prefix + lonLatName[0]);
//                    double[] lats = ReadLonLatUtil.readLonLat(datasMap, prefix + lonLatName[1]);
//                    gribDatas = SliceArrayUtil.slice(dataValues[0], lons, lats, lonlat);
//                }
//                double lon = 0;
//                double lat = 0;
//                double value = 999999;
//
//                for(ObsDataEntity obsData : obsDataListSub)
//                {
//                    Date date = TimeUtil.String2Date(obsData.getDatetime(), "yyyy-MM-dd HH:mm:ss");
//                    Calendar calendar = Calendar.getInstance();
//                    calendar.setTime(date);
//                    calendar.add(Calendar.HOUR_OF_DAY, 8);
//                    if(station.equals(obsData.getStation()) && fstDataList.get(i).getValidDate().compareTo(TimeUtil.date2String(calendar.getTime(), "yyyy-MM-dd HH:mm:ss")) == 0)
//                    {
//                        lon = obsData.getLon();
//                        lat = obsData.getLat();
//                        value = obsData.getRain24();
//                        break;
//                    }
//                }
//                double fstValue = getFstValue(lon, lat, gribDatas);
//                fsMap.put(fstDataList.get(i).getVti() + "", fstValue);
//                fstValue = fstValue - fsMap.get((fstDataList.get(i).getVti() - 24) + "");
//
////                System.out.println("vti:" + fstDataList.get(i).getVti() + "fst:" + fstValue + " obs:" + value);
//
//                if(value == 999999)
//                {
////                    value = 0;
//                    nilCount++;
//                    continue;
//                }
//                if(value >= 0.1 && fstValue >= 0.1)
//                {
//                    h++;
//                }
//                else if(value < 0.1 && fstValue < 0.1)
//                {
//                    tn++;
//                }
//                else if(value >= 0.1 && fstValue < 0.1)
//                {
//                    m++;
//                }
//                else if(value < 0.1 && fstValue >= 0.1)
//                {
//                    f++;
//                }
//            }
//
//            if(count == nilCount)
//            {
//                if(!result.containsKey(station))
//                {
//                    result.put(station, new HashMap<>());
//                }
//                result.get(station).put(vti, null);
//            }
//            else
//            {
//                th = (h + tn) / (h + f + m + tn);
//                tf = f / ((f + h) == 0 ? 1 : (f + h));
//                tm = m / ((m + tn) == 0 ? 1 : (tn + m));
//
////                System.out.println("station:" + station + " h: " + h + " f: " + f + " m: " + m + " tn: " + tn);
//
//                hSum += th;
//                fSum += tf;
//                mSum += tm;
//                if(!result.containsKey(station))
//                {
//                    result.put(station, new HashMap<>());
//                }
////                realCount = count - nilCount;
//                result.get(station).put(vti, new double[]{hSum, fSum, mSum});
//            }
//
//        }
//
//        return result;
//    }
//
//    private double getFstValue(double lon, double lat, double[][] values)
//    {
//        Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
//        String lonlat = configMap.get("lonlat");
//        String[] split = lonlat.split(",");
//        double[] lonlats = new double[split.length];
//        for(int i = 0, count = lonlats.length; i < count; i++)
//        {
//            lonlats[i] = Double.parseDouble(split[i]);
//        }
//        double lon_dis = Double.parseDouble(configMap.get("lon_dis"));
//        double lat_dis = Double.parseDouble(configMap.get("lat_dis"));
//        double[] findFirstPoint = ReadGribRainUtil.findFindFirstPoint(lon, lat, lon_dis, lat_dis, lonlats);
////        System.out.println("lonCount:" + values[0].length + ", latCount" + values.length);
////        System.out.println("lon:" + lon + ", lat:" + lat);
////        findFirstPoint: 670.0, 405.0
////        System.out.println("findFirstPoint: " + findFirstPoint[0] + ", " + findFirstPoint[1]);
//        double topLeft = values[(int) findFirstPoint[1]][(int) findFirstPoint[0]];
//        double topRight = values[(int) findFirstPoint[1] + 1][(int) findFirstPoint[0]];
//        double bottomLeft = values[(int) findFirstPoint[1]][(int) findFirstPoint[0] + 1];
//        double bottomRight = values[(int) findFirstPoint[1] + 1][(int) findFirstPoint[0] + 1];
//        double value = ReadGribRainUtil.bilinearInterpolation(topLeft, topRight, bottomLeft, bottomRight, findFirstPoint[2], findFirstPoint[3]);
//
//        return value;
//    }
//
//    private double getDoubleValueFromObject(Object object, String element)
//    {
//        double result = 999999;
//        Object value = ReflectUtil.getFieldValueByName(object, element);
//        if(value != null)
//        {
//            result = Double.parseDouble(value.toString());
//        }
//        else
//        {
//            result = 999999;
//        }
//
//        return result;
//    }
//
//}
