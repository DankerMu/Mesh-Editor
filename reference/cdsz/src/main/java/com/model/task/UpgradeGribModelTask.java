package com.model.task;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.model.pojo.ModelManagerParams;
import com.model.service.inf.ModelInfoService;

public class UpgradeGribModelTask {
	
	private static ScheduledExecutorService executorService = null;
	public static void upgradeModel(ModelManagerParams params, ModelInfoService modelInfoService) throws Exception
	{
		if(executorService != null)
		{
			executorService.shutdownNow();
		}
		executorService = Executors.newScheduledThreadPool(1);
		executorService.scheduleAtFixedRate(new UpgradeGribModelThread(params, modelInfoService), 0, params.getFixRateDay(), TimeUnit.DAYS);
		
	}
}
