package com.model.service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.model.dao.ModelInfoMapper;
import com.model.pojo.GribModelManagerDetailEntity;
import com.model.pojo.ModelInfoEntity;
import com.model.pojo.ModelInfoParams;
import com.model.pojo.ModelManagerDetailEntity;
import com.model.pojo.ModelManagerEntity;
import com.model.pojo.ModelManagerParams;
import com.model.service.inf.ModelInfoService;
import com.station.pojo.StationInfoEntity;
import com.tool.PageBuilder;
import com.tool.PageResult;
import com.util.TimeUtil;

/**
 * @category
 * @date 2025/3/14 17:12
 * @description TODO
 */
@Service
public class ModelInfoServiceImpl implements ModelInfoService {
    @Resource
    private ModelInfoMapper modelInfoMapper;

    @Override
    public List<ModelInfoEntity> queryModelInfoLike(ModelInfoParams params) {
        return modelInfoMapper.queryModelInfoLike(params);
    }

    @Override
    public PageResult queryModelInfo(ModelInfoParams params) {
        PageResult pageResult = new PageResult();
        IPage<ModelInfoEntity> result = modelInfoMapper.queryModelInfo(PageBuilder.build(params.getPageNum(), params.getPageSize()), params);
        pageResult.setPage(params.getPageNum());
        pageResult.setPageSize(params.getPageSize());
        pageResult.setTotal(result.getTotal());
        pageResult.setData(result.getRecords());

        return pageResult;
    }

    @Override
    public List<ModelManagerEntity> queryModelManagerList()
    {
        List<ModelManagerEntity> result = modelInfoMapper.queryModelManagerList();
        ModelManagerParams params = new ModelManagerParams();
        for(ModelManagerEntity data : result)
        {
            params.setId(data.getId());
            List<ModelManagerEntity> modelManagerEntities = modelInfoMapper.queryModelReplaceStatus(params);
            List<ModelManagerEntity> builders = modelInfoMapper.queryModelBuilderStatus(params);
            int hasReplacing = 1;//没有正在替换的模型
            if(modelManagerEntities != null && modelManagerEntities.size() > 0)
            {
                hasReplacing = 0;
            }
            if(builders != null && builders.size() > 0)
            {
            	hasReplacing = 0;
            }
            if(data.getId() == 1 || data.getId() == 3)
            {
                data.setHasReplacing(hasReplacing);
            }
            else
            {
                data.setHasReplacing(1);
            }
            if(data.getId() == 1 || data.getId() == 2 || data.getId() == 3 || data.getId() == 6)
            {
                data.setDetail(1);
            }
            else
            {
                data.setDetail(0);
            }
            if(data.getId() <= 3)
            {
                data.setUpgrade(1);
            }
            else
            {
                data.setUpgrade(0);
            }
        }

        return result;
    }

    @Override
    public int updateModelStatus(ModelManagerParams params) {
        ModelManagerEntity modelManagerEntity = new ModelManagerEntity();
        modelManagerEntity.setId(params.getId());
        modelManagerEntity.setStatus(params.getStatus());
        modelManagerEntity.setAuthor(params.getAuthor());
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.HOUR_OF_DAY, 8);
        modelManagerEntity.setLatestUpdateTime(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
        calendar.setTime(new Date());
//        ModelManagerEntity entity = modelInfoMapper.queryModelByModelId(params);
        calendar.add(Calendar.DAY_OF_MONTH, params.getFixRateDay());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        
        modelManagerEntity.setNextUpdateTime(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));

