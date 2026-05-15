package com.util;

public class BilinearInterpolationForTwoArrays {
    // 双线性插值计算函数
    private static double bilinearInterpolate(double q00_1, double q01_1, double q10_1, double q11_1,
                                             double q00_2, double q01_2, double q10_2, double q11_2, double t) {
        // 对第一个数组进行双线性插值
        double r1_1 = (1 - t) * q00_1 + t * q10_1;
        double r2_1 = (1 - t) * q01_1 + t * q11_1;
        double value1 = (1 - t) * r1_1 + t * r2_1;

        // 对第二个数组进行双线性插值
        double r1_2 = t * q00_2 + (1 - t)* q10_2;
        double r2_2 = t * q01_2 + (1 - t) * q11_2;
        double value2 = t * r1_2 + (1 - t) * r2_2;

        // 在两个双线性插值结果之间进行线性插值
        double value = (1 - t) * value1 + t * value2;
        double result = Double.parseDouble(NumberFormatUtil.scienceD(value, 3));

        return result;
    }

    /**
     * @category 在两个二维数组之间插值出指定个数的二维数组
     * @date 2025/2/21 8:16
     * @param array1
     * @param array2
     * @param t
     * @return double[][][]
     */
    public static double[][][] generateInterpolatedArrays(double[][] array1, double[][] array2, double[] t) {
        int rows = array1.length;
        int cols = array1[0].length;
        int count = t.length;
        double[][][] result = new double[count][rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                // 获取第一个数组当前位置及其相邻位置的元素值，处理边界情况
                double q00_1 = array1[i][j];
                double q01_1 = (j < cols - 1)? array1[i][j + 1] : array1[i][j];
                double q10_1 = (i < rows - 1)? array1[i + 1][j] : array1[i][j];
                double q11_1 = (i < rows - 1 && j < cols - 1)? array1[i + 1][j + 1] : array1[i][j];

//                if (i == 0 && j == 0) {
//                    System.out.println("At boundary position (0, 0), expected q10_1 = " + array1[1][0] + ", actual q10_1 = " + q10_1);
//                    System.out.println("At boundary position (0, 0), expected q11_1 = " + array1[1][1] + ", actual q11_1 = " + q11_1);
//                }

                // 获取第二个数组当前位置及其相邻位置的元素值，处理边界情况
                double q00_2 = array2[i][j];
                double q01_2 = (j < cols - 1)? array2[i][j + 1] : array2[i][j];
                double q10_2 = (i < rows - 1)? array2[i + 1][j] : array2[i][j];
                double q11_2 = (i < rows - 1 && j < cols - 1)? array2[i + 1][j + 1] : array2[i][j];

                for(int k = 0; k < count; k++)
                {
                    // 生成双线性插值数组
                    result[k][i][j] = bilinearInterpolate(q00_1, q01_1, q10_1, q11_1,  q00_2, q01_2, q10_2, q11_2, t[k]);
//                    // 生成第二个双线性插值数组
//                    result[1][i][j] = bilinearInterpolate(q00_1, q01_1, q10_1, q11_1, q00_2, q01_2, q10_2, q11_2, t2);
                }
            }
        }
        return result;
    }
}
