package com.original.service.inf;

import com.original.pojo.DataQcManagerEntity;
import com.original.pojo.DataQcManagerParams;
import com.original.pojo.NafpDataParams;
import com.tool.PageResult;

import java.util.List;

public interface QueryNafpDataService {
    PageResult queryOriginalData(NafpDataParams params);

    PageResult queryNafpDataCount(NafpDataParams params);

    int deleteNafpDataByDateTimeDataSource(DataQcManagerParams params);

    int updateNafpDataByDateTimeDataSource(DataQcManagerParams params);

    DataQcManagerEntity queryOriginalNafpData(DataQcManagerParams params);

    List<DataQcManagerEntity> queryOriginalNafpDataCount(DataQcManagerParams params);
    List<DataQcManagerEntity> queryOriginalNafpDataCountAll(DataQcManagerParams params);
    
    PageResult queryOriginalNafpDataCountByPage(DataQcManagerParams params);
    PageResult queryOriginalNafpDataCountByPageAll(DataQcManagerParams params);
    
    PageResult queryDataManagerCountByPageAll(DataQcManagerParams params);
    PageResult queryDataManagerCountByPage(DataQcManagerParams params);
    
    List<DataQcManagerEntity> queryDataManagerCountAll(DataQcManagerParams params);
    List<DataQcManagerEntity> queryDataManagerCount(DataQcManagerParams params);
}
