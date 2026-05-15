package com.station.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.model.pojo.ModelManagerDetailEntity;
import com.station.dao.StationInfoMapper;
import com.station.extract.AddYyzdJob;
import com.station.pojo.City;
import com.station.pojo.Cnty;
import com.station.pojo.Province;
import com.station.pojo.StationEntity;
import com.station.pojo.StationInfoEntity;
import com.station.pojo.StationInfoParams;
import com.station.pojo.TaskListEntity;
import com.station.pojo.TaskStationEntity;
import com.station.service.inf.StationInfoService;
import com.tool.PageBuilder;
import com.tool.PageResult;
import com.util.ThreadPoolUtil;
import com.util.TimeUtil;

/**
 * @category
 * @date 2025/3/14 15:21
 * @description TODO
 */
@Service
public class StationInfoServiceImpl implements StationInfoService {
    @Resource
    private StationInfoMapper stationInfoMapper;
    private ExecutorService executor = ThreadPoolUtil.getInstance();
    
    @Override
    public List<StationInfoEntity> queryStationInfo(StationInfoParams params) {
        String regex = "^\\d+$";
        Pattern datePattern = Pattern.compile(regex);
        Matcher matcher = datePattern.matcher(params.getStationName());
        String stationName = params.getStationName();
        while (matcher.find()) {
            params.setStationCode(matcher.group());
            params.setStationName(null);
        }
        List<StationInfoEntity> result = new ArrayList<>();
        List<StationInfoEntity> stationInfoEntities = stationInfoMapper.queryStationInfo(params);
        result.addAll(stationInfoEntities);
        if(result == null || result.size() == 0)
        {
            params.setStationCode(null);
            params.setStationName(stationName);
            stationInfoEntities = stationInfoMapper.queryStationInfo(params);
            result.addAll(stationInfoEntities);
        }

        return result;
    }
    @Override
    public List<StationInfoEntity> queryStationInfoShow(StationInfoParams params) {
        String regex = "^\\d+$";
        Pattern datePattern = Pattern.compile(regex);
        Matcher matcher = datePattern.matcher(params.getStationName());
        String stationName = params.getStationName();
        while (matcher.find()) {
            params.setStationCode(matcher.group());
            params.setStationName(null);
        }
        List<StationInfoEntity> result = new ArrayList<>();
        List<StationInfoEntity> stationInfoEntities = stationInfoMapper.queryStationInfo(params);
        List<StationInfoEntity> stationInfoEntitiesZj = stationInfoMapper.queryStationInfoZj(params);
        List<StationInfoEntity> stationInfoEntitiesUpload = stationInfoMapper.queryStationInfoUpload(params);
        for(StationInfoEntity data : stationInfoEntities)
        {
        	data.setFlag(0);
        }
        for(StationInfoEntity data : stationInfoEntitiesZj)
        {
        	data.setFlag(1);
        }
        for(StationInfoEntity data : stationInfoEntitiesUpload)
        {
        	data.setFlag(2);
        }
        result.addAll(stationInfoEntities);
        result.addAll(stationInfoEntitiesZj);
        result.addAll(stationInfoEntitiesUpload);
        if(result == null || result.size() == 0)
        {
            params.setStationCode(null);
            params.setStationName(stationName);
            stationInfoEntities = stationInfoMapper.queryStationInfo(params);
            stationInfoEntitiesZj = stationInfoMapper.queryStationInfoZj(params);
            stationInfoEntitiesUpload = stationInfoMapper.queryStationInfoUpload(params);
            result.addAll(stationInfoEntities);
            result.addAll(stationInfoEntitiesZj);
            result.addAll(stationInfoEntitiesUpload);
        }

        return result;
    }

    @Override
    public List<StationInfoEntity> queryStationInfoZd(StationInfoParams params) {
        String regex = "^\\d+$";
        Pattern datePattern = Pattern.compile(regex);
        Matcher matcher = datePattern.matcher(params.getStationName());
        String stationName = params.getStationName();
        while (matcher.find()) {
            params.setStationCode(matcher.group());
            params.setStationName(null);
        }
        List<StationInfoEntity> result = new ArrayList<>();
        List<StationInfoEntity> stationInfoEntities = stationInfoMapper.queryStationInfoZd(params);
        result.addAll(stationInfoEntities);
        if(result == null || result.size() == 0)
        {
            params.setStationCode(null);
            params.setStationName(stationName);
            stationInfoEntities = stationInfoMapper.queryStationInfoZd(params);
            result.addAll(stationInfoEntities);
        }

        return result;
    }

