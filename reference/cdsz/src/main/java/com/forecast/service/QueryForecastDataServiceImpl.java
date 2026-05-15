package com.forecast.service;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.check.pojo.CheckDataGribIndbEntity;
import com.check.pojo.CheckDataIndbEntity;
import com.check.pojo.CheckDataParams;
import com.config.dao.ConfigMapper;
import com.config.pojo.ConfigParams;
import com.config.pojo.HeaderEntity;
import com.constants.DecodeConstants;
import com.forecast.dao.GribForecastDataMapper;
import com.forecast.dao.StationForecastDataMapper;
import com.forecast.pojo.GribForecastDataEntity;
import com.forecast.pojo.GribForecastDataParams;
import com.forecast.pojo.StationForecastDataEntity;
import com.forecast.pojo.StationForecastDataParams;
import com.forecast.service.inf.QueryForecastDataService;
import com.tool.PageBuilder;
import com.tool.PageResult;
import com.util.DbUtils;
import com.util.NumberFormatUtil;
import com.util.ReadPropertiesUtil;
import com.util.ReflectUtil;
import com.util.TimeUtil;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @category
 * @date 2025/3/13 17:24
 * @description TODO
 */
@Service
public class QueryForecastDataServiceImpl implements QueryForecastDataService {
    @Resource
    private StationForecastDataMapper stationForecastDataMapper;
    @Resource
    private ConfigMapper configMapper;
    @Resource
    private GribForecastDataMapper gribForecastDataMapper;
//    private static Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("data_table.properties");

    @Override
    public PageResult queryStationForecastByPage(StationForecastDataParams params) {
        PageResult pageResult = new PageResult();

        String startDateTimeStr = params.getStartDateTime();
//        Date date = TimeUtil.dateTimeStr2date(startDateTimeStr, "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm:ss");
//        Calendar calendar = Calendar.getInstance();
//        calendar.setTime(date);
//        int year = calendar.get(Calendar.YEAR);
//        int month = calendar.get(Calendar.MONTH) + 1;
//        int day = calendar.get(Calendar.DAY_OF_MONTH);
//        int hour = calendar.get(Calendar.HOUR_OF_DAY);
//        LocalDateTime testTime = LocalDateTime.of(year, month, day, hour, 0);
//        String pastTime = getPastTime(testTime);
        params.setDataTime(startDateTimeStr);


        IPage<StationForecastDataEntity> result = stationForecastDataMapper.queryStationForecastByPage(PageBuilder.build(params.getPageNum(), params.getPageSize()), params);
        pageResult.setPage(params.getPageNum());
        pageResult.setPageSize(params.getPageSize());
        pageResult.setTotal(result.getTotal());
        pageResult.setData(result.getRecords());

        return pageResult;
    }

