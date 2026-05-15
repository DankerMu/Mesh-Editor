package com.station.indb.util;

import com.constants.DecodeConstants;
//import com.grib.NcReader;
//import com.t1279.entity.ElementConfigBean;
import com.util.NumberFormatUtil;
import com.util.ReadGribRainUtil;

import java.util.HashMap;
import java.util.Map;

public class DealDatasUtil {

    public static Map<String, ElementConfigBean> map = new HashMap<>();
//    static
//    {
//        Map<String, String> configMap = ReadPropertiesUtil.getT1279ConfigMap("element_config.properties");
//        ElementConfigBean configBean = null;
//        String[] split = null;
//        for(String key : configMap.keySet())
//        {
//            configBean = new ElementConfigBean();
//            split = configMap.get(key).split(",");
//            configBean.setElementName(split[0]);
//            configBean.setElementCnName(split[1]);
//            configBean.setScale(Float.parseFloat(split[2]));
//            configBean.setOffset(Float.parseFloat(split[3]));
//            configBean.setUnit(split[4]);
//            map.put(key, configBean);
//        }
//    }

    public static float[] dealAtDatas(float[] datas)
    {
        int length = datas.length;
        float[] result = new float[length];
        for(int i = 0; i < length; i++)
        {
            result[i] = datas[i] - 273.15f;
        }

        return result;
    }

    public static float[] dealHeightDatas(float[] datas)
    {
        int length = datas.length;
        float[] result = new float[length];
        for(int i = 0; i < length; i++)
        {
            result[i] = datas[i] * 9.8f;
        }

        return result;
    }

    public static float[] dealRhDatas(float[] datas)
    {
        int length = datas.length;
        float[] result = new float[length];
        for(int i = 0; i < length; i++)
        {
            result[i] = datas[i] > 100 ? 100 : datas[i];
        }

        return result;
    }

    public static float[] dealDatasByConfig(String element, float[] datas)
    {
        int length = datas.length;
        float[] result = new float[length];
        ElementConfigBean configBean = map.get(element);
        float scale = 1;
        float offset = 0;
        if(configBean != null)
        {
            scale = configBean.getScale();
            offset = configBean.getOffset();
        }
        for(int i = 0; i < length; i++)
        {
            result[i] = datas[i] * scale + offset;
        }

        return result;
    }

    public static double[] calWsWd(double u, double v)
    {
        double[] result = new double[2];
        result[0] = Math.sqrt(u * u + v * v);
        result[1] = (270 - Math.toDegrees(Math.atan2(v, u))) % 360d;
        result[0] = NumberFormatUtil.numFormat(result[0], 3);
        result[1] = NumberFormatUtil.numFormat(result[1], 3);

        return result;
    }

    public static double[][] getFstValue(double[] lons, double[] lats, double lon, double lat, double[][] values, String lonlat)
    {
//        Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
//        String lonlat = configMap.get("lonlat");
        String[] split = lonlat.split(",");
        double[] lonlats = new double[split.length];
        for(int i = 0, count = lonlats.length; i < count; i++)
        {
            lonlats[i] = Double.parseDouble(split[i]);
        }

        double lon_dis = Math.abs(lons[0] - lons[1]);
        double lat_dis = Math.abs(lats[0] - lats[1]);
        double[] findFirstPoint = ReadGribRainUtil.findFindFirstPoint(lon, lat, lon_dis, lat_dis, lonlats);
//        double[] findFirstPoint = findFirstPointAndLonLat[0];
        int lonIndex = (int) findFirstPoint[0];
        int latIndex = (int) findFirstPoint[1];
        double topLeft = values[latIndex][lonIndex];
        double topRight = values[latIndex][lonIndex + 1];
        double bottomLeft = values[latIndex + 1][lonIndex];
        double bottomRight = values[latIndex + 1][lonIndex + 1];
        double value = ReadGribRainUtil.bilinearInterpolation(topLeft, topRight, bottomLeft, bottomRight, findFirstPoint[2], findFirstPoint[3]);
        if(Double.isNaN(value))
        {
            value = DecodeConstants.UNDEF_DOUBLE_VALUE;
        }
        double[] pointValues = new double[]{topLeft, topRight, bottomLeft, bottomRight, value};
        double[] pointLonLats = new double[]{lons[lonIndex], lats[latIndex],
                                             lons[lonIndex + 1], lats[latIndex],
                                             lons[lonIndex], lats[latIndex + 1],
                                             lons[lonIndex + 1], lats[latIndex + 1]};
        for(int i = 0; i < pointValues.length; i++)
        {
            pointValues[i] = NumberFormatUtil.numFormat(pointValues[i], 3);
        }
        for(int i = 0; i < pointLonLats.length; i++)
        {
            pointLonLats[i] = NumberFormatUtil.numFormat(pointLonLats[i], 3);
        }
        double[][] result = new double[2][];
        result[0] = pointValues;
        result[1] = pointLonLats;

        return result;
    }

