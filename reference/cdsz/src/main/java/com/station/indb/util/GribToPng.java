package com.station.indb.util;

import com.alibaba.fastjson2.JSONObject;
import com.constants.DecodeConstants;
import com.util.AroundDataUtil;
import com.util.NumberFormatUtil;

import javax.imageio.ImageIO;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class GribToPng {
	
	private static GribToPng instance = new GribToPng();
	
	private DecimalFormat df =  new DecimalFormat("0.00000000");
	
	private GribToPng(){}
	
	public static GribToPng getInstance()
	{
		return instance;
	}
	
	public List<Object> png(String outPath, double[] datas, int width, int height, double lonMin, double latMin, double lonMax, double latMax, double lonStep, double latStep, String unit, int invalidValue)
	{
		List<Object> result = new ArrayList<>();
		BufferedImage bufImg = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
	    Graphics2D g2d = bufImg.createGraphics();
		java.awt.geom.Rectangle2D.Double r = new java.awt.geom.Rectangle2D.Double(0, 0, width, height);
        g2d.fill(r);
        String prefix = cn.hutool.core.io.FileUtil.getPrefix(outPath);
	    double min = 999999;
	    double max = -999999;
	    for(int i = 0, count = datas.length; i < count; i++)
	    {
	    	if(datas[i] == DecodeConstants.UNDEF_INT_VALUE)
	    	{
	    		continue;
	    	}
	    	if(prefix.toLowerCase().startsWith("ew") && datas[i] == -999)
			{
				continue;
			}
	    	if(min > datas[i])
	    	{
	    		min = datas[i];
	    	}
	    	if(max < datas[i])
	    	{
	    		max = datas[i];
	    	}
	    }
	    max = Double.parseDouble(scienceD(max));
	    min = Double.parseDouble(scienceD(min));
	    
	    int a = 255;
	    for(int i = 0; i < height; i++)
	    {
	    	for(int j = 0; j < width; j++)
	    	{
	    		if(datas[i * width + j] == 999999 || datas[i * width + j] == -999)
	    		{
	    			a = 0;
	    		}
	    		else
	    		{
	    			a = 255;
	    		}
	    		int rgb =  (a << 24) | (getRgbaValue(min, max, datas[i * width + j]) << 16) | (0 << 8) | 0;
	    		bufImg.setRGB(j, i, rgb);
	    	}
	    }
	    g2d.dispose();
		
	    File file = new File(outPath);
	    if(!file.exists())
	    {
	    	file.getParentFile().mkdirs();
	    }
	    try {
			ImageIO.write(bufImg, "png", file);
		} catch (Exception e) {
			e.printStackTrace();
		}
	    if(lonMin == 0 && lonMax == 360)
	    {
	    	lonMin = -180;
	    	lonMax = 180;
	    }
	    
	    String latStepStr = latStep + "";
	    if(prefix.toLowerCase().startsWith("soda_"))
	    {
	    	latStepStr = df.format(latStep);
	    }
	    
//	    String json = "{\"min\": " + min + ", \"max\": " + max + ", \"width\": " + width + ", \"height\": " + height + ", \"lonmin\": " + lonMin + ", \"latmin\": " 
//	    + latMin + ", \"lonmax\": " + lonMax + ", \"latmax\": " + latMax + ", \"lonstep\": " + lonStep + ", \"latstep\": " + latStepStr + ", \"unit\": \"" + unit + "\"}";
//	    FileUtil.writeStrToFile(json, outPath.replace(".png", ".json"));

		return result;
	}
	
	public List<Object> png(String outPath, double[][] datas, double lonMin, double latMin, double lonMax, double latMax, double lonStep, double latStep, String unit)
	{
		List<Object> result = new ArrayList<>();
		int height = datas.length;
		int width = datas[0].length;
		
//		height = height * 4;
//		width = width * 4;
		
		BufferedImage bufImg = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2d = bufImg.createGraphics();

		java.awt.geom.Rectangle2D.Double r = new java.awt.geom.Rectangle2D.Double(0, 0, width, height);
		g2d.fill(r);
//		String prefix = cn.hutool.core.io.FileUtil.getPrefix(outPath);
		double min = 999999;
		double max = -999999;

		datas = AroundDataUtil.bilinearInterpolation(datas, height, width);
		
		for(int i = 0 ; i < height; i++)
		{
			for(int j = 0; j < width; j++)
			{
				if(datas[i][j] == DecodeConstants.UNDEF_INT_VALUE || datas[i][j] < 0.1)
				{
					continue;
				}
				if(min > datas[i][j])
				{
					min = datas[i][j];
				}
				if(max < datas[i][j])
				{
					max = datas[i][j];
				}
			}
		}
		max = Double.parseDouble(NumberFormatUtil.scienceD(max));
	    min = Double.parseDouble(NumberFormatUtil.scienceD(min));
		
		int a = 255;
		for(int i = 0; i < height; i++)
		{
			for(int j = 0; j < width; j++)
			{
//				if(datas[i][j]== 999999 || datas[i][j] == -999 || datas[i][j] == 0)
//				{
//					a = 0;
//				}
//				else
//				{
//					a = 255;
//				}
//				int rgb =  (a << 24) | (getRgbaValue(min, max, datas[i][j]) << 16) | (0 << 8) | 0;
				int rgb = getRainRgbaValue(datas[i][j]);
				bufImg.setRGB(j, i, rgb);
			}
		}
		g2d.dispose();
		
		File file = new File(outPath);
		if(!file.exists())
		{
			file.getParentFile().mkdirs();
		}
		try {
			ImageIO.write(bufImg, "png", file);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(lonMin == 0 && lonMax == 360)
		{
			lonMin = -180;
			lonMax = 180;
		}
//		String json = "{\"min\": " + min + ", \"max\": " + max + ", \"width\": " + width + ", \"height\": " + height + ", \"lonmin\": " + lonMin + ", \"latmin\": " 
//				+ latMin + ", \"lonmax\": " + lonMax + ", \"latmax\": " + latMax + ", \"lonstep\": " + lonStep + ", \"latstep\": " + latStep + ", \"unit\": \"" + unit + "\"}";
//		FileUtil.writeStrToFile(json, outPath.replace(".png", ".json"));
		
		return result;
	}
	

	public List<Object> pngCombinePtype(String outPath, double[][] datas, double[][] ptypes, double lonMin, double latMin, double lonMax, double latMax, double lonStep, double latStep, String unit)
	{
		List<Object> result = new ArrayList<>();
		int height = datas.length;
		int width = datas[0].length;
		
//		height = height * 4;
//		width = width * 4;
		
		BufferedImage bufImg = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2d = bufImg.createGraphics();

		java.awt.geom.Rectangle2D.Double r = new java.awt.geom.Rectangle2D.Double(0, 0, width, height);
		g2d.fill(r);
//		String prefix = cn.hutool.core.io.FileUtil.getPrefix(outPath);
		double min = 999999;
		double max = -999999;
		
		
//		datas = AroundDataUtil.bilinearInterpolation(datas, height, width);
		int originalRows = ptypes.length;
		int originalCols = ptypes[0].length;
//		ptyes = AroundDataUtil.nearestInterpolation(ptyes, originalRows, originalCols, height, width);
//		FileUtil.writeStrToFile(JSONObject.toJSONString(ptyes), "E:/fl/datas/images/" + cn.hutool.core.io.FileUtil.getPrefix(outPath) + ".txt");
//		System.out.println("E:/fl/datas/images/" + cn.hutool.core.io.FileUtil.getPrefix(outPath) + ".txt");
		
		for(int i = 0 ; i < height; i++)
		{
			for(int j = 0; j < width; j++)
			{
				if(datas[i][j] == DecodeConstants.UNDEF_INT_VALUE || datas[i][j] < 0.1)
				{
					continue;
				}
				if(min > datas[i][j])
				{
					min = datas[i][j];
				}
				if(max < datas[i][j])
				{
					max = datas[i][j];
				}
			}
		}
		max = Double.parseDouble(NumberFormatUtil.scienceD(max));
	    min = Double.parseDouble(NumberFormatUtil.scienceD(min));
		
//		int a = 255;
		int rgb = 0;
		for(int i = 0; i < height; i++)
		{
			for(int j = 0; j < width; j++)
			{
				if(ptypes[i][j] == 2)
				{
					rgb = getSnowRgbaValue(datas[i][j]);
				}
				else if(ptypes[i][j] == 3)
				{
					rgb = getRainSnowRgbaValue(datas[i][j]);
				}
				else
				{
					rgb = getRainRgbaValue(datas[i][j]);
				}
				bufImg.setRGB(j, i, rgb);
			}
		}
		g2d.dispose();
		
		File file = new File(outPath);
		if(!file.exists())
		{
			file.getParentFile().mkdirs();
		}
		try {
			ImageIO.write(bufImg, "png", file);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
//		if(lonMin == 0 && lonMax == 360)
//		{
//			lonMin = -180;
//			lonMax = 180;
//		}
//		String json = "{\"min\": " + min + ", \"max\": " + max + ", \"width\": " + width + ", \"height\": " + height + ", \"lonmin\": " + lonMin + ", \"latmin\": " 
//				+ latMin + ", \"lonmax\": " + lonMax + ", \"latmax\": " + latMax + ", \"lonstep\": " + lonStep + ", \"latstep\": " + latStep + ", \"unit\": \"" + unit + "\"}";
//		FileUtil.writeStrToFile(json, outPath.replace(".png", ".json"));
		
		return result;
	}

	public List<Object> png(String outPath, double[] datas, double[] datas2, int width, int height, double lonMin, double latMin, double lonMax, double latMax, double lonStep, double latStep, String unit)
	{
		List<Object> result = new ArrayList<>();
		BufferedImage bufImg = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2d = bufImg.createGraphics();
		java.awt.geom.Rectangle2D.Double r = new java.awt.geom.Rectangle2D.Double(0, 0, width, height);
        g2d.fill(r);
        String prefix = cn.hutool.core.io.FileUtil.getPrefix(outPath);
		double[] maxMin = maxMin(prefix, datas);
		double[] maxMin2 = maxMin(prefix, datas2);

		int a = 255;
		for(int i = 0; i < height; i++)
		{
			for(int j = 0; j < width; j++)
			{
				if(datas[i * width + j] == 999999 || datas[i * width + j] == -999)
	    		{
	    			a = 0;
	    		}
	    		else
	    		{
	    			a = 255;
	    		}
				int rgb =  (a << 24) | (getRgbaValue(maxMin[1], maxMin[0], datas[i * width + j]) << 16) | (getRgbaValue(maxMin2[1], maxMin2[0], datas2[i * width + j]) << 8) | 0;
				bufImg.setRGB(j, i, rgb);
			}
		}
		g2d.dispose();

	    File file = new File(outPath);
	    if(!file.exists())
	    {
	    	file.getParentFile().mkdirs();
	    }
	    try {
			ImageIO.write(bufImg, "png", file);
		} catch (IOException e) {
			e.printStackTrace();
		}

	    if(lonMin == 0 && lonMax == 360)
	    {
	    	lonMin = -180;
	    	lonMax = 180;
	    }
	    String json = "{\"umin\": " + maxMin[1] + ", \"umax\": " + maxMin[0] + ", \"vmin\": " + maxMin2[1] + ", \"vmax\": " + maxMin2[0] + ", \"width\": " + width + ", \"height\": " 
	            + height + ", \"lonmin\": " + lonMin + ", \"latmin\": " + latMin + ", \"lonmax\": " + lonMax + ", \"latmax\": " + latMax + ", \"lonstep\": " + lonStep + ", \"latstep\": " 
	    		+ latStep + ", \"unit\": \"" + unit + "\"}";
	    FileUtil.writeStrToFile(json, outPath.replace(".png", ".json"));

		return result;
	}
	
	public List<Object> png(String outPath, double[][] datas, double[][] datas2, double lonMin, double latMin, double lonMax, double latMax, double lonStep, double latStep, String unit)
	{
		List<Object> result = new ArrayList<>();
		int height = datas.length;
		int width = datas[0].length;
		BufferedImage bufImg = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2d = bufImg.createGraphics();
		java.awt.geom.Rectangle2D.Double r = new java.awt.geom.Rectangle2D.Double(0, 0, width, height);
		g2d.fill(r);
		double[] maxMin = maxMin(datas);
		double[] maxMin2 = maxMin(datas2);
		int a = 255;
		for(int i = 0; i < height; i++)
		{
			for(int j = 0; j < width; j++)
			{
				if(datas[i][j] == 999999)
				{
					a = 0;
				}
				else
				{
					a = 255;
				}
				int rgb =  (a << 24) | (getRgbaValue(maxMin[1], maxMin[0], datas[i][j]) << 16) | (getRgbaValue(maxMin2[1], maxMin2[0], datas2[i][j]) << 8) | 0;
				bufImg.setRGB(j, i, rgb);
			}
		}
		g2d.dispose();
		
		File file = new File(outPath);
		if(!file.exists())
		{
			file.getParentFile().mkdirs();
		}
		try {
			ImageIO.write(bufImg, "png", file);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(lonMin == 0 && lonMax == 360)
		{
			lonMin = -180;
			lonMax = 180;
		}
		String json = "{\"umin\": " + maxMin[1] + ", \"umax\": " + maxMin[0] + ", \"vmin\": " + maxMin2[1] + ", \"vmax\": " + maxMin2[0] + ", \"width\": " + width + ", \"height\": " 
				+ height + ", \"lonmin\": " + lonMin + ", \"latmin\": " + latMin + ", \"lonmax\": " + lonMax + ", \"latmax\": " + latMax + ", \"lonstep\": " + lonStep + ", \"latstep\": " 
				+ latStep + ", \"unit\": \"" + unit + "\"}";
		FileUtil.writeStrToFile(json, outPath.replace(".png", ".json"));
		
		return result;
	}

	private int getRainRgbaValue(double value)
	{
		int rgb = 0;
		int a = 255;
		if(value == 999999)
		{
			a = 0;
		}
		else
		{
			a = 255;
		}
		int r = 255;
		int g = 255;
		int b = 255;
		if(value < 0.1)
		{
			a = 0;
		}
		else if(value >= 0.1 && value < 10)
		{
			r = 170;
			g = 240;
			b = 144;
		}
		else if(value >= 10 && value < 25)
		{
			r = 61;
			g = 172;
			b = 5;
		}
		else if(value >= 25 && value < 50)
		{
			r = 104;
			g = 185;
			b = 249;
		}
		else if(value >= 50 && value < 100)
		{
			r = 5;
			g = 4;
			b = 247;
		}
		else if(value >= 100 && value < 250)
		{
			r = 253;
			g = 4;
			b = 255;
		}
		else if(value >= 250)
		{
			r = 253;
			g = 4;
			b = 255;
		}
		rgb = (a << 24) | r << 16 | g << 8 | b;

		return rgb;
	}
	
	private int getSnowRgbaValue(double value)
	{
		int rgb = 0;
		int a = 255;
		if(value == 999999)
		{
			a = 0;
		}
		else
		{
			a = 255;
		}
		int r = 255;
		int g = 255;
		int b = 255;
		if(value < 0.1)
		{
			a = 0;
		}
		else if(value >= 0.1 && value < 2.5)
		{ 
			r = 209;
			g = 210;
			b = 211;
		}
		else if(value >= 2.5 && value < 5)
		{
			r = 166;
			g = 167;
			b = 168;
		}
		else if(value >= 5 && value < 10)
		{
			r = 117;
			g = 118;
			b = 119;
		}
		else if(value >= 10 && value < 20)
		{
			r = 77;
			g = 78;
			b = 79;
		}
		else if(value >= 20 && value < 30)
		{
			r = 77;
			g = 78;
			b = 79;
		}
		else if(value >= 30)
		{
			r = 77;
			g = 78;
			b = 79;
		}
		rgb = (a << 24) | r << 16 | g << 8 | b;

		return rgb;
	}
	
	private int getRainSnowRgbaValue(double value)
	{
		int rgb = 0;
		int a = 255;
		if(value == 999999)
		{
			a = 0;
		}
		else
		{
			a = 255;
		}
		int r = 251;
		int g = 201;
		int b = 252;
		
		rgb = (a << 24) | r << 16 | g << 8 | b;
		
		return rgb;
	}

	private int getRgbaValue(double min, double max, double value)
	{
		if(value == DecodeConstants.UNDEF_INT_VALUE || Double.isNaN(value) || value == -999)
		{
			return 0;
		}
		return (int) (Math.abs((value - min) / (max - min)) * 255);
	}

	private double[] maxMin(String prefix, double[] datas)
	{
		double min = 999999;
		double max = -999999;
		for(int i = 0, count = datas.length; i < count; i++)
		{
			if(datas[i] == DecodeConstants.UNDEF_INT_VALUE)
			{
				continue;
			}
			if(prefix.toLowerCase().startsWith("ew") && datas[i] == -999)
			{
				continue;
			}
			if(min > datas[i])
			{
				min = datas[i];
			}
			if(max < datas[i])
			{
				max = datas[i];
			}
		}
		max = Double.parseDouble(NumberFormatUtil.scienceD(max));
	    min = Double.parseDouble(NumberFormatUtil.scienceD(min));

		return new double[]{max, min};
	}
	
	private double[] maxMin(double[][] datas)
	{
		double min = 999999;
		double max = -999999;
		for(int i = 0, height = datas.length; i < height; i++)
		{
			for(int j = 0, width = datas[i].length; j < width; j++)
			{
				if(datas[i][j] == DecodeConstants.UNDEF_INT_VALUE)
				{
					continue;
				}
				if(min > datas[i][j])
				{
					min = datas[i][j];
				}
				if(max < datas[i][j])
				{
					max = datas[i][j];
				}
			}
		}
		
		return new double[]{max, min};
	}
	
	 public static String scienceD(double num) {
		NumberFormat nf = NumberFormat.getNumberInstance();
		//设置保留多少为小数
		nf.setMaximumFractionDigits(5);
		//取消科学计数法
		nf.setGroupingUsed(false);


		return nf.format(num);
	}
}
