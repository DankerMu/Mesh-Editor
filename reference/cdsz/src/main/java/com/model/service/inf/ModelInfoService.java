package com.model.service.inf;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.model.pojo.ModelInfoEntity;
import com.model.pojo.ModelInfoParams;
import com.model.pojo.ModelManagerDetailEntity;
import com.model.pojo.ModelManagerEntity;
import com.model.pojo.ModelManagerParams;
import com.tool.PageResult;

public interface ModelInfoService {

    List<ModelInfoEntity> queryModelInfoLike(ModelInfoParams params);
    PageResult queryModelInfo(ModelInfoParams params);
    boolean deleteModelById(ModelInfoParams params);
    boolean addModelInfo(ModelInfoEntity params);

    List<ModelInfoEntity> queryUseableModelList(ModelInfoParams params);

    List<ModelManagerEntity> queryModelManagerList();
    
    ModelManagerEntity queryGribUsedModel();

    int updateModelStatus(ModelManagerParams params);

    ModelManagerEntity queryModelUpgradeStatusById(ModelManagerParams params);

    List<ModelManagerDetailEntity> queryModelManagerDetailById(ModelManagerParams params);
    PageResult queryModelManagerListDetail(ModelManagerParams params);
    PageResult queryGribModelManagerListDetail(ModelManagerParams params);
    
    
    List<ModelManagerDetailEntity> queryModelReplaceList(ModelManagerParams params);
    List<ModelManagerDetailEntity> queryGribModelReplaceList(ModelManagerParams params);
    
    List<ModelManagerDetailEntity> queryModelDetailListLike(ModelManagerParams params);
    int updateStationModelEnabled(ModelManagerParams params);
    
    int updateModelReplaceStatus(ModelManagerParams params);
    int updateModelReplaceStatusChange(ModelManagerParams params);
    
    int updateGribModelReplaceStatus(ModelManagerParams params);
    int updateGribModelReplaceStatusChange(ModelManagerParams params);
    
    int updateGribModelEnabledModelDetailTabById(ModelManagerParams params);
    
    List<ModelManagerDetailEntity> queryGribModelAll();
}
