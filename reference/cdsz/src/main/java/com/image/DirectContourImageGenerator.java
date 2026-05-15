package com.image;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

import javax.imageio.ImageIO;

import com.util.GribUtil;

/**
 * 直接使用已有的等值线数据结构生成图片
 * 适用于已经计算好等值线数据（Map<Double, List<List<double[]>>>）的情况
 */
public class DirectContourImageGenerator {
    // 图片尺寸
    private int imageWidth = 1000;
    private int imageHeight = 800;
    
    // 边距
    private int margin = 50;
    
    // 网格尺寸
    private int gridWidth;
    private int gridHeight;
    
    // 数据
    private double[][] gridData;
    private Map<Double, List<List<double[]>>> contourLines;
    private double minValue;
    private double maxValue;
    
    // 颜色方案
    private static final Color[][] COLOR_SCHEMES = {
        // 蓝-红渐变
        {new Color(0, 0, 255), new Color(0, 128, 255), new Color(0, 255, 255), 
         new Color(255, 255, 0), new Color(255, 128, 0), new Color(255, 0, 0)},
        // 蓝-绿-红渐变
        {new Color(0, 0, 255), new Color(0, 255, 0), new Color(255, 255, 0), 
         new Color(255, 128, 0), new Color(255, 0, 0)},
        // Viridis渐变
        {new Color(68, 1, 84), new Color(59, 82, 139), new Color(33, 144, 140), 
         new Color(94, 201, 98), new Color(253, 231, 37), new Color(255, 255, 255)},
        // Inferno渐变
        {new Color(0, 0, 4), new Color(78, 43, 115), new Color(177, 35, 100), 
         new Color(253, 101, 33), new Color(254, 217, 56), new Color(255, 255, 255)}
    };
    
    /**
     * 构造函数
     * @param gridData 原始网格数据
     * @param contourLines 已计算好的等值线数据
     */
    public DirectContourImageGenerator(double[][] gridData, Map<Double, List<List<double[]>>> contourLines) {
        this.gridData = gridData;
        this.contourLines = contourLines;
        this.gridWidth = gridData.length;
        this.gridHeight = gridData[0].length;
        calculateMinMaxValues();
    }
    
    /**
     * 计算数据的最小值和最大值
     */
    private void calculateMinMaxValues() {
        if (gridData == null || gridData.length == 0) {
            minValue = 0;
            maxValue = 1;
            return;
        }
        
        minValue = Double.MAX_VALUE;
        maxValue = Double.MIN_VALUE;
        
        for (double[] row : gridData) {
            for (double value : row) {
                if (value < minValue) minValue = value;
                if (value > maxValue) maxValue = value;
            }
        }
        System.out.println("maxValue: " + maxValue + ", minValue: " + minValue);
    }
    
    /**
     * 生成等值面图片
     * @param outputPath 输出文件路径
     * @param colorSchemeIndex 颜色方案索引
     * @param drawContourLines 是否绘制等值线
     * @param drawLabels 是否绘制标签
     */
    public void generateImage(String outputPath, int colorSchemeIndex, 
                             boolean drawContourLines, boolean drawLabels) {
        // 创建缓冲图像
        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        // 开启抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        
        // 绘制背景
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, imageWidth, imageHeight);
        
        if (gridData == null || gridData.length == 0) {
            saveImage(image, outputPath);
            return;
        }
        
        // 计算网格尺寸
        int width = imageWidth - 2 * margin - 100; // 预留图例空间
        int height = imageHeight - 2 * margin;
        
        double cellWidth = (double) width / (gridWidth - 1);
        double cellHeight = (double) height / (gridHeight - 1);
        
        // 绘制填充区域
        drawFilledContours(g2d, margin, margin, cellWidth, cellHeight, colorSchemeIndex);
        
        // 绘制等值线
        if (drawContourLines && contourLines != null) {
            drawContourLines(g2d, margin, margin, cellWidth, cellHeight, drawLabels);
        }
        
