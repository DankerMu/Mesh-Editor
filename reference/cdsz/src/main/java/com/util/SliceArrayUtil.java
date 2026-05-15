package com.util;

import java.text.DecimalFormat;

public class SliceArrayUtil {

    public static float[][] slice(float[][] values, double[] lon, double[] lat, double[] lonlat)
    {
        int[][] indexArray = getIndexArrayNew(lon, lat, lonlat[0], lonlat[1], lonlat[2], lonlat[3]);

        float[][] tempValue = null;
        if(indexArray[0][0] < indexArray[1][0])
        {
            tempValue = new float[Math.abs(indexArray[0][1] - indexArray[1][1]) + 1][Math.abs(indexArray[0][0] - indexArray[1][0]) + 1];
            for(int i = indexArray[0][1], x = 0; i <= indexArray[1][1]; i++, x++)
            {
                for(int j = indexArray[0][0], y = 0; j <= indexArray[1][0]; j++, y++)
                {
                    tempValue[x][y] = values[i][j];
                }
            }
        }
        else
        {
            int m = 0;
            tempValue = new float[Math.abs(indexArray[0][1] - indexArray[1][1]) + 1][Math.abs(indexArray[0][0] - 359) + 1 + indexArray[1][0] + 1];
            for(int i = indexArray[0][1], x = 0; i <= indexArray[1][1]; i++, x++)
            {
                for(int j = indexArray[0][0], y = 0; j <= 359; j++, y++)
                {
                    tempValue[x][y] = values[i][j];
                    m = y;
                }
            }
            for(int i = indexArray[0][1], z = 0; i <= indexArray[1][1]; i++, z++)
            {
                for(int j = 0, k = m + 1; j <= indexArray[1][0]; j++, k++)
                {
                    tempValue[z][k] = values[i][j];
                }
            }
        }

        return tempValue;
    }

    public static double[][] slice(double[][] values, double[] lon, double[] lat, double[] lonlat)
    {
        int[][] indexArray = getIndexArrayNew(lon, lat, lonlat[0], lonlat[1], lonlat[2], lonlat[3]);

        double[][] tempValue = null;
        if(indexArray[0][0] < indexArray[1][0])
        {
            tempValue = new double[Math.abs(indexArray[0][1] - indexArray[1][1]) + 1][Math.abs(indexArray[0][0] - indexArray[1][0]) + 1];
            for(int i = indexArray[0][1], x = 0; i <= indexArray[1][1]; i++, x++)
            {
                for(int j = indexArray[0][0], y = 0; j <= indexArray[1][0]; j++, y++)
                {
                    tempValue[x][y] = values[i][j];
                }
            }
        }
        else
        {
            int m = 0;
            tempValue = new double[Math.abs(indexArray[0][1] - indexArray[1][1]) + 1][Math.abs(indexArray[0][0] - 359) + 1 + indexArray[1][0] + 1];
            for(int i = indexArray[0][1], x = 0; i <= indexArray[1][1]; i++, x++)
            {
                for(int j = indexArray[0][0], y = 0; j <= 359; j++, y++)
                {
                    tempValue[x][y] = values[i][j];
                    m = y;
                }
            }
            for(int i = indexArray[0][1], z = 0; i <= indexArray[1][1]; i++, z++)
            {
                for(int j = 0, k = m + 1; j <= indexArray[1][0]; j++, k++)
                {
                    tempValue[z][k] = values[i][j];
                }
            }
        }

        return tempValue;
    }

