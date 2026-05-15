package com.forecast.util;

public class WindUtil {
	
	public static String getWs(double ws)
    {
        String result = "";
        if(ws >= 0.0 && ws <=0.2)
        {
            result = "0级";
        }
        else if(ws >= 0.3 && ws <=1.5)
        {
            result = "1级";
        }
        else if(ws >= 1.6 && ws <=3.3)
        {
            result = "2级";
        }
        else if(ws >= 3.4 && ws <=5.4)
        {
            result = "3级";
        }
        else if(ws >= 5.5 && ws <=7.9)
        {
            result = "4级";
        }
        else if(ws >= 8.0 && ws <=10.7)
        {
            result = "5级";
        }
        else if(ws >= 10.8 && ws <=13.8)
        {
            result = "6级";
        }
        else if(ws >= 13.9 && ws <=17.1)
        {
            result = "7级";
        }
        else if(ws >= 17.2 && ws <=20.7)
        {
            result = "8级";
        }
        else if(ws >= 20.8 && ws <=24.4)
        {
            result = "9级";
        }
        else if(ws >= 24.5 && ws <=28.4)
        {
            result = "10级";
        }
        else if(ws >= 28.5 && ws <=32.6)
        {
            result = "11级";
        }
        else if(ws >= 32.7 && ws <=36.9)
        {
            result = "12级";
        }
        else if(ws >= 37.0 && ws <=41.4)
        {
            result = "13级";
        }
        else if(ws >= 41.5 && ws <=46.1)
        {
            result = "14级";
        }
        else if(ws >= 46.2 && ws <=50.9)
        {
            result = "15级";
        }
        else if(ws >= 51.0 && ws <=56.0)
        {
            result = "16级";
        }
        else if(ws >= 56.1)
        {
            result = "17级";
        }

        return result;
    }
    
    public static String getWd(double wd)
    {
        String result = "";
        if(wd == 999017)
        {
        	return result;
        }
        if(wd > 337.5 || wd <= 22.5)
        {
            result = "北";
        }
        else if(wd > 22.5 && wd <= 67.5)
        {
            result = "东北";
        }
        else if(wd > 67.5 && wd <= 112.5)
        {
            result = "东";
        }
        else if(wd > 112.5 && wd <= 157.5)
        {
            result = "东南";
        }
        else if(wd > 157.5 && wd <= 202.5)
        {
            result = "南";
        }
        else if(wd > 202.5 && wd <= 247.5)
        {
            result = "西南";
        }
        else if(wd > 247.5 && wd <= 292.5)
        {
            result = "西";
        }
        else if(wd >= 292.5 && wd <= 337.5)
        {
            result = "西北";
        }

        return result + "风";
    }
}