        // 绘制坐标轴
//        drawAxes(g2d, margin, margin, width, height);
        
        // 绘制图例
        drawLegend(g2d, margin + width + 20, margin, 20, height, colorSchemeIndex);
        
        // 保存图片
        saveImage(image, outputPath);
        
        // 释放资源
        g2d.dispose();
    }
    
    /**
     * 绘制填充的等值面
     */
    private void drawFilledContours(Graphics2D g2d, int xOffset, int yOffset, 
                                   double cellWidth, double cellHeight, int colorSchemeIndex) {
        // 获取颜色方案
        Color[] colors = COLOR_SCHEMES[Math.min(colorSchemeIndex, COLOR_SCHEMES.length - 1)];
        
        // 绘制每个网格单元
        for (int i = 0; i < gridWidth - 1; i++) {
            for (int j = 0; j < gridHeight - 1; j++) {
                // 获取四个顶点的值
                double v0 = gridData[i][j];
                double v1 = gridData[i+1][j];
                double v2 = gridData[i+1][j+1];
                double v3 = gridData[i][j+1];
                
                // 计算四个顶点的颜色
                Color c0 = getColorForValue(v0, minValue, maxValue, colors);
                Color c1 = getColorForValue(v1, minValue, maxValue, colors);
                Color c2 = getColorForValue(v2, minValue, maxValue, colors);
                Color c3 = getColorForValue(v3, minValue, maxValue, colors);
                
                // 绘制渐变矩形
                drawGradientQuadrilateral(g2d, 
                    xOffset + i * cellWidth, yOffset + j * cellHeight,
                    xOffset + (i+1) * cellWidth, yOffset + j * cellHeight,
                    xOffset + (i+1) * cellWidth, yOffset + (j+1) * cellHeight,
                    xOffset + i * cellWidth, yOffset + (j+1) * cellHeight,
                    c0, c1, c2, c3);
            }
        }
    }
    
    /**
     * 绘制渐变四边形
     */
    private void drawGradientQuadrilateral(Graphics2D g2d, 
                                          double x1, double y1, double x2, double y2,
                                          double x3, double y3, double x4, double y4,
                                          Color c1, Color c2, Color c3, Color c4) {
        // 创建路径
        GeneralPath path = new GeneralPath();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        path.lineTo(x3, y3);
        path.lineTo(x4, y4);
        path.closePath();
        
        // 创建渐变
        Rectangle2D bounds = path.getBounds2D();
        GradientPaint gradient = new GradientPaint(
            (float) bounds.getMinX(), (float) bounds.getMinY(), c1,
            (float) bounds.getMaxX(), (float) bounds.getMaxY(), c3
        );
        
        g2d.setPaint(gradient);
        g2d.fill(path);
    }
    
    /**
     * 绘制等值线
     */
    private void drawContourLines(Graphics2D g2d, int xOffset, int yOffset, 
                                 double cellWidth, double cellHeight, boolean drawLabels) {
        if (contourLines == null || contourLines.isEmpty()) {
            return;
        }
        
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1));
        
        // 绘制每条等值线
        for (Map.Entry<Double, List<List<double[]>>> entry : contourLines.entrySet()) {
            double level = entry.getKey();
            List<List<double[]>> lines = entry.getValue();
            
            for (List<double[]> line : lines) {
                GeneralPath path = new GeneralPath();
                
                if (line.isEmpty()) continue;
                
                // 移动到第一个点
                double[] first = line.get(0);
                path.moveTo(
                    xOffset + first[0] * cellWidth,
                    yOffset + first[1] * cellHeight
                );
                
                // 添加其他点
                for (int i = 1; i < line.size(); i++) {
                    double[] p = line.get(i);
                    path.lineTo(
                        xOffset + p[0] * cellWidth,
                        yOffset + p[1] * cellHeight
                    );
                }
                
                // 绘制路径
                g2d.draw(path);
                
                // 绘制标签
                if (drawLabels) {
                    drawContourLabel(g2d, line, level, xOffset, yOffset, cellWidth, cellHeight);
                }
            }
        }
    }
    
    /**
     * 绘制等值线标签
     */
    private void drawContourLabel(Graphics2D g2d, List<double[]> line, double level, 
                                 int xOffset, int yOffset, double cellWidth, double cellHeight) {
        if (line.size() < 5) return; // 跳过短线段
        
        // 获取中间点
        int midIndex = line.size() / 2;
        double[] midPoint = line.get(midIndex);
        
        double x = xOffset + midPoint[0] * cellWidth;
        double y = yOffset + midPoint[1] * cellHeight;
        
        // 绘制标签背景
        String label = String.format("%.1f", level);
        FontMetrics metrics = g2d.getFontMetrics();
        int labelWidth = metrics.stringWidth(label);
        int labelHeight = metrics.getHeight();
        
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.fillRect(
            (int) x - labelWidth / 2 - 2,
            (int) y - labelHeight / 2 - 2,
            labelWidth + 4,
            labelHeight + 4
        );
        
        // 绘制标签文本
        g2d.setColor(Color.BLACK);
        g2d.drawString(label, (int) x - labelWidth / 2, (int) y + labelHeight / 4);
    }
    
    /**
     * 绘制坐标轴
     */
    private void drawAxes(Graphics2D g2d, int xOffset, int yOffset, int width, int height) {
        int axisLength = 10;
        
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1));
        
        // X轴
        g2d.drawLine(xOffset, yOffset + height, xOffset + width, yOffset + height);
        
        // X轴箭头
        g2d.drawLine(xOffset + width, yOffset + height, 
                    xOffset + width - axisLength, yOffset + height - axisLength / 2);
        g2d.drawLine(xOffset + width, yOffset + height, 
                    xOffset + width - axisLength, yOffset + height + axisLength / 2);
        
        // Y轴
        g2d.drawLine(xOffset, yOffset, xOffset, yOffset + height);
        
        // Y轴箭头
        g2d.drawLine(xOffset, yOffset, 
                    xOffset - axisLength / 2, yOffset + axisLength);
        g2d.drawLine(xOffset, yOffset, 
                    xOffset + axisLength / 2, yOffset + axisLength);
        
        // 绘制标签
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.drawString("X (km)", xOffset + width / 2 - 20, yOffset + height + 30);
        
        // 旋转绘制Y轴标签
