package com.model.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson2.JSONObject;
import com.config.pojo.DataSourceType;
import com.log.pojo.LogRecordParams;
import com.log.service.inf.LogService;
import com.model.pojo.ModelInfoEntity;
import com.model.pojo.ModelInfoParams;
import com.model.pojo.ModelManagerDetailEntity;
import com.model.pojo.ModelManagerEntity;
import com.model.pojo.ModelManagerParams;
import com.model.service.inf.ModelInfoService;
import com.model.task.HttpTool;
import com.model.task.UpgradeGribModelTask;
import com.model.task.UpgradeStationModelTask;
import com.tool.ApiResult;
import com.tool.PageResult;
import com.util.ReadPropertiesUtil;
import com.util.TimeUtil;

/**
 * @category
 * @date 2025/3/14 17:12
 * @description TODO
 */
@RestController
@RequestMapping("/model/")
@Api(tags = "模型管理")
public class ModelInfoController {
    @Resource
    private ModelInfoService modelInfoService;
    @Resource
    private LogService logService;
//    private static Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
    @PostMapping("queryModelInfoLike")
    @ApiOperation("模糊查询模型信息")
    public List<ModelInfoEntity> queryModelInfoLike(@RequestBody ModelInfoParams params)
    {
        return modelInfoService.queryModelInfoLike(params);
    }

    @PostMapping("queryModelInfo")
    @ApiOperation("查询模型信息")
    public PageResult queryModelInfo(@RequestBody ModelInfoParams params)
    {
        try {
            return modelInfoService.queryModelInfo(params);
        } catch (Exception e) {
            e.printStackTrace();
            return PageResult.fail(e.getMessage());
        }
    }

    @PostMapping("deleteModelById")
    @ApiOperation("删除模型")
    public ApiResult deleteModelById(@RequestBody ModelInfoParams params)
    {
        boolean result = modelInfoService.deleteModelById(params);
        return ApiResult.success(result);
    }

    @PostMapping("addModelInfo")
    @ApiOperation("创建模型")
    public ApiResult addModelInfo(@RequestBody ModelInfoEntity params)
    {
        boolean result = modelInfoService.addModelInfo(params);
        return ApiResult.success(result);
    }

    @PostMapping("queryUseableModelList")
    @ApiOperation("查询可替换的模型列表")
    public List<ModelInfoEntity> queryUseableModelList(@RequestBody ModelInfoParams params)
    {
        return modelInfoService.queryUseableModelList(params);
    }

//    @PostMapping("upgradeModel")
//    @ApiOperation("模型升级")
//    public ApiResult upgradeModel(@RequestBody ModelInfoParams params)
//    {
//        String result = "模型升级成功";
//        if(!params.getModelType().equals("zone"))
//        {
//            String configFilePath = "/data/revised-model/stations/工作站点列表_更新.csv";
//            File file = new File("/data/revised-model/stations/");
//            if(file.exists())
//            {
//                File[] files = file.listFiles();
//                for(File f : files)
//                {
//                    System.out.println(f.getName());
//                }
//            }
//            else
//            {
//                System.out.println("/data/revised-model/stations/ 文件不存在!");
//            }
//            List<String> lines = FileUtil.readLines(configFilePath, "utf-8");
//            List<String> outLines = new ArrayList<>();
//            String station = params.getModelName().split("_")[0];
//            if(lines == null || lines.size() == 0)
//            {
//                outLines.add("station,lon,lat");
//            }
//            else
//            {
//                lines.get(0);
//            }
//            outLines.add(station + ",0,0");
//            FileUtil.writeLines(outLines, configFilePath, "utf-8");
//            List<String> resultContent = RunCommandUtil.runShFile("/home/yyyh/project/StationCorrection/update_site.sh");
//            System.out.println(resultContent);
//        }
//
//
//        return ApiResult.success(result);
//    }

    
//  ===============================================================================================================================
    

    @PostMapping("queryModelManagerList")
    @ApiOperation("查询模型管理列表")
    public List<ModelManagerEntity> queryModelManagerList()
    {
    	LogRecordParams logRecordParams = new LogRecordParams("模型管理", "查询模型管理列表");
        logService.addLogRecord(logRecordParams);
        
        return modelInfoService.queryModelManagerList();
    }
    
