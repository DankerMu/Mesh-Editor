package com.warn.service.inf;

import com.warn.pojo.WarnInfoEntity;

import java.util.List;

public interface WarnService {
    int queryWarnCount(WarnInfoEntity entity);

    List<WarnInfoEntity> queryWarnInfo(WarnInfoEntity entity);
}
