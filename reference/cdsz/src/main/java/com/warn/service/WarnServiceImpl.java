package com.warn.service;

import com.util.TimeUtil;
import com.warn.dao.WarnMapper;
import com.warn.pojo.WarnInfoEntity;
import com.warn.service.inf.WarnService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @category
 * @date 2025/5/6 23:16
 * @description TODO
 */
@Service
public class WarnServiceImpl implements WarnService {
    @Resource
    private WarnMapper warnMapper;
    @Override
    public int queryWarnCount(WarnInfoEntity entity) {
//        WarnInfoEntity entity = new WarnInfoEntity();
//        Calendar calendar = Calendar.getInstance();
//        calendar.setTime(new Date());
//        calendar.add(Calendar.DAY_OF_MONTH, -1);
//        entity.setInsertTime(entity.getInsertTime());

        return warnMapper.queryWarnCount(entity);
    }

    @Override
    public List<WarnInfoEntity> queryWarnInfo(WarnInfoEntity entity) {
        if(entity.getInsertTime() == null || entity.getInsertTime().length() == 0)
        {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_MONTH, -7);
            entity.setInsertTime(TimeUtil.date2String(calendar.getTime(), "yyyy-MM-dd HH:mm:ss"));
        }

        return warnMapper.queryWarnInfo(entity);
    }
}
