package com.util;

import ucar.ma2.Array;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.Map;

public class ReadLonLatUtil {
    public static double[] readLonLat(Map<String, Object> datasMap, String lonlat)
    {
        double[] result = null;
        Variable variable = (Variable) datasMap.get(lonlat);
        if(variable == null)
        {
        	return result;
        }
        try {
            Array array = variable.read();
            if(array != null)
            {
                Object object = array.copyTo1DJavaArray();
                if(object.getClass() == double[].class)
                {
                    result = (double[]) object;
                }
                else if(object.getClass() == float[].class)
                {
                    float[] tem = (float[]) object;
                    result = new double[tem.length];
                    for(int i = 0; i < result.length; i++)
                    {
                        result[i] = tem[i];
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        return result;
    }
}
