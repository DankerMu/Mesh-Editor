package com.util;

import com.constants.DataTypeEnum;
import com.constants.DecodeConstants;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AroundDataUtil {
    private static double lon_dis = 0.1;
    private static double lat_dis = 0.1;

    public static Map<String, Double> getDatas(String element, double[][] datas, double[] lons, double[] lats, int[] index, double level, String aroundStr)
    {
        Map<String, Double> result = new LinkedHashMap<>();
        String levelStr = level == 999 ? null : level + "";
        float around = Float.parseFloat(aroundStr) / 2;
        double lonDis = Math.abs(lons[0] - lons[1]);
        double latDis = Math.abs(lats[0] - lats[1]);
        int lonCount = (int) Math.ceil(around / Double.parseDouble(NumberFormatUtil.scienceD(lonDis, 2)));
        int latCount = (int) Math.ceil(around / Double.parseDouble(NumberFormatUtil.scienceD(latDis, 2)));
        for(int i = index[1] - latCount, count = index[1] + latCount; i < count; i++)
        {
            for(int j = index[0] - lonCount, num = index[0] + lonCount; j < num; j++)
            {
                result.put(element + (levelStr == null ? "" : "_" + levelStr) + "_" + NumberFormatUtil.scienceD(lats[j], 2) + "_" +
                                                                                      NumberFormatUtil.scienceD(lons[i], 2), datas[i][j]);
            }
        }

        return result;
    }

    public static Map<String, Double> getRainDatas(String element, double[][] datas, double[][] prefDatas, double[] lons, double[] lats, int[] index, String level, String aroundStr)
    {
        Map<String, Double> result = new LinkedHashMap<>();
        float around = Float.parseFloat(aroundStr) / 2;
        double lonDis = Math.abs(lons[0] - lons[1]);
        double latDis = Math.abs(lats[0] - lats[1]);
        int lonCount = (int) Math.ceil(around / Double.parseDouble(NumberFormatUtil.scienceD(lonDis, 2)));
        int latCount = (int) Math.ceil(around / Double.parseDouble(NumberFormatUtil.scienceD(latDis, 2)));
        double value;
        for(int i = index[1] - latCount, count = index[1] + latCount; i < count; i++)
        {
            for(int j = index[0] - latCount, num = index[0] + lonCount; j < num; j++)
            {
                value = datas[i][j] - prefDatas[i][j];
                result.put(element + (level == null ? "" : "_" + level) + "_" + NumberFormatUtil.scienceD(lats[j], 2) + "_" +
                                                                                NumberFormatUtil.scienceD(lons[i], 2), value);
            }
        }

        return result;
    }

    /**
     * @category 抽取站点周围降水数据时，处理预报数据的前两个时效之后的数据
     * @date 2025/2/12 15:31
     * @param dataType
     * @param element
     * @param datas
     * @param prefDatas
     * @param prefDatas1
     * @param lons
     * @param lats
     * @param index
     * @param level
     * @param aroundStr
     * @param disVtiAndVti
     * @return java.util.List<java.util.Map < java.lang.String, java.lang.Double>>
     */
    public static List<Map<String, Double>> getInterpolateRainDatas(String dataType, String element, double[][] datas, double[][] prefDatas, double[][] prefDatas1, double[] lons, double[] lats, int[] index, String level, String aroundStr, int[] disVtiAndVti)
    {
        List<Map<String, Double>> result = new ArrayList<>();
        float around = Float.parseFloat(aroundStr) / 2;
        double lonDis = Math.abs(lons[0] - lons[1]);
        double latDis = Math.abs(lats[0] - lats[1]);
        int lonCount = (int) Math.ceil(around / Double.parseDouble(NumberFormatUtil.scienceD(lonDis, 3)));
        int latCount = (int) Math.ceil(around / Double.parseDouble(NumberFormatUtil.scienceD(latDis, 3)));
        int lonCountNew = lonCount * 2;
        int latCountNew = latCount * 2;
        double value;
        double[] lonsNew = new double[lonCountNew];
        double[] latsNew = new double[latCountNew];
        double[][] datasNew = new double[latCountNew][lonCountNew];
        double[][] prefDatasNew = new double[latCountNew][lonCountNew];
        int dis_vti = disVtiAndVti[0];
        int vti = disVtiAndVti[1];
        double[] t = GribUtil.getCzVtiStation(dataType, dis_vti, vti);
//        int tt = 1;
//        if(t != null)
//        {
//            tt = dis_vti / (dis_vti / (t.length + 1));
//        }
        if(dis_vti == 0)
        {
            dis_vti = 1;
        }

        int bl = 1;
        if(dataType.equals(DataTypeEnum.ECMF.getDataType()))
        {
            bl = 1000;
        }
        for(int i = index[1] - latCount, ii = 0, count = index[1] + latCount; i < count; i++, ii++)
        {
            latsNew[ii] = lats[i];
            for(int j = index[0] - lonCount, jj = 0, num = index[0] + lonCount; j < num; j++, jj++)
            {
                lonsNew[jj] = lons[j];
                datasNew[ii][jj] = bl * (datas[i][j] - prefDatas[i][j]) / dis_vti;
                prefDatasNew[ii][jj] = bl * (prefDatas[i][j] - prefDatas1[i][j]) / dis_vti;
            }
        }

        int czLonCount = lonCountNew;
        int czLatCount = latCountNew;
        if(lonDis != lon_dis)
        {
            czLonCount = (int) Math.ceil(around * 2 / lon_dis);
        }
        if(latDis != lat_dis)
        {
            czLatCount = (int) Math.ceil(around * 2 / lat_dis);
        }
        double[][] czDatas = bilinearInterpolation(datasNew, czLatCount, czLonCount);
        double[][] czPrefDatas = bilinearInterpolation(prefDatasNew, czLatCount, czLonCount);


        if(t != null)
        {
            double[][][] arrays = BilinearInterpolationForTwoArrays.generateInterpolatedArrays(czPrefDatas, czDatas, t);
            for(int i = 0, count = arrays.length; i < count; i++)
            {
                Map<String, Double> map = new LinkedHashMap<>();
                for(int j = 0, num = arrays[i].length; j < num; j++)
                {
                    for(int k = 0, total = arrays[i][j].length; k < total; k++)
                    {
                        map.put(element + (level == null ? "" : "_" + level) + "_" +
                                        NumberFormatUtil.scienceD(latsNew[0] + k * lat_dis, 3) + "_" +
                                        NumberFormatUtil.scienceD(lonsNew[0] + j * lon_dis, 3),
                                arrays[i][j][k] < 0 ? 0 : arrays[i][j][k]);
                    }
                }
                result.add(map);
            }
        }

        Map<String, Double> map = new LinkedHashMap<>();
        for(int i = 0; i < czLatCount; i++)
        {
            for(int j = 0; j < czLonCount; j++)
            {
                value = czDatas[i][j] - czPrefDatas[i][j];
                value = value < 0 ? 0 : value;
                map.put(element + (level == null ? "" : "_" + level) + "_" + NumberFormatUtil.scienceD(latsNew[0] + j * lat_dis, 3) + "_" +
                                                                             NumberFormatUtil.scienceD(lonsNew[0] + i * lon_dis, 3), value);
            }
        }
        result.add(map);

        return result;
    }
    public static List<Map<String, Double>> getInterpolateRainNaNDatas(String dataType, String element, double[][] datas, double[][] prefDatas, double[][] prefDatas1, double[] lons, double[] lats, int[] index, String level, String aroundStr, int[] disVtiAndVti)
    {
        List<Map<String, Double>> result = new ArrayList<>();
        float around = Float.parseFloat(aroundStr) / 2;
        double lonDis = Math.abs(lons[0] - lons[1]);
        double latDis = Math.abs(lats[0] - lats[1]);
        int lonCount = (int) Math.ceil(around / Double.parseDouble(NumberFormatUtil.scienceD(lonDis, 3)));
        int latCount = (int) Math.ceil(around / Double.parseDouble(NumberFormatUtil.scienceD(latDis, 3)));
        int lonCountNew = lonCount * 2;
        int latCountNew = latCount * 2;
        double[] lonsNew = new double[lonCountNew];
        double[] latsNew = new double[latCountNew];
        int dis_vti = disVtiAndVti[0];
        int vti = disVtiAndVti[1];
        double[] t = GribUtil.getCzVtiStation(dataType, dis_vti, vti);
        for(int i = index[1] - latCount, ii = 0, count = index[1] + latCount; i < count; i++, ii++)
        {
            latsNew[ii] = lats[i];
            for(int j = index[0] - lonCount, jj = 0, num = index[0] + lonCount; j < num; j++, jj++)
            {
                lonsNew[jj] = lons[j];
            }
        }
        int czLonCount = lonCountNew;
        int czLatCount = latCountNew;
        if(lonDis != lon_dis)
        {
            czLonCount = (int) Math.ceil(around * 2 / lon_dis);
        }
        if(latDis != lat_dis)
        {
            czLatCount = (int) Math.ceil(around * 2 / lat_dis);
        }

        if(t != null)
        {
            for(int i = 0, count = t.length; i < count; i++)
            {
                Map<String, Double> map = new LinkedHashMap<>();
                for(int j = 0, num = czLatCount; j < num; j++)
                {
                    for(int k = 0, total = czLonCount; k < total; k++)
                    {
                        map.put(element + (level == null ? "" : "_" + level) + "_" +
                                        NumberFormatUtil.scienceD(latsNew[0] + k * lat_dis, 3) + "_" +
                                        NumberFormatUtil.scienceD(lonsNew[0] + j * lon_dis, 3), DecodeConstants.UNDEF_DOUBLE_VALUE);
                    }
                }
                result.add(map);
            }
        }

        Map<String, Double> map = new LinkedHashMap<>();
        for(int i = 0; i < czLatCount; i++)
        {
            for(int j = 0; j < czLonCount; j++)
            {
                map.put(element + (level == null ? "" : "_" + level) + "_" + NumberFormatUtil.scienceD(latsNew[0] + j * lat_dis, 3) + "_" +
                                                                             NumberFormatUtil.scienceD(lonsNew[0] + i * lon_dis, 3), DecodeConstants.UNDEF_DOUBLE_VALUE);
            }
        }
        result.add(map);

        return result;
    }

    /**
     * @category 抽取站点周围降水数据时，处理预报数据第二个时效的数据
     * @date 2025/2/12 15:38
     * @param dataType
     * @param element
     * @param datas
     * @param prefDatas
     * @param lons
     * @param lats
     * @param index
     * @param level
     * @param aroundStr
     * @param disVtiAndVti
     * @return java.util.List<java.util.Map < java.lang.String, java.lang.Double>>
     */
    public static List<Map<String, Double>> getInterpolateRainDatas2(String dataType, String element, double[][] datas, double[][] prefDatas, double[] lons, double[] lats, int[] index, String level, String aroundStr, int[] disVtiAndVti)
    {
        List<Map<String, Double>> result = new ArrayList<>();
        float around = Float.parseFloat(aroundStr) / 2;
        double lonDis = Math.abs(lons[0] - lons[1]);
        double latDis = Math.abs(lats[0] - lats[1]);
        int lonCount = (int) Math.ceil(around / Double.parseDouble(NumberFormatUtil.scienceD(lonDis, 3)));
        int latCount = (int) Math.ceil(around / Double.parseDouble(NumberFormatUtil.scienceD(latDis, 3)));
        int lonCountNew = lonCount * 2;
        int latCountNew = latCount * 2;
        double value;
        double[] lonsNew = new double[lonCountNew];
        double[] latsNew = new double[latCountNew];
        double[][] datasNew = new double[latCountNew][lonCountNew];
        double[][] prefDatasNew = new double[latCountNew][lonCountNew];
        int bl = 1;
        if(dataType.equals(DataTypeEnum.ECMF.getDataType()))
        {
            bl = 1000;
        }
        int dis_vti = disVtiAndVti[0];
        if(dis_vti == 0)
        {
            dis_vti = 1;
        }

        for(int i = index[1] - latCount, ii = 0, count = index[1] + latCount; i < count; i++, ii++)
        {
            latsNew[ii] = lats[i];
            for(int j = index[0] - lonCount, jj = 0, num = index[0] + lonCount; j < num; j++, jj++)
            {
                lonsNew[jj] = lons[j];
                datasNew[ii][jj] = bl * datas[i][j] / dis_vti;
                prefDatasNew[ii][jj] = bl * prefDatas[i][j] / dis_vti;
            }
        }

        int czLonCount = lonCountNew;
        int czLatCount = latCountNew;
        if(lonDis != lon_dis)
        {
            czLonCount = (int) Math.ceil(around * 2 / lon_dis);
        }
        if(latDis != lat_dis)
        {
            czLatCount = (int) Math.ceil(around * 2 / lat_dis);
        }
        double[][] czDatas = bilinearInterpolation(datasNew, czLatCount, czLonCount);
        double[][] czPrefDatas = bilinearInterpolation(prefDatasNew, czLatCount, czLonCount);

        if(disVtiAndVti[1] <= 3)
        {
            Map<String, Double> map = new LinkedHashMap<>();
            for(int i = 0; i < czLatCount; i++)
            {
                for(int j = 0; j < czLonCount; j++)
                {
                    map.put(element + (level == null ? "" : "_" + level) + "_" +
                                    NumberFormatUtil.scienceD(latsNew[0] + j * lat_dis, 3) + "_" +
                                    NumberFormatUtil.scienceD(lonsNew[0] + i * lon_dis, 3),
                                    czPrefDatas[i][j]);
                }
            }
            result.add(map);
        }

        double[] t = GribUtil.getCzVtiStation(dataType, disVtiAndVti[0], disVtiAndVti[1]);
        if(t != null)
        {
            double[][][] arrays = BilinearInterpolationForTwoArrays.generateInterpolatedArrays(czPrefDatas, czDatas, t);
            for(int i = 0, count = arrays.length; i < count; i++)
            {
                Map<String, Double> map = new LinkedHashMap<>();
                for(int j = 0, num = arrays[i].length; j < num; j++)
                {
                    for(int k = 0, total = arrays[i][j].length; k < total; k++)
                    {
                        map.put(element + (level == null ? "" : "_" + level) + "_" +
                                        NumberFormatUtil.scienceD(latsNew[0] + k * lat_dis, 3) + "_" +
                                        NumberFormatUtil.scienceD(lonsNew[0] + j * lon_dis, 3),
                                        arrays[i][j][k] < 0 ? 0 : arrays[i][j][k]);
                    }
                }
                result.add(map);
            }
        }

        Map<String, Double> map = new LinkedHashMap<>();
        for(int i = 0; i < czLatCount; i++)
        {
            for(int j = 0; j < czLonCount; j++)
            {
                value = czDatas[i][j] - czPrefDatas[i][j];
                value = value < 0 ? 0 : value;
                map.put(element + (level == null ? "" : "_" + level) + "_" + NumberFormatUtil.scienceD(latsNew[0] + j * lat_dis, 3) + "_" +
                                                                             NumberFormatUtil.scienceD(lonsNew[0] + i * lon_dis, 3), value);
            }
        }
        result.add(map);

        return result;
    }
    public static List<Map<String, Double>> getInterpolateRainNaNDatas2(String dataType, String element, double[][] datas, double[][] prefDatas, double[] lons, double[] lats, int[] index, String level, String aroundStr, int[] disVtiAndVti)
    {
        List<Map<String, Double>> result = new ArrayList<>();
        float around = Float.parseFloat(aroundStr) / 2;
        double lonDis = Math.abs(lons[0] - lons[1]);
        double latDis = Math.abs(lats[0] - lats[1]);
        int lonCount = (int) Math.ceil(around / Double.parseDouble(NumberFormatUtil.scienceD(lonDis, 3)));
        int latCount = (int) Math.ceil(around / Double.parseDouble(NumberFormatUtil.scienceD(latDis, 3)));
        int lonCountNew = lonCount * 2;
        int latCountNew = latCount * 2;
        double value;
        double[] lonsNew = new double[lonCountNew];
        double[] latsNew = new double[latCountNew];
        double[][] datasNew = new double[latCountNew][lonCountNew];
        double[][] prefDatasNew = new double[latCountNew][lonCountNew];

        for(int i = index[1] - latCount, ii = 0, count = index[1] + latCount; i < count; i++, ii++)
        {
            latsNew[ii] = lats[i];
            for(int j = index[0] - lonCount, jj = 0, num = index[0] + lonCount; j < num; j++, jj++)
            {
                lonsNew[jj] = lons[j];
            }
        }

        int czLonCount = lonCountNew;
        int czLatCount = latCountNew;
        if(lonDis != lon_dis)
        {
            czLonCount = (int) Math.ceil(around * 2 / lon_dis);
        }
        if(latDis != lat_dis)
        {
            czLatCount = (int) Math.ceil(around * 2 / lat_dis);
        }

        if(disVtiAndVti[1] <= 3)
        {
            Map<String, Double> map = new LinkedHashMap<>();
            for(int i = 0; i < czLatCount; i++)
            {
                for(int j = 0; j < czLonCount; j++)
                {
                    map.put(element + (level == null ? "" : "_" + level) + "_" +
                                    NumberFormatUtil.scienceD(latsNew[0] + j * lat_dis, 3) + "_" +
                                    NumberFormatUtil.scienceD(lonsNew[0] + i * lon_dis, 3), DecodeConstants.UNDEF_DOUBLE_VALUE);
                }
            }
            result.add(map);
        }

        double[] t = GribUtil.getCzVtiStation(dataType, disVtiAndVti[0], disVtiAndVti[1]);
        if(t != null)
        {
            for(int i = 0, count = t.length; i < count; i++)
            {
                Map<String, Double> map = new LinkedHashMap<>();
                for(int j = 0, num = czLatCount; j < num; j++)
                {
                    for(int k = 0, total = czLonCount; k < total; k++)
                    {
                        map.put(element + (level == null ? "" : "_" + level) + "_" +
                                        NumberFormatUtil.scienceD(latsNew[0] + k * lat_dis, 3) + "_" +
                                        NumberFormatUtil.scienceD(lonsNew[0] + j * lon_dis, 3), DecodeConstants.UNDEF_DOUBLE_VALUE);
                    }
                }
                result.add(map);
            }
        }

        Map<String, Double> map = new LinkedHashMap<>();
        for(int i = 0; i < czLatCount; i++)
        {
            for(int j = 0; j < czLonCount; j++)
            {
                map.put(element + (level == null ? "" : "_" + level) + "_" + NumberFormatUtil.scienceD(latsNew[0] + j * lat_dis, 3) + "_" +
                                                                             NumberFormatUtil.scienceD(lonsNew[0] + i * lon_dis, 3), DecodeConstants.UNDEF_DOUBLE_VALUE);
            }
        }
        result.add(map);

        return result;
    }
    public static List<Map<String, Double>> getInterpolateSwc3kmRainDatas2(String dataType, String element, double[][] datas, double[][] prefDatas, double[] lons, double[] lats, int[] index, String level, String aroundStr, int[] disVtiAndVti)
    {
        List<Map<String, Double>> result = new ArrayList<>();
        float around = Float.parseFloat(aroundStr) / 2;
        double lonDis = Math.abs(lons[0] - lons[1]);
        double latDis = Math.abs(lats[0] - lats[1]);
        int lonCount = (int) Math.ceil(around / Double.parseDouble(NumberFormatUtil.scienceD(lonDis, 3)));
        int latCount = (int) Math.ceil(around / Double.parseDouble(NumberFormatUtil.scienceD(latDis, 3)));
        int lonCountNew = lonCount * 2;
        int latCountNew = latCount * 2;
        double value;
        double[] lonsNew = new double[lonCountNew];
        double[] latsNew = new double[latCountNew];
        double[][] datasNew = new double[latCountNew][lonCountNew];
        double[][] prefDatasNew = new double[latCountNew][lonCountNew];
        int bl = 1;
        if(dataType.equals(DataTypeEnum.ECMF.getDataType()))
        {
            bl = 1000;
        }
        int dis_vti = disVtiAndVti[0];

        for(int i = index[1] - latCount, ii = 0, count = index[1] + latCount; i < count; i++, ii++)
        {
            latsNew[ii] = lats[i];
            for(int j = index[0] - lonCount, jj = 0, num = index[0] + lonCount; j < num; j++, jj++)
            {
                lonsNew[jj] = lons[j];
                datasNew[ii][jj] = bl * datas[i][j] / dis_vti;
                prefDatasNew[ii][jj] = bl * prefDatas[i][j] / dis_vti;
            }
        }

        int czLonCount = lonCountNew;
        int czLatCount = latCountNew;
        if(lonDis != lon_dis)
        {
            czLonCount = (int) Math.ceil(around * 2 / lon_dis);
        }
        if(latDis != lat_dis)
        {
            czLatCount = (int) Math.ceil(around * 2 / lat_dis);
        }

//        if(disVtiAndVti[1] == 1)
//        {
//            Map<String, Double> map = new LinkedHashMap<>();
//            for(int i = 0; i < czLatCount; i++)
//            {
//                for(int j = 0; j < czLonCount; j++)
//                {
//                    map.put(element + (level == null ? "" : "_" + level) + "_" +
//                                    NumberFormatUtil.scienceD(latsNew[0] + j * lat_dis, 3) + "_" +
//                                    NumberFormatUtil.scienceD(lonsNew[0] + i * lon_dis, 3),
//                                    prefDatasNew[i][j]);
//                }
//            }
//            result.add(map);
//        }

        Map<String, Double> map = new LinkedHashMap<>();
        for(int i = 0; i < czLatCount; i++)
        {
            for(int j = 0; j < czLonCount; j++)
            {
                value = datasNew[i][j] - prefDatasNew[i][j];
                value = value < 0 ? 0 : value;
                if(Double.isNaN(value))
                {
                    value = DecodeConstants.UNDEF_DOUBLE_VALUE;
                }
                map.put(element + (level == null ? "" : "_" + level) + "_" + NumberFormatUtil.scienceD(latsNew[0] + j * lat_dis, 3) + "_" +
                                                                             NumberFormatUtil.scienceD(lonsNew[0] + i * lon_dis, 3), value);
            }
        }
        result.add(map);

        return result;
    }

    public static List<Map<String, Double>> getInterpolateSwc3kmRainDatas1(String dataType, String element, double[][] datas, double[] lons, double[] lats, int[] index, String level, String aroundStr, int[] disVtiAndVti)
    {
        List<Map<String, Double>> result = new ArrayList<>();
        float around = Float.parseFloat(aroundStr) / 2;
        double lonDis = Math.abs(lons[0] - lons[1]);
        double latDis = Math.abs(lats[0] - lats[1]);
        int lonCount = (int) Math.ceil(around / Double.parseDouble(NumberFormatUtil.scienceD(lonDis, 3)));
        int latCount = (int) Math.ceil(around / Double.parseDouble(NumberFormatUtil.scienceD(latDis, 3)));
        int lonCountNew = lonCount * 2;
        int latCountNew = latCount * 2;
        double value;
        double[] lonsNew = new double[lonCountNew];
        double[] latsNew = new double[latCountNew];
        double[][] datasNew = new double[latCountNew][lonCountNew];
//        double[][] prefDatasNew = new double[latCountNew][lonCountNew];
        int bl = 1;
        if(dataType.equals(DataTypeEnum.ECMF.getDataType()))
        {
            bl = 1000;
        }
        int dis_vti = disVtiAndVti[0];

        for(int i = index[1] - latCount, ii = 0, count = index[1] + latCount; i < count; i++, ii++)
        {
            latsNew[ii] = lats[i];
            for(int j = index[0] - lonCount, jj = 0, num = index[0] + lonCount; j < num; j++, jj++)
            {
                lonsNew[jj] = lons[j];
                datasNew[ii][jj] = bl * datas[i][j] / dis_vti;
//                prefDatasNew[ii][jj] = bl * prefDatas[i][j] / dis_vti;
            }
        }

        int czLonCount = lonCountNew;
        int czLatCount = latCountNew;
        if(lonDis != lon_dis)
        {
            czLonCount = (int) Math.ceil(around * 2 / lon_dis);
        }
        if(latDis != lat_dis)
        {
            czLatCount = (int) Math.ceil(around * 2 / lat_dis);
        }

//        if(disVtiAndVti[1] == 1)
//        {
//            Map<String, Double> map = new LinkedHashMap<>();
//            for(int i = 0; i < czLatCount; i++)
//            {
//                for(int j = 0; j < czLonCount; j++)
//                {
//                    map.put(element + (level == null ? "" : "_" + level) + "_" +
//                                    NumberFormatUtil.scienceD(latsNew[0] + j * lat_dis, 3) + "_" +
//                                    NumberFormatUtil.scienceD(lonsNew[0] + i * lon_dis, 3),
//                                    prefDatasNew[i][j]);
//                }
//            }
//            result.add(map);
//        }

        Map<String, Double> map = new LinkedHashMap<>();
        for(int i = 0; i < czLatCount; i++)
        {
            for(int j = 0; j < czLonCount; j++)
            {
                value = datasNew[i][j];
                value = value < 0 ? 0 : value;
                if(Double.isNaN(value))
                {
                    value = DecodeConstants.UNDEF_DOUBLE_VALUE;
                }
                map.put(element + (level == null ? "" : "_" + level) + "_" + NumberFormatUtil.scienceD(latsNew[0] + j * lat_dis, 3) + "_" +
                                                                             NumberFormatUtil.scienceD(lonsNew[0] + i * lon_dis, 3), value);
            }
        }
        result.add(map);

        return result;
    }

    /**
     * @category 抽取站点周围降水数据时，处理预报数据第一个时效的数据
     * @date 2025/2/12 15:42
     * @param element
     * @param datas
     * @param lons
     * @param lats
     * @param index
     * @param level
     * @param aroundStr
     * @param disVtiAndVti
     * @return java.util.List<java.util.Map < java.lang.String, java.lang.Double>>
     */
    public static List<Map<String, Double>> getInterpolateRainDatas1(String dataType, String element, double[][] datas, double[] lons, double[] lats, int[] index, String level, String aroundStr, int[] disVtiAndVti)
    {
        List<Map<String, Double>> result = new ArrayList<>();
        float around = Float.parseFloat(aroundStr) / 2;
        double lonDis = Math.abs(lons[0] - lons[1]);
        double latDis = Math.abs(lats[0] - lats[1]);
        int lonCount = (int) Math.ceil(around / Double.parseDouble(NumberFormatUtil.scienceD(lonDis, 3)));
        int latCount = (int) Math.ceil(around / Double.parseDouble(NumberFormatUtil.scienceD(latDis, 3)));
        int lonCountNew = lonCount * 2;
        int latCountNew = latCount * 2;
        double value;
        double[] lonsNew = new double[lonCountNew];
        double[] latsNew = new double[latCountNew];
        double[][] datasNew = new double[latCountNew][lonCountNew];
        int bl = 1;
        if(dataType.equals(DataTypeEnum.ECMF.getDataType()))
        {
            bl = 1000;
        }
        for(int i = index[1] - latCount, ii = 0, count = index[1] + latCount; i < count; i++, ii++)
        {
            latsNew[ii] = lats[i];
            for(int j = index[0] - lonCount, jj = 0, num = index[0] + lonCount; j < num; j++, jj++)
            {
                lonsNew[jj] = lons[j];
                datasNew[ii][jj] = bl * datas[i][j];
            }
        }

        int czLonCount = lonCountNew;
        int czLatCount = latCountNew;
        if(lonDis != lon_dis)
        {
            czLonCount = (int) Math.ceil(around * 2 / lon_dis);
        }
        if(latDis != lat_dis)
        {
            czLatCount = (int) Math.ceil(around * 2 / lat_dis);
        }
        double[][] czDatas = bilinearInterpolation(datasNew, czLatCount, czLonCount);

        Map<String, Double> map = new LinkedHashMap<>();
        for(int i = 0; i < czLatCount; i++)
        {
            for(int j = 0; j < czLonCount; j++)
            {
                value = czDatas[i][j];
                value = value < 0 ? 0 : value;
                map.put(element + (level == null ? "" : "_" + level) + "_" + NumberFormatUtil.scienceD(latsNew[0] + j * lat_dis, 3) + "_" +
                                                                             NumberFormatUtil.scienceD(lonsNew[0] + i * lon_dis, 3), value);
            }
        }
        result.add(map);

        return result;
    }

    /**
     * @category 抽取站点周围除降水之外的数据时，处理预报数据的第一个时效之后的数据
     * @date 2025/2/12 15:47
     * @param dataType
     * @param element
     * @param datas
     * @param prefDatas
     * @param lons
     * @param lats
     * @param index
     * @param level
     * @param aroundStr
     * @param disVtiAndVti
     * @return java.util.List<java.util.Map < java.lang.String, java.lang.Double>>
     */
    public static List<Map<String, Double>> getInterpolateDatas(String dataType, String element, double[][] datas, double[][] prefDatas, double[] lons, double[] lats, int[] index, String level, String aroundStr, int[] disVtiAndVti)
    {
        List<Map<String, Double>> result = new ArrayList<>();
        float around = Float.parseFloat(aroundStr) / 2;
        double lonDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lons[0] - lons[1]), 3));
        double latDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lats[0] - lats[1]), 3));
        int lonCount = (int) Math.ceil(around / lonDis);
        int latCount = (int) Math.ceil(around / latDis);
        int lonCountNew = lonCount * 2;
        int latCountNew = latCount * 2;
        double[] lonsNew = new double[lonCountNew];
        double[] latsNew = new double[latCountNew];
        double[][] datasNew = new double[latCountNew][lonCountNew];
        double[][] prefDatasNew = new double[latCountNew][lonCountNew];
        for(int i = index[1] - latCount, ii = 0, count = index[1] + latCount; i < count; i++, ii++)
        {
            latsNew[ii] = lats[i];
            for(int j = index[0] - lonCount, jj = 0, num = index[0] + lonCount; j < num; j++, jj++)
            {
                lonsNew[jj] = lons[j];
                datasNew[ii][jj] = datas[i][j];
                prefDatasNew[ii][jj] = prefDatas[i][j];
            }
        }

        int czLonCount = lonCountNew;
        int czLatCount = latCountNew;
        if(lonDis != lon_dis)
        {
            czLonCount = (int) Math.ceil(around * 2 / lon_dis);
        }
        if(latDis != lat_dis)
        {
            czLatCount = (int) Math.ceil(around * 2 / lat_dis);
        }
        double[][] czDatas = bilinearInterpolation(datasNew, czLatCount, czLonCount);
        double[][] czPrefDatas = bilinearInterpolation(prefDatasNew, czLatCount, czLonCount);

        int dis_vti = disVtiAndVti[0];
        int vti = disVtiAndVti[1];

        if(vti <= 3)
        {
            Map<String, Double> map = new LinkedHashMap<>();
            for(int i = 0; i < czLatCount; i++)
            {
                for(int j = 0; j < czLonCount; j++)
                {
                    map.put(element + (level == null ? "" : "_" + level) + "_" +
                            NumberFormatUtil.scienceD(latsNew[0] + j * lat_dis, 3) + "_" +
                            NumberFormatUtil.scienceD(lonsNew[0] + i * lon_dis, 3),
                            czPrefDatas[i][j]);
                }
            }
            result.add(map);
        }
        double[] t = GribUtil.getCzVtiStation(dataType, dis_vti, vti);
        if(t != null)
        {
            double[][][] arrays = BilinearInterpolationForTwoArrays.generateInterpolatedArrays(czPrefDatas, czDatas, t);

            for(int i = 0, count = arrays.length; i < count; i++)
            {
                Map<String, Double> map = new LinkedHashMap<>();
                for(int j = 0, num = arrays[i].length; j < num; j++)
                {
                    for(int k = 0, total = arrays[i][j].length; k < total; k++)
                    {
                        map.put(element + (level == null ? "" : "_" + level) + "_" +
                                        NumberFormatUtil.scienceD(latsNew[0] + k * lat_dis, 3) + "_" +
                                        NumberFormatUtil.scienceD(lonsNew[0] + j * lon_dis, 3), arrays[i][j][k]);
                    }
                }
                result.add(map);
            }
        }
        Map<String, Double> map = new LinkedHashMap<>();
        for(int i = 0; i < czLatCount; i++)
        {
            for(int j = 0; j < czLonCount; j++)
            {
                map.put(element + (level == null ? "" : "_" + level) + "_" + NumberFormatUtil.scienceD(latsNew[0] + j * lat_dis, 3) + "_" +
                                                                             NumberFormatUtil.scienceD(lonsNew[0] + i * lon_dis, 3), czDatas[i][j]);
            }
        }
        result.add(map);

        return result;
    }
    public static List<Map<String, Double>> getInterpolateNaNDatas(String dataType, String element, double[][] datas, double[][] prefDatas, double[] lons, double[] lats, int[] index, String level, String aroundStr, int[] disVtiAndVti)
    {
        List<Map<String, Double>> result = new ArrayList<>();
        float around = Float.parseFloat(aroundStr) / 2;
        double lonDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lons[0] - lons[1]), 3));
        double latDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lats[0] - lats[1]), 3));
        int lonCount = (int) Math.ceil(around / lonDis);
        int latCount = (int) Math.ceil(around / latDis);
        int lonCountNew = lonCount * 2;
        int latCountNew = latCount * 2;
        double[] lonsNew = new double[lonCountNew];
        double[] latsNew = new double[latCountNew];
        double[][] datasNew = new double[latCountNew][lonCountNew];
        double[][] prefDatasNew = new double[latCountNew][lonCountNew];
        for(int i = index[1] - latCount, ii = 0, count = index[1] + latCount; i < count; i++, ii++)
        {
            latsNew[ii] = lats[i];
            for(int j = index[0] - lonCount, jj = 0, num = index[0] + lonCount; j < num; j++, jj++)
            {
                lonsNew[jj] = lons[j];
            }
        }

        int czLonCount = lonCountNew;
        int czLatCount = latCountNew;
        if(lonDis != lon_dis)
        {
            czLonCount = (int) Math.ceil(around * 2 / lon_dis);
        }
        if(latDis != lat_dis)
        {
            czLatCount = (int) Math.ceil(around * 2 / lat_dis);
        }

        int dis_vti = disVtiAndVti[0];
        int vti = disVtiAndVti[1];
        //TODO ?????????????????
        if(vti <= 3)
        {
            Map<String, Double> map = new LinkedHashMap<>();
            for(int i = 0; i < czLatCount; i++)
            {
                for(int j = 0; j < czLonCount; j++)
                {
                    map.put(element + (level == null ? "" : "_" + level) + "_" +
                            NumberFormatUtil.scienceD(latsNew[0] + j * lat_dis, 3) + "_" +
                            NumberFormatUtil.scienceD(lonsNew[0] + i * lon_dis, 3), DecodeConstants.UNDEF_DOUBLE_VALUE);
                }
            }
            result.add(map);
        }
        double[] t = GribUtil.getCzVtiStation(dataType, dis_vti, vti);
        if(t != null)
        {
            for(int i = 0, count = t.length; i < count; i++)
            {
                Map<String, Double> map = new LinkedHashMap<>();
                for(int j = 0, num = czLatCount; j < num; j++)
                {
                    for(int k = 0, total = czLonCount; k < total; k++)
                    {
                        map.put(element + (level == null ? "" : "_" + level) + "_" +
                                        NumberFormatUtil.scienceD(latsNew[0] + k * lat_dis, 3) + "_" +
                                        NumberFormatUtil.scienceD(lonsNew[0] + j * lon_dis, 3), DecodeConstants.UNDEF_DOUBLE_VALUE);
                    }
                }
                result.add(map);
            }
        }
        Map<String, Double> map = new LinkedHashMap<>();
        for(int i = 0; i < czLatCount; i++)
        {
            for(int j = 0; j < czLonCount; j++)
            {
                map.put(element + (level == null ? "" : "_" + level) + "_" + NumberFormatUtil.scienceD(latsNew[0] + j * lat_dis, 3) + "_" +
                                                                             NumberFormatUtil.scienceD(lonsNew[0] + i * lon_dis, 3), DecodeConstants.UNDEF_DOUBLE_VALUE);
            }
        }
        result.add(map);

        return result;
    }

    /**
     * @category 使用最邻近插值算法抽取站点周围降水类型数据
     * @date 2025/2/24 16:08
     * @param dataType
     * @param element
     * @param datas
     * @param prefDatas
     * @param lons
     * @param lats
     * @param index
     * @param level
     * @param aroundStr
     * @param disVtiAndVti
     * @return java.util.List<java.util.Map < java.lang.String, java.lang.Double>>
     */
    public static List<Map<String, Double>> getInterpolateNearestDatas(String dataType, String element, double[][] datas, double[][] prefDatas, double[] lons, double[] lats, int[] index, String level, String aroundStr, int[] disVtiAndVti)
    {
        List<Map<String, Double>> result = new ArrayList<>();
        float around = Float.parseFloat(aroundStr) / 2;
        double lonDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lons[0] - lons[1]), 3));
        double latDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lats[0] - lats[1]), 3));
        int lonCount = (int) Math.ceil(around / lonDis);
        int latCount = (int) Math.ceil(around / latDis);
        int lonCountNew = lonCount * 2;
        int latCountNew = latCount * 2;
        double[] lonsNew = new double[lonCountNew];
        double[] latsNew = new double[latCountNew];
        double[][] datasNew = new double[latCountNew][lonCountNew];
        double[][] prefDatasNew = new double[latCountNew][lonCountNew];
        for(int i = index[1] - latCount, ii = 0, count = index[1] + latCount; i < count; i++, ii++)
        {
            latsNew[ii] = lats[i];
            for(int j = index[0] - lonCount, jj = 0, num = index[0] + lonCount; j < num; j++, jj++)
            {
                lonsNew[jj] = lons[j];
                datasNew[ii][jj] = datas[i][j];
                prefDatasNew[ii][jj] = prefDatas[i][j];
            }
        }

        int czLonCount = lonCountNew;
        int czLatCount = latCountNew;
        if(lonDis != lon_dis)
        {
            czLonCount = (int) Math.ceil(around * 2 / lon_dis);
        }
        if(latDis != lat_dis)
        {
            czLatCount = (int) Math.ceil(around * 2 / lat_dis);
        }
