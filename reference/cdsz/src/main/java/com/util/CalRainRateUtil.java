package com.util;

import com.constants.DecodeConstants;

public class CalRainRateUtil {
	public static double calRainRate(double h, double tn, double m, double f, String method)
	{
		double[] result = new double[4];
		double ts = 0;
		double th = 0;
		double tf = 0;
		double tm = 0;
		
		ts = h / ((h + m + f) == 0 ? 1 : (h + m + f));
        th = (h + tn) / ((h + f + m + tn) == 0 ? 1 : (h + f + m + tn));
        tf = f / ((f + h) == 0 ? 1 : (f + h));
        tm = m / ((m + h) == 0 ? 1 : (h + m));
//        System.out.println("h:" + h + ",tn:" + tn + ",m:" + m + ",f:" + f);
        result[0] = NumberFormatUtil.numFormat(ts * 100, 1);   // TS评分
        result[1] = NumberFormatUtil.numFormat(th * 100, 1);   // 准确率
        result[2] = NumberFormatUtil.numFormat(tf * 100, 1);   // 空报
        result[3] = NumberFormatUtil.numFormat(tm * 100, 1);   // 漏报
        if(h == 0 && tn == 0 && m == 0 && f == 0)
        {
        	result[0] = DecodeConstants.UNDEF_DOUBLE_VALUE;
        	result[1] = DecodeConstants.UNDEF_DOUBLE_VALUE;
        	result[2] = DecodeConstants.UNDEF_DOUBLE_VALUE;
        	result[3] = DecodeConstants.UNDEF_DOUBLE_VALUE;
        }
        int index = 0;
        if(method.equals("晴雨准确率"))
        {
        	index = 1;
        }
        else if(method.equals("TS评分"))
        {
        	index = 0;
        }
        else if(method.equals("空报率"))
        {
        	index = 2;
        }
        else if(method.equals("漏报率"))
        {
        	index = 3;
        }
        
        return result[index];
	}
}
