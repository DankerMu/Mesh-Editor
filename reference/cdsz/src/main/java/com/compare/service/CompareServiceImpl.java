package com.compare.service;

import com.alibaba.druid.pool.DruidDataSource;
import com.check.pojo.OrgNafpDataEntity;
import com.check.pojo.OrgNafpDataParams;
import com.compare.pojo.CompareDataParams;
import com.compare.service.inf.CompareService;
import com.config.dao.ConfigMapper;
import com.config.pojo.ConfigParams;
import com.config.pojo.DataSourceType;
import com.config.pojo.HeaderEntity;
import com.constants.DataTypeEnum;
import com.constants.DecodeConstants;
import com.forecast.dao.GribForecastDataMapper;
import com.forecast.dao.StationForecastDataMapper;
import com.forecast.pojo.*;
import com.obs.dao.ObsDataMapper;
import com.obs.pojo.ObsDataEntity;
import com.obs.pojo.ObsDataParams;
import com.station.dao.StationInfoMapper;
import com.station.pojo.StationInfoEntity;
import com.util.*;
import com.util.BilinearInterpolateUtil.InterpolationResult;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @category
 * @date 2025/3/18 10:37
 * @description TODO
 */
@Service
public class CompareServiceImpl implements CompareService {
    @Resource
    private ObsDataMapper obsDataMapper;
    @Resource
    private StationForecastDataMapper stationForecastDataMapper;
    @Resource
    private GribForecastDataMapper gribForecastDataMapper;
    
    @Resource
    private StationInfoMapper stationInfoMapper;

    @Resource
    private ConfigMapper configMapper;
    private double absAt = 273.15;
    private static Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("data_table.properties");
    
    private List<ObsDataEntity> queryBfzObsData(CompareDataParams params)
    {
    	List<ObsDataEntity> result = new ArrayList<>();
//    	select station_id_d as station,
//        datetime,
//        tem as at,
//        tem_max_24h as at_max,
//        tem_min_24h as at_min,
//        pre_1h as rain,
//        pre_24h as rain24,
//        win_d_avg_10mi as wd,
//        win_s_avg_10mi as ws,
//        vis,
//        tcc as n,
//        wep_now as ptype
//		 from public.surf_chn_mul_hor_micaps
//		 where station_id_d = #{station}
//		 and datetime &gt;= #{dataTime}
//		 and datetime &lt;= #{endDateTime}
//		 order by datetime
    	String sql = "select station_id_d as station,datetime,tem as at,tem_max as at_max,tem_min as at_min,pre_1h as rain,pre_24h as rain24,"
    			   + "win_d_avg_10mi as wd,win_s_avg_10mi as ws,vis,tcc as n,wep_now as ptype from public.surf_bfz_mul_hor "
    			   + "where station_id_d = '" + params.getStation() + "' and datetime >= '" + params.getDataTime() + "' and datetime <= '" + params.getEndDateTime() + "'"
    			   + " order by datetime";
    	DruidDataSource dataSource = RDbUtils.getInstance().getDataSource();
    	ObsDataEntity data = null;
    	try (Connection conn = dataSource.getConnection();
    		 Statement st = conn.createStatement();
    		 ResultSet rs = st.executeQuery(sql);){
    		
    		while(rs.next())
    		{
    			data = new ObsDataEntity();
    			data.setStation(rs.getString(1));
    			data.setDatetime(rs.getString(2));
    			data.setAt(rs.getDouble(3));
    			data.setAtmax(rs.getDouble(4));
    			data.setAtmin(rs.getDouble(5));
    			data.setRain(rs.getDouble(6));
    			data.setRain24(rs.getDouble(7));
    			data.setWd(rs.getDouble(8));
    			data.setWs(rs.getDouble(9));
    			data.setVis(rs.getDouble(10));
    			data.setN(rs.getDouble(11));
    			data.setPtype(rs.getDouble(12));
    			result.add(data);
    		}
    	} catch (SQLException e) {
			e.printStackTrace();
		}
    	
    	return result;
    }

