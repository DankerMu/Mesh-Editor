package com.check.thread;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import com.alibaba.druid.pool.DruidDataSource;
import com.constants.DecodeConstants;
import com.util.DbUtils;
import com.util.NumberFormatUtil;

public class StationDayCheckThread implements Runnable{
	
	private String dataSource;
	private String element;
	private String method;
	private List<String> dateList;
	private String sql;
//              dataSource  element     method       datetime
	private Map<String, Map<String, Map<String, Map<String, Double>>>> result;
	private CountDownLatch latch;
	public StationDayCheckThread(String dataSource, String element, String method, List<String> dateList, String sql, Map<String, Map<String, Map<String, Map<String, Double>>>> result, CountDownLatch latch) {
		this.dataSource = dataSource;
		this.element = element;
		this.method = method;
		this.dateList = dateList;
		this.sql = sql;
		this.result = result;
		this.latch = latch;
	}

	@Override
	public void run() {
		DruidDataSource dataSourceJDBC = DbUtils.getInstance().getDataSource();
		Map<String, Map<String, List<Double>>> rsMap = new HashMap<>();
		Map<String, Map<String, Double>> rsResultMap = new HashMap<>();
		
		
		rsMap.put(dataSource, new HashMap<>());
		for(String date : dateList)
		{
			rsMap.get(dataSource).put(date, new ArrayList<>());
			
			rsResultMap.put(dataSource, new HashMap<>());
			result.put(dataSource, new HashMap<>());
			result.get(dataSource).put(element, new HashMap<>());
			result.get(dataSource).get(element).put(method, new LinkedHashMap<>());
		}
		
		
		long time = System.currentTimeMillis();
		long tt = time;
		try (Connection conn = dataSourceJDBC.getConnection();
				Statement st = conn.createStatement();
				ResultSet rs = st.executeQuery(sql);){
			System.out.println("使用JDBC站点逐时查询耗时: " + (System.currentTimeMillis() - time));
			time = System.currentTimeMillis();
			while(rs.next())
			{
				if(rs.getDouble(4) != DecodeConstants.UNDEF_DOUBLE_VALUE)
				{
					rsMap.get(rs.getString(3)).get(rs.getString(2)).add(rs.getDouble(4));
				}
			}
			System.out.println("使用JDBC获取结果集数据耗时: " + (System.currentTimeMillis() - time));
			time = System.currentTimeMillis();
			int[] scaleNum = new int[2];
			if(element.equals("atmax") || element.equals("atmin"))
			{
				scaleNum[0] = 100;
				scaleNum[1] = 1;
			}
			else if(element.equals("at"))
			{
				scaleNum[0] = 1;
				scaleNum[1] = 2;
			}
			else if(element.equals("ws"))
			{
				scaleNum[0] = 1;
				scaleNum[1] = 1;
			}
			else
			{
				scaleNum[0] = 1;
				scaleNum[1] = 1;
			}
			
			for(String dataDateStr : dateList)
			{
				List<Double> list = rsMap.get(dataSource).get(dataDateStr);
				double sum = 0;
				for(double value : list)
				{
					sum += value;
				}
				if(list.size() == 0)
				{
					rsResultMap.get(dataSource).put(dataDateStr, null); //= DecodeConstants.UNDEF_DOUBLE_VALUE;
				}
				else
				{
					rsResultMap.get(dataSource).put(dataDateStr, NumberFormatUtil.numFormat(scaleNum[0] * sum / list.size(), scaleNum[1]));// = NumberFormatUtil.numFormat(scaleNum[0] * sum / list.size(), scaleNum[1]);
				}
			}
			for(String dataDateStr : dateList)
			{
				result.get(dataSource).get(element).get(method).put(dataDateStr, rsResultMap.get(dataSource).get(dataDateStr));
			}
			
			System.out.println("使用JDBC计算结果耗时: " + (System.currentTimeMillis() - time));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			latch.countDown();
		}
		System.out.println("使用JDBC和多线程站点逐时总耗时: " + (System.currentTimeMillis() - tt));
	
		
	}

}
