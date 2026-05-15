package com.util;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * <br>
 *
 * @Title: TimeUtil.java
 * @Package org.cimiss2.dwp.RADAR.tool
 * @Description: TODO(时间工具类)
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Engineer    Description
 * ------------ ----------- --------------------------
 * 2017年12月15日 下午7:27:46   wuzuoqiang    Initial creation.
 * </pre>
 */
public class TimeUtil {

    /**
     * 缺省的日期显示格式： yyyy-MM-dd
     */
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";

    /**
     * 缺省的日期时间显示格式：yyyy-MM-dd HH:mm:ss
     */
    public static final String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static final String DATE_FMT_YMD = "yyyyMMdd";
    public static final String DATE_FMT_YMDH = "yyyyMMddHH";
    public static final String DATE_FMT_YMDHM = "yyyyMMddHHmm";
    public static final String DATE_FMT_YMDHMS = "yyyyMMddHHmmss";

    /**
     * @throws
     * @Title: getSysTime
     * @Description: TODO(获取当前系统时间)
     * @param: @return
     * @return: String
     */
    public static String getSysTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        String currentSysTime = dateFormat.format(date);
        return currentSysTime;
    }

    /**
     * 功能：获取系统时间
     *
     * @param strDateFormat 时间类格式：yyyy-MM-dd HH:mm:ss.SSS / yyyyMMdd /yyyyMMdd HH:mm:ss
     * @return ：返回指定格式的系统时间字符串
     */
    public static String getSysTime(String strDateFormat) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(strDateFormat);
        Date date = new Date();
        String currentSysTime = dateFormat.format(date);
        return currentSysTime;
    }

    /**
     * 获取制定格式的日期字符串
     * 时间类格式：yyyy-MM-dd HH:mm:ss.SSS / yyyyMMdd /yyyyMMdd HH:mm:ss
     *
     * @param date          时间
     * @param strDateFormat 时间类格式：yyyy-MM-dd HH:mm:ss.SSS / yyyyMMdd /yyyyMMdd HH:mm:ss
     * @return
     */
    public static String date2String(Date date, String strDateFormat) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(strDateFormat);
        String strTime = dateFormat.format(date);
        return strTime;
    }

    /**
     * @param dateTime 需要格式化的时间
     * @param from     原始的格式
     * @param to       转换后的格式
     * @return
     */
    public static String dateTimeStr2Str(String dateTime, String from, String to) {
        Date date = String2Date(dateTime, from);
        String result = date2String(date, to);

        return result;
    }

    /**
     * @param dateTime 需要格式化的时间
     * @param from     原始的格式
     * @param to       转换后的格式
     * @return
     */
    public static Date dateTimeStr2date(String dateTime, String from, String to) {
        Date result = null;
        Date date = String2Date(dateTime, from);
        String resultstr = date2String(date, to);
        result = String2Date(resultstr, to);
        return result;
    }

    /**
     * 字符串转为日期
     *
     * @param strDate       日期字符串
     * @param strDateFormat 日期格式，时间类格式：yyyy-MM-dd HH:mm:ss.SSS / yyyyMMdd /yyyyMMdd HH:mm:ss
     * @return
     */
    public static Date String2Date(String strDate, String strDateFormat) {
        Date date = null;
        try {
            date = new SimpleDateFormat(strDateFormat).parse(strDate);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date;
    }

    /**
     * 获取某一个日期的年份
     *
     * @param date
     * @return
     */
    public static int getYear(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c.get(Calendar.YEAR);
    }

    /**
     * 获取某一个日期的月份
     *
     * @param date
     * @return
     */
    public static int getMonth(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c.get(Calendar.MONTH) + 1;
    }

    /**
     * 获取某一个月的日
     *
     * @param date
     * @return
     */
    public static int getDayOfMonth(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * 根据日期确定星期几:1-星期日，2-星期一.....s
     *
     * @param date
     * @return
     */
    public static int getDayOfWeek(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int mydate = c.get(Calendar.DAY_OF_WEEK);
        return mydate;
    }

    /**
     * 获取某一天的小时
     *
     * @param date
     * @return
     */
    public static int getHourOfDay(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * 获取某一日期的小时，1到12小时
     *
     * @param date
     * @return
     */
    public static int getHour(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c.get(Calendar.HOUR);
    }

    /**
     * 获取某一个日期的分钟
     *
     * @param date
     * @return
     */
    public static int getMinute(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c.get(Calendar.MINUTE);
    }

    /**
     * 获取某一个日期的秒
     *
     * @param date
     * @return
     */
    public static int getSecond(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c.get(Calendar.SECOND);
    }
    
    public static int getTimeDisHour(Date t1, Date t2)
    {
        int result = 0;
        long dis = t2.getTime() - t1.getTime();
        result = (int) (dis / 3600000);

        return result;
    }

    public static String[] getYmdhm(String date) {
        int size = 5;
        String[] datea = new String[size];
        datea[0] = date.substring(0, 4);
        datea[1] = date.substring(4, 6);
        datea[2] = date.substring(6, 8);
        datea[3] = date.substring(8, 10);
        datea[4] = date.substring(10, 12);

        return datea;
    }

    public static String[] getYmdhms(String date) {
        int size = 6;
        String[] datea = new String[size];
        datea[0] = date.substring(0, 4);
        datea[1] = date.substring(4, 6);
        datea[2] = date.substring(6, 8);
        datea[3] = date.substring(8, 10);
        datea[4] = date.substring(10, 12);
        datea[5] = date.substring(12, 14);
        return datea;
    }

    public static int getTimeDis(String t1, String t2)
    {
        int result = 0;
        Date date1 = TimeUtil.String2Date(t1, "MMddHH");
        Date date2 = TimeUtil.String2Date(t2, "MMddHH");
        if (date1.after(date2))
        {
            date2.setYear(date2.getYear() + 1);
        }
        long time1 = date1.getTime();
        long time2 = date2.getTime();
        result = (int) ((time2 - time1) / 3600000);

        return result;
    }
    
    
//    public static String addHours(String input, int add) {
//        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
//
//        // 解析年月日（假设格式为 yyyy-MM-ddTHH:mm:ss）
//        int year = parseInt(bytes, 0, 4);
//        int month = parseInt(bytes, 5, 2);
//        int day = parseInt(bytes, 8, 2);
//        int hour = parseInt(bytes, 11, 2);
//        int minute = parseInt(bytes, 14, 2);
//        int second = parseInt(bytes, 17, 2);
//
//        // 增加add小时
//        hour += add;
//        int daysToAdd = hour / 24;
//        hour %= 24;
//        day += daysToAdd;
//
//        // 处理日期进位
//        int[] daysInMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
//
//        // 处理闰年
//        if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) {
//            daysInMonth[1] = 29; // 闰年2月有29天
//        }
//
//        while (day > daysInMonth[month - 1]) {
//            day -= daysInMonth[month - 1];
//            month++;
//
//            // 处理年份进位
//            if (month > 12) {
//                month = 1;
//                year++;
//
//                // 检查新年份是否为闰年
//                if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) {
//                    daysInMonth[1] = 29;
//                } else {
//                    daysInMonth[1] = 28;
//                }
//            }
//        }
//
//        // 直接构造byte[]并转为字符串
//        byte[] result = new byte[19];
//        putInt(result, year, 0, 4);
//        result[4] = '-';
//        putInt(result, month, 5, 2);
//        result[7] = '-';
//        putInt(result, day, 8, 2);
//        result[10] = ' ';
//        putInt(result, hour, 11, 2);
//        result[13] = ':';
//        putInt(result, minute, 14, 2);
//        result[16] = ':';
//        putInt(result, second, 17, 2);
//
//        return new String(result, StandardCharsets.UTF_8);
//    }
//
//    // 辅助方法：解析子数组为整数
//    private static int parseInt(byte[] bytes, int start, int length) {
//        int num = 0;
//        for (int i = 0; i < length; i++) {
//            num = num * 10 + (bytes[start + i] - '0');
//        }
//        return num;
//    }
//
//    // 辅助方法：将整数写入byte数组
//    private static void putInt(byte[] bytes, int num, int start, int length) {
//        for (int i = length - 1; i >= 0; i--) {
//            bytes[start + i] = (byte) ('0' + (num % 10));
//            num /= 10;
//        }
//    }
    
    public static void main(String[] args) {
		String date = "2025-07-06 08:00:00";
		String string = addHours(date, -8 + 24 + 12);
//		string = addHours(string, -8);
//		string = addHours(string, -8);
		System.out.println(string);
	}
    
    public static String addHours(String input, int hours) {
//      List<String> results = new ArrayList<>(inputs.size());
      char[] buffer = new char[19]; // 重用缓冲区

//      for (String input : inputs) {
          // 1. 解析日期组件
          int year = parse4Digit(input, 0);
          int month = parse2Digit(input, 5);
          int day = parse2Digit(input, 8);
          int hour = parse2Digit(input, 11);
          int minute = parse2Digit(input, 14);
          int second = parse2Digit(input, 17);

          // 2. 添加小时并处理进位
          hour += hours;
          int dayCarry = 0;

          // 处理负数小时情况
          if (hour < 0) {
              // 计算需要减去的天数
              dayCarry = (hour / 24) - 1;
              hour = hour % 24;
              if (hour < 0) hour += 24;
          } else if (hour >= 24) {
              // 处理正数小时情况
              dayCarry = hour / 24;
              hour %= 24;
          }

          // 3. 处理日期进位（支持跨月/跨年）
          if (dayCarry != 0) {
              day += dayCarry;

              // 处理日期减少（负数小时导致）
              while (day < 1) {
                  month--;
                  if (month < 1) {
                      month = 12;
                      year--;
                  }
                  day += daysInMonth(year, month);
              }

              // 处理日期增加（正数小时导致）
              while (day > daysInMonth(year, month)) {
                  day -= daysInMonth(year, month);
                  month++;
                  if (month > 12) {
                      month = 1;
                      year++;
                  }
              }
          }

          // 4. 写入格式化结果
          write4Digit(buffer, 0, year);
          buffer[4] = '-';
          write2Digit(buffer, 5, month);
          buffer[7] = '-';
          write2Digit(buffer, 8, day);
          buffer[10] = ' ';
          write2Digit(buffer, 11, hour);
          buffer[13] = ':';
          write2Digit(buffer, 14, minute);
          buffer[16] = ':';
          write2Digit(buffer, 17, second);

//          results.add(new String(buffer));
//      }

      return new String(buffer);
  }
    private static final int CHAR_ZERO = '0';
    
    // 月份天数表 [平年, 闰年]
    private static final int[][] MONTH_DAYS = {
        {31, 31}, {28, 29}, {31, 31}, {30, 30}, {31, 31}, {30, 30},
        {31, 31}, {31, 31}, {30, 30}, {31, 31}, {30, 30}, {31, 31}
    };
  // 计算月份天数（考虑闰年）
  private static int daysInMonth(int year, int month) {
      int isLeap = isLeapYear(year) ? 1 : 0;
      return MONTH_DAYS[month - 1][isLeap];
  }
  
  // 闰年判断
  private static boolean isLeapYear(int year) {
      return (year % 4 == 0) && (year % 100 != 0) || (year % 400 == 0);
  }
  
  // 以下辅助方法保持不变...
  private static int parse4Digit(CharSequence s, int offset) {
      return (s.charAt(offset) - CHAR_ZERO) * 1000 +
             (s.charAt(offset + 1) - CHAR_ZERO) * 100 +
             (s.charAt(offset + 2) - CHAR_ZERO) * 10 +
             (s.charAt(offset + 3) - CHAR_ZERO);
  }
  
  private static int parse2Digit(CharSequence s, int offset) {
      return (s.charAt(offset) - CHAR_ZERO) * 10 +
             (s.charAt(offset + 1) - CHAR_ZERO);
  }
  
  private static void write4Digit(char[] buf, int offset, int value) {
      buf[offset] = (char) (value / 1000 + CHAR_ZERO);
      buf[offset + 1] = (char) ((value % 1000) / 100 + CHAR_ZERO);
      buf[offset + 2] = (char) ((value % 100) / 10 + CHAR_ZERO);
      buf[offset + 3] = (char) (value % 10 + CHAR_ZERO);
  }
  
  private static void write2Digit(char[] buf, int offset, int value) {
      buf[offset] = (char) (value / 10 + CHAR_ZERO);
      buf[offset + 1] = (char) (value % 10 + CHAR_ZERO);
  }
    
}
