package com.original.schedule;

import com.alibaba.fastjson2.JSONObject;
import com.original.dao.NafpDataMapper;
import com.original.pojo.DataQcManagerEntity;
import com.original.pojo.DataQcManagerParams;
import com.util.ReadPropertiesUtil;
import com.util.TimeUtil;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 定时任务 静态定时任务
 *
 * 第一位，表示秒，取值0-59
 * 第二位，表示分，取值0-59
 * 第三位，表示小时，取值0-23
 * 第四位，日期天/日，取值1-31
 * 第五位，日期月份，取值1-12
 * 第六位，星期，取值1-7，1表示星期天，2表示星期一
 * 第七位，年份，可以留空，取值1970-2099
 * @author crush
 * @since 1.0.0
 * @Date: 2021-07-27 21:13
 */
@Component
public class SchedulingTaskBasic {

    @Resource
    private NafpDataMapper nafpDataMapper;

    private static Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("data_count.properties");
    /**
     * 每五秒执行一次
     */
    @Scheduled(cron = "*/5 * * * * ?")
    private void queryOriginalData() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        LocalDateTime testTime = LocalDateTime.of(year, month, day, hour, 0);
        String dataTime = getPastTime(testTime);
        List<DataQcManagerEntity> list = new ArrayList<>();
        for(String dataSource : configMap.keySet())
        {
            DataQcManagerParams dataQcManagerParams = new DataQcManagerParams();
            dataQcManagerParams.setDataSource(dataSource);
            DataQcManagerEntity data = nafpDataMapper.queryOriginalNafpDataTotal(dataQcManagerParams);
            data.setDataTime(dataTime);
            data.setTotal(Integer.parseInt(configMap.get(dataSource)));
            data.setUnarrived(data.getTotal() - data.getArrived());
            data.setStatus(data.getUnarrived() == 0 ? "正常" : "异常");
            data.setDataSource(dataSource);
            data.setInsertTime(TimeUtil.date2String(new Date(), "yyyy-MM-dd HH:mm:ss"));
            list.add(data);
        }
        for(DataQcManagerEntity data : list)
        {
            nafpDataMapper.insertQcCount(data);
        }
    }

    public static String getPastTime(LocalDateTime t) {
        LocalDate date = t.toLocalDate();
        LocalDateTime d8 = date.atTime(8, 0);     // 当天08:00
        LocalDateTime d20 = date.atTime(20, 0);    // 当天20:00

        // 确定最近的过去时间点（baseTime）
        LocalDateTime baseTime;
        if (t.isEqual(d20) || t.isAfter(d20)) {
            baseTime = d20;                        // 当天20:00
        } else if (t.isEqual(d8) || t.isAfter(d8)) {
            baseTime = d8;                         // 当天08:00
        } else {
            baseTime = d20.minusDays(1);           // 前一天的20:00
        }

        // 计算时间差（注意顺序：baseTime到t的持续时间）
        Duration duration = Duration.between(baseTime, t);
        if (duration.toHours() <= 2) {             // 不足或等于2小时
            baseTime = baseTime.minusHours(12);    // 向前推12小时
        }
        String timeStr = TimeUtil.dateTimeStr2Str(baseTime.toString().replace("T", " "), "yyyy-MM-dd HH", "yyyy/MM/dd HH:mm");

        return timeStr;
    }
}