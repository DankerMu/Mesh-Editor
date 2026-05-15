package com.util;

import cn.hutool.core.util.ArrayUtil;

import java.util.Arrays;

public class ArrayReverse {

    /**
     * @category 将二维数组上下对调
     * @date 2025/2/12 18:15
     * @param datas
     * @return double[][]
     */
    public static double[][] transLevelLine(double[][] datas)
    {
        int count = datas.length;
        int ii = datas[0].length;
        double[][] result = new double[count][ii];
        for(int i = 0; i < count; i++)
        {
            for(int j = 0; j < ii; j++)
            {
                result[i][j] = datas[count - i - 1][j];
            }
        }

        return result;
    }

    /**
     * @category 将一维数组中的数据前后颠倒
     * @date 2025/2/12 17:23
     * @param datas
     * @return double[]
     */
    public static double[] reverse(double[] datas)
    {
        double[] result = new double[datas.length];
        for(int i = 0, count = datas.length; i < count; i++)
        {
            result[i] = datas[count - 1 - i];
        }


        return result;
    }

    public static float[][] transReverse(float[][] datas)
    {
        int line = datas.length;
        int col = datas[0].length;
        float[][] result = new float[col][line];
        for(int i = 0; i < line; i++)
        {
            for(int j = 0; j < col; j++)
            {
                result[j][line - 1 - i] = datas[i][j];
            }
        }

        return result;
    }

    /**
     * 二维数组转一维数组
     * @param datas
     * @return
     */
    public static float[] transToOneArray(float[][] datas) {
        int count = datas.length;
        int ii = datas[0].length;
        float[] result = new float[count * ii];
        for (int i = 0; i < count; i++) {
            for (int j = 0, num = datas[i].length; j < num; j++) {
                result[i * num + j] = datas[i][j];
            }

        }


        return result;
    }

    public static float[] trans(float[] datas, int width, int height)
    {
        float[] result = new float[width * height];
        for(int i = width - 1; i >= 0; i--)
        {
            for(int j = 0; j < height; j++)
            {
                result[(width - i - 1) * height + j] = datas[i * height + j];
            }
        }

        return result;
    }

    /**
     * @category: 将数组的数据上下交换
     * @Date: 2024/6/7 10:02
     * @param: datas
     * @param: width
     * @param: height
     * @return: float[]
     **/
    public static float[] transDatasUpDown(float[] datas, int width, int height)
    {
        float[] result = new float[width * height];
        for(int i = height - 1; i >= 0; i--)
        {
            for(int j = 0; j < width; j++)
            {
                result[(height - i - 1) * width + j] = datas[i * width + j];
            }
        }

        return result;
    }



    public static float[] trans(float[][] datas)
    {
        int count = datas.length;
        int ii = datas[0].length;
        float[] result = new float[count * ii];
        for(int i = count - 1; i >= 0; i--)
        {
            for(int j = 0, num = datas[i].length; j < num; j++)
            {
                result[(count - i - 1) * num + j] = datas[i][j];
            }
        }

        return result;
    }

    public static float[] trans(double[][] datas)
    {
        int count = datas.length;
        int ii = datas[0].length;
        float[] result = new float[count * ii];
        for(int i = count - 1; i >= 0; i--)
        {
            for(int j = 0, num = datas[i].length; j < num; j++)
            {
                result[(count - i - 1) * num + j] = (float) datas[i][j];
            }
        }

        return result;
    }

    public static float[] trans_reverse(float[][] datas, int index)
    {
        int count = datas.length;
        int ii = datas[0].length;
        float[] result = new float[count * ii];
        float temp;
        for(int i = count - 1; i >= 0; i--)
        {
            for(int j = 0, num = datas[i].length; j < num; j++)
            {
                result[(count - i - 1) * num + j] = datas[i][j];
            }
            for(int j = 0, num = datas[i].length; j < index; j++)
            {
                temp = result[(count - i - 1) * num + j];
                result[(count - i - 1) * num + j] = result[(count - i - 1) * num + (num - index) + j];
                result[(count - i - 1) * num + (num - index) + j] = temp;
                result[(count - i - 1) * num + index] = result[(count - i - 1) * num + (num - index)];
            }
        }

        return result;
    }

    /**
     * @category 将二维数组左右对调
     * @date 2025/2/13 8:20
     * @param datas
     * @param index
     * @return float[]
     */
    public static float[] trans_reverse(double[][] datas, int index)
    {
        int count = datas.length;
        int ii = datas[0].length;

        float[] result = new float[count * ii];
        float temp;
        for(int i = count - 1; i >= 0; i--)
        {
            for(int j = 0, num = datas[i].length; j < num; j++)
            {
                result[(count - i - 1) * num + j] = (float) datas[i][j];
            }

            for(int j = 0, num = datas[i].length; j < index; j++)
            {
                temp = result[(count - i - 1) * num + j];
                result[(count - i - 1) * num + j] = result[(count - i - 1) * num + (num - index) + j];
                result[(count - i - 1) * num + (num - index) + j] = temp;

                result[(count - i - 1) * num + 360] = result[(count - i - 1) * num + 360];
            }
        }

        return result;
    }

    public static float[] trans_reverse(float[] datas, int width, int height, int index)
    {
        float[] result = new float[width * height];
        float temp;
        for(int i = width - 1; i >= 0; i--)
        {
            for(int j = 0; j < height; j++)
            {
                result[(width - i - 1) * height + j] = datas[i * height + j];
            }
            for(int j = 0; j < index; j++)
            {
                temp = result[(width - i - 1) * height + j];
                result[(width - i - 1) * height + j] = result[(width - i - 1) * height + (height - index) + j];
                result[(width - i - 1) * height + (height - index) + j] = temp;
                result[(width - i - 1) * height + index] = result[(width - i - 1) * height + (height - index)];
            }
        }

        return result;
    }


    /**
     * @param datas 数组
     * @param index 对调索引位置
     * @return double[][]
     * @category 将数组数据在索引位置处按列对调数据
     */
    public static float[][] reverseArray(float[][] datas, int index) {
        int count = datas.length;
        int ii = datas[0].length;
        float[][] result = new float[count][ii];
        for (int i = 0; i < count; i++) {
            for (int j = 0; j < index; j++) {
                float[] ds1 = Arrays.copyOfRange(datas[i], 0, index);
                float[] ds2 = Arrays.copyOfRange(datas[i], index, ii);
                result[i] = ArrayUtil.addAll(ds2, ds1);
            }
        }

        return result;
    }

    /**
     * @category 将一维数组转换成二维数组
     * @date 2025/2/12 17:15
     * @param datas
     * @param width
     * @param height
     * @return double[][]
     */
    public static double[][] transTwo(float[] datas, int width, int height)
    {
        double[][] values = new double[height][width];
        for(int i = 0; i < height; i++)
        {
            for(int j = 0; j < width; j++)
            {
                values[i][j] = datas[i * width + j];
            }
        }

        return values;
    }

    public static double[][] floatToDouble(float[][] datas)
    {
        double[][] result = new double[datas.length][datas[0].length];
        for (int i = 0; i < datas.length; i++)
        {
            for (int j = 0; j < datas.length; j++)
            {
                result[i][j] = datas[i][j];
            }
        }

        return result;
    }
}
