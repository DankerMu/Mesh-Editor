package com.station.indb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import cn.hutool.core.date.DateUtil;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.util.DateUtils;
import com.util.TimeUtil;

public class DealCheckJob {
	public static void main(String[] args) {
		
		args = new String[]{"2025-08-08", "2025-08-08"};
		
		String startTime = args[0];
		String endTime = args[1];
		
		
		JSONObject jsonStation = new JSONObject();
		jsonStation.put("disVti", "24");
		jsonStation.put("zone", "6");
//		jsonStation.put("dataSources", new String[]{"ecmf"});
		jsonStation.put("dataSources", new String[]{"ecmf", "grapes", "swc9km"});
		jsonStation.put("elements", new String[]{"at","atmax","atmin","ws"});
		
		
		JSONObject jsonGrib = new JSONObject();
		jsonGrib.put("disVti", "24");
		jsonGrib.put("zone", "6");
		jsonGrib.put("dataSources", new String[]{"ecmf", "grapes", "deep"});
//		jsonGrib.put("dataSources", new String[]{});
		jsonGrib.put("dataSourcesOrg", new String[]{"ecmf", "grapes"});
		jsonGrib.put("elements", new String[]{"rain"});
		
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(TimeUtil.String2Date(startTime, TimeUtil.DEFAULT_DATE_FORMAT));
        Date startDate = calendar.getTime();
        calendar.setTime(TimeUtil.String2Date(endTime, TimeUtil.DEFAULT_DATE_FORMAT));
        Date endDate = calendar.getTime();
        List<String> dateList = new ArrayList<>();
        for(int i = 0;; i++)
        {
            calendar.setTime(startDate);
            calendar.add(Calendar.DATE, i);
            if(calendar.getTime().after(endDate))
            {
                break;
            }
            dateList.add(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATE_FORMAT) + " 08:00:00");
            dateList.add(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATE_FORMAT) + " 20:00:00");
        }
        
        for(String date : dateList)
        {
    		jsonStation.put("startValidDate", date);
    		jsonStation.put("endValidDate", date);
    		long time = System.currentTimeMillis();
        	station(jsonStation);
        	System.out.println("station: " + (System.currentTimeMillis() - time));
    		jsonGrib.put("startValidDate", date);
    		jsonGrib.put("endValidDate", date);
    		time = System.currentTimeMillis();
    		grib(jsonGrib);
    		System.out.println("grib: " + (System.currentTimeMillis() - time));
        }
		
		
		
	}
	
	public static void checkIndb(String startTime, String endTime)
	{
		JSONObject jsonStation = new JSONObject();
		jsonStation.put("disVti", "24");
		jsonStation.put("zone", "6");
		jsonStation.put("dataSources", new String[]{"ecmf", "grapes", "swc9km"});
		jsonStation.put("elements", new String[]{"atmax","atmin","at","ws"});
		
		
		JSONObject jsonGrib = new JSONObject();
		jsonGrib.put("disVti", "24");
		jsonGrib.put("zone", "6");
		jsonGrib.put("dataSources", new String[]{"ecmf", "grapes", "deep"});
		jsonGrib.put("dataSourcesOrg", new String[]{"ecmf", "grapes"});
		jsonGrib.put("elements", new String[]{"rain"});
		
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(TimeUtil.String2Date(startTime, TimeUtil.DEFAULT_DATE_FORMAT));
        Date startDate = calendar.getTime();
        calendar.setTime(TimeUtil.String2Date(endTime, TimeUtil.DEFAULT_DATE_FORMAT));
        Date endDate = calendar.getTime();
        List<String> dateList = new ArrayList<>();
        for(int i = 0;; i++)
        {
            calendar.setTime(startDate);
            calendar.add(Calendar.DATE, i);
            if(calendar.getTime().after(endDate))
            {
                break;
            }
            dateList.add(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATE_FORMAT) + " 08:00:00");
            dateList.add(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATE_FORMAT) + " 20:00:00");
        }
        
        for(String date : dateList)
        {
    		jsonStation.put("startValidDate", date);
    		jsonStation.put("endValidDate", date);
    		long time = System.currentTimeMillis();
        	station(jsonStation);
        	System.out.println("station: " + (System.currentTimeMillis() - time));
    		jsonGrib.put("startValidDate", date);
    		jsonGrib.put("endValidDate", date);
    		time = System.currentTimeMillis();
    		grib(jsonGrib);
    		System.out.println("grib: " + (System.currentTimeMillis() - time));
        }
	}
	
	private static void station(JSONObject json)
	{
		System.out.println("站点检验参数：" + json.toString());
		doPost("http://192.168.2.81:7088/cdsz/check/checkStationDataDayIndb", json.toJSONString());
//		doPost("http://localhost:7088/cdsz/check/checkStationDataDayIndb", json.toJSONString());
		
	}
	
	private static void grib(JSONObject json)
	{
		System.out.println("格点检验参数：" + json.toString());
		doPost("http://192.168.2.81:7088/cdsz/check/checkGribDataDayIndb", json.toJSONString());
//		doPost("http://localhost:7088/cdsz/check/checkGribDataDayIndb", json.toJSONString());
	}
	
	public static void month()
	{
		JSONObject json = new JSONObject();
		Date date = new Date();
		String dateStr = TimeUtil.date2String(date, TimeUtil.DEFAULT_DATE_FORMAT);
		String start = dateStr.substring(0, 8);
		int lastDayOfMonth = DateUtil.getLastDayOfMonth(date);
		
		json.put("startTime", start + "01");
		json.put("endTime", start + lastDayOfMonth);
		System.out.println("月统计参数：" + json.toString());
		doPost("http://192.168.2.81:7088/cdsz/check/checkStationDataMonth", json.toJSONString());
		doPost("http://192.168.2.81:7088/cdsz/check/checkGribDataMonth", json.toJSONString());
	}
	
	public static String doPost(String pathUrl, String data) {
        OutputStreamWriter out = null;
        BufferedReader br = null;
        String result = "";

        try {
            URL url = new URL(pathUrl);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(3000000);
            conn.setReadTimeout(3000000);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            conn.connect();
            out = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
            out.write(data);
            out.flush();
            InputStream is = conn.getInputStream();
            br = new BufferedReader(new InputStreamReader(is));

            for(String str = ""; (str = br.readLine()) != null; result = result + str) {
            }

            System.out.println(result);
            is.close();
            conn.disconnect();
        } catch (Exception var17) {
            var17.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }

                if (br != null) {
                    br.close();
                }
            } catch (IOException var16) {
                var16.printStackTrace();
            }

        }

        return result;
    }
}