        return modelInfoMapper.updateModelStatus(modelManagerEntity);
    }

    @Override
    public ModelManagerEntity queryModelUpgradeStatusById(ModelManagerParams params) {

        return modelInfoMapper.queryModelUpgradeStatusById(params);
    }
    
    @Override
    public List<ModelManagerDetailEntity> queryModelManagerDetailById(ModelManagerParams params)
    {
    	List<ModelManagerDetailEntity> list = null;
        if(params.getStationName() == null && params.getId() < 5)
        {
        	list = modelInfoMapper.queryGribModelManagerDetailById(params);
        	for(ModelManagerDetailEntity data : list)
        	{
        		data.setStationNum("");
        	}
        }
        else
        {
        	list = modelInfoMapper.queryModelManagerDetailById(params);
        }
    	
    	
    	return list;
    }

    @Override
    public PageResult queryModelManagerListDetail(ModelManagerParams params) {
        PageResult pageResult = new PageResult();
        IPage<ModelManagerDetailEntity> result = modelInfoMapper.queryModelManagerListDetail(PageBuilder.build(params.getPageNum(), params.getPageSize()), params);

        for(ModelManagerDetailEntity data : result.getRecords())
        {
        	data.setStationName(data.getStationName() == null ? "" : data.getStationName());
        	data.setReplaceflag(1);
        }
        
        pageResult.setPage(params.getPageNum());
        pageResult.setPageSize(params.getPageSize());
        pageResult.setTotal(result.getTotal());
        pageResult.setData(result.getRecords());

        return pageResult;
    }
    
    @Override
    public PageResult queryGribModelManagerListDetail(ModelManagerParams params) {
    	PageResult pageResult = new PageResult();
    	IPage<GribModelManagerDetailEntity> result = modelInfoMapper.queryGribModelManagerListDetail(PageBuilder.build(params.getPageNum(), params.getPageSize()), params);
    	List<GribModelManagerDetailEntity> list = result.getRecords();
    	for(GribModelManagerDetailEntity gd : list)
    	{
    		gd.setReplaceflag(0);
    	}
    	pageResult.setPage(params.getPageNum());
    	pageResult.setPageSize(params.getPageSize());
    	pageResult.setTotal(result.getTotal());
    	pageResult.setData(result.getRecords());
    	
    	return pageResult;
    }

    @Override
    public List<ModelManagerDetailEntity> queryModelReplaceList(ModelManagerParams params) {
        List<ModelManagerDetailEntity> result = modelInfoMapper.queryModelReplaceList(params);

        return result;
    }
    
	@Override
	public List<ModelManagerDetailEntity> queryGribModelReplaceList(ModelManagerParams params) {
		List<ModelManagerDetailEntity> result = modelInfoMapper.queryGribModelReplaceList(params);
		
		return result;
	}

    @Override
    public List<ModelManagerDetailEntity> queryModelDetailListLike(ModelManagerParams params) {
        String regex = "^\\d+$";
        Pattern datePattern = Pattern.compile(regex);
        Matcher matcher = datePattern.matcher(params.getStationName());
        String stationName = params.getStationName();
        while (matcher.find()) {
//            params.setStationNum(Integer.parseInt(matcher.group()));
            params.setStationNum(matcher.group());
            params.setStationName(null);
        }

        List<ModelManagerDetailEntity> list = null;
        if(params.getId() == 2)
        {
        	list = modelInfoMapper.queryGribModelDetailListLike(params);
        	for(ModelManagerDetailEntity data : list)
        	{
        		data.setStationNum("");
        	}
        }
        else
        {
        	list = modelInfoMapper.queryModelDetailListLike(params);
        }
        
        if(list == null || list.size() == 0)
        {
            params.setStationNum(null);
            params.setStationName(stationName);
            list = modelInfoMapper.queryModelDetailListLike(params);
        }

        return list;
    }

    @Transactional
    @Override
    public int updateStationModelEnabled(ModelManagerParams params) {
        int managerId = params.getManagerId();
        if(managerId == 1)
        {
            params.setTableName("public.station_info_tab");
        }
        else if(managerId == 3)
        {
            params.setTableName("public.station_info_tab_zj");
        }
        else if(managerId == 6)
        {
        	params.setTableName("public.station_info_tab_upload");
        }
        int count = modelInfoMapper.updateStationModelEnabledModelDetailTabById(params);
        int result = 0;
        if(managerId == 6)
        {
        	result = modelInfoMapper.updateStationModelEnabledInt(params);
        }
        else
        {
        	result = modelInfoMapper.updateStationModelEnabled(params);
        }
        if(managerId == 1)
        {
        	StationInfoEntity station = new StationInfoEntity();
        	station.setTableName("public.station_info_tab");
//        	station.setStationIdD(params.getStationInt());
        	station.setStationNumStr(params.getStationNum());
        	StationInfoEntity stationInfo = modelInfoMapper.queryStationInfo(station);
        	stationInfo.setTransName("public.station_info_tab_all");
        	stationInfo.setEnabled(0);
        	int allEnabled = modelInfoMapper.updateStationInfoTrans(stationInfo);
        	
//        	int allEnabled = modelInfoMapper.updateStationInfoAllEnabled(params);
        	if(count == 1 && result == 1 && allEnabled == 1)
        	{
        		result = 1;
        	}
        	else
        	{
        		result = 0;
        	}
        }
        else
        {
        	if(count == 1 && result == 1)
        	{
        		result = 1;
        	}
        	else
        	{
        		result = 0;
        	}
        }

        return result;
    }

    @Override
    public int updateModelReplaceStatus(ModelManagerParams params) {
        int result = modelInfoMapper.updateModelReplaceStatus(params);

        return result;
    }

    @Override
    public int updateModelReplaceStatusChange(ModelManagerParams params) {

        return modelInfoMapper.updateModelReplaceStatusChange(params);
    }
    
    @Override
    public int updateGribModelReplaceStatus(ModelManagerParams params) {
    	int result = modelInfoMapper.updateGribModelReplaceStatus(params);
    	
    	return result;
    }
    
    @Override
    public int updateGribModelReplaceStatusChange(ModelManagerParams params) {
    	
    	return modelInfoMapper.updateGribModelReplaceStatusChange(params);
    }

    @Override
    public boolean deleteModelById(ModelInfoParams params) {
        return modelInfoMapper.deleteModelById(params);
    }

    @Override
    public boolean addModelInfo(ModelInfoEntity params) {
        if(params.getCreateTime() == null || params.getCreateTime().length() == 0)
        {
            params.setCreateTime(TimeUtil.date2String(new Date(), "yyyy-MM-dd HH:mm:ss"));
        }
        if(params.getModelType().equals("nil"))
        {
            params.setParams(params.getParams() + "," + params.getLon() + "," + params.getLat());
        }

        return modelInfoMapper.addModelInfo(params);
    }

    @Override
    public List<ModelInfoEntity> queryUseableModelList(ModelInfoParams params) {
        ModelInfoEntity modelInfo = modelInfoMapper.queryModelInfoById(params);
        List<ModelInfoEntity> modelInfoEntities = modelInfoMapper.queryUseableModelList(modelInfo);

        return modelInfoEntities;
    }

	@Override
	public ModelManagerEntity queryGribUsedModel() {
		
		return modelInfoMapper.queryGribUsedModel();
	}

	@Override
	public int updateGribModelEnabledModelDetailTabById(ModelManagerParams params) {
		
		return modelInfoMapper.updateGribModelEnabledModelDetailTabById(params);
	}

	@Override
	public List<ModelManagerDetailEntity> queryGribModelAll() {
		
		return modelInfoMapper.queryGribModelAll();
	}

}