    @Override
    public Map<String, Map<String, List<String>>> queryStationForecast(StationForecastDataParams params) {

        Map<String, String> ptypeMap = new HashMap<>();
        ptypeMap.put("0.0", "晴");
        ptypeMap.put("1.0", "雨");
        ptypeMap.put("2.0", "雪");
        ptypeMap.put("3.0", "少云");
        ptypeMap.put("4.0", "多云");
        ptypeMap.put("5.0", "阴");
        
        Calendar calendar = Calendar.getInstance();
	    calendar.setTime(TimeUtil.dateTimeStr2date(params.getDataTime(), "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm:ss"));
	    calendar.add(Calendar.HOUR_OF_DAY, -8);
	    params.setDataTime(TimeUtil.date2String(calendar.getTime(), "yyyy-MM-dd HH:mm:ss"));
	    calendar.add(Calendar.DATE, 10);
	    params.setEndDateTime(TimeUtil.date2String(calendar.getTime(), "yyyy-MM-dd HH:mm:ss"));

        List<StationForecastDataEntity> dataList = stationForecastDataMapper.queryStationForecast(params);
        ConfigParams params1 = new ConfigParams();
        params1.setMk("fst");
        List<HeaderEntity> stationForecastHeader = configMapper.getStationForecastHeader(params1);
        Map<String, Map<String, List<String>>> result = new HashMap<>();
        StringBuilder values = null;
//        Calendar calendar = Calendar.getInstance();
        result.put("hour", new LinkedHashMap<>());
        for(StationForecastDataEntity data : dataList)
        {
        	if(data.getAt() == DecodeConstants.UNDEF_DOUBLE_VALUE)
        	{
        		continue;
        	}
            String validDate = data.getValiddate();
            calendar.setTime(TimeUtil.String2Date(validDate, TimeUtil.DEFAULT_DATETIME_FORMAT));
            calendar.add(Calendar.HOUR_OF_DAY, 8);
            validDate = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT);
            String[] split = validDate.split("\\s+");
            String dateStr = split[0];
            String hourStr = split[1];
            if(!result.get("hour").containsKey(dateStr))
            {
                result.get("hour").put(dateStr, new ArrayList<>());
            }
            values = new StringBuilder();
            values.append(hourStr.substring(0, hourStr.length() - 3));
            values.append(",");
            int nScale = 10;
            for(HeaderEntity header : stationForecastHeader)
            {
                String element = header.getElement();
                if(element.equals("element"))
                {
                    continue;
                }
                if(!element.equals("vis"))
                {
                    Object value = ReflectUtil.getFieldValueByName(data, element);
                    if(element.equals("ptype"))
                    {
                        values.append(ptypeMap.get(String.valueOf(value)));
                    }
                    else if(element.equals("lcc"))
                    {
                    	values.append((int)format(Double.parseDouble(data.getLcc()) * nScale, 0));
                    }
                    else if(element.equals("n"))
                    {
                    	values.append((int)format(data.getN() * nScale, 0));
                    }
                    else if(element.equals("rain"))
                    {
                    	String rainStr = "";
                    	if(data.getRain() > 0.0333 && data.getRain() < 0.1)
                    	{
                    		rainStr = "T";
                    	}
                    	else
                    	{
                    		rainStr = format(data.getRain(), 1) + "";
                    	}
                    	
//                    	values.append(data.getRain() < 0.1 ? "0.0" : format(data.getRain(), 1));
                    	values.append(rainStr);
                    }
                    else
                    {
                        if(value.getClass().equals(Double.class))
                        {
                            values.append(format(value, 1));
                        }
                        else
                        {
                            values.append(value);
                        }
                    }

                }
                else
                {
                    Object value = ReflectUtil.getFieldValueByName(data, element);
                    double vis = Double.parseDouble(String.valueOf(value));
                    values.append(NumberFormatUtil.numFormat(vis / 1000, 1));
                }
                values.append(",");
            }

            result.get("hour").get(dateStr).add(values.substring(0, values.length() - 1));
        }

        result.put("day", new HashMap<>());
        for(String date : result.get("hour").keySet())
        {
            List<String> lines = result.get("hour").get(date);
            double maxAt = -999999;
            double minAt = 999999;
            double maxWs = -999999;
            for(String line : lines)
            {
                String[] split = line.split(",");
                double value = Double.parseDouble(split[1]);
                double ws = Double.parseDouble(split[2]);
                if(maxAt < value)
                {
                    maxAt = value;
                }
                if(minAt > value)
                {
                    minAt = value;
                }
                if(ws > maxWs)
                {
                    maxWs = ws;
                }
            }
            result.get("day").put(date, new ArrayList<>());
            result.get("day").get(date).add(maxAt + "");
            result.get("day").get(date).add(minAt + "");
            result.get("day").get(date).add(maxWs + "");
        }

