//package com.check.service;
//
//import com.check.pojo.CheckDataParams;
//import com.check.service.inf.CheckDataService;
//import com.forecast.dao.GribForecastDataMapper;
//import com.forecast.dao.StationForecastDataMapper;
//import com.forecast.pojo.GribForecastDataEntity;
//import com.forecast.pojo.StationForecastDataEntity;
//import com.obs.dao.ObsDataMapper;
//import com.obs.pojo.ObsDataEntity;
//import com.station.pojo.StationEntity;
//import com.util.*;
//import org.springframework.stereotype.Service;
//
//import javax.annotation.Resource;
//import java.util.*;
//
///**
// * @category
// * @date 2025/3/19 10:52
// * @description TODO
// */
//@Service
//public class CheckDataServiceImpl_0520 implements CheckDataService {
//
//    @Resource
//    private ObsDataMapper obsDataMapper;
//    @Resource
//    private StationForecastDataMapper stationForecastDataMapper;
//    @Resource
//    private GribForecastDataMapper gribForecastDataMapper;
//    private String totalStr = "合计";
////    private static Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
//    @Override
//    public Map<String, Map<String, Map<String, Map<String, Double>>>> checkStationData(CheckDataParams param) {
//        String[] vtis = param.getVtis();
//        param.setStartTime(param.getStartTime());
//        param.setEndTime(param.getEndTime());
//        if(vtis.length == 2)
//        {
//            param.setStartValidDate(param.getStartTime());
//            Calendar calendar = Calendar.getInstance();
//            calendar.setTime(TimeUtil.dateTimeStr2date(param.getStartTime(), TimeUtil.DEFAULT_DATE_FORMAT, TimeUtil.DEFAULT_DATETIME_FORMAT));
//            calendar.add(Calendar.HOUR_OF_DAY, 240);
//            param.setEndValidDate(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATE_FORMAT));
//        }
//        else if(vtis[0].equals("72"))
//        {
//            param.setStartValidDate(param.getStartTime());
//            Calendar calendar = Calendar.getInstance();
//            calendar.setTime(TimeUtil.dateTimeStr2date(param.getStartTime(), TimeUtil.DEFAULT_DATE_FORMAT, TimeUtil.DEFAULT_DATETIME_FORMAT));
//            calendar.add(Calendar.HOUR_OF_DAY, 72);
//            param.setEndValidDate(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
//        }
//        else if(vtis[0].equals("240"))
//        {
//            Calendar calendar = Calendar.getInstance();
//            calendar.setTime(TimeUtil.dateTimeStr2date(param.getStartTime(), TimeUtil.DEFAULT_DATE_FORMAT, TimeUtil.DEFAULT_DATETIME_FORMAT));
//            calendar.add(Calendar.HOUR_OF_DAY, 72);
//            param.setStartValidDate(param.getStartTime());
//            calendar.add(Calendar.HOUR_OF_DAY, 168);
//            param.setEndValidDate(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
//        }
//
//        if(param.getStations() == null || param.getStations().length == 0)
//        {
//            if(param.getProvinceId() != null && param.getProvinceId().length == 0)
//            {
//                param.setProvinceId(null);
//            }
//            if(param.getCityId() != null && param.getCityId().length == 0)
//            {
//                param.setCityId(null);
//            }
//            if(param.getCntyId() != null && param.getCntyId().length == 0)
//            {
//                param.setCntyId(null);
//            }
//            List<StationEntity> stationEntities = stationForecastDataMapper.queryForecastStations(param);
//            int count = stationEntities.size();
//            String[] stations = new String[count];
//            for(int i = 0; i < count; i++)
//            {
//                stations[i] = stationEntities.get(i).getStation();
//            }
//            param.setStations(stations);
//        }
//
//
//        List<ObsDataEntity> obsList = obsDataMapper.queryObsCheckData(param);
//        List<StationForecastDataEntity> stationForecastList = stationForecastDataMapper.queryStationForecastCheck(param);
//        Set<String> dataTimeSet = new HashSet<>();
//        for(StationForecastDataEntity entity : stationForecastList)
//        {
//            dataTimeSet.add(entity.getDatatime().toString());
//        }
//        String[] elements = param.getElements();
//        String[] stations = param.getStations();
//        //station element
//        Map<String, Map<String, List<ObsDataEntity>>> datasObsMap = new HashMap<>();
//        Map<String, Map<String, List<StationForecastDataEntity>>> datasfstMap = new HashMap<>();
//        for(String station : stations)
//        {
//            if(!datasObsMap.containsKey(station))
//            {
//                datasObsMap.put(station, new HashMap<>());
//            }
//            if(!datasfstMap.containsKey(station))
//            {
//                datasfstMap.put(station, new HashMap<>());
//            }
//            for(String element : elements) {
//                if (!datasObsMap.get(station).containsKey(element)) {
//                    datasObsMap.get(station).put(element, new ArrayList<>());
//                }
//                if (!datasfstMap.get(station).containsKey(element)) {
//                    datasfstMap.get(station).put(element, new ArrayList<>());
//                }
//                for(StationForecastDataEntity data : stationForecastList)
//                {
//                    if(data.getStation().equals(station))
//                    {
//                        for(ObsDataEntity obs : obsList)
//                        {
//                            if(obs.getStation().equals(data.getStation()) && obs.getDatetime().equals(data.getValiddate()))
//                            {
//                                datasfstMap.get(station).get(element).add(data);
//                                datasObsMap.get(station).get(element).add(obs);
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        Calendar calendar = Calendar.getInstance();
//        String[] methods = param.getMethods();
////      station element vti method
//        Map<String, Map<String, Map<String, Map<String, Double>>>> result = new HashMap<>();
////        Set<String> set = datasObsMap.keySet();
////        Set<String> newSet = new HashSet<>();
////        for(String key : set)
////        {
////            newSet.add(key);
////        }
////        newSet.add(totalStr);
//        result.put(totalStr, new HashMap<>());
//        for(String stationKey : datasObsMap.keySet())
//        {
//            if(!result.containsKey(stationKey))
//            {
//                result.put(stationKey, new HashMap<>());
//            }
//            for(String elementKey : datasObsMap.get(stationKey).keySet())
//            {
//                if(!result.get(stationKey).containsKey(elementKey))
//                {
//                    result.get(stationKey).put(elementKey, new HashMap<>());
//                }
//                if(!result.get(totalStr).containsKey(elementKey))
//                {
//                    result.get(totalStr).put(elementKey, new HashMap<>());
//                }
//
//                List<ObsDataEntity> obsDataList = datasObsMap.get(stationKey).get(elementKey);
////                List<StationForecastDataEntity> fstDataList = datasfstMap.get(stationKey).get(elementKey);
//                for(String vti : vtis)
//                {
//                    if(!result.get(stationKey).get(elementKey).containsKey(vti))
//                    {
//                        result.get(stationKey).get(elementKey).put(vti, new HashMap<>());
//                    }
//                    if(!result.get(totalStr).get(elementKey).containsKey(vti))
//                    {
//                        result.get(totalStr).get(elementKey).put(vti, new HashMap<>());
//                    }
//                    Map<String, Double> valueMap = new HashMap<>();
//                    for(String dataTime : dataTimeSet)
//                    {
//                        List<StationForecastDataEntity> fstDataList = datasfstMap.get(stationKey).get(elementKey);
//                        calendar.setTime(TimeUtil.String2Date(dataTime, TimeUtil.DEFAULT_DATETIME_FORMAT));
//                        String validDateStart = dataTime;
//                        String validDateEnd = dataTime;
//                        if(vti.equals("72"))
//                        {
//                            calendar.add(Calendar.HOUR_OF_DAY, 72);
//                            validDateEnd = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT);
//                        }
//                        else if(vti.equals("240"))
//                        {
//                            calendar.add(Calendar.HOUR_OF_DAY, 73);
//                            validDateStart = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT);
//                            calendar.add(Calendar.HOUR_OF_DAY, 168);
//                            validDateEnd = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT);
//                        }
//                        List<StationForecastDataEntity> fstDataListSub = new ArrayList<>();
//                        for(StationForecastDataEntity fstData : fstDataList)
//                        {
//                            if(fstData.getValiddate().compareTo(validDateStart) >= 0 && fstData.getValiddate().compareTo(validDateEnd) <= 0)
//                            {
//                                fstDataListSub.add(fstData);
//                            }
//                        }
//                        for(String method : methods)
//                        {
//                            double value = getValue(elementKey.replace("_", ""), vti, method, obsDataList, fstDataListSub);
//                            String key = stationKey + "_" + elementKey + "_" + vti + "_" + method;
////                            String keyTotal = "total_" + elementKey + "_" + vti + "_" + method;
//                            if(!valueMap.containsKey(key))
//                            {
//                                valueMap.put(key, value);
//                            }
//                            else
//                            {
//                                valueMap.put(key, valueMap.get(key));
//                            }
////                            if(!valueMap.containsKey(keyTotal))
////                            {
////                                valueMap.put(keyTotal, value);
////                            }
////                            else
////                            {
////                                valueMap.put(keyTotal, valueMap.get(keyTotal));
////                            }
//
////                            result.get(stationKey).get(elementKey).get(vti).put(method, value);
//                        }
//                    }
//                    int size = dataTimeSet.size();
//                    for(String key : valueMap.keySet())
//                    {
//                        String[] split = key.split("_");
//                        result.get(split[0]).get(split[1]).get(split[2]).put(split[3], valueMap.get(key) / size);
//                        Double total = result.get(totalStr).get(split[1]).get(split[2]).get(split[3]);
//                        if(total == null)
//                        {
//                            total = 0.0;
//                        }
//                        result.get(totalStr).get(split[1]).get(split[2]).put(split[3], (valueMap.get(key) / size + total) / datasObsMap.size());
//                    }
//                }
//                result.get(stationKey).put(elementKey, result.get(stationKey).get(elementKey));
//            }
//            result.put(stationKey, result.get(stationKey));
//        }
//
//
//
//        return result;
//    }
//
//    @Override
//    public Map<String, Map<String, double[]>> checkGribData(CheckDataParams param) {
//        String[] vtis = param.getVtis();
//        param.setStartTime(param.getStartTime());
//        param.setEndTime(param.getEndTime());
//        Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("data_table.properties");
//        if(vtis.length == 2)
//        {
//            param.setStartValidDate(param.getStartTime());
//            Calendar calendar = Calendar.getInstance();
//            calendar.setTime(TimeUtil.dateTimeStr2date(param.getEndTime(), TimeUtil.DEFAULT_DATE_FORMAT, TimeUtil.DEFAULT_DATETIME_FORMAT));
//            calendar.add(Calendar.HOUR_OF_DAY, 240);
//            param.setEndValidDate(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATE_FORMAT));
//        }
//        else if(vtis[0].equals("72"))
//        {
//            param.setStartValidDate(param.getStartTime());
//            Calendar calendar = Calendar.getInstance();
//            calendar.setTime(TimeUtil.dateTimeStr2date(param.getEndTime(), TimeUtil.DEFAULT_DATE_FORMAT, TimeUtil.DEFAULT_DATETIME_FORMAT));
//            calendar.add(Calendar.HOUR_OF_DAY, 72);
//            param.setEndValidDate(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
//        }
//        else if(vtis[0].equals("240"))
//        {
//            Calendar calendar = Calendar.getInstance();
//            calendar.setTime(TimeUtil.dateTimeStr2date(param.getEndTime(), TimeUtil.DEFAULT_DATE_FORMAT, TimeUtil.DEFAULT_DATETIME_FORMAT));
////            calendar.add(Calendar.HOUR_OF_DAY, 72);
//            param.setStartValidDate(param.getStartTime());
//            calendar.add(Calendar.HOUR_OF_DAY, 240);
//            param.setEndValidDate(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
//        }
//        String tableName = configMap.get(param.getDataSource() + "_rain");
//        param.setTableName(tableName);
//
//        if(param.getStations() == null || param.getStations().length == 0)
//        {
//            List<StationEntity> stationEntities = stationForecastDataMapper.queryForecastStations(param);
//            int count = stationEntities.size();
//            String[] stations = new String[count];
//            for(int i = 0; i < count; i++)
//            {
//                stations[i] = stationEntities.get(i).getStation();
//            }
//            param.setStations(stations);
//        }
//
//
//        List<ObsDataEntity> obsList = obsDataMapper.queryObsCheckData(param);
//        List<GribForecastDataEntity> gribForecastList = gribForecastDataMapper.queryGribForecastCheck(param);
//        String[] stations = param.getStations();
//        Map<String, List<GribForecastDataEntity>> datasfstMap = new HashMap<>();
//        for(GribForecastDataEntity data : gribForecastList)
//        {
//            if(!datasfstMap.containsKey(data.getDataTime()))
//            {
//                datasfstMap.put(data.getDataTime(), new ArrayList<>());
//            }
//            if(data.getVti() % 24 == 0 && data.getVti() != 0)
//            {
//                datasfstMap.get(data.getDataTime()).add(data);
//            }
//        }
//        Calendar calendar = Calendar.getInstance();
////        station vti
//        Map<String, Map<String, double[]>> result = new HashMap<>();
//        result.put(totalStr, new HashMap<>());
//        List<Map<String, Map<String, double[]>>> list = new ArrayList<>();
//        for(String vti : vtis)
//        {
//            for(String dataTime : datasfstMap.keySet())
//            {
//                List<GribForecastDataEntity> fstDataList = datasfstMap.get(dataTime);
//                calendar.setTime(TimeUtil.String2Date(dataTime, TimeUtil.DEFAULT_DATETIME_FORMAT));
//                String validDateStart = dataTime;
//                String validDateEnd = dataTime;
//                int startVti = 0;
//                int endVti = 0;
//                if(vti.equals("72"))
//                {
////                    calendar.add(Calendar.HOUR_OF_DAY, 72);
////                    validDateEnd = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT);
//                    endVti = 72;
//                }
//                else if(vti.equals("240"))
//                {
////                    calendar.add(Calendar.HOUR_OF_DAY, 73);
////                    validDateStart = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT);
////                    calendar.add(Calendar.HOUR_OF_DAY, 168);
////                    validDateEnd = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT);
//                    startVti = 73;
//                    endVti = 240;
//                }
//                List<GribForecastDataEntity> fstDataListSub = new ArrayList<>();
//                Map<String, GribForecastDataEntity> fstDataMapSub = new LinkedHashMap<>();
//                for(GribForecastDataEntity fstData : fstDataList)
//                {
////                    if(fstData.getValidDate().compareTo(validDateStart) >= 0 && fstData.getValidDate().compareTo(validDateEnd) <= 0)
////                    {
////                        fstDataListSub.add(fstData);
////                    }
//                    if(fstData.getVti() > startVti && fstData.getVti() <= endVti)
//                    {
////                        fstDataListSub.add(fstData);
//                        fstDataMapSub.put(fstData.getDataTime() + "_" + fstData.getValidDate() + "_" + fstData.getVti(), fstData);
//                    }
//                }
//                for(String key : fstDataMapSub.keySet())
//                {
//                    fstDataListSub.add(fstDataMapSub.get(key));
//                }
//                Map<String, Map<String, double[]>> gribValueMap = getGribValue(stations, vti, obsList, fstDataListSub);
//                list.add(gribValueMap);
//            }
//
//            Map<String, Map<String, Integer>> countMap = new HashMap<>();
//            for(Map<String, Map<String, double[]>> gribValueMap : list)
//            {
//                for(String key : gribValueMap.keySet())
//                {
////                    int invalidCount = 0;
//                    if(!result.containsKey(key))
//                    {
//                        result.put(key, new HashMap<>());
//                        countMap.put(key, new HashMap<>());
//                    }
//                    double[] doubles = gribValueMap.get(key).get(vti);
//                    if(doubles == null)
//                    {
////                        invalidCount++;
//                        countMap.get(key).put(vti, countMap.get(key).get(vti) == null ? 1 : countMap.get(key).get(vti) + 1);
//                        continue;
//                    }
//                    if(!result.get(key).containsKey(vti))
//                    {
//                        result.get(key).put(vti, new double[doubles.length]);
////                        countMap.get(key).put(vti, doubles);
//                    }
//                    double[] doubles1 = result.get(key).get(vti);
//                    double[] doubles2 = new double[doubles1.length];
//                    for(int i = 0; i < doubles1.length; i++)
//                    {
//                        doubles2[i] = doubles1[i] + doubles[i];
//                    }
//                    result.get(key).put(vti, doubles2);
//                }
//            }
//            int stationCount = datasfstMap.size();
//            for(String key : result.keySet())
//            {
//                if(totalStr.equals(key))
//                {
//                    continue;
//                }
//                for(String kk : result.get(key).keySet())
//                {
//                    Integer i = countMap.get(key).get(kk);
//                    double[] doubles1 = result.get(totalStr).get(kk);
//                    if(doubles1 == null)
//                    {
//                        result.get(totalStr).put(kk, new double[3]);
//                        doubles1 = result.get(totalStr).get(kk);
//                    }
//                    double[] doubles = result.get(key).get(kk);
//                    if(i == null)
//                    {
//                        i = 0;
//                    }
//                    doubles[0] = doubles[0] / (stationCount - i);
//                    doubles[1] = doubles[1] / (stationCount - i);
//                    doubles[2] = doubles[2] / (stationCount - i);
//                    result.get(key).put(kk, doubles);
//                    doubles1[0] += doubles[0];
//                    doubles1[1] += doubles[1];
//                    doubles1[2] += doubles[2];
//                }
//            }
//        }
//
//
//        return result;
//
//    }
//
//    private double getValue(String element, String vti, String method, List<ObsDataEntity> obsDataList, List<StationForecastDataEntity> fstDataList)
//    {
//        Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
//        List<ObsDataEntity> obsDataListSub = new ArrayList<>();
//        Map<String, List<Object>> obsMapList = new HashMap<>();
//        for(StationForecastDataEntity fstData : fstDataList)
//        {
//            for(ObsDataEntity obsData : obsDataList)
//            {
//                if(fstData.getValiddate().compareTo(obsData.getDatetime()) == 0)
//                {
//                    obsDataListSub.add(obsData);
//                    ArrayList<Object> objects = new ArrayList<>();
//                    objects.add(obsData);
//                    objects.add(fstData);
//                    obsMapList.put(fstData.getValiddate(), objects);
//                }
//            }
//        }
//        int count = obsDataList.size() == 0 ? 1 : obsDataList.size();
//        double sum = 0;
//        if("RMSE".equals(method))
//        {
//            for(String key : obsMapList.keySet())
//            {
//                List<Object> objects = obsMapList.get(key);
//                double obs = getDoubleValueFromObject(objects.get(0), element);
//                double fst = getDoubleValueFromObject(objects.get(1), element);
//                sum += Math.pow(obs - fst, 2);
//            }
////            for(int i = 0; i < count; i++)
////            {
////                double obs = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(obsDataList.get(i), element)));
////                double fst = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(fstDataList.get(i), element)));
////                sum += Math.pow(obs - fst, 2);
////            }
//            sum = Math.sqrt(sum / count);
//        }
//        else if("MAE".equals(method))
//        {
//            for(String key : obsMapList.keySet())
//            {
//                List<Object> objects = obsMapList.get(key);
//                double obs = getDoubleValueFromObject(objects.get(0), element);
//                double fst = getDoubleValueFromObject(objects.get(1), element);
//                sum += Math.abs(obs - fst);
//            }
////            for(int i = 0; i < count; i++)
////            {
////                double obs = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(obsDataList.get(i), element)));
////                double fst = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(fstDataList.get(i), element)));
////                sum += Math.abs(obs - fst);
////            }
//            sum = sum / count;
//        }
//        else if("CORR".equals(method))
//        {
//            double obsSum = 0;
//            double fstSum = 0;
//            for(String key : obsMapList.keySet())
//            {
//                List<Object> objects = obsMapList.get(key);
//                double obs = getDoubleValueFromObject(objects.get(0), element);
//                double fst = getDoubleValueFromObject(objects.get(1), element);
//                obsSum += obs;
//                fstSum += fst;
//            }
////            for(int i = 0; i < count; i++)
////            {
////                double obs = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(obsDataList.get(i), element)));
////                double fst = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(fstDataList.get(i), element)));
////                obsSum += obs;
////                fstSum += fst;
////            }
//            double obsAvg = obsSum / count;
//            double fstAvg = fstSum / count;
//
//            double upSum = 0;
//            double downSum = 0;
//            for(String key : obsMapList.keySet())
//            {
//                List<Object> objects = obsMapList.get(key);
//                double obs = getDoubleValueFromObject(objects.get(0), element);
//                double fst = getDoubleValueFromObject(objects.get(1), element);
//                upSum += (obs - obsAvg) * (fst - fstAvg);
//                downSum += Math.sqrt(Math.pow(obs - obsAvg, 2)) * Math.sqrt(Math.pow(fst - fstAvg, 2));
//            }
////            for(int i = 0; i < count; i++)
////            {
////                double obs = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(obsDataList.get(i), element)));
////                double fst = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(fstDataList.get(i), element)));
////                upSum += (obs - obsAvg) * (fst - fstAvg);
////                downSum += Math.sqrt(Math.pow(obs - obsAvg, 2)) * Math.sqrt(Math.pow(fst - fstAvg, 2));
////            }
//            sum = upSum / downSum;
//        }
//        else if("准确率".equals(method))
//        {
//            double dis = Double.parseDouble(configMap.get(element));
//            int correct = 0;
//            for(String key : obsMapList.keySet())
//            {
//                List<Object> objects = obsMapList.get(key);
//                double obs = getDoubleValueFromObject(objects.get(0), element);
//                double fst = getDoubleValueFromObject(objects.get(1), element);
//                if(Math.abs(obs - fst) < dis)
//                {
//                    correct++;
//                }
//            }
////            for(int i = 0; i < count; i++)
////            {
////                double obs = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(obsDataList.get(i), element)));
////                double fst = Double.parseDouble(String.valueOf(ReflectUtil.getFieldValueByName(fstDataList.get(i), element)));
////                if(Math.abs(obs - fst) < dis)
////                {
////                    correct++;
////                }
////            }
//            sum = correct / count;
//        }
//        sum = NumberFormatUtil.numFormat(sum, 3);
//
//        return sum;
//    }
//
//    private Map<String, Map<String, double[]>> getGribValue(String[] stations, String vti, List<ObsDataEntity> obsDataList, List<GribForecastDataEntity> fstDataList)
//    {
//        Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
//        List<ObsDataEntity> obsDataListSub = new ArrayList<>();
//        Map<String, Map<String, double[]>> result = new HashMap<>();
//        if(fstDataList.size() == 0)
//        {
//            return result;
//        }
//        for(GribForecastDataEntity fstData : fstDataList)
//        {
//            for(ObsDataEntity obsData : obsDataList)
//            {
//                Date date = TimeUtil.String2Date(obsData.getDatetime(), "yyyy-MM-dd HH:mm:ss");
//                Calendar calendar = Calendar.getInstance();
//                calendar.setTime(date);
//                calendar.add(Calendar.HOUR_OF_DAY, 8);
//                if(TimeUtil.date2String(calendar.getTime(), "yyyy-MM-dd HH:mm:ss").compareTo(fstData.getValidDate()) == 0)
//                {
//                    obsDataListSub.add(obsData);
//                }
//            }
//        }
//        int count = fstDataList.size();
//        for(String station : stations)
//        {
//            double h = 0;
//            double tn = 0;
//            double f = 0;
//            double m = 0;
//            double hSum = 0;
//            double fSum = 0;
//            double mSum = 0;
//            double th = 0;
//            double tm = 0;
//            double tf = 0;
//            int realCount = 0;
//            int nilCount = 0;
//            Map<String, Double> fsMap = new HashMap<>();
//            fsMap.put("0", Double.parseDouble("0"));
//            for(int i = 0; i < count; i++)
//            {
//                String filePath = fstDataList.get(i).getSourcePath();
//                double[][] gribDatas = null;
//                if(filePath.endsWith(".txt"))
//                {
//                    gribDatas = GribUtil.readGribDatasFromTxt(filePath, ",");
//                }
//                else
//                {
//                    Map<String, Object> datasMap = GribFileReaderUtil.getDatasMap(filePath);
//                    String prefix = "";
//                    for(String key : datasMap.keySet())
//                    {
//                        if (key.contains(":")) {
//                            String[] split = key.split(":");
//                            prefix = split[0] + ":";
//                        }
//                    }
//                    String lonlatStr = configMap.get("lonlat");
//                    String[] split = lonlatStr.split(",");
//                    //startLon,startLat,endLon,endLat
//                    double[] lonlat = new double[]{Double.parseDouble(split[0]), Double.parseDouble(split[1]),
//                                                   Double.parseDouble(split[2]), Double.parseDouble(split[3])};
//                    String dataType = DataTypeUtil.getDataType(filePath);
//                     String element = configMap.get(dataType);
//                    if(element.contains("$"))
//                    {
//                        int[] disVtiAndVti = GribUtil.getDisVtiAndVtiStation(filePath, dataType);
//                        element = element.replace("$", disVtiAndVti[1] + "");
//                    }
//                    double[][][] dataValues = NcReader.readByElemNameLayerSlice(datasMap, element, null);
//                    String[] lonLatName = NcReader.getLonLatName(datasMap);
//                    double[] lons = ReadLonLatUtil.readLonLat(datasMap, prefix + lonLatName[0]);
//                    double[] lats = ReadLonLatUtil.readLonLat(datasMap, prefix + lonLatName[1]);
//                    gribDatas = SliceArrayUtil.slice(dataValues[0], lons, lats, lonlat);
//                }
//                double lon = 0;
//                double lat = 0;
//                double value = 999999;
//
//                for(ObsDataEntity obsData : obsDataListSub)
//                {
//                    Date date = TimeUtil.String2Date(obsData.getDatetime(), "yyyy-MM-dd HH:mm:ss");
//                    Calendar calendar = Calendar.getInstance();
//                    calendar.setTime(date);
//                    calendar.add(Calendar.HOUR_OF_DAY, 8);
//                    if(station.equals(obsData.getStation()) && fstDataList.get(i).getValidDate().compareTo(TimeUtil.date2String(calendar.getTime(), "yyyy-MM-dd HH:mm:ss")) == 0)
//                    {
//                        lon = obsData.getLon();
//                        lat = obsData.getLat();
//                        value = obsData.getRain24();
//                        break;
//                    }
//                }
//                double fstValue = getFstValue(lon, lat, gribDatas);
//                fsMap.put(fstDataList.get(i).getVti() + "", fstValue);
//                fstValue = fstValue - fsMap.get((fstDataList.get(i).getVti() - 24) + "");
//
//                System.out.println("vti:" + fstDataList.get(i).getVti() + "fst:" + fstValue + " obs:" + value);
//
//                if(value == 999999)
//                {
////                    value = 0;
//                    nilCount++;
//                    continue;
//                }
//                if(value >= 0.1 && fstValue >= 0.1)
//                {
//                    h++;
//                }
//                else if(value < 0.1 && fstValue < 0.1)
//                {
//                    tn++;
//                }
//                else if(value >= 0.1 && fstValue < 0.1)
//                {
//                    m++;
//                }
//                else if(value < 0.1 && fstValue >= 0.1)
//                {
//                    f++;
//                }
//            }
//
//            if(count == nilCount)
//            {
//                if(!result.containsKey(station))
//                {
//                    result.put(station, new HashMap<>());
//                }
//                result.get(station).put(vti, null);
//            }
//            else
//            {
//                th = (h + tn) / (h + f + m + tn);
//                tf = f / ((f + h) == 0 ? 1 : (f + h));
//                tm = m / ((m + tn) == 0 ? 1 : (tn + m));
//
//                System.out.println("station:" + station + " h: " + h + " f: " + f + " m: " + m + " tn: " + tn);
//
//                hSum += th;
//                fSum += tf;
//                mSum += tm;
//                if(!result.containsKey(station))
//                {
//                    result.put(station, new HashMap<>());
//                }
////                realCount = count - nilCount;
//                result.get(station).put(vti, new double[]{hSum, fSum, mSum});
//            }
//
//        }
//
//        return result;
//    }
//
//    private double getFstValue(double lon, double lat, double[][] values)
//    {
//        Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
//        String lonlat = configMap.get("lonlat");
//        String[] split = lonlat.split(",");
//        double[] lonlats = new double[split.length];
//        for(int i = 0, count = lonlats.length; i < count; i++)
//        {
//            lonlats[i] = Double.parseDouble(split[i]);
//        }
//        double lon_dis = Double.parseDouble(configMap.get("lon_dis"));
//        double lat_dis = Double.parseDouble(configMap.get("lat_dis"));
//        double[] findFirstPoint = ReadGribRainUtil.findFindFirstPoint(lon, lat, lon_dis, lat_dis, lonlats);
////        System.out.println("lonCount:" + values[0].length + ", latCount" + values.length);
////        System.out.println("lon:" + lon + ", lat:" + lat);
////        findFirstPoint: 670.0, 405.0
////        System.out.println("findFirstPoint: " + findFirstPoint[0] + ", " + findFirstPoint[1]);
//        double topLeft = values[(int) findFirstPoint[1]][(int) findFirstPoint[0]];
//        double topRight = values[(int) findFirstPoint[1] + 1][(int) findFirstPoint[0]];
//        double bottomLeft = values[(int) findFirstPoint[1]][(int) findFirstPoint[0] + 1];
//        double bottomRight = values[(int) findFirstPoint[1] + 1][(int) findFirstPoint[0] + 1];
//        double value = ReadGribRainUtil.bilinearInterpolation(topLeft, topRight, bottomLeft, bottomRight, findFirstPoint[2], findFirstPoint[3]);
//
//        return value;
//    }
//
//    private double getDoubleValueFromObject(Object object, String element)
//    {
//        double result = 999999;
//        Object value = ReflectUtil.getFieldValueByName(object, element);
//        if(value != null)
//        {
//            result = Double.parseDouble(value.toString());
//        }
//        else
//        {
//            result = 999999;
//        }
//
//        return result;
//    }
//
//}