    public Map<String, Map<String, List<Map<String, String>>>> compareStationData(CompareDataParams params)
    {
    	String startTimeStr = params.getDataTime();
        StationForecastDataParams stationForecastDataParams = new StationForecastDataParams();
        stationForecastDataParams.setStation(params.getStation());
        stationForecastDataParams.setDataTime(params.getDataTime());
        
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(TimeUtil.String2Date(params.getDataTime(), "yyyy-MM-dd HH:mm:ss"));
        calendar.add(Calendar.HOUR_OF_DAY, -8);

        StationForecastDataParams fstParams = new StationForecastDataParams();
        fstParams.setStation(params.getStation());
        fstParams.setDataTime(TimeUtil.date2String(calendar.getTime(), "yyyy-MM-dd HH:mm:ss"));

        ObsDataParams obsDataParams = new ObsDataParams();
        obsDataParams.setStation(params.getStation());
        obsDataParams.setDataTime(TimeUtil.date2String(calendar.getTime(), "yyyy-MM-dd HH:mm:ss"));
        calendar.add(Calendar.HOUR_OF_DAY, 240);
        String endTime = TimeUtil.date2String(calendar.getTime(), "yyyy-MM-dd HH:mm:ss");
        obsDataParams.setEndDateTime(endTime);
        fstParams.setEndDateTime(endTime);
        calendar.add(Calendar.HOUR_OF_DAY, 8);
        stationForecastDataParams.setEndDateTime(TimeUtil.date2String(calendar.getTime(), "yyyy-MM-dd HH:mm:ss"));
        
        StationInfoEntity stationInfo = new StationInfoEntity();
        stationInfo.setStationIdC(params.getStation());
//        StationInfoEntity stationInfoEntity = stationInfoMapper.queryStationInfoByNum(stationInfo);
//        List<ObsDataEntity> obsDataEntities = null;
//        if(stationInfoEntity.getBfz() == 1)
//        {
//        	//TODO查询边防站实况数据
//        	obsDataEntities = queryBfzObsData(params);
//        }
//        else
//        {
//        	obsDataEntities = obsDataMapper.queryObsData(obsDataParams);
//        }
        List<ObsDataEntity> obsDataEntities = obsDataMapper.queryObsData(obsDataParams);
        Map<String, ObsDataEntity> obsDataMap = new HashMap<>();
        int nScale = 10;
        if(obsDataEntities != null && obsDataEntities.size() > 0)
        {
            for(ObsDataEntity data : obsDataEntities)
            {
                data.setDatetime(TimeUtil.addHours(data.getDatetime(), 8));
                data.setVis(NumberFormatUtil.numFormat((data.getVis() == 9999 || data.getVis() == 999999) ? 999999 : data.getVis() / 1000 , 1));
                data.setLcc((int)NumberFormatUtil.numFormat(data.getLcc() * nScale, 0));
                data.setN(NumberFormatUtil.numFormat(data.getN() * nScale, 0));
                data.setWs(NumberFormatUtil.numFormat(data.getWs(), 1));
                data.setRain(NumberFormatUtil.numFormat(data.getRain(), 1));
                obsDataMap.put(data.getDatetime(), data);
            }
        }
        List<StationForecastDataEntity> stationForecastDataEntities = stationForecastDataMapper.queryStationForecast(fstParams);
        if(stationForecastDataEntities != null && stationForecastDataEntities.size() > 0)
        {
            for(StationForecastDataEntity data : stationForecastDataEntities)
            {
            	if(data.getAt() == DecodeConstants.UNDEF_DOUBLE_VALUE)
            	{
            		data.setValiddate(TimeUtil.addHours(data.getValiddate(), 8));
                    data.setVis(DecodeConstants.UNDEF_DOUBLE_VALUE);
                    data.setAt(DecodeConstants.UNDEF_DOUBLE_VALUE);
                    data.setLcc((int)DecodeConstants.UNDEF_DOUBLE_VALUE + "");
                    data.setN(DecodeConstants.UNDEF_DOUBLE_VALUE);
                    data.setWs(DecodeConstants.UNDEF_DOUBLE_VALUE);
                    data.setRain(DecodeConstants.UNDEF_DOUBLE_VALUE);
            	}
                data.setValiddate(TimeUtil.addHours(data.getValiddate(), 8));
                data.setVis(NumberFormatUtil.numFormat(data.getVis() / 1000 , 1));
                data.setAt(NumberFormatUtil.numFormat(data.getAt(), 1));
                data.setLcc((int)NumberFormatUtil.numFormat(Double.parseDouble(data.getLcc()) * nScale, 0) + "");
                data.setN(NumberFormatUtil.numFormat(data.getN() * nScale, 0));
                data.setWs(NumberFormatUtil.numFormat(data.getWs(), 1));
                data.setRain(NumberFormatUtil.numFormat(data.getRain(), 1));
            }
        }

        String[] dataSources = params.getDataSource();
        Map<String, List<StationForecastDataEntity>> stationGribDatas = new HashMap<>();
        stationGribDatas.put("fst", stationForecastDataEntities);
        
        for(String dataSource : dataSources)
        {
        	dataSource = dataSource.replace("_org", "");
            stationForecastDataParams.setTableName(configMap.get(dataSource + "_value"));
            stationForecastDataParams.setDataTime(TimeUtil.addHours(startTimeStr, -12));
            
            List<StationForecastDataEntity> list = stationForecastDataMapper.queryOrgStationForecast(stationForecastDataParams);
            if(list == null || list.size() == 0)
            {
            	continue;
            }
            
            int scale = 1;
            double scaleN = 10;
            if(dataSource.equals(DataTypeEnum.ECMF.getDataType()))
            {
            	scale = 1000;
            }
            if(dataSource.equals(DataTypeEnum.SWC9KM.getDataType()) || dataSource.equals(DataTypeEnum.GRAPES.getDataType()))
            {
            	scaleN = 0.1;
            }
            list.get(0).setAt(list.get(0).getAt() - absAt);
            for(int i = 1, count = list.size(); i < count; i++)
            {
        		StationForecastDataEntity data = list.get(i);
                data.setAt(NumberFormatUtil.numFormat(data.getAt() >= 9999 ? 999999 : data.getAt() - absAt, 1));
                data.setVis(NumberFormatUtil.numFormat((data.getVis() == 9999 || data.getVis() == 999999) ? 999999 : data.getVis() / 1000, 1));
                data.setLcc(NumberFormatUtil.numFormat(Double.parseDouble(data.getLcc()) * scaleN, 0) + "");
                data.setN(NumberFormatUtil.numFormat(data.getN() * scaleN, 0));
                data.setWs(NumberFormatUtil.numFormat(data.getWs(), 1));
                if(data.getVti() != 0)
                {
                	data.setRain(NumberFormatUtil.numFormat((data.getRain24() - list.get(i - 1).getRain24()) * scale, 1));
                }
            }
            
            List<StationForecastDataEntity> listCz = new ArrayList<>();
            List<StationForecastDataEntity> listCzz = new ArrayList<>();
            List<StationForecastDataEntity> listCzzz = new ArrayList<>();
            if(!dataSource.equals(DataTypeEnum.SWC9KM.getDataType()))
            {
            	List<StationForecastDataEntity> listTem = new ArrayList<>();
                List<StationForecastDataEntity> aftList = new ArrayList<>();
                List<StationForecastDataEntity> zList = new ArrayList<>();
                for(StationForecastDataEntity data : list)
                {
                	int vti = data.getVti();
                	if(vti >= 12 && vti <= 48)//0-36逐小时
                	{
                		listTem.add(data);
                	}
//                	else if(vti > 48 && vti < 72)//
//                	{
//                		aftList.add(data);
//                	}
                	else if(vti >= 48 && vti <= 132)
                	{
                		zList.add(data);
                	}
                	else if(vti >= 132)
                	{
                		aftList.add(data);
                	}
                }
//                Map<String, double[]> valuesMap = new HashMap<>();
//                  element     datetime
                Map<String, Map<String, Double>> valuesMap = new HashMap<>();
                String[] elements = new String[]{"at", "ws", "rain", "vis", "lcc", "n"};
                for(String element : elements)
                {
//                	valuesMap.put(element, new double[listTem.size()]);
                	valuesMap.put(element, new HashMap<>());
                }
                for(String element : elements)
                {
                	for(int i = 0, count = listTem.size(); i < count; i++)
                    {
//                		valuesMap.get(element)[i] = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(listTem.get(i), element)));
                		valuesMap.get(element).put(listTem.get(i).getValiddate(), Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(listTem.get(i), element))));
                    }
                	for(int i = 0, count = zList.size(); i < count; i++)
                	{
//                		valuesMap.get(element)[i] = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(listTem.get(i), element)));
                		valuesMap.get(element).put(zList.get(i).getValiddate(), Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(zList.get(i), element))));
                	}
                	for(int i = 0, count = aftList.size(); i < count; i++)
                	{
//                		valuesMap.get(element)[i] = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(listTem.get(i), element)));
                		valuesMap.get(element).put(aftList.get(i).getValiddate(), Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(aftList.get(i), element))));
                	}
                }
                
                
                StationForecastDataEntity data = null;
                for(int i = 0; i < 37; i++)
                {
                	data = new StationForecastDataEntity();
                	data.setValiddate(TimeUtil.addHours(listTem.get(0).getValiddate(), i));
                	listCz.add(data);
                }
                for(int i = 0; i < 28; i++)
                {
                	if(zList.size() == 0)
                	{
                		break;
                	}
                	data = new StationForecastDataEntity();
                	data.setValiddate(TimeUtil.addHours(zList.get(0).getValiddate(), i * 3));
                	listCzz.add(data);
                }
                for(int i = 0; i < 20; i++)
                {
                	if(aftList.size() == 0)
                	{
                		break;
                	}
                	data = new StationForecastDataEntity();
                	data.setValiddate(TimeUtil.addHours(aftList.get(0).getValiddate(), i * 6));
                	listCzzz.add(data);
                }
                String startTime = TimeUtil.addHours(listTem.get(0).getDatatime(), 12);
                String endTimeStr = TimeUtil.addHours(startTime, 36);
                String startTime1 = zList.size() == 0 ? null : zList.get(0).getValiddate();
                String endTimeStr1 = startTime1 == null ? null : TimeUtil.addHours(startTime1, 93);
                String startTime2 = aftList.size() == 0 ? null : aftList.get(0).getValiddate();
                String endTimeStr2 = startTime2 == null ? null : TimeUtil.addHours(startTime2, 114);
                for(String element : elements)
                {
                	Map<String, Double> map = valuesMap.get(element);
                	InterpolationResult interpolate = BilinearInterpolateUtil.interpolate(map, startTime, endTimeStr, 1, null);
                	double[] ds2 = interpolate.getValues();
                	
//                    	double[] ds = valuesMap.get(element);
//                    	double[] ds2 = UniversalArrayInterpolator.interpolateWithFixedPoints(ds, 37);
                	for(int i = 0; i < 37; i++)
                	{
                		if(element.equals("lcc"))
                		{
                			ReflectUtil.setFieldValueByName(listCz.get(i), (int)NumberFormatUtil.numFormat(ds2[i], 0) + "", element);
                		}
                		else if(element.equals("n"))
                		{
                			ReflectUtil.setFieldValueByName(listCz.get(i), (int)NumberFormatUtil.numFormat(ds2[i], 0), element);
                		}
                		else
                		{
                			ReflectUtil.setFieldValueByName(listCz.get(i), NumberFormatUtil.numFormat(ds2[i], 1), element);
                		}
                	}
                	
                	if(startTime1 != null)
                	{
                		InterpolationResult interpolate1 = BilinearInterpolateUtil.interpolate(map, startTime1, endTimeStr1, 3, null);
                    	double[] ds21 = interpolate1.getValues();
                    	for(int i = 0; i < 28; i++)
                    	{
                    		if(listCzz.size() == 0)
                    		{
                    			break;
                    		}
                    		if(element.equals("lcc"))
                    		{
                    			ReflectUtil.setFieldValueByName(listCzz.get(i), (int)NumberFormatUtil.numFormat(ds21[i], 0) + "", element);
                    		}
                    		else if(element.equals("n"))
                    		{
                    			ReflectUtil.setFieldValueByName(listCzz.get(i), (int)NumberFormatUtil.numFormat(ds21[i], 0), element);
                    		}
                    		else
                    		{
                    			ReflectUtil.setFieldValueByName(listCzz.get(i), NumberFormatUtil.numFormat(ds21[i], 1), element);
                    		}
                    	}
                	}

                	if(startTime2 != null)
                	{
                		InterpolationResult interpolate1 = BilinearInterpolateUtil.interpolate(map, startTime2, endTimeStr2, 6, null);
                    	double[] ds21 = interpolate1.getValues();
                    	for(int i = 0; i < 20; i++)
                    	{
                    		if(listCzzz.size() == 0)
                    		{
                    			break;
                    		}
                    		if(element.equals("lcc"))
                    		{
                    			ReflectUtil.setFieldValueByName(listCzzz.get(i), (int)NumberFormatUtil.numFormat(ds21[i], 0) + "", element);
                    		}
                    		else if(element.equals("n"))
                    		{
                    			ReflectUtil.setFieldValueByName(listCzzz.get(i), (int)NumberFormatUtil.numFormat(ds21[i], 0), element);
                    		}
                    		else
                    		{
                    			ReflectUtil.setFieldValueByName(listCzzz.get(i), NumberFormatUtil.numFormat(ds21[i], 1), element);
                    		}
                    	}
                	}
                	
                }
                listCz.addAll(aftList);
                listCz.addAll(listCzz);
                listCz.addAll(listCzzz);
                
                
            }
            else
            {
            	listCz.addAll(list);
            }
            
            
            
            stationGribDatas.put(dataSource + "_org", listCz);
        }


        Map<String, Map<String, Object>> resultMap = new LinkedHashMap<>();
        resultMap.put("obs", new HashMap<>());
        for(String dataSource : stationGribDatas.keySet())
        {
            resultMap.put(dataSource, new HashMap<>());
            for(StationForecastDataEntity data : stationGribDatas.get(dataSource))
            {
                String validate = data.getValiddate();
                ObsDataEntity obsData = obsDataMap.get(validate);
                resultMap.get("obs").put(validate, obsData);
                if(validate.compareTo(startTimeStr) >= 0)
                {
                	resultMap.get(dataSource).put(validate, data);
                }
            }
        }
        if(resultMap.get("obs").size() == 0)
        {
            resultMap.remove("obs");
        }

        ConfigParams params1 = new ConfigParams();
        params1.setMk("cmp");
        List<HeaderEntity> stationForecastHeader = configMapper.getStationForecastHeader(params1);
        int count = stationForecastHeader.size();
        String[] elements = new String[count];
        for(int i = 1; i < count; i++)
        {
            elements[i - 1] = stationForecastHeader.get(i).getElement();
        }


        calendar.setTime(TimeUtil.String2Date(params.getDataTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
        Date startDate = calendar.getTime();
        calendar.add(Calendar.HOUR_OF_DAY, 240);
        Date endDate = calendar.getTime();
        List<String> dateList = new ArrayList<>();
        for(int i = 0;; i++)
        {
            calendar.setTime(startDate);
            if(i <= 36)
            {
            	
            }
            else if(i > 36 && i < 120)
            {
            	i += 2;
            }
            else
            {
            	i += 5;
            }
            calendar.add(Calendar.HOUR_OF_DAY, i);
            if(calendar.getTime().after(endDate))
            {
                break;
            }
            dateList.add(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
        }
        
        Map<String, Map<String, List<Map<String, String>>>> rs = new HashMap<>();
        for(String dataSource : resultMap.keySet())
        {
            rs.put(dataSource, new HashMap<>());
            Map<String, Object> objectMap = resultMap.get(dataSource);
            for(int i = 0; i < count - 1; i++)
            {
                if(!rs.get(dataSource).containsKey(elements[i]))
                {
                    rs.get(dataSource).put(elements[i], new ArrayList<>());
                }
                for(Object obj : objectMap.values())
                {
                    Map<String, String> strMap = new HashMap<>();

                    StringBuilder sb = new StringBuilder();
//                    String value = "999999";
                    if(obj == null)
                    {
                        continue;
                    }
                    String value = String.valueOf(ReflectUtil.getFieldValueByName(obj, elements[i].replace("_", "")));
                    if("rain".equals(elements[i]) && value.startsWith("9999"))
                    {
                    	value = "0";
                    }
                    if("n".equals(elements[i]))
                    {
                    	value = value.split("\\.")[0];
                    }
                    if("obs".equals(dataSource) && "lcc".equals(elements[i]))
                    {
                    	value = "null";
                    }
                    sb.append(value).append(",");
                    strMap.put(String.valueOf(ReflectUtil.getFieldValueByName(obj, getElementNameByDataSource(dataSource))), sb.substring(0, sb.length() - 1));
                    rs.get(dataSource).get(elements[i]).add(strMap);
                }
            }
        }
        

        for(String dataSource : rs.keySet())
        {
            for(String element : rs.get(dataSource).keySet())
            {
                List<Map<String, String>> maps = rs.get(dataSource).get(element);
                
                
                List<Map<String, String>> listMaps = new ArrayList<>();
                for(Map<String, String> m : maps)
                {
                	for(String dateStr : m.keySet())
                	{
                		if(dateList.contains(dateStr))
                		{
                			if(m.get(dateStr) != null)
                			{
                				listMaps.add(m);
                			}
                			else
                			{
                				Map<String, String> tempMap = new HashMap<>();
                                tempMap.put(dateStr, null);
                                listMaps.add(tempMap);
                			}
                		}
                	}
                }
                
//                Set<String> set = new HashSet<>();
//                for(Map<String, String> map : maps)
//                {
//                    set.add(map.keySet().iterator().next());
//                }
//                for(String dateStr : dateList)
//                {
//                    if(!set.contains(dateStr))
//                    {
//                        Map<String, String> tempMap = new HashMap<>();
//                        tempMap.put(dateStr, null);
//                        maps.add(tempMap);
//                    }
//                }
                for(Map<String, String> map : listMaps)
                {
                	String key = map.keySet().iterator().next();
                	String string = map.get(key);
                	if(string != null && string.startsWith("9999"))
                	{
                		map.put(key, null);
                	}
                }
                List<Map<String, String>> mapList = listMaps.stream().sorted((map11, map22) -> {
                        if (map11.size() != 1 || map22.size() != 1) {
                        throw new IllegalArgumentException("");
                    }
                    String key1 = map11.keySet().iterator().next();
                    String key2 = map22.keySet().iterator().next();
                    int result = key1.compareTo(key2);
                    return true ? result : -result;
                }).collect(Collectors.toList());
                rs.get(dataSource).put(element, mapList);
            }
        }

        ConfigParams configParams = new ConfigParams();
        configParams.setDataType("cmpstation");
        List<DataSourceType> orgList = configMapper.getDataSourceByDataType(configParams);

        Map<String, Map<String, List<Map<String, String>>>> rsMap = new LinkedHashMap<>();
        rsMap.put("obs", rs.get("obs"));
        
        if(rsMap.get("obs") != null)
        {
        	Map<String, List<Map<String, String>>> map = rs.get("obs");
        	for(String element : map.keySet())
        	{
        		List<Map<String, String>> list = map.get(element);
        		Map<String, Map<String, String>> mm = new LinkedHashMap<>();
        		for(Map<String, String> m : list)
        		{
        			for(String k : m.keySet())
        			{
        				mm.put(k, m);
        			}
        		}
        		List<Map<String, String>> lists = new ArrayList<>();
        		for(String dateStr : dateList)
            	{
            		Map<String, String> map2 = mm.get(dateStr);
            		if(map2 == null)
            		{
            			Map<String, String> mp = new HashMap<>();
            			mp.put(dateStr, null);
            			lists.add(mp);
            		}
            		else
            		{
            			lists.add(map2);
            		}
            	}
        		map.put(element, lists);
        	}
        	
        }
        
        
        for(DataSourceType dataSourceType : orgList)
        {
            if(rs.keySet().contains(dataSourceType.getDataSource()))
            {
                rsMap.put(dataSourceType.getDataSource(), rs.get(dataSourceType.getDataSource()));
            }
        }
        rsMap.put("fst", rs.get("fst"));
        
        for(String dataSource : dataSources)
        {
        	if(!rsMap.containsKey(dataSource))
        	{
        		rsMap.put(dataSource, null);
        	}
        }

        for(String dataSource : rsMap.keySet())
        {
            if(rsMap.get(dataSource) == null || rsMap.get(dataSource).size() == 0)
            {
                rsMap.put(dataSource, new HashMap<>());
                for(String method : rsMap.get("fst").keySet())
                {
                    rsMap.get(dataSource).put(method, new ArrayList<>());
                    for(Map<String, String> map : rsMap.get("fst").get(method))
                    {
                        Map<String, String> map1 = new HashMap<>();
                        map1.put(map.keySet().iterator().next(), null);
                        rsMap.get(dataSource).get(method).add(map1);
                    }
                }
            }
        }
        
        if(rsMap.containsKey("swc9km_org"))
        {
        	for(String el : rsMap.get("swc9km_org").keySet())
        	{
        		List<Map<String, String>> list = rsMap.get("swc9km_org").get(el);
        		List<Map<String, String>> newList = new ArrayList<>();
        		for(int k = 0, total = list.size(); k < total; k++)
        		{
        			if(k <= 36)
        			{
        				newList.add(list.get(k));
        			}
        			else if(k % 3 == 0 && k <= 60)
        			{
        				newList.add(list.get(k));
        			} 
        			else if(k > 60)
        			{
        				newList.add(list.get(k));
        			}
        		}
        		rsMap.get("swc9km_org").put(el, newList);
        	}
        }

        return rsMap;
    }

    private String getElementNameByDataSource(String dataSource)
    {
        if("obs".equals(dataSource))
        {
            return "datetime";
        }
        else
        {
            return "validdate";
        }
    }

    private Map<String, List<StationForecastDataEntity>> readStationGribDatas(String station, String[] dataTypes, String dataTime)
    {
        Map<String, List<StationForecastDataEntity>> result = new HashMap<>();
        Map<String, String> dataTableConfigMap = ReadPropertiesUtil.getUserConfigMap("data_table.properties");

        List<OrgNafpDataParams> list = new ArrayList<>();
        OrgNafpDataParams params = null;
        for(String dataType : dataTypes)
        {
            result.put(dataType, new ArrayList<>());
            params = new OrgNafpDataParams();
            params.setDataTime(dataTime);
            params.setTableName(dataTableConfigMap.get(dataType));
            params.setDataType(dataType);
            list.add(params);
        }
//        OrgNafpDataParams params = new OrgNafpDataParams();
//        params.setDataTime(dataTime);
//        params.setDataType(dataTypes);
        List<OrgNafpDataEntity> orgNafpDataEntities = new ArrayList<>();
        for(OrgNafpDataParams param : list)
        {
            List<OrgNafpDataEntity> orgList = stationForecastDataMapper.queryOrgNafpDatas(param);
            orgNafpDataEntities.addAll(orgList);
        }
        Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("stations_info.properties");
        double lon = Double.parseDouble(configMap.get(station));
        double lat = Double.parseDouble(configMap.get(station));
        StationForecastDataEntity stationData = null;
        for(OrgNafpDataEntity data : orgNafpDataEntities)
        {
            stationData = readDataByStation(data.getDataType(), data.getFilePath(), lon, lat);
            result.get(data.getDataType()).add(stationData);
        }

        return result;
    }

    private StationForecastDataEntity readDataByStation(String dataType, String filePath, double lon, double lat)
    {
        StationForecastDataEntity result = null;
        Map<String, Object> datasMap = NcReader.getDatasMap(filePath);
        String[] lonLatName = NcReader.getLonLatName(datasMap);
        String prefix = "";
        if(dataType.equals(DataTypeEnum.ECMF.getDataType()))
        {
            prefix = "latlon_1441x2880-0p06s-180p00e:";
        }
        double[] lons = ReadLonLatUtil.readLonLat(datasMap, prefix + lonLatName[0]);
        double[] lats = ReadLonLatUtil.readLonLat(datasMap, prefix + lonLatName[1]);
        Map<String, String> elementsConfigMap = ReadPropertiesUtil.getUserConfigMap(dataType + "_elements_map.properties");
        result = readDataFromGribFile(datasMap, lons, lats, lon, lat, elementsConfigMap);

        return result;
    }

    @Override
    public Map<String, Map<String, Double>> compareGribData(CompareDataParams params) {
        Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("data_table.properties");
        ObsDataParams obsDataParams = new ObsDataParams();
        obsDataParams.setStation(params.getStation());
        obsDataParams.setDataTime(params.getDataTime());
        
        String dataTime = params.getDataTime();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(TimeUtil.String2Date(params.getDataTime(), "yyyy-MM-dd HH:mm:ss"));
        calendar.add(Calendar.HOUR_OF_DAY, -8);
        obsDataParams.setDataTime(TimeUtil.date2String(calendar.getTime(), "yyyy-MM-dd HH:mm:ss"));
        calendar.add(Calendar.HOUR_OF_DAY, 240);
        obsDataParams.setEndDateTime(TimeUtil.date2String(calendar.getTime(), "yyyy-MM-dd HH:mm:ss"));

        String[] dataSources = params.getDataSource();
        String[] dataSourceOrg = params.getDataSourceOrg();
        List<String> dataSourceList = new ArrayList<>();
        for(String dataSource : dataSources)
        {
            dataSourceList.add(dataSource);
        }
        for(String dataSource : dataSourceOrg)
        {
            dataSourceList.add(dataSource + "_org");
        }
        List<ObsDataEntity> obsDataEntities = obsDataMapper.queryObsData(obsDataParams);

        calendar.setTime(TimeUtil.String2Date(params.getDataTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
        Date startDate = calendar.getTime();
        calendar.add(Calendar.HOUR_OF_DAY, 240);
        Date endDate = calendar.getTime();
        params.setEndDateTime(TimeUtil.date2String(endDate, "yyyy-MM-dd HH:mm:ss"));
        List<String> dateList = new ArrayList<>();
        for(int i = 0;; i++)
        {
            calendar.setTime(startDate);
            calendar.add(Calendar.HOUR_OF_DAY, i);
            if(calendar.getTime().after(endDate))
            {
                break;
            }
            dateList.add(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
        }

        List<ObsDataEntity> obsDatasList = new ArrayList<>();
        Map<String, ObsDataEntity> mapObsDatas = new HashMap<>();
        for(ObsDataEntity obs : obsDataEntities)
        {
            calendar.setTime(TimeUtil.String2Date(obs.getDatetime(), "yyyy-MM-dd HH:mm:ss"));
            calendar.add(Calendar.HOUR_OF_DAY, 8);
            obs.setDatetime(TimeUtil.date2String(calendar.getTime(), "yyyy-MM-dd HH:mm:ss"));
            mapObsDatas.put(obs.getDatetime(), obs);
        }
        for(String dateStr : dateList)
        {
            if(!mapObsDatas.containsKey(dateStr))
            {
                ObsDataEntity data = new ObsDataEntity();
                data.setDatetime(dateStr);
                data.setRain24(DecodeConstants.UNDEF_DOUBLE_VALUE);
                obsDatasList.add(data);
            }
            else
            {
                obsDatasList.add(mapObsDatas.get(dateStr));
            }
        }


        Map<String, Map<String, double[]>> result = new HashMap<>();
        for(String dataSource : dataSourceList)
        {
            List<GribForecastRainValueEntity> gribForecastDataEntities = null;
            if(!dataSource.endsWith("_org"))
            {
                params.setTableName(configMap.get(dataSource + "_rain_value"));
                params.setDataTime(dataTime);
                gribForecastDataEntities = gribForecastDataMapper.queryGribRainValueCompare(params);
                for(GribForecastRainValueEntity data : gribForecastDataEntities)
                {
                	data.setRain(NumberFormatUtil.numFormat(data.getRain(), 1));
                }
            }
            else
            {
                params.setTableName(configMap.get(dataSource.split("_")[0] + "_value"));
                params.setDataTime(TimeUtil.addHours(dataTime, -12));
                params.setStartDateTime(dataTime);
                gribForecastDataEntities = gribForecastDataMapper.queryGribRainValueCompareOrg(params);
                int scale = 1;
                if(dataSource.startsWith("ecmf"))
                {
                	scale = 1000;
                }
                for(GribForecastRainValueEntity data : gribForecastDataEntities)
                {
                	data.setRain(NumberFormatUtil.numFormat(data.getRain() * scale, 1));
                }
            }
//            List<GribForecastDataEntity> gribForecastDataEntities = gribForecastDataMapper.queryGribRainCompare(params);
//            List<GribForecastRainValueEntity> gribForecastDataEntities = gribForecastDataMapper.queryGribRainValueCompare(params);
            Map<String, double[]> map = new LinkedHashMap<>();
            for(GribForecastRainValueEntity data : gribForecastDataEntities)
            {
                String validDate = data.getValidDate();
                ObsDataEntity obsData = mapObsDatas.get(validDate);
                if(obsData != null)
                {
                    map.put(validDate, new double[]{obsData.getRain24(), data.getRain()});
                }
                else
                {
                    map.put(validDate, new double[]{DecodeConstants.UNDEF_DOUBLE_VALUE, data.getRain()});
                }
            }
            result.put(dataSource, map);
        }

        Map<String, Map<String, Double>> resultData = new HashMap<>();
        resultData.put("obs", new LinkedHashMap<>());
        for(String dataSource : result.keySet())
        {
        	boolean obsDone = false;
            Map<String, double[]> map = result.get(dataSource);
            Map<String, Double> mapDouble = new LinkedHashMap<>();
            for(String time : map.keySet())
            {
                if(!obsDone)
                {
                	if(map.get(time)[0] == 9999)
                	{
                		resultData.get("obs").put(time, Double.valueOf(0));
                	}
                	else if(map.get(time)[0] == 999999)
                	{
                		resultData.get("obs").put(time, null);
                	}
                	else
                	{
                		resultData.get("obs").put(time, map.get(time)[0]);
                	}
                }
                mapDouble.put(time, map.get(time)[1]);
            }
            obsDone = true;
            resultData.put(dataSource, mapDouble);
        }
        
        if(resultData.get("ecmf_org") != null)
        {
        	Map<String, Double> m = new LinkedHashMap<>();
        	Map<String, Double> zmap = resultData.get("ecmf_org");
        	String endTimeStr = TimeUtil.addHours(dataTime, 240);
        	
        	InterpolationResult interpolate = BilinearInterpolateUtil.interpolate(zmap, dataTime, endTimeStr, 3, null);
        	double[] ds2 = interpolate.getValues();
        	for(int i = 0, count = ds2.length; i < count; i++)
        	{
        		if(i <= 24)
        		{
        			m.put(TimeUtil.addHours(dataTime, i * 3), ds2[i]);
        		}
        		else
        		{
        			if(i % 2 == 0)
        			{
        				m.put(TimeUtil.addHours(dataTime, i * 3), ds2[i]);
        			}
        		}
        	}
        	
        	resultData.put("ecmf_org", m);
        }
        if(resultData.get("grapes_org") != null)
        {
        	Map<String, Double> m = new LinkedHashMap<>();
        	Map<String, Double> zmap = resultData.get("grapes_org");
        	String endTimeStr = TimeUtil.addHours(dataTime, 240);
        	
        	InterpolationResult interpolate = BilinearInterpolateUtil.interpolate(zmap, dataTime, endTimeStr, 3, null);
        	double[] ds2 = interpolate.getValues();
        	for(int i = 0, count = ds2.length; i < count; i++)
        	{
        		if(i <= 24)
        		{
        			m.put(TimeUtil.addHours(dataTime, i * 3), ds2[i]);
        		}
        		else
        		{
        			if(i % 2 == 0)
        			{
        				m.put(TimeUtil.addHours(dataTime, i * 3), ds2[i]);
        			}
        		}
        	}
        	
        	resultData.put("grapes_org", m);
        }
        

        ConfigParams configParams = new ConfigParams();
        configParams.setDataType("cmpgrib");
        List<DataSourceType> orgList = configMapper.getDataSourceByDataType(configParams);
        configParams.setDataType("prc");
        List<DataSourceType> prcList = configMapper.getDataSourceByDataType(configParams);

        Map<String, Map<String, Double>> rs = new LinkedHashMap<>();
        rs.put("obs", resultData.get("obs"));
        
        for(DataSourceType dataSourceType : prcList)
        {
            if(dataSourceList.contains(dataSourceType.getDataSource()))
            {
            	Map<String, Double> map = resultData.get(dataSourceType.getDataSource());
            	if(map == null || map.size() == 0)
            	{
            		fileMapValue(map, dateList);
            	}
                rs.put(dataSourceType.getDataSource(), map);
            }
        }
        
        for(DataSourceType dataSourceType : orgList)
        {
            if(dataSourceList.contains(dataSourceType.getDataSource() + "_org"))
            {
            	Map<String, Double> map = resultData.get(dataSourceType.getDataSource() + "_org");
            	if(map == null || map.size() == 0)
            	{
            		fileMapValue(map, dateList);
            	}
                rs.put(dataSourceType.getDataSource() + "_org", map);
            }
        }

        String[] orders = new String[]{"obs", "ecmf_org", "ecmf", "grapes_org", "grapes", "deep"};
        Map<String, Map<String, Double>> resultMap = new LinkedHashMap<>();
        Map<String, Map<String, Double>> resultMapNew = new LinkedHashMap<>();
        for(String order : orders)
        {
        	if(rs.get(order) == null)
        	{
        		continue;
        	}
        	resultMap.put(order, rs.get(order));
        }
        
        Map<String, ObsDataEntity> obsMap = new LinkedHashMap<>();
        for(ObsDataEntity obs : obsDataEntities)
        {
        	obsMap.put(obs.getDatetime(), obs);
        }
        
        if(resultMap.get("obs") != null)
        {
        	List<String> dateListObs = new ArrayList<>();
        	for(String date : resultMap.get("obs").keySet())
        	{
        		dateListObs.add(date);
        	}
        	Map<String, Double> obsR = new LinkedHashMap<>();
        	Map<String, Double> map = resultMap.get("obs");
        	int num = 0;
        	for(String date : map.keySet())
        	{
        		if(num > 0)
        		{
        			Double double1 = map.get(date);
        			if(double1 == null)
        			{
        				continue;
        			}
        			else
        			{
        				if(num <= 37)
        				{
        					Double total = new Double(0);
        					for(int k = 1; k < 3; k++)
        					{
        						ObsDataEntity entity = obsMap.get(TimeUtil.addHours(date, -k));
        						if(entity == null || entity.getRain() == DecodeConstants.UNDEF_DOUBLE_VALUE)
        						{
        							total = null;
        							break;
        						}
        						else
        						{
        							total += entity.getRain() == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE ? 0 : entity.getRain();
        						}
        					}
        					obsR.put(date, total);
        				}
        				else
        				{
        					Double total = new Double(0);
        					for(int k = 1; k < 6; k++)
        					{
        						ObsDataEntity entity = obsMap.get(TimeUtil.addHours(date, -k));
        						if(entity == null || entity.getRain() == DecodeConstants.UNDEF_DOUBLE_VALUE)
        						{
        							total = null;
        							break;
        						}
        						else
        						{
        							total += entity.getRain() == DecodeConstants.MICAPS_UNDEF_DOUBLE_VALUE ? 0 : entity.getRain();
        						}
        					}
        					obsR.put(date, total);
        				}
        			}
        		}
        		else
        		{
        			obsR.put(date, map.get(date));
        		}
        		
        		num++;
        	}
        	for(String date : resultMap.get("obs").keySet())
        	{
        		if(!obsR.containsKey(date))
        		{
        			obsR.put(date, null);
        		}
        	}
        	resultMapNew.put("obs", obsR);
        }
        
        for(String dataSource : resultMap.keySet())
        {
        	if("obs".equals(dataSource) || resultMap.get(dataSource) == null)
        	{
        		continue;
        	}
        	resultMapNew.put(dataSource, new LinkedHashMap<>());
        	int i = 0;
        	List<String> dateListT = new ArrayList<>();
        	for(String date : resultMap.get(dataSource).keySet())
        	{
        		dateListT.add(date);
        	}
        	for(String date : resultMap.get(dataSource).keySet())
        	{
        		if(i > 0)
        		{
        			Double value1 = resultMap.get(dataSource).get(date);
        			Double value2 = resultMap.get(dataSource).get(dateListT.get(i - 1));
        			if(value1 != null && value2 != null)
        			{
        				resultMapNew.get(dataSource).put(date, NumberFormatUtil.numFormat(value1 - value2, 1));
        			}
        			else
        			{
        				resultMapNew.get(dataSource).put(date, null);
        			}
        		}
        		else
        		{
        			resultMapNew.get(dataSource).put(date, resultMap.get(dataSource).get(date));
        		}
        		
        		i++;
        	}
        }
        

        return resultMapNew;
    }
    
    private void fileMapValue(Map<String, Double> map, List<String> dateList)
    {
    	for(String date : dateList)
    	{
    		map.put(date, null);
    	}
    }

    private double getValue(String filePath, double lon, double lat)
    {
        Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
        double[][] values = null;
        if(filePath.endsWith("txt"))
        {
            values = GribUtil.readGribDatasFromTxt(filePath);
        }
        else
        {
            Map<String, Object> datasMap = GribFileReaderUtil.getDatasMap(filePath);
            String prefix = "";
            for(String key : datasMap.keySet())
            {
                if (key.contains(":")) {
                    String[] split = key.split(":");
                    prefix = split[0] + ":";
                }
            }
            String lonlatStr = configMap.get("lonlat");
            String[] split = lonlatStr.split(",");
            //startLon,startLat,endLon,endLat
            double[] lonlat = new double[]{Double.parseDouble(split[0]), Double.parseDouble(split[1]),
                    Double.parseDouble(split[2]), Double.parseDouble(split[3])};
            String dataType = DataTypeUtil.getDataType(filePath);
            String element = configMap.get(dataType);
            if(element.contains("$"))
            {
                int[] disVtiAndVti = GribUtil.getDisVtiAndVtiStation(filePath, dataType);
                element = element.replace("$", disVtiAndVti[1] + "");
            }
            double[][][] dataValues = NcReader.readByElemNameLayerSlice(datasMap, element, null);
            String[] lonLatName = NcReader.getLonLatName(datasMap);
            double[] lons = ReadLonLatUtil.readLonLat(datasMap, prefix + lonLatName[0]);
            double[] lats = ReadLonLatUtil.readLonLat(datasMap, prefix + lonLatName[1]);
            values = SliceArrayUtil.slice(dataValues[0], lons, lats, lonlat);
        }
        double fstValue = getFstValue(lon, lat, values);

        return fstValue;
    }

    private double getFstValue(double lon, double lat, double[][] values)
    {
        Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
        String lonlat = configMap.get("lonlat");
        String[] split = lonlat.split(",");
        double[] lonlats = new double[split.length];
        for(int i = 0, count = split.length; i < count; i++)
        {
            lonlats[i] = Double.parseDouble(split[i]);
        }
        double lon_dis = Double.parseDouble(configMap.get("lon_dis"));
        double lat_dis = Double.parseDouble(configMap.get("lat_dis"));
        double[] findFirstPoint = ReadGribRainUtil.findFindFirstPoint(lon, lat, lon_dis, lat_dis, lonlats);
        double topLeft = values[(int) findFirstPoint[0]][(int) findFirstPoint[0]];
        double topRight = values[(int) findFirstPoint[0] + 1][(int) findFirstPoint[0]];
        double bottomLeft = values[(int) findFirstPoint[0]][(int) findFirstPoint[0] + 1];
        double bottomRight = values[(int) findFirstPoint[0] + 1][(int) findFirstPoint[0] + 1];
        double value = ReadGribRainUtil.bilinearInterpolation(topLeft, topRight, bottomLeft, bottomRight, findFirstPoint[2], findFirstPoint[3]);

        return value;
    }

    private StationForecastDataEntity readDataFromGribFile(Map<String, Object> datasMap, double[] lons, double[] lats, double lon, double lat, Map<String, String> elementsMap)
    {
        StationForecastDataEntity result = new StationForecastDataEntity();
        int[] pointIndex = getPointIndex(lons, lats, lon, lat);
        for(String element : elementsMap.keySet())
        {
            double[][][] datas = NcReader.readByElemNameLayerSlice(datasMap, element, null);
            ReflectUtil.setFieldValueByName(result, datas[0][pointIndex[1]][pointIndex[0]], elementsMap.get(element));
        }

        return result;
    }

    private static int[] getPointIndex(double[] lon, double[] lat, double x, double y)
    {
        int xIndex = getPointIndex(lon, x);
        int yIndex = getPointIndex(lat, y);

        return new int[]{xIndex, yIndex};
    }

    private static int getPointIndex(double[] values, double value)
    {
        int result = 0;
        for(int i = 0, count = values.length; i < count - 1; i++)
        {
            if(value <= values[i + 1] && value > values[i])
            {
                result = Math.abs(value - values[i + 1]) <= Math.abs(value - values[i]) ? (i + 1) : i;
                break;
            }
        }

        return result;
    }
}
