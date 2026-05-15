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

public class GribHourCheckThread implements Runnable{
	
	private String dataSource;
	private String method;
	private int[] vtis;
	private String sql;
//              dataSource  method      datetime
	private Map<String, Map<String, Map<String, Double>>> result;
	private CountDownLatch latch;

	public GribHourCheckThread(String dataSource, String method, int[] vtis, String sql, Map<String, Map<String, Map<String, Double>>> result, CountDownLatch latch) {
		this.dataSource = dataSource;
		this.method = method;
		this.vtis = vtis;
		this.sql = sql;
		this.result = result;
		this.latch = latch;
	}
	
	@Override
	public void run() {
		
		DruidDataSource dataSourceJDBC = DbUtils.getInstance().getDataSource();
		
		Map<String, List<List<double[]>>> rsMap = new HashMap<>();
		Map<String, double[]> rsResultMap = new HashMap<>();
		
		List<List<double[]>> listss = new ArrayList<>();
		for(int i = 0; i < 10; i++)
		{
			listss.add(new ArrayList<>());
		}
		rsMap.put(dataSource, listss);
		rsResultMap.put(dataSource, new double[10]);
		result.put(dataSource, new HashMap<>());
		result.get(dataSource).put(method, new LinkedHashMap<>());
		
		long time = System.currentTimeMillis();
		long tt = time;
		try (Connection conn = dataSourceJDBC.getConnection();
				Statement st = conn.createStatement();
				ResultSet rs = st.executeQuery(sql);){
			System.out.println("使用JDBC站点逐时查询耗时: " + (System.currentTimeMillis() - time));
			time = System.currentTimeMillis();
			double[] rainValueNum = null;
			while(rs.next())
			{
				for(int i = 0; i < 10; i++)
				{
					rainValueNum = new double[4];
					for(int j = 0; j < 4; j++)
					{
						rainValueNum[j] = rs.getDouble(i * 4 + 4 + j);
					}
					if(rainValueNum[0] != DecodeConstants.UNDEF_DOUBLE_VALUE && rainValueNum[1] != DecodeConstants.UNDEF_DOUBLE_VALUE &&
					   rainValueNum[2] != DecodeConstants.UNDEF_DOUBLE_VALUE && rainValueNum[3] != DecodeConstants.UNDEF_DOUBLE_VALUE)
					{
						rsMap.get(rs.getString(3)).get(i).add(rainValueNum);
					}
				}
			}
			System.out.println("使用JDBC获取结果集数据耗时: " + (System.currentTimeMillis() - time));
			time = System.currentTimeMillis();
			
			double h3 = 0;
			double tn3 = 0;
			double m3 = 0;
			double f3 = 0;
			List<List<double[]>> lists = rsMap.get(dataSource);
			for(int i = 0; i < 10; i++)
			{
				List<double[]> list = lists.get(i);
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
					if(vtis[i] <= 72)
					{
						h3 += value[0];
						tn3 += value[1];
						m3 += value[2];
						f3 += value[3];
					}
				}
				Double calRainRate = NumberFormatUtil.numFormat(CalRainRateUtil.calRainRate(h, tn, m, f, method), 3);
				if(calRainRate == DecodeConstants.UNDEF_DOUBLE_VALUE)
				{
					calRainRate = null;
				}
				result.get(dataSource).get(method).put(vtis[i] + "", calRainRate);
			}
			Double calRainRate3 = NumberFormatUtil.numFormat(CalRainRateUtil.calRainRate(h3, tn3, m3, f3, method), 3);
			if(calRainRate3 == DecodeConstants.UNDEF_DOUBLE_VALUE)
			{
				calRainRate3 = null;
			}
			result.get(dataSource).get(method).put("0-72", calRainRate3);
			System.out.println("使用JDBC计算结果耗时: " + (System.currentTimeMillis() - time));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			latch.countDown();
		}
		System.out.println("使用JDBC和多线程站点逐时总耗时: " + (System.currentTimeMillis() - tt));

	}

}
