package com.model.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.model.pojo.*;
import com.station.pojo.StationInfoEntity;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ModelInfoMapper {

    List<ModelInfoEntity> queryModelInfoLike(@Param("params") ModelInfoParams params);

    IPage<ModelInfoEntity> queryModelInfo(IPage page, @Param("params") ModelInfoParams params);

    boolean deleteModelById(@Param("params") ModelInfoParams params);
    boolean addModelInfo(@Param("params") ModelInfoEntity params);

    ModelInfoEntity queryModelInfoById(@Param("params") ModelInfoParams params);

    List<ModelInfoEntity> queryUseableModelList(@Param("params") ModelInfoEntity params);
    List<ModelManagerEntity> queryModelManagerList();
    ModelManagerEntity queryModelByModelId(ModelManagerParams params);
    ModelManagerEntity queryGribUsedModel();

    int updateModelStatus(ModelManagerEntity params);

    ModelManagerEntity queryModelUpgradeStatusById(ModelManagerParams params);

    List<ModelManagerDetailEntity> queryModelManagerDetailById(@Param("params") ModelManagerParams params);
    List<ModelManagerDetailEntity> queryGribModelManagerDetailById(@Param("params") ModelManagerParams params);
    IPage<ModelManagerDetailEntity> queryModelManagerListDetail(IPage page, @Param("params") ModelManagerParams params);
    IPage<GribModelManagerDetailEntity> queryGribModelManagerListDetail(IPage page, @Param("params") ModelManagerParams params);

    List<ModelManagerEntity> queryModelReplaceStatus(ModelManagerParams params);
    List<ModelManagerEntity> queryModelBuilderStatus(ModelManagerParams params);
    
    List<ModelManagerDetailEntity> queryModelReplaceList(ModelManagerParams params);
    List<ModelManagerDetailEntity> queryGribModelReplaceList(ModelManagerParams params);
    
    List<ModelManagerDetailEntity> queryModelDetailListLike(ModelManagerParams params);
    List<ModelManagerDetailEntity> queryGribModelDetailListLike(ModelManagerParams params);
    
    int updateStationModelEnabled(ModelManagerParams params);
    int updateStationModelEnabledInt(ModelManagerParams params);
    int updateStationInfoAllEnabled(ModelManagerParams params);
    int updateStationInfoTrans(StationInfoEntity params);
    StationInfoEntity queryStationInfo(StationInfoEntity params);
    
    int updateStationModelEnabledModelDetailTabById(ModelManagerParams params);
    
    int updateGribModelEnabledModelDetailTabById(ModelManagerParams params);
    
    int updateModelReplaceStatus(ModelManagerParams params);
    int updateModelReplaceStatusChange(ModelManagerParams params);
    
    int updateGribModelReplaceStatus(ModelManagerParams params);
    int updateGribModelReplaceStatusChange(ModelManagerParams params);
    
    List<ModelManagerDetailEntity> queryGribModelAll();
}
