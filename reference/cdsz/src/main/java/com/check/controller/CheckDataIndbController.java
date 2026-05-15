package com.check.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.hutool.core.io.FileUtil;

import com.check.pojo.CheckDataGribIndbEntity;
import com.check.pojo.CheckDataIndbEntity;
import com.check.pojo.CheckDataParams;
import com.check.service.inf.CheckDataIndbService;
import com.util.ReadPropertiesUtil;
import com.util.ReflectUtil;
import com.util.TimeUtil;

@RestController
@RequestMapping("/check")
@Api(tags = "预报检验入库")
public class CheckDataIndbController {
	 	@Resource
	    private CheckDataIndbService checkDataService;
	 	private Map<String, String> vtiElementMap = ReadPropertiesUtil.getUserConfigMap("vti_elements.properties");
	    @ApiOperation("站点评估逐时")
	    @PostMapping("checkStationDataHourIndb")
	    public Map<String, Map<String, Map<String, Map<String, Double>>>> checkStationDataHour(@RequestBody CheckDataParams param)
	    {
	        Map<String, Map<String, Map<String, Map<String, Double>>>> checkDataEntities = checkDataService.checkStationDataHour(param);

//	        System.out.println(checkDataEntities);
	        return checkDataEntities;
	    }

	    @ApiOperation("站点评估逐日")
	    @PostMapping("checkStationDataDayIndb")
	    public Map<String, Map<String, Map<String, CheckDataIndbEntity>>> checkStationDataDay(@RequestBody CheckDataParams param)
	    {
	    	System.out.println("check indb start time: " + TimeUtil.date2String(new Date(), TimeUtil.DEFAULT_DATETIME_FORMAT));
	    	try{
	    		int[] vtis = new int[]{24, 48, 72, 96, 120, 144, 168, 192, 216, 240};
//		    	int[] vtis = new int[]{240};
		    	Map<String, Map<String, Map<String, CheckDataIndbEntity>>> resultMap = new HashMap<>();
		    	Map<String, Map<String, Map<String, CheckDataIndbEntity>>> resultRainMap = new HashMap<>();
//		    	   dataSource    station    datetime
		    	Map<String, Map<String, Map<String, CheckDataIndbEntity>>> map = null;
		    	Map<String, Map<String, Map<String, CheckDataIndbEntity>>> mapRain = null;
		    	CheckDataIndbEntity data = null;
		    	CheckDataIndbEntity orgData = null;
		    	CheckDataIndbEntity orgDataRain = null;
		    	String[] vtiElements = null;
		    	String startTime = param.getStartValidDate();
		    	String endTime = param.getEndValidDate();
		    	String[] elements = param.getElements();
		    	int hour = Integer.parseInt(startTime.split(" ")[1].split(":")[0]);
		    	for(int vti : vtis)
		    	{
		    		param.setVti(vti);
		    		param.setElements(elements);
		    		param.setStartValidDate(TimeUtil.addHours(startTime, 24));
		    		param.setEndValidDate(TimeUtil.addHours(endTime, 24));
//		    		System.out.println("站点计算中除降水外的要素参数:" + JSONObject.toJSONString(param));
		    		map = checkDataService.checkStationDataDay(param);
		    		param.setStartValidDate(startTime);
		    		param.setEndValidDate(endTime);
		    		mapRain = checkDataService.checkStationDataDayRain(param);
		    		for(String dataSource : map.keySet())
		    		{
		    			if(!resultMap.containsKey(dataSource))
		    			{
		    				resultMap.put(dataSource, new HashMap<>());
		    			}
		    			if(map.get(dataSource) == null)
						{
							continue;
						}
		    			for(String station : map.get(dataSource).keySet())
		    			{
		    				if(!resultMap.get(dataSource).containsKey(station))
		    				{
		    					resultMap.get(dataSource).put(station, new HashMap<>());
		    				}
		    				if(map.get(dataSource).get(station) == null)
	    					{
	    						continue;
	    					}
		    				for(String date : map.get(dataSource).get(station).keySet())
		    				{
		    					if(!resultMap.get(dataSource).get(station).containsKey(date))
		    					{
		    						resultMap.get(dataSource).get(station).put(date, new CheckDataIndbEntity());
		    					}
		    					orgData = map.get(dataSource).get(station).get(date);
//		    					orgDataRain = mapRain.get(dataSource).get(station).get(date);
		    					data = resultMap.get(dataSource).get(station).get(date);
//		    					System.out.println("========================: " + ReflectUtil.getFieldValueByName(orgData, "dataSource"));
		    					ReflectUtil.setFieldValueByName(data, dataSource, "dataSource");
		    					ReflectUtil.setFieldValueByName(data, hour, "hour");
		    					ReflectUtil.setFieldValueByName(data, ReflectUtil.getFieldValueByName(orgData, "station"), "station");
		    					ReflectUtil.setFieldValueByName(data, ReflectUtil.getFieldValueByName(orgData, "dataTime"), "dataTime");
		    					vtiElements = vtiElementMap.get(vti + "").split(",");
		    					for(String element : vtiElements)
		    					{
		    						ReflectUtil.setFieldValueByName(data, ReflectUtil.getFieldValueByName(orgData, element), element);
		    					}
		    				}
		    			}
		    		}
		    		
		    		for(String dataSource : mapRain.keySet())
		    		{
		    			if(!resultRainMap.containsKey(dataSource))
		    			{
		    				resultRainMap.put(dataSource, new HashMap<>());
		    			}
		    			if(mapRain.get(dataSource) == null)
		    			{
		    				continue;
		    			}
		    			for(String station : mapRain.get(dataSource).keySet())
		    			{
		    				if(!resultRainMap.get(dataSource).containsKey(station))
		    				{
		    					resultRainMap.get(dataSource).put(station, new HashMap<>());
		    				}
		    				if(mapRain.get(dataSource).get(station) == null)
		    				{
		    					continue;
		    				}
		    				for(String date : mapRain.get(dataSource).get(station).keySet())
		    				{
		    					if(!resultRainMap.get(dataSource).get(station).containsKey(date))
		    					{
		    						resultRainMap.get(dataSource).get(station).put(date, new CheckDataIndbEntity());
		    					}
		    					if(mapRain.get(dataSource) == null || mapRain.get(dataSource).get(station) == null || mapRain.get(dataSource).get(station).get(date) == null)
		    					{
		    						continue;
		    					}
//		    					orgData = map.get(dataSource).get(station).get(date);
		    					orgDataRain = mapRain.get(dataSource).get(station).get(date);
		    					data = resultRainMap.get(dataSource).get(station).get(date);
//		    					System.out.println("========================: " + ReflectUtil.getFieldValueByName(orgData, "dataSource"));
		    					ReflectUtil.setFieldValueByName(data, dataSource, "dataSource");
		    					ReflectUtil.setFieldValueByName(data, hour, "hour");
		    					ReflectUtil.setFieldValueByName(data, ReflectUtil.getFieldValueByName(orgDataRain, "station"), "station");
		    					ReflectUtil.setFieldValueByName(data, ReflectUtil.getFieldValueByName(orgDataRain, "dataTime"), "dataTime");
		    					vtiElements = vtiElementMap.get(vti + "").split(",");
//		    					for(String element : vtiElements)
//		    					{
//		    						ReflectUtil.setFieldValueByName(data, ReflectUtil.getFieldValueByName(orgData, element), element);
//		    					}
		    					if(orgDataRain != null)
		    					{
		    						vtiElements = vtiElementMap.get("rain" + vti).split(",");
		    						for(String element : vtiElements)
		    						{
		    							ReflectUtil.setFieldValueByName(data, ReflectUtil.getFieldValueByName(orgDataRain, element), element);
		    						}
		    					}
		    				}
		    			}
		    		}
		    	}
		    	
		    	for(String dataSource : resultMap.keySet())
		    	{
		    		for(String station : resultMap.get(dataSource).keySet())
		    		{
		    			if(resultMap.get(dataSource).get(station) == null)
		    			{
		    				continue;
		    			}
		    			for(String date : resultMap.get(dataSource).get(station).keySet())
		    			{
		    				CheckDataIndbEntity dataIndbEntity = resultMap.get(dataSource).get(station).get(date);
		    				if(dataIndbEntity.getStation() == null)
		    				{
		    					continue;
		    				}
		    				ReflectUtil.setFieldValueByName(dataIndbEntity, TimeUtil.date2String(new Date(), TimeUtil.DEFAULT_DATETIME_FORMAT), "inserttime");
		    				List<CheckDataIndbEntity> list = checkDataService.queryCheckValue(dataIndbEntity);
//		    				System.out.println(JSONObject.toJSONString(dataIndbEntity));
//		    				System.out.println("f: " + dataIndbEntity.getRain24f24() + " m: " + dataIndbEntity.getRain24m24());
		    				if(list.size() > 0)
		    				{
		    					checkDataService.updateCheckValue(dataIndbEntity);
		    				}
		    				else
		    				{
		    					checkDataService.addCheckValue(dataIndbEntity);
		    				}
		    			}
		    			
		    			if(resultRainMap.get(dataSource).get(station) == null)
		    			{
		    				continue;
		    			}
		    			for(String date : resultRainMap.get(dataSource).get(station).keySet())
		    			{
		    				CheckDataIndbEntity dataIndbEntity = resultRainMap.get(dataSource).get(station).get(date);
		    				if(dataIndbEntity.getStation() == null)
		    				{
		    					continue;
		    				}
		    				ReflectUtil.setFieldValueByName(dataIndbEntity, TimeUtil.date2String(new Date(), TimeUtil.DEFAULT_DATETIME_FORMAT), "inserttime");
		    				List<CheckDataIndbEntity> list = checkDataService.queryCheckValue(dataIndbEntity);
//		    				System.out.println(JSONObject.toJSONString(dataIndbEntity));
//		    				System.out.println("f: " + dataIndbEntity.getRain24f24() + " m: " + dataIndbEntity.getRain24m24());
		    				if(list.size() > 0)
		    				{
		    					checkDataService.updateStationCheckRainValue(dataIndbEntity);
		    				}
		    				else
		    				{
		    					checkDataService.addCheckValue(dataIndbEntity);
		    				}
		    			}
		    		}
		    	}
	    	} catch(Exception e){
	    		e.printStackTrace();
	    		FileUtil.writeString(e.getMessage(), "/home/yyyh/cdsz/check_error.txt", "utf-8");
	    	}
	    	
	    	

	        return null;
	    }

