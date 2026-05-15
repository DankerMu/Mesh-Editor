package com.check.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.check.pojo.CheckDataGribIndbEntity;
import com.check.pojo.CheckDataIndbEntity;
import com.check.pojo.CheckDataParams;
import com.check.service.inf.CheckDataIndbService;
import com.config.dao.ConfigMapper;
import com.config.pojo.ConfigParams;
import com.config.pojo.MethodEntity;
import com.constants.DataTypeEnum;
import com.constants.DecodeConstants;
import com.forecast.dao.GribForecastDataMapper;
import com.forecast.dao.StationForecastDataMapper;
import com.forecast.pojo.GribForecastDataEntity;
import com.forecast.pojo.GribForecastRainValueEntity;
import com.forecast.pojo.StationForecastDataEntity;
import com.obs.dao.ObsDataMapper;
import com.obs.pojo.ObsDataEntity;
import com.station.pojo.StationEntity;
import com.util.BilinearInterpolateUtil;
import com.util.BilinearInterpolateUtil.InterpolationResult;
import com.util.DataTypeUtil;
import com.util.GribFileReaderUtil;
import com.util.GribUtil;
import com.util.NcReader;
import com.util.NumberFormatUtil;
import com.util.ReadGribRainUtil;
import com.util.ReadLonLatUtil;
import com.util.ReadPropertiesUtil;
import com.util.ReflectUtil;
import com.util.SliceArrayUtil;
import com.util.TimeUtil;

/**
 * @category
 * @date 2025/3/19 10:52
 * @description TODO
 */
@Service
public class CheckDataIndbServiceImpl implements CheckDataIndbService {

    @Resource
    private ObsDataMapper obsDataMapper;
    @Resource
    private StationForecastDataMapper stationForecastDataMapper;
    @Resource
    private GribForecastDataMapper gribForecastDataMapper;
    @Resource
    private ConfigMapper configMapper;
    private String rate = "晴雨准确率";
    private double absAt = 273.15;
//    private static Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
    private Map<String, String> tableNameMap = ReadPropertiesUtil.getUserConfigMap("data_table.properties");
    private Map<String, String> methodMap = ReadPropertiesUtil.getUserConfigMap("method_map.properties");
    @Override
    public Map<String, Map<String, Map<String, Map<String, Double>>>> checkStationDataHour(CheckDataParams param) {
        int disVti = param.getDisVti();
        int[] vtis = null;
        if(disVti == 3)
        {
            vtis = new int[]{3  ,6  ,9  ,12 ,15 ,18 ,21 ,24 ,27 ,30 ,33 ,36 ,39 ,42 ,45 ,48 ,51 ,54 ,57 ,60 ,63 ,66 ,69 ,72 ,75 ,78 ,81 ,84 ,87 ,90 ,93 ,96 ,99 ,102,105,108,111,114,117,120,123,126,129,132,135,138,141,144,147,150,153,156,159,162,165,168,171,174,177,180,183,186,189,192,195,198,201,204,207,210,213,216,219,222,225,228,231,234,237,240};
        }
        else if(disVti == 6)
        {
            vtis = new int[]{6  ,12 ,18 ,24 ,30 ,36 ,42 ,48 ,54 ,60 ,66 ,72 ,78 ,84 ,90 ,96 ,102,108,114,120,126,132,138,144,150,156,162,168,174,180,186,192,198,204,210,216,222,228,234,240};
        }
        else if(disVti == 12)
        {
            vtis = new int[]{12 ,24 ,36 ,48 ,60 ,72 ,84 ,96 ,108,120,132,144,156,168,180,192,204,216,228,240};
        }
        else
        {
            vtis = new int[]{24 ,48 ,72 ,96 ,120,144,168,192,216,240};
//            vtis = new int[]{24};
        }
        param.setVtis(vtis);
        param.setEndValidDate(TimeUtil.addHours(param.getEndValidDate(), 24));
        Map<String, String> tableNamesMap = new HashMap<>();
        if(param.getDataSources() != null)
        {
            for(String dataSource : param.getDataSources())
            {
                tableNamesMap.put(dataSource, tableNameMap.get(dataSource + "_value"));
            }
        }

        if(param.getStations() == null || param.getStations().length == 0)
        {
            ConfigParams params = new ConfigParams();
            params.setZone(param.getZone());
            List<StationEntity> stationEntities = null;
            if(param.getZone() != 6)
            {
                stationEntities = stationForecastDataMapper.queryForecastStationsByZone(param);
            }
            else
            {
                stationEntities = stationForecastDataMapper.queryForecastAllStations(param);
            }
            int count = stationEntities.size();
            String[] stations = new String[count];
            for(int i = 0; i < count; i++)
            {
                stations[i] = stationEntities.get(i).getStation();
            }
            param.setStations(stations);
        }

        String startTimeStr = param.getStartValidDate();
        String startValidDate = TimeUtil.addHours(param.getStartValidDate(), -8);
        param.setStartValidDate(startValidDate);
        
        String endTimeStr = param.getEndValidDate();
        String endValidDate = TimeUtil.addHours(endTimeStr, -8);
        param.setEndValidDate(endValidDate);

//        long time = System.currentTimeMillis();
        int hour = Integer.parseInt(startTimeStr.split(" ")[1].split(":")[0]);
        param.setHour(hour - 8);
        param.setStartValidDate(TimeUtil.addHours(param.getStartValidDate(), -24));
        
        
        List<StationForecastDataEntity> stationForecastList = null;
        if(param.getElements()[0].equals("rain"))
        {
        	stationForecastList = stationForecastDataMapper.queryStationForecastCheckRain(param);
        }
        else
        {
            param.setStartValidDate(TimeUtil.addHours(param.getStartValidDate(), 24));
        	stationForecastList = stationForecastDataMapper.queryStationForecastCheck(param);
        }

        if(hour == 8)
        {
        	hour = 20;
        }
        else
        {
        	hour = 8;
        }
        param.setHour(hour);
        int[] vtisPre12 = new int[param.getVtis().length];
        for(int i = 0, count = param.getVtis().length - 1; i < count; i++)
        {
        	vtisPre12[i] = param.getVtis()[i] + 12;
        }
        vtisPre12[vtisPre12.length - 1] = vtis[param.getVtis().length - 1];
        Map<String, List<StationForecastDataEntity>> orgDatasMap = new HashMap<>();
        param.setStartValidDate(startTimeStr);
        param.setEndValidDate(endTimeStr);
        for(String dataSource : tableNamesMap.keySet())
        {
            param.setTableName(tableNamesMap.get(dataSource));
            List<StationForecastDataEntity> stationForecastDataEntities = stationForecastDataMapper.queryOrgStationForecastCheck(param);
            if(stationForecastDataEntities == null || stationForecastDataEntities.size() == 0)
            {
                continue;
            }
            if(dataSource.equals(DataTypeEnum.ECMF.getDataType()))
            {
            	for(StationForecastDataEntity data : stationForecastDataEntities)
            	{
            		data.setAt(data.getAt() - absAt);
            		data.setAtmax(data.getAtmax() - absAt);
            		data.setAtmin(data.getAtmin() - absAt);
            		data.setRain24(data.getRain24() * 1000);
            		data.setDatatime(TimeUtil.addHours(data.getDatatime(), -8));
            		data.setValiddate(TimeUtil.addHours(data.getValiddate(), -8));
            	}
            }
            else
            {
            	for(StationForecastDataEntity data : stationForecastDataEntities)
            	{
            		data.setAt(data.getAt() - absAt);
            		data.setAtmax(data.getAtmax() - absAt);
            		data.setAtmin(data.getAtmin() - absAt);
            		data.setDatatime(TimeUtil.addHours(data.getDatatime(), -8));
            		data.setValiddate(TimeUtil.addHours(data.getValiddate(), -8));
            	}
            }
            orgDatasMap.put(dataSource, stationForecastDataEntities);
        }

//        System.out.println("查询预报数据耗时: " + (System.currentTimeMillis() - time));

        param.setStartTime(startValidDate);

        param.setEndValidDate(endValidDate);

//        time = System.currentTimeMillis();
        List<ObsDataEntity> obsList = obsDataMapper.queryObsCheckData(param);
//        System.out.println("查询实况数据耗时: " + (System.currentTimeMillis() - time));

        Map<String, Map<String, Map<String, Map<String, Double>>>> result = new LinkedHashMap<>();
        orgDatasMap.put("fst", stationForecastList);
        for(String dataSource : orgDatasMap.keySet())
        {
            Map<String, Map<String, Map<String, Double>>> calStation = null;
            if(dataSource.equals("fst"))
            {
            	param.setVtis(vtis);
            	calStation = calStationHour(param, obsList, orgDatasMap.get(dataSource));
            }
            else
            {
            	param.setVtis(vtisPre12);
            	param.setDataSource(dataSource);
            	calStation = calStationHourPre12(param, obsList, orgDatasMap.get(dataSource));
            }
            result.put(dataSource, calStation);
        }

        for(String dataSource : tableNamesMap.keySet())
        {
            if(!result.containsKey(dataSource))
            {
                result.put(dataSource, new LinkedHashMap<>());
                for(String element : result.get("fst").keySet())
                {
                    result.get(dataSource).put(element, new LinkedHashMap<>());
                    for(String method : result.get("fst").get(element).keySet())
                    {
                        result.get(dataSource).get(element).put(method, new LinkedHashMap<>());
                        for(String vti : result.get("fst").get(element).get(method).keySet())
                        {
                            result.get(dataSource).get(element).get(method).put(vti, null);
                        }
                    }
                }
            }
        }

        return result;
    }

    private Map<String, Map<String, Map<String, Double>>> calStationHour(CheckDataParams param, List<ObsDataEntity> obsList, List<StationForecastDataEntity> stationForecastList)
    {
//          station     datetime
        Map<String, Map<String, ObsDataEntity>> obsDataMap = new HashMap<>();
//          station     datetime_vti
        Map<String, Map<String, StationForecastDataEntity>> fstDataMap = new HashMap<>();
        String[] stations = param.getStations();
        for(String station : stations)
        {
            obsDataMap.put(station, new HashMap<>());
            fstDataMap.put(station, new HashMap<>());
        }

        for(ObsDataEntity obsData : obsList)
        {
            obsDataMap.get(obsData.getStation()).put(obsData.getDatetime(), obsData);
        }
        for(StationForecastDataEntity fstData : stationForecastList)
        {
            fstDataMap.get(fstData.getStation()).put(fstData.getDatatime() + "_" + fstData.getVti(), fstData);
        }
        String[] elements = param.getElements();
        int[] vtis = param.getVtis();
        Map<String, List<StationForecastDataEntity>> vtiDataMap = new HashMap<>();
        if(!elements[0].equals("rain"))
    	{
    		int start = 0;
    		for(int vti : vtis)
    		{
    			vtiDataMap.put(vti + "", new ArrayList<>());
    			for(StationForecastDataEntity fstData : stationForecastList)
    			{
    				if(fstData.getVti() > (start == 0 ? -1 : start) && fstData.getVti() <= vti && fstData.getValiddate().compareTo(param.getStartValidDate()) > 0)
    				{
    					vtiDataMap.get(vti + "").add(fstData);
    				}
    			}
    			start = vti;
    		}
    	}
    	else
    	{
    		for(int vti : vtis)
    		{
    			vtiDataMap.put(vti + "", new ArrayList<>());
    			for(StationForecastDataEntity fstData : stationForecastList)
    			{
    				if(fstData.getVti() == vti && fstData.getValiddate().compareTo(param.getStartValidDate()) > 0)
    				{
    					vtiDataMap.get(vti + "").add(fstData);
    				}
    			}
    		}
    	}


        String[] methods = param.getMethods();
        Map<String, Map<String, Map<String, Map<String, Double>>>> result = new LinkedHashMap<>();
        if(!elements[0].equals("rain"))
        {
            for(int vti : vtis)
            {
                double value = getStationValue(elements[0], methods[0], obsDataMap, vtiDataMap.get(vti + ""));
                result.put(vti + "", new HashMap<>());
                result.get(vti + "").put(elements[0], new HashMap<>());
                result.get(vti + "").get(elements[0]).put(methods[0], new HashMap<>());
                result.get(vti + "").get(elements[0]).get(methods[0]).put("value", value);
            }
        }
        else
        {
            for(int vti : vtis)
            {
                double[] value = getStationCalHourValue(obsDataMap, fstDataMap, vtiDataMap.get(vti + ""));
//                System.out.println("vti:" + vti + "," + JSONObject.toJSON(value));
                result.put(vti + "", new LinkedHashMap<>());
                result.get(vti + "").put(elements[0], new LinkedHashMap<>());
                result.get(vti + "").get(elements[0]).put("TS评分", new LinkedHashMap<>());
                result.get(vti + "").get(elements[0]).put("晴雨准确率", new LinkedHashMap<>());
                result.get(vti + "").get(elements[0]).put("空报率", new LinkedHashMap<>());
                result.get(vti + "").get(elements[0]).put("漏报率", new LinkedHashMap<>());
                result.get(vti + "").get(elements[0]).get("TS评分").put("value", value[0]);
                result.get(vti + "").get(elements[0]).get("晴雨准确率").put("value", value[1]);
                result.get(vti + "").get(elements[0]).get("空报率").put("value", value[2]);
                result.get(vti + "").get(elements[0]).get("漏报率").put("value", value[3]);
            }
        }

        Map<String, Map<String, Map<String, Double>>> resultMap = new LinkedHashMap<>();
        for(String element : elements)
        {
            resultMap.put(element, new LinkedHashMap<>());
            for(String method : methods)
            {
                resultMap.get(element).put(method, new LinkedHashMap<>());
                for(int vti : vtis)
                {
                	Double value = result.get(String.valueOf(vti)).get(element).get(method).get("value");
                	if(value == DecodeConstants.UNDEF_DOUBLE_VALUE)
                	{
                		value = null;
                	}
                    resultMap.get(element).get(method).put(String.valueOf(vti), value);
                }
            }
        }

        return resultMap;
    }
    private Map<String, Map<String, Map<String, Double>>> calStationHourPre12(CheckDataParams param, List<ObsDataEntity> obsList, List<StationForecastDataEntity> stationForecastList)
    {
//          station     datetime
    	Map<String, Map<String, ObsDataEntity>> obsDataMap = new HashMap<>();
//          station     datetime_vti
    	Map<String, Map<String, StationForecastDataEntity>> fstDataMap = new HashMap<>();
    	String[] stations = param.getStations();
    	for(String station : stations)
    	{
    		obsDataMap.put(station, new HashMap<>());
    		fstDataMap.put(station, new HashMap<>());
    	}
    	
    	for(ObsDataEntity obsData : obsList)
    	{
    		obsDataMap.get(obsData.getStation()).put(obsData.getDatetime(), obsData);
    	}
    	for(StationForecastDataEntity fstData : stationForecastList)
    	{
    		fstDataMap.get(fstData.getStation()).put(fstData.getDatatime() + "_" + fstData.getVti(), fstData);
//            System.out.println("key:" + (fstData.getDatatime() + "_" + fstData.getVti()));
    	}
    	String[] elements = param.getElements();
    	int[] vtisTem = param.getVtis();
    	int[] vtis = new int[vtisTem.length];
    	for(int i = 0, count = vtis.length; i < count; i++)
    	{
    		vtis[i] = vtisTem[i];
    	}
    	Map<String, List<StationForecastDataEntity>> vtiDataMap = new HashMap<>();
    	if(!elements[0].equals("rain"))
    	{
    		int start = 12;
    		int disVti = 0;
    		for(int vti : vtis)
    		{
    			disVti = vti - 12;
    			vtiDataMap.put((disVti == 228 ? 240 : disVti) + "", new ArrayList<>());
    			for(StationForecastDataEntity fstData : stationForecastList)
    			{
    				if(fstData.getVti() >= (start == 0 ? -1 : start) && fstData.getVti() <= vti && fstData.getValiddate().compareTo(param.getStartTime()) >= 0)
    				{
    					vtiDataMap.get((disVti == 228 ? 240 : disVti) + "").add(fstData);
    				}
    			}
    			start = vti;
    		}
    	}
    	else
    	{
    		int disVti = 0;
    		for(int vti : vtis)
    		{
    			disVti = vti - 12;
    			vtiDataMap.put((disVti == 228 ? 240 : disVti) + "", new ArrayList<>());
    			for(StationForecastDataEntity fstData : stationForecastList)
    			{
    				if(fstData.getVti() == vti && fstData.getValiddate().compareTo(param.getStartValidDate()) > 0)
    				{
    					vtiDataMap.get((disVti == 228 ? 240 : disVti) + "").add(fstData);
    				}
    			}
    		}
    	}
    	
    	
    	String[] methods = param.getMethods();
    	Map<String, Map<String, Map<String, Map<String, Double>>>> result = new LinkedHashMap<>();
    	
    	for(int i = 0, count = vtis.length - 1; i < count; i++)
        {
    		vtis[i] -= 12;
        }
    	if(!elements[0].equals("rain"))
    	{
    		for(int vti : vtis)
    		{
    			double value = getStationValuePre12(param.getDataSource(), vti, elements[0], methods[0], obsDataMap, vtiDataMap.get(vti + ""));
    			result.put(vti + "", new HashMap<>());
    			result.get(vti + "").put(elements[0], new HashMap<>());
    			result.get(vti + "").get(elements[0]).put(methods[0], new HashMap<>());
    			result.get(vti + "").get(elements[0]).get(methods[0]).put("value", value);
    		}
    	}
    	else
    	{
    		for(int vti : vtis)
    		{
    			double[] value = getStationCalHourValuePre12(vti, obsDataMap, fstDataMap, vtiDataMap.get(vti + ""));
//                System.out.println("vti:" + vti + "," + JSONObject.toJSON(value));
    			result.put(vti + "", new LinkedHashMap<>());
    			result.get(vti + "").put(elements[0], new LinkedHashMap<>());
    			result.get(vti + "").get(elements[0]).put("TS评分", new LinkedHashMap<>());
    			result.get(vti + "").get(elements[0]).put("晴雨准确率", new LinkedHashMap<>());
    			result.get(vti + "").get(elements[0]).put("空报率", new LinkedHashMap<>());
    			result.get(vti + "").get(elements[0]).put("漏报率", new LinkedHashMap<>());
    			result.get(vti + "").get(elements[0]).get("TS评分").put("value", value[0]);
    			result.get(vti + "").get(elements[0]).get("晴雨准确率").put("value", value[1]);
    			result.get(vti + "").get(elements[0]).get("空报率").put("value", value[2]);
    			result.get(vti + "").get(elements[0]).get("漏报率").put("value", value[3]);
    		}
    	}
    	
    	Map<String, Map<String, Map<String, Double>>> resultMap = new LinkedHashMap<>();
    	for(String element : elements)
    	{
    		resultMap.put(element, new LinkedHashMap<>());
    		for(String method : methods)
    		{
    			resultMap.get(element).put(method, new LinkedHashMap<>());
    			for(int vti : vtis)
    			{
    				Double value = result.get(String.valueOf(vti)).get(element).get(method).get("value");
    				if(value == DecodeConstants.UNDEF_DOUBLE_VALUE)
    				{
    					value = null;
    				}
    				resultMap.get(element).get(method).put(String.valueOf(vti), value);
    			}
    		}
    	}
    	
    	return resultMap;
    }
    