    public static double[][] getFstValueWsWd(double[] lons, double[] lats, double lon, double lat, double[][] valuesU, double[][] valuesV, String lonlat)
    {
//        Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
//        String lonlat = configMap.get("lonlat");
        String[] split = lonlat.split(",");
        double[] lonlats = new double[split.length];
        for(int i = 0, count = lonlats.length; i < count; i++)
        {
            lonlats[i] = Double.parseDouble(split[i]);
        }
        double lon_dis = Math.abs(lons[0] - lons[1]);
        double lat_dis = Math.abs(lats[0] - lats[1]);
        double[] findFirstPoint = ReadGribRainUtil.findFindFirstPoint(lon, lat, lon_dis, lat_dis, lonlats);
//        double[] lons = findFirstPointAndLonLat[1];
//        double[] lats = findFirstPointAndLonLat[2];
//        double[] findFirstPoint = findFirstPointAndLonLat[0];
        int lonIndex = (int) findFirstPoint[0];
        int latIndex = (int) findFirstPoint[1];
        double topLeftU = valuesU[latIndex][lonIndex];
        double topRightU = valuesU[latIndex + 1][lonIndex];
        double bottomLeftU = valuesU[latIndex][lonIndex + 1];
        double bottomRightU = valuesU[latIndex + 1][lonIndex + 1];
        double valueU = ReadGribRainUtil.bilinearInterpolation(topLeftU, topRightU, bottomLeftU, bottomRightU, findFirstPoint[2], findFirstPoint[3]);
        double topLeftV = valuesV[latIndex][lonIndex];
        double topRightV = valuesV[latIndex + 1][lonIndex];
        double bottomLeftV = valuesV[latIndex][lonIndex + 1];
        double bottomRightV = valuesV[latIndex + 1][lonIndex + 1];
        double valueV = ReadGribRainUtil.bilinearInterpolation(topLeftV, topRightV, bottomLeftV, bottomRightV, findFirstPoint[2], findFirstPoint[3]);
        double[] topLeftWsWd = DealDatasUtil.calWsWd(topLeftU, topLeftV);
        double[] topRightWsWd = DealDatasUtil.calWsWd(topRightU, topRightV);
        double[] bottomLeftWsWd = DealDatasUtil.calWsWd(bottomLeftU, bottomLeftV);
        double[] bottomRightWsWd = DealDatasUtil.calWsWd(bottomRightU, bottomRightV);
        double[] valueWsWd = DealDatasUtil.calWsWd(valueU, valueV);
        double[] pointValuesWs = new double[]{topLeftWsWd[0], topRightWsWd[0], bottomLeftWsWd[0], bottomRightWsWd[0], valueWsWd[0]};
        double[] pointValuesWd = new double[]{topLeftWsWd[1], topRightWsWd[1], bottomLeftWsWd[1], bottomRightWsWd[1], valueWsWd[1]};

        double[] pointLonLats = new double[]{lons[lonIndex], lats[latIndex],
                lons[lonIndex], lats[latIndex + 1],
                lons[lonIndex + 1], lats[latIndex],
                lons[lonIndex + 1], lats[latIndex + 1]};
        for(int i = 0; i < pointValuesWs.length; i++)
        {
            pointValuesWs[i] = NumberFormatUtil.numFormat(pointValuesWs[i], 3);
        }
        for(int i = 0; i < pointValuesWd.length; i++)
        {
            pointValuesWd[i] = NumberFormatUtil.numFormat(pointValuesWd[i], 3);
        }
        for(int i = 0; i < pointLonLats.length; i++)
        {
            pointLonLats[i] = NumberFormatUtil.numFormat(pointLonLats[i], 3);
        }
        double[][] result = new double[3][];
        result[0] = pointValuesWs;
        result[1] = pointValuesWd;
        result[2] = pointLonLats;

        return result;
    }
}