//        double[][] czDatas = bilinearInterpolation(datasNew, czLatCount, czLonCount);
//        double[][] czPrefDatas = bilinearInterpolation(prefDatasNew, czLatCount, czLonCount);
        double[][] czDatas = nearestInterpolation(datas, datas.length, datas[0].length, czLatCount, czLonCount);
        double[][] czPrefDatas = nearestInterpolation(prefDatas, datas.length, datas[0].length, czLatCount, czLonCount);

        int dis_vti = disVtiAndVti[0];
        int vti = disVtiAndVti[1];
        //TODO ?????????????????
        if(vti <= 3)
        {
            Map<String, Double> map = new LinkedHashMap<>();
            for(int i = 0; i < czLatCount; i++)
            {
                for(int j = 0; j < czLonCount; j++)
                {
                    map.put(element + (level == null ? "" : "_" + level) + "_" +
                            NumberFormatUtil.scienceD(latsNew[0] + j * lat_dis, 3) + "_" +
                            NumberFormatUtil.scienceD(lonsNew[0] + i * lon_dis, 3),
                            czPrefDatas[i][j]);
                }
            }
            result.add(map);
        }
        double[] t = GribUtil.getCzVtiStation(dataType, dis_vti, vti);
        if(t != null)
        {
            double[][][] arrays = nearestInterpolationByTime(czPrefDatas, czDatas, t.length);

            for(int i = 0, count = arrays.length; i < count; i++)
            {
                Map<String, Double> map = new LinkedHashMap<>();
                for(int j = 0, num = arrays[i].length; j < num; j++)
                {
                    for(int k = 0, total = arrays[i][j].length; k < total; k++)
                    {
                        map.put(element + (level == null ? "" : "_" + level) + "_" +
                                        NumberFormatUtil.scienceD(latsNew[0] + k * lat_dis, 3) + "_" +
                                        NumberFormatUtil.scienceD(lonsNew[0] + j * lon_dis, 3),
                                arrays[i][j][k]);
                    }
                }
                result.add(map);
            }
        }
        Map<String, Double> map = new LinkedHashMap<>();
        for(int i = 0; i < czLatCount; i++)
        {
            for(int j = 0; j < czLonCount; j++)
            {
                map.put(element + (level == null ? "" : "_" + level) + "_" + NumberFormatUtil.scienceD(latsNew[0] - j * lat_dis, 3) + "_" +
                                                                             NumberFormatUtil.scienceD(lonsNew[0] + i * lon_dis, 3), czDatas[i][j]);
            }
        }
        result.add(map);

        return result;
    }

    /**
     * @category 抽取站点周围除降水之外的数据时，处理预报数据的第一个时效的数据
     * @date 2025/2/12 15:49
     * @param element
     * @param datas
     * @param lons
     * @param lats
     * @param index
     * @param level
     * @param aroundStr
     * @param disVtiAndVti
     * @return java.util.List<java.util.Map < java.lang.String, java.lang.Double>>
     */
    public static List<Map<String, Double>> getInterpolateOneDatas(String element, double[][] datas, double[] lons, double[] lats, int[] index, String level, String aroundStr, int[] disVtiAndVti)
    {
        List<Map<String, Double>> result = new ArrayList<>();
        float around = Float.parseFloat(aroundStr) / 2;
        double lonDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lons[0] - lons[1]), 3));
        double latDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lats[0] - lats[1]), 3));
        int lonCount = (int) Math.ceil(around / lonDis);
        int latCount = (int) Math.ceil(around / latDis);
        int lonCountNew = lonCount * 2;
        int latCountNew = latCount * 2;
        double[] lonsNew = new double[lonCountNew];
        double[] latsNew = new double[latCountNew];
        double[][] datasNew = new double[latCountNew][lonCountNew];
        for(int i = index[1] - latCount, ii = 0, count = index[1] + latCount; i < count; i++, ii++)
        {
            if(i == -4)
            {
                System.out.println("-4");
            }
            latsNew[ii] = lats[i];
            for(int j = index[0] - lonCount, jj = 0, num = index[0] + lonCount; j < num; j++, jj++)
            {
                lonsNew[jj] = lons[j];
                datasNew[ii][jj] = datas[i][j];
            }
        }

        int czLonCount = lonCountNew;
        int czLatCount = latCountNew;
        if(lonDis != lon_dis)
        {
            czLonCount = (int) Math.ceil(around * 2 / lon_dis);
        }
        if(latDis != lat_dis)
        {
            czLatCount = (int) Math.ceil(around * 2 / lat_dis);
        }
        double[][] czDatas = bilinearInterpolation(datasNew, czLatCount, czLonCount);