    public Map<String, Map<String, Map<String, CheckDataIndbEntity>>> checkStationDataDay(CheckDataParams param) {
        int disVti = param.getDisVti();
        int[] vtis = null;
        if(disVti == 3)
        {
            vtis = new int[]{3  ,6  ,9  ,12 ,15 ,18 ,21 ,24 ,27 ,30 ,33 ,36 ,39 ,42 ,45 ,48 ,51 ,54 ,57 ,60 ,63 ,66 ,69 ,72 ,75 ,78 ,81 ,84 ,87 ,90 ,93 ,96 ,99 ,102,105,108,111,114,117,120,123,126,129,132,135,138,141,144,147,150,153,156,159,162,165,168,171,174,177,180,183,186,189,192,195,198,201,204,207,210,213,216,219,222,225,228,231,234,237,240};
        }
        else if(disVti == 6)
        {
            vtis = new int[]{6  ,12 ,18 ,24 ,30 ,36 ,42 ,48 ,54 ,60 ,66 ,72 ,78 ,84 ,90 ,96 ,102,108,114,120,126,132,138,144,150,156,162,168,174,180,186,192,198,204,210,216,222,228,234,240};
        }
        else if(disVti == 12)
        {
            vtis = new int[]{12 ,24 ,36 ,48 ,60 ,72 ,84 ,96 ,108,120,132,144,156,168,180,192,204,216,228,240};
        }
        else
        {
            vtis = new int[]{24 ,48 ,72 ,96 ,120,144,168,192,216,240};
//            vtis = new int[]{240};
        }
        param.setVtis(vtis);

        String startTimeStr = param.getStartValidDate();
        String endTimeStr = param.getEndValidDate();
        int hour = Integer.parseInt(startTimeStr.split(" ")[1].split(":")[0]);
        param.setHour(hour);

        int vti = param.getVti();
        Map<String, String> tableNamesMap = new HashMap<>();
        if(param.getDataSources() != null)
        {
            for(String dataSource : param.getDataSources())
            {
                tableNamesMap.put(dataSource, tableNameMap.get(dataSource + "_value"));
            }
        }


        if(param.getStations() == null || param.getStations().length == 0)
        {
            ConfigParams params = new ConfigParams();
            params.setZone(param.getZone());
            List<StationEntity> stationEntities = null;
            List<StationEntity> stationWzdEntities = null;
            if(param.getZone() != 6)
            {
                stationEntities = stationForecastDataMapper.queryForecastStationsByZone(param);
            }
            else
            {
                stationEntities = stationForecastDataMapper.queryForecastAllStations(param);
                stationWzdEntities = stationForecastDataMapper.queryForecastAllWzdStations(param);
                if(stationWzdEntities != null)
                {
                	stationEntities.addAll(stationWzdEntities);
                }
            }
            int count = stationEntities.size();
            String[] stations = new String[count];
            for(int i = 0; i < count; i++)
            {
                stations[i] = stationEntities.get(i).getStation();
            }
            param.setStations(stations);
        }
//        param.setStations(new String[]{"41535"});
        Calendar calendar = Calendar.getInstance();
        
        param.setStartValidDate(TimeUtil.addHours(startTimeStr, -8));
        
        param.setEndValidDate(TimeUtil.addHours(endTimeStr, -8));
        
        param.setStartVti(vti - 24);
        param.setEndVti(vti);
        param.setHour(hour - 8);
        String elementStr = param.getElements()[0];
//      ===================================================站点预报数据查询================================================================
        List<StationForecastDataEntity> stationForecastList = null;
        if(elementStr.equals("rain"))
        {
        	if(vti == 24)
        	{
        		param.setVtis(new int[]{0, 24});
        	}
        	else
        	{
        		param.setVtis(new int[]{vti - 24, vti});
        	}
        	calendar.setTime(TimeUtil.String2Date(param.getStartValidDate(), TimeUtil.DEFAULT_DATETIME_FORMAT));
        	calendar.add(Calendar.HOUR_OF_DAY, -(vti - 24));
        	param.setStartValidDate(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
        	param.setEndValidDate(TimeUtil.addHours(endTimeStr, 24));
        	stationForecastList = stationForecastDataMapper.queryStationForecastCheckDayRain(param);
        }
        else
        {
        	param.setEndValidDate(TimeUtil.addHours(endTimeStr, 24));
        	stationForecastList = stationForecastDataMapper.queryStationForecastCheckDay(param);
        }
        for(StationForecastDataEntity data : stationForecastList)
        {
        	data.setVis(data.getVis() / 1000);
        }

        param.setStartTime(TimeUtil.addHours(startTimeStr, -8));

//        long time = System.currentTimeMillis();
        calendar.setTime(TimeUtil.String2Date(startTimeStr, TimeUtil.DEFAULT_DATETIME_FORMAT));
//        calendar.add(Calendar.HOUR_OF_DAY, -24);
        calendar.add(Calendar.HOUR_OF_DAY, -8);
        param.setStartTime(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
        param.setEndValidDate(TimeUtil.addHours(endTimeStr, 24 - 8));
//      ===================================================实况数据查询================================================================  
        List<ObsDataEntity> obsList = obsDataMapper.queryObsCheckData(param);
//        System.out.println("查询实况数据耗时: " + (System.currentTimeMillis() - time));
        for(ObsDataEntity data : obsList)
        {
        	data.setVis(data.getVis() / 1000);
        }
        param.setStartTime(startTimeStr);
        
        
        param.setEndValidDate(endTimeStr);

        Map<String, List<StationForecastDataEntity>> orgDatasMap = new HashMap<>();
//        time = System.currentTimeMillis();
        if(hour == 8)
        {
        	param.setHour(20);
        }
        else
        {
        	param.setHour(8);
        }
        for(int i = 0, count = param.getVtis().length; i < count; i++)
        {
    		if(param.getVtis()[i] == 240)
    		{
    			continue;
    		}
        	param.getVtis()[i] += 12;
        }
//        param.setHour(hour);
        param.setStartValidDate(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
        param.setStartValidDate(TimeUtil.addHours(param.getStartValidDate(), 8));
        param.setEndValidDate(endTimeStr);
        
//      ===================================================原始模式预报数据查询================================================================
        int startVti = param.getStartVti();
        int endVti = param.getEndVti();
        for(String dataSource : tableNamesMap.keySet())
        {
            param.setTableName(tableNamesMap.get(dataSource));
            List<StationForecastDataEntity> stationForecastDataEntities = null;
            if(elementStr.equals("rain"))
            {
            	param.setStartValidDate(TimeUtil.addHours(startTimeStr, - 24));
            	param.setEndValidDate(TimeUtil.addHours(endTimeStr, 24 + 12));
//            	param.setEndValidDate(TimeUtil.addHours(param.getEndValidDate(), 12));
            	if(dataSource.equals(DataTypeEnum.SWC9KM.getDataType()))
            	{
            		for(int i = 0, count = param.getVtis().length; i < count; i++)
            		{
            			if(param.getVtis()[i] > 72)
            			{
            				param.getVtis()[i] = 72;
            			}
            		}
            	}
            	stationForecastDataEntities = stationForecastDataMapper.queryOrgStationForecastCheckDayRain(param);
            }
            else
            {
            	param.setEndValidDate(TimeUtil.addHours(endTimeStr, 24));
            	param.setStartVti(startVti + 12);
                param.setEndVti(endVti + 12);
            	stationForecastDataEntities = stationForecastDataMapper.queryOrgStationForecastCheckDay(param);
            }
            if(stationForecastDataEntities == null || stationForecastDataEntities.size() == 0)
            {
                continue;
            }
            if(dataSource.equals(DataTypeEnum.ECMF.getDataType()))
            {
            	for(StationForecastDataEntity data : stationForecastDataEntities)
                {
                    data.setAt(data.getAt() - absAt);
                    data.setVis(data.getVis() / 1000);
                    data.setRain24(data.getRain24() * 1000);
                    data.setDatatime(TimeUtil.addHours(data.getDatatime(), -8));
                    data.setValiddate(TimeUtil.addHours(data.getValiddate(), -8));
                }
            }
            else
            {
            	for(StationForecastDataEntity data : stationForecastDataEntities)
                {
                    data.setAt(data.getAt() - absAt);
                    data.setVis(data.getVis() / 1000);
                    data.setDatatime(TimeUtil.addHours(data.getDatatime(), -8));
                    data.setValiddate(TimeUtil.addHours(data.getValiddate(), -8));
                    
                }
            }
            
            orgDatasMap.put(dataSource, stationForecastDataEntities);
        }

//        System.out.println("查询预报数据耗时: " + (System.currentTimeMillis() - time));

        for(int i = 0, count = param.getVtis().length; i < count; i++)
        {
    		if(param.getVtis()[i] == 240)
    		{
    			continue;
    		}
        	param.getVtis()[i] -= 12;
        }

        Map<String, Map<String, Map<String, CheckDataIndbEntity>>> result = new LinkedHashMap<>();
        orgDatasMap.put("fst", stationForecastList);
        for(String dataSource : orgDatasMap.keySet())
        {   
//        	time = System.currentTimeMillis();
//        	System.out.println("dataSource: " + dataSource);
            Map<String, Map<String, CheckDataIndbEntity>> calStation = null;
            if(!dataSource.endsWith("fst"))
            {
            	param.setDataSource(dataSource);
            	calStation = calStationDayPre12(dataSource, param, obsList, orgDatasMap.get(dataSource));
            }
            else
            {
            	calStation = calStationDay(dataSource, param, obsList, orgDatasMap.get(dataSource));
            }
            
            result.put(dataSource, calStation);
//            System.out.println("计算数据耗时: " + (System.currentTimeMillis() - time));
        }
        
        System.out.println(startTimeStr + " 站点检验计算完成。");

        return result;
    }
    
    public Map<String, Map<String, Map<String, CheckDataIndbEntity>>> checkStationDataDayRain(CheckDataParams param) {
        int disVti = param.getDisVti();
        int[] vtis = null;
        if(disVti == 3)
        {
            vtis = new int[]{3  ,6  ,9  ,12 ,15 ,18 ,21 ,24 ,27 ,30 ,33 ,36 ,39 ,42 ,45 ,48 ,51 ,54 ,57 ,60 ,63 ,66 ,69 ,72 ,75 ,78 ,81 ,84 ,87 ,90 ,93 ,96 ,99 ,102,105,108,111,114,117,120,123,126,129,132,135,138,141,144,147,150,153,156,159,162,165,168,171,174,177,180,183,186,189,192,195,198,201,204,207,210,213,216,219,222,225,228,231,234,237,240};
        }
        else if(disVti == 6)
        {
            vtis = new int[]{6  ,12 ,18 ,24 ,30 ,36 ,42 ,48 ,54 ,60 ,66 ,72 ,78 ,84 ,90 ,96 ,102,108,114,120,126,132,138,144,150,156,162,168,174,180,186,192,198,204,210,216,222,228,234,240};
        }
        else if(disVti == 12)
        {
            vtis = new int[]{12 ,24 ,36 ,48 ,60 ,72 ,84 ,96 ,108,120,132,144,156,168,180,192,204,216,228,240};
        }
        else
        {
            vtis = new int[]{24 ,48 ,72 ,96 ,120,144,168,192,216,240};
//            vtis = new int[]{48};
        }
        param.setVtis(vtis);
        param.setElements(new String[]{"rain"});
        String startTimeStr = param.getStartValidDate();
        String endTimeStr = param.getEndValidDate();
        int hour = Integer.parseInt(startTimeStr.split(" ")[1].split(":")[0]);
        param.setHour(hour);

        int vti = param.getVti();
        Map<String, String> tableNamesMap = new HashMap<>();
        if(param.getDataSources() != null)
        {
            for(String dataSource : param.getDataSources())
            {
                tableNamesMap.put(dataSource, tableNameMap.get(dataSource + "_value"));
            }
        }


        if(param.getStations() == null || param.getStations().length == 0)
        {
            ConfigParams params = new ConfigParams();
            params.setZone(param.getZone());
            List<StationEntity> stationEntities = null;
            if(param.getZone() != 6)
            {
                stationEntities = stationForecastDataMapper.queryForecastStationsByZone(param);
            }
            else
            {
                stationEntities = stationForecastDataMapper.queryForecastAllStations(param);
            }
            int count = stationEntities.size();
            String[] stations = new String[count];
            for(int i = 0; i < count; i++)
            {
                stations[i] = stationEntities.get(i).getStation();
            }
            param.setStations(stations);
        }

        Calendar calendar = Calendar.getInstance();
        
        param.setStartValidDate(TimeUtil.addHours(param.getStartValidDate(), -8));
        
        param.setEndValidDate(TimeUtil.addHours(endTimeStr, -8));
        
        param.setStartVti(vti - 24);
        param.setEndVti(vti);
        param.setHour(hour - 8);
//        String elementStr = param.getElements()[0];
//      ===================================================站点预报数据查询================================================================
        List<StationForecastDataEntity> stationForecastList = null;
        if(vti == 24)
    	{
    		param.setVtis(new int[]{0, 24});
    	}
    	else
    	{
    		param.setVtis(new int[]{vti - 24, vti});
    	}
    	calendar.setTime(TimeUtil.String2Date(param.getStartValidDate(), TimeUtil.DEFAULT_DATETIME_FORMAT));
//    	calendar.add(Calendar.HOUR_OF_DAY, -(vti - 24));//20250807 不需要查询这么多的数据
    	param.setStartValidDate(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
    	param.setEndValidDate(TimeUtil.addHours(endTimeStr, 24));
    	stationForecastList = stationForecastDataMapper.queryStationForecastCheckDayRain(param);
        
        for(StationForecastDataEntity data : stationForecastList)
        {
        	data.setVis(data.getVis() / 1000);
        }

        param.setStartTime(TimeUtil.addHours(startTimeStr, -8));

//        long time = System.currentTimeMillis();
        calendar.setTime(TimeUtil.String2Date(startTimeStr, TimeUtil.DEFAULT_DATETIME_FORMAT));
//        calendar.add(Calendar.HOUR_OF_DAY, -24);
        calendar.add(Calendar.HOUR_OF_DAY, -8);
        param.setStartTime(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
        param.setEndValidDate(TimeUtil.addHours(endTimeStr, 24 - 8));
//      ===================================================实况数据查询================================================================  
        List<ObsDataEntity> obsList = obsDataMapper.queryObsCheckData(param);
//        System.out.println("查询实况数据耗时: " + (System.currentTimeMillis() - time));
        for(ObsDataEntity data : obsList)
        {
        	data.setVis(data.getVis() / 1000);
        }
        param.setStartTime(startTimeStr);
        
        
        param.setEndValidDate(endTimeStr);

        Map<String, List<StationForecastDataEntity>> orgDatasMap = new HashMap<>();
//        time = System.currentTimeMillis();
        if(hour == 8)
        {
        	param.setHour(20);
        }
        else
        {
        	param.setHour(8);
        }
        for(int i = 0, count = param.getVtis().length; i < count; i++)
        {
    		if(param.getVtis()[i] == 240)
    		{
    			continue;
    		}
        	param.getVtis()[i] += 12;
        }
//        param.setHour(hour);
        param.setStartValidDate(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
        param.setStartValidDate(TimeUtil.addHours(param.getStartValidDate(), 8));
        param.setEndValidDate(endTimeStr);
        int[] vtis2 = new int[]{param.getVtis()[0], param.getVtis()[1]};
//      ===================================================原始模式预报数据查询================================================================
        for(String dataSource : tableNamesMap.keySet())
        {
            param.setTableName(tableNamesMap.get(dataSource));
            List<StationForecastDataEntity> stationForecastDataEntities = null;
            param.setStartValidDate(TimeUtil.addHours(startTimeStr, - 24));
        	param.setEndValidDate(TimeUtil.addHours(endTimeStr, 24 + 12));
        	if(dataSource.equals(DataTypeEnum.SWC9KM.getDataType()))
        	{
        		for(int i = 0, count = param.getVtis().length; i < count; i++)
        		{
        			if(param.getVtis()[i] > 72)
        			{
        				param.getVtis()[i] = 72;
        			}
        		}
        	}
        	else
        	{
        		param.setVtis(vtis2);
        	}
        	stationForecastDataEntities = stationForecastDataMapper.queryOrgStationForecastCheckDayRain(param);
            if(stationForecastDataEntities == null || stationForecastDataEntities.size() == 0)
            {
                continue;
            }
            if(dataSource.equals(DataTypeEnum.ECMF.getDataType()))
            {
            	for(StationForecastDataEntity data : stationForecastDataEntities)
                {
                    data.setAt(data.getAt() - absAt);
                    data.setVis(data.getVis() / 1000);
                    data.setRain24(data.getRain24() * 1000);
                    data.setDatatime(TimeUtil.addHours(data.getDatatime(), -8));
                    data.setValiddate(TimeUtil.addHours(data.getValiddate(), -8));
                }
            }
            else
            {
            	for(StationForecastDataEntity data : stationForecastDataEntities)
                {
                    data.setAt(data.getAt() - absAt);
                    data.setVis(data.getVis() / 1000);
                    data.setDatatime(TimeUtil.addHours(data.getDatatime(), -8));
                    data.setValiddate(TimeUtil.addHours(data.getValiddate(), -8));
                    
                }
            }
            
            orgDatasMap.put(dataSource, stationForecastDataEntities);
        }

//        System.out.println("查询预报数据耗时: " + (System.currentTimeMillis() - time));

        for(int i = 0, count = param.getVtis().length; i < count; i++)
        {
    		if(param.getVtis()[i] == 240)
    		{
    			continue;
    		}
        	param.getVtis()[i] -= 12;
        }

        Map<String, Map<String, Map<String, CheckDataIndbEntity>>> result = new LinkedHashMap<>();
        orgDatasMap.put("fst", stationForecastList);
        for(String dataSource : orgDatasMap.keySet())
        {   
//        	time = System.currentTimeMillis();
//        	System.out.println("dataSource: " + dataSource);
            Map<String, Map<String, CheckDataIndbEntity>> calStation = null;
            if(!dataSource.endsWith("fst"))
            {
            	param.setDataSource(dataSource);
            	calStation = calStationDayPre12(dataSource, param, obsList, orgDatasMap.get(dataSource));
            }
            else
            {
            	calStation = calStationDay(dataSource, param, obsList, orgDatasMap.get(dataSource));
            }
            
            result.put(dataSource, calStation);
//            System.out.println("计算数据耗时: " + (System.currentTimeMillis() - time));
        }
        
        System.out.println(startTimeStr + " 站点检验降水计算完成。");

        return result;
    }

    private Map<String, Map<String, CheckDataIndbEntity>> calStationDay(String dataSource, CheckDataParams param, List<ObsDataEntity> obsList, List<StationForecastDataEntity> stationForecastList)
    {
//          station     datetime
        Map<String, Map<String, ObsDataEntity>> obsDataMap = new HashMap<>();
//          station     datetime_vti
//        Map<String, Map<String, StationForecastDataEntity>> fstDataMap = new HashMap<>();
        Map<String, Map<String, StationForecastDataEntity>> rainDataMap = new HashMap<>();
        String[] stations = param.getStations();
        for(String station : stations)
        {
            obsDataMap.put(station, new HashMap<>());
//            fstDataMap.put(station, new HashMap<>());
            rainDataMap.put(station, new HashMap<>());
        }

        for(ObsDataEntity obsData : obsList)
        {
            obsDataMap.get(obsData.getStation()).put(obsData.getDatetime(), obsData);
        }
        String[] elements = param.getElements();

//          station    validdate
        Map<String, Map<String, List<StationForecastDataEntity>>> stationDateDataMap = new HashMap<>();


//        逐日
        String validDateStr = null;
        String station = null;
//        Set<String> validDateSet = new HashSet<>();
        List<String> validDateList = new ArrayList<>();
        
        Map<String, Map<String, CheckDataIndbEntity>> resultIndbMap = new HashMap<>();
        Map<String, Map<String, CheckDataIndbEntity>> resultIndbMapNew = new HashMap<>();
        String[] split = param.getStartTime().split(" ");
        String calDate = split[0];
        if(param.getElements()[0].equals("rain"))
        {
        	String dateStr = param.getStartTime();
//        	dateStr = TimeUtil.addHours(dateStr, -24);
        	calDate = TimeUtil.addHours(dateStr, 24).split(" ")[0];
        }
        int hour = Integer.parseInt(split[1].split(":")[0]);
        CheckDataIndbEntity indbData = null;
        String startValidDate = dataSource.equals("fst") ? TimeUtil.addHours(param.getStartValidDate(), -8) : param.getStartValidDate();
        for(StationForecastDataEntity data : stationForecastList)
        {
        	station = data.getStation();
            validDateStr = data.getValiddate().split(" ")[0];
            if(!stationDateDataMap.containsKey(station))
            {
            	stationDateDataMap.put(station, new HashMap<>());
            }
            if(!resultIndbMap.containsKey(station))
            {
            	resultIndbMap.put(station, new HashMap<>());
            	resultIndbMapNew.put(station, new HashMap<>());
            	indbData = new CheckDataIndbEntity();
            	indbData.setStation(station);
            	indbData.setDataTime(calDate);
            	indbData.setHour(hour);
            	resultIndbMap.get(station).put(calDate, indbData);
            }
            if(!stationDateDataMap.get(station).containsKey(validDateStr))
            {
            	stationDateDataMap.get(station).put(validDateStr, new ArrayList<>());
            }
            if(!param.getElements()[0].equals("rain"))
            {
            	if(data.getValiddate().compareTo(startValidDate) >= 0)
                {
            		stationDateDataMap.get(station).get(validDateStr).add(data);
//                	validDateSet.add(validDateStr);
                }
            }
            else
            {
            	stationDateDataMap.get(station).get(validDateStr).add(data);
//            	validDateSet.add(validDateStr);
            }
        }
        validDateList.add(calDate);
//        validDateList = validDateSet.stream().sorted().collect(Collectors.toList());

        int vti = param.getVti();
        
        for(String element : elements)
        {
        	ConfigParams configParams = new ConfigParams();
        	configParams.setElement(element);
        	List<MethodEntity> list = configMapper.getMethodByElement(configParams);
        	String[] methods = list.get(0).getMethod().split(",");
        	if(!element.equals("rain"))
        	{
        		for(String method : methods)
            	{
        			for(String stationNum : stationDateDataMap.keySet())
        			{
        				for(String validdate : validDateList)
        				{
        					double value = getStationValue(element, method, obsDataMap, stationDateDataMap.get(stationNum).get(validdate));
        					CheckDataIndbEntity dataIndbEntity = resultIndbMap.get(stationNum).get(validdate);
        					if(dataIndbEntity != null)
        					{
        						ReflectUtil.setFieldValueByName(dataIndbEntity, value, element + methodMap.get(method) + vti);
        						ReflectUtil.setFieldValueByName(dataIndbEntity, dataSource, "dataSource");
        					}
        				}
        			}
            	}
        	}
        	else
        	{
        		for(StationForecastDataEntity fstData : stationForecastList)
    			{
    				rainDataMap.get(fstData.getStation()).put(fstData.getDatatime() + "_" + fstData.getVti(), fstData);
    			}
    			
//              station       validdate
    			Map<String, Map<String, List<StationForecastDataEntity>>> dateRainDataMap = new HashMap<>();
    			for(StationForecastDataEntity data : stationForecastList)
    			{
    				validDateStr = data.getValiddate().split(" ")[0];
    				station = data.getStation();
    				if(!dateRainDataMap.containsKey(station))
    	            {
    					dateRainDataMap.put(station, new HashMap<>());
    	            }
    				if(!dateRainDataMap.get(station).containsKey(validDateStr))
    				{
    					dateRainDataMap.get(station).put(validDateStr, new ArrayList<>());
    				}
    				dateRainDataMap.get(station).get(validDateStr).add(data);
    			}
    			
    			for(String stationNum : dateRainDataMap.keySet())
    			{
    				for(int i = 0, count = validDateList.size(); i < count; i++)
    				{
    					String validdate = validDateList.get(i);
//    					System.out.println("stationNum:" + stationNum + " validdate: " + validdate + " datas: " + dateRainDataMap.get(stationNum).get(validdate));
    					if(dateRainDataMap.get(stationNum).get(validdate) == null)
    					{
    						continue;
    					}
    					double[] value = getStationCalHourValueNum(vti, obsDataMap, rainDataMap, dateRainDataMap.get(stationNum).get(validdate));
    					CheckDataIndbEntity dataIndbEntity = resultIndbMap.get(stationNum).get(validdate);
    					if(dataIndbEntity != null)
    					{
    						ReflectUtil.setFieldValueByName(dataIndbEntity, value[0], "rain24h" + vti);
    						ReflectUtil.setFieldValueByName(dataIndbEntity, value[1], "rain24tn" + vti);
    						ReflectUtil.setFieldValueByName(dataIndbEntity, value[2], "rain24f" + vti);
    						ReflectUtil.setFieldValueByName(dataIndbEntity, value[3], "rain24m" + vti);
    						ReflectUtil.setFieldValueByName(dataIndbEntity, dataSource, "dataSource");
    					}
    				}
    			}
    			String startTime = param.getStartTime().split(" ")[0];
    			for(String stationStr : resultIndbMap.keySet())
    			{
    				CheckDataIndbEntity data = resultIndbMap.get(stationStr).get(calDate);
    				data.setDataTime(startTime);
    				Map<String, CheckDataIndbEntity> m = new HashMap<>();
    				m.put(startTime, data);
    				resultIndbMapNew.put(stationStr, m);
    			}
    			
    			resultIndbMap = resultIndbMapNew;
        	}
        	
        }

        return resultIndbMap;
    }
    
    private Map<String, Map<String, CheckDataIndbEntity>> calStationDayPre12(String dataSource, CheckDataParams param, List<ObsDataEntity> obsList, List<StationForecastDataEntity> stationForecastList)
    {
//        Map<String, Map<String, Map<String, Double>>> result = new LinkedHashMap<>();
//          station     datetime
        Map<String, Map<String, ObsDataEntity>> obsDataMap = new HashMap<>();
//          station     datetime_vti
        Map<String, Map<String, StationForecastDataEntity>> fstDataMap = new HashMap<>();
        Map<String, Map<String, StationForecastDataEntity>> rainDataMap = new HashMap<>();
        String[] stations = param.getStations();
        for(String station : stations)
        {
            obsDataMap.put(station, new HashMap<>());
            fstDataMap.put(station, new HashMap<>());
            rainDataMap.put(station, new HashMap<>());
        }

        for(ObsDataEntity obsData : obsList)
        {
            obsDataMap.get(obsData.getStation()).put(obsData.getDatetime(), obsData);
        }
        for(StationForecastDataEntity fstData : stationForecastList)
        {
            fstDataMap.get(fstData.getStation()).put(fstData.getDatatime() + "_" + fstData.getVti(), fstData);
        }
        String[] elements = param.getElements();

//          station     validdate
        Map<String, Map<String, List<StationForecastDataEntity>>> stationDateDataMap = new HashMap<>();
//         station      validdate
        Map<String, Map<String, CheckDataIndbEntity>> resultIndbMap = new HashMap<>();
        Map<String, Map<String, CheckDataIndbEntity>> resultIndbMapNew = new HashMap<>();
        String[] split = param.getStartTime().split(" ");
        String calDate = split[0];
        int hour = Integer.parseInt(split[1].split(":")[0]);
        int vti = param.getVti();
        CheckDataIndbEntity indbData = null;
        if(elements[0].equals("rain"))
        {
//        	if(!dataSource.equals(DataTypeEnum.SWC9KM.getDataType()) && param.getVti() != 240)
//        	{
//        		calDate = TimeUtil.addHours(param.getStartTime(), 24).split(" ")[0];
//        	}
//        	else
//        	{
//        		if(param.getVti() < 72)
//        		{
//        			calDate = TimeUtil.addHours(param.getStartTime(), 24).split(" ")[0];
//        		}
//        	}
        	if(dataSource.equals(DataTypeEnum.SWC9KM.getDataType()) && param.getVti() < 72)
        	{
        		calDate = TimeUtil.addHours(param.getStartTime(), 24).split(" ")[0];
        	}
        	else if(!dataSource.equals(DataTypeEnum.SWC9KM.getDataType()) && param.getVti() < 240)
        	{
        		calDate = TimeUtil.addHours(param.getStartTime(), 24).split(" ")[0];
        	}
        	else
        	{
        		calDate = TimeUtil.addHours(param.getStartTime(), 24).split(" ")[0];
        	}
            if(param.getVti() == 240 && hour == 8)
        	{
        		calDate = split[0];
        	}
        }

//        逐日
        String validDateStr = null;
        String station = null;
        Set<String> validDateSet = new HashSet<>();
        List<String> validDateList = new ArrayList<>();
        for(StationForecastDataEntity data : stationForecastList)
        {
            validDateStr = data.getValiddate().split(" ")[0];
            station = data.getStation();
            if(!stationDateDataMap.containsKey(station))
            {
            	stationDateDataMap.put(station, new HashMap<>());
            }
            if(!resultIndbMap.containsKey(station))
            {
            	resultIndbMap.put(station, new HashMap<>());
            	resultIndbMapNew.put(station, new HashMap<>());
            	indbData = new CheckDataIndbEntity();
            	indbData.setStation(station);
            	indbData.setDataTime(calDate);
            	indbData.setHour(hour);
            	resultIndbMap.get(station).put(calDate, indbData);
            }
            if(!stationDateDataMap.get(station).containsKey(validDateStr))
            {
            	stationDateDataMap.get(station).put(validDateStr, new ArrayList<>());
            }
            if(!param.getElements()[0].equals("rain"))
            {
            	if(data.getValiddate().compareTo(TimeUtil.addHours(param.getStartValidDate(), -8)) >= 0 ||
            	   data.getValiddate().compareTo(TimeUtil.addHours(param.getEndValidDate(), -8)) >= 0)
                {
            		stationDateDataMap.get(station).get(validDateStr).add(data);
                	validDateSet.add(validDateStr);
                }
            }
            else
            {
            	stationDateDataMap.get(station).get(validDateStr).add(data);
            	validDateSet.add(validDateStr);
            }
        }
        if(!elements[0].equals("rain"))
        {
        	Calendar calendar = Calendar.getInstance();
        	calendar.setTime(TimeUtil.String2Date(calDate, TimeUtil.DEFAULT_DATE_FORMAT));
        	calendar.add(Calendar.HOUR_OF_DAY, 24);
        	String dd = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATE_FORMAT);
        	for(String st : stationDateDataMap.keySet())
        	{
        		List<StationForecastDataEntity> list = stationDateDataMap.get(st).get(dd);
        		if(list != null && stationDateDataMap.get(st).get(calDate) != null)
        		{
        			stationDateDataMap.get(st).get(calDate).addAll(list);
        		}
        	}
        }
        validDateList.add(calDate);
//        validDateList = validDateSet.stream().sorted().collect(Collectors.toList());
//        int vti = param.getVti();
        int num = 0;
        for(String element : elements)
        {
        	ConfigParams configParams = new ConfigParams();
        	configParams.setElement(element);
        	List<MethodEntity> list = configMapper.getMethodByElement(configParams);
        	String[] methods = list.get(0).getMethod().split(",");
        	if(!element.equals("rain"))
        	{
        		int n = 0;
        		for(String method : methods)
            	{
        			for(String stationNum : stationDateDataMap.keySet())
        			{
        				for(String validdate : validDateList)
                        {
                            double value = DecodeConstants.UNDEF_DOUBLE_VALUE;
                            if(param.getVti() == 240)
                    		{
                            	value = getStationValuePre12(elements[num], methods[n], obsDataMap, stationDateDataMap.get(stationNum).get(validdate));
//                            	getGribCalHourValuePre12Num(obsDataMap, fstDataMap, rainValueList)
                    		}
                            else if(param.getVti() <= 48 || param.getVti() == 96 || param.getVti() == 120)
                            {
                            	value = getStationValuePre12(param.getDataSource(), param.getVti(), elements[num], methods[n], obsDataMap, stationDateDataMap.get(stationNum).get(validdate));
                            }
                            else
                            {
                            	value = getStationValue(elements[num], methods[n], obsDataMap, stationDateDataMap.get(stationNum).get(validdate));
                            }
                            CheckDataIndbEntity dataIndbEntity = resultIndbMap.get(stationNum).get(validdate);
        					if(dataIndbEntity != null)
        					{
        						ReflectUtil.setFieldValueByName(dataIndbEntity, value, element + methodMap.get(method) + vti);
        						ReflectUtil.setFieldValueByName(dataIndbEntity, dataSource, "dataSource");
        					}
                        }
        			}
        			n++;
            	}
        	}
        	else
        	{
        		for(StationForecastDataEntity fstData : stationForecastList)
                {
                    rainDataMap.get(fstData.getStation()).put(fstData.getDatatime() + "_" + fstData.getVti(), fstData);
                }

                Map<String, Map<String, List<StationForecastDataEntity>>> dateRainDataMap = new HashMap<>();
                for(StationForecastDataEntity data : stationForecastList)
                {
                    validDateStr = data.getValiddate().split(" ")[0];
    				station = data.getStation();
    				if(!dateRainDataMap.containsKey(station))
    	            {
    					dateRainDataMap.put(station, new HashMap<>());
    	            }
    				if(!dateRainDataMap.get(station).containsKey(validDateStr))
    				{
    					dateRainDataMap.get(station).put(validDateStr, new ArrayList<>());
    				}
    				dateRainDataMap.get(station).get(validDateStr).add(data);
                }
                
                for(String stationNum : stationDateDataMap.keySet())
    			{
                	for(int i = 0, count = validDateList.size(); i < count; i++)
                    {
                    	String validdate = validDateList.get(i);
                    	List<StationForecastDataEntity> list2 = dateRainDataMap.get(stationNum).get(validdate);
                    	if(list2 == null)
                    	{
                    		continue;
                    	}
                        double[] value = null;
                        if(param.getVti() == 240 || (dataSource.equals(DataTypeEnum.SWC9KM.getDataType()) && param.getVti() == 72))
                		{
                        	value = getStationCalHourValuePre12Num(vti, obsDataMap, rainDataMap, list2);
                		}
                        else
                        {
                        	value = getStationCalHourValueNumOrg(vti, obsDataMap, rainDataMap, list2);
                        }
                        CheckDataIndbEntity dataIndbEntity = resultIndbMap.get(stationNum).get(validdate);
    					if(dataIndbEntity != null)
    					{
    						ReflectUtil.setFieldValueByName(dataIndbEntity, value[0], "rain24h" + vti);
    						ReflectUtil.setFieldValueByName(dataIndbEntity, value[1], "rain24tn" + vti);
    						ReflectUtil.setFieldValueByName(dataIndbEntity, value[2], "rain24f" + vti);
    						ReflectUtil.setFieldValueByName(dataIndbEntity, value[3], "rain24m" + vti);
    						ReflectUtil.setFieldValueByName(dataIndbEntity, dataSource, "dataSource");
    					}
                    }
    			}
                String startTime = param.getStartTime().split(" ")[0];
    			for(String stationStr : resultIndbMap.keySet())
    			{
    				CheckDataIndbEntity data = resultIndbMap.get(stationStr).get(calDate);
    				data.setDataTime(startTime);
    				Map<String, CheckDataIndbEntity> m = new HashMap<>();
    				m.put(startTime, data);
    				resultIndbMapNew.put(stationStr, m);
    			}
    			resultIndbMap = resultIndbMapNew;
        	}
        	num++;
        }
        
        return resultIndbMap;
    }

    @Override
    public Map<String, Map<String, Map<String, Double>>> checkGribDataHour(CheckDataParams param) {

        String tableName = tableNameMap.get(param.getDataSource() + "_rain_value");
        param.setTableName(tableName);

//        datasource tableName
        Map<String, String> tableNamesMap = new HashMap<>();
        if(param.getDataSources() != null && param.getDataSources().length != 0)
        {
            for(String dataSource : param.getDataSources())
            {
                tableNamesMap.put(dataSource, tableNameMap.get(dataSource + "_rain_value"));
            }
        }

        if(param.getDataSourcesOrg() != null && param.getDataSourcesOrg().length != 0)
        {
            for(String dataSource : param.getDataSourcesOrg())
            {
                tableNamesMap.put(dataSource + "_org", tableNameMap.get(dataSource + "_value"));
            }
        }


        if(param.getZone() != null)
        {
            ConfigParams params = new ConfigParams();
            params.setZone(param.getZone());
            List<StationEntity> stationEntities = null;
            if(param.getZone() != 6)
            {
                stationEntities = stationForecastDataMapper.queryForecastStationsByZone(param);
            }
            else
            {
                stationEntities = stationForecastDataMapper.queryForecastAllStations(param);
            }
            int count = stationEntities.size();
            if(count == 0)
            {
            	return new HashMap<>();
            }
            String[] stations = new String[count];
            for(int i = 0; i < count; i++)
            {
                stations[i] = stationEntities.get(i).getStation();
            }
            param.setStations(stations);
        }
        else
        {
            Double lonLeft = param.getLonLeft();
            Double lonRight = param.getLonRight();
            Double latUp = param.getLatUp();
            Double latDown = param.getLatDown();
            if(lonLeft != null && lonRight != null && latUp != null && latDown != null)
            {
                List<StationEntity> stationEntities = stationForecastDataMapper.queryForecastAllStationsLonLat(param);
                List<String> stationList = new ArrayList<>();
                for(StationEntity stationEntity : stationEntities)
                {
                    if(stationEntity.getLon() >= lonLeft && stationEntity.getLon() <= lonRight && stationEntity.getLat() <= latUp && stationEntity.getLat() >= latDown)
                    {
                        stationList.add(stationEntity.getStation());
                    }
                }
                String[] stations = new String[stationList.size()];
                for(int i = 0; i < stations.length; i++)
                {
                    stations[i] = stationList.get(i);
                }
                param.setStations(stations);
            }
        }

        String startTimeStr = param.getStartValidDate();
        String endTimeStr = param.getEndValidDate();
        int hour = Integer.parseInt(startTimeStr.split(" ")[1].split(":")[0]);
//        int vti = param.getVti();
        int vti = 24;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(TimeUtil.String2Date(param.getStartValidDate(), TimeUtil.DEFAULT_DATETIME_FORMAT));
        calendar.add(Calendar.HOUR_OF_DAY, -vti);
        param.setStartValidDate(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));

        long time = System.currentTimeMillis();
//        List<GribForecastRainValueEntity> gribForecastList = gribForecastDataMapper.queryGribForecastRainCheckHour(param);

        param.setEndValidDate(TimeUtil.addHours(param.getEndValidDate(), 24));
        Map<String, List<GribForecastRainValueEntity>> datasMap = new HashMap<>();
        for(String dataSource : tableNamesMap.keySet())
        {
            param.setTableName(tableNamesMap.get(dataSource));
            List<GribForecastRainValueEntity> gribForecastDataRainEntities = null;
            if(dataSource.contains("_org"))
            {
            	if(hour == 8)
            	{
            		param.setHour(20);
            	}
            	else
            	{
            		param.setHour(8);
            	}
            	for(int i = 0, count = param.getVtis().length - 1; i < count; i++)
                {
                	param.getVtis()[i] += 12;
                }
                gribForecastDataRainEntities = gribForecastDataMapper.queryGribForecastRainCheckHourOrg(param);
                for(int i = 0, count = param.getVtis().length - 1; i < count; i++)
                {
                	param.getVtis()[i] -= 12;
                }
            }
            else
            {
            	param.setHour(hour);
                gribForecastDataRainEntities = gribForecastDataMapper.queryGribForecastRainCheckHour(param);
            }
//            List<GribForecastRainValueEntity> gribForecastDataRainEntities = gribForecastDataMapper.queryGribForecastRainCheckHour(param);
            if(gribForecastDataRainEntities == null || gribForecastDataRainEntities.size() == 0)
            {
                continue;
            }
            
            if(dataSource.equals(DataTypeEnum.ECMF.getDataType() + "_org"))
            {
            	for(GribForecastRainValueEntity data : gribForecastDataRainEntities)
                {
                    data.setRain(data.getRain() * 1000);
                    data.setDataTime(TimeUtil.addHours(data.getDataTime(), -8));
                    data.setValidDate(TimeUtil.addHours(data.getValidDate(), -8));
                }
            }
            else
            {
            	for(GribForecastRainValueEntity data : gribForecastDataRainEntities)
                {
                    data.setDataTime(TimeUtil.addHours(data.getDataTime(), -8));
                    data.setValidDate(TimeUtil.addHours(data.getValidDate(), -8));
                }
            }
            
            datasMap.put(dataSource, gribForecastDataRainEntities);
        }

//        System.out.println("查询预报数据耗时: " + (System.currentTimeMillis() - time));

        calendar.setTime(TimeUtil.String2Date(startTimeStr, TimeUtil.DEFAULT_DATETIME_FORMAT));
        calendar.add(Calendar.HOUR_OF_DAY, -vti);
        calendar.add(Calendar.HOUR_OF_DAY, -8);
        param.setStartTime(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));

        param.setEndValidDate(TimeUtil.addHours(endTimeStr, -8));
        param.setEndValidDate(TimeUtil.addHours(param.getEndValidDate(), 24));

        time = System.currentTimeMillis();
        List<ObsDataEntity> obsList = obsDataMapper.queryObsCheckData(param);
//        System.out.println("查询实况数据耗时: " + (System.currentTimeMillis() - time));
//        orgDatasMap.put("org", gribForecastList);
//          datasource  method      vti
        Map<String, Map<String, Map<String, Double>>> result = new HashMap<>();
        for(String dataSource : datasMap.keySet())
        {
//        	System.out.println("dataSource: " + dataSource);
            Map<String, Map<String, Double>> calGribHour = null;
            if(dataSource.endsWith("_org"))
            {
            	calGribHour = calGribHourPre12(param, obsList, datasMap.get(dataSource));
            }
            else
            {
            	calGribHour = calGribHour(param, obsList, datasMap.get(dataSource));
            }
            result.put(dataSource, calGribHour);
        }

        String next = result.keySet().iterator().next();

        for(String dataSource : tableNamesMap.keySet())
        {
            if(!result.containsKey(dataSource))
            {
                result.put(dataSource, new LinkedHashMap<>());

                for(String method : result.get(next).keySet())
                {
                    result.get(dataSource).put(method, new LinkedHashMap<>());
                    for(String vtiStr : result.get(next).get(method).keySet())
                    {
                        result.get(dataSource).get(method).put(vtiStr, null);
                    }
                }
            }
        }
        String[] resultOrder = new String[]{"ecmf_org", "ecmf", "grapes_org", "grapes", "deep"};
        Map<String, Map<String, Map<String, Double>>> resultMap = new LinkedHashMap<>();
        for(String order : resultOrder)
        {
        	Map<String, Map<String, Double>> map = result.get(order);
        	if(map != null)
        	{
        		resultMap.put(order, map);
        	}
        }



        return resultMap;
    }
    

    private Map<String, Map<String, Double>> calGribHour(CheckDataParams param, List<ObsDataEntity> obsList, List<GribForecastRainValueEntity> gribForecastList)
    {
//          station     datetime
        Map<String, Map<String, ObsDataEntity>> obsDataMap = new HashMap<>();
//          station     datetime_vti
        Map<String, Map<String, GribForecastRainValueEntity>> fstDataMap = new HashMap<>();
        String[] stations = param.getStations();
        for(String station : stations)
        {
            obsDataMap.put(station, new HashMap<>());
            fstDataMap.put(station, new HashMap<>());
        }

//        Calendar calendar = Calendar.getInstance();
        for(ObsDataEntity obsData : obsList)
        {
            obsDataMap.get(obsData.getStation()).put(obsData.getDatetime(), obsData);
        }
        for(GribForecastRainValueEntity fstData : gribForecastList)
        {
            fstDataMap.get(fstData.getStation()).put(fstData.getDataTime() + "_" + fstData.getVti(), fstData);
        }


//           validdate
        Map<String, List<GribForecastRainValueEntity>> datasfstMap = new HashMap<>();

//        逐时
        String vtiStr = null;
        for(GribForecastRainValueEntity data : gribForecastList)
        {
            vtiStr = String.valueOf(data.getVti());
            if(!datasfstMap.containsKey(vtiStr))
            {
                datasfstMap.put(vtiStr, new ArrayList<>());
            }
            datasfstMap.get(vtiStr).add(data);
        }

        Map<String, double[]> result = new HashMap<>();
        for(String vti : datasfstMap.keySet())
        {
            List<GribForecastRainValueEntity> gribForecastRainValueEntities = datasfstMap.get(vti);
            double[] gribCalValue = getGribCalHourValue(obsDataMap, fstDataMap, gribForecastRainValueEntities);
            result.put(vti, gribCalValue);
        }
        result.remove("0");


        Map<String, Integer> methodIndex = new HashMap<>();
        methodIndex.put("TS评分", 0);
        methodIndex.put("晴雨准确率", 1);
        methodIndex.put("空报率", 2);
        methodIndex.put("漏报率", 3);
//      method          vti
        Map<String, Map<String, Double>> resultMap = new LinkedHashMap<>();
        for(String method : param.getMethods())
        {
            resultMap.put(method, new LinkedHashMap<>());
            String vtiString = null;
            for(int vti : param.getVtis())
            {
                if(vti == 0)
                {
                    continue;
                }
                vtiString = String.valueOf(vti);
                if(result.size() == 0)
                {
                    resultMap.get(method).put(vtiString, null);
                }
                else
                {
                    resultMap.get(method).put(vtiString, result.get(vtiString) == null ? null : result.get(vtiString)[methodIndex.get(method)]);
                }
            }
        }



        return resultMap;
    }
    
    
    private Map<String, Map<String, Double>> calGribHourPre12(CheckDataParams param, List<ObsDataEntity> obsList, List<GribForecastRainValueEntity> gribForecastList)
    {
//          station     datetime
    	Map<String, Map<String, ObsDataEntity>> obsDataMap = new HashMap<>();
//          station     datetime_vti
    	Map<String, Map<String, GribForecastRainValueEntity>> fstDataMap = new HashMap<>();
    	String[] stations = param.getStations();
    	for(String station : stations)
    	{
    		obsDataMap.put(station, new HashMap<>());
    		fstDataMap.put(station, new HashMap<>());
    	}
    	
    	for(ObsDataEntity obsData : obsList)
    	{
    		obsDataMap.get(obsData.getStation()).put(obsData.getDatetime(), obsData);
    	}
    	for(GribForecastRainValueEntity fstData : gribForecastList)
    	{
    		fstDataMap.get(fstData.getStation()).put(fstData.getDataTime() + "_" + (fstData.getVti()), fstData);
    	}
    	
    	
//           validdate
    	Map<String, List<GribForecastRainValueEntity>> datasfstMap = new HashMap<>();
    	
//        逐时
    	String vtiStr = null;
    	for(GribForecastRainValueEntity data : gribForecastList)
    	{
    		vtiStr = String.valueOf(data.getVti() - 12);
    		if(!datasfstMap.containsKey(vtiStr))
    		{
    			datasfstMap.put(vtiStr, new ArrayList<>());
    		}
    		datasfstMap.get(vtiStr).add(data);
    	}
    	
    	Map<String, double[]> result = new HashMap<>();
    	for(String vtiString : datasfstMap.keySet())
    	{
    		int vti = Integer.parseInt(vtiString);
    		List<GribForecastRainValueEntity> gribForecastRainValueEntities = datasfstMap.get(vtiString);
    		double[] gribCalValue = getGribCalHourValuePre12(vti, obsDataMap, fstDataMap, gribForecastRainValueEntities);
    		if(vti == 228)
    		{
    			result.put("240", gribCalValue);
    		}
    		else
    		{
    			result.put(vtiString, gribCalValue);
    		}
    	}
    	result.remove("0");
    	
    	Map<String, Integer> methodIndex = new HashMap<>();
    	methodIndex.put("TS评分", 0);
    	methodIndex.put("晴雨准确率", 1);
    	methodIndex.put("空报率", 2);
    	methodIndex.put("漏报率", 3);
//      method          vti
    	Map<String, Map<String, Double>> resultMap = new LinkedHashMap<>();
    	for(String method : param.getMethods())
    	{
    		resultMap.put(method, new LinkedHashMap<>());
    		String vtiString = null;
    		for(int vti : param.getVtis())
    		{
    			if(vti == 0)
    			{
    				continue;
    			}
    			vtiString = String.valueOf(vti);
    			if(result.size() == 0)
    			{
    				resultMap.get(method).put(vtiString, null);
    			}
    			else
    			{
    				resultMap.get(method).put(vtiString, result.get(vtiString) == null ? null : result.get(vtiString)[methodIndex.get(method)]);
    			}
    		}
    	}
    	
    	
    	
    	return resultMap;
    }

    @Override
    public Map<String, Map<String, Map<String, CheckDataGribIndbEntity>>> checkGribDataDay(CheckDataParams param) {
        String tableName = tableNameMap.get(param.getDataSource() + "_rain_value");
        param.setTableName(tableName);

        String startTimeStr = param.getStartValidDate();
        String endTimeStr = param.getEndValidDate();

        Map<String, String> tableNamesMap = new HashMap<>();
        if(param.getDataSources() != null && param.getDataSources().length != 0)
        {
            for(String dataSource : param.getDataSources())
            {
                tableNamesMap.put(dataSource, tableNameMap.get(dataSource + "_rain_value"));
            }
        }

        if(param.getDataSourcesOrg() != null && param.getDataSourcesOrg().length != 0)
        {
            for(String dataSource : param.getDataSourcesOrg())
            {
                tableNamesMap.put(dataSource + "_org", tableNameMap.get(dataSource + "_value"));
            }
        }

        if(param.getZone() != null)
        {
            ConfigParams params = new ConfigParams();
            params.setZone(param.getZone());
            List<StationEntity> stationEntities = null;
            if(param.getZone() != 6)
            {
                stationEntities = stationForecastDataMapper.queryForecastStationsByZone(param);
            }
            else
            {
                stationEntities = stationForecastDataMapper.queryForecastAllStations(param);
            }
            int count = stationEntities.size();
            if(count == 0)
            {
            	return new HashMap<>();
            }
            String[] stations = new String[count];
            for(int i = 0; i < count; i++)
            {
                stations[i] = stationEntities.get(i).getStation();
            }
            param.setStations(stations);
        }
        else
        {
            Double lonLeft = param.getLonLeft();
            Double lonRight = param.getLonRight();
            Double latUp = param.getLatUp();
            Double latDown = param.getLatDown();
            if(lonLeft != null && lonRight != null && latUp != null && latDown != null)
            {
                List<StationEntity> stationEntities = stationForecastDataMapper.queryForecastAllStationsLonLat(param);
                List<String> stationList = new ArrayList<>();
                for(StationEntity stationEntity : stationEntities)
                {
                    if(stationEntity.getLon() >= lonLeft && stationEntity.getLon() <= lonRight && stationEntity.getLat() <= latUp && stationEntity.getLat() >= latDown)
                    {
                        stationList.add(stationEntity.getStation());
                    }
                }
                String[] stations = new String[stationList.size()];
                for(int i = 0; i < stationList.size(); i++)
                {
                    stations[i] = stationList.get(i);
                }
                param.setStations(stations);
            }
        }
        
//        param.setStations(new String[]{"55564"});

        int vti = param.getVti();
        if(param.getVtis() == null || param.getVtis().length == 0)
        {
            param.setVtis(new int[]{vti});
        }
        param.setVtis(new int[]{vti - 24, vti});
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(TimeUtil.String2Date(startTimeStr, TimeUtil.DEFAULT_DATETIME_FORMAT));
        calendar.add(Calendar.HOUR_OF_DAY, -24);
        param.setStartValidDate(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));

        int hour = Integer.parseInt(startTimeStr.split(" ")[1].split(":")[0]);
        
//        long time = System.currentTimeMillis();

        Map<String, List<GribForecastRainValueEntity>> orgDatasMap = new HashMap<>();
        param.setEndValidDate(TimeUtil.addHours(endTimeStr, 24));
        for(String dataSource : tableNamesMap.keySet())
        {
            param.setTableName(tableNamesMap.get(dataSource));
            List<GribForecastRainValueEntity> gribForecastDataRainEntities = null;
            if(dataSource.contains("_org"))
            {
            	if(hour == 8)
            	{
            		param.setHour(20);
            	}
            	else
            	{
            		param.setHour(8);
            	}
            	for(int i = 0, count = param.getVtis().length; i < count; i++)
                {
            		if(param.getVtis()[i] == 240)
            		{
            			continue;
            		}
                	param.getVtis()[i] += 12;
                }
            	param.setEndValidDate(TimeUtil.addHours(endTimeStr, 12 + 24));
                gribForecastDataRainEntities = gribForecastDataMapper.queryGribForecastRainCheckDayOrg(param);
                for(int i = 0, count = param.getVtis().length; i < count; i++)
                {
                	if(param.getVtis()[i] == 240)
            		{
            			continue;
            		}
                	param.getVtis()[i] -= 12;
                }
            }
            else
            {
            	param.setHour(hour);
            	if(param.getVti() == 240)
            	{
            		param.getVtis()[1] = 228;
            	}
            	param.setEndValidDate(TimeUtil.addHours(endTimeStr, 24));
                gribForecastDataRainEntities = gribForecastDataMapper.queryGribForecastRainCheckDay(param);
            }
            if(gribForecastDataRainEntities == null || gribForecastDataRainEntities.size() == 0)
            {
                continue;
            }
            
            if(dataSource.equals(DataTypeEnum.ECMF.getDataType() + "_org"))
            {
            	for(GribForecastRainValueEntity data : gribForecastDataRainEntities)
                {
                    data.setRain(data.getRain() * 1000);
                    data.setDataTime(TimeUtil.addHours(data.getDataTime(), -8));
                    data.setValidDate(TimeUtil.addHours(data.getValidDate(), -8));
                }
            }
            else
            {
            	for(GribForecastRainValueEntity data : gribForecastDataRainEntities)
                {
                    data.setDataTime(TimeUtil.addHours(data.getDataTime(), -8));
                    data.setValidDate(TimeUtil.addHours(data.getValidDate(), -8));
                }
            }
            
            orgDatasMap.put(dataSource, gribForecastDataRainEntities);
        }



//        System.out.println("查询预报数据耗时: " + (System.currentTimeMillis() - time));

        calendar.setTime(TimeUtil.String2Date(startTimeStr, TimeUtil.DEFAULT_DATETIME_FORMAT));
        calendar.add(Calendar.HOUR_OF_DAY, -vti);
        calendar.add(Calendar.HOUR_OF_DAY, -8);
        param.setStartTime(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
        param.setEndValidDate(TimeUtil.addHours(endTimeStr, -8 + 24 + 12));
//        param.setEndValidDate(TimeUtil.addHours(param.getEndValidDate(), 24));
//        param.setEndValidDate(TimeUtil.addHours(param.getEndValidDate(), 12));

//        time = System.currentTimeMillis();
        List<ObsDataEntity> obsList = obsDataMapper.queryObsCheckData(param);
//        System.out.println("查询实况数据耗时: " + (System.currentTimeMillis() - time));
//        orgDatasMap.put("org", gribForecastList);

//        time = System.currentTimeMillis();
        param.setStartTime(TimeUtil.addHours(startTimeStr, -8));
        Map<String, Map<String, Map<String, CheckDataGribIndbEntity>>> result = new HashMap<>();
        for(String dataSource : orgDatasMap.keySet())
        {
        	Map<String, Map<String, CheckDataGribIndbEntity>> calGribHour = null;
            if(dataSource.endsWith("_org"))
            {
            	calGribHour = calGribDayPre12(dataSource, param, obsList, orgDatasMap.get(dataSource));
            }
            else
            {
            	calGribHour = calGribDay(dataSource, param, obsList, orgDatasMap.get(dataSource));
            }
            result.put(dataSource, calGribHour);
        }
//        System.out.println("计算耗时: " + (System.currentTimeMillis() - time));
        
        System.out.println(startTimeStr + " 格点检验计算完成。");

        return result;
    }

    private Map<String, Map<String, CheckDataGribIndbEntity>> calGribDay(String dataSource, CheckDataParams param, List<ObsDataEntity> obsList, List<GribForecastRainValueEntity> gribForecastList)
    {
//          station     datetime
        Map<String, Map<String, ObsDataEntity>> obsDataMap = new HashMap<>();
//          station     datetime
        Map<String, Map<String, GribForecastRainValueEntity>> fstDataMap = new HashMap<>();
        String[] stations = param.getStations();
//        long time = System.currentTimeMillis();
        for(String station : stations)
        {
            obsDataMap.put(station, new HashMap<>());
            fstDataMap.put(station, new HashMap<>());
        }
//        System.out.println("每种数据源预处理111111111111111耗时: " + (System.currentTimeMillis() - time));
        
//        time = System.currentTimeMillis();

//        Calendar calendar = Calendar.getInstance();
        Set<String> obsDateTimeSet = new HashSet<>();
        for(ObsDataEntity obsData : obsList)
        {
            obsDataMap.get(obsData.getStation()).put(obsData.getDatetime(), obsData);
            obsDateTimeSet.add(obsData.getDatetime());
        }
//        System.out.println("每种数据源预处理222222222222222耗时: " + (System.currentTimeMillis() - time));
        
//        time = System.currentTimeMillis();
        for(GribForecastRainValueEntity fstData : gribForecastList)
        {
            fstDataMap.get(fstData.getStation()).put(fstData.getDataTime() + "_" + fstData.getVti(), fstData);
        }
//        System.out.println("每种数据源预处理333333333333333耗时: " + (System.currentTimeMillis() - time));
        
//        time = System.currentTimeMillis();
//      station   validdate
        Map<String, Map<String, List<GribForecastRainValueEntity>>> datasfstMap = new HashMap<>();
// 		station      validdate
        Map<String, Map<String, CheckDataGribIndbEntity>> resultIndbMap = new HashMap<>();
        Map<String, Map<String, CheckDataGribIndbEntity>> resultIndbMapNew = new HashMap<>();
        String[] split = param.getStartTime().split(" ");
        int orgHour = Integer.parseInt(split[1].split(":")[0]);
        String calDate = split[0];
        if(param.getElements()[0].equals("rain"))
        {
        	String dateStr = param.getStartTime();
        	calDate = TimeUtil.addHours(dateStr, 24).split(" ")[0];
        	if(param.getVti() == 240 && orgHour == 0)
        	{
        		calDate = split[0];
        	}
        }
        int hour = Integer.parseInt(split[1].split(":")[0]);
        int vti = param.getVti();
        CheckDataGribIndbEntity indbData = null;
//        逐日
        String validDateStr = null;
        String station = null;
        List<String> validDateList = new ArrayList<>();
        Set<String> validDateSet = new HashSet<>();
        for(GribForecastRainValueEntity data : gribForecastList)
        {
            validDateStr = data.getValidDate().split(" ")[0];
            station = data.getStation();
            if(!datasfstMap.containsKey(station))
            {
            	datasfstMap.put(station, new HashMap<>());
            }
            if(!resultIndbMap.containsKey(station))
            {
            	resultIndbMap.put(station, new HashMap<>());
            	indbData = new CheckDataGribIndbEntity();
            	indbData.setStation(station);
            	indbData.setDataTime(calDate);
            	indbData.setHour(hour);
            	resultIndbMap.get(station).put(calDate, indbData);
            }
            if(!datasfstMap.get(station).containsKey(validDateStr))
    		{
    			datasfstMap.get(station).put(validDateStr, new ArrayList<>());
    		}
            validDateSet.add(validDateStr);
            datasfstMap.get(station).get(validDateStr).add(data);
        }
        validDateList.add(calDate);
//        validDateList = validDateSet.stream().sorted().collect(Collectors.toList());
        
        
//        System.out.println("每种数据源预处理444444444444444耗时: " + (System.currentTimeMillis() - time));
        
//        time = System.currentTimeMillis();
//        Map<String, double[]> result = new HashMap<>();
        
        for(String stationNum : datasfstMap.keySet())
		{
        	for(int i = 0, count = validDateList.size(); i < count; i++)
            {
            	String validDate = validDateList.get(i);
                List<GribForecastRainValueEntity> gribForecastRainValueEntities = datasfstMap.get(stationNum).get(validDate);
                if(gribForecastRainValueEntities == null)
                {
                	continue;
                }
                double[] gribCalValue = null;
                if(param.getVti() == 240)
	    		{
	    			gribCalValue = getGribCalHourValuePre12Num(obsDataMap, fstDataMap, gribForecastRainValueEntities);
	    		}
                else
                {
                	gribCalValue = getGribCalHourValueNum(obsDataMap, fstDataMap, gribForecastRainValueEntities);
                }
                
                CheckDataGribIndbEntity dataIndbEntity = resultIndbMap.get(stationNum).get(validDate);
				if(dataIndbEntity != null)
				{
					ReflectUtil.setFieldValueByName(dataIndbEntity, (int)gribCalValue[0], "rain24h" + vti);
					ReflectUtil.setFieldValueByName(dataIndbEntity, (int)gribCalValue[1], "rain24tn" + vti);
					ReflectUtil.setFieldValueByName(dataIndbEntity, (int)gribCalValue[2], "rain24f" + vti);
					ReflectUtil.setFieldValueByName(dataIndbEntity, (int)gribCalValue[3], "rain24m" + vti);
					ReflectUtil.setFieldValueByName(dataIndbEntity, dataSource, "dataSource");
				}
                
//                validDate = validDateList.get(i - 1);
//                result.put(validDate, gribCalValue);
            }
		}
        
        String startTime = param.getStartTime().split(" ")[0];
		for(String stationStr : resultIndbMap.keySet())
		{
			CheckDataGribIndbEntity data = resultIndbMap.get(stationStr).get(calDate);
			data.setDataTime(startTime);
			Map<String, CheckDataGribIndbEntity> m = new HashMap<>();
			m.put(startTime, data);
			resultIndbMapNew.put(stationStr, m);
		}
		resultIndbMap = resultIndbMapNew;
        
        return resultIndbMap;
    }
    
    private Map<String, Map<String, CheckDataGribIndbEntity>> calGribDayPre12(String dataSource, CheckDataParams param, List<ObsDataEntity> obsList, List<GribForecastRainValueEntity> gribForecastList)
    {
//          station     datetime
    	Map<String, Map<String, ObsDataEntity>> obsDataMap = new HashMap<>();
//          station     datetime
    	Map<String, Map<String, GribForecastRainValueEntity>> fstDataMap = new HashMap<>();
    	String[] stations = param.getStations();
//    	long time = System.currentTimeMillis();
    	for(String station : stations)
    	{
    		obsDataMap.put(station, new HashMap<>());
    		fstDataMap.put(station, new HashMap<>());
    	}
//    	System.out.println("每种数据源预处理111111111111111耗时: " + (System.currentTimeMillis() - time));
    	
//    	time = System.currentTimeMillis();
    	
//        Calendar calendar = Calendar.getInstance();
    	Set<String> obsDateTimeSet = new HashSet<>();
    	for(ObsDataEntity obsData : obsList)
    	{
    		obsDataMap.get(obsData.getStation()).put(obsData.getDatetime(), obsData);
    		obsDateTimeSet.add(obsData.getDatetime());
    	}
//    	System.out.println("每种数据源预处理222222222222222耗时: " + (System.currentTimeMillis() - time));
    	
//    	time = System.currentTimeMillis();
    	for(GribForecastRainValueEntity fstData : gribForecastList)
    	{
    		fstDataMap.get(fstData.getStation()).put(fstData.getDataTime() + "_" + fstData.getVti(), fstData);
    	}
//    	System.out.println("每种数据源预处理333333333333333耗时: " + (System.currentTimeMillis() - time));
    	
//    	time = System.currentTimeMillis();
//           station   validdate
    	Map<String, Map<String, List<GribForecastRainValueEntity>>> datasfstMap = new HashMap<>();
//      	station      validdate
    	Map<String, Map<String, CheckDataGribIndbEntity>> resultIndbMap = new HashMap<>();
    	Map<String, Map<String, CheckDataGribIndbEntity>> resultIndbMapNew = new HashMap<>();
        String[] split = param.getStartTime().split(" ");
        String calDate = split[0];
        int orgHour = Integer.parseInt(split[1].split(":")[0]);
        if(param.getElements()[0].equals("rain"))
        {
        	String dateStr = param.getStartTime();
        	calDate = TimeUtil.addHours(dateStr, 24).split(" ")[0];
        	if(param.getVti() == 240 && orgHour == 0)
        	{
        		calDate = split[0];
        	}
        }
        int hour = param.getHour();
        int vti = param.getVti();
        CheckDataGribIndbEntity indbData = null;
    	
//        逐日
    	String validDateStr = null;
    	String station = null;
    	List<String> validDateList = new ArrayList<>();
    	Set<String> validDateSet = new HashSet<>();
    	for(GribForecastRainValueEntity data : gribForecastList)
    	{
    		validDateStr = data.getValidDate().split(" ")[0];
    		station = data.getStation();
            if(!datasfstMap.containsKey(station))
            {
            	datasfstMap.put(station, new HashMap<>());
            }
            if(!resultIndbMap.containsKey(station))
            {
            	resultIndbMap.put(station, new HashMap<>());
            	indbData = new CheckDataGribIndbEntity();
            	indbData.setStation(station);
            	indbData.setDataTime(calDate);
            	indbData.setHour(hour);
            	resultIndbMap.get(station).put(calDate, indbData);
            }
    		if(!datasfstMap.get(station).containsKey(validDateStr))
    		{
    			datasfstMap.get(station).put(validDateStr, new ArrayList<>());
    		}
    		validDateSet.add(validDateStr);
    		
    		datasfstMap.get(station).get(validDateStr).add(data);
    	}
    	validDateList.add(calDate);
//    	validDateList = validDateSet.stream().sorted().collect(Collectors.toList());
    	
    	
//    	System.out.println("每种数据源预处理444444444444444耗时: " + (System.currentTimeMillis() - time));
    	
//    	time = System.currentTimeMillis();
//    	Map<String, double[]> result = new HashMap<>();
    	
    	for(String stationNum : datasfstMap.keySet())
		{
			for(int i = 0, count = validDateList.size(); i < count; i++)
	    	{
	    		String validDate = validDateList.get(i);
	    		List<GribForecastRainValueEntity> gribForecastRainValueEntities = datasfstMap.get(stationNum).get(validDate);
	    		if(gribForecastRainValueEntities == null)
	    		{
	    			continue;
	    		}
	    		double[] gribCalValue = null;
	    		if(param.getVti() == 240)
	    		{
	    			gribCalValue = getGribCalHourValuePre12Num(obsDataMap, fstDataMap, gribForecastRainValueEntities);
	    		}
	    		else
	    		{
	    			gribCalValue = getGribCalHourValueNum(obsDataMap, fstDataMap, gribForecastRainValueEntities);
	    		}
	    		CheckDataGribIndbEntity dataIndbEntity = resultIndbMap.get(stationNum).get(validDate);
				if(dataIndbEntity != null)
				{
					ReflectUtil.setFieldValueByName(dataIndbEntity, (int)gribCalValue[0], "rain24h" + vti);
					ReflectUtil.setFieldValueByName(dataIndbEntity, (int)gribCalValue[1], "rain24tn" + vti);
					ReflectUtil.setFieldValueByName(dataIndbEntity, (int)gribCalValue[2], "rain24f" + vti);
					ReflectUtil.setFieldValueByName(dataIndbEntity, (int)gribCalValue[3], "rain24m" + vti);
					ReflectUtil.setFieldValueByName(dataIndbEntity, dataSource, "dataSource");
				}
//	    		validDate = validDateList.get(i - 1);
//	    		result.put(validDate, gribCalValue);
	    	}
		}
    	
    	String startTime = param.getStartTime().split(" ")[0];
		for(String stationStr : resultIndbMap.keySet())
		{
			CheckDataGribIndbEntity data = resultIndbMap.get(stationStr).get(calDate);
			data.setDataTime(startTime);
			Map<String, CheckDataGribIndbEntity> m = new HashMap<>();
			m.put(startTime, data);
			resultIndbMapNew.put(stationStr, m);
		}
		resultIndbMap = resultIndbMapNew;
    	
    	
    	
    	
//    	System.out.println("每种数据源计算耗时: " + (System.currentTimeMillis() - time));
    	
//    	Map<String, Integer> methodIndex = new HashMap<>();
//    	methodIndex.put("TS评分", 0);
//    	methodIndex.put("晴雨准确率", 1);
//    	methodIndex.put("空报率", 2);
//    	methodIndex.put("漏报率", 3);
    	
    	return resultIndbMap;
    }

    private double[] getStationCalHourValue(Map<String, Map<String, ObsDataEntity>> obsDataMap, Map<String, Map<String, StationForecastDataEntity>> fstDataMap, List<StationForecastDataEntity> rainValueList)
    {
        double h = 0;
        double tn = 0;
        double f = 0;
        double m = 0;
        double th = 0;
        double tm = 0;
        double tf = 0;
        double ts = 0;
        ObsDataEntity obsValue = null;
        double[] result = new double[4];
        for(StationForecastDataEntity rainValue : rainValueList)
        {
            obsValue = obsDataMap.get(rainValue.getStation()).get(rainValue.getValiddate());
            if(obsValue == null || rainValue.getVti() == 0)
            {
                continue;
            }
            double value = obsValue.getRain24() == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE ? 0 : obsValue.getRain24();
            if(value == DecodeConstants.UNDEF_DOUBLE_VALUE)
            {
            	continue;
            }
            double fstValue;
            int preVti = rainValue.getVti() - 24;
            StationForecastDataEntity dataEntity = fstDataMap.get(obsValue.getStation()).get(rainValue.getDatatime() + "_" + preVti);
            if(preVti == 0)
            {
            	dataEntity = new StationForecastDataEntity();
            	dataEntity.setRain24(0);
            }
            
            if(fstDataMap.get(obsValue.getStation()) == null || dataEntity == null)
            {
            	fstValue = 999999;
            }
            else
            {
                fstValue = rainValue.getRain24() - dataEntity.getRain24();
            }
            if(fstValue == 999999)
            {
                continue;
            }
//            System.out.println("obsDate: " + obsValue.getDatetime() + " fstDate: " + rainValue.getValiddate() + " fstTime: " + rainValue.getDatatime() + " vti: " + rainValue.getVti() + " value: " + value + ", fstValue: " + fstValue);
            if(value >= 0.1 && fstValue >= 0.1)
            {
                h++;
            }
            else if(value < 0.1 && fstValue < 0.1)
            {
                tn++;
            }
            else if(value >= 0.1 && fstValue < 0.1)
            {
                m++;
            }
            else if(value < 0.1 && fstValue >= 0.1)
            {
                f++;
            }
        }
        ts = h / ((h + m + f) == 0 ? 1 : (h + m + f));
        th = (h + tn) / ((h + f + m + tn) == 0 ? 1 : (h + f + m + tn));
        tf = f / ((f + h) == 0 ? 1 : (f + h));
        tm = m / ((m + tn) == 0 ? 1 : (tn + m));
//        System.out.println("h:" + h + ",tn:" + tn + ",m:" + m + ",f:" + f);
        result[0] = NumberFormatUtil.numFormat(ts, 3);   // TS评分
        result[1] = NumberFormatUtil.numFormat(th, 3);   // 准确率
        result[2] = NumberFormatUtil.numFormat(tf, 3);   // 空报
        result[3] = NumberFormatUtil.numFormat(tm, 3);   // 漏报
        if(h == 0 && tn == 0 && m == 0 && f == 0)
        {
        	result[0] = DecodeConstants.UNDEF_DOUBLE_VALUE;
        	result[1] = DecodeConstants.UNDEF_DOUBLE_VALUE;
        	result[2] = DecodeConstants.UNDEF_DOUBLE_VALUE;
        	result[3] = DecodeConstants.UNDEF_DOUBLE_VALUE;
        }
//        System.out.println("date: " + time + " rate: " + result[1]);
        
        return result;
    }
    
    private double[] getStationCalHourValueNum(int vti, Map<String, Map<String, ObsDataEntity>> obsDataMap, Map<String, Map<String, StationForecastDataEntity>> fstDataMap, List<StationForecastDataEntity> rainValueList)
    {
        double h = 0;
        double tn = 0;
        double f = 0;
        double m = 0;
        ObsDataEntity obsValue = null;
        double[] result = new double[4];
        for(StationForecastDataEntity rainValue : rainValueList)
        {
        	if(vti != rainValue.getVti())
        	{
        		continue;
        	}
            obsValue = obsDataMap.get(rainValue.getStation()).get(rainValue.getValiddate());
            if(obsValue == null || rainValue.getVti() == 0)
            {
                continue;
            }
            double value = obsValue.getRain24() == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE ? 0 : obsValue.getRain24();
            if(value == DecodeConstants.UNDEF_DOUBLE_VALUE)
            {
            	continue;
            }
            double fstValue;
            int preVti = rainValue.getVti() - 24;
            StationForecastDataEntity dataEntity = fstDataMap.get(obsValue.getStation()).get(rainValue.getDatatime() + "_" + preVti);
            if(preVti == 0)
            {
            	dataEntity = new StationForecastDataEntity();
            	dataEntity.setRain24(0);
            }
            
            if(fstDataMap.get(obsValue.getStation()) == null || dataEntity == null)
            {
            	fstValue = 999999;
            }
            else
            {
                fstValue = rainValue.getRain24() - dataEntity.getRain24();
            }
            if(fstValue == 999999)
            {
                continue;
            }
//            System.out.println("obsDate: " + obsValue.getDatetime() + " fstDate: " + rainValue.getValiddate() + " fstTime: " + rainValue.getDatatime() + " vti: " + rainValue.getVti() + " value: " + value + ", fstValue: " + fstValue);
            if(value >= 0.1 && fstValue >= 0.1)
            {
                h++;
            }
            else if(value < 0.1 && fstValue < 0.1)
            {
                tn++;
            }
            else if(value >= 0.1 && fstValue < 0.1)
            {
                m++;
            }
            else if(value < 0.1 && fstValue >= 0.1)
            {
                f++;
            }
        }
//        System.out.println("h:" + h + ",tn:" + tn + ",m:" + m + ",f:" + f);
//        ts = h / ((h + m + f) == 0 ? 1 : (h + m + f));
//        th = (h + tn) / ((h + f + m + tn) == 0 ? 1 : (h + f + m + tn));
//        tf = f / ((f + h) == 0 ? 1 : (f + h));
//        tm = m / ((m + tn) == 0 ? 1 : (tn + m));
        result[0] = h;   // 
        result[1] = tn;   // 
        result[2] = f;   //空报
        result[3] = m;   //漏报 
        
        return result;
    }

    private double[] getStationCalHourValueNumOrg(int vti, Map<String, Map<String, ObsDataEntity>> obsDataMap, Map<String, Map<String, StationForecastDataEntity>> fstDataMap, List<StationForecastDataEntity> rainValueList)
    {
        double h = 0;
        double tn = 0;
        double f = 0;
        double m = 0;
        ObsDataEntity obsValue = null;
        double[] result = new double[4];
        for(StationForecastDataEntity rainValue : rainValueList)
        {
        	if((vti + 12) != rainValue.getVti())
        	{
        		continue;
        	}
            obsValue = obsDataMap.get(rainValue.getStation()).get(rainValue.getValiddate());
            if(obsValue == null || rainValue.getVti() == 0)
            {
                continue;
            }
            double value = obsValue.getRain24() == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE ? 0 : obsValue.getRain24();
            if(value == DecodeConstants.UNDEF_DOUBLE_VALUE)
            {
            	continue;
            }
            double fstValue;
            int preVti = rainValue.getVti() - 24;
            StationForecastDataEntity dataEntity = fstDataMap.get(obsValue.getStation()).get(rainValue.getDatatime() + "_" + preVti);
            if(preVti == 0)
            {
            	dataEntity = new StationForecastDataEntity();
            	dataEntity.setRain24(0);
            }
            
            if(fstDataMap.get(obsValue.getStation()) == null || dataEntity == null)
            {
            	fstValue = 999999;
            }
            else
            {
                fstValue = rainValue.getRain24() - dataEntity.getRain24();
            }
            if(fstValue == 999999)
            {
                continue;
            }
//            System.out.println("obsDate: " + obsValue.getDatetime() + " fstDate: " + rainValue.getValiddate() + " fstTime: " + rainValue.getDatatime() + " vti: " + rainValue.getVti() + " value: " + value + ", fstValue: " + fstValue);
            if(value >= 0.1 && fstValue >= 0.1)
            {
                h++;
            }
            else if(value < 0.1 && fstValue < 0.1)
            {
                tn++;
            }
            else if(value >= 0.1 && fstValue < 0.1)
            {
                m++;
            }
            else if(value < 0.1 && fstValue >= 0.1)
            {
                f++;
            }
        }
        result[0] = h;   // 
        result[1] = tn;   // 
        result[2] = f;   //空报
        result[3] = m;   //漏报  
        
        return result;
    }
    
    private double[] getStationCalHourValuePre12(Map<String, Map<String, ObsDataEntity>> obsDataMap, Map<String, Map<String, StationForecastDataEntity>> fstDataMap, List<StationForecastDataEntity> rainValueList)
    {
        double h = 0;
        double tn = 0;
        double f = 0;
        double m = 0;
        double th = 0;
        double tm = 0;
        double tf = 0;
        double ts = 0;
        ObsDataEntity obsValue = null;
        double[] result = new double[4];
        for(StationForecastDataEntity rainValue : rainValueList)
        {
            obsValue = obsDataMap.get(rainValue.getStation()).get(rainValue.getValiddate());
            if(obsValue == null || rainValue.getVti() == 0)
            {
                continue;
            }
            double value = obsValue.getRain24() == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE ? 0 : obsValue.getRain24();
            if(value == DecodeConstants.UNDEF_DOUBLE_VALUE)
            {
            	continue;
            }
            double fstValue;
            int preVti = rainValue.getVti() - 12;
            StationForecastDataEntity dataEntity = fstDataMap.get(obsValue.getStation()).get(rainValue.getDatatime() + "_" + preVti);
            if(preVti == 0)
            {
            	dataEntity = new StationForecastDataEntity();
            	dataEntity.setRain24(0);
            }
            
            if(fstDataMap.get(obsValue.getStation()) == null || dataEntity == null)
            {
            	fstValue = 999999;
            }
            else
            {
                fstValue = rainValue.getRain24() - dataEntity.getRain24();
            }
            if(fstValue == 999999)
            {
                continue;
            }
//            System.out.println("obsDate: " + obsValue.getDatetime() + " fstDate: " + rainValue.getValiddate() + " fstTime: " + rainValue.getDatatime() + " vti: " + rainValue.getVti() + " value: " + value + ", fstValue: " + fstValue);
            if(value >= 0.1 && fstValue >= 0.1)
            {
                h++;
            }
            else if(value < 0.1 && fstValue < 0.1)
            {
                tn++;
            }
            else if(value >= 0.1 && fstValue < 0.1)
            {
                m++;
            }
            else if(value < 0.1 && fstValue >= 0.1)
            {
                f++;
            }
        }
        ts = h / ((h + m + f) == 0 ? 1 : (h + m + f));
        th = (h + tn) / ((h + f + m + tn) == 0 ? 1 : (h + f + m + tn));
        tf = f / ((f + h) == 0 ? 1 : (f + h));
        tm = m / ((m + tn) == 0 ? 1 : (tn + m));
//        System.out.println("h:" + h + ",tn:" + tn + ",m:" + m + ",f:" + f);
        result[0] = NumberFormatUtil.numFormat(ts, 3);   // TS评分
        result[1] = NumberFormatUtil.numFormat(th, 3);   // 准确率
        result[2] = NumberFormatUtil.numFormat(tf, 3);   // 空报
        result[3] = NumberFormatUtil.numFormat(tm, 3);   // 漏报
        if(h == 0 && tn == 0 && m == 0 && f == 0)
        {
        	result[0] = DecodeConstants.UNDEF_DOUBLE_VALUE;
        	result[1] = DecodeConstants.UNDEF_DOUBLE_VALUE;
        	result[2] = DecodeConstants.UNDEF_DOUBLE_VALUE;
        	result[3] = DecodeConstants.UNDEF_DOUBLE_VALUE;
        }
//        System.out.println("date: " + time + " rate: " + result[1]);
        
        return result;
    }
    
    private double[] getStationCalHourValuePre12Num(int vti, Map<String, Map<String, ObsDataEntity>> obsDataMap, Map<String, Map<String, StationForecastDataEntity>> fstDataMap, List<StationForecastDataEntity> rainValueList)
    {
        double h = 0;
        double tn = 0;
        double f = 0;
        double m = 0;
        ObsDataEntity obsValue = null;
        double[] result = new double[4];
        for(StationForecastDataEntity rainValue : rainValueList)
        {
        	if(vti != rainValue.getVti())
        	{
        		continue;
        	}
            obsValue = obsDataMap.get(rainValue.getStation()).get(rainValue.getValiddate());
            if(obsValue == null || rainValue.getVti() == 0)
            {
                continue;
            }
            double value = obsValue.getRain24() == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE ? 0 : obsValue.getRain24();
            if(value == DecodeConstants.UNDEF_DOUBLE_VALUE)
            {
            	continue;
            }
            double fstValue;
            int preVti = rainValue.getVti() - 12;
            StationForecastDataEntity dataEntity = fstDataMap.get(obsValue.getStation()).get(rainValue.getDatatime() + "_" + preVti);
            if(preVti == 0)
            {
            	dataEntity = new StationForecastDataEntity();
            	dataEntity.setRain24(0);
            }
            
            if(fstDataMap.get(obsValue.getStation()) == null || dataEntity == null)
            {
            	fstValue = 999999;
            }
            else
            {
                fstValue = rainValue.getRain24() - dataEntity.getRain24();
            }
            if(fstValue == 999999)
            {
                continue;
            }
//            System.out.println("obsDate: " + obsValue.getDatetime() + " fstDate: " + rainValue.getValiddate() + " fstTime: " + rainValue.getDatatime() + " vti: " + rainValue.getVti() + " value: " + value + ", fstValue: " + fstValue);
            if(value >= 0.1 && fstValue >= 0.1)
            {
                h++;
            }
            else if(value < 0.1 && fstValue < 0.1)
            {
                tn++;
            }
            else if(value >= 0.1 && fstValue < 0.1)
            {
                m++;
            }
            else if(value < 0.1 && fstValue >= 0.1)
            {
                f++;
            }
        }
//        ts = h / ((h + m + f) == 0 ? 1 : (h + m + f));
//        th = (h + tn) / ((h + f + m + tn) == 0 ? 1 : (h + f + m + tn));
//        tf = f / ((f + h) == 0 ? 1 : (f + h));
//        tm = m / ((m + h) == 0 ? 1 : (h + m));
//        System.out.println("h:" + h + ",tn:" + tn + ",m:" + m + ",f:" + f);
        result[0] = h;   // 
        result[1] = tn;   // 
        result[2] = f;   //空报
        result[3] = m;   //漏报 
        
        return result;
    }
    
    private double[] getStationCalHourValuePre12(int vti, Map<String, Map<String, ObsDataEntity>> obsDataMap, Map<String, Map<String, StationForecastDataEntity>> fstDataMap, List<StationForecastDataEntity> rainValueList)
    {
        double h = 0;
        double tn = 0;
        double f = 0;
        double m = 0;
        double th = 0;
        double tm = 0;
        double tf = 0;
        double ts = 0;
        ObsDataEntity obsValue = null;
        double[] result = new double[4];
        for(StationForecastDataEntity rainValue : rainValueList)
        {
            obsValue = obsDataMap.get(rainValue.getStation()).get(rainValue.getValiddate());
            if(obsValue == null || rainValue.getVti() == 0)
            {
                continue;
            }
            double value = obsValue.getRain24() == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE ? 0 : obsValue.getRain24();
            if(value == DecodeConstants.UNDEF_DOUBLE_VALUE)
            {
            	continue;
            }
            double fstValue;
            int preVti = rainValue.getVti() - (vti == 240 ? 12 : 24);
            StationForecastDataEntity dataEntity = fstDataMap.get(obsValue.getStation()).get(rainValue.getDatatime() + "_" + preVti);
            if(preVti == 0)
            {
            	dataEntity = new StationForecastDataEntity();
            	dataEntity.setRain24(0);
            }
            
            if(fstDataMap.get(obsValue.getStation()) == null || dataEntity == null)
            {
            	fstValue = 999999;
            }
            else
            {
                fstValue = rainValue.getRain24() - dataEntity.getRain24();
            }
            if(fstValue == 999999)
            {
                continue;
            }
//            System.out.println("obsDate: " + obsValue.getDatetime() + " fstDate: " + rainValue.getValiddate() + " fstTime: " + rainValue.getDatatime() + " vti: " + rainValue.getVti() + " value: " + value + ", fstValue: " + fstValue);
            if(value >= 0.1 && fstValue >= 0.1)
            {
                h++;
            }
            else if(value < 0.1 && fstValue < 0.1)
            {
                tn++;
            }
            else if(value >= 0.1 && fstValue < 0.1)
            {
                m++;
            }
            else if(value < 0.1 && fstValue >= 0.1)
            {
                f++;
            }
        }
        ts = h / ((h + m + f) == 0 ? 1 : (h + m + f));
        th = (h + tn) / ((h + f + m + tn) == 0 ? 1 : (h + f + m + tn));
        tf = f / ((f + h) == 0 ? 1 : (f + h));
        tm = m / ((m + tn) == 0 ? 1 : (tn + m));
//        System.out.println("h:" + h + ",tn:" + tn + ",m:" + m + ",f:" + f);
        result[0] = NumberFormatUtil.numFormat(ts, 3);   // TS评分
        result[1] = NumberFormatUtil.numFormat(th, 3);   // 准确率
        result[2] = NumberFormatUtil.numFormat(tf, 3);   // 空报
        result[3] = NumberFormatUtil.numFormat(tm, 3);   // 漏报
        if(h == 0 && tn == 0 && m == 0 && f == 0)
        {
        	result[0] = DecodeConstants.UNDEF_DOUBLE_VALUE;
        	result[1] = DecodeConstants.UNDEF_DOUBLE_VALUE;
        	result[2] = DecodeConstants.UNDEF_DOUBLE_VALUE;
        	result[3] = DecodeConstants.UNDEF_DOUBLE_VALUE;
        }
//        System.out.println("date: " + time + " rate: " + result[1]);
        
        return result;
    }
    
    private double[] getGribCalHourValue(Map<String, Map<String, ObsDataEntity>> obsDataMap, Map<String, Map<String, GribForecastRainValueEntity>> fstDataMap, List<GribForecastRainValueEntity> rainValueList)
    {
        double h = 0;
        double tn = 0;
        double f = 0;
        double m = 0;
        double th = 0;
        double tm = 0;
        double tf = 0;
        double ts = 0;
        ObsDataEntity obsValue = null;
        double[] result = new double[4];
        for(GribForecastRainValueEntity rainValue : rainValueList)
        {
            obsValue = obsDataMap.get(rainValue.getStation()).get(rainValue.getValidDate());
            if(obsValue == null || rainValue.getVti() == 0)
            {
                continue;
            }
            double value = obsValue.getRain24() == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE ? 0 : obsValue.getRain24();
            if(value == DecodeConstants.UNDEF_DOUBLE_VALUE)
            {
            	continue;
            }
            int preVti = rainValue.getVti() - 24;
//            preVti = (preVti == 0 ? 24 : preVti);
            if(fstDataMap.get(rainValue.getStation()) == null || fstDataMap.get(rainValue.getStation()).get(rainValue.getDataTime() + "_" + preVti) == null)
            {
                continue;
            }
            double fstValue = rainValue.getRain() - fstDataMap.get(rainValue.getStation()).get(rainValue.getDataTime() + "_" + preVti).getRain();
//            System.out.println("obsDate: " + obsValue.getDatetime() + " fstDate: " + rainValue.getValidDate() + " fstDataTime: " + rainValue.getDataTime() + " vti: " + rainValue.getVti() + " value: " + value + ", fstValue: " + fstValue);
            if(fstValue >= 9999)
            {
                continue;
            }
            if(value >= 0.1 && fstValue >= 0.1)
            {
                h++;
            }
            else if(value < 0.1 && fstValue < 0.1)
            {
                tn++;
            }
            else if(value >= 0.1 && fstValue < 0.1)
            {
                m++;
            }
            else if(value < 0.1 && fstValue >= 0.1)
            {
                f++;
            }
        }
//        System.out.println("h:" + h + ",tn:" + tn + ",m:" + m + ",f:" + f);
        ts = h / ((h + m + f) == 0 ? 1 : (h + m + f));
        th = (h + tn) / ((h + f + m + tn) == 0 ? 1 : (h + f + m + tn));
        tf = f / ((f + h) == 0 ? 1 : (f + h));
        tm = m / ((m + tn) == 0 ? 1 : (tn + m));
        result[0] = NumberFormatUtil.numFormat(ts, 3);   // TS评分
        result[1] = NumberFormatUtil.numFormat(th, 3);   // 准确率
        result[2] = NumberFormatUtil.numFormat(tf, 3);   // 空报
        result[3] = NumberFormatUtil.numFormat(tm, 3);   // 漏报
        if(h == 0 && tn == 0 && m == 0 && f == 0)
        {
        	result[0] = DecodeConstants.UNDEF_DOUBLE_VALUE;
        	result[1] = DecodeConstants.UNDEF_DOUBLE_VALUE;
        	result[2] = DecodeConstants.UNDEF_DOUBLE_VALUE;
        	result[3] = DecodeConstants.UNDEF_DOUBLE_VALUE;
        }

        return result;
    }
    
    private double[] getGribCalHourValueNum(Map<String, Map<String, ObsDataEntity>> obsDataMap, Map<String, Map<String, GribForecastRainValueEntity>> fstDataMap, List<GribForecastRainValueEntity> rainValueList)
    {
        double h = 0;
        double tn = 0;
        double f = 0;
        double m = 0;
        ObsDataEntity obsValue = null;
        double[] result = new double[4];
        for(GribForecastRainValueEntity rainValue : rainValueList)
        {
            obsValue = obsDataMap.get(rainValue.getStation()).get(rainValue.getValidDate());
            if(obsValue == null || rainValue.getVti() == 0)
            {
                continue;
            }
            double value = obsValue.getRain24() == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE ? 0 : obsValue.getRain24();
            if(value == DecodeConstants.UNDEF_DOUBLE_VALUE)
            {
            	continue;
            }
            int preVti = rainValue.getVti() - 24;
//            preVti = (preVti == 0 ? 24 : preVti);
            if(fstDataMap.get(rainValue.getStation()) == null)
            {
                continue;
            }
            if(preVti != 0 && fstDataMap.get(rainValue.getStation()).get(rainValue.getDataTime() + "_" + preVti) == null)
            {
            	continue;
            }
            double fstValue = 0;
            if(preVti == 0)
            {
            	fstValue = rainValue.getRain();
            }
            else
            {
            	fstValue = rainValue.getRain() - fstDataMap.get(rainValue.getStation()).get(rainValue.getDataTime() + "_" + preVti).getRain();
            }
//            System.out.println("obsDate: " + obsValue.getDatetime() + " fstDate: " + rainValue.getValidDate() + " fstDataTime: " + rainValue.getDataTime() + " vti: " + rainValue.getVti() + " value: " + value + ", fstValue: " + fstValue);
            if(fstValue >= 9999)
            {
                continue;
            }
//            if("56385".equals(rainValue.getStation()))
//            {
//            	System.out.println("lkdjlsdfsfas ddsf");
//            }
//            System.out.println(rainValue.getStation() + " = " + value + " : " + fstValue);
            if(value >= 0.1 && fstValue >= 0.1)
            {
                h++;
            }
            else if(value < 0.1 && fstValue < 0.1)
            {
                tn++;
            }
            else if(value >= 0.1 && fstValue < 0.1)
            {
                m++;
            }
            else if(value < 0.1 && fstValue >= 0.1)
            {
                f++;
            }
        }
//        System.out.println("h:" + h + ",tn:" + tn + ",m:" + m + ",f:" + f);
        result[0] = h;   // 
        result[1] = tn;   // 
        result[2] = f;   //空报
        result[3] = m;   //漏报  

        return result;
    }
    
    private double[] getGribCalHourValuePre12(Map<String, Map<String, ObsDataEntity>> obsDataMap, Map<String, Map<String, GribForecastRainValueEntity>> fstDataMap, List<GribForecastRainValueEntity> rainValueList)
    {
    	double h = 0;
    	double tn = 0;
    	double f = 0;
    	double m = 0;
    	double th = 0;
    	double tm = 0;
    	double tf = 0;
    	double ts = 0;
    	ObsDataEntity obsValue = null;
    	double[] result = new double[4];
    	for(GribForecastRainValueEntity rainValue : rainValueList)
    	{
    		obsValue = obsDataMap.get(rainValue.getStation()).get(rainValue.getValidDate());
    		if(obsValue == null || rainValue.getVti() == 0)
    		{
    			continue;
    		}
    		double value = obsValue.getRain24() == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE ? 0 : obsValue.getRain24();
    		if(value == DecodeConstants.UNDEF_DOUBLE_VALUE)
            {
            	continue;
            }
    		int preVti = rainValue.getVti() - 12;
			//            preVti = (preVti == 0 ? 24 : preVti);
			if(fstDataMap.get(rainValue.getStation()) == null || fstDataMap.get(rainValue.getStation()).get(rainValue.getDataTime() + "_" + preVti) == null)
			{
				continue;
			}
			double fstValue = rainValue.getRain() - fstDataMap.get(rainValue.getStation()).get(rainValue.getDataTime() + "_" + preVti).getRain();
//			System.out.println("obsDate: " + obsValue.getDatetime() + " fstDate: " + rainValue.getValidDate() + " fstDataTime: " + rainValue.getDataTime() + " vti: " + rainValue.getVti() + " value: " + value + ", fstValue: " + fstValue);
			if(fstValue >= 9999)
			{
				continue;
			}
			if(value >= 0.1 && fstValue >= 0.1)
			{
				h++;
			}
			else if(value < 0.1 && fstValue < 0.1)
			{
				tn++;
			}
			else if(value >= 0.1 && fstValue < 0.1)
			{
				m++;
			}
			else if(value < 0.1 && fstValue >= 0.1)
			{
				f++;
			}
    	}
//        System.out.println("h:" + h + ",tn:" + tn + ",m:" + m + ",f:" + f);
    	ts = h / ((h + m + f) == 0 ? 1 : (h + m + f));
    	th = (h + tn) / ((h + f + m + tn) == 0 ? 1 : (h + f + m + tn));
    	tf = f / ((f + h) == 0 ? 1 : (f + h));
    	tm = m / ((m + tn) == 0 ? 1 : (tn + m));
    	result[0] = NumberFormatUtil.numFormat(ts, 3);   // TS评分
    	result[1] = NumberFormatUtil.numFormat(th, 3);   // 准确率
    	result[2] = NumberFormatUtil.numFormat(tf, 3);   // 空报
    	result[3] = NumberFormatUtil.numFormat(tm, 3);   // 漏报
    	if(h == 0 && tn == 0 && m == 0 && f == 0)
    	{
    		result[0] = DecodeConstants.UNDEF_DOUBLE_VALUE;
    		result[1] = DecodeConstants.UNDEF_DOUBLE_VALUE;
    		result[2] = DecodeConstants.UNDEF_DOUBLE_VALUE;
    		result[3] = DecodeConstants.UNDEF_DOUBLE_VALUE;
    	}
    	
    	return result;
    }
    
    private double[] getGribCalHourValuePre12Num(Map<String, Map<String, ObsDataEntity>> obsDataMap, Map<String, Map<String, GribForecastRainValueEntity>> fstDataMap, List<GribForecastRainValueEntity> rainValueList)
    {
    	double h = 0;
    	double tn = 0;
    	double f = 0;
    	double m = 0;
    	ObsDataEntity obsValue = null;
    	double[] result = new double[4];
    	for(GribForecastRainValueEntity rainValue : rainValueList)
    	{
    		obsValue = obsDataMap.get(rainValue.getStation()).get(rainValue.getValidDate());
    		if(obsValue == null || rainValue.getVti() == 0)
    		{
    			continue;
    		}
    		double value = obsValue.getRain24() == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE ? 0 : obsValue.getRain24();
    		if(value == DecodeConstants.UNDEF_DOUBLE_VALUE)
            {
            	continue;
            }
    		int preVti = rainValue.getVti() - 12;
			//            preVti = (preVti == 0 ? 24 : preVti);
			if(fstDataMap.get(rainValue.getStation()) == null || fstDataMap.get(rainValue.getStation()).get(rainValue.getDataTime() + "_" + preVti) == null)
			{
				continue;
			}
			double fstValue = rainValue.getRain() - fstDataMap.get(rainValue.getStation()).get(rainValue.getDataTime() + "_" + preVti).getRain();
//			System.out.println("obsDate: " + obsValue.getDatetime() + " fstDate: " + rainValue.getValidDate() + " fstDataTime: " + rainValue.getDataTime() + " vti: " + rainValue.getVti() + " value: " + value + ", fstValue: " + fstValue);
			if(fstValue >= 9999)
			{
				continue;
			}
			if(value >= 0.1 && fstValue >= 0.1)
			{
				h++;
			}
			else if(value < 0.1 && fstValue < 0.1)
			{
				tn++;
			}
			else if(value >= 0.1 && fstValue < 0.1)
			{
				m++;
			}
			else if(value < 0.1 && fstValue >= 0.1)
			{
				f++;
			}
    	}
//        System.out.println("h:" + h + ",tn:" + tn + ",m:" + m + ",f:" + f);
        result[0] = h;   // 
        result[1] = tn;   // 
        result[2] = f;   //空报
        result[3] = m;   //漏报  
    	
    	return result;
    }
    
    
    private double[] getGribCalHourValuePre12(int vti, Map<String, Map<String, ObsDataEntity>> obsDataMap, Map<String, Map<String, GribForecastRainValueEntity>> fstDataMap, List<GribForecastRainValueEntity> rainValueList)
    {
        double h = 0;
        double tn = 0;
        double f = 0;
        double m = 0;
        double th = 0;
        double tm = 0;
        double tf = 0;
        double ts = 0;
        ObsDataEntity obsValue = null;
        double[] result = new double[4];
        for(GribForecastRainValueEntity rainValue : rainValueList)
        {
        	double fstValue = 0;
        	double value = 0;
        	if(vti == 228)
        	{
        		fstValue = rainValue.getRain() - fstDataMap.get(rainValue.getStation()).get(rainValue.getDataTime() + "_" + 228).getRain();
        		Map<String, ObsDataEntity> map = obsDataMap.get(rainValue.getStation());
        		String startDate = TimeUtil.addHours(rainValue.getValidDate(), -12);
        		double sumRain = 0;
        		for(String date : map.keySet())
        		{
        			if(date.compareTo(startDate) >= 0 && date.compareTo(rainValue.getValidDate()) <= 0)
        			{
        				sumRain += map.get(date).getRain();
        			}
        		}
        		value = sumRain;
        	}
        	else
        	{
        		obsValue = obsDataMap.get(rainValue.getStation()).get(rainValue.getValidDate());
        		if(obsValue == null || rainValue.getVti() == 0)
        		{
        			continue;
        		}
        		value = obsValue.getRain24() == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE ? 0 : obsValue.getRain24();
        		if(value == DecodeConstants.UNDEF_DOUBLE_VALUE)
                {
                	continue;
                }
        		int preVti = rainValue.getVti() - 24;
//            preVti = (preVti == 0 ? 24 : preVti);
        		if(fstDataMap.get(rainValue.getStation()) == null || fstDataMap.get(rainValue.getStation()).get(rainValue.getDataTime() + "_" + preVti) == null)
        		{
        			continue;
        		}
        		fstValue = rainValue.getRain() - fstDataMap.get(rainValue.getStation()).get(rainValue.getDataTime() + "_" + preVti).getRain();
//        		System.out.println("obsDate: " + obsValue.getDatetime() + " fstDate: " + rainValue.getValidDate() + " fstDataTime: " + rainValue.getDataTime() + " vti: " + rainValue.getVti() + " value: " + value + ", fstValue: " + fstValue);
        	}
            if(fstValue >= 9999)
            {
                continue;
            }
            if(value >= 0.1 && fstValue >= 0.1)
            {
                h++;
            }
            else if(value < 0.1 && fstValue < 0.1)
            {
                tn++;
            }
            else if(value >= 0.1 && fstValue < 0.1)
            {
                m++;
            }
            else if(value < 0.1 && fstValue >= 0.1)
            {
                f++;
            }
        }
//        System.out.println("h:" + h + ",tn:" + tn + ",m:" + m + ",f:" + f);
        ts = h / ((h + m + f) == 0 ? 1 : (h + m + f));
        th = (h + tn) / ((h + f + m + tn) == 0 ? 1 : (h + f + m + tn));
        tf = f / ((f + h) == 0 ? 1 : (f + h));
        tm = m / ((m + tn) == 0 ? 1 : (tn + m));
        result[0] = NumberFormatUtil.numFormat(ts, 3);   // TS评分
        result[1] = NumberFormatUtil.numFormat(th, 3);   // 准确率
        result[2] = NumberFormatUtil.numFormat(tf, 3);   // 空报
        result[3] = NumberFormatUtil.numFormat(tm, 3);   // 漏报
        if(h == 0 && tn == 0 && m == 0 && f == 0)
        {
        	result[0] = DecodeConstants.UNDEF_DOUBLE_VALUE;
        	result[1] = DecodeConstants.UNDEF_DOUBLE_VALUE;
        	result[2] = DecodeConstants.UNDEF_DOUBLE_VALUE;
        	result[3] = DecodeConstants.UNDEF_DOUBLE_VALUE;
        }

        return result;
    }

    private double getValue(String element, String vti, String method, List<ObsDataEntity> obsDataList, List<StationForecastDataEntity> fstDataList)
    {
        Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
        List<ObsDataEntity> obsDataListSub = new ArrayList<>();
        Map<String, List<Object>> obsMapList = new HashMap<>();
        for(StationForecastDataEntity fstData : fstDataList)
        {
            for(ObsDataEntity obsData : obsDataList)
            {
                if(fstData.getValiddate().compareTo(obsData.getDatetime()) == 0)
                {
                    obsDataListSub.add(obsData);
                    ArrayList<Object> objects = new ArrayList<>();
                    objects.add(obsData);
                    objects.add(fstData);
                    obsMapList.put(fstData.getValiddate(), objects);
                }
            }
        }
        int count = obsDataList.size() == 0 ? 1 : obsDataList.size();
        double sum = 0;
        if("RMSE".equals(method))
        {
            for(String key : obsMapList.keySet())
            {
                List<Object> objects = obsMapList.get(key);
                double obs = getDoubleValueFromObject(objects.get(0), element);
                double fst = getDoubleValueFromObject(objects.get(1), element);
                sum += Math.pow(obs - fst, 2);
            }
//            for(int i = 0; i < count; i++)
//            {
//                double obs = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(obsDataList.get(i), element)));
//                double fst = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(fstDataList.get(i), element)));
//                sum += Math.pow(obs - fst, 2);
//            }
            sum = Math.sqrt(sum / count);
        }
        else if("MAE".equals(method))
        {
            for(String key : obsMapList.keySet())
            {
                List<Object> objects = obsMapList.get(key);
                double obs = getDoubleValueFromObject(objects.get(0), element);
                double fst = getDoubleValueFromObject(objects.get(1), element);
                sum += Math.abs(obs - fst);
            }
//            for(int i = 0; i < count; i++)
//            {
//                double obs = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(obsDataList.get(i), element)));
//                double fst = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(fstDataList.get(i), element)));
//                sum += Math.abs(obs - fst);
//            }
            sum = sum / count;
        }
        else if("R".equals(method))
        {
            double obsSum = 0;
            double fstSum = 0;
            for(String key : obsMapList.keySet())
            {
                List<Object> objects = obsMapList.get(key);
                double obs = getDoubleValueFromObject(objects.get(0), element);
                double fst = getDoubleValueFromObject(objects.get(1), element);
                obsSum += obs;
                fstSum += fst;
            }
//            for(int i = 0; i < count; i++)
//            {
//                double obs = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(obsDataList.get(i), element)));
//                double fst = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(fstDataList.get(i), element)));
//                obsSum += obs;
//                fstSum += fst;
//            }
            double obsAvg = obsSum / count;
            double fstAvg = fstSum / count;

            double upSum = 0;
            double downSum = 0;
            for(String key : obsMapList.keySet())
            {
                List<Object> objects = obsMapList.get(key);
                double obs = getDoubleValueFromObject(objects.get(0), element);
                double fst = getDoubleValueFromObject(objects.get(1), element);
                upSum += (obs - obsAvg) * (fst - fstAvg);
                downSum += Math.sqrt(Math.pow(obs - obsAvg, 2)) * Math.sqrt(Math.pow(fst - fstAvg, 2));
            }
//            for(int i = 0; i < count; i++)
//            {
//                double obs = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(obsDataList.get(i), element)));
//                double fst = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(fstDataList.get(i), element)));
//                upSum += (obs - obsAvg) * (fst - fstAvg);
//                downSum += Math.sqrt(Math.pow(obs - obsAvg, 2)) * Math.sqrt(Math.pow(fst - fstAvg, 2));
//            }
            sum = upSum / downSum;
        }
        else if("准确率".equals(method))
        {
            double dis = Double.parseDouble(configMap.get(element));
            int correct = 0;
            for(String key : obsMapList.keySet())
            {
                List<Object> objects = obsMapList.get(key);
                double obs = getDoubleValueFromObject(objects.get(0), element);
                double fst = getDoubleValueFromObject(objects.get(1), element);
                if(Math.abs(obs - fst) < dis)
                {
                    correct++;
                }
            }
//            for(int i = 0; i < count; i++)
//            {
//                double obs = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(obsDataList.get(i), element)));
//                double fst = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(fstDataList.get(i), element)));
//                if(Math.abs(obs - fst) < dis)
//                {
//                    correct++;
//                }
//            }
            sum = correct / count;
        }
        sum = NumberFormatUtil.numFormat(sum, 3);

        return sum;
    }
    
    private double getStationValue(String element, String method, Map<String, Map<String, ObsDataEntity>> obsDataMap, List<StationForecastDataEntity> fstDataList)
    {
    	if(fstDataList == null)
    	{
    		return DecodeConstants.UNDEF_DOUBLE_VALUE;
    	}
        Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
        Map<String, List<Object>> obsMapList = new HashMap<>();
        Map<String, Map<String, List<Object>>> dataStationDateMapList = new HashMap<>();
        
        if(element.equals("atmax") || element.equals("atmin"))
        {
        	for(StationForecastDataEntity fstData : fstDataList)
            {
        		String validDate = fstData.getValiddate().split(" ")[0];
                ObsDataEntity obsData = obsDataMap.get(fstData.getStation()).get(fstData.getValiddate());
                if(!dataStationDateMapList.containsKey(fstData.getStation()))
                {
                	dataStationDateMapList.put(fstData.getStation(), new HashMap<>());
                }
                if(!dataStationDateMapList.get(fstData.getStation()).containsKey(validDate))
                {
                	dataStationDateMapList.get(fstData.getStation()).put(validDate, new ArrayList<>());
                }
                dataStationDateMapList.get(fstData.getStation()).get(validDate).add(obsData);
                dataStationDateMapList.get(fstData.getStation()).get(validDate).add(fstData);
                
            }
        	
        	for(String station : dataStationDateMapList.keySet())
        	{
        		List<Object> listObjs = new ArrayList<>();
        		for(String date : dataStationDateMapList.get(station).keySet())
        		{
        			List<Object> list = dataStationDateMapList.get(station).get(date);
            		double atMaxObs = -9999;
            		double atMinObs = 9999;
            		double atMaxFst = -9999;
            		double atMinFst = 9999;
            		String obsDate = null;
            		String fstDate = null;
            		String fstHour = null;
            		String vtiStr = null;
            		for(int i = 0, num = list.size(); i < num - 1; i = i + 2)
                    {
            			if(list.get(i) == null || list.get(i + 1) == null)
                    	{
                        	continue;
                    	}
            			double obs = getDoubleValueFromObject(list.get(i), "at");
                    	double fst = getDoubleValueFromObject(list.get(i + 1), "at");
                    	if(atMaxObs < obs)
                    	{
                    		atMaxObs = obs;
                    	}
                    	if(atMinObs > obs)
                    	{
                    		atMinObs = obs;
                    	}
                    	if(atMaxFst < fst)
                    	{
                    		atMaxFst = fst;
                    	}
                    	if(atMinFst > fst)
                    	{
                    		atMinFst = fst;
                    	}
                    	obsDate = ReflectUtil.getFieldValueByName(list.get(i), "datetime").toString();
                    	fstDate = ReflectUtil.getFieldValueByName(list.get(i + 1), "validdate").toString();
                    	fstHour = ReflectUtil.getFieldValueByName(list.get(i + 1), "datatime").toString();
                    	vtiStr = ReflectUtil.getFieldValueByName(list.get(i + 1), "vti").toString();
                    }
            		ObsDataEntity obsData = new ObsDataEntity();
            		obsData.setAtmax(atMaxObs);
            		obsData.setAtmin(atMinObs);
            		obsData.setDatetime(obsDate);
            		StationForecastDataEntity fstData = new StationForecastDataEntity();
            		fstData.setAtmax(atMaxFst);
            		fstData.setAtmin(atMinFst);
            		fstData.setValiddate(fstDate);
            		fstData.setDatatime(fstHour);
            		if(vtiStr != null)
                	{
                		fstData.setVti(Integer.parseInt(vtiStr));
                	}
            		listObjs.add(obsData);
            		listObjs.add(fstData);
        		}
        		obsMapList.put(station, listObjs);
        		
        	}
        	
        }
        else
        {
        	for(StationForecastDataEntity fstData : fstDataList)
            {
        		if(fstData.getVti() > 228)
        		{
        			continue;
        		}
                ObsDataEntity obsData = obsDataMap.get(fstData.getStation()).get(fstData.getValiddate());
                if(!obsMapList.containsKey(fstData.getStation()))
                {
                	obsMapList.put(fstData.getStation(), new ArrayList<>());
                }
                obsMapList.get(fstData.getStation()).add(obsData);
                obsMapList.get(fstData.getStation()).add(fstData);
            }
        }
        
        int count = 0;
        for(String key : obsMapList.keySet())
        {
        	count += obsMapList.get(key).size() / 2;
        }
        double sum = 0;
        if("RMSE".equals(method))
        {
            int invalidCount = 0;
            for(String key : obsMapList.keySet())
            {
                List<Object> objects = obsMapList.get(key);
                if(objects == null || objects.size() == 0)
                {
                    invalidCount++;
                    continue;
                }
                for(int i = 0, num = objects.size(); i < num - 1; i = i + 2)
                {
                	if(objects.get(i) == null || objects.get(i + 1) == null)
                	{
                		invalidCount++;
                    	continue;
                	}
                	double obs = getDoubleValueFromObject(objects.get(i), element);
                	double fst = getDoubleValueFromObject(objects.get(i + 1), element);
                	if(obs == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || obs == DecodeConstants.UNDEF_DOUBLE_VALUE || Double.isNaN(obs)
                      	  || fst >= DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || Double.isNaN(fst))
                    {
                		invalidCount++;
                		continue;
                	}
                	sum += Math.pow(obs - fst, 2);
                }
            }
            
            if(count == invalidCount)
            {
                sum = DecodeConstants.UNDEF_DOUBLE_VALUE;
            }
            else
            {
                sum = Math.sqrt(sum / (count - invalidCount));
            }
        }
        else if("MAE".equals(method))
        {
            int invalidCount = 0;
            
            for(String key : obsMapList.keySet())
            {
                List<Object> objects = obsMapList.get(key);
                if(objects == null || objects.size() == 0)
                {
                	invalidCount++;
                	continue;
                }
                for(int i = 0, num = objects.size(); i < num - 1; i = i + 2)
                {
                	if(objects.get(i) == null || objects.get(i + 1) == null)
                	{
                		invalidCount++;
                    	continue;
                	}
                	double obs = getDoubleValueFromObject(objects.get(i), element);
                	double fst = getDoubleValueFromObject(objects.get(i + 1), element);
//                	System.out.println("vti: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "vti"));
                	if(obs == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || obs == DecodeConstants.UNDEF_DOUBLE_VALUE || Double.isNaN(obs)
                	  || fst >= DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || Double.isNaN(fst))
                	{
                		invalidCount++;
                		continue;
                	}
//                	System.out.println("obsDate: " + ReflectUtil.getFieldValueByName(objects.get(i), "datetime") + " fstDate: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "validdate") + " vti: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "vti") + " fsttime: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "datatime") + " obs: " + obs + " fst: " + fst + " MAE: " + Math.abs(obs - fst));
//                	System.out.println("obs: " + obs + " fst: " + fst + " MAE: " + Math.abs(obs - fst));
                	sum += Math.abs(obs - fst);
                }
            }
//            System.out.println("invalidCount: " + invalidCount);
            if(count == invalidCount)
            {
                sum = DecodeConstants.UNDEF_DOUBLE_VALUE;
            }
            else
            {
                sum = sum / (count - invalidCount);
            }
        }
        else if("R".equals(method))
        {
            double obsSum = 0;
            double fstSum = 0;
            int invalidCount = 0;
            for(String key : obsMapList.keySet())
            {
                List<Object> objects = obsMapList.get(key);
                if(objects == null || objects.size() == 0)
                {
                    invalidCount++;
                    continue;
                }
                for(int i = 0, num = objects.size(); i < num - 1; i = i + 2)
                {
                	if(objects.get(i) == null || objects.get(i + 1) == null)
                	{
                		invalidCount++;
                    	continue;
                	}
                	double obs = getDoubleValueFromObject(objects.get(i), element);
                	double fst = getDoubleValueFromObject(objects.get(i + 1), element);
                	if(obs == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || obs == DecodeConstants.UNDEF_DOUBLE_VALUE || Double.isNaN(obs)
                      	  || fst >= DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || Double.isNaN(fst))
                    {
                		invalidCount++;
                		continue;
                	}
                	obsSum += obs;
                	fstSum += fst;
                }
            }
            double obsAvg = obsSum / (count - invalidCount);
            double fstAvg = fstSum / (count - invalidCount);

            double upSum = 0;
            double downSum = 0;
            for(String key : obsMapList.keySet())
            {
                List<Object> objects = obsMapList.get(key);
                if(objects == null || objects.size() == 0)
                {
                    continue;
                }
                for(int i = 0, num = objects.size(); i < num - 1; i = i + 2)
                {
                	if(objects.get(i) == null || objects.get(i + 1) == null)
                	{
                		invalidCount++;
                    	continue;
                	}
                	double obs = getDoubleValueFromObject(objects.get(i), element);
                	double fst = getDoubleValueFromObject(objects.get(i + 1), element);
                	if(obs == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || obs == DecodeConstants.UNDEF_DOUBLE_VALUE || Double.isNaN(obs)
                      	  || fst >= DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || Double.isNaN(fst))
                    {
                		continue;
                	}
                	upSum += (obs - obsAvg) * (fst - fstAvg);
                	downSum += Math.sqrt(Math.pow(obs - obsAvg, 2)) * Math.sqrt(Math.pow(fst - fstAvg, 2));
                }
            }
            if(downSum == 0)
            {
                sum = DecodeConstants.UNDEF_DOUBLE_VALUE;
            }
            else
            {
                sum = upSum / downSum;
            }
        }
        else if("准确率".equals(method))
        {
            double dis = Double.parseDouble(configMap.get(element));
            double correct = 0;
            int invalidCount = 0;
            for(String key : obsMapList.keySet())
            {
                List<Object> objects = obsMapList.get(key);
                if(objects == null || objects.size() == 0)
                {
                    invalidCount++;
                    continue;
                }
                for(int i = 0, num = objects.size(); i < num - 1; i = i + 2)
                {
                	if(objects.get(i) == null || objects.get(i + 1) == null)
                	{
                		invalidCount++;
                    	continue;
                	}
                	double obs = getDoubleValueFromObject(objects.get(i), element);
                	double fst = getDoubleValueFromObject(objects.get(i + 1), element);
                	if(obs == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || obs == DecodeConstants.UNDEF_DOUBLE_VALUE || Double.isNaN(obs)
                      	  || fst >= DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || Double.isNaN(fst))
                    {
                		invalidCount++;
                		continue;
                	}
//                	System.out.println("obsDate: " + ReflectUtil.getFieldValueByName(objects.get(i), "datetime") + " fstDate: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "validdate") + " vti: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "vti") + " obs: " + obs + " fst: " + fst + " dis: " + Math.abs(obs - fst));
//                	System.out.println("obs: " + obs + " fst: " + fst + " dis: " + Math.abs(obs - fst));
                	if(Math.abs(obs - fst) <= dis)
                	{
                		correct++;
                	}
                }
            }
            if(count == invalidCount)
            {
                sum = DecodeConstants.UNDEF_DOUBLE_VALUE;
            }
            else
            {
                sum = correct / (count - invalidCount);
            }
        }

        sum = NumberFormatUtil.numFormat(sum, 3);

        return sum;
    }
    

    private double getStationValuePre12(String element, String method, Map<String, Map<String, ObsDataEntity>> obsDataMap, List<StationForecastDataEntity> fstDataList)
    {
    	if(fstDataList == null)
    	{
    		return DecodeConstants.UNDEF_DOUBLE_VALUE;
    	}
        Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
        Map<String, List<Object>> obsMapList = new HashMap<>();
        Map<String, Map<String, List<Object>>> dataStationDateMapList = new HashMap<>();
        
        if(element.equals("atmax") || element.equals("atmin"))
        {
        	for(StationForecastDataEntity fstData : fstDataList)
            {
        		String validDate = fstData.getValiddate().split(" ")[0];
                ObsDataEntity obsData = obsDataMap.get(fstData.getStation()).get(fstData.getValiddate());
                if(!dataStationDateMapList.containsKey(fstData.getStation()))
                {
                	dataStationDateMapList.put(fstData.getStation(), new HashMap<>());
                }
                if(!dataStationDateMapList.get(fstData.getStation()).containsKey(validDate))
                {
                	dataStationDateMapList.get(fstData.getStation()).put(validDate, new ArrayList<>());
                }
                dataStationDateMapList.get(fstData.getStation()).get(validDate).add(obsData);
                dataStationDateMapList.get(fstData.getStation()).get(validDate).add(fstData);
                
            }
        	
        	for(String station : dataStationDateMapList.keySet())
        	{
        		List<Object> listObjs = new ArrayList<>();
        		for(String date : dataStationDateMapList.get(station).keySet())
        		{
        			List<Object> list = dataStationDateMapList.get(station).get(date);
            		double atMaxObs = -9999;
            		double atMinObs = 9999;
            		double atMaxFst = -9999;
            		double atMinFst = 9999;
            		String obsDate = null;
            		String fstDate = null;
            		String fstHour = null;
            		String vtiStr = null;
            		for(int i = 0, num = list.size(); i < num - 1; i = i + 2)
                    {
            			if(list.get(i) == null || list.get(i + 1) == null)
                    	{
                        	continue;
                    	}
            			double obs = getDoubleValueFromObject(list.get(i), "at");
                    	double fst = getDoubleValueFromObject(list.get(i + 1), "at");
                    	if(atMaxObs < obs)
                    	{
                    		atMaxObs = obs;
                    	}
                    	if(atMinObs > obs)
                    	{
                    		atMinObs = obs;
                    	}
                    	if(atMaxFst < fst)
                    	{
                    		atMaxFst = fst;
                    	}
                    	if(atMinFst > fst)
                    	{
                    		atMinFst = fst;
                    	}
                    	obsDate = ReflectUtil.getFieldValueByName(list.get(i), "datetime").toString();
                    	fstDate = ReflectUtil.getFieldValueByName(list.get(i + 1), "validdate").toString();
                    	fstHour = ReflectUtil.getFieldValueByName(list.get(i + 1), "datatime").toString();
                    	vtiStr = ReflectUtil.getFieldValueByName(list.get(i + 1), "vti").toString();
                    }
            		ObsDataEntity obsData = new ObsDataEntity();
            		obsData.setAtmax(atMaxObs);
            		obsData.setAtmin(atMinObs);
            		obsData.setDatetime(obsDate);
            		StationForecastDataEntity fstData = new StationForecastDataEntity();
            		fstData.setAtmax(atMaxFst);
            		fstData.setAtmin(atMinFst);
            		fstData.setValiddate(fstDate);
            		fstData.setDatatime(fstHour);
            		if(vtiStr != null)
                	{
                		fstData.setVti(Integer.parseInt(vtiStr));
                	}
            		listObjs.add(obsData);
            		listObjs.add(fstData);
        		}
        		obsMapList.put(station, listObjs);
        		
        	}
        	
        }
        else
        {
        	for(StationForecastDataEntity fstData : fstDataList)
            {
                ObsDataEntity obsData = obsDataMap.get(fstData.getStation()).get(fstData.getValiddate());
                if(!obsMapList.containsKey(fstData.getStation()))
                {
                	obsMapList.put(fstData.getStation(), new ArrayList<>());
                }
                obsMapList.get(fstData.getStation()).add(obsData);
                obsMapList.get(fstData.getStation()).add(fstData);
            }
        	
        }
        
        int count = 0;
        for(String key : obsMapList.keySet())
        {
        	count += obsMapList.get(key).size() / 2;
        }
        double sum = 0;
        if("RMSE".equals(method))
        {
            int invalidCount = 0;
            for(String key : obsMapList.keySet())
            {
                List<Object> objects = obsMapList.get(key);
                if(objects == null || objects.size() == 0)
                {
                    invalidCount++;
                    continue;
                }
                for(int i = 0, num = objects.size(); i < num - 1; i = i + 2)
                {
                	if(objects.get(i) == null || objects.get(i + 1) == null)
                	{
                		invalidCount++;
                    	continue;
                	}
                	double obs = getDoubleValueFromObject(objects.get(i), element);
                	double fst = getDoubleValueFromObject(objects.get(i + 1), element);
                	if(obs == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || obs == DecodeConstants.UNDEF_DOUBLE_VALUE || fst >= DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE)
                	{
                		invalidCount++;
                		continue;
                	}
                	sum += Math.pow(obs - fst, 2);
                }
            }
            
            if(count == invalidCount)
            {
                sum = DecodeConstants.UNDEF_DOUBLE_VALUE;
            }
            else
            {
                sum = Math.sqrt(sum / (count - invalidCount));
            }
        }
        else if("MAE".equals(method))
        {
            int invalidCount = 0;
            for(String key : obsMapList.keySet())
            {
                List<Object> objects = obsMapList.get(key);
                if(objects == null || objects.size() == 0)
                {
                	invalidCount++;
                	continue;
                }
                for(int i = 0, num = objects.size(); i < num - 1; i = i + 2)
                {
                	if(objects.get(i) == null || objects.get(i + 1) == null)
                	{
                		invalidCount++;
                    	continue;
                	}
                	double obs = getDoubleValueFromObject(objects.get(i), element);
                	double fst = getDoubleValueFromObject(objects.get(i + 1), element);
//                	System.out.println("vti: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "vti"));
                	if(obs == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || obs == DecodeConstants.UNDEF_DOUBLE_VALUE || fst >= DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE)
                	{
                		invalidCount++;
                		continue;
                	}
//                	System.out.println("obsDate: " + ReflectUtil.getFieldValueByName(objects.get(i), "datetime") + " fstDate: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "validdate") + " vti: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "vti") + " fsttime: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "datatime") + " obs: " + obs + " fst: " + fst + " MAE: " + Math.abs(obs - fst));
//                	System.out.println("obs: " + obs + " fst: " + fst + " MAE: " + Math.abs(obs - fst));
                	sum += Math.abs(obs - fst);
                }
            }
//            System.out.println("invalidCount: " + invalidCount);
            if(count == invalidCount)
            {
                sum = DecodeConstants.UNDEF_DOUBLE_VALUE;
            }
            else
            {
                sum = sum / (count - invalidCount);
            }
        }
        else if("R".equals(method))
        {
            double obsSum = 0;
            double fstSum = 0;
            int invalidCount = 0;
            for(String key : obsMapList.keySet())
            {
                List<Object> objects = obsMapList.get(key);
                if(objects == null || objects.size() == 0)
                {
                    invalidCount++;
                    continue;
                }
                for(int i = 0, num = objects.size(); i < num - 1; i = i + 2)
                {
                	if(objects.get(i) == null || objects.get(i + 1) == null)
                	{
                		invalidCount++;
                    	continue;
                	}
                	double obs = getDoubleValueFromObject(objects.get(i), element);
                	double fst = getDoubleValueFromObject(objects.get(i + 1), element);
                	if(obs == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || obs == DecodeConstants.UNDEF_DOUBLE_VALUE || fst >= DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE)
                	{
                		invalidCount++;
                		continue;
                	}
                	obsSum += obs;
                	fstSum += fst;
                }
            }
            double obsAvg = obsSum / (count - invalidCount);
            double fstAvg = fstSum / (count - invalidCount);

            double upSum = 0;
            double downSum = 0;
            for(String key : obsMapList.keySet())
            {
                List<Object> objects = obsMapList.get(key);
                if(objects == null || objects.size() == 0)
                {
                    continue;
                }
                for(int i = 0, num = objects.size(); i < num - 1; i = i + 2)
                {
                	if(objects.get(i) == null || objects.get(i + 1) == null)
                	{
                		invalidCount++;
                    	continue;
                	}
                	double obs = getDoubleValueFromObject(objects.get(i), element);
                	double fst = getDoubleValueFromObject(objects.get(i + 1), element);
                	if(obs == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || obs == DecodeConstants.UNDEF_DOUBLE_VALUE || fst >= DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE)
                	{
                		continue;
                	}
                	upSum += (obs - obsAvg) * (fst - fstAvg);
                	downSum += Math.sqrt(Math.pow(obs - obsAvg, 2)) * Math.sqrt(Math.pow(fst - fstAvg, 2));
                }
            }
            if(downSum == 0)
            {
                sum = DecodeConstants.UNDEF_DOUBLE_VALUE;
            }
            else
            {
                sum = upSum / downSum;
            }
        }
        else if("准确率".equals(method))
        {
            double dis = Double.parseDouble(configMap.get(element));
            double correct = 0;
            int invalidCount = 0;
            for(String key : obsMapList.keySet())
            {
                List<Object> objects = obsMapList.get(key);
                if(objects == null || objects.size() == 0)
                {
                    invalidCount++;
                    continue;
                }
                for(int i = 0, num = objects.size(); i < num - 1; i = i + 2)
                {
                	if(objects.get(i) == null || objects.get(i + 1) == null)
                	{
                		invalidCount++;
                    	continue;
                	}
                	double obs = getDoubleValueFromObject(objects.get(i), element);
                	double fst = getDoubleValueFromObject(objects.get(i + 1), element);
                	if(Math.abs(obs) == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || Math.abs(fst) >= DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE)
                	{
                		invalidCount++;
                		continue;
                	}
//                	System.out.println("obsDate: " + ReflectUtil.getFieldValueByName(objects.get(i), "datetime") + " fstDate: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "validdate") + " vti: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "vti") + " obs: " + obs + " fst: " + fst + " dis: " + Math.abs(obs - fst));
//                	System.out.println("obs: " + obs + " fst: " + fst + " dis: " + Math.abs(obs - fst));
                	if(Math.abs(obs - fst) <= dis)
                	{
                		correct++;
                	}
                }
            }
            if(count == invalidCount)
            {
                sum = DecodeConstants.UNDEF_DOUBLE_VALUE;
            }
            else
            {
                sum = correct / (count - invalidCount);
            }
        }

        sum = NumberFormatUtil.numFormat(sum, 3);

        return sum;
    }
    
    
    private double getStationValuePre12(String dataSource, int vti, String element, String method, Map<String, Map<String, ObsDataEntity>> obsDataMap, List<StationForecastDataEntity> fstDataList)
    {
    	if(fstDataList == null)
    	{
    		return DecodeConstants.UNDEF_DOUBLE_VALUE;
    	}
    	Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
    	Map<String, List<Object>> obsMapList = new HashMap<>();
    	Map<String, Map<String, List<Object>>> dataStationDateMapList = new HashMap<>();
    	
    	
    	if(element.equals("atmax") || element.equals("atmin"))
    	{
    		for(StationForecastDataEntity fstData : fstDataList)
    		{
    			String validDate = fstData.getValiddate().split(" ")[0];
    			ObsDataEntity obsData = obsDataMap.get(fstData.getStation()).get(fstData.getValiddate());
    			if(!dataStationDateMapList.containsKey(fstData.getStation()))
    			{
    				dataStationDateMapList.put(fstData.getStation(), new HashMap<>());
    			}
    			if(!dataStationDateMapList.get(fstData.getStation()).containsKey(validDate))
    			{
    				dataStationDateMapList.get(fstData.getStation()).put(validDate, new ArrayList<>());
    			}
    			dataStationDateMapList.get(fstData.getStation()).get(validDate).add(obsData);
    			dataStationDateMapList.get(fstData.getStation()).get(validDate).add(fstData);
    			
    		}
    		
    		for(String station : dataStationDateMapList.keySet())
    		{
    			List<Object> listObjs = new ArrayList<>();
    			for(String date : dataStationDateMapList.get(station).keySet())
    			{
    				List<Object> list = dataStationDateMapList.get(station).get(date);
    				double atMaxObs = -9999;
    				double atMinObs = 9999;
    				double atMaxFst = -9999;
    				double atMinFst = 9999;
    				String obsDate = null;
    				String fstDate = null;
    				String fstHour = null;
    				String vtiStr = null;
    				for(int i = 0, num = list.size(); i < num - 1; i = i + 2)
    				{
    					if(list.get(i) == null || list.get(i + 1) == null)
    					{
    						continue;
    					}
    					double obs = getDoubleValueFromObject(list.get(i), "at");
    					double fst = getDoubleValueFromObject(list.get(i + 1), "at");
    					if(atMaxObs < obs)
    					{
    						atMaxObs = obs;
    					}
    					if(atMinObs > obs)
    					{
    						atMinObs = obs;
    					}
    					if(atMaxFst < fst)
    					{
    						atMaxFst = fst;
    					}
    					if(atMinFst > fst)
    					{
    						atMinFst = fst;
    					}
    					obsDate = ReflectUtil.getFieldValueByName(list.get(i), "datetime").toString();
    					fstDate = ReflectUtil.getFieldValueByName(list.get(i + 1), "validdate").toString();
    					fstHour = ReflectUtil.getFieldValueByName(list.get(i + 1), "datatime").toString();
    					vtiStr = ReflectUtil.getFieldValueByName(list.get(i + 1), "vti").toString();
    				}
    				ObsDataEntity obsData = new ObsDataEntity();
    				obsData.setAtmax(atMaxObs);
    				obsData.setAtmin(atMinObs);
    				obsData.setDatetime(obsDate);
    				StationForecastDataEntity fstData = new StationForecastDataEntity();
    				fstData.setAtmax(atMaxFst);
    				fstData.setAtmin(atMinFst);
    				fstData.setValiddate(fstDate);
    				fstData.setDatatime(fstHour);
    				if(vtiStr != null)
    				{
    					fstData.setVti(Integer.parseInt(vtiStr));
    				}
    				listObjs.add(obsData);
    				listObjs.add(fstData);
    			}
    			obsMapList.put(station, listObjs);
    			
    		}
    		
    	}
    	else if(dataSource.equals(DataTypeEnum.SWC9KM.getDataType()))
    	{
    		for(StationForecastDataEntity fstData : fstDataList)
			{
				ObsDataEntity obsData = obsDataMap.get(fstData.getStation()).get(fstData.getValiddate());
				if(!obsMapList.containsKey(fstData.getStation()))
				{
					obsMapList.put(fstData.getStation(), new ArrayList<>());
				}
				obsMapList.get(fstData.getStation()).add(obsData);
				obsMapList.get(fstData.getStation()).add(fstData);
			}
    	}
    	else
    	{
    		Map<String, Map<String, List<StationForecastDataEntity>>> dataStationDateMapListTemp = new HashMap<>();
    		if(vti == 24)
    		{
    			String startDataTimeStr = fstDataList.get(0).getValiddate().split(" ")[0];
    			for(StationForecastDataEntity fstData : fstDataList)
    			{
    				String dataTime = TimeUtil.addHours(fstData.getDatatime(), 24);
    				if(!dataTime.startsWith(startDataTimeStr))
    				{
    					continue;
    				}
    				if(!dataStationDateMapListTemp.containsKey(fstData.getStation()))
    				{
    					dataStationDateMapListTemp.put(fstData.getStation(), new HashMap<>());
    				}
    				if(!dataStationDateMapListTemp.get(fstData.getStation()).containsKey(dataTime))
    				{
    					dataStationDateMapListTemp.get(fstData.getStation()).put(dataTime, new ArrayList<>());
    				}
    				dataStationDateMapListTemp.get(fstData.getStation()).get(dataTime).add(fstData);
    				
    			}
    			List<StationForecastDataEntity> czList = new ArrayList<>();
    			for(String station : dataStationDateMapListTemp.keySet())
    			{
    				for(String dateTime : dataStationDateMapListTemp.get(station).keySet())
    				{
    					List<StationForecastDataEntity> list = dataStationDateMapListTemp.get(station).get(dateTime);
    					Map<String, Double> map = new HashMap<>();
    					for(int i = 0, count = list.size(); i < count; i++)
						{
							map.put(list.get(i).getValiddate(), Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(list.get(i), element))));
						}
    					String startTime = TimeUtil.addHours(list.get(0).getDatatime(), 12);
    	                String endTimeStr = TimeUtil.addHours(startTime, 24);
    	                InterpolationResult interpolate = BilinearInterpolateUtil.interpolate(map, startTime, endTimeStr, 1, null);
    	                double[] czValues = interpolate.getValues();
						String time = String.valueOf(ReflectUtil.getFieldValueByName(list.get(0), "datatime"));
						int vtiTem = Integer.parseInt(String.valueOf(ReflectUtil.getFieldValueByName(list.get(0), "vti")));
						for(int i = 0; i < 24; i++)
						{
							String timeTem = TimeUtil.addHours(time, i);
							StationForecastDataEntity data = new StationForecastDataEntity();
							data.setDatatime(timeTem);
							data.setStation(station);
							data.setValiddate(TimeUtil.addHours(time, vtiTem + i));
							ReflectUtil.setFieldValueByName(data, czValues[i], element);
							czList.add(data);
						}
    					
    				}
    			}
    			
    			for(StationForecastDataEntity fstData : czList)
    			{
    				ObsDataEntity obsData = obsDataMap.get(fstData.getStation()).get(fstData.getValiddate());
    				if(!obsMapList.containsKey(fstData.getStation()))
    				{
    					obsMapList.put(fstData.getStation(), new ArrayList<>());
    				}
    				obsMapList.get(fstData.getStation()).add(obsData);
    				obsMapList.get(fstData.getStation()).add(fstData);
    			}
    			
    		}
    		else if(vti == 48)
    		{
    			String startDataTimeStr = fstDataList.get(0).getValiddate().split(" ")[0];
    			for(StationForecastDataEntity fstData : fstDataList)
    			{
    				String dataTime = TimeUtil.addHours(fstData.getDatatime(), 48);
    				if(!dataTime.startsWith(startDataTimeStr))
    				{
    					continue;
    				}
    				if(!dataStationDateMapListTemp.containsKey(fstData.getStation()))
    				{
    					dataStationDateMapListTemp.put(fstData.getStation(), new HashMap<>());
    				}
    				if(!dataStationDateMapListTemp.get(fstData.getStation()).containsKey(dataTime))
    				{
    					dataStationDateMapListTemp.get(fstData.getStation()).put(dataTime, new ArrayList<>());
    				}
    				dataStationDateMapListTemp.get(fstData.getStation()).get(dataTime).add(fstData);
    				
    			}
    			
    			List<StationForecastDataEntity> czList = new ArrayList<>();
    			for(String station : dataStationDateMapListTemp.keySet())
    			{
    				for(String dateTime : dataStationDateMapListTemp.get(station).keySet())
    				{
    					List<StationForecastDataEntity> list = dataStationDateMapListTemp.get(station).get(dateTime);
    					Map<String, Double> map = new HashMap<>();
    					for(int i = 0, count = list.size(); i < count; i++)
						{
							map.put(list.get(i).getValiddate(), Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(list.get(i), element))));
						}
    					String startTime = TimeUtil.addHours(list.get(0).getDatatime(), 36);
    	                String endTimeStr = TimeUtil.addHours(startTime, 24);
    	                InterpolationResult interpolate = BilinearInterpolateUtil.interpolate(map, startTime, endTimeStr, 1, null);
    	                double[] czValues = interpolate.getValues();
						String time = String.valueOf(ReflectUtil.getFieldValueByName(list.get(0), "datatime"));
						int vtiTem = Integer.parseInt(String.valueOf(ReflectUtil.getFieldValueByName(list.get(0), "vti")));
						for(int i = 0; i < 13; i++)
						{
							String timeTem = TimeUtil.addHours(time, i);
							StationForecastDataEntity data = new StationForecastDataEntity();
							data.setDatatime(timeTem);
							data.setStation(station);
							data.setValiddate(TimeUtil.addHours(time, vtiTem + i));
							ReflectUtil.setFieldValueByName(data, czValues[i], element);
							czList.add(data);
						}
    				}
    			}
    			
    			for(StationForecastDataEntity fstData : czList)
    			{
    				ObsDataEntity obsData = obsDataMap.get(fstData.getStation()).get(fstData.getValiddate());
    				if(!obsMapList.containsKey(fstData.getStation()))
    				{
    					obsMapList.put(fstData.getStation(), new ArrayList<>());
    				}
    				obsMapList.get(fstData.getStation()).add(obsData);
    				obsMapList.get(fstData.getStation()).add(fstData);
    			}
    			
    		}
    		else if(vti == 96 || vti == 120)
    		{
    			String startDataTimeStr = fstDataList.get(0).getValiddate().split(" ")[0];
    			for(StationForecastDataEntity fstData : fstDataList)
    			{
    				String dataTime = TimeUtil.addHours(fstData.getDatatime(), vti);
    				if(!dataTime.startsWith(startDataTimeStr))
    				{
    					continue;
    				}
    				if(!dataStationDateMapListTemp.containsKey(fstData.getStation()))
    				{
    					dataStationDateMapListTemp.put(fstData.getStation(), new HashMap<>());
    				}
    				if(!dataStationDateMapListTemp.get(fstData.getStation()).containsKey(dataTime))
    				{
    					dataStationDateMapListTemp.get(fstData.getStation()).put(dataTime, new ArrayList<>());
    				}
    				dataStationDateMapListTemp.get(fstData.getStation()).get(dataTime).add(fstData);
    				
    			}
    			
    			List<StationForecastDataEntity> czList = new ArrayList<>();
    			for(String station : dataStationDateMapListTemp.keySet())
    			{
    				for(String dateTime : dataStationDateMapListTemp.get(station).keySet())
    				{
    					List<StationForecastDataEntity> list = dataStationDateMapListTemp.get(station).get(dateTime);
    					Map<String, Double> map = new HashMap<>();
    					for(int i = 0, count = list.size(); i < count; i++)
						{
							map.put(list.get(i).getValiddate(), Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(list.get(i), element))));
//							System.out.println(list.get(i).getValiddate() + ": " + list.get(i).getAt());
						}
    					String startTime = TimeUtil.addHours(list.get(0).getDatatime(), vti + 12 - 24);
    	                String endTimeStr = TimeUtil.addHours(startTime, 24);
    	                InterpolationResult interpolate = BilinearInterpolateUtil.interpolate(map, startTime, endTimeStr, 3, null);
    	                double[] czValues = interpolate.getValues();
						String time = String.valueOf(ReflectUtil.getFieldValueByName(list.get(0), "datatime"));
						int vtiTem = Integer.parseInt(String.valueOf(ReflectUtil.getFieldValueByName(list.get(0), "vti")));
						for(int i = 0; i < 9; i++)
						{
							String timeTem = TimeUtil.addHours(time, i * 3);
							StationForecastDataEntity data = new StationForecastDataEntity();
							data.setDatatime(timeTem);
							data.setStation(station);
							data.setValiddate(TimeUtil.addHours(time, vtiTem + i * 3));
							ReflectUtil.setFieldValueByName(data, czValues[i], element);
							czList.add(data);
//							System.out.println(data.getValiddate() + ": " + data.getAt());
						}
    				}
    			}
    			
