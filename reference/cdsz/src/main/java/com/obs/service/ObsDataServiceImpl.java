package com.obs.service;

import com.obs.pojo.ObsDataEntity;
import com.obs.pojo.ObsDataParams;
import com.obs.service.inf.ObsDataService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * @category
 * @date 2025/3/18 10:10
 * @description TODO
 */
@Service
public class ObsDataServiceImpl implements ObsDataService {
    @Override
    public List<ObsDataEntity> queryObsData(ObsDataParams params) {
        return Collections.emptyList();
    }
}