	    @ApiOperation("格点逐时评估")
	    @PostMapping("checkGribDataHourIndb")
	    public Map<String, Map<String, Map<String, Double>>> checkGribDataHour(@RequestBody CheckDataParams param)
	    {
	        Map<String, Map<String, Map<String, Double>>> checkDataEntities = checkDataService.checkGribDataHour(param);

//	        System.out.println(checkDataEntities);
	        return checkDataEntities;
	    }

	    @ApiOperation("格点逐日评估")
	    @PostMapping("checkGribDataDayIndb")
	    public Map<String, Map<String, Map<String, CheckDataGribIndbEntity>>> checkGribDataDay(@RequestBody CheckDataParams param)
	    {
	    	int[] vtis = new int[]{24 ,48 ,72 ,96 ,120,144,168,192,216,240};
//	    	int[] vtis = new int[]{24};
//	    	   dataSource    station    datetime
	    	Map<String, Map<String, Map<String, CheckDataGribIndbEntity>>> resultMap = new HashMap<>();
//	    	   dataSource    station    datetime
	    	Map<String, Map<String, Map<String, CheckDataGribIndbEntity>>> map = null;
	    	
	    	CheckDataGribIndbEntity data = null;
	    	CheckDataGribIndbEntity orgData = null;
	    	String[] vtiElements = null;
	    	String startTime = param.getStartValidDate();
	    	int hour = Integer.parseInt(startTime.split(" ")[1].split(":")[0]);
	    	for(int vti : vtis)
	    	{
	    		param.setVti(vti);
	    		param.setStartValidDate(startTime);
	    		map = checkDataService.checkGribDataDay(param);
	    		for(String dataSource : map.keySet())
	    		{
	    			if(!resultMap.containsKey(dataSource))
	    			{
	    				resultMap.put(dataSource, new HashMap<>());
	    			}
	    			for(String station : map.get(dataSource).keySet())
	    			{
	    				if(!resultMap.get(dataSource).containsKey(station))
	    				{
	    					resultMap.get(dataSource).put(station, new HashMap<>());
	    				}
	    				for(String date : map.get(dataSource).get(station).keySet())
	    				{
	    					if(!resultMap.get(dataSource).get(station).containsKey(date))
	    					{
	    						resultMap.get(dataSource).get(station).put(date, new CheckDataGribIndbEntity());
	    					}
	    					
	    					orgData = map.get(dataSource).get(station).get(date);
	    					data = resultMap.get(dataSource).get(station).get(date);
//	    					System.out.println("========================: " + ReflectUtil.getFieldValueByName(orgData, "dataSource"));
	    					ReflectUtil.setFieldValueByName(data, dataSource, "dataSource");
	    					ReflectUtil.setFieldValueByName(data, hour, "hour");
	    					ReflectUtil.setFieldValueByName(data, ReflectUtil.getFieldValueByName(orgData, "station"), "station");
	    					ReflectUtil.setFieldValueByName(data, ReflectUtil.getFieldValueByName(orgData, "dataTime"), "dataTime");
	    					vtiElements = vtiElementMap.get("rain" + vti + "").split(",");
	    					for(String element : vtiElements)
	    					{
	    						ReflectUtil.setFieldValueByName(data, ReflectUtil.getFieldValueByName(orgData, element), element);
	    					}
	    				}
	    			}
	    		}
	    	}
	    	
	    	for(String dataSource : resultMap.keySet())
	    	{
	    		for(String station : resultMap.get(dataSource).keySet())
	    		{
	    			for(String date : resultMap.get(dataSource).get(station).keySet())
	    			{
	    				CheckDataGribIndbEntity dataIndbEntity = resultMap.get(dataSource).get(station).get(date);
	    				ReflectUtil.setFieldValueByName(dataIndbEntity, TimeUtil.date2String(new Date(), TimeUtil.DEFAULT_DATETIME_FORMAT), "inserttime");
	    				List<CheckDataGribIndbEntity> list = checkDataService.queryGribCheckValue(dataIndbEntity);
	    				if(list.size() > 0)
	    				{
	    					checkDataService.updateCheckGribValue(dataIndbEntity);
	    				}
	    				else
	    				{
	    					checkDataService.addCheckGribValue(dataIndbEntity);
	    				}
	    			}
	    		}
	    	}

	        return null;
	    }
	    
	    @ApiOperation("站点月统计")
	    @PostMapping("checkStationDataMonth")
	    public void checkStationDataMonth(@RequestBody CheckDataParams param)
	    {
	    	List<CheckDataIndbEntity> list = checkDataService.dealStationMonthCheck(param);
	    }
	    
	    
	    @ApiOperation("格点月统计")
	    @PostMapping("checkGribDataMonth")
	    public void checkGribDataMonth(@RequestBody CheckDataParams param)
	    {
	    	List<CheckDataIndbEntity> list = checkDataService.dealGribMonthCheck(param);
	    }
}
