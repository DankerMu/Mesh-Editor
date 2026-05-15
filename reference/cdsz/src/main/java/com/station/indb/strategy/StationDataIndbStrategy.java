package com.station.indb.strategy;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.hutool.core.io.FileUtil;

import com.alibaba.fastjson.JSONObject;
import com.station.indb.inf.DataIndbStrategy;
import com.station.indb.util.QueryStationsInfoFromDBUtil;
import com.station.indb.util.ReadTableConfigMapUtils;
import com.station.indb.util.StringSplit;
import com.util.TimeUtil;

/**
 * @category
 * @date 2025/3/17 10:48
 * @description TODO
 */
public class StationDataIndbStrategy implements DataIndbStrategy {

//	private static Map<String, double[]> stationLonlats = QueryStationsInfoFromDBUtil.queryAllStationsInfo();
//    private static Map<String, double[]> stationLonlats = ReadPropertiesUtil.getStationInfoConfigMap("stations_info.properties");
    @Override
    public Map<String, List<JSONObject>> getMapDataIndbList(String filePath, String tableName) {
        Map<String, List<JSONObject>> result = new HashMap<>();
        Map<String, double[]> stationLonlats = QueryStationsInfoFromDBUtil.queryAllStationsInfo();
//        55555-2025010108.csv
        if(!FileUtil.exist(filePath))
        {
        	return result;
        }
        List<String> lines = FileUtil.readLines(filePath, "utf-8");
//        "station", "lon", "lat", "datatime", "year", "month", "day", "hour", "at", "rh", "wd", "ws", "slp", "vis", "n", "rain", "filename", "filepath", "inserttime"
        String prefix = FileUtil.getPrefix(filePath);
        String[] split = prefix.split("-");
        String station = split[0];
        String dateStr = split[1];
//        analDate,validDate,lcc,tcc,pr,t2,vis,ws,wd
        String header = lines.get(0);
        String[] headerArray = header.split(",");
        Map<String, String> stationMap = ReadTableConfigMapUtils.stationMap;
        Map<String, Integer> elementIndexMap = new HashMap<>();
        for(int i = 0, count = headerArray.length; i < count; i++)
        {
            elementIndexMap.put(stationMap.get(headerArray[i]), i);
        }
        String[] datas = null;
        String year = null;
        String month = null;
        String day = null;
        String hour = null;
        String dataTimeStr = null;
        Date dataTimeDate = null;
        List<JSONObject> list = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        for(int i = 1, count = lines.size(); i < count; i++)
        {
            datas = lines.get(i).replace("/", "-").split(",");
            JSONObject json = new JSONObject();

            json.put(stationMap.get("station"), station);
//            String datatime = stationMap.get("analDate");
//            dataTimeStr = datas[elementIndexMap.get(datatime)];
            dataTimeStr = dateStr;
            calendar.setTime(TimeUtil.String2Date(dataTimeStr, "yyyyMMddHH"));
            calendar.add(Calendar.HOUR_OF_DAY, -8);
            dataTimeDate = calendar.getTime();
            dataTimeStr = TimeUtil.date2String(calendar.getTime(), "yyyy-MM-dd HH:mm:ss");
//            dataTimeStr = TimeUtil.dateTimeStr2Str(dataTimeStr, "yyyy-MM-dd HH:mm", "yyyy-MM-dd HH:mm:ss");
            json.put(stationMap.get("analDate"), dataTimeStr);
//            2025/1/1 8:00
            dataTimeStr = dataTimeStr.replace(" ", "-");
            String[] dataTime = StringSplit.split(dataTimeStr, "- :");
            year = dataTime[0];
            month = dataTime[1];
            day = dataTime[2];
            hour = dataTime[3];
//            System.out.println("filePath: " + filePath);
            json.put(stationMap.get("lon"), stationLonlats.get(station)[0]);
            json.put(stationMap.get("lat"), stationLonlats.get(station)[1]);
            json.put(stationMap.get("year"), Integer.parseInt(year));
            json.put(stationMap.get("month"), Integer.parseInt(month));
            json.put(stationMap.get("day"), Integer.parseInt(day));
            json.put(stationMap.get("hour"), Integer.parseInt(hour));
            String validDateStr = datas[elementIndexMap.get(stationMap.get("validDate"))];
            calendar.setTime(TimeUtil.String2Date(validDateStr, "yyyy-MM-dd HH:mm"));
            calendar.add(Calendar.HOUR_OF_DAY, -8);
            int vti = TimeUtil.getTimeDisHour(dataTimeDate, calendar.getTime());
            validDateStr = TimeUtil.date2String(calendar.getTime(), "yyyy-MM-dd HH:mm:ss");
//            validDateStr = TimeUtil.dateTimeStr2Str(validDateStr, "yyyy-MM-dd HH:mm", "yyyy-MM-dd HH:mm:ss");
            json.put(stationMap.get("validDate"), validDateStr);
            json.put(stationMap.get("vti"), vti);
            json.put(stationMap.get("lcc"), Double.parseDouble(datas[elementIndexMap.get(stationMap.get("lcc"))]));
            json.put(stationMap.get("tcc"), Double.parseDouble(datas[elementIndexMap.get(stationMap.get("tcc"))]));
            json.put(stationMap.get("pr"), Double.parseDouble(datas[elementIndexMap.get(stationMap.get("pr"))]));
            json.put(stationMap.get("t2"), Double.parseDouble(datas[elementIndexMap.get(stationMap.get("t2"))]));
            json.put(stationMap.get("vis"), Double.parseDouble(datas[elementIndexMap.get(stationMap.get("vis"))]));
            json.put(stationMap.get("ws10"), Double.parseDouble(datas[elementIndexMap.get(stationMap.get("ws10"))]));
            json.put(stationMap.get("wd10"), Double.parseDouble(datas[elementIndexMap.get(stationMap.get("wd10"))]));
            json.put(stationMap.get("ptype"), Integer.parseInt(datas[elementIndexMap.get(stationMap.get("ptype"))]));
            json.put(stationMap.get("tp"), Double.parseDouble(datas[elementIndexMap.get(stationMap.get("tp"))]));
            json.put(stationMap.get("tmax_24h"), Double.parseDouble(datas[elementIndexMap.get(stationMap.get("tmax_24h"))]));
            json.put(stationMap.get("tmin_24h"), Double.parseDouble(datas[elementIndexMap.get(stationMap.get("tmin_24h"))]));
            json.put(stationMap.get("filename"), FileUtil.getName(filePath));
            json.put(stationMap.get("filepath"), filePath);
            json.put(stationMap.get("inserttime"), TimeUtil.date2String(new Date(), TimeUtil.DEFAULT_DATETIME_FORMAT));
            list.add(json);
        }
//        result.put(ReadTableConfigMapUtils.tableMap.get(DataTypeEnum.STATION.getDataType()), list);
        result.put(tableName, list);


        return result;
    }

    public static void main(String[] args) {
        String filePath = "E:\\fl\\datas\\station/55026-2025050710.csv";
        StationDataIndbStrategy stationDataIndbStrategy = new StationDataIndbStrategy();
        stationDataIndbStrategy.getMapDataIndbList(filePath, "");
    }

	@Override
	public void setStationLonlats(Map<String, double[]> stationLonlats) {
		
	}
}
