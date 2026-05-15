package com.model.task;

import com.alibaba.fastjson2.JSONObject;
import com.model.pojo.ModelManagerParams;
import com.model.service.inf.ModelInfoService;

public class UpgradeStationModelThread implements Runnable{

	private ModelManagerParams params;
	private ModelInfoService modelInfoService;
	public UpgradeStationModelThread(ModelManagerParams params, ModelInfoService modelInfoService) {
		this.params = params;
		this.modelInfoService = modelInfoService;
	}
	
	@Override
	public void run() {
		//TODO 调用python站点模型升级接口    http://192.168.2.81:8035/upgradeModel
		int upType = 0;
		if(params.getId() == 1)
		{
			upType = 0;//已有站点
		}
		else if(params.getId() == 3)
		{
			upType = 1;//无站点
		}
		JSONObject json = new JSONObject();
		json.put("up_type", upType);
//		String pathUrl = "http://192.168.2.81:8035/upgradeModel";
//		params.setAuthor("System");
		modelInfoService.updateModelStatus(params);
		HttpTool.doPost(params.getUrl(), json.toString());
	}

}