    			for(StationForecastDataEntity fstData : czList)
    			{
    				ObsDataEntity obsData = obsDataMap.get(fstData.getStation()).get(fstData.getValiddate());
    				if(!obsMapList.containsKey(fstData.getStation()))
    				{
    					obsMapList.put(fstData.getStation(), new ArrayList<>());
    				}
    				obsMapList.get(fstData.getStation()).add(obsData);
    				obsMapList.get(fstData.getStation()).add(fstData);
    			}
    			
    		}
    		else
    		{
    			for(StationForecastDataEntity fstData : fstDataList)
    			{
    				ObsDataEntity obsData = obsDataMap.get(fstData.getStation()).get(fstData.getValiddate());
    				if(!obsMapList.containsKey(fstData.getStation()))
    				{
    					obsMapList.put(fstData.getStation(), new ArrayList<>());
    				}
    				obsMapList.get(fstData.getStation()).add(obsData);
    				obsMapList.get(fstData.getStation()).add(fstData);
    			}
    		}
    		
    	}
    	
    	int count = 0;
    	for(String key : obsMapList.keySet())
    	{
    		count += obsMapList.get(key).size() / 2;
    	}
    	double sum = 0;
    	if("RMSE".equals(method))
    	{
    		int invalidCount = 0;
    		for(String key : obsMapList.keySet())
    		{
    			List<Object> objects = obsMapList.get(key);
    			if(objects == null || objects.size() == 0)
    			{
    				invalidCount++;
    				continue;
    			}
    			for(int i = 0, num = objects.size(); i < num - 1; i = i + 2)
    			{
    				if(objects.get(i) == null || objects.get(i + 1) == null)
    				{
    					invalidCount++;
    					continue;
    				}
    				double obs = getDoubleValueFromObject(objects.get(i), element);
    				double fst = getDoubleValueFromObject(objects.get(i + 1), element);
    				if(obs == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || obs == DecodeConstants.UNDEF_DOUBLE_VALUE || fst >= DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE)
    				{
    					invalidCount++;
    					continue;
    				}
    				sum += Math.pow(obs - fst, 2);
    			}
    		}
    		
    		if(count == invalidCount)
    		{
    			sum = DecodeConstants.UNDEF_DOUBLE_VALUE;
    		}
    		else
    		{
    			sum = Math.sqrt(sum / (count - invalidCount));
    		}
    	}
    	else if("MAE".equals(method))
    	{
    		int invalidCount = 0;
    		for(String key : obsMapList.keySet())
    		{
    			List<Object> objects = obsMapList.get(key);
    			if(objects == null || objects.size() == 0)
    			{
    				invalidCount++;
    				continue;
    			}
    			for(int i = 0, num = objects.size(); i < num - 1; i = i + 2)
    			{
    				if(objects.get(i) == null || objects.get(i + 1) == null)
    				{
    					invalidCount++;
    					continue;
    				}
    				double obs = getDoubleValueFromObject(objects.get(i), element);
    				double fst = getDoubleValueFromObject(objects.get(i + 1), element);
//                	System.out.println("vti: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "vti"));
					if(obs == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || obs == DecodeConstants.UNDEF_DOUBLE_VALUE || fst >= DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE)
					{
						invalidCount++;
						continue;
					}
//                	System.out.println("obsDate: " + ReflectUtil.getFieldValueByName(objects.get(i), "datetime") + " fstDate: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "validdate") + " vti: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "vti") + " fsttime: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "datatime") + " obs: " + obs + " fst: " + fst + " MAE: " + Math.abs(obs - fst));
//                	System.out.println("obs: " + obs + " fst: " + fst + " MAE: " + Math.abs(obs - fst));
					sum += Math.abs(obs - fst);
			    	}
			 }
//            System.out.println("invalidCount: " + invalidCount);
    		if(count == invalidCount)
    		{
    			sum = DecodeConstants.UNDEF_DOUBLE_VALUE;
    		}
    		else
    		{
    			sum = sum / (count - invalidCount);
    		}
    	}
    	else if("R".equals(method))
    	{
    		double obsSum = 0;
    		double fstSum = 0;
    		int invalidCount = 0;
    		for(String key : obsMapList.keySet())
    		{
    			List<Object> objects = obsMapList.get(key);
    			if(objects == null || objects.size() == 0)
    			{
    				invalidCount++;
    				continue;
    			}
    			for(int i = 0, num = objects.size(); i < num - 1; i = i + 2)
    			{
    				if(objects.get(i) == null || objects.get(i + 1) == null)
    				{
    					invalidCount++;
    					continue;
    				}
    				double obs = getDoubleValueFromObject(objects.get(i), element);
    				double fst = getDoubleValueFromObject(objects.get(i + 1), element);
    				if(obs == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || obs == DecodeConstants.UNDEF_DOUBLE_VALUE || fst >= DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE)
    				{
    					invalidCount++;
    					continue;
    				}
    				obsSum += obs;
    				fstSum += fst;
    			}
    		}
    		double obsAvg = obsSum / (count - invalidCount);
    		double fstAvg = fstSum / (count - invalidCount);
    		
    		double upSum = 0;
    		double downSum = 0;
    		for(String key : obsMapList.keySet())
    		{
    			List<Object> objects = obsMapList.get(key);
    			if(objects == null || objects.size() == 0)
    			{
    				continue;
    			}
    			for(int i = 0, num = objects.size(); i < num - 1; i = i + 2)
    			{
    				if(objects.get(i) == null || objects.get(i + 1) == null)
    				{
    					invalidCount++;
    					continue;
    				}
    				double obs = getDoubleValueFromObject(objects.get(i), element);
    				double fst = getDoubleValueFromObject(objects.get(i + 1), element);
    				if(obs == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || obs == DecodeConstants.UNDEF_DOUBLE_VALUE || fst >= DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE)
    				{
    					continue;
    				}
    				upSum += (obs - obsAvg) * (fst - fstAvg);
    				downSum += Math.sqrt(Math.pow(obs - obsAvg, 2)) * Math.sqrt(Math.pow(fst - fstAvg, 2));
    			}
    		}
    		if(downSum == 0)
    		{
    			sum = DecodeConstants.UNDEF_DOUBLE_VALUE;
    		}
    		else
    		{
    			sum = upSum / downSum;
    		}
    	}
    	else if("准确率".equals(method))
    	{
    		double dis = Double.parseDouble(configMap.get(element));
    		double correct = 0;
    		int invalidCount = 0;
    		for(String key : obsMapList.keySet())
    		{
    			List<Object> objects = obsMapList.get(key);
    			if(objects == null || objects.size() == 0)
    			{
    				invalidCount++;
    				continue;
    			}
    			for(int i = 0, num = objects.size(); i < num - 1; i = i + 2)
    			{
    				if(objects.get(i) == null || objects.get(i + 1) == null)
    				{
    					invalidCount++;
    					continue;
    				}
    				double obs = getDoubleValueFromObject(objects.get(i), element);
    				double fst = getDoubleValueFromObject(objects.get(i + 1), element);
    				if(Math.abs(obs) == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE || Math.abs(fst) >= DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE)
    				{
    					invalidCount++;
    					continue;
    				}
//                	System.out.println("obsDate: " + ReflectUtil.getFieldValueByName(objects.get(i), "datetime") + " fstDate: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "validdate") + " vti: " + ReflectUtil.getFieldValueByName(objects.get(i + 1), "vti") + " obs: " + obs + " fst: " + fst + " dis: " + Math.abs(obs - fst));
//                	System.out.println("obs: " + obs + " fst: " + fst + " dis: " + Math.abs(obs - fst));
    				if(Math.abs(obs - fst) <= dis)
    				{
    					correct++;
    				}
    			}
    		}
    		if(count == invalidCount)
    		{
    			sum = DecodeConstants.UNDEF_DOUBLE_VALUE;
    		}
    		else
    		{
    			sum = correct / (count - invalidCount);
    		}
    	}
    	
    	sum = NumberFormatUtil.numFormat(sum, 3);
    	
    	return sum;
    }

    private Map<String, Map<String, double[]>> getGribValue(String[] stations, String vti, List<ObsDataEntity> obsDataList, List<GribForecastDataEntity> fstDataList)
    {
        Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
        List<ObsDataEntity> obsDataListSub = new ArrayList<>();
        Map<String, Map<String, double[]>> result = new HashMap<>();
        if(fstDataList.size() == 0)
        {
            return result;
        }
        for(GribForecastDataEntity fstData : fstDataList)
        {
            for(ObsDataEntity obsData : obsDataList)
            {
                Date date = TimeUtil.String2Date(obsData.getDatetime(), "yyyy-MM-dd HH:mm:ss");
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                calendar.add(Calendar.HOUR_OF_DAY, 8);
                if(TimeUtil.date2String(calendar.getTime(), "yyyy-MM-dd HH:mm:ss").compareTo(fstData.getValidDate()) == 0)
                {
                    obsDataListSub.add(obsData);
                }
            }
        }
        int count = fstDataList.size();
        for(String station : stations)
        {
            double h = 0;
            double tn = 0;
            double f = 0;
            double m = 0;
            double hSum = 0;
            double fSum = 0;
            double mSum = 0;
            double th = 0;
            double tm = 0;
            double tf = 0;
            int realCount = 0;
            int nilCount = 0;
            Map<String, Double> fsMap = new HashMap<>();
            fsMap.put("0", Double.parseDouble("0"));
            for(int i = 0; i < count; i++)
            {
                String filePath = fstDataList.get(i).getSourcePath();
                double[][] gribDatas = null;
                if(filePath.endsWith(".txt"))
                {
                    gribDatas = GribUtil.readGribDatasFromTxt(filePath, ",");
                }
                else
                {
                    Map<String, Object> datasMap = GribFileReaderUtil.getDatasMap(filePath);
                    String prefix = "";
                    for(String key : datasMap.keySet())
                    {
                        if (key.contains(":")) {
                            String[] split = key.split(":");
                            prefix = split[0] + ":";
                        }
                    }
                    String lonlatStr = configMap.get("lonlat");
                    String[] split = lonlatStr.split(",");
                    //startLon,startLat,endLon,endLat
                    double[] lonlat = new double[]{Double.parseDouble(split[0]), Double.parseDouble(split[1]),
                                                   Double.parseDouble(split[2]), Double.parseDouble(split[3])};
                    String dataType = DataTypeUtil.getDataType(filePath);
                     String element = configMap.get(dataType);
                    if(element.contains("$"))
                    {
                        int[] disVtiAndVti = GribUtil.getDisVtiAndVtiStation(filePath, dataType);
                        element = element.replace("$", disVtiAndVti[1] + "");
                    }
                    double[][][] dataValues = NcReader.readByElemNameLayerSlice(datasMap, element, null);
                    String[] lonLatName = NcReader.getLonLatName(datasMap);
                    double[] lons = ReadLonLatUtil.readLonLat(datasMap, prefix + lonLatName[0]);
                    double[] lats = ReadLonLatUtil.readLonLat(datasMap, prefix + lonLatName[1]);
                    gribDatas = SliceArrayUtil.slice(dataValues[0], lons, lats, lonlat);
                }
                double lon = 0;
                double lat = 0;
                double value = 999999;

                for(ObsDataEntity obsData : obsDataListSub)
                {
                    Date date = TimeUtil.String2Date(obsData.getDatetime(), "yyyy-MM-dd HH:mm:ss");
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(date);
                    calendar.add(Calendar.HOUR_OF_DAY, 8);
                    if(station.equals(obsData.getStation()) && fstDataList.get(i).getValidDate().compareTo(TimeUtil.date2String(calendar.getTime(), "yyyy-MM-dd HH:mm:ss")) == 0)
                    {
                        lon = obsData.getLon();
                        lat = obsData.getLat();
                        value = obsData.getRain24();
                        break;
                    }
                }
                double fstValue = getFstValue(lon, lat, gribDatas);
                fsMap.put(fstDataList.get(i).getVti() + "", fstValue);
                fstValue = fstValue - fsMap.get((fstDataList.get(i).getVti() - 24) + "");

//                System.out.println("vti:" + fstDataList.get(i).getVti() + "fst:" + fstValue + " obs:" + value);

                if(value == 999999)
                {
//                    value = 0;
                    nilCount++;
                    continue;
                }
                if(value >= 0.1 && fstValue >= 0.1)
                {
                    h++;
                }
                else if(value < 0.1 && fstValue < 0.1)
                {
                    tn++;
                }
                else if(value >= 0.1 && fstValue < 0.1)
                {
                    m++;
                }
                else if(value < 0.1 && fstValue >= 0.1)
                {
                    f++;
                }
            }

            if(count == nilCount)
            {
                if(!result.containsKey(station))
                {
                    result.put(station, new HashMap<>());
                }
                result.get(station).put(vti, null);
            }
            else
            {
                th = (h + tn) / (h + f + m + tn);
                tf = f / ((f + h) == 0 ? 1 : (f + h));
                tm = m / ((m + tn) == 0 ? 1 : (tn + m));

//                System.out.println("station:" + station + " h: " + h + " f: " + f + " m: " + m + " tn: " + tn);

                hSum += th;
                fSum += tf;
                mSum += tm;
                if(!result.containsKey(station))
                {
                    result.put(station, new HashMap<>());
                }
//                realCount = count - nilCount;
                result.get(station).put(vti, new double[]{hSum, fSum, mSum});
            }

        }

        return result;
    }

    private double getFstValue(double lon, double lat, double[][] values)
    {
        Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
        String lonlat = configMap.get("lonlat");
        String[] split = lonlat.split(",");
        double[] lonlats = new double[split.length];
        for(int i = 0, count = lonlats.length; i < count; i++)
        {
            lonlats[i] = Double.parseDouble(split[i]);
        }
        double lon_dis = Double.parseDouble(configMap.get("lon_dis"));
        double lat_dis = Double.parseDouble(configMap.get("lat_dis"));
        double[] findFirstPoint = ReadGribRainUtil.findFindFirstPoint(lon, lat, lon_dis, lat_dis, lonlats);
//        System.out.println("lonCount:" + values[0].length + ", latCount" + values.length);
//        System.out.println("lon:" + lon + ", lat:" + lat);
//        findFirstPoint: 670.0, 405.0
//        System.out.println("findFirstPoint: " + findFirstPoint[0] + ", " + findFirstPoint[1]);
        double topLeft = values[(int) findFirstPoint[1]][(int) findFirstPoint[0]];
        double topRight = values[(int) findFirstPoint[1] + 1][(int) findFirstPoint[0]];
        double bottomLeft = values[(int) findFirstPoint[1]][(int) findFirstPoint[0] + 1];
        double bottomRight = values[(int) findFirstPoint[1] + 1][(int) findFirstPoint[0] + 1];
        double value = ReadGribRainUtil.bilinearInterpolation(topLeft, topRight, bottomLeft, bottomRight, findFirstPoint[2], findFirstPoint[3]);

        return value;
    }

    private double getDoubleValueFromObject(Object object, String element)
    {
        double result = 999999;
        Object value = ReflectUtil.getFieldValueByName(object, element);
        if(value != null)
        {
            result = Double.parseDouble(value.toString());
        }
        else
        {
            result = 999999;
        }

        return result;
    }

	@Override
	public int addCheckValue(CheckDataIndbEntity data) {
		
		return stationForecastDataMapper.addCheckValue(data);
	}

	@Override
	public int addCheckGribValue(CheckDataGribIndbEntity data) {
		
		return gribForecastDataMapper.addCheckGribValue(data);
	}

	@Override
	public int updateCheckGribValue(CheckDataGribIndbEntity data) {
		
		return gribForecastDataMapper.updateCheckGribValue(data);
	}

	@Override
	public List<CheckDataGribIndbEntity> queryGribCheckValue(
			CheckDataGribIndbEntity data) {
		
		return gribForecastDataMapper.queryGribCheckValue(data);
	}

	@Override
	public int updateCheckValue(CheckDataIndbEntity data) {
		
		return stationForecastDataMapper.updateCheckValue(data);
	}

	@Override
	public List<CheckDataIndbEntity> queryCheckValue(CheckDataIndbEntity data) {
		
		return stationForecastDataMapper.queryCheckValue(data);
	}

	@Override
	public int updateStationCheckRainValue(CheckDataIndbEntity data) {
		
		return stationForecastDataMapper.updateStationCheckRainValue(data);
	}

	@Override
	public List<CheckDataIndbEntity> dealStationMonthCheck(CheckDataParams params) {
		String elementsStr = "atmae24,atrsme24,atcorr24,atrate24,atmaxmae24,atmaxrsme24,atmaxcorr24,atmaxrate24,atminmae24,atminrsme24,atmincorr24,atminrate24,rain24h24,rain24tn24,rain24m24,rain24f24,wsmae24,wsrsme24,wscorr24,wsrate24,atmae48,atrsme48,atcorr48,atrate48,atmaxmae48,atmaxrsme48,atmaxcorr48,atmaxrate48,atminmae48,atminrsme48,atmincorr48,atminrate48,rain24h48,rain24tn48,rain24m48,rain24f48,wsmae48,wsrsme48,wscorr48,wsrate48,atmae72,atrsme72,atcorr72,atrate72,atmaxmae72,atmaxrsme72,atmaxcorr72,atmaxrate72,atminmae72,atminrsme72,atmincorr72,atminrate72,rain24h72,rain24tn72,rain24m72,rain24f72,wsmae72,wsrsme72,wscorr72,wsrate72,atmae96,atrsme96,atcorr96,atrate96,atmaxmae96,atmaxrsme96,atmaxcorr96,atmaxrate96,atminmae96,atminrsme96,atmincorr96,atminrate96,rain24h96,rain24tn96,rain24m96,rain24f96,wsmae96,wsrsme96,wscorr96,wsrate96,atmae120,atrsme120,atcorr120,atrate120,atmaxmae120,atmaxrsme120,atmaxcorr120,atmaxrate120,atminmae120,atminrsme120,atmincorr120,atminrate120,rain24h120,rain24tn120,rain24m120,rain24f120,wsmae120,wsrsme120,wscorr120,wsrate120,atmae144,atrsme144,atcorr144,atrate144,atmaxmae144,atmaxrsme144,atmaxcorr144,atmaxrate144,atminmae144,atminrsme144,atmincorr144,atminrate144,rain24h144,rain24tn144,rain24m144,rain24f144,wsmae144,wsrsme144,wscorr144,wsrate144,atmae168,atrsme168,atcorr168,atrate168,atmaxmae168,atmaxrsme168,atmaxcorr168,atmaxrate168,atminmae168,atminrsme168,atmincorr168,atminrate168,rain24h168,rain24tn168,rain24m168,rain24f168,wsmae168,wsrsme168,wscorr168,wsrate168,atmae192,atrsme192,atcorr192,atrate192,atmaxmae192,atmaxrsme192,atmaxcorr192,atmaxrate192,atminmae192,atminrsme192,atmincorr192,atminrate192,rain24h192,rain24tn192,rain24m192,rain24f192,wsmae192,wsrsme192,wscorr192,wsrate192,atmae216,atrsme216,atcorr216,atrate216,atmaxmae216,atmaxrsme216,atmaxcorr216,atmaxrate216,atminmae216,atminrsme216,atmincorr216,atminrate216,rain24h216,rain24tn216,rain24m216,rain24f216,wsmae216,wsrsme216,wscorr216,wsrate216,atmae240,atrsme240,atcorr240,atrate240,atmaxmae240,atmaxrsme240,atmaxcorr240,atmaxrate240,atminmae240,atminrsme240,atmincorr240,atminrate240,rain24h240,rain24tn240,rain24m240,rain24f240,wsmae240,wsrsme240,wscorr240,wsrate240";
		String[] elements = elementsStr.split(",");
		String othersStr = "atmae24,atrsme24,atcorr24,atrate24,atmaxmae24,atmaxrsme24,atmaxcorr24,atmaxrate24,atminmae24,atminrsme24,atmincorr24,atminrate24,wsmae24,wsrsme24,wscorr24,wsrate24,atmae48,atrsme48,atcorr48,atrate48,atmaxmae48,atmaxrsme48,atmaxcorr48,atmaxrate48,atminmae48,atminrsme48,atmincorr48,atminrate48,wsmae48,wsrsme48,wscorr48,wsrate48,atmae72,atrsme72,atcorr72,atrate72,atmaxmae72,atmaxrsme72,atmaxcorr72,atmaxrate72,atminmae72,atminrsme72,atmincorr72,atminrate72,wsmae72,wsrsme72,wscorr72,wsrate72,atmae96,atrsme96,atcorr96,atrate96,atmaxmae96,atmaxrsme96,atmaxcorr96,atmaxrate96,atminmae96,atminrsme96,atmincorr96,atminrate96,wsmae96,wsrsme96,wscorr96,wsrate96,atmae120,atrsme120,atcorr120,atrate120,atmaxmae120,atmaxrsme120,atmaxcorr120,atmaxrate120,atminmae120,atminrsme120,atmincorr120,atminrate120,wsmae120,wsrsme120,wscorr120,wsrate120,atmae144,atrsme144,atcorr144,atrate144,atmaxmae144,atmaxrsme144,atmaxcorr144,atmaxrate144,atminmae144,atminrsme144,atmincorr144,atminrate144,wsmae144,wsrsme144,wscorr144,wsrate144,atmae168,atrsme168,atcorr168,atrate168,atmaxmae168,atmaxrsme168,atmaxcorr168,atmaxrate168,atminmae168,atminrsme168,atmincorr168,atminrate168,wsmae168,wsrsme168,wscorr168,wsrate168,atmae192,atrsme192,atcorr192,atrate192,atmaxmae192,atmaxrsme192,atmaxcorr192,atmaxrate192,atminmae192,atminrsme192,atmincorr192,atminrate192,wsmae192,wsrsme192,wscorr192,wsrate192,atmae216,atrsme216,atcorr216,atrate216,atmaxmae216,atmaxrsme216,atmaxcorr216,atmaxrate216,atminmae216,atminrsme216,atmincorr216,atminrate216,wsmae216,wsrsme216,wscorr216,wsrate216,atmae240,atrsme240,atcorr240,atrate240,atmaxmae240,atmaxrsme240,atmaxcorr240,atmaxrate240,atminmae240,atminrsme240,atmincorr240,atminrate240,wsmae240,wsrsme240,wscorr240,wsrate240";
		String[] others = othersStr.split(",");
//		String rainEleStr = "rain24h24,rain24tn24,rain24m24,rain24f24,rain24h48,rain24tn48,rain24m48,rain24f48,rain24h72,rain24tn72,rain24m72,rain24f72,rain24h96,rain24tn96,rain24m96,rain24f96,rain24h120,rain24tn120,rain24m120,rain24f120,rain24h144,rain24tn144,rain24m144,rain24f144,rain24h168,rain24tn168,rain24m168,rain24f168,rain24h192,rain24tn192,rain24m192,rain24f192,rain24h216,rain24tn216,rain24m216,rain24f216,rain24h240,rain24tn240,rain24m240,rain24f240";
//		String[] rainEles = rainEleStr.split(",");
		List<CheckDataIndbEntity> list = stationForecastDataMapper.queryStationCheckDataDay(params);
//   station_datasource  hour
		Map<String, Map<String, List<CheckDataIndbEntity>>> map = new HashMap<>();
		String station = null;
		String hour = null;
		String dataSource = null;
		String key = null;
		Calendar calendar = Calendar.getInstance();
		for(CheckDataIndbEntity data : list)
		{
			station = data.getStation();
			hour = String.valueOf(data.getHour());
			dataSource = data.getDataSource();
			key = station + "_" + dataSource;
			if(!map.containsKey(key))
			{
				map.put(key, new HashMap<>());
			}
			if(!map.get(key).containsKey(hour))
			{
				map.get(key).put(hour, new ArrayList<>());
			}
			
			map.get(key).get(hour).add(data);
		}
		List<CheckDataIndbEntity> resultList = new ArrayList<>();
		List<CheckDataIndbEntity> tempList = null;
		CheckDataIndbEntity tempData = null;
		CheckDataIndbEntity countData = null;
		for(String stationDataSource : map.keySet())
		{
			for(String hourStr : map.get(stationDataSource).keySet())
			{
				tempList = map.get(stationDataSource).get(hourStr);
				tempData = new CheckDataIndbEntity();
				countData = new CheckDataIndbEntity();
				tempData.init();
				countData.init();
				for(CheckDataIndbEntity d : tempList)
				{
					for(String ele : elements)
					{
						double value = (double) ReflectUtil.getFieldValueByName(d, ele);
						if(value != DecodeConstants.UNDEF_DOUBLE_VALUE)
						{
							ReflectUtil.setFieldValueByName(tempData, value + (double) ReflectUtil.getFieldValueByName(tempData, ele), ele);
							ReflectUtil.setFieldValueByName(countData, (double) ReflectUtil.getFieldValueByName(countData, ele) + 1, ele);
						}
					}
				}
				for(String ele : others)
				{
					double value = (double) ReflectUtil.getFieldValueByName(tempData, ele);
					double count = (double) ReflectUtil.getFieldValueByName(countData, ele);
					if(count != 0)
					{
						ReflectUtil.setFieldValueByName(tempData, NumberFormatUtil.numFormat(value / count, 3), ele);
					}
					else
					{
						ReflectUtil.setFieldValueByName(tempData, DecodeConstants.UNDEF_DOUBLE_VALUE, ele);
					}
				}
//				for(int i = 0, total = rainEles.length; i < total; i = i + 4)
//				{
//					
//				}
				tempData.setHour(Integer.parseInt(hourStr));
				String[] split = stationDataSource.split("_");
				tempData.setStation(split[0]);
				tempData.setDataSource(split[1]);
				tempData.setDataTime(params.getStartTime().substring(0, params.getStartTime().length() - 3));
				calendar.setTime(new Date());
				calendar.add(Calendar.HOUR_OF_DAY, 8);
				tempData.setInserttime(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
				resultList.add(tempData);
			}
		}
		
		for(CheckDataIndbEntity data : resultList)
		{
			int i = stationForecastDataMapper.addStationCheckValueMonth(data);
		}
		
		
		return null;
	}

	@Override
	public List<CheckDataIndbEntity> dealGribMonthCheck(CheckDataParams params) {
		String rainEleStr = "rain24h24,rain24tn24,rain24m24,rain24f24,rain24h48,rain24tn48,rain24m48,rain24f48,rain24h72,rain24tn72,rain24m72,rain24f72,rain24h96,rain24tn96,rain24m96,rain24f96,rain24h120,rain24tn120,rain24m120,rain24f120,rain24h144,rain24tn144,rain24m144,rain24f144,rain24h168,rain24tn168,rain24m168,rain24f168,rain24h192,rain24tn192,rain24m192,rain24f192,rain24h216,rain24tn216,rain24m216,rain24f216,rain24h240,rain24tn240,rain24m240,rain24f240";
		String[] elements = rainEleStr.split(",");
		List<CheckDataIndbEntity> list = stationForecastDataMapper.queryGribCheckDataDay(params);
//   station_datasource  hour
		Map<String, Map<String, List<CheckDataIndbEntity>>> map = new HashMap<>();
		String station = null;
		String hour = null;
		String dataSource = null;
		String key = null;
		Calendar calendar = Calendar.getInstance();
		for(CheckDataIndbEntity data : list)
		{
			station = data.getStation();
			hour = String.valueOf(data.getHour());
			dataSource = data.getDataSource();
			key = station + "-" + dataSource;
			if(!map.containsKey(key))
			{
				map.put(key, new HashMap<>());
			}
			if(!map.get(key).containsKey(hour))
			{
				map.get(key).put(hour, new ArrayList<>());
			}
			
			map.get(key).get(hour).add(data);
		}
		List<CheckDataIndbEntity> resultList = new ArrayList<>();
		List<CheckDataIndbEntity> tempList = null;
		CheckDataIndbEntity tempData = null;
//		CheckDataIndbEntity countData = null;
		for(String stationDataSource : map.keySet())
		{
			for(String hourStr : map.get(stationDataSource).keySet())
			{
				tempList = map.get(stationDataSource).get(hourStr);
				tempData = new CheckDataIndbEntity();
//				countData = new CheckDataIndbEntity();
				tempData.init();
//				countData.init();
				for(CheckDataIndbEntity d : tempList)
				{
					for(String ele : elements)
					{
						double value = (double) ReflectUtil.getFieldValueByName(d, ele);
						if(value != DecodeConstants.UNDEF_DOUBLE_VALUE)
						{
							ReflectUtil.setFieldValueByName(tempData, value + (double) ReflectUtil.getFieldValueByName(tempData, ele), ele);
//							ReflectUtil.setFieldValueByName(countData, (double) ReflectUtil.getFieldValueByName(countData, ele) + 1, ele);
						}
					}
				}
				tempData.setHour(Integer.parseInt(hourStr));
				String[] split = stationDataSource.split("-");
				tempData.setStation(split[0]);
				tempData.setDataSource(split[1]);
				tempData.setDataTime(params.getStartTime().substring(0, params.getStartTime().length() - 3));
				calendar.setTime(new Date());
				calendar.add(Calendar.HOUR_OF_DAY, 8);
				tempData.setInserttime(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
				resultList.add(tempData);
			}
		}
		
		for(CheckDataIndbEntity data : resultList)
		{
			int i = stationForecastDataMapper.addGribCheckValueMonth(data);
		}
		
		return null;
	}

}