//        g2d.save();
        g2d.translate(xOffset - 35, yOffset + height / 2);
        g2d.rotate(-Math.PI / 2);
        g2d.drawString("Y (km)", 0, 0);
//        g2d.restore();
        
        // 绘制刻度
        int tickCount = 5;
        double xTickStep = (double) width / (tickCount - 1);
        double yTickStep = (double) height / (tickCount - 1);
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        
        // X轴刻度
        for (int i = 0; i < tickCount; i++) {
            double x = xOffset + i * xTickStep;
            g2d.drawLine((int) x, yOffset + height, (int) x, yOffset + height + 5);
            g2d.drawString(String.format("%.1f", i * (width / (tickCount - 1)) / 100), 
                          (int) x - 15, yOffset + height + 20);
        }
        
        // Y轴刻度
        for (int i = 0; i < tickCount; i++) {
            double y = yOffset + i * yTickStep;
            g2d.drawLine(xOffset - 5, (int) y, xOffset, (int) y);
            g2d.drawString(String.format("%.1f", i * (height / (tickCount - 1)) / 100), 
                          xOffset - 40, (int) y + 5);
        }
    }
    
    /**
     * 绘制图例
     */
    private void drawLegend(Graphics2D g2d, int x, int y, int width, int height, int colorSchemeIndex) {
        // 获取颜色方案
        Color[] colors = COLOR_SCHEMES[Math.min(colorSchemeIndex, COLOR_SCHEMES.length - 1)];
        
        // 绘制渐变条
        for (int i = 0; i < height; i++) {
            double ratio = (double) i / height;
            Color color = getColorForValue(minValue + ratio * (maxValue - minValue), 
                                         minValue, maxValue, colors);
            g2d.setColor(color);
            g2d.drawLine(x, y + i, x + width, y + i);
        }
        
        // 绘制边框
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x, y, width, height);
        
        // 绘制标签
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString(String.format("%.1f", maxValue), x + width + 5, y + 15);
        g2d.drawString(String.format("%.1f", minValue), x + width + 5, y + height - 5);
        g2d.drawString("降水量", x + width + 5, y + height / 2 - 10);
        g2d.drawString("(mm)", x + width + 5, y + height / 2 + 10);
    }
    
    /**
     * 根据值获取颜色
     */
