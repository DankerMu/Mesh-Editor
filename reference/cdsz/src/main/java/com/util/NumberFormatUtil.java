package com.util;

import com.constants.DecodeConstants;

import java.text.NumberFormat;

public class NumberFormatUtil {
    /**
     * 科学计数转String
     *
     * @param num
     * @return
     */
    public static String science(float num) {
        NumberFormat nf = NumberFormat.getNumberInstance();
        //设置保留多少为小数
        nf.setMaximumFractionDigits(5);
        //取消科学计数法
        nf.setGroupingUsed(false);


        return nf.format(num);
    }
    
    public static String scienceD(double num) {
    	NumberFormat nf = NumberFormat.getNumberInstance();
    	//设置保留多少为小数
    	nf.setMaximumFractionDigits(5);
    	//取消科学计数法
    	nf.setGroupingUsed(false);
        if(Double.isNaN(num))
        {
            num = DecodeConstants.UNDEF_DOUBLE_VALUE;
        }
    	
    	return nf.format(num);
    }

    public static String scienceD(double num, int count) {
    	NumberFormat nf = NumberFormat.getNumberInstance();
    	//设置保留多少为小数
    	nf.setMaximumFractionDigits(count);
    	//取消科学计数法
    	nf.setGroupingUsed(false);
        if(Double.isNaN(num))
        {
            num = DecodeConstants.UNDEF_DOUBLE_VALUE;
        }

    	return nf.format(num);
    }

    public static double[][] scienceDs(double[][] values, int count)
    {
        int n = values.length;
        int m = values[0].length;
        double[][] result = new double[n][m];
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < m; j++)
            {
                result[i][j] = Double.parseDouble(scienceD(values[i][j], 3));
            }
        }

        return result;
    }

    public static double numFormat(double num, int count)
    {
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(count);

        return Double.parseDouble(nf.format(num).replace(",", ""));
    }

}
