package com.util;

/**
 * @category
 * @date 2025/3/21 9:27
 * @description TODO
 */
public class ReadGribRainUtil {
//           起点 左上  终点 右下
//    lonlats=70, 50, 111, 25
    public static double[] findFindFirstPoint(double lon, double lat, double lon_dis, double lat_dis, double[] lonlats)
    {
        double[] result = new double[4];
        int lonCount = (int) ((lonlats[2] - lonlats[0]) / lon_dis + 1);
        int latCount = (int) ((lonlats[1] - lonlats[3]) / lon_dis + 1);
        double[] lons = new double[lonCount];
        double[] lats = new double[latCount];
        for(int i = 0; i < lonCount; i++)
        {
            lons[i] = lonlats[0] + lon_dis * i;
        }
        for(int i = 0; i < latCount; i++)
        {
            lats[i] = lonlats[1] - lat_dis * i;
        }
        for(int i = 0; i < lonCount; i++)
        {
            if(lon < lons[i])
            {
                result[0] = i - 1;
                if(i >= 1)
                {
                    result[2] = (lon - lons[i - 1]) / lon_dis;
                }
                else
                {
                    result[2] = 0;
                }
                break;
            }
        }
        for(int i = 0; i < latCount; i++)
        {
            if(lat > lats[i])
            {
                result[1] = i - 1;
                if(i >= 1)
                {
                    result[3] = (lats[i - 1] - lat) / lat_dis;
                }
                else
                {
                    result[3] = 0;
                }
                break;
            }
        }
        if(result[0] < 0 || result[1] < 0)
        {
            result[0] = 0;
            result[1] = 0;
            result[2] = 0;
            result[3] = 0;
        }

        return result;
    }

    /**
     * 执行双线性插值计算。
     *
     * @param topLeft      左上角点的值
     * @param topRight     右上角点的值
     * @param bottomLeft   左下角点的值
     * @param bottomRight  右下角点的值
     * @param dx           x方向的插值系数，范围[0,1]，0表示左边界，1表示右边界
     * @param dy           y方向的插值系数，范围[0,1]，0表示下边界，1表示上边界
     * @return             插值结果
     */
    public static double bilinearInterpolation(
            double topLeft, double topRight,
            double bottomLeft, double bottomRight,
            double dx, double dy)
    {

        // 计算横向插值（上边和下边）
        double top = topLeft * (1 - dx) + topRight * dx;
        double bottom = bottomLeft * (1 - dx) + bottomRight * dx;

        // 计算纵向插值
        return bottom * (1 - dy) + top * dy;
    }
}