    @PostMapping("queryGribUsedModel")
    @ApiOperation("查询正在使用的格点模型")
    public DataSourceType queryGribUsedModel()
    {
    	ModelManagerEntity usedModel = modelInfoService.queryGribUsedModel();
    	DataSourceType result = new DataSourceType();
    	String dataSource = "";
    	String dataSourceName = "";
    	if(usedModel.getId() == 1)
    	{
    		dataSource = "deep";
    		dataSourceName = "智能订正";
    	}
    	else if(usedModel.getId() == 2)
    	{
    		dataSource = "ecmf";
        	dataSourceName = "ECMWF订正";
    	}
    	else if(usedModel.getId() == 3)
    	{
    		dataSource = "grapes";
    		dataSourceName = "GRAPES_GFS订正";
    	}
//    	dataSourceName = "订正";
    	result.setId(usedModel.getId());
    	result.setDataSource(dataSource);
    	result.setDataSourceName(dataSourceName);
    	
    	return result;
    }
    
    
    
    
    @PostMapping("upgradeModel")
    @ApiOperation("模型升级")
    public ApiResult upgradeModel(@RequestBody ModelManagerParams params)
    {
        String result = "模型升级成功";
        Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
        //TODO 调用python模型升级接口，获取返回结果
        try {
        	if(params.getId() == 2)
            {
        		params.setUrl(configMap.get("upgrade_grib_model_url"));
        		params.setFixRateDay(Integer.parseInt(configMap.get("upgrade_grib_model_fix")));
            	UpgradeGribModelTask.upgradeModel(params, modelInfoService);
            }
            else
            {
            	params.setUrl(configMap.get("upgrade_station_model_url"));
        		params.setFixRateDay(Integer.parseInt(configMap.get("upgrade_station_model_fix")));
            	UpgradeStationModelTask.upgradeModel(params, modelInfoService);
            }
		} catch (Exception e) {
			e.printStackTrace();
			return ApiResult.fail("模型升级失败");
		}
        

        //TODO 更新模型管理表的模型状态
        params.setStatus(0);  //0是升级中，1是升级完成
        int i = modelInfoService.updateModelStatus(params);
        if(i <= 0)
        {
            result = "模型升级失败";
        }
        LogRecordParams logRecordParams = new LogRecordParams("模型管理", "模型升级");
        logService.addLogRecord(logRecordParams);

        return ApiResult.success(result);
    }