        int j = 0;
        for(String date : result.get("hour").keySet())
        {
            List<String> lines = result.get("hour").get(date);
            List<String> newLines = new ArrayList<>();
            int i = 0;
            for(String line : lines)
            {
                String[] split = line.split(",");
                double ws = Double.parseDouble(split[2]);
                String wsStr = getWs(ws);
                double wd = Double.parseDouble(split[3]);
                String wdStr = getWd(wd);
                if(j == 0 && i == 0)
                {
                	line = split[0] + "," + split[1] + "," + wsStr + "," + wdStr + ",," + split[5] + "," + split[6] + "," + split[7] + ",";
                }
                else
                {
                	line = split[0] + "," + split[1] + "," + wsStr + "," + wdStr + "," + split[4] + "," + split[5] + "," + split[6] + "," + split[7] + "," + split[8];
                }
//                line = line.replace("," + ws + ",", "," + wsStr + ",");
                newLines.add(line);
                i++;
            }
            j++;
            result.get("hour").put(date, newLines);
        }
        
        result.put("mae", new HashMap<>());
        calendar.setTime(new Date());
        String endDate = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATE_FORMAT);
        calendar.add(Calendar.DAY_OF_MONTH, -7);
        String startDate = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATE_FORMAT);
        String sql = "select station,datatime,datasource,atmae24,atmae48,atmae72 from public.station_check_value_tab where datatime >='" + 
				   startDate + "' and datatime <= '" + endDate + "' and station = '" + params.getStation() + "' and datasource = 'fst'"; 
        double mae = getLatestMae("fst", "at", "mae", new int[]{24, 48, 72}, sql, params.getStation());
        List<String> maeList = new ArrayList<>();
        maeList.add(mae + "");
        result.get("mae").put("mae", maeList);
        
        return result;
    }
    
    
    private double getLatestMae(String dataSource, String element, String method, int[] vtis, String sql, String station)
    {
    	double resultD = 0;
    	DruidDataSource dataSourceJDBC = DbUtils.getInstance().getDataSource();
    	
    	String checkWzdStationSql = "select * from public.station_info_tab_zj where station_id_d = '" + station + "'";
    	try (Connection conn = dataSourceJDBC.getConnection();
				Statement st = conn.createStatement();
				ResultSet rs = st.executeQuery(checkWzdStationSql);){
			while(rs.next())
			{
				return -1;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	String checkUploadStationSql = "select * from public.station_info_tab_upload where station_id_d = '" + station + "'";
    	try (Connection conn = dataSourceJDBC.getConnection();
				Statement st = conn.createStatement();
				ResultSet rs = st.executeQuery(checkUploadStationSql);){
			while(rs.next())
			{
				return -1;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	Map<String, Map<String, Map<String, Map<String, Double>>>> result = new HashMap<>();
    	
		Map<String, List<List<Double>>> rsMap = new HashMap<>();
		Map<String, double[]> rsResultMap = new HashMap<>();
		List<List<Double>> listss = new ArrayList<>();
		int vtisCount = vtis.length;
		for(int i = 0; i < vtisCount; i++)
		{
			listss.add(new ArrayList<>());
		}
		rsMap.put(dataSource, listss);
		rsResultMap.put(dataSource, new double[vtisCount]);
		result.put(dataSource, new HashMap<>());
		result.get(dataSource).put(element, new HashMap<>());
		result.get(dataSource).get(element).put(method, new LinkedHashMap<>());
		try (Connection conn = dataSourceJDBC.getConnection();
				Statement st = conn.createStatement();
				ResultSet rs = st.executeQuery(sql);){
			while(rs.next())
			{
				for(int i = 4; i <= 6; i++)
				{
					if(rs.getDouble(i) != DecodeConstants.UNDEF_DOUBLE_VALUE)
					{
						rsMap.get(rs.getString(3)).get(i - 4).add(rs.getDouble(i));
					}
				}
			}
			int[] scaleNum = new int[2];
			if(element.equals("atmax") || element.equals("atmin"))
			{
				scaleNum[0] = 100;
				scaleNum[1] = 1;
			}
			else if(element.equals("at"))
			{
				scaleNum[0] = 1;
				scaleNum[1] = 2;
			}
			else if(element.equals("ws"))
			{
				scaleNum[0] = 1;
				scaleNum[1] = 1;
			}
			else
			{
				scaleNum[0] = 1;
				scaleNum[1] = 1;
			}
			
			List<List<Double>> lists = rsMap.get(dataSource);
			for(int i = 0; i < vtisCount; i++)
			{
				List<Double> list = lists.get(i);
				double sum = 0;
				for(double value : list)
				{
					sum += value;
				}
				if(list.size() == 0)
				{
					rsResultMap.get(dataSource)[i] = DecodeConstants.UNDEF_DOUBLE_VALUE;
				}
				else
				{
					rsResultMap.get(dataSource)[i] = NumberFormatUtil.numFormat(scaleNum[0] * sum / list.size(), scaleNum[1]);
				}
			}
			
			
			for(int i = 0; i < vtisCount; i++)
			{
				result.get(dataSource).get(element).get(method).put(vtis[i] + "", rsResultMap.get(dataSource)[i] != DecodeConstants.UNDEF_DOUBLE_VALUE ? rsResultMap.get(dataSource)[i] : null);
			}
			if(element.equals("at") || element.equals("ws"))
			{
				Double v24 = result.get(dataSource).get(element).get(method).get("24");
				if(v24 == null)
				{
					resultD = -1;
				}
				else
				{
					Double v48 = result.get(dataSource).get(element).get(method).get("48");
					Double v72 = result.get(dataSource).get(element).get(method).get("72");
					if(v48 == null || v72 == null)
					{
						return -1;
					}
					result.get(dataSource).get(element).get(method).put("0-72", NumberFormatUtil.numFormat(v24 * 0.5 + v48 * 0.333 + v72 * 0.167, 2));
					resultD = NumberFormatUtil.numFormat(v24 * 0.5 + v48 * 0.333 + v72 * 0.167, 2);
				}
				
			}
    	
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	
    	return resultD;
    }
    

    private double format(Object obj, int count)
    {
        double result = 0;
        double value = (double) obj;
        result = NumberFormatUtil.numFormat(value, count);

        return result;
    }

    private String getWs(double ws)
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
    
    private String getWd(double wd)
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

        return result;
    }

    @Override
    public List<GribForecastDataEntity> queryGribForecast(GribForecastDataParams params) {
        Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("data_table.properties");
        params.setTableName(configMap.get(params.getDataSource() + "_rain"));
        int rainVti = params.getRainVti();
//        if(rainVti >= 12)
//        {
//        	params.setRainVti(12);
//        }
        params.setRainVti(rainVti);
        List<GribForecastDataEntity> gribForecastDataEntities = gribForecastDataMapper.queryGribForecast(params);

        return gribForecastDataEntities;
    }

    public static String getPastTime(LocalDateTime t) {
        LocalDate date = t.toLocalDate();
        LocalDateTime d8 = date.atTime(8, 0);     // 当天08:00
        LocalDateTime d20 = date.atTime(20, 0);    // 当天20:00

        // 确定最近的过去时间点（baseTime）
        LocalDateTime baseTime;
        if (t.isEqual(d20) || t.isAfter(d20)) {
            baseTime = d20;                        // 当天20:00
        } else if (t.isEqual(d8) || t.isAfter(d8)) {
            baseTime = d8;                         // 当天08:00
        } else {
            baseTime = d20.minusDays(1);           // 前一天的20:00
        }

        // 计算时间差（注意顺序：baseTime到t的持续时间）
        Duration duration = Duration.between(baseTime, t);
        if (duration.toHours() <= 2) {             // 不足或等于2小时
            baseTime = baseTime.minusHours(12);    // 向前推12小时
        }
        String timeStr = TimeUtil.dateTimeStr2Str(baseTime.toString().replace("T", " "), "yyyy-MM-dd HH", "yyyy/MM/dd HH:mm");

        return timeStr;
    }

	@Override
	public List<CheckDataIndbEntity> queryStationCheckHourDataFromDb(
			CheckDataParams params) {
		
		return stationForecastDataMapper.queryStationCheckHourDataFromDb(params);
	}

	@Override
	public List<CheckDataGribIndbEntity> queryGribCheckHourDataFromDb(
			CheckDataParams params) {
		
		return gribForecastDataMapper.queryGribCheckHourDataFromDb(params);
	}
}
