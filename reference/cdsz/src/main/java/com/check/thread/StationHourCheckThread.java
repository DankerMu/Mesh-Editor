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

public class StationHourCheckThread implements Runnable{

	private String dataSource;
	private String element;
	private String method;
	private int[] vtis;
	private String sql;
//              dataSource  element     method       vti
	private Map<String, Map<String, Map<String, Map<String, Double>>>> result;
	private CountDownLatch latch;
	
	public StationHourCheckThread(String dataSource, String element, String method, int[] vtis, String sql, Map<String, Map<String, Map<String, Map<String, Double>>>> result, CountDownLatch latch) {
		this.dataSource = dataSource;
		this.element = element;
		this.method = method;
		this.vtis = vtis;
		this.sql = sql;
		this.result = result;
		this.latch = latch;
	}
	
	@Override
	public void run() {
		DruidDataSource dataSourceJDBC = DbUtils.getInstance().getDataSource();
		Map<String, List<List<Double>>> rsMap = new HashMap<>();
		Map<String, double[]> rsResultMap = new HashMap<>();
		List<List<Double>> listss = new ArrayList<>();
		for(int i = 0; i < 10; i++)
		{
			listss.add(new ArrayList<>());
		}
		rsMap.put(dataSource, listss);
		rsResultMap.put(dataSource, new double[10]);
		result.put(dataSource, new HashMap<>());
		result.get(dataSource).put(element, new HashMap<>());
		result.get(dataSource).get(element).put(method, new LinkedHashMap<>());
		long time = System.currentTimeMillis();
//		long tt = time;
		try (Connection conn = dataSourceJDBC.getConnection();
				Statement st = conn.createStatement();
				ResultSet rs = st.executeQuery(sql);){
			System.out.println(dataSource + " 使用JDBC站点逐时查询耗时: " + (System.currentTimeMillis() - time));
			time = System.currentTimeMillis();
			//TODO 使用流并行获取结果集中的数据
//			Stream.of(rs).parallel();
			while(rs.next())
			{
				for(int i = 0; i < 10; i++)
				{
					if(rs.getDouble(i + 4) != DecodeConstants.UNDEF_DOUBLE_VALUE)
					{
						rsMap.get(rs.getString(3)).get(i).add(rs.getDouble(i + 4));
					}
				}
			}
			System.out.println(dataSource + " 使用JDBC获取结果集数据耗时: " + (System.currentTimeMillis() - time));
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
			
			List<List<Double>> lists = rsMap.get(dataSource);
			for(int i = 0; i < 10; i++)
			{
				List<Double> list = lists.get(i);
				double sum = 0;
				for(double value : list)
				{
					sum += value;
				}
				if(list.size() == 0)
				{
					rsResultMap.get(dataSource)[i] = DecodeConstants.UNDEF_DOUBLE_VALUE;
				}
				else
				{
					rsResultMap.get(dataSource)[i] = NumberFormatUtil.numFormat(scaleNum[0] * sum / list.size(), scaleNum[1]);
				}
			}
			
			
			for(int i = 0; i < 10; i++)
			{
				result.get(dataSource).get(element).get(method).put(vtis[i] + "", rsResultMap.get(dataSource)[i] != DecodeConstants.UNDEF_DOUBLE_VALUE ? rsResultMap.get(dataSource)[i] : null);
			}
			if(element.equals("at") || element.equals("ws"))
			{
				double v24 = result.get(dataSource).get(element).get(method).get("24");
				double v48 = result.get(dataSource).get(element).get(method).get("48");
				double v72 = result.get(dataSource).get(element).get(method).get("72");
				result.get(dataSource).get(element).get(method).put("0-72", NumberFormatUtil.numFormat(v24 * 0.5 + v48 * 0.333 + v72 * 0.167, 2));
			}
			if(element.equals("atmax") || element.equals("atmin"))
			{
				double v24 = result.get(dataSource).get(element).get(method).get("24");
				double v48 = result.get(dataSource).get(element).get(method).get("48");
				double v72 = result.get(dataSource).get(element).get(method).get("72");
				result.get(dataSource).get(element).get(method).put("0-72", NumberFormatUtil.numFormat((v24 + v48 + v72) / 3, 2));
			}
			
			
			
			
			System.out.println(dataSource + " 使用JDBC计算结果耗时: " + (System.currentTimeMillis() - time));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			latch.countDown();
		}
		
	}

}