    @PostMapping("replaceModel")
    @ApiOperation("模型替换")
    public int replaceModel(@RequestBody ModelManagerParams params)
    {
//        String result = "模型替换成功";
        //TODO 调用python模型替换接口，接收返回值
//    	{
//    	    "st_id": 55564,
//    	    "model_name": 2    //1:普通模型   2:强拟合模型
//    	}
    	int result = 0;
    	Calendar calendar = Calendar.getInstance();
    	calendar.setTime(new Date());
    	calendar.add(Calendar.HOUR_OF_DAY, 8);
    	if(params.getStationNum() == null || params.getStationNum().length() == 0)
    	{
    		//TODO 更新模型状态    0:替换中  1: 替换完成
        	params.setReplaceTime(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
            int ii = modelInfoService.updateGribModelReplaceStatusChange(params);
            int i = modelInfoService.updateGribModelReplaceStatus(params);
            if(i >= 0 && ii >= 0)
            {
                result = 1;
                LogRecordParams logRecordParams = new LogRecordParams("模型管理", "格点模型替换");
                logService.addLogRecord(logRecordParams);
            }
    	}
    	else
    	{
    		Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
        	try {
        		JSONObject json = new JSONObject();
        		json.put("st_id", params.getStationNum());
        		int modelName = 0;
    			if("普通模型 ".equals(params.getModel()))
    			{
    				modelName = 1;
    			}
    			else
    			{
    				modelName = 2;
    			}
    			json.put("model_name", modelName);
    			//0:有站点 1:无站点 2:自建站点
    			if(params.getManagerId() == 1)
    			{
    				json.put("st_class", 0);
    			}
    			else if(params.getManagerId() == 3)
    			{
    				json.put("st_class", 1);
    			}
    			else if(params.getManagerId() == 6)
    			{
    				json.put("st_class", 2);
    			}
    			  
//    			String pathUrl = "http://192.168.2.81:8035/replaceModel";
    			HttpTool.doPost(configMap.get("replace_station_model_url"), json.toString());
    			
    		} catch (Exception e) {
    			e.printStackTrace();
    			return 0;
    		}

            //TODO 更新模型状态    0:替换中  1: 替换完成
        	params.setReplaceTime(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
            int ii = modelInfoService.updateModelReplaceStatusChange(params);
            int i = modelInfoService.updateModelReplaceStatus(params);
            
            if(i >= 0 && ii >= 0)
            {
                result = 1;
                LogRecordParams logRecordParams = new LogRecordParams("模型管理", "站点模型替换");
                logService.addLogRecord(logRecordParams);
            }
    	}
    	
    	
        

        return result;
    }
    
    @PostMapping("replaceGribModel")
    @ApiOperation("格点模型替换")
    public int replaceGribModel(@RequestBody ModelManagerParams params)
    {
    	Calendar calendar = Calendar.getInstance();
    	calendar.setTime(new Date());
    	calendar.add(Calendar.HOUR_OF_DAY, 8);
    	//TODO 更新模型状态    0:替换中  1: 替换完成
    	params.setReplaceTime(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
        int ii = modelInfoService.updateGribModelReplaceStatusChange(params);
        int i = modelInfoService.updateGribModelReplaceStatus(params);
        int result = 0;
        if(i >= 0 && ii >= 0)
        {
            result = 1;
            LogRecordParams logRecordParams = new LogRecordParams("模型管理", "格点模型替换");
            logService.addLogRecord(logRecordParams);
        }
    	
    	return result;
    }
    

    @PostMapping("queryModelUpgradeStatusById")
    @ApiOperation("查询模型升级状态")
    public int queryModelUpgradeStatusById(@RequestBody ModelManagerParams params)
    {
        ModelManagerEntity modelManagerEntity = modelInfoService.queryModelUpgradeStatusById(params);

        return modelManagerEntity.getStatus();
    }

    @PostMapping("queryModelManagerDetailById")
    @ApiOperation("根据模型ID查询模型详情")
    public List<ModelManagerDetailEntity> queryModelManagerDetailById(@RequestBody ModelManagerParams params)
    {
    	
    	return modelInfoService.queryModelManagerDetailById(params);
    }
    
    
    @PostMapping("queryModelManagerListDetail")
    @ApiOperation("查询模型详情")
    public PageResult queryModelManagerListDetail(@RequestBody ModelManagerParams params)
    {
        PageResult pageResult = null;
        if(params.getId() == 2)
        {
        	pageResult = modelInfoService.queryGribModelManagerListDetail(params);
        }
        else
        {
        	pageResult = modelInfoService.queryModelManagerListDetail(params);
        }
        LogRecordParams logRecordParams = new LogRecordParams("模型管理", "站点模型详情查询");
        logService.addLogRecord(logRecordParams);

        return pageResult;
    }
    
    @PostMapping("queryGribModelManagerListDetail")
    @ApiOperation("查询格点模型详情")
    public PageResult queryGribModelManagerListDetail(@RequestBody ModelManagerParams params)
    {
    	PageResult pageResult = modelInfoService.queryGribModelManagerListDetail(params);
    	LogRecordParams logRecordParams = new LogRecordParams("模型管理", "格点模型详情查询");
        logService.addLogRecord(logRecordParams);
    	
    	return pageResult;
    }

    @PostMapping("queryModelReplaceList")
    @ApiOperation("查询可以替换的模型列表")
    public List<ModelManagerDetailEntity> queryModelReplaceList(@RequestBody ModelManagerParams params)
    {
    	List<ModelManagerDetailEntity> result = null;
    	if(params.getStationNum() == null || params.getStationNum().length() == 0)
    	{
    		result = modelInfoService.queryGribModelReplaceList(params);
    	}
    	else
    	{
    		result = modelInfoService.queryModelReplaceList(params);
    	}
    	
        return result;
    }
    
    @PostMapping("queryGribModelReplaceList")
    @ApiOperation("查询可以替换的格点模型列表")
    public List<ModelManagerDetailEntity> queryGribModelReplaceList(@RequestBody ModelManagerParams params)
    {
    	return modelInfoService.queryGribModelReplaceList(params);
    }

    @PostMapping("queryModelDetailListLike")
    @ApiOperation("模型详情列表模糊查询")
    public List<ModelManagerDetailEntity> queryModelDetailListLike(@RequestBody ModelManagerParams params)
    {
        return modelInfoService.queryModelDetailListLike(params);
    }

    @PostMapping("deleteModelByStationNum")
    @ApiOperation("删除模型")
    public int deleteModelByStationNum(@RequestBody ModelManagerParams params)
    {
    	int result = 0;
    	if(params.getStationNum() == null || params.getStationNum().length() == 0)
    	{
    		result = modelInfoService.updateGribModelEnabledModelDetailTabById(params);
    	}
    	else
    	{
    		params.setStationInt(Integer.parseInt(params.getStationNum()));
    		result = modelInfoService.updateStationModelEnabled(params);
    	}
    	
    	LogRecordParams logRecordParams = new LogRecordParams("模型管理", "站点模型删除");
        logService.addLogRecord(logRecordParams);
    	
        return result;
    }
    
    @PostMapping("deleteGribModel")
    @ApiOperation("删除格点模型")
    public int deleteGribModel(@RequestBody ModelManagerParams params)
    {
    	int result = modelInfoService.updateGribModelEnabledModelDetailTabById(params);
    	LogRecordParams logRecordParams = new LogRecordParams("模型管理", "格点模型删除");
    	logService.addLogRecord(logRecordParams);
    	
    	return result;
    }
    
    @PostMapping("queryGribModelAll")
    @ApiOperation("查询所有格点模型")
    public List<ModelManagerDetailEntity> queryGribModelAll()
    {
    	List<ModelManagerDetailEntity> list = modelInfoService.queryGribModelAll();
    	for(ModelManagerDetailEntity data : list)
    	{
    		if(data.getModel().contains("智能"))
    		{
    			data.setModel("智能订正");
    			data.setDataType("deep");
    		}
    		else if(data.getModel().contains("ECMWF"))
    		{
    			data.setModel("ECMWF订正");
    			data.setDataType("ecmf");
    		}
    		else if(data.getModel().contains("GRAPES_GFS"))
    		{
    			data.setModel("GRAPES_GFS订正");
    			data.setDataType("grapes");
    		}
    	}
    	
    	return list;
    }
}
