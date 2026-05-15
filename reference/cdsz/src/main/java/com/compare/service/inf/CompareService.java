package com.compare.service.inf;

import com.compare.pojo.CompareDataParams;

import java.util.List;
import java.util.Map;

public interface CompareService {
    Map<String, Map<String, List<Map<String, String>>>> compareStationData(CompareDataParams params);
    Map<String, Map<String, Double>> compareGribData(CompareDataParams params);
}
