package com.original.dao;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.original.pojo.DataQcManagerEntity;
import com.original.pojo.DataQcManagerParams;
import com.original.pojo.NafpDataEntity;
import com.original.pojo.NafpDataParams;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @category
 * @date 2025/3/13 11:44
 * @description TODO
 */
@Mapper
@DS("smdb")
public interface NafpDataMapper extends BaseMapper<NafpDataEntity> {
    IPage<NafpDataEntity> queryT1279Data(IPage page, NafpDataParams params);
    IPage<NafpDataEntity> queryKT1279Data(IPage page, NafpDataParams params);
    IPage<NafpDataEntity> queryEcmfData(IPage page, NafpDataParams params);
    IPage<NafpDataEntity> queryGrapesData(IPage page, NafpDataParams params);
    IPage<NafpDataEntity> querySwc3kmData(IPage page, NafpDataParams params);
    IPage<NafpDataEntity> querySwc9kmData(IPage page, NafpDataParams params);
    IPage<NafpDataEntity> queryCldasData(IPage page, NafpDataParams params);
    IPage<NafpDataEntity> queryCmpaData(IPage page, NafpDataParams params);
    IPage<NafpDataEntity> queryScmocData(IPage page, NafpDataParams params);

    IPage<DataQcManagerEntity> queryNafpDataCount(IPage page, @Param("params") NafpDataParams params);

    int deleteNafpDataByDateTimeDataSource(@Param("params") DataQcManagerParams params);

    int updateNafpDataByDateTimeDataSource(@Param("params") DataQcManagerParams params);
    DataQcManagerEntity queryOriginalNafpDataTotal(@Param("params") DataQcManagerParams params);

    int insertQcCount(@Param("params") DataQcManagerEntity params);

    IPage<DataQcManagerEntity> queryOriginalNafpDataCountByPage(IPage page, @Param("params") DataQcManagerParams params);
    IPage<DataQcManagerEntity> queryOriginalNafpDataCountByPageAll(IPage page, @Param("params") DataQcManagerParams params);
    IPage<DataQcManagerEntity> queryOriginalNafpDataCountByPageAllCldasCmpa(IPage page, @Param("params") DataQcManagerParams params);
    
    List<DataQcManagerEntity> queryOriginalNafpDataCountByAll(@Param("params") DataQcManagerParams params);
    List<DataQcManagerEntity> queryOriginalNafpDataCountByAllCldas(@Param("params") DataQcManagerParams params);
    List<DataQcManagerEntity> queryOriginalNafpDataCountByAllCmpa(@Param("params") DataQcManagerParams params);
    
    IPage<DataQcManagerEntity> queryOriginalNafpDataCountCldasCmpa(IPage page, @Param("params") DataQcManagerParams params);
    IPage<DataQcManagerEntity> queryOriginalNafpDataCountPageCldasCmpa(IPage page, @Param("params") DataQcManagerParams params);
    
    
    
    List<DataQcManagerEntity> queryOriginalNafpDataCount(@Param("params") DataQcManagerParams params);
    List<DataQcManagerEntity> queryOriginalNafpDataCountAll(@Param("params") DataQcManagerParams params);
    
    List<DataQcManagerEntity> queryDataManagerCountAll(@Param("params") DataQcManagerParams params);
    List<DataQcManagerEntity> queryDataManagerCount(@Param("params") DataQcManagerParams params);
    
    IPage<DataQcManagerEntity> queryDataManagerCountByPageAll(IPage page, @Param("params") DataQcManagerParams params);
    IPage<DataQcManagerEntity> queryDataManagerCountByPage(IPage page, @Param("params") DataQcManagerParams params);
    
//    List<DataQcManagerEntity> queryOriginalNafpDataCountCldasCmpa(@Param("params") DataQcManagerParams params);
//    List<DataQcManagerEntity> queryOriginalNafpDataCountAllCldasCmpa(@Param("params") DataQcManagerParams params);
}
