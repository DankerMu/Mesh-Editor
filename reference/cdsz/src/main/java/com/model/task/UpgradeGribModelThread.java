package com.model.task;

import com.model.pojo.ModelManagerParams;
import com.model.service.inf.ModelInfoService;

public class UpgradeGribModelThread implements Runnable{

	private ModelManagerParams params;
	private  ModelInfoService modelInfoService;
	public UpgradeGribModelThread(ModelManagerParams params,  ModelInfoService modelInfoService) {
		this.params = params;
		this.modelInfoService = modelInfoService;
	}
	
	@Override
	public void run() {
		
		modelInfoService.updateModelStatus(params);
//		params.setAuthor("System");
		//TODO 调用python智能订正模型升级接口
		HttpTool.doPost(params.getUrl(), "");
//		System.out.println(params.getUrl());
	}

}