    public static int[][] getIndexArray(double[] lon, double[] lat, double startLon, double startLat, double endLon, double endLat)
    {
        if(startLon < 0)
        {
            startLon = 0;
        }
        if(startLon > 359)
        {
            startLon = 359;
        }
        if(startLat < -90)
        {
            startLat = -90;
        }
        if(startLat > 90)
        {
            startLat = 90;
        }
        if(endLon < 0)
        {
            endLon = 0;
        }
        if(endLon > 359)
        {
            endLon = 359;
        }
        if(endLat < -90)
        {
            endLat = -90;
        }
        if(endLat > 90)
        {
            endLat = 90;
        }

        if(lat[0] < 0)
        {
            lat = ArrayReverse.reverse(lat);
        }

        int startLonIndex = 0;
        int startLatIndex = 0;
        int endLonIndex = 0;
        int endLatIndex = 0;
        for(int i = 0, count = lon.length; i < count; i++)
        {
            if(startLon <= lon[i])
            {
                startLonIndex = i;
                break;
            }
        }
        for(int i = 0, count = lat.length; i < count; i++)
        {
            if(startLat > 0)
            {
                if(startLat >= lat[i])
                {
                    startLatIndex = i;
                    break;
                }
            }
            else
            {
                if(startLat >= lat[i + 91])
                {
                    startLatIndex = i + 91;
                    break;
                }
            }
        }
        for(int i = 0, count = lon.length; i < count; i++)
        {
            if(endLon <= lon[i])
            {
                endLonIndex = i;
                break;
            }
        }
        for(int i = 1, count = lat.length; i < count; i++)
        {
            if(endLat > 0)
            {
                if(endLat >= lat[i])
                {
                    endLatIndex = i;
                    break;
                }
            }
            else
            {
                if(endLat >= lat[i + 91])
                {
                    endLatIndex = i + 91;
                    break;
                }
            }
        }

        int[][] result = new int[][]{{startLonIndex, startLatIndex}, {endLonIndex, endLatIndex}};

        return result;
    }
    
    private static int[][] getIndexArrayNew(double[] lon, double[] lat, double startLon, double startLat, double endLon, double endLat)
    {
        if(startLon > 359)
        {
            startLon = 359;
        }
        if(startLat < -90)
        {
            startLat = -90;
        }
        if(startLat > 90)
        {
            startLat = 90;
        }
        if(endLon > 359)
        {
            endLon = 359;
        }
        if(endLat < -90)
        {
            endLat = -90;
        }
        if(endLat > 90)
        {
            endLat = 90;
        }

        if(lat[1] - lat[0] > 0)
        {
            lat = ArrayReverse.reverse(lat);
        }

        int startLonIndex = 0;
        int startLatIndex = 0;
        int endLonIndex = 0;
        int endLatIndex = 0;
        DecimalFormat df = new DecimalFormat("#.0000");
        for(int i = 0, count = lon.length; i < count; i++)
        {
            String formattedNumber = df.format(lon[i]);
            if(startLon <= Double.parseDouble(formattedNumber))
            {
                startLonIndex = i;
                break;
            }
        }
        for(int i = 0, count = lat.length; i < count; i++)
        {
            if(startLat > 0)
            {
                String formattedNumber = df.format(lat[i]);
                if(startLat >= Double.parseDouble(formattedNumber))
                {
                    startLatIndex = i;
                    break;
                }
            }
            else
            {
                String formattedNumber = df.format(lat[i + 91]);
                if(startLat >= Double.parseDouble(formattedNumber))
                {
                    startLatIndex = i + 91;
                    break;
                }
            }
        }
        endLon = endLon > lon[lon.length - 1] ? endLon - Math.abs(lon[0] - lon[1]) : endLon;
        for(int i = 0, count = lon.length; i < count; i++)
        {
            String formattedNumber = df.format(lon[i]);
            if(endLon <= Double.parseDouble(formattedNumber))
            {
                endLonIndex = i;
                break;
            }
        }
        for(int i = 1, count = lat.length; i < count; i++)
        {
            if(endLat >= 0)
            {
                String formattedNumber = df.format(lat[i]);
                if(endLat >= Double.parseDouble(formattedNumber))
                {
                    endLatIndex = i;
                    break;
                }
            }
            else
            {
                String formattedNumber = df.format(lat[i + 91]);
                if(endLat >= Double.parseDouble(formattedNumber))
                {
                    endLatIndex = i + 91;
                    break;
                }
            }
        }

        int[][] result = new int[][]{{startLonIndex, startLatIndex}, {endLonIndex, endLatIndex}};

        return result;
    }
}