//    private Color getColorForValue(double value, double min, double max, Color[] colors) {
//        // 归一化值
//        double ratio = (value - min) / (max - min);
//        
//        // 查找颜色区间
//        for (int i = 0; i < colors.length - 1; i++) {
//            double r1 = (double) i / (colors.length - 1);
//            double r2 = (double) (i + 1) / (colors.length - 1);
//            
//            if (ratio >= r1 && ratio <= r2) {
//                // 计算区间内的比例
//                double t = (ratio - r1) / (r2 - r1);
//                
//                // 线性插值
//                int r = (int) (colors[i].getRed() + t * (colors[i+1].getRed() - colors[i].getRed()));
//                int g = (int) (colors[i].getGreen() + t * (colors[i+1].getGreen() - colors[i].getGreen()));
//                int b = (int) (colors[i].getBlue() + t * (colors[i+1].getBlue() - colors[i].getBlue()));
//                
//                return new Color(r, g, b);
//            }
//        }
//        
//        // 默认返回最后一个颜色
//        return colors[colors.length - 1];
//    }
    
    private Color getColorForValue(double value, double min, double max, Color[] colors)
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

		return new Color(r, g, b, 200);
	}
    
    /**
     * 保存图片
     */
    private void saveImage(BufferedImage image, String outputPath) {
        try {
            File outputFile = new File(outputPath);
            
            // 创建父目录
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            // 保存图片
            ImageIO.write(image, "png", outputFile);
            System.out.println("图片已保存到: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("保存图片失败: " + e.getMessage());
        }
    }
    
    /**
     * 设置图片尺寸
     */
    public void setImageSize(int width, int height) {
        this.imageWidth = width;
        this.imageHeight = height;
    }
    
    /**
     * 设置边距
     */
    public void setMargin(int margin) {
        this.margin = margin;
    }
    
    /**
     * 使用示例：直接处理已有的等值线数据
     */
    public static void main(String[] args) {
        System.out.println("使用已有的等值线数据生成图片...");
        
        try {
            // 1. 准备示例数据
            int gridSize = 30;
            
//             1.1 生成原始网格数据
//            double[][] gridData = generateSampleGridData(gridSize, gridSize);
//
//            // 1.2 生成等值线数据（Map<Double, List<List<double[]>>>）
//            Map<Double, List<List<double[]>>> contourLines = generateSampleContourLines(gridData);

            ContourTool tool = new ContourTool();
            double[] levelValues = new double[] { 0.1, 10, 25, 50, 100, 250};
            double[] prjValues = new double[] { 70, 50, 0.05, 0.05 };
            String filePath = "E:/fl/导入20251117/tp_999_deeplearning_2025111720_024.sql";
            double[][] values = GribUtil.readGribDatasFromTxt(filePath, ",");
            Map<Double, List> contourLines = tool.contourLine(values, levelValues, prjValues, -1);

            Map<Double, List<List<double[]>>> linesMap = new HashMap<>();
            for(Double d : contourLines.keySet())
            {
                List list = contourLines.get(d);
                linesMap.put(d, list);
            }
            
            // 2. 创建直接图片生成器
            DirectContourImageGenerator generator = new DirectContourImageGenerator(values, linesMap);
            
            // 3. 设置图片尺寸
            generator.setImageSize(1201, 900);
            generator.setMargin(60);
            
            // 4. 生成不同风格的图片
            String[] colorSchemes = {"blue-red", "blue-green-red", "viridis", "inferno"};
            
            for (int i = 0; i < colorSchemes.length; i++) {
                String outputPath = String.format("direct_contour_%s.png", colorSchemes[i]);
                
                // 生成图片
                generator.generateImage(
                    outputPath,       // 输出路径
                    i,                // 颜色方案索引
                    true,             // 绘制等值线
                    true              // 绘制标签
                );
            }
            
            // 5. 生成无等值线的填充图
            generator.generateImage(
                "direct_contour_fill_only.png",
                0,                // 蓝-红颜色方案
                false,            // 不绘制等值线
                false             // 不绘制标签
            );
            
            System.out.println("所有图片生成完成！");
            
        } catch (Exception e) {
            System.err.println("生成图片时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 生成示例网格数据
     */
    private static double[][] generateSampleGridData(int width, int height) {
        double[][] grid = new double[width][height];
        
        // 创建两个降水中心
        double center1X = width * 0.3;
        double center1Y = height * 0.3;
        double radius1 = width * 0.2;
        
        double center2X = width * 0.7;
        double center2Y = height * 0.7;
        double radius2 = width * 0.25;
        
        // 生成网格数据
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                // 第一个中心
                double dx1 = i - center1X;
                double dy1 = j - center1Y;
                double distance1 = Math.sqrt(dx1 * dx1 + dy1 * dy1);
                double gaussian1 = Math.exp(-(distance1 * distance1) / (2 * radius1 * radius1));
                
                // 第二个中心
                double dx2 = i - center2X;
                double dy2 = j - center2Y;
                double distance2 = Math.sqrt(dx2 * dx2 + dy2 * dy2);
                double gaussian2 = Math.exp(-(distance2 * distance2) / (2 * radius2 * radius2));
                
                // 组合结果
                grid[i][j] = (gaussian1 * 0.8 + gaussian2 * 0.7) * 50.0;
            }
        }
        
        return grid;
    }
    
    /**
     * 生成示例等值线数据
     */
    private static Map<Double, List<List<double[]>>> generateSampleContourLines(double[][] grid) {
        Map<Double, List<List<double[]>>> contourMap = new HashMap<>();
        int width = grid.length;
        int height = grid[0].length;
        
        // 计算数据范围
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (double[] row : grid) {
            for (double value : row) {
                if (value < min) min = value;
                if (value > max) max = value;
            }
        }
        
        // 生成等值线级别
        int contourCount = 8;
        double step = (max - min) / contourCount;
        
        // 为每个级别生成简单的矩形等值线（仅用于演示）
        for (int levelIndex = 1; levelIndex <= contourCount; levelIndex++) {
            double level = min + levelIndex * step;
            List<List<double[]>> lines = new ArrayList<>();
            
            // 计算级别对应的半径比例
            double ratio = 1.0 - (double) levelIndex / contourCount;
            double center1X = width * 0.3;
            double center1Y = height * 0.3;
            double radius1 = width * 0.2 * ratio * 1.5;
            
            double center2X = width * 0.7;
            double center2Y = height * 0.7;
            double radius2 = width * 0.25 * ratio * 1.5;
            
            // 生成第一个中心的等值线
            List<double[]> circle1 = generateCircle(center1X, center1Y, radius1, 100);
            lines.add(circle1);
            
            // 生成第二个中心的等值线
            List<double[]> circle2 = generateCircle(center2X, center2Y, radius2, 100);
            lines.add(circle2);
            
            contourMap.put(level, lines);
        }
        
        return contourMap;
    }
    
    /**
     * 生成圆的点列表
     */
    private static List<double[]> generateCircle(double centerX, double centerY, double radius, int points) {
        List<double[]> circlePoints = new ArrayList<>();
        
        for (int i = 0; i <= points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);
            circlePoints.add(new double[]{x, y});
        }
        
        return circlePoints;
    }
}
