package com.util;

import java.util.Arrays;

public class UniversalArrayInterpolator {

    /**
     * 将任意大小的一维数组插值为指定大小的新数组，保持原始数据点不变
     * 
     * @param original 原始数组
     * @param newSize 目标数组大小 (必须大于原始数组大小)
     * @return 插值后的新数组
     */
    public static double[] interpolateWithFixedPoints(double[] original, int newSize) {
        // 验证输入参数
        if (original == null || original.length < 2) {
            throw new IllegalArgumentException("原始数组长度至少为2");
        }
        if (newSize <= original.length) {
            throw new IllegalArgumentException("新数组大小必须大于原始数组大小");
        }
        
        // 计算原始点在新数组中的位置
        int[] originalPositions = calculateOriginalPositions(original.length, newSize);
        
        // 创建结果数组
        double[] result = new double[newSize];
        
        // 1. 复制原始数据到新数组的指定位置
        for (int i = 0; i < original.length; i++) {
            result[originalPositions[i]] = original[i];
        }
        
        // 2. 在每对原始点之间插入点
        for (int i = 0; i < original.length - 1; i++) {
            int startIndex = originalPositions[i];
            int endIndex = originalPositions[i + 1];
            double startValue = original[i];
            double endValue = original[i + 1];
            
            // 计算插入点数量
            int pointsToInsert = endIndex - startIndex - 1;
            
            // 计算每个插入点之间的步长
            double step = (endValue - startValue) / (pointsToInsert + 1.0);
            
            // 插入点
            for (int j = 1; j <= pointsToInsert; j++) {
                result[startIndex + j] = NumberFormatUtil.numFormat(startValue + j * step, 1);
            }
        }
        
        return result;
    }
    
    /**
     * 计算原始数据点在新数组中的位置
     * 
     * @param originalSize 原始数组大小
     * @param newSize 新数组大小
     * @return 位置索引数组
     */
    private static int[] calculateOriginalPositions(int originalSize, int newSize) {
        int[] positions = new int[originalSize];
        
        // 第一个点在位置0
        positions[0] = 0;
        
        // 最后一个点在位置newSize-1
        positions[originalSize - 1] = newSize - 1;
        
        // 计算基本间距和余数
        int totalPointsBetween = newSize - originalSize;
        int intervals = originalSize - 1;
        int basePointsPerInterval = totalPointsBetween / intervals;
        int remainder = totalPointsBetween % intervals;
        
        // 计算中间点的位置
        int currentPosition = 0;
        for (int i = 1; i < originalSize - 1; i++) {
            // 计算当前位置
            int pointsToAdd = basePointsPerInterval + (i <= remainder ? 1 : 0) + 1;
            currentPosition += pointsToAdd;
            positions[i] = currentPosition;
        }
        
        return positions;
    }
    
    /**
     * 验证原始数据点是否保持不变
     */
    private static void verifyOriginalPoints(double[] original, double[] interpolated, int[] positions) {
        System.out.println("\n验证原始数据点:");
        for (int i = 0; i < original.length; i++) {
            double origValue = original[i];
            double newValue = interpolated[positions[i]];
            System.out.printf("位置 %2d: 原始值 = %5.2f, 新值 = %5.2f -> %s%n",
                    positions[i], 
                    origValue, 
                    newValue,
                    Math.abs(origValue - newValue) < 1e-6 ? "匹配 ✓" : "不匹配 ✗");
        }
    }
    
    /**
     * 打印数组
     */
    public static void printArray(double[] array) {
        System.out.print("[");
        for (int i = 0; i < array.length; i++) {
            System.out.printf("%.2f", array[i]);
            if (i < array.length - 1) {
                System.out.print(", ");
            }
        }
        System.out.println("]");
    }

    public static void main(String[] args) {
        // 测试不同大小的数组
        double[] array5 = {10, 20, 15, 30, 25};
        double[] array3 = {5, 15, 10};
        double[] array7 = {2, 4, 6, 8, 10, 12, 14};
        
        System.out.println("测试1: 5个元素 → 11个元素");
        testInterpolation(array5, 11);
        
        System.out.println("\n测试2: 3个元素 → 10个元素");
        testInterpolation(array3, 10);
        
        System.out.println("\n测试3: 7个元素 → 20个元素");
        testInterpolation(array7, 20);
    }
    
    private static void testInterpolation(double[] original, int newSize) {
        System.out.println("原始数组 (" + original.length + "个元素):");
        printArray(original);
        
        // 计算插值
        double[] interpolated = interpolateWithFixedPoints(original, newSize);
        
        // 计算原始点位置
        int[] positions = calculateOriginalPositions(original.length, newSize);
        
        System.out.println("\n插值后数组 (" + newSize + "个元素):");
        printArray(interpolated);
        
        // 验证原始点
        verifyOriginalPoints(original, interpolated, positions);
        
        // 可视化
        System.out.println("\n可视化:");
        visualizeInterpolation(original, interpolated, positions);
    }
    
    /**
     * 可视化插值结果
     */
    private static void visualizeInterpolation(double[] original, double[] interpolated, int[] positions) {
        // 确定值范围
        double min = Arrays.stream(original).min().orElse(0);
        double max = Arrays.stream(original).max().orElse(1);
        double range = max - min;
        if (range == 0) range = 1;
        
        // 可视化新数组
        System.out.println("索引 | 值     | 图表");
        System.out.println("----|--------|" + "-");
        
        for (int i = 0; i < interpolated.length; i++) {
            // 检查是否是原始点
            boolean isOriginal = false;
            for (int pos : positions) {
                if (i == pos) {
                    isOriginal = true;
                    break;
                }
            }
            
            // 计算位置
            int position = (int) ((interpolated[i] - min) / range * 50);
            
            // 打印索引和值
            System.out.printf("%3d | %6.2f | ", i, interpolated[i]);
            
            // 打印图表
            for (int j = 0; j <= 50; j++) {
                if (j == position) {
                    System.out.print(isOriginal ? "★" : "●"); // 原始点用★，插值点用●
                } else {
                    System.out.print(" ");
                }
            }
            System.out.println();
        }
    }
}