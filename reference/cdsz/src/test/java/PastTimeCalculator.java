import com.util.TimeUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;

public class PastTimeCalculator {
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

    public static void main(String[] args) {
        // 示例测试
//        LocalDateTime testTime = LocalDateTime.of(2023, 10, 1, 11, 0);  // 输入10:00
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        LocalDateTime testTime = LocalDateTime.of(year, month, day, hour, 0);
        System.out.println(getPastTime(testTime));  // 输出前一天的20:00（2023-09-30T20:00）
    }
}