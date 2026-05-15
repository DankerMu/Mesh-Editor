package com.check.thread;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import com.constants.DecodeConstants;

public class GetResultSetRainDataThread implements Runnable{
	private int i;
	private ResultSet rs;
	private Map<String, List<List<double[]>>> rsMap;
	private CountDownLatch latch;
	public GetResultSetRainDataThread(ResultSet rs, Map<String, List<List<double[]>>> rsMap, int i, CountDownLatch latch) {
		this.i = i;
		this.rs = rs;
		this.rsMap = rsMap;
		this.latch = latch;
	}
	@Override
	public void run() {
		try {
			double[] rainValueNum = new double[4];
			for(int j = 0; j < 4; j++)
			{
				rainValueNum[j] = rs.getDouble(i * 4 + 4 + j);
			}
			if(rainValueNum[0] != DecodeConstants.UNDEF_DOUBLE_VALUE && rainValueNum[1] != DecodeConstants.UNDEF_DOUBLE_VALUE &&
			   rainValueNum[2] != DecodeConstants.UNDEF_DOUBLE_VALUE && rainValueNum[3] != DecodeConstants.UNDEF_DOUBLE_VALUE)
			{
				rsMap.get(rs.getString(3)).get(i).add(rainValueNum);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			latch.countDown();
		}
		
		
	}

}