    @Override
    public List<StationInfoEntity> queryStationInfoNotModel(StationInfoParams params) {
        String regex = "^\\d+$";
        Pattern datePattern = Pattern.compile(regex);
        Matcher matcher = datePattern.matcher(params.getStationName());
        String stationName = params.getStationName();
        while (matcher.find()) {
            params.setStationCode(matcher.group());
            params.setStationName(null);
        }

        List<StationInfoEntity> stationInfoEntities = stationInfoMapper.queryStationInfoNotModel(params);
        if(stationInfoEntities.size() == 0)
        {
            params.setStationCode(null);
            params.setStationName(stationName);
            stationInfoEntities = stationInfoMapper.queryStationInfoNotModel(params);
        }

        return stationInfoEntities;
    }

    @Override
    public List<StationInfoEntity> queryAllStations() {
        List<StationInfoEntity> result = stationInfoMapper.queryAllStations();

        return result;
    }
    @Override
    public List<StationInfoEntity> queryAllStationsFlag() {
        List<StationInfoEntity> result = new ArrayList<>();
        List<StationInfoEntity> stationInfoEntities = stationInfoMapper.queryAllStations();
        for(StationInfoEntity data : stationInfoEntities)
        {
            data.setFlag(0);//已有站点 
        }
        List<StationInfoEntity> stationInfoEntities1 = stationInfoMapper.queryAllStationsZj();
        for(StationInfoEntity data : stationInfoEntities1)
        {
            data.setFlag(1);//无站点 
        }
        List<StationInfoEntity> stationInfoEntities2 = stationInfoMapper.queryAllStationsUpload();
        for(StationInfoEntity data : stationInfoEntities2)
        {
        	data.setFlag(2);//自建站
        }
        result.addAll(stationInfoEntities);
        result.addAll(stationInfoEntities1);
        result.addAll(stationInfoEntities2);

        return result;
    }
    @Override
    public List<StationInfoEntity> queryAllStationsZj() {

        return stationInfoMapper.queryAllStationsZj();
    }

    @Override
    public List<StationInfoEntity> queryAllStationsNotModel() {

        return stationInfoMapper.queryAllStationsNotModel();
    }

    @Override
    public StationInfoEntity queryZjStationByStationName(StationInfoEntity params) {

        return stationInfoMapper.queryZjStationByStationName(params);
    }


    @Override
    public int addZjStation(StationInfoEntity stationInfo) {
    	StationInfoEntity queryStationByLonLat = stationInfoMapper.queryStationByLonLat(stationInfo);
    	if(queryStationByLonLat != null)
    	{
    		return 10;
    	}
        StationInfoEntity stationInfoEntity = stationInfoMapper.queryMaxStationNum();
        int stationNum = (stationInfoEntity.getMaxNum() + 1);
        stationInfo.setStationIdC(stationNum + "");
        stationInfo.setStationIdD(stationNum);
        stationInfo.setEnabled(0);

        int count = stationInfoMapper.addZjStation(stationInfo);

        ModelManagerDetailEntity data = new ModelManagerDetailEntity();
//        StationInfoEntity station = stationInfoMapper.queryZjStationByStationName(stationInfo);

        data.setStationNum(stationNum + "");
        data.setStationName(stationInfo.getStationName());
        data.setLon(stationInfo.getLon());
        data.setLat(stationInfo.getLat());
//        data.setAuthor(stationInfo.getAuthor());
        data.setStatus(1);
        data.setManagerId(3);
        data.setUsed(1);
        data.setEnabled(0);
        
        int addWzdModel = addWzdModel(stationInfo);

        int result = 0;
        if(count > 0 && addWzdModel > 0)
        {
            result = 1;
        }

        return result;
    }

