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
import com.util.CalRainRateUtil;
import com.util.DbUtils;
import com.util.NumberFormatUtil;

public class StationDayRainCheckThread implements Runnable{

	private String dataSource;
	private String element;
	private String method;
	private List<String> dateList;
	private String sql;
//              dataSource  element     method       datetime
	private Map<String, Map<String, Map<String, Map<String, Double>>>> result;
	private CountDownLatch latch;
	
	public StationDayRainCheckThread(String dataSource, String element, String method, List<String> dateList, String sql, Map<String, Map<String, Map<String, Map<String, Double>>>> result, CountDownLatch latch) {
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
		Map<String, Map<String, List<double[]>>> rsMap = new HashMap<>();
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
			String dateStr = null;
			double[] rainValueNum = null;
			while(rs.next())
			{
				dateStr = rs.getString(2).split(" ")[0];
				rainValueNum = new double[4];
				for(int j = 0; j < 4; j++)
				{
					rainValueNum[j] = rs.getDouble(4 + j);
				}
				if(rainValueNum[0] != DecodeConstants.UNDEF_DOUBLE_VALUE && rainValueNum[1] != DecodeConstants.UNDEF_DOUBLE_VALUE &&
				   rainValueNum[2] != DecodeConstants.UNDEF_DOUBLE_VALUE && rainValueNum[3] != DecodeConstants.UNDEF_DOUBLE_VALUE)
				{
					rsMap.get(rs.getString(3)).get(dateStr).add(rainValueNum);
				}
			}
			System.out.println("使用JDBC获取结果集数据耗时: " + (System.currentTimeMillis() - time));
			time = System.currentTimeMillis();
			
			for(String dataDateStr : dateList)
			{
				List<double[]> list = rsMap.get(dataSource).get(dataDateStr);
				double h = 0;
				double tn = 0;
				double m = 0;
				double f = 0;
				for(double[] value : list)
				{
					h += value[0];
					tn += value[1];
					m += value[2];
					f += value[3];
				}
				Double calRainRate = NumberFormatUtil.numFormat(CalRainRateUtil.calRainRate(h, tn, m, f, method), 3);
//				System.out.println(dataSource + ": " + h + "," + tn + "," + m + "," + f);
				if(calRainRate == DecodeConstants.UNDEF_DOUBLE_VALUE)
				{
					calRainRate = null;
				}
				result.get(dataSource).get(element).get(method).put(dataDateStr, calRainRate);
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