//        if(disVtiAndVti[1] <= 3)
//        {
//            Map<String, Double> map = new LinkedHashMap<>();
//            for(int i = 0; i < czLatCount; i++)
//            {
//                for(int j = 0; j < czLonCount; j++)
//                {
//                    map.put(element + (level == null ? "" : "_" + level) + "_" +
//                            NumberFormatUtil.scienceD(latsNew[0] + j * lat_dis, 2) + "_" +
//                            NumberFormatUtil.scienceD(lonsNew[0] + i * lon_dis, 2),
//                            czDatas[i][j]);
//                }
//            }
//            result.add(map);
//        }

        Map<String, Double> map = new LinkedHashMap<>();
        for(int i = 0; i < czLatCount; i++)
        {
            for(int j = 0; j < czLonCount; j++)
            {
                map.put(element + (level == null ? "" : "_" + level) + "_" + NumberFormatUtil.scienceD(latsNew[0] + j * lat_dis, 3) + "_" +
                                                                             NumberFormatUtil.scienceD(lonsNew[0] + i * lon_dis, 3), czDatas[i][j]);
            }
        }
        result.add(map);

        return result;
    }

    public static List<Map<String, Double>> getInterpolateDatas(String element, double[][] datas, double[] lons, double[] lats, int[] index, String level, String aroundStr)
    {
        List<Map<String, Double>> result = new ArrayList<>();
        float around = Float.parseFloat(aroundStr) / 2;
        double lonDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lons[0] - lons[1]), 3));
        double latDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lats[0] - lats[1]), 3));
        int lonCount = (int) Math.ceil(around / lonDis);
        int latCount = (int) Math.ceil(around / latDis);
        int lonCountNew = lonCount * 2;
        int latCountNew = latCount * 2;
        double[] lonsNew = new double[lonCountNew];
        double[] latsNew = new double[latCountNew];
        double[][] datasNew = new double[latCountNew][lonCountNew];
