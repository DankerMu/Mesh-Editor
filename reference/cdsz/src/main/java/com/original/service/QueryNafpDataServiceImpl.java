package com.original.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.config.dao.ConfigMapper;
import com.config.pojo.ConfigParams;
import com.config.pojo.DataSourceType;
import com.constants.DataTypeEnum;
import com.original.dao.NafpDataMapper;
import com.original.pojo.DataQcManagerEntity;
import com.original.pojo.DataQcManagerParams;
import com.original.pojo.NafpDataEntity;
import com.original.pojo.NafpDataParams;
import com.original.service.inf.QueryNafpDataService;
import com.tool.PageBuilder;
import com.tool.PageResult;
import com.util.NumberFormatUtil;
import com.util.ReadPropertiesUtil;
import com.util.RunCommandUtil;
import com.util.TimeUtil;

/**
 * @category
 * @date 2025/3/13 11:22
 * @description TODO
 */
@Service
public class QueryNafpDataServiceImpl implements QueryNafpDataService {
    @Resource
    private NafpDataMapper nafpDataMapper;
    @Resource
    private ConfigMapper configMapper;
    private static Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
    private static Map<String, String> dataCountMap = ReadPropertiesUtil.getUserConfigMap("data_count.properties");
    private static Map<String, String> dataTableMap = ReadPropertiesUtil.getUserConfigMap("data_table.properties");
    @Override
    public PageResult queryOriginalData(NafpDataParams params) {

        PageResult apiPageResult = new PageResult();
        IPage<NafpDataEntity> result = queryData(PageBuilder.build(params.getPageNum(), params.getPageSize()),params);
        apiPageResult.setPage(params.getPageNum());
        apiPageResult.setPageSize(params.getPageSize());
        apiPageResult.setTotal(result.getTotal());
        apiPageResult.setData(result.getRecords());

        return apiPageResult;
    }

    @Override
    public PageResult queryNafpDataCount(NafpDataParams params) {
        IPage<DataQcManagerEntity> result = nafpDataMapper.queryNafpDataCount(PageBuilder.build(params.getPageNum(), params.getPageSize()), params);
        PageResult pageResult = new PageResult();
        pageResult.setPage(params.getPageNum());
        pageResult.setPageSize(params.getPageSize());
        pageResult.setTotal(result.getTotal());
        pageResult.setData(result.getRecords());

        return pageResult;
    }

    @Override
    public int deleteNafpDataByDateTimeDataSource(DataQcManagerParams params) {
        int i = nafpDataMapper.deleteNafpDataByDateTimeDataSource(params);
        String dataSource = params.getDataSource();
        String sourcePath = configMap.get(dataSource + "_source");
        String dataBackPath = configMap.get(dataSource + "_back");
        String[] datatimes = params.getDatatimes();
        List<String> deleteFileList = new ArrayList<>();
        for(String datatime : datatimes)
        {
            String[] commands = new String[]{"ls", sourcePath};
            List<String> listT = RunCommandUtil.run(commands);
            for(String filePath : listT)
            {
                if(filePath.contains(datatime))
                {
                    deleteFileList.add(filePath);
                }
            }
        }
        String[] commands = new String[]{"mv", sourcePath, dataBackPath};
        for(String filePath : deleteFileList)
        {
            commands[1] = filePath;
            RunCommandUtil.run(commands);
        }

        return i;
    }

    @Override
    public int updateNafpDataByDateTimeDataSource(DataQcManagerParams params) {
        return nafpDataMapper.updateNafpDataByDateTimeDataSource(params);
    }

    @Override
    public DataQcManagerEntity queryOriginalNafpData(DataQcManagerParams params) {
        return nafpDataMapper.queryOriginalNafpDataTotal(params);
    }


