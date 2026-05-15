package com.station.controller;

import io.swagger.annotations.ApiOperation;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.annotation.Resource;
import javax.transaction.Transactional;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.hutool.core.io.FileUtil;

import com.alibaba.fastjson2.JSONObject;
import com.constants.DataTypeEnum;
import com.log.pojo.LogRecordParams;
import com.log.service.inf.LogService;
import com.station.cut.StartCutDataThread;
import com.station.extract.AddWzdJob;
import com.station.extract.StartExtractDataThread;
import com.station.extract.StartForecastIndbThread;
import com.station.pojo.City;
import com.station.pojo.Cnty;
import com.station.pojo.CutDataParam;
import com.station.pojo.ForecastDataParam;
import com.station.pojo.Province;
import com.station.pojo.StationEntity;
import com.station.pojo.StationInfoEntity;
import com.station.pojo.StationInfoParams;
import com.station.pojo.TaskListEntity;
import com.station.pojo.TaskParam;
import com.station.pojo.TaskStationEntity;
import com.station.service.inf.StationInfoService;
import com.tool.PageResult;
import com.util.ReadPropertiesUtil;
import com.util.ThreadPoolUtil;
import com.util.TimeUtil;

/**
 * @category
 * @date 2025/3/14 15:24
 * @description TODO
 */
@RestController
@RequestMapping("/station/")
public class StationInfoController {
	private ExecutorService executor = ThreadPoolUtil.getInstance();
    @Resource
    private StationInfoService stationInfoService;
    
    @Resource
    private LogService logService;

    @ApiOperation("查询已建模型的站点")
    @PostMapping("queryStationInfo")
    public List<StationInfoEntity> queryStationInfo(@RequestBody StationInfoParams params)
    {
        return stationInfoService.queryStationInfo(params);
    }

    @ApiOperation("查询已建模型的站点")
    @PostMapping("queryStationInfoShow")
    public List<StationInfoEntity> queryStationInfoShow(@RequestBody StationInfoParams params)
    {
        return stationInfoService.queryStationInfoShow(params);
    }
    @ApiOperation("查询一万站点数据接口")
    @PostMapping("queryStationInfoZd")
    public List<StationInfoEntity> queryStationInfoZd(@RequestBody StationInfoParams params)
    {
        return stationInfoService.queryStationInfoZd(params);
    }

    @ApiOperation("查询没有创建模型的站点  2万站点")
    @PostMapping("queryStationInfoNotModel")
    public List<StationInfoEntity> queryStationInfoNotModel(@RequestBody StationInfoParams params)
    {
        return stationInfoService.queryStationInfoNotModel(params);
    }

    @GetMapping("queryAllStations")
    public List<StationInfoEntity> queryAllStations()
    {
        return stationInfoService.queryAllStations();
    }

    @GetMapping("queryAllStationsFlag")
    public List<StationInfoEntity> queryAllStationsFlag()
    {
        return stationInfoService.queryAllStationsFlag();
    }

    @GetMapping("queryAllStationsZj")
    public List<StationInfoEntity> queryAllStationsZj()
    {
        return stationInfoService.queryAllStationsZj();
    }
    
    @GetMapping("queryForcastStations")
    public List<StationInfoEntity> queryForcastStations()
    {
    	return stationInfoService.queryForcastStations();
    }

    @ApiOperation("查询没有创建模型的站点接口")
    @GetMapping("queryAllStationsNotModel")
    public List<StationInfoEntity> queryAllStationsNotModel()
    {
        return stationInfoService.queryAllStationsNotModel();
    }

    @ApiOperation("添加无站点测试接口")
    @PostMapping("extractDataTest")
    public void extractDataTest()
    {
    	String[] fileDirs = new String[1];
		String[] dataTypes = new String[1];
		fileDirs[0] = "/cloud/nas/rsync/ecmwf_cut/";
		dataTypes[0] = DataTypeEnum.ECMF.getDataType();
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.MONTH, -1);
		