//        if("tcc,lcc".contains(element))
//        {
//
//        }
        for(int i = index[1] - latCount, ii = 0, count = index[1] + latCount; i < count; i++, ii++)
        {
            latsNew[ii] = lats[i];
            for(int j = index[0] - lonCount, jj = 0, num = index[0] + lonCount; j < num; j++, jj++)
            {
                lonsNew[jj] = lons[j];
                datasNew[ii][jj] = datas[i][j];
            }
        }

        int czLonCount = lonCountNew;
        int czLatCount = latCountNew;
        if(lonDis != lon_dis)
        {
            czLonCount = (int) Math.ceil(around * 2 / lon_dis);
        }
        if(latDis != lat_dis)
        {
            czLatCount = (int) Math.ceil(around * 2 / lat_dis);
        }
        double[][] czDatas = bilinearInterpolation(datasNew, czLatCount, czLonCount);

        Map<String, Double> map = new LinkedHashMap<>();
        for(int i = 0; i < czLatCount; i++)
        {
            for(int j = 0; j < czLonCount; j++)
            {
                map.put(element + (level == null ? "" : "_" + level) + "_" + NumberFormatUtil.scienceD(latsNew[0] + j * lat_dis, 3) + "_" +
                                                                             NumberFormatUtil.scienceD(lonsNew[0] + i * lon_dis, 3), czDatas[i][j]);
            }
        }
        result.add(map);

        return result;
    }

    public static List<Map<String, double[][]>> getInterpolateDatasArray(String element, double[][] datas, double[] lons, double[] lats, double[] lonlat, String level)
    {
        List<Map<String, double[][]>> result = new ArrayList<>();
        double lonDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lons[0] - lons[1]), 3));
        double latDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lats[0] - lats[1]), 3));
        int czLonCount = datas[0].length;
        int czLatCount = datas.length;
        if(lonDis != lon_dis)
        {
            czLonCount = (int) Math.ceil(Math.abs(lonlat[0] - lonlat[2]) / lon_dis);
        }
        if(latDis != lat_dis)
        {
            czLatCount = (int) Math.ceil(Math.abs(lonlat[1] - lonlat[3]) / lat_dis);
        }
        double[][] czDatas = bilinearInterpolation(datas, czLatCount, czLonCount);
        Map<String, double[][]> map = new LinkedHashMap<>();
        map.put(element + "_" + level, czDatas);
        result.add(map);

        return result;
    }

    public static List<Map<String, double[][]>> getInterpolateCldasDatasArray(String element, double[][] datas, double[] lons, double[] lats, double[] lonlat, String level)
    {
        List<Map<String, double[][]>> result = new ArrayList<>();
        double lonDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lons[0] - lons[1]), 4));
        double latDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lats[0] - lats[1]), 4));
        int czLonCount = datas[0].length;
        int czLatCount = datas.length;
        double lonStep = 0.05;
        double latStep = 0.05;
        if(lonDis != lonStep)
        {
            czLonCount = (int) Math.ceil(Math.abs(lonlat[0] - lonlat[2]) / lonStep);
        }
        if(latDis != latStep)
        {
            czLatCount = (int) Math.ceil(Math.abs(lonlat[1] - lonlat[3]) / latStep);
        }
        double[][] czDatas = bilinearInterpolation(datas, czLatCount, czLonCount);
        Map<String, double[][]> map = new LinkedHashMap<>();
        map.put(element +  "_" + level, czDatas);
        result.add(map);

        return result;
    }

    public static List<Map<String, double[][]>> getInterpolateDatasArray(String dataType, String element, double[][] datas, double[][] prefDatas, double[] lons, double[] lats, double[] lonlat, String level, int[] disVtiAndVti)
    {
        List<Map<String, double[][]>> result = new ArrayList<>();
        double lonDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lons[0] - lons[1]), 3));
        double latDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lats[0] - lats[1]), 3));
        int czLonCount = datas[0].length;
        int czLatCount = datas.length;
        if(lonDis != lon_dis)
        {
            czLonCount = (int) Math.ceil(Math.abs(lonlat[0] - lonlat[2]) / lon_dis);
        }
        if(latDis != lat_dis)
        {
            czLatCount = (int) Math.ceil(Math.abs(lonlat[1] - lonlat[3]) / lat_dis);
        }
        double[][] czDatas = null;
        double[][] czPrefDatas = null;
        if(element.startsWith("q"))
        {
            czDatas = bilinearInterpolationQ(datas, czLatCount, czLonCount);
            czPrefDatas = bilinearInterpolationQ(prefDatas, czLatCount, czLonCount);
        }
        else
        {
            czDatas = bilinearInterpolation(datas, czLatCount, czLonCount);
            czPrefDatas = bilinearInterpolation(prefDatas, czLatCount, czLonCount);
        }
        double[] t = GribUtil.getCzVtiZone(dataType, disVtiAndVti[0], disVtiAndVti[1]);
        if(t != null)
        {
            double[][][] arrays = BilinearInterpolationForTwoArrays.generateInterpolatedArrays(czPrefDatas, czDatas, t);

            for(int i = 0, count = arrays.length; i < count; i++)
            {
                Map<String, double[][]> map = new LinkedHashMap<>();
                map.put(element + (level == null ? "" : "_" + level), arrays[i]);
                result.add(map);
            }
        }
        Map<String, double[][]> map = new LinkedHashMap<>();
        map.put(element + (level == null ? "" : "_" + level), czDatas);
        result.add(map);

        return result;
    }
    public static List<Map<String, double[][]>> getInterpolateDatasArrayOne(String dataType, String element, double[][] datas, double[] lons, double[] lats, double[] lonlat, String level, int[] disVtiAndVti)
    {
        List<Map<String, double[][]>> result = new ArrayList<>();
        double lonDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lons[0] - lons[1]), 3));
        double latDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lats[0] - lats[1]), 3));
        int czLonCount = datas[0].length;
        int czLatCount = datas.length;
        if(lonDis != lon_dis)
        {
            czLonCount = (int) Math.ceil(Math.abs(lonlat[0] - lonlat[2]) / lon_dis);
        }
        if(latDis != lat_dis)
        {
            czLatCount = (int) Math.ceil(Math.abs(lonlat[1] - lonlat[3]) / lat_dis);
        }
        int bl = 1;
        if(dataType.equals(DataTypeEnum.ECMF.getDataType()))
        {
            bl = 1000;
        }
        for(int i = 0, count = datas.length; i < count; i++)
        {
            for(int j = 0, num = datas[i].length; j < num; j++)
            {
                datas[i][j] = bl * datas[i][j];
            }
        }
        double[][] czDatas = null;
        if(element.startsWith("q"))
        {
            czDatas = bilinearInterpolationQ(datas, czLatCount, czLonCount);
        }
        else
        {
            czDatas = bilinearInterpolation(datas, czLatCount, czLonCount);
        }
