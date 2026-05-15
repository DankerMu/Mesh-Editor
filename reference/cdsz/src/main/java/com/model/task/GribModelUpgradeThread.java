package com.model.task;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import com.alibaba.druid.pool.DruidDataSource;
import com.model.pojo.ModelManagerEntity;
import com.station.indb.util.DbUtils;
import com.util.ReadPropertiesUtil;
import com.util.TimeUtil;

public class GribModelUpgradeThread implements Runnable{
	private static DruidDataSource dataSource = DbUtils.getInstance().getDataSource();
	private ModelManagerEntity entity;
	public GribModelUpgradeThread(ModelManagerEntity entity) {
		this.entity = entity;
	}
	
	@Override
	public void run() {
		Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
		String pathUrl = configMap.get("upgrade_grib_model_url");
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
		
			st.executeUpdate(sql);
			
        } catch (SQLException e) {
            e.printStackTrace();
        }
		
		
		HttpTool.doPost(pathUrl, "");
	}
}