		Map<String, String> sufMap = new HashMap<>();
		sufMap.put(dataTypes[0], "grib");
		Date startDate = calendar.getTime();
        Date endDate = new Date();
        List<String> dateList = new ArrayList<>();
        for(int i = 0;; i++)
        {
            calendar.setTime(startDate);
            calendar.add(Calendar.DATE, i);
            if(calendar.getTime().after(endDate))
            {
                break;
            }
            dateList.add(TimeUtil.date2String(calendar.getTime(), TimeUtil.DATE_FMT_YMD));
        }
        
        
        Map<String, List<String>> filePathMapList = new HashMap<>();
        for(String dataType : dataTypes)
        {
        	filePathMapList.put(dataType, new ArrayList<>());
        }
        for(int i = 0, num = fileDirs.length; i < num; i++)
        {
        	for(String date : dateList)
			{
        		File file = new File(fileDirs[i] + File.separator + date);
        		String[] ll = file.list();
        		if(!file.exists() || ll == null || ll.length == 0)
        		{
        			continue;
        		}
        		String suffix = sufMap.get(dataTypes[i]);
        		File[] list = file.listFiles(new FilenameFilter() {
					
					@Override
					public boolean accept(File dir, String name) {
//						System.out.println("dir: " + dir + " name: " + name);
						if(name == null)
						{
							return false;
						}
						String lower = name.toLowerCase();
						
						return lower.endsWith(suffix);
					}
				});
        		if(list == null || list.length == 0)
        		{
        			System.out.println(dataTypes[i] + ":没符合条件的历史数据");
        			continue;
        		}
        		for(File str : list)
        		{
        			filePathMapList.get(dataTypes[i]).add(str.getAbsolutePath());
        		}
			}
        }
        