//        double[] t = GribUtil.getCzVtiZone(dataType, disVtiAndVti[0], disVtiAndVti[1]);
//        if(t != null)
//        {
//            double[][][] arrays = BilinearInterpolationForTwoArrays.generateInterpolatedArrays(czPrefDatas, czDatas, t);
//
//            for(int i = 0, count = arrays.length; i < count; i++)
//            {
//                Map<String, double[][]> map = new LinkedHashMap<>();
//                map.put(element + (level == null ? "" : "_" + level), arrays[i]);
//                result.add(map);
//            }
//        }
        Map<String, double[][]> map = new LinkedHashMap<>();
        map.put(element + (level == null ? "" : "_" + level), czDatas);
        result.add(map);

        return result;
    }

    /**
     * @category 区域，使用最邻近插值算法在空间和时间维度上插值
     * @date 2025/2/24 15:40
     * @param dataType
     * @param element
     * @param datas
     * @param prefDatas
     * @param lons
     * @param lats
     * @param lonlat
     * @param level
     * @param disVtiAndVti
     * @return java.util.List<java.util.Map < java.lang.String, double [ ] [ ]>>
     */
    public static List<Map<String, double[][]>> getInterpolateNearestDatasArray(String dataType, String element, double[][] datas, double[][] prefDatas, double[] lons, double[] lats, double[] lonlat, String level, int[] disVtiAndVti)
    {
        List<Map<String, double[][]>> result = new ArrayList<>();
        double lonDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lons[0] - lons[1]), 3));
        double latDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lats[0] - lats[1]), 3));
        int czLonCount = datas[0].length;
        int czLatCount = datas.length;
        double lonStep = lon_dis;
        double latStep = lat_dis;
        if(dataType.equals(DataTypeEnum.ECMF.getDataType()) || dataType.equals(DataTypeEnum.GRAPES.getDataType()))
        {
            lonStep = 0.05;
            latStep = 0.05;
        }
        if(lonDis != lonStep)
        {
            czLonCount = (int) Math.ceil(Math.abs(lonlat[0] - lonlat[2]) / lonStep);
        }
        if(latDis != latStep)
        {
            czLatCount = (int) Math.ceil(Math.abs(lonlat[1] - lonlat[3]) / latStep);
        }
        double[][] czDatas = null;
        double[][] czPrefDatas = null;
        czDatas = nearestInterpolation(datas, datas.length, datas[0].length, czLatCount, czLonCount);
        czPrefDatas = nearestInterpolation(prefDatas, datas.length, datas[0].length, czLatCount, czLonCount);

        double[] t = GribUtil.getCzVtiZone(dataType, disVtiAndVti[0], disVtiAndVti[1]);
        if(t != null)
        {
            double[][][] arrays = nearestInterpolationByTime(czPrefDatas, czDatas, t.length);

            for(int i = 0, count = arrays.length; i < count; i++)
            {
                Map<String, double[][]> map = new LinkedHashMap<>();
                map.put(element + (level == null ? "" : "_" + level), arrays[i]);
                result.add(map);
            }
        }
        Map<String, double[][]> map = new LinkedHashMap<>();
        map.put(element + (level == null ? "" : "_" + level), czDatas);
        result.add(map);

        return result;
    }

    /**
     * @category 区域，使用最邻近插值算法在空间维度上插值
     * @date 2025/2/24 16:49
     * @param dataType
     * @param element
     * @param datas
     * @param lons
     * @param lats
     * @param lonlat
     * @param level
     * @param disVtiAndVti
     * @return java.util.List<java.util.Map < java.lang.String, double [ ] [ ]>>
     */
    public static List<Map<String, double[][]>> getInterpolateNearestDatasArray1(String dataType, String element, double[][] datas, double[] lons, double[] lats, double[] lonlat, String level, int[] disVtiAndVti)
    {
        List<Map<String, double[][]>> result = new ArrayList<>();

        Map<String, double[][]> map = new LinkedHashMap<>();
        map.put(element + (level == null ? "" : "_" + level), datas);
        result.add(map);

        return result;
    }

    /**
     * @category 抽取区域数据时，处理第一个时效的数据
     * @date 2025/2/12 15:26
     * @param dataType
     * @param element
     * @param datas
     * @param prefDatas
     * @param lons
     * @param lats
     * @param lonlat
     * @param level
     * @param disVtiAndVti
     * @return java.util.List<java.util.Map < java.lang.String, double [ ] [ ]>>
     */
    public static List<Map<String, double[][]>> getInterpolateDatasArray1(String dataType, String element, double[][] datas, double[][] prefDatas, double[] lons, double[] lats, double[] lonlat, String level, int[] disVtiAndVti)
    {
        List<Map<String, double[][]>> result = new ArrayList<>();
        double lonDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lons[0] - lons[1]), 3));
        double latDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lats[0] - lats[1]), 3));
        int czLonCount = datas[0].length;
        int czLatCount = datas.length;
        if(lonDis != lon_dis)
        {
            czLonCount = (int) Math.ceil(Math.abs(lonlat[0] - lonlat[2]) / lon_dis);
        }
        if(latDis != lat_dis)
        {
            czLatCount = (int) Math.ceil(Math.abs(lonlat[1] - lonlat[3]) / lat_dis);
        }
        double[][] czDatas = bilinearInterpolation(datas, czLatCount, czLonCount);
        double[][] czPrefDatas = bilinearInterpolation(prefDatas, czLatCount, czLonCount);
        double[] t = GribUtil.getCzVtiZone(dataType, disVtiAndVti[0], disVtiAndVti[1]);
        if(t != null)
        {
            double[][][] arrays = BilinearInterpolationForTwoArrays.generateInterpolatedArrays(czPrefDatas, czDatas, t);

            for(int i = 0, count = arrays.length; i < count; i++)
            {
                Map<String, double[][]> map = new LinkedHashMap<>();
                map.put(element + (level == null ? "" : "_" + level), arrays[i]);
                result.add(map);
            }
        }
        Map<String, double[][]> map = new LinkedHashMap<>();
        map.put(element + (level == null ? "" : "_" + level), czDatas);
        result.add(map);

        return result;
    }
    /**
     * @category 抽取区域数据时，处理第一个时效的数据
     * @date 2025/2/12 15:26
     * @param dataType
     * @param element
     * @param datas
//     * @param prefDatas
     * @param lons
     * @param lats
     * @param lonlat
     * @param level
     * @param disVtiAndVti
     * @return java.util.List<java.util.Map < java.lang.String, double [ ] [ ]>>
     */
    public static List<Map<String, double[][]>> getInterpolateDatasArray1_new(String dataType, String element, double[][] datas, double[] lons, double[] lats, double[] lonlat, String level, int[] disVtiAndVti)
    {
        List<Map<String, double[][]>> result = new ArrayList<>();
        double lonDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lons[0] - lons[1]), 3));
        double latDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lats[0] - lats[1]), 3));
        int czLonCount = datas[0].length;
        int czLatCount = datas.length;
        if(lonDis != lon_dis)
        {
            czLonCount = (int) Math.ceil(Math.abs(lonlat[0] - lonlat[2]) / lon_dis);
        }
        if(latDis != lat_dis)
        {
            czLatCount = (int) Math.ceil(Math.abs(lonlat[1] - lonlat[3]) / lat_dis);
        }
        double[][] czDatas = bilinearInterpolation(datas, czLatCount, czLonCount);
        Map<String, double[][]> map = new LinkedHashMap<>();
        map.put(element + (level == null ? "" : "_" + level), czDatas);
        result.add(map);

        return result;
    }

    /**
     * @category 抽取指定区域降水数据时，处理预报数据的前两个时效之后的数据
     * @date 2025/2/12 15:11
     * @param dataType
     * @param element
     * @param datas
     * @param prefDatas
     * @param prefDatas1
     * @param lons
     * @param lats
     * @param lonlat
     * @param level
     * @param disVtiAndVti
     * @return java.util.List<java.util.Map < java.lang.String, double [ ] [ ]>>
     */
    public static List<Map<String, double[][]>> getInterpolateRainDatasArray(String dataType, String element, double[][] datas, double[][] prefDatas, double[][] prefDatas1, double[] lons, double[] lats, double[] lonlat, String level, int[] disVtiAndVti)
    {
        List<Map<String, double[][]>> result = new ArrayList<>();
        double lonDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lons[0] - lons[1]), 3));
        double latDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lats[0] - lats[1]), 3));
        int czLonCount = datas[0].length;
        int czLatCount = datas.length;
        if(lonDis != lon_dis)
        {
            czLonCount = (int) Math.ceil(Math.abs(lonlat[0] - lonlat[2]) / lon_dis);
        }
        if(latDis != lat_dis)
        {
            czLatCount = (int) Math.ceil(Math.abs(lonlat[1] - lonlat[3]) / lat_dis);
        }
        double[][] czDatas = bilinearInterpolation(datas, czLatCount, czLonCount);
        double[][] czPrefDatas = bilinearInterpolation(prefDatas, czLatCount, czLonCount);
        double[][] czPrefDatas1 = bilinearInterpolation(prefDatas1, czLatCount, czLonCount);
        double[][] datas1 = new double[czLatCount][czLonCount];
        double[][] datas2 = new double[czLatCount][czLonCount];
        int dis_vti = disVtiAndVti[0];
        if(dis_vti == 0)
        {
            dis_vti = 1;
        }
        int bl = 1;
        if(dataType.equals(DataTypeEnum.ECMF.getDataType()))
        {
            bl = 1000;
        }
        level = level == null ? "" : "_" + level;
        for(int i = 0, count = czDatas.length; i < count; i++)
        {
            for(int j = 0, num = czDatas[i].length; j < num; j++)
            {
                datas1[i][j] = NumberFormatUtil.numFormat(bl * (czDatas[i][j] - czPrefDatas[i][j]) / dis_vti, 3);
                datas2[i][j] = NumberFormatUtil.numFormat(bl * (czPrefDatas[i][j] - czPrefDatas1[i][j]) / dis_vti, 3);
            }
        }
        double[] t = GribUtil.getCzVtiZone(dataType, disVtiAndVti[0], disVtiAndVti[1]);
        if(t != null)
        {
            double[][][] arrays = BilinearInterpolationForTwoArrays.generateInterpolatedArrays(datas2, datas1, t);

            for(int i = 0, count = arrays.length; i < count; i++)
            {
                for(int j = 0, num = arrays[i].length; j < num; j++)
                {
                    for(int k = 0, num2 = arrays[i][j].length; k < num2; k++)
                    {
                        if(arrays[i][j][k] < 0)
                        {
                            arrays[i][j][k] = 0;
                        }
                        arrays[i][j][k] = NumberFormatUtil.numFormat(arrays[i][j][k], 3);
                    }
                }
                Map<String, double[][]> map = new LinkedHashMap<>();
                map.put(element + level, arrays[i]);
                result.add(map);
            }
        }
        Map<String, double[][]> map = new LinkedHashMap<>();
        map.put(element + level, datas1);
        result.add(map);

        return result;
    }

    /**
     * @category 抽取指定区域降水数据时，处理预报数据的第二个时效的数据
     * @date 2025/2/12 15:06
     * @param dataType
     * @param element
     * @param datas
     * @param prefDatas
     * @param lons
     * @param lats
     * @param lonlat
     * @param level
     * @param disVtiAndVti
     * @return java.util.List<java.util.Map < java.lang.String, double [ ] [ ]>>
     */
    public static List<Map<String, double[][]>> getInterpolateRainDatasArray2(String dataType, String element, double[][] datas, double[][] prefDatas, double[] lons, double[] lats, double[] lonlat, String level, int[] disVtiAndVti)
    {
        List<Map<String, double[][]>> result = new ArrayList<>();
        double lonDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lons[0] - lons[1]), 3));
        double latDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lats[0] - lats[1]), 3));
        int czLonCount = datas[0].length;
        int czLatCount = datas.length;
        if(lonDis != lon_dis)
        {
            czLonCount = (int) Math.ceil(Math.abs(lonlat[0] - lonlat[2]) / lon_dis);
        }
        if(latDis != lat_dis)
        {
            czLatCount = (int) Math.ceil(Math.abs(lonlat[1] - lonlat[3]) / lat_dis);
        }
        double[][] czDatas = bilinearInterpolation(datas, czLatCount, czLonCount);
        double[][] czPrefDatas = bilinearInterpolation(prefDatas, czLatCount, czLonCount);
        level = level == null ? "" : "_" + level;
        int dis_vti = disVtiAndVti[0];
        if(dis_vti == 0)
        {
            dis_vti = 1;
        }
        int bl = 1;
        if(dataType.equals(DataTypeEnum.ECMF.getDataType()))
        {
            bl = 1000;
        }
        for(int i = 0, count = czDatas.length; i < count; i++)
        {
            for(int j = 0, num = czDatas[i].length; j < num; j++)
            {
                czDatas[i][j] = NumberFormatUtil.numFormat(bl * (czDatas[i][j] - czPrefDatas[i][j]) / dis_vti, 3);
            }
        }
        double[] t = GribUtil.getCzVtiZone(dataType, disVtiAndVti[0], disVtiAndVti[1]);
        if(t != null)
        {
            double[][][] arrays = BilinearInterpolationForTwoArrays.generateInterpolatedArrays(czPrefDatas, czDatas, t);

            for(int i = 0, count = arrays.length; i < count; i++)
            {
                for(int j = 0, num = arrays[i].length; j < num; j++)
                {
                    for(int k = 0, num2 = arrays[i][j].length; k < num2; k++)
                    {
                        if(arrays[i][j][k] < 0)
                        {
                            arrays[i][j][k] = 0;
                        }
                        arrays[i][j][k] = NumberFormatUtil.numFormat(arrays[i][j][k], 3);
                    }
                }
                Map<String, double[][]> map = new LinkedHashMap<>();
                map.put(element + level, arrays[i]);
                result.add(map);
            }
        }
        Map<String, double[][]> map = new LinkedHashMap<>();
        map.put(element + level, czDatas);
        result.add(map);

        return result;
    }

    /**
     * @category 抽取指定区域降水数据时，处理预报数据的第一个时效的数据
     * @date 2025/2/12 15:12
     * @param element
     * @param datas
     * @param lons
     * @param lats
     * @param lonlat
     * @param level
     * @param disVtiAndVti
     * @return java.util.List<java.util.Map < java.lang.String, double [ ] [ ]>>
     */
    public static List<Map<String, double[][]>> getInterpolateRainDatasArray1(String dataType, String element, double[][] datas, double[] lons, double[] lats, double[] lonlat, String level, int[] disVtiAndVti)
    {
        List<Map<String, double[][]>> result = new ArrayList<>();
        double lonDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lons[0] - lons[1]), 3));
        double latDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lats[0] - lats[1]), 3));
        int czLonCount = datas[0].length;
        int czLatCount = datas.length;
        if(lonDis != lon_dis)
        {
            czLonCount = (int) Math.ceil(Math.abs(lonlat[0] - lonlat[2]) / lon_dis);
        }
        if(latDis != lat_dis)
        {
            czLatCount = (int) Math.ceil(Math.abs(lonlat[1] - lonlat[3]) / lat_dis);
        }
        int bl = 1;
        if(dataType.equals(DataTypeEnum.ECMF.getDataType()))
        {
            bl = 1000;
        }
        for(int i = 0, count = datas.length; i < count; i++)
        {
            for(int j = 0, num = datas[i].length; j < num; j++)
            {
                datas[i][j] = bl * datas[i][j];
            }
        }
        double[][] czDatas = bilinearInterpolation(datas, czLatCount, czLonCount);
        level = level == null ? "" : "_" + level;
        Map<String, double[][]> map = new LinkedHashMap<>();
        map.put(element + level, czDatas);
        result.add(map);

        return result;
    }

    public static Map<String, Double> getUVDatas(String element, double[][] datasU, double[][] datasV, double[] lons, double[] lats, int[] index, double level, String aroundStr)
    {
        Map<String, Double> result = new LinkedHashMap<>();
        String levelStr = level == 999 ? null : level + "";
        float around = Float.parseFloat(aroundStr) / 2;
        double lonDis = Math.abs(lons[0] - lons[1]);
        double latDis = Math.abs(lats[0] - lats[1]);
        int lonCount = (int) Math.ceil(around / lonDis);
        int latCount = (int) Math.ceil(around / latDis);
        double value;
        for(int i = index[1] - latCount, count = index[1] + latCount; i < count; i++)
        {
            for(int j = index[0] - lonCount, num = index[0] + lonCount; j < num; j++)
            {
                value = Math.sqrt(datasU[i][j] * datasU[i][j] + datasV[i][j] * datasV[i][j]);
                result.put(element + (levelStr == null ? "" : "_" + levelStr) + "_" + NumberFormatUtil.scienceD(lats[j], 2) + "_" +
                                                                                      NumberFormatUtil.scienceD(lons[i], 2), value);
            }
        }

        return result;
    }

    public static List<Map<String, Double>> getInterpolateUVDatas(String dataType, String element, double[][] datasU, double[][] datasV, double[][] prefDatasU, double[][] prefDatasV, double[] lons, double[] lats, int[] index, double level, String aroundStr, int[] disVtiAndVti)
    {
        List<Map<String, Double>> result = new ArrayList<>();
        String[] split = element.split(",");
        String ws = split[0];
        String wd = split[1];
        String levelStr = (int)level + "";
        if(level == 10)
        {
            levelStr = "";
        }
        else
        {
            levelStr = "_" + levelStr;
        }
        float around = Float.parseFloat(aroundStr) / 2;
        double lonDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lons[0] - lons[1]), 3));
        double latDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lats[0] - lats[1]), 3));
        int lonCount = (int) Math.ceil(around / Double.parseDouble(NumberFormatUtil.scienceD(lonDis, 3)));
        int latCount = (int) Math.ceil(around / Double.parseDouble(NumberFormatUtil.scienceD(latDis, 3)));
        int lonCountNew = lonCount * 2;
        int latCountNew = latCount * 2;
        double[] lonsNew = new double[lonCountNew];
        double[] latsNew = new double[latCountNew];
        double valueWs;
        double valueWd;
        double[][] datasUNew = new double[latCountNew][lonCountNew];
        double[][] prefDatasUNew = new double[latCountNew][lonCountNew];
        double[][] datasVNew = new double[latCountNew][lonCountNew];
        double[][] prefDatasVNew = new double[latCountNew][lonCountNew];
        for(int i = index[1] - latCount, ii = 0, count = index[1] + latCount; i < count; i++, ii++)
        {
            latsNew[ii] = lats[i];
            for(int j = index[0] - lonCount, jj = 0, num = index[0] + lonCount; j < num; j++, jj++)
            {
                lonsNew[jj] = lons[j];
                datasUNew[ii][jj] = datasU[i][j];
                prefDatasUNew[ii][jj] = prefDatasU[i][j];
                datasVNew[ii][jj] = datasV[i][j];
                prefDatasVNew[ii][jj] = prefDatasV[i][j];
            }
        }

        int czLonCount = lonCountNew;
        int czLatCount = latCountNew;
        if(lonDis != lon_dis)
        {
            czLonCount = (int) Math.ceil(around * 2 / lon_dis);
        }
        if(latDis != lat_dis)
        {
            czLatCount = (int) Math.ceil(around * 2 / lat_dis);
        }
        double[][] czUDatas = bilinearInterpolation(datasUNew, czLatCount, czLonCount);
        double[][] czUPrefDatas = bilinearInterpolation(prefDatasUNew, czLatCount, czLonCount);
        double[][] czVDatas = bilinearInterpolation(datasVNew, czLatCount, czLonCount);
        double[][] czVPrefDatas = bilinearInterpolation(prefDatasVNew, czLatCount, czLonCount);

        int dis_vti = disVtiAndVti[0];
        int vti = disVtiAndVti[1];

        if(vti <= 3)
        {
            Map<String, Double> map = new LinkedHashMap<>();
            for(int i = 0; i < czLatCount; i++)
            {
                for(int j = 0; j < czLonCount; j++)
                {
                    valueWs = Math.sqrt(czUDatas[i][j] * czUDatas[i][j] + czVDatas[i][j] * czVDatas[i][j]);
                    valueWd = (270 - Math.toDegrees(Math.atan2(czVDatas[i][j], czUDatas[i][j]))) % 360d;
                    map.put(ws + levelStr + "_" +
                                    NumberFormatUtil.scienceD(latsNew[0] + j * lat_dis, 3) + "_" +
                                    NumberFormatUtil.scienceD(lonsNew[0] + i * lon_dis, 3), valueWs);
                    map.put(wd + levelStr + "_" +
                                    NumberFormatUtil.scienceD(latsNew[0] + j * lat_dis, 3) + "_" +
                                    NumberFormatUtil.scienceD(lonsNew[0] + i * lon_dis, 3), valueWd);
                }
            }
            result.add(map);
        }

        double[] t = GribUtil.getCzVtiStation(dataType, dis_vti, vti);

        if(t != null)
        {
            double[][][] arraysU = BilinearInterpolationForTwoArrays.generateInterpolatedArrays(czUPrefDatas, czUDatas, t);
            double[][][] arraysV = BilinearInterpolationForTwoArrays.generateInterpolatedArrays(czVPrefDatas, czVDatas, t);

            for(int i = 0, count = arraysU.length; i < count; i++)
            {
                Map<String, Double> map = new LinkedHashMap<>();
                for(int j = 0, num = arraysU[i].length; j < num; j++)
                {
                    for(int k = 0, total = arraysU[i][j].length; k < total; k++)
                    {
                        valueWs = Math.sqrt(arraysU[i][j][k] * arraysU[i][j][k] + arraysV[i][j][k] * arraysV[i][j][k]);
                        valueWd = (270 - Math.toDegrees(Math.atan2(arraysV[i][j][k], arraysU[i][j][k]))) % 360d;
                        map.put(ws + "_" + levelStr + "_" +
                                        NumberFormatUtil.scienceD(latsNew[0] + k * lat_dis, 3) + "_" +
                                        NumberFormatUtil.scienceD(lonsNew[0] + j * lon_dis, 3), valueWs);
                        map.put(wd + "_" + levelStr + "_" +
                                        NumberFormatUtil.scienceD(latsNew[0] + k * lat_dis, 3) + "_" +
                                        NumberFormatUtil.scienceD(lonsNew[0] + j * lon_dis, 3), valueWd);
                    }
                }
                result.add(map);
            }
        }
        Map<String, Double> map = new LinkedHashMap<>();
        for(int i = 0, count = czLatCount; i < count; i++)
        {
            for(int j = 0, num = czLonCount; j < num; j++)
            {
                valueWs = Math.sqrt(czUDatas[i][j] * czUDatas[i][j] + czVDatas[i][j] * czVDatas[i][j]);
                valueWd = (270 - Math.toDegrees(Math.atan2(czVDatas[i][j], czUDatas[i][j]))) % 360d;
                map.put(ws + "_" + levelStr + "_" + NumberFormatUtil.scienceD(latsNew[0] + j * lat_dis, 3) + "_" +
                                                    NumberFormatUtil.scienceD(lonsNew[0] + i * lon_dis, 3), valueWs);
                map.put(wd + "_" + levelStr + "_" + NumberFormatUtil.scienceD(latsNew[0] + j * lat_dis, 3) + "_" +
                                                    NumberFormatUtil.scienceD(lonsNew[0] + i * lon_dis, 3), valueWd);
            }
        }
        result.add(map);

        return result;
    }

    public static List<Map<String, Double>> getInterpolateUVDatasOne(String dataType, String element, double[][] datasU, double[][] datasV, double[] lons, double[] lats, int[] index, double level, String aroundStr, int[] disVtiAndVti)
    {
        List<Map<String, Double>> result = new ArrayList<>();
        String[] split = element.split(",");
        String ws = split[0];
        String wd = split[1];
        String levelStr = (int)level + "";
        float around = Float.parseFloat(aroundStr) / 2;
        double lonDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lons[0] - lons[1]), 3));
        double latDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lats[0] - lats[1]), 3));
        int lonCount = (int) Math.ceil(around / Double.parseDouble(NumberFormatUtil.scienceD(lonDis, 3)));
        int latCount = (int) Math.ceil(around / Double.parseDouble(NumberFormatUtil.scienceD(latDis, 3)));
        int lonCountNew = lonCount * 2;
        int latCountNew = latCount * 2;
        double[] lonsNew = new double[lonCountNew];
        double[] latsNew = new double[latCountNew];
        double valueWs;
        double valueWd;
        double[][] datasUNew = new double[latCountNew][lonCountNew];
        double[][] datasVNew = new double[latCountNew][lonCountNew];
        for(int i = index[1] - latCount, ii = 0, count = index[1] + latCount; i < count; i++, ii++)
        {
            latsNew[ii] = lats[i];
            for(int j = index[0] - lonCount, jj = 0, num = index[0] + lonCount; j < num; j++, jj++)
            {
                lonsNew[jj] = lons[j];
                datasUNew[ii][jj] = datasU[i][j];
                datasVNew[ii][jj] = datasV[i][j];
            }
        }

        int czLonCount = lonCountNew;
        int czLatCount = latCountNew;
        if(lonDis != lon_dis)
        {
            czLonCount = (int) Math.ceil(around * 2 / lon_dis);
        }
        if(latDis != lat_dis)
        {
            czLatCount = (int) Math.ceil(around * 2 / lat_dis);
        }
        double[][] czUDatas = bilinearInterpolation(datasUNew, czLatCount, czLonCount);
        double[][] czVDatas = bilinearInterpolation(datasVNew, czLatCount, czLonCount);

        Map<String, Double> map = new LinkedHashMap<>();
        for(int i = 0, count = czLatCount; i < count; i++)
        {
            for(int j = 0, num = czLonCount; j < num; j++)
            {
                valueWs = Math.sqrt(czUDatas[i][j] * czUDatas[i][j] + czVDatas[i][j] * czVDatas[i][j]);
                valueWd = (270 - Math.toDegrees(Math.atan2(czVDatas[i][j], czUDatas[i][j]))) % 360d;
                map.put(ws + "_" + levelStr + "_" + NumberFormatUtil.scienceD(latsNew[0] + j * lat_dis, 3) + "_" +
                                                    NumberFormatUtil.scienceD(lonsNew[0] + i * lon_dis, 3), valueWs);
                map.put(wd + "_" + levelStr + "_" + NumberFormatUtil.scienceD(latsNew[0] + j * lat_dis, 3) + "_" +
                                                    NumberFormatUtil.scienceD(lonsNew[0] + i * lon_dis, 3), valueWd);
            }
        }
        result.add(map);

        return result;
    }
    public static List<Map<String, Double>> getInterpolateSwc3kmUVDatas(String dataType, String element, double[][] datasU, double[][] datasV, double[] lons, double[] lats, int[] index, double level, String aroundStr)
    {
        List<Map<String, Double>> result = new ArrayList<>();
        String[] split = element.split(",");
        String ws = split[0];
        String wd = split[1];
        String levelStr = (int)level + "";
        float around = Float.parseFloat(aroundStr) / 2;
//        double lonDis = Math.abs(lons[0] - lons[1]);
//        double latDis = Math.abs(lats[0] - lats[1]);
        double lonDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lons[0] - lons[1]), 3));
        double latDis = Double.parseDouble(NumberFormatUtil.scienceD(Math.abs(lats[0] - lats[1]), 3));
        int lonCount = (int) Math.ceil(around / Double.parseDouble(NumberFormatUtil.scienceD(lonDis, 3)));
        int latCount = (int) Math.ceil(around / Double.parseDouble(NumberFormatUtil.scienceD(latDis, 3)));
        int lonCountNew = lonCount * 2;
        int latCountNew = latCount * 2;
        double[] lonsNew = new double[lonCountNew];
        double[] latsNew = new double[latCountNew];
        double valueWs;
        double valueWd;
        double[][] datasUNew = new double[latCountNew][lonCountNew];
        double[][] datasVNew = new double[latCountNew][lonCountNew];
        for(int i = index[1] - latCount, ii = 0, count = index[1] + latCount; i < count; i++, ii++)
        {
            latsNew[ii] = lats[i];
            for(int j = index[0] - lonCount, jj = 0, num = index[0] + lonCount; j < num; j++, jj++)
            {
                lonsNew[jj] = lons[j];
                datasUNew[ii][jj] = datasU[i][j];
                datasVNew[ii][jj] = datasV[i][j];
            }
        }

        int czLonCount = lonCountNew;
        int czLatCount = latCountNew;
        if(lonDis != lon_dis)
        {
            czLonCount = (int) Math.ceil(around * 2 / lonDis);
        }
        if(latDis != lat_dis)
        {
            czLatCount = (int) Math.ceil(around * 2 / latDis);
        }
        double[][] czUDatas = bilinearInterpolation(datasUNew, czLatCount, czLonCount);
        double[][] czVDatas = bilinearInterpolation(datasVNew, czLatCount, czLonCount);

        Map<String, Double> map = new LinkedHashMap<>();
        for(int i = 0, count = czLatCount; i < count; i++)
        {
            for(int j = 0, num = czLonCount; j < num; j++)
            {
                if(czUDatas[i][j] == DecodeConstants.UNDEF_DOUBLE_VALUE || czVDatas[i][j] == DecodeConstants.UNDEF_DOUBLE_VALUE)
                {
                    valueWs = DecodeConstants.UNDEF_DOUBLE_VALUE;
                    valueWd = DecodeConstants.UNDEF_DOUBLE_VALUE;
                }
                else
                {
                    valueWs = Math.sqrt(czUDatas[i][j] * czUDatas[i][j] + czVDatas[i][j] * czVDatas[i][j]);
                    valueWd = (270 - Math.toDegrees(Math.atan2(czVDatas[i][j], czUDatas[i][j]))) % 360d;
                }
                map.put(ws + "_" + levelStr + "_" + NumberFormatUtil.scienceD(latsNew[0] + j * lat_dis, 3) + "_" +
                                                    NumberFormatUtil.scienceD(lonsNew[0] + i * lon_dis, 3), valueWs);
                map.put(wd + "_" + levelStr + "_" + NumberFormatUtil.scienceD(latsNew[0] + j * lat_dis, 3) + "_" +
                                                    NumberFormatUtil.scienceD(lonsNew[0] + i * lon_dis, 3), valueWd);
            }
        }
        result.add(map);

        return result;
    }

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

    public static double[][] bilinearInterpolationQ(double[][] originalArray, int targetRows, int targetCols)
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
                    result[i][j] = Double.parseDouble(NumberFormatUtil.scienceD(value * 1000, 3));
                }
            }
        }
        return result;
    }

    /**
     * @category 使用最邻近算法在空间维度上插值
     * @date 2025/2/21 8:40
     * @param originalArray
     * @param originalRows
     * @param originalCols
     * @param targetRows
     * @param targetCols
     * @return int[][]
     */
    public static double[][] nearestInterpolation(double[][] originalArray, int originalRows, int originalCols, int targetRows, int targetCols)
    {
        double[][] result = new double[targetRows][targetCols];
        double xRatio = (double) originalCols / targetCols;
        double yRatio = (double) originalRows / targetRows;
        for(int yTarget = 0; yTarget < targetRows; yTarget++)
        {
            for(int xTarget = 0; xTarget < targetCols; xTarget++)
            {
                double xOriginal = (xTarget + 0.5) * xRatio - 0.5;
                double yOriginal = (yTarget + 0.5) * yRatio - 0.5;
                int nearestX = (int) Math.round(xOriginal);
                int nearestY = (int) Math.round(yOriginal);
                nearestX = Math.max(0, Math.min(nearestX, originalCols - 1));
                nearestY = Math.max(0, Math.min(nearestY, originalRows - 1));
                result[yTarget][xTarget] = (int) originalArray[nearestY][nearestX];
            }
        }

        return result;
    }

    public static double[][][] nearestInterpolationByTime(double[][] startData, double[][] endData, int count)
    {
        final int total = count + 1;
        double[][][] result = new double[count][][];
        for(int i = 0; i < count; i++)
        {
            double ratio = (double) i / total;
            double[][] selected = ratio <= 0.5 ? deepCopyArray(startData) : deepCopyArray(endData);
            result[i] = selected;
        }

        return result;
    }

    private static double[][] deepCopyArray(double[][] original)
    {
        double[][] result = new double[original.length][];
        for (int i = 0; i < original.length; i++)
        {
            result[i] = original[i].clone();
        }

        return result;
    }
}