    @Override
    public List<DataQcManagerEntity> queryOriginalNafpDataCount(DataQcManagerParams params)
    {
        ConfigParams configParams = new ConfigParams();
        configParams.setDataType("mde");
        List<DataSourceType> dataSourceTypeList = configMapper.getDataSourceByDataType(configParams);
        
        Map<String, String> map = new HashMap<>();
        for(DataSourceType dataSourceType : dataSourceTypeList)
        {
            map.put(dataSourceType.getDataSource(), dataSourceType.getDataSourceName());
        }
//        params.setTableName(dataTableMap.get(params.getDataSource()));
        params.setTableName(dataTableMap.get(params.getDataSource() + "_datamanager"));

        List<DataQcManagerEntity> result = nafpDataMapper.queryOriginalNafpDataCount(params);
        List<DataQcManagerEntity> resultCldas = nafpDataMapper.queryOriginalNafpDataCountByAllCldas(params);
        List<DataQcManagerEntity> resultCmpa = nafpDataMapper.queryOriginalNafpDataCountByAllCmpa(params);
        Map<String, Integer> cldasMap = new HashMap<>();
        for(DataQcManagerEntity data : resultCldas)
        {
        	cldasMap.put(data.getDataSource() + "_" + data.getDataTime(), data.getArrived());
        }
        Map<String, Integer> cmpaMap = new HashMap<>();
        for(DataQcManagerEntity data : resultCmpa)
        {
        	cmpaMap.put(data.getDataSource() + "_" + data.getDataTime(), data.getArrived());
        }
        int total = Integer.parseInt(dataCountMap.get(params.getDataSource()));
    	for(DataQcManagerEntity data : result)
    	{
    		data.setTotal(total);
    		data.setUnarrived(total - data.getArrived());
    		data.setRate((int)NumberFormatUtil.numFormat(data.getArrived() * 100/ total, 1) + "");
    		data.setDataSource(map.get(params.getDataSource()));
    		String dataType = data.getDataSource().toLowerCase();
            if(dataType.equals(DataTypeEnum.CLDAS.getDataType()) || dataType.equals(DataTypeEnum.CMPA.getDataType()))
            {
            	data.setDataTime(data.getDataTime().split(" ")[0]);
            }
    	}

        return result;
    }
    
    @Override
    public List<DataQcManagerEntity> queryOriginalNafpDataCountAll(DataQcManagerParams params)
    {
    	ConfigParams configParams = new ConfigParams();
        configParams.setDataType("mde");
        List<DataSourceType> dataSourceTypeList = configMapper.getDataSourceByDataType(configParams);
        Map<String, String> map = new HashMap<>();
        for(DataSourceType dataSourceType : dataSourceTypeList)
        {
            map.put(dataSourceType.getDataSource(), dataSourceType.getDataSourceName());
        }
//        params.setTableName(dataTableMap.get(params.getDataSource()));
        params.setTableName(dataTableMap.get(params.getDataSource() + "_datamanager"));

        List<DataQcManagerEntity> result = nafpDataMapper.queryOriginalNafpDataCountByAll(params);
        List<DataQcManagerEntity> resultCldas = nafpDataMapper.queryOriginalNafpDataCountByAllCldas(params);
        List<DataQcManagerEntity> resultCmpa = nafpDataMapper.queryOriginalNafpDataCountByAllCmpa(params);
        Map<String, Integer> cldasMap = new HashMap<>();
        for(DataQcManagerEntity data : resultCldas)
        {
        	cldasMap.put(data.getDataSource() + "_" + data.getDataTime(), data.getArrived());
        }
        Map<String, Integer> cmpaMap = new HashMap<>();
        for(DataQcManagerEntity data : resultCmpa)
        {
        	cmpaMap.put(data.getDataSource() + "_" + data.getDataTime(), data.getArrived());
        }
        for(DataQcManagerEntity data : result)
        {
        	int total = Integer.parseInt(dataCountMap.get(data.getDataSource()));
            data.setTotal(total);
            data.setUnarrived(total - data.getArrived());
            data.setRate((int)NumberFormatUtil.numFormat(data.getArrived() * 100/ total, 1) + "");
            data.setDataSource(map.get(data.getDataSource()));
            String dataType = data.getDataSource().toLowerCase();
            if(dataType.equals(DataTypeEnum.CLDAS.getDataType()))
            {
            	data.setRate((int)NumberFormatUtil.numFormat(cldasMap.get(dataType + "_" + data.getDataTime()) * 100/ total, 1) + "");
            	data.setArrived(cldasMap.get(dataType + "_" + data.getDataTime()));
            	data.setUnarrived(total - cldasMap.get(dataType + "_" + data.getDataTime()));
            	data.setDataTime(data.getDataTime().split(" ")[0]);
            }
            if(dataType.equals(DataTypeEnum.CMPA.getDataType()))
            {
            	data.setRate((int)NumberFormatUtil.numFormat(cmpaMap.get(dataType + "_" + data.getDataTime()) * 100/ total, 1) + "");
            	data.setArrived(cmpaMap.get(dataType + "_" + data.getDataTime()));
            	data.setUnarrived(total - cmpaMap.get(dataType + "_" + data.getDataTime()));
            	data.setDataTime(data.getDataTime().split(" ")[0]);
            }
        }

        return result;
    }