        Map<String, double[]> stationInfo = new HashMap<>();
        stationInfo.put("999999", new double[]{88.9850, 38.7517});
        System.out.println("开始抽取测试历史数据......");
        StartExtractDataThread extractData = new StartExtractDataThread(stationInfo, filePathMapList, calendar.getTime(), new Date());
        executor.execute(extractData);
    }
    
    @ApiOperation("添加无站点接口")
    @PostMapping("addZjStation")
    public int addZjStation(@RequestBody StationInfoEntity stationInfoEntity)
    {
        int result = stationInfoService.addZjStation(stationInfoEntity);
        if(result == 10)
        {
        	return -10;
        }
        
        //TODO 添加无站点后开始抽取新建站点的历史数据
        Map<String, double[]> stationInfo = new HashMap<>();
        stationInfo.put(stationInfoEntity.getStationIdC(), new double[]{stationInfoEntity.getLon(), stationInfoEntity.getLat()});
        
        executor.execute(new AddWzdJob(stationInfo));
        
        LogRecordParams logRecordParams = new LogRecordParams("模型管理", "添加无站点");
        logService.addLogRecord(logRecordParams);
        
        return result;
    }

    @ApiOperation("添加已有站点")
    @PostMapping("addStations")
    public int addStations(@RequestBody StationInfoParams stationInfoParams)
    {
    	int result = stationInfoService.addStations(stationInfoParams, 1);
    	
    	LogRecordParams logRecordParams = new LogRecordParams("模型管理", "添加已有站点");
        logService.addLogRecord(logRecordParams);
    	
        return result;
    }

    @ApiOperation("查询站点名称是否已经存在")
    @PostMapping("queryZjStationByStationName")
    public int queryZjStationByStationName(@RequestBody StationInfoParams stationInfoParams)
    {
        StationInfoEntity data = new StationInfoEntity();
        data.setStationName(stationInfoParams.getStationName());
        StationInfoEntity stationInfoEntity = stationInfoService.queryZjStationByStationName(data);

        return stationInfoEntity == null ? 0 : 1;
    }
    
    @ApiOperation("预报结果数据入库接口")
    @PostMapping("indb/forecastDataIndb")
    public void forecastDataIndb(@RequestBody ForecastDataParam param)
    {
//    	System.out.println("=====================python=======================" + " params:" + JSONObject.toJSONString(param));
//    	FileUtil.appendString("python调用入库接口参数 params:" + TimeUtil.date2String(new Date(), TimeUtil.DEFAULT_DATETIME_FORMAT) + " " + JSONObject.toJSONString(param), "/home/log.txt\\r\\n", "utf-8");
//    	FileUtil.w
    	List<String> list = new ArrayList<>(1);
		list.add("python调用入库接口参数 params:" + TimeUtil.date2String(new Date(), TimeUtil.DEFAULT_DATETIME_FORMAT) + " " + JSONObject.toJSONString(param));
    	FileUtil.writeLines(list, "/home/log.txt", "utf-8", true);
    	StartForecastIndbThread forecastIndbThread = new StartForecastIndbThread(param.getDataType(), param.getDataTime());
    	executor.execute(forecastIndbThread);
    }
    
    @ApiOperation("预处理历史数据接口")
    @PostMapping("cut/cutHisData")
    public void cutHisData(@RequestBody CutDataParam param)
    {
    	List<String> list = new ArrayList<>(1);
		list.add("python调用预处理历史数据接口参数 params:" + TimeUtil.date2String(new Date(), TimeUtil.DEFAULT_DATETIME_FORMAT) + " " + JSONObject.toJSONString(param));
    	FileUtil.writeLines(list, "/home/log.txt", "utf-8", true);
    	StartCutDataThread startCutDataThread = new StartCutDataThread(param.getDataType(), param.getDate());
    	executor.execute(startCutDataThread);
    }
    
    @ApiOperation("添加任务列表接口")
    @PostMapping("task/addTaskList")
    @Transactional
    public int addTaskList(@RequestBody TaskListEntity task)
    {
    	int taskName = stationInfoService.queryTaskName(task);
    	if(taskName > 0)
    	{
    		return -1;
    	}
    	
    	int result = stationInfoService.addTaskList(task);
    	
    	
    	LogRecordParams logRecordParams = new LogRecordParams("分组管理", "添加任务列表");
        logService.addLogRecord(logRecordParams);
    	
    	return result;
    }
    
    @ApiOperation("修改任务列表接口")
    @PostMapping("task/modifyTaskList")
    public int modifyTaskList(@RequestBody TaskListEntity task)
    {
    	int result = stationInfoService.modifyTaskList(task);
    	
    	return result;
    }
    
    @ApiOperation("修改任务名称接口")
    @PostMapping("task/modifyTaskName")
    public int modifyTaskName(@RequestBody TaskListEntity param)
    {
    	int result = stationInfoService.updateTaskName(param);
    	
    	return result;
    }
    
    @ApiOperation("查询任务名称是否已存在接口")
    @PostMapping("task/queryTaskName")
    public int queryTaskName(@RequestBody TaskListEntity task)
    {
    	int result = stationInfoService.queryTaskName(task);
    	
    	return result;
    }
    
    @ApiOperation("查询任务列表接口")
    @PostMapping("task/queryTaskList")
    public List<TaskListEntity> queryTaskList()
    {
    	List<TaskListEntity> result = stationInfoService.queryTaskList();
    	LogRecordParams logRecordParams = new LogRecordParams("分组管理", "查询任务列表");
        logService.addLogRecord(logRecordParams);
    	
    	return result;
    }
    
    @ApiOperation("查询任务列表和任务包含的站点接口")
    @PostMapping("task/queryTaskListAndStations")
