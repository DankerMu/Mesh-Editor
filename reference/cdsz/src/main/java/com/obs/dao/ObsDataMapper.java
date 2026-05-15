package com.obs.dao;

import com.check.pojo.CheckDataParams;
import com.obs.pojo.ObsDataEntity;
import com.obs.pojo.ObsDataParams;
import com.upload.UploadStationData;

import java.util.List;

public interface ObsDataMapper {
    List<ObsDataEntity> queryObsData(ObsDataParams params);
    List<ObsDataEntity> queryObsCheckData(CheckDataParams params);
    int addUploadStationData(UploadStationData params);
}
