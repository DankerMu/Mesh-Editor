package com.check.service.inf;

import com.check.pojo.CheckDataParams;

import java.util.Map;

/**
 * @category
 * @date 2025/3/19 10:50
 * @description TODO
 */
public interface CheckDataService {
    Map<String, Map<String, Map<String, Map<String, Double>>>> checkStationDataHour(CheckDataParams params);
    Map<String, Map<String, Map<String, Map<String, Double>>>> checkStationDataDay(CheckDataParams params);
    Map<String, Map<String, Map<String, Double>>> checkGribDataHour(CheckDataParams params);
    Map<String, Map<String, Map<String, Double>>> checkGribDataDay(CheckDataParams params);
    
    Map<String, Map<String, Map<String, Map<String, Double>>>> checkStationDataHourNew(CheckDataParams params);
    Map<String, Map<String, Map<String, Map<String, Double>>>> checkStationDataHourJdbc(CheckDataParams params);
    
    Map<String, Map<String, Map<String, Map<String, Double>>>> checkStationDataDayNew(CheckDataParams params);
    Map<String, Map<String, Map<String, Map<String, Double>>>> checkStationDataDayJdbc(CheckDataParams params);
    
    
    Map<String, Map<String, Map<String, Double>>> checkGribDataHourNew(CheckDataParams params);
    Map<String, Map<String, Map<String, Double>>> checkGribDataHourJdbc(CheckDataParams params);
    
    Map<String, Map<String, Map<String, Double>>> checkGribDataDayNew(CheckDataParams params);
    Map<String, Map<String, Map<String, Double>>> checkGribDataDayJdbc(CheckDataParams params);
}
