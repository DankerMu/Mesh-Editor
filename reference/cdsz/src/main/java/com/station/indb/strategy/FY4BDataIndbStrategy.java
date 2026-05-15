package com.station.indb.strategy;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.hutool.core.io.FileUtil;

import com.alibaba.fastjson.JSONObject;
import com.constants.DataTypeEnum;
import com.station.indb.inf.DataIndbStrategy;
import com.station.indb.util.ReadTableConfigMapUtils;
import com.util.TimeUtil;

/**
 * @category
 * @date 2025/6/4 10:12
 * @description TODO
 */
public class FY4BDataIndbStrategy implements DataIndbStrategy {
    @Override
    public Map<String, List<JSONObject>> getMapDataIndbList(String filePath, String tableName) {
        Map<String, List<JSONObject>> result = new HashMap<>();
        List<String> lines = FileUtil.readLines(filePath, "utf-8");
        String header = lines.get(0);
        String[] headerArray = header.split(",");
        Map<String, String> stationMap = ReadTableConfigMapUtils.fy4bMap;
        Map<String, Integer> elementIndexMap = new HashMap<>();
        for(int i = 0, count = headerArray.length; i < count; i++)
        {
            elementIndexMap.put(stationMap.get(headerArray[i]), i);
        }
        String[] datas = null;
        String dataTimeStr = null;
        List<JSONObject> list = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        for(int i = 1, count = lines.size(); i < count; i++)
        {
            datas = lines.get(i).replace("/", "-").split(",");
            JSONObject json = new JSONObject();

            json.put(stationMap.get("station_id_d"), datas[elementIndexMap.get(stationMap.get("station_id_d"))]);
            dataTimeStr = datas[elementIndexMap.get(stationMap.get("datetime"))];
            String fmt = "yyyy-MM-dd HH:mm";
            if(dataTimeStr.length() == 10)
            {
                fmt = "yyyy-MM-dd";
            }
            calendar.setTime(TimeUtil.String2Date(dataTimeStr, fmt));
            calendar.add(Calendar.HOUR_OF_DAY, -8);
            dataTimeStr = TimeUtil.date2String(calendar.getTime(), "yyyy-MM-dd HH:mm:ss");
            json.put(stationMap.get("datetime"), dataTimeStr);
            json.put(stationMap.get("tcc"), Double.parseDouble(datas[elementIndexMap.get(stationMap.get("tcc"))]));
            json.put(stationMap.get("tccupdatetime"), TimeUtil.date2String(new Date(), TimeUtil.DEFAULT_DATETIME_FORMAT));
            list.add(json);
        }
        result.put(DataTypeEnum.FY4B.getDataType(), list);

        if(!filePath.contains("_his"))
        {
        	List<JSONObject> fileManagerList = new ArrayList<>();
            String name = FileUtil.getName(filePath);
            String suffix = FileUtil.getSuffix(filePath);
            String vtiDataTime = TimeUtil.dateTimeStr2Str(name.substring(0, 14), TimeUtil.DATE_FMT_YMDHMS, TimeUtil.DEFAULT_DATETIME_FORMAT);
            String[] split = vtiDataTime.split(" ");
            String dataTime = split[0] + " 00:00:00";
            Date date = TimeUtil.dateTimeStr2date(vtiDataTime, TimeUtil.DATE_FMT_YMDHMS, TimeUtil.DATE_FMT_YMDHMS);
            calendar.setTime(date);
            int hour = Integer.parseInt(split[1].split(":")[0]);
            long fileSize = FileUtil.size(new File(filePath));
            String inserttime = TimeUtil.date2String(new Date(), TimeUtil.DEFAULT_DATETIME_FORMAT);
            Map<String, String> nafpMap = ReadTableConfigMapUtils.nafpMap;
            JSONObject json = new JSONObject();
            json.put(nafpMap.get("filename"), name);
            json.put(nafpMap.get("dataTime"), dataTime);
            json.put(nafpMap.get("validdate"), vtiDataTime);
            json.put(nafpMap.get("hour"), hour);
            json.put(nafpMap.get("vti"), 1);
            json.put(nafpMap.get("datatype"), DataTypeEnum.FY4B.getDataType());
            json.put(nafpMap.get("filepath"), filePath);
            json.put(nafpMap.get("inserttime"), inserttime);
            json.put(nafpMap.get("filesize"), fileSize);
            json.put(nafpMap.get("filetype"), suffix);
            fileManagerList.add(json);
            
            result.put("public.nafp_grib_tab", fileManagerList);
        }
        
        
        return result;
    }

	@Override
	public void setStationLonlats(Map<String, double[]> stationLonlats) {
		// TODO Auto-generated method stub
		
	}

}