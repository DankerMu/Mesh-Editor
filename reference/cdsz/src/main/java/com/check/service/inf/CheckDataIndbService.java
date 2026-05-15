package com.check.service.inf;

import com.check.pojo.CheckDataGribIndbEntity;
import com.check.pojo.CheckDataIndbEntity;
import com.check.pojo.CheckDataParams;

import java.util.List;
import java.util.Map;

/**
 * @category
 * @date 2025/3/19 10:50
 * @description TODO
 */
public interface CheckDataIndbService {
    Map<String, Map<String, Map<String, Map<String, Double>>>> checkStationDataHour(CheckDataParams params);
    Map<String, Map<String, Map<String, CheckDataIndbEntity>>> checkStationDataDay(CheckDataParams params);
    Map<String, Map<String, Map<String, CheckDataIndbEntity>>> checkStationDataDayRain(CheckDataParams params);
    Map<String, Map<String, Map<String, Double>>> checkGribDataHour(CheckDataParams params);
    Map<String, Map<String, Map<String, CheckDataGribIndbEntity>>> checkGribDataDay(CheckDataParams params);
    
    int addCheckValue(CheckDataIndbEntity data);
    int updateCheckValue(CheckDataIndbEntity data);
    int updateStationCheckRainValue(CheckDataIndbEntity data);
    List<CheckDataIndbEntity> queryCheckValue(CheckDataIndbEntity data);
    
    int addCheckGribValue(CheckDataGribIndbEntity data);
    int updateCheckGribValue(CheckDataGribIndbEntity data);
    List<CheckDataGribIndbEntity> queryGribCheckValue(CheckDataGribIndbEntity data);
    
    List<CheckDataIndbEntity> dealStationMonthCheck(CheckDataParams params);
    List<CheckDataIndbEntity> dealGribMonthCheck(CheckDataParams params);
}
