package com.model.task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.model.pojo.ModelManagerEntity;
import com.util.TimeUtil;

public class GribModelUpgradeTask {
	private static ScheduledExecutorService executorService = null;
	public static void upgradeModel(ModelManagerEntity entity)
	{
		try {
			if(executorService != null)
			{
				executorService.shutdownNow();
			}
			executorService = Executors.newScheduledThreadPool(1);
			
			String nextUpdateTime = entity.getNextUpdateTime();
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(TimeUtil.dateTimeStr2date(nextUpdateTime, TimeUtil.DEFAULT_DATETIME_FORMAT, TimeUtil.DEFAULT_DATETIME_FORMAT));
			Date startDate = new Date();
	        Date endDate = calendar.getTime();
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
	        int count = 0;
	        if(dateList != null && dateList.size() != 0)
	        {
	        	count = dateList.size();
	        }
			
			executorService.scheduleAtFixedRate(new GribModelUpgradeThread(entity), count, 30, TimeUnit.DAYS);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}