//    public List<TaskListEntity> queryTaskListAndStations()
    public PageResult queryTaskListAndStations(@RequestBody TaskListEntity task)
    {
    	PageResult pageResult = null;
    	
    	pageResult = stationInfoService.queryTaskListByPage(task);
    	
//    	List<TaskListEntity> result = stationInfoService.queryTaskList();
//    	for(TaskListEntity data : result)
//    	{
//    		data.setStations(stationInfoService.queryTaskStations(data));
//    	}
    	
    	
    	
    	LogRecordParams logRecordParams = new LogRecordParams("分组管理", "查询任务列表");
        logService.addLogRecord(logRecordParams);
    	
    	return pageResult;
    }
    
    @ApiOperation("查询单个任务的站点列表接口")
    @PostMapping("task/queryTaskStations")
    public List<TaskStationEntity> queryTaskStations(@RequestBody TaskParam param)
    {
    	TaskListEntity data = new TaskListEntity();
    	data.setId(param.getTaskId());
    	List<TaskStationEntity> result = stationInfoService.queryTaskStations(data);
    	for(TaskStationEntity station : result)
    	{
    		if(station.getStationName() == null)
    		{
    			station.setStationName("");
    		}
    	}
    	LogRecordParams logRecordParams = new LogRecordParams(param.getAuthor(), "分组管理", "查询单个任务的站点列表");
        logService.addLogRecord(logRecordParams);
    	
    	return result;
    }
    
    @ApiOperation("添加任务站点接口")
    @PostMapping("task/addTaskStation")
    @Transactional
    public int addTaskStation(@RequestBody TaskParam param)
    {
    	List<StationInfoEntity> stationList = param.getStationList();
    	TaskStationEntity data = null;
    	int total = 0;
    	String inserttime = TimeUtil.date2String(new Date(), TimeUtil.DEFAULT_DATETIME_FORMAT);
    	for(StationInfoEntity station : stationList)
    	{
    		data = new TaskStationEntity();
    		data.setTaskId(param.getTaskId());
    		data.setStation(station.getStationIdD() + "");
    		data.setStationName(station.getStationName());
    		data.setLon(station.getLon());
    		data.setLat(station.getLat());
    		data.setFlag(station.getFlag());
    		data.setInserttime(inserttime);
    		int addTaskStation = stationInfoService.addTaskStation(data);
    		total += addTaskStation;
    	}
    	
    	int result = 0;
    	if(total == stationList.size())
    	{
    		result = 1;
    		LogRecordParams logRecordParams = new LogRecordParams("分组管理", "添加任务站点");
            logService.addLogRecord(logRecordParams);
    	}
    	
    	return result;
    }
    
    @ApiOperation("删除任务站点接口")
    @PostMapping("task/deleteTaskStation")
    @Transactional
    public int deleteTaskStation(@RequestBody TaskParam param)
    {
    	String[] stations = param.getStations();
    	TaskStationEntity data = null;
    	int total = 0;
    	for(String station : stations)
    	{
    		data = new TaskStationEntity();
    		data.setTaskId(param.getTaskId());
    		data.setStation(station);
    		int i = stationInfoService.deleteTaskStation(data);
    		total += i;
    	}
    	int result = 0;
    	if(total == stations.length)
    	{
    		result = 1;
    		LogRecordParams logRecordParams = new LogRecordParams("分组管理", "删除任务站点");
            logService.addLogRecord(logRecordParams);
    	}
    	
    	return result;
    }

    @ApiOperation("删除任务接口")
    @PostMapping("task/deleteTask")
    @Transactional
    public int deleteTask(@RequestBody TaskParam param)
    {
    	int result = 0;
    	
    	TaskListEntity taskList = new TaskListEntity();
    	taskList.setId(param.getTaskId());
    	int i = stationInfoService.deleteTask(taskList);
    	int j = stationInfoService.deleteTaskStationsByTaskId(taskList);
    	if(i > 0 && j > 0)
    	{
    		result = 1;
    		LogRecordParams logRecordParams = new LogRecordParams("分组管理", "删除任务");
            logService.addLogRecord(logRecordParams);
    	}
    	
    	return result;
    }

    @GetMapping("queryProvince")
    public List<Province> queryProvince()
    {
        return stationInfoService.queryProvince();
    }
    @PostMapping("queryCity")
    public List<City> queryCity(@RequestBody Province province)
    {
        return stationInfoService.queryCityById(province);
    }
    @PostMapping("queryCnty")
    public List<Cnty> queryCnty(@RequestBody City city)
    {
        return stationInfoService.queryCntyById(city);
    }
    @PostMapping("queryStations")
    public List<StationEntity> queryStations(@RequestBody Cnty cnty)
    {
        return stationInfoService.queryStations(cnty);
    }
}
