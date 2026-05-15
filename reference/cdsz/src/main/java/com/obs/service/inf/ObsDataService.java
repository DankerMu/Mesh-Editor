package com.obs.service.inf;

import com.obs.pojo.ObsDataEntity;
import com.obs.pojo.ObsDataParams;

import java.util.List;

public interface ObsDataService {
    List<ObsDataEntity> queryObsData(ObsDataParams params);
}
