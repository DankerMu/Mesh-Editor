package com.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import com.alibaba.druid.pool.DruidDataSource;

public class JdbcQueryUtil {
	public static ResultSet queryData(String sql)
	{
		DruidDataSource dataSource = DbUtils.getInstance().getDataSource();
		try (Connection conn = dataSource.getConnection();
			 Statement st = conn.createStatement();
			 ResultSet rs = st.executeQuery(sql);){
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		
		return null;
	}
}