    @Override
    public PageResult queryOriginalNafpDataCountByPage(DataQcManagerParams params) {
        ConfigParams configParams = new ConfigParams();
        configParams.setDataType("mde");
        List<DataSourceType> dataSourceTypeList = configMapper.getDataSourceByDataType(configParams);
        Map<String, String> map = new HashMap<>();
        for(DataSourceType dataSourceType : dataSourceTypeList)
        {
            map.put(dataSourceType.getDataSource(), dataSourceType.getDataSourceName());
        }
        params.setTableName(dataTableMap.get(params.getDataSource()));

        IPage<DataQcManagerEntity> iPage = nafpDataMapper.queryOriginalNafpDataCountByPage(PageBuilder.build(params.getPageNum(), params.getPageSize()), params);
        List<DataQcManagerEntity> iPageCldas = nafpDataMapper.queryOriginalNafpDataCountByAllCldas(params);
        List<DataQcManagerEntity> iPageCmpa = nafpDataMapper.queryOriginalNafpDataCountByAllCmpa(params);
        Map<String, Integer> cldasMap = new HashMap<>();
        for(DataQcManagerEntity data : iPageCldas)
        {
        	cldasMap.put(data.getDataSource() + "_" + data.getDataTime(), data.getArrived());
        }
        Map<String, Integer> cmpaMap = new HashMap<>();
        for(DataQcManagerEntity data : iPageCmpa)
        {
        	cmpaMap.put(data.getDataSource() + "_" + data.getDataTime(), data.getArrived());
        }
        int total = Integer.parseInt(dataCountMap.get(params.getDataSource()));
        for(DataQcManagerEntity data : iPage.getRecords())
        {
            data.setTotal(total);
            data.setUnarrived(total - data.getArrived());
            if(data.getUnarrived() < 0)
            {
            	data.setRate("100");
            	data.setUnarrived(0);
            }
            else
            {
            	data.setRate((int)NumberFormatUtil.numFormat(data.getArrived() * 100/ total, 1) + "");
            }
            data.setDataSource(map.get(data.getDataSource()));
            String dataType = data.getDataSource().toLowerCase();
            if(dataType.equals(DataTypeEnum.CLDAS.getDataType()))
            {
            	data.setRate((int)NumberFormatUtil.numFormat(cldasMap.get(dataType + "_" + data.getDataTime()) * 100/ total, 1) + "");
            	data.setArrived(cldasMap.get(dataType + "_" + data.getDataTime()));
            	data.setUnarrived(total - cldasMap.get(dataType + "_" + data.getDataTime()));
            	data.setDataTime(data.getDataTime().split(" ")[0]);
            }
            if(dataType.equals(DataTypeEnum.CMPA.getDataType()))
            {
            	data.setRate((int)NumberFormatUtil.numFormat(cmpaMap.get(dataType + "_" + data.getDataTime()) * 100/ total, 1) + "");
            	data.setArrived(cmpaMap.get(dataType + "_" + data.getDataTime()));
            	data.setUnarrived(total - cmpaMap.get(dataType + "_" + data.getDataTime()));
            	data.setDataTime(data.getDataTime().split(" ")[0]);
            }
        }
        PageResult pageResult = new PageResult();
        pageResult.setPage(params.getPageNum());
        pageResult.setPageSize(params.getPageSize());
        pageResult.setTotal(iPage.getTotal());
        pageResult.setData(iPage.getRecords());

        return pageResult;
    }
    
