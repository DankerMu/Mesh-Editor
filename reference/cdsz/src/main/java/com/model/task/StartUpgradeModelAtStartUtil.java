package com.model.task;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.druid.pool.DruidDataSource;
import com.model.pojo.ModelManagerEntity;
import com.station.indb.util.DbUtils;

public class StartUpgradeModelAtStartUtil {
	
	private static DruidDataSource dataSource = DbUtils.getInstance().getDataSource();

	public static void start()
	{
		List<ModelManagerEntity> list = getList();
		for(ModelManagerEntity data : list)
		{
			if(data.getType().equals("station"))
			{
				StationModelUpgradeTask.upgradeModel(data);
			}
			else
			{
				GribModelUpgradeTask.upgradeModel(data);
			}
		}
	}
	
	private static List<ModelManagerEntity> getList()
	{
		List<ModelManagerEntity> result = new ArrayList<>();
		ModelManagerEntity data = null;
        String sql = "select * from public.model_manager_tab where used = 0";
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql);){
            while(rs.next())
            {
            	data = new ModelManagerEntity();
            	data.setId(rs.getInt(1));
            	data.setModelName(rs.getString(2));
            	if(data.getModelName().startsWith("自建站点"))
            	{
            		continue;
            	}
            	data.setLatestUpdateTime(rs.getString(4));
            	data.setNextUpdateTime(rs.getString(5));
            	data.setType(rs.getString(10));
            	result.add(data);
            }
            
            
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
		
		
		return result;
	}
}
