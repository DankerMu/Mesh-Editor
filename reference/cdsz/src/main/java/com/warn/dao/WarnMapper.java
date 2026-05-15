package com.warn.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.warn.pojo.WarnInfoEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface WarnMapper extends BaseMapper<WarnInfoEntity> {
    int queryWarnCount(@Param("params") WarnInfoEntity entity);

    List<WarnInfoEntity> queryWarnInfo(@Param("params") WarnInfoEntity entity);
}