    @Override
    public PageResult queryDataManagerCountByPageAll(DataQcManagerParams params) {
    	ConfigParams configParams = new ConfigParams();
        configParams.setDataType("mde");
        List<DataSourceType> dataSourceTypeList = configMapper.getDataSourceByDataType(configParams);
        Map<String, String> map = new LinkedHashMap<>();
        for(DataSourceType dataSourceType : dataSourceTypeList)
        {
            map.put(dataSourceType.getDataSource(), dataSourceType.getDataSourceName());
        }
        
        IPage<DataQcManagerEntity> iPage = nafpDataMapper.queryDataManagerCountByPageAll(PageBuilder.build(params.getPageNum(), params.getPageSize()), params);
        
        for(DataQcManagerEntity data : iPage.getRecords())
        {
        	data.setDataSource(map.get(data.getDataSource()));
        }
        
        PageResult pageResult = new PageResult();
        pageResult.setPage(params.getPageNum());
        pageResult.setPageSize(params.getPageSize());
        pageResult.setTotal(iPage.getTotal());
        pageResult.setData(iPage.getRecords());
        
    	
    	return pageResult;
    }
    
    @Override
	public PageResult queryDataManagerCountByPage(DataQcManagerParams params) {
    	ConfigParams configParams = new ConfigParams();
        configParams.setDataType("mde");
        List<DataSourceType> dataSourceTypeList = configMapper.getDataSourceByDataType(configParams);
        Map<String, String> map = new LinkedHashMap<>();
        for(DataSourceType dataSourceType : dataSourceTypeList)
        {
            map.put(dataSourceType.getDataSource(), dataSourceType.getDataSourceName());
        }
        
        IPage<DataQcManagerEntity> iPage = nafpDataMapper.queryDataManagerCountByPage(PageBuilder.build(params.getPageNum(), params.getPageSize()), params);
        
        for(DataQcManagerEntity data : iPage.getRecords())
        {
        	data.setDataSource(map.get(data.getDataSource()));
        }
        
        PageResult pageResult = new PageResult();
        pageResult.setPage(params.getPageNum());
        pageResult.setPageSize(params.getPageSize());
        pageResult.setTotal(iPage.getTotal());
        pageResult.setData(iPage.getRecords());
        
    	
    	return pageResult;
	}
    