    @Override
    public int addWzdModel(StationInfoEntity stationInfo) {
        ModelManagerDetailEntity data = new ModelManagerDetailEntity();
//        StationInfoEntity station = stationInfoMapper.queryZjStationByStationName(stationInfo);
        data.setStationNum(stationInfo.getStationIdD() + "");
        data.setStationName(stationInfo.getStationName());
        data.setLon(stationInfo.getLon());
        data.setLat(stationInfo.getLat());
//        data.setAuthor(stationInfo.getAuthor());
        data.setStatus(2);
        data.setManagerId(3);
        data.setUsed(0);
        data.setEnabled(0);
        data.setModel("通用模型");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.HOUR_OF_DAY, 8);
        data.setInsertTime(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
        int i = stationInfoMapper.addWzdModel(data);

        ModelManagerDetailEntity data1 = new ModelManagerDetailEntity();
        data1.setStationNum(stationInfo.getStationIdD() + "");
        data1.setStationName(stationInfo.getStationName());
        data1.setLon(stationInfo.getLon());
        data1.setLat(stationInfo.getLat());
//        data1.setAuthor(stationInfo.getAuthor());
        data1.setStatus(2);
        data1.setManagerId(3);
        data1.setUsed(1);
        data1.setEnabled(0);
        data1.setModel("强拟合模型");
        data1.setInsertTime(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
        int j = stationInfoMapper.addWzdModel(data1);
        int result = 0;
        if(i > 0 && j > 0)
        {
            result = 1;
        }

        return result;
    }

    
    @Override
    public int addStations(StationInfoParams params, int managerId) {
        List<StationInfoEntity> stationInfoEntities = stationInfoMapper.queryStationsNotModelByNum(params);
        int count = 0;
        
        for(StationInfoEntity data : stationInfoEntities)
        {
            data.setEnabled(0);
            int i = stationInfoMapper.addStation(data);
            int addModel = addModel(data, managerId);
            if(i > 0 && addModel > 0)
            {
                count += i;
            }
        }
        stationInfoMapper.updateStationNotModelStatus(params);
        
        
      //TODO 添加站点后开始抽取站点的历史数据
        Map<String, double[]> stationInfo = new HashMap<>();
        for(StationInfoEntity data : stationInfoEntities)
        {
        	stationInfo.put(data.getStationIdC(), new double[]{data.getLon(), data.getLat()});
        }
        executor.execute(new AddYyzdJob(stationInfo));

        return count;
    }
    
    public int addModel(StationInfoEntity stationInfo, int managerId) {
    	StationInfoEntity queryStationByLonLat = stationInfoMapper.queryStationByLonLat(stationInfo);
    	if(queryStationByLonLat != null)
    	{
    		return 1;
    	}
        ModelManagerDetailEntity data = new ModelManagerDetailEntity();
//        StationInfoEntity station = stationInfoMapper.queryZjStationByStationName(stationInfo);
        data.setStationNum(stationInfo.getStationIdD() + "");
        data.setStationName(stationInfo.getStationName());
        data.setLon(stationInfo.getLon());
        data.setLat(stationInfo.getLat());
        data.setAuthor(stationInfo.getAuthor());
//        data.setStatus(1);
        data.setStatus(2);//建模中
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
//        data1.setStatus(1);
        data.setStatus(2);//建模中
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
    
    @Override
	public List<StationInfoEntity> queryForcastStations() {
		
		return stationInfoMapper.queryForcastStations();
	}
    
    @Override
	public List<TaskListEntity> queryTaskList() {
		List<TaskListEntity> taskList = stationInfoMapper.queryTaskList();
    	
		return taskList;
	}
    
    @Override
	public PageResult queryTaskListByPage(TaskListEntity params) {
    	PageResult pageResult = new PageResult();
    	IPage<TaskListEntity> pageList = stationInfoMapper.queryTaskListAndStationsByPage(PageBuilder.build(params.getPageNum(), params.getPageSize()), params);
    	StationInfoEntity station = null;
    	for(TaskListEntity data : pageList.getRecords())
    	{
    		List<TaskStationEntity> list = stationInfoMapper.queryTaskStations(data);
    		List<StationInfoEntity> ss = new ArrayList<>();
    		for(TaskStationEntity s : list)
    		{
    			station = new StationInfoEntity();
    			station.setLat(s.getLat());
    			station.setLon(s.getLon());
    			station.setStationName(s.getStationName());
    			station.setStationIdD(Integer.parseInt(s.getStation()));
    			ss.add(station);
    			
    		}
    		data.setStationList(ss);
    	}
    	pageResult.setPage(params.getPageNum());
    	pageResult.setPageSize(params.getPageSize());
    	pageResult.setTotal(pageList.getTotal());
    	pageResult.setData(pageList.getRecords());
    	
    	
		return pageResult;
	}
    
    @Override
	public int addTaskStation(TaskStationEntity param) {
    	param.setInserttime(TimeUtil.date2String(new Date(), TimeUtil.DEFAULT_DATETIME_FORMAT));
		param.setEnabled(0);
		int result = stationInfoMapper.addTaskStation(param);
		
		return result;
	}
    
	@Override
	public int addTaskList(TaskListEntity param) {
		param.setInserttime(TimeUtil.date2String(new Date(), TimeUtil.DEFAULT_DATETIME_FORMAT));
		param.setEnabled(0);
		int result = 0;
		int taskNameCount = stationInfoMapper.addTaskList(param);
		if(taskNameCount > 0)
		{
			TaskListEntity taskData = stationInfoMapper.queryTaskName(param);
			List<StationInfoEntity> stationList = param.getStationList();
			TaskStationEntity data = null;
			int total = 0;
			String inserttime = TimeUtil.date2String(new Date(), TimeUtil.DEFAULT_DATETIME_FORMAT);
			for(StationInfoEntity station : stationList)
	    	{
	    		data = new TaskStationEntity();
	    		data.setTaskId(taskData.getId());
	    		data.setStation(station.getStationIdD() + "");
	    		data.setStationName(station.getStationName());
	    		data.setLon(station.getLon());
	    		data.setLat(station.getLat());
	    		data.setFlag(station.getFlag());
	    		data.setInserttime(inserttime);
                data.setIndex(station.getIndex());
	    		int addTaskStation = stationInfoMapper.addTaskStation(data);
	    		total += addTaskStation;
	    	}
			if(total == stationList.size())
	    	{
	    		result = 1;
	    	}
		}
		
		
		return result;
	}
	
	@Override
	public int deleteTaskStation(TaskStationEntity param) {
		int result = stationInfoMapper.deleteTaskStation(param);
		
		return result;
	}

	@Override
	public int deleteTask(TaskListEntity param) {
		int result = stationInfoMapper.deleteTask(param);
		
		return result;
	}
	
	@Override
	public int deleteTaskStationsByTaskId(TaskListEntity param) {
		int result = stationInfoMapper.deleteTaskStationsByTaskId(param);
		
		return result;
	}
	
	@Override
	public List<TaskStationEntity> queryTaskStations(TaskListEntity param) {
		List<TaskStationEntity> result = stationInfoMapper.queryTaskStations(param);
		
		return result;
	}
	
	@Override
	public int queryTaskName(TaskListEntity param) {
		TaskListEntity data = stationInfoMapper.queryTaskName(param);
		int result = 0;
		if(data != null)
		{
			result = 1;
		}
		
		return result;
	}
	
	@Override
	public int updateTaskName(TaskListEntity param) {
		int result = stationInfoMapper.updateTaskName(param);
		
		return result;
	}
	
	@Override
	public int modifyTaskList(TaskListEntity param) {
		
		int deleteCount = stationInfoMapper.deleteTaskStationsByTaskId(param);
		int result = 0;
		if(deleteCount >= 0)
		{
			List<StationInfoEntity> stationList = param.getStationList();
			int total = 0;
			String inserttime = TimeUtil.date2String(new Date(), TimeUtil.DEFAULT_DATETIME_FORMAT);
			TaskStationEntity data = null;
			for(StationInfoEntity station : stationList)
	    	{
				data = new TaskStationEntity();
				data.setTaskId(param.getId());
	    		data.setStation(station.getStationIdD() + "");
	    		data.setStationName(station.getStationName());
	    		data.setLon(station.getLon());
	    		data.setLat(station.getLat());
	    		data.setFlag(station.getFlag());
	    		data.setInserttime(inserttime);
                data.setIndex(station.getIndex());
	    		int addTaskStation = stationInfoMapper.addTaskStation(data);
	    		total += addTaskStation;
	    	}
			if(total == stationList.size())
	    	{
	    		result = 1;
	    	}
		}
		int updateTaskName = stationInfoMapper.updateTaskName(param);
		if(result == 1 && updateTaskName > 0)
		{
			result = 1;
		}
		
		
		return result;
	}

    
//    ====================================================分界线==================================================================
    
    @Override
    public List<Province> queryProvince() {
        return stationInfoMapper.queryProvince();
    }

    @Override
    public List<City> queryCityById(Province province) {
        return stationInfoMapper.queryCityById(province);
    }

    @Override
    public List<Cnty> queryCntyById(City city) {
        return stationInfoMapper.queryCntyById(city);
    }

    @Override
    public List<StationEntity> queryStations(Cnty cnty) {

        return stationInfoMapper.queryStations(cnty);
    }
	
	
	
}
