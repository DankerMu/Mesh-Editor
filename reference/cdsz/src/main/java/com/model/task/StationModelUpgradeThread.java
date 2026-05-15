package com.model.task;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson2.JSONObject;
import com.model.pojo.ModelManagerEntity;
import com.station.indb.util.DbUtils;
import com.util.ReadPropertiesUtil;
import com.util.TimeUtil;

public class StationModelUpgradeThread implements Runnable{
	private static DruidDataSource dataSource = DbUtils.getInstance().getDataSource();
	private ModelManagerEntity entity;
	public StationModelUpgradeThread(ModelManagerEntity entity) {
		this.entity = entity;
	}
	
	@Override
	public void run() {
		Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
		int upType = 0;
		if(entity.getId() == 1)
		{
			upType = 0;//已有站点
		}
		else if(entity.getId() == 3)
		{
			upType = 1;//无站点
		}
		JSONObject json = new JSONObject();
		json.put("up_type", upType);
//		String pathUrl = "http://192.168.2.81:8035/upgradeModel";
//		params.setAuthor("System");
		String pathUrl = configMap.get("upgrade_station_model_url");
//		modelInfoService.updateModelStatus(params);
//		update public.model_manager_tab set latestupdatetime = #{latestUpdateTime}, nextupdatetime = #{nextUpdateTime}, author = #{author}, status = #{status} where id = #{id}

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		String latestUpdateTime = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT);
		calendar.add(Calendar.DAY_OF_MONTH, 30);
		String nextUpdateTime = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATE_FORMAT) + " 00:00:00";
		String author = "System";
		String status = "0";
		
		String sql = "update public.model_manager_tab set latestupdatetime = '" + latestUpdateTime + "', nextupdatetime = '" + nextUpdateTime + "', author = '" + author + "', status = '" + status + "' where id = " + entity.getId();
		try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement();){
			
				int count = st.executeUpdate(sql);
				
	        } catch (SQLException e) {
	            e.printStackTrace();
	        }
		
		
		HttpTool.doPost(pathUrl, json.toString());
	}

}