    @Override
    public PageResult queryOriginalNafpDataCountByPageAll(DataQcManagerParams params) {
        ConfigParams configParams = new ConfigParams();
        configParams.setDataType("mde");
        List<DataSourceType> dataSourceTypeList = configMapper.getDataSourceByDataType(configParams);
        Map<String, String> map = new LinkedHashMap<>();
        for(DataSourceType dataSourceType : dataSourceTypeList)
        {
            map.put(dataSourceType.getDataSource(), dataSourceType.getDataSourceName());
        }
//        params.setTableName(dataTableMap.get(params.getDataSource()));

        IPage<DataQcManagerEntity> iPage = nafpDataMapper.queryOriginalNafpDataCountByPageAll(PageBuilder.build(params.getPageNum(), params.getPageSize()), params);
        
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(TimeUtil.String2Date(params.getEndTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        
//        params.setEndTime(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
        String endTime = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT);
        List<DataQcManagerEntity> iPageCldas = nafpDataMapper.queryOriginalNafpDataCountByAllCldas(params);
        List<DataQcManagerEntity> iPageCmpa = nafpDataMapper.queryOriginalNafpDataCountByAllCmpa(params);
        Map<String, Integer> cldasMap = new HashMap<>();
        for(DataQcManagerEntity data : iPageCldas)
        {
        	cldasMap.put(data.getDataSource() + "_" + data.getDataTime(), data.getArrived());
        }
        Map<String, Integer> cmpaMap = new HashMap<>();
        for(DataQcManagerEntity data : iPageCmpa)
        {
        	cmpaMap.put(data.getDataSource() + "_" + data.getDataTime(), data.getArrived());
        }
//        String nowDate = TimeUtil.date2String(new Date(), TimeUtil.DEFAULT_DATE_FORMAT);
//        Map<String, Map<String, DataQcManagerEntity>> mapList = new LinkedHashMap<>();
        List<DataQcManagerEntity> list = new ArrayList<>();
        for(DataQcManagerEntity data : iPage.getRecords())
        {
            int total = Integer.parseInt(dataCountMap.get(data.getDataSource()));
            data.setTotal(total);
            data.setUnarrived(total - data.getArrived());
            data.setRate((int)NumberFormatUtil.numFormat(data.getArrived() * 100/ total, 1) + "");
            data.setDataSource(map.get(data.getDataSource()));
            String dataType = data.getDataSource().toLowerCase();
            if(dataType.equals(DataTypeEnum.CLDAS.getDataType()) && data.getDataTime().compareTo(endTime) < 0)
            {
            	data.setRate((int)NumberFormatUtil.numFormat(cldasMap.get(dataType + "_" + data.getDataTime()) * 100/ total, 1) + "");
            	data.setArrived(cldasMap.get(dataType + "_" + data.getDataTime()));
            	data.setUnarrived(total - cldasMap.get(dataType + "_" + data.getDataTime()));
            	data.setDataTime(data.getDataTime().split(" ")[0]);
//            	list.add(data);
            }
            if(dataType.equals(DataTypeEnum.CMPA.getDataType()) && data.getDataTime().compareTo(endTime) < 0)
            {
            	data.setRate((int)NumberFormatUtil.numFormat(cmpaMap.get(dataType + "_" + data.getDataTime()) * 100/ total, 1) + "");
            	data.setArrived(cmpaMap.get(dataType + "_" + data.getDataTime()));
            	data.setUnarrived(total - cmpaMap.get(dataType + "_" + data.getDataTime()));
            	data.setDataTime(data.getDataTime().split(" ")[0]);
//            	list.add(data);
            }
//            if(!dataType.equals(DataTypeEnum.CLDAS.getDataType()) && !dataType.equals(DataTypeEnum.CMPA.getDataType()))
//            {
//            	list.add(data);
//            }
            if(dataType.equals(DataTypeEnum.CLDAS.getDataType()) && data.getDataTime().compareTo(endTime) > 0)
            {
            	calendar.setTime(new Date());
            	calendar.add(Calendar.HOUR_OF_DAY, 8);
            	data.setTotal(calendar.get(Calendar.HOUR_OF_DAY) * 4 + 32);
            	data.setArrived(cldasMap.get(dataType + "_" + data.getDataTime()));
            	data.setUnarrived(data.getTotal() - data.getArrived());
            	data.setRate((int)NumberFormatUtil.numFormat(data.getArrived() * 100/ data.getTotal(), 1) + "");
            	data.setDataTime(data.getDataTime().split(" ")[0]);
            }
            if(dataType.equals(DataTypeEnum.CMPA.getDataType()) && data.getDataTime().compareTo(endTime) > 0)
            {
            	calendar.setTime(new Date());
            	calendar.add(Calendar.HOUR_OF_DAY, 8);
            	data.setTotal(calendar.get(Calendar.HOUR_OF_DAY) + 8);
            	data.setArrived(cmpaMap.get(dataType + "_" + data.getDataTime()));
            	data.setUnarrived(data.getTotal() - data.getArrived());
            	data.setRate((int)NumberFormatUtil.numFormat(data.getArrived() * 100/ data.getTotal(), 1) + "");
            	data.setDataTime(data.getDataTime().split(" ")[0]);
            }
//            if(!mapList.containsKey(data.getDataSource()))
//            {
//            	mapList.put(data.getDataSource(), new LinkedHashMap<>());
//            }
//            if(!mapList.get(data.getDataSource()).containsKey(data.getDataTime()))
//            {
//            	if((dataType.equals(DataTypeEnum.CLDAS.getDataType()) || dataType.equals(DataTypeEnum.CMPA.getDataType())) && data.getDataTime().equals(nowDate))
//            	{
//            		
//            	}
//            	else
//            	{
//            		mapList.get(data.getDataSource()).put(data.getDataTime(), data);
//            	}
//            }
        }
        
        
        
//        List<DataQcManagerEntity> list = new ArrayList<>();
//        for(DataQcManagerEntity data : iPage.getRecords())
//        {
//        	if((data.getDataSource().toLowerCase().equals(DataTypeEnum.CLDAS.getDataType()) || data.getDataSource().toLowerCase().equals(DataTypeEnum.CMPA.getDataType())) && data.getDataTime().equals(nowDate))
//        	{
//        		continue;
//        	}
//        	list.add(data);
//        }
        
//        iPage.setRecords(list);
        PageResult pageResult = new PageResult();
        pageResult.setPage(params.getPageNum());
        pageResult.setPageSize(params.getPageSize());
        pageResult.setTotal(iPage.getTotal());
        pageResult.setData(iPage.getRecords());

        return pageResult;
    }

    private IPage<NafpDataEntity> queryData(IPage page, NafpDataParams params) {
        IPage<NafpDataEntity> result = null;
        if(params.getDataType().equals(DataTypeEnum.T1279.getDataType()))
        {
            result = nafpDataMapper.queryT1279Data(page, params);
        }
        else if(params.getDataType().equals(DataTypeEnum.ECMF.getDataType()))
        {
            result = nafpDataMapper.queryKT1279Data(page, params);
        }
        else if(params.getDataType().equals(DataTypeEnum.GRAPES.getDataType()))
        {
            result = nafpDataMapper.queryGrapesData(page, params);
        }
        else if(params.getDataType().equals(DataTypeEnum.SWC3KM.getDataType()))
        {
            result = nafpDataMapper.querySwc3kmData(page, params);
        }
        else if(params.getDataType().equals(DataTypeEnum.SWC9KM.getDataType()))
        {
            result = nafpDataMapper.querySwc9kmData(page, params);
        }
        else if(params.getDataType().equals(DataTypeEnum.CLDAS.getDataType()))
        {
            result = nafpDataMapper.queryCldasData(page, params);
        }
        else if(params.getDataType().equals(DataTypeEnum.CMPA.getDataType()))
        {
            result = nafpDataMapper.queryCmpaData(page, params);
        }
        else if(params.getDataType().equals(DataTypeEnum.SCMOC.getDataType()))
        {
            result = nafpDataMapper.queryScmocData(page, params);
        }

        return result;
    }

	@Override
	public List<DataQcManagerEntity> queryDataManagerCountAll(DataQcManagerParams params) {
		ConfigParams configParams = new ConfigParams();
        configParams.setDataType("mde");
        List<DataSourceType> dataSourceTypeList = configMapper.getDataSourceByDataType(configParams);
        Map<String, String> map = new LinkedHashMap<>();
        for(DataSourceType dataSourceType : dataSourceTypeList)
        {
            map.put(dataSourceType.getDataSource(), dataSourceType.getDataSourceName());
        }
        
        List<DataQcManagerEntity> list = nafpDataMapper.queryDataManagerCountAll(params);
        
        for(DataQcManagerEntity data : list)
        {
        	data.setDataSource(map.get(data.getDataSource()));
        }
		
		return list;
	}

	@Override
	public List<DataQcManagerEntity> queryDataManagerCount(DataQcManagerParams params) {
		
		ConfigParams configParams = new ConfigParams();
        configParams.setDataType("mde");
        List<DataSourceType> dataSourceTypeList = configMapper.getDataSourceByDataType(configParams);
        Map<String, String> map = new LinkedHashMap<>();
        for(DataSourceType dataSourceType : dataSourceTypeList)
        {
            map.put(dataSourceType.getDataSource(), dataSourceType.getDataSourceName());
        }
        
		List<DataQcManagerEntity> list = nafpDataMapper.queryDataManagerCount(params);
		
		for(DataQcManagerEntity data : list)
        {
        	data.setDataSource(map.get(data.getDataSource()));
        }
		
		return list;
	}

}
