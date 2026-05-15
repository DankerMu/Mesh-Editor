package com.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import net.bytebuddy.asm.Advice.This;

import com.constants.DecodeConstants;

public class BilinearInterpolateUtil {
	/**
     * @category 使用双线性插值，将二维数组插值为指定大小的二维数组
     * @date 2025/2/21 8:18
     * @param originalArray
     * @param targetRows
     * @param targetCols
     * @return double[][]
     */
    public static double[][] bilinearInterpolation(double[][] originalArray, int targetRows, int targetCols)
    {
        int originalRows = originalArray.length;
        int originalCols = originalArray[0].length;
        double scaleRow = (double) (originalRows - 1) / (targetRows - 1);
        double scaleCol = (double) (originalCols - 1) / (targetCols - 1);
        double[][] result = new double[targetRows][targetCols];

        for (int i = 0; i < targetRows; i++) {
            for (int j = 0; j < targetCols; j++) {
                double originalI = i * scaleRow;
                double originalJ = j * scaleCol;
                int i1 = (int) Math.floor(originalI);
                int i2 = (int) Math.ceil(originalI);
                int j1 = (int) Math.floor(originalJ);
                int j2 = (int) Math.ceil(originalJ);

                double value11 = originalArray[i1][j1];
                double value12 = originalArray[i1][j2];
                double value21 = originalArray[i2][j1];
                double value22 = originalArray[i2][j2];

                double x1 = originalI - i1;
                double x2 = 1 - x1;
                double y1 = originalJ - j1;
                double y2 = 1 - y1;
                double value = x2 * (y2 * value11 + y1 * value12) + x1 * (y2 * value21 + y1 * value22);

                if(Double.isNaN(value))
                {
                    result[i][j] = DecodeConstants.UNDEF_DOUBLE_VALUE;
                }
                else
                {
                    result[i][j] = Double.parseDouble(NumberFormatUtil.scienceD(value, 3));
                }
            }
        }
        return result;
    }
    
    
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern(TimeUtil.DEFAULT_DATETIME_FORMAT);
    
    public static InterpolationResult interpolate(Map<String, Double> inputDates, String startTime, String endTime, int disHour, String timeFormat)
    {
    	DateTimeFormatter ft = timeFormat != null ? DateTimeFormatter.ofPattern(timeFormat) : DF;
    	LocalDateTime start = LocalDateTime.parse(startTime, ft);
    	LocalDateTime end = LocalDateTime.parse(endTime, ft);
    	
    	long hours = ChronoUnit.HOURS.between(start, end);
    	if(hours < 0)
    	{
    		throw new IllegalArgumentException("起始时间不能晚于结束时间");
    	}
    	int pointCount = (int)hours / disHour + 1;
    	
    	List<LocalDateTime> inputTimes = new ArrayList<>();
    	List<Double> inputValues = new ArrayList<>();
    	
    	for(Map.Entry<String, Double> entry : inputDates.entrySet())
    	{
    		LocalDateTime time = LocalDateTime.parse(entry.getKey(), ft);
    		inputTimes.add(time);
//    		inputValues.add(entry.getValue());
    	}
    	
    	Collections.sort(inputTimes);
    	for(LocalDateTime time : inputTimes)
    	{
    		inputValues.add(inputDates.get(time.format(ft)));
    	}
    	
    	List<Double> sortedValues = new ArrayList<>();
    	Map<LocalDateTime, Double> timeToValue = new HashMap<>();
    	for(int i = 0; i < inputTimes.size(); i++)
    	{
    		timeToValue.put(inputTimes.get(i), inputValues.get(i));
    	}
    	for(LocalDateTime time : inputTimes)
    	{
    		sortedValues.add(timeToValue.get(time));
    	}
    	
    	if(inputTimes.size() == 1)
    	{
    		double[] result = new double[pointCount];
    		double singleValue = sortedValues.get(0);
    		Arrays.fill(result, singleValue);
    		List<String> timestamps = new ArrayList<>();
    		LocalDateTime current = start;
    		for(int i = 0; i < pointCount; i++)
    		{
    			timestamps.add(current.format(ft));
    			current = current.plusHours(disHour);
    		}
    		
    		return new InterpolationResult(timestamps, result);
    	}
    	
    	int size = inputTimes.size();
    	double[] x = new double[size];
    	double[] y = new double[size];
    	
    	for(int i = 0; i < size; i++)
    	{
    		x[i] = ChronoUnit.HOURS.between(start, inputTimes.get(i));
    		y[i] = sortedValues.get(i);
    	}
    	
    	LinearInterpolator interpolator = new LinearInterpolator();
    	PolynomialSplineFunction splineFunction = interpolator.interpolate(x, y);
    	double[] result = new double[pointCount];
    	List<String> timestamps = new ArrayList<>();
    	LocalDateTime current = start;
    	for(int i = 0; i < pointCount; i++)
    	{
    		double time = i * disHour;
    		timestamps.add(current.format(ft));
    		if(time >= x[0] && time <= x[size - 1])
    		{
    			result[i] = splineFunction.value(time);
    		}
    		else
    		{
    			result[i] = time < x[0] ? y[0] : y[size - 1];
    		}
    		current = current.plusHours(disHour);
    	}
    	
//    	for(int i = 0; i < pointCount; i++)
//    	{
//    		result[i] = NumberFormatUtil.numFormat(result[i], 1);
//    	}
    	
    	return new InterpolationResult(timestamps, result);
    }
    
    public static class InterpolationResult{
    	private final List<String> timestamps;
    	private final double[] values;
    	public InterpolationResult(List<String> timestamps, double[] values)
    	{
    		this.timestamps = timestamps;
    		this.values = values;
    	}
    	
    	public List<String> getTimestamps()
    	{
    		return timestamps;
    	}
    	
    	public double[] getValues()
    	{
    		return values;
    	}
    }
    
    public static void printInterpolationResults(InterpolationResult result)
    {
    	System.out.println("插值结果:");
    	List<String> timestamps = result.getTimestamps();
    	double[] values = result.getValues();
    	for(int i = 0; i < timestamps.size(); i++)
    	{
    		System.out.printf("时间:%s,值:%.2f%n", timestamps.get(i), values[i]);
    	}
    }
}
