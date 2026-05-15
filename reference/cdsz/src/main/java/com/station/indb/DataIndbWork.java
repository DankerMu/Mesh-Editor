package com.station.indb;

import cn.hutool.core.io.FileUtil;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.JSONObject;
import com.constants.DataTypeEnum;
import com.station.indb.util.DbUtils;
import com.station.indb.util.LoggableStatementUtil;
import com.util.ReadPropertiesUtil;
import com.util.TimeUtil;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * @PROJECT_NAME: data-indb
 * @Package com.data
 * @Description: java类作用描述
 * @date 2024/2/28 15:15
 * @Version: 1.0
 */
@Slf4j
public class DataIndbWork {

    private static DruidDataSource dataSource = DbUtils.getInstance().getDataSource();
    private static DataIndbStrategyContextService dataListService = new DataIndbStrategyContextService();
    private static Set<String> persistEleSets = new HashSet<>();
    private static Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config_indb.properties");
    static
    {
        persistEleSets.add("station");
        persistEleSets.add("datatime");
        persistEleSets.add("validdate");
        persistEleSets.add("hour");
        persistEleSets.add("vti");
    }

    public static void indb(String filePath, String dataType, String tableName, Map<String, double[]> stationLonlats) {
        Map<String, List<JSONObject>> dataMapList = dataListService.getDataList(filePath, dataType, tableName, stationLonlats);
//        FileUtil.appendString(filePath + " 文件抽取结果：" + JSONObject.toJSON(dataMapList) + "\r\n", "/data/extract.txt", "utf-8");
        for(String key : dataMapList.keySet())
        {
        	
        	if(key.equals(DataTypeEnum.FY4B.getDataType()))
            {
                updateMicapsStationDataTcc(dataMapList.get(key), tableName);
            }
        	else if(dataType.equals(DataTypeEnum.STATION.getDataType()) 
            		|| dataType.endsWith("_" + DataTypeEnum.RAIN.getDataType())
            		|| dataType.startsWith(DataTypeEnum.DEEP.getDataType())
            		|| dataType.equals(DataTypeEnum.ECMF.getDataType())
            		|| dataType.equals(DataTypeEnum.GRAPES.getDataType())
            		|| dataType.equals(DataTypeEnum.MICAPS.getDataType())
            		|| dataType.startsWith(DataTypeEnum.CLDAS.getDataType()))
            {
//        		TODO 测试注掉，发布时去掉
            	insertUpdateDataToDb(dataMapList.get(key), key);
            }
            else
            {
            	insertDataToDb(dataMapList.get(key), key);
            }
        }
        
        if(dataType.equals(DataTypeEnum.MICAPS.getDataType()) && !filePath.contains("_his"))
        {
        	Calendar calendar = Calendar.getInstance();
        	calendar.setTime(new Date());
        	String endTime = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATE_FORMAT);
        	calendar.add(Calendar.HOUR_OF_DAY, -24);
        	String startTime = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATE_FORMAT);
        	
        	DealCheckJob.checkIndb(startTime, endTime);
        	
        	DealCheckJob.month();
        }
        
        if(dataType.equals(DataTypeEnum.STATION.getDataType()))
        {
        	if(dataMapList.get("public.surf_fc_tab") != null)
        	{
//        		System.out.println("00000000");
        		String dataTime = dataMapList.get("public.surf_fc_tab").get(0).getString("datatime");
        		insertFcDateTime(dataTime);
        	}
//        	if(dataMapList.get("public.surf_fc_tab").get(0) == null)
//        	{
//        		System.out.println("11111111");
//        	}
        }

    }

    private static void insertDataToDb(List<JSONObject> dataList, String tableName) {

        Connection conn = null;
        ResultSet rs = null;
        LoggableStatementUtil ps = null;
        int batchSize = 250;
        List<String> sqlList = new ArrayList<>();
        try {
            conn = dataSource.getConnection();
            // 不自动提交事务
            conn.setAutoCommit(false);
            // 获取所有全面的字段的key
            Set<String> fieldNames = new HashSet<>();
            for (JSONObject jsonObject : dataList) {
                fieldNames.addAll(jsonObject.keySet());
            }
            if(fieldNames.size() == 0)
            {
                return ;
            }
            StringBuffer fieldSb = new StringBuffer();
            StringBuffer valueSb = new StringBuffer();
            for (String field : fieldNames) {
                fieldSb.append(field).append(",");
                valueSb.append(" ? ").append(",");
            }
            String sql = "insert into " + tableName + "(" + fieldSb.substring(0, fieldSb.length() - 1) + ") values("
                    + valueSb.substring(0, valueSb.length() - 1) + ")";
//            ps = conn.prepareStatement(sql);
            ps = new LoggableStatementUtil(conn, sql);
//            log.info("预前置sql:{}", sql);
            int count = 0;
            for (JSONObject jsonObject : dataList) {
                int valueIndex = 0;
                for (String fieldName : fieldNames) {
                    Object obj = (jsonObject.containsKey(fieldName) && jsonObject.get(fieldName) != null) ? jsonObject.get(fieldName) : null;
                    ps.setObject(valueIndex + 1, obj);
                    valueIndex++;
                }
                count++;
                ps.addBatch();
                sqlList.add(ps.getQueryString());
//                if(ps.getQueryString().contains("42071"))
//                {
//                	System.out.println(ps.getQueryString());
//                }
                if (count % batchSize == 0) {
//                    log.info("提交一次{}", count);
                    ps.executeBatch();
                    ps.clearBatch();
                }
            }
            // 提交最后一次
            if (dataList.size() % batchSize != 0) {
                ps.executeBatch();
//                log.info("提交最后一次{}", dataList.size() % batchSize);
                ps.clearBatch();
            }
            log.info("该批次处理完毕!");
            conn.commit();
            
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                conn.close();
//                conn.rollback();
                conn = dataSource.getConnection();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            log.info("开始逐条入库>>>>>>>");
            Statement st = null;
            try {
                st = conn.createStatement();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            if(st != null)
            {
                for(String sql : sqlList)
                {
                    try {
                    	System.out.println("===============>sql0000: " + sql);
                        st.executeUpdate(sql);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
            }
            sqlList.clear();
            try {
                st.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    private static void insertUpdateDataToDb(List<JSONObject> dataList, String tableName) {

        Connection conn = null;
//        ResultSet rs = null;
        LoggableStatementUtil ps = null;
        int batchSize = 250;
        
        long time = System.currentTimeMillis();
        List<String> sqlList = new ArrayList<>();
        try {
            conn = dataSource.getConnection();
            // 不自动提交事务
            conn.setAutoCommit(false);
            // 获取所有全面的字段的key
            Set<String> fieldNames = new HashSet<>();
            for (JSONObject jsonObject : dataList) {
                fieldNames.addAll(jsonObject.keySet());
            }
            if(fieldNames.size() == 0)
            {
                return ;
            }
            StringBuffer fieldSb = new StringBuffer();
            StringBuffer valueSb = new StringBuffer();
            StringBuffer setSb = new StringBuffer();
            for (String field : fieldNames) {
                fieldSb.append(field).append(",");
                setSb.append(field).append(" =").append(" ? ").append(",");
                valueSb.append(" ? ").append(",");
            }
//            System.out.println("============: " + configMap.get(tableName));
            String sql = "insert into " + tableName + "(" + fieldSb.substring(0, fieldSb.length() - 1) + ") values("
                    + valueSb.substring(0, valueSb.length() - 1) + ")"
                    + " on conflict(" + configMap.get(tableName) + ") do update set " + setSb.substring(0, setSb.length() - 1);
//            ps = conn.prepareStatement(sql);
            ps = new LoggableStatementUtil(conn, sql);
//            log.info("预前置sql:{}", sql);
            int count = 0;
            for (JSONObject jsonObject : dataList) {
                int valueIndex = 0;
                for (String fieldName : fieldNames) {
                    Object obj = (jsonObject.containsKey(fieldName) && jsonObject.get(fieldName) != null) ? jsonObject.get(fieldName) : null;
                    ps.setObject(valueIndex + 1, obj);
                    ps.setObject(fieldNames.size() + valueIndex + 1, obj);
                    valueIndex++;
                }
                count++;
                ps.addBatch();
                sqlList.add(ps.getQueryString());
//                System.out.println(ps.getQueryString());
                if (count % batchSize == 0) {
//                    log.info("提交一次{}", count);
                    ps.executeBatch();
                    ps.clearBatch();
                }
            }
            // 提交最后一次
            if (dataList.size() % batchSize != 0) {
                ps.executeBatch();
//                log.info("提交最后一次{}", dataList.size() % batchSize);
                ps.clearBatch();
            }
            log.info("该批次处理完毕!");
            conn.commit();
            
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                conn.close();
//                conn.rollback();
                conn = dataSource.getConnection();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            log.info("开始逐条入库>>>>>>>");
            Statement st = null;
            try {
                st = conn.createStatement();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            if(st != null)
            {
                for(String sql : sqlList)
                {
                    try {
//                    	System.out.println("===============>sql: " + sql);
                        st.executeUpdate(sql);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
            }
            sqlList.clear();
            try {
                st.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        
        System.out.println(tableName + "表入库耗时:" + (System.currentTimeMillis() - time) + " 入库条数: " + dataList.size());
    }

    private static void insertT1279DataToDb(List<JSONObject> dataList, String tableName) {

        Connection conn = null;
        ResultSet rs = null;
        LoggableStatementUtil ps = null;
        int batchSize = 250;
        List<String> sqlList = new ArrayList<>();
        try {
            conn = dataSource.getConnection();
            // 不自动提交事务
            conn.setAutoCommit(false);

            String querySql = "select filepath from " + tableName + " where datatime = '" + dataList.get(0).getString("datatime") + "' and vti = " + dataList.get(0).getInteger("vti");
            ps = new LoggableStatementUtil(conn, querySql);
            ResultSet resultSet = ps.executeQuery();
            boolean has = false;
            String oldFilePath = null;
            while (resultSet.next())
            {
                has = true;
                oldFilePath = resultSet.getString("filepath");
                break;
            }
            if(has)
            {
                Set<String> fieldNames = new HashSet<>();
                for (String fieldName : dataList.get(0).keySet()) {
                    if(!persistEleSets.contains(fieldName))
                    {
                        fieldNames.add(fieldName);
                    }
                }
                if(fieldNames.size() == 0)
                {
                    return ;
                }
                StringBuffer fieldSb = new StringBuffer();
//                StringBuffer valueSb = new StringBuffer();
                List<String> fieldIndexList = new ArrayList<>();
                for (String field : fieldNames) {
                    fieldSb.append(field).append(" = ").append(" ? ").append(",");
                    fieldIndexList.add(field);
//                    valueSb.append(" ? ").append(",");
                }
//                update grib_rain_tab1 set vti = 2,datatime = '2' where datasource = '1'
                String update = "update " + tableName + " set " + fieldSb.substring(0, fieldSb.length() - 1) + " where station = ? and datatime = ? and vti = ?";
                fieldIndexList.add("station");
                fieldIndexList.add("datatime");
                fieldIndexList.add("vti");
                ps = new LoggableStatementUtil(conn, update);
                log.info("预前置sql:{}", update);
                int count = 0;
                for (JSONObject jsonObject : dataList) {
                    jsonObject.put("filepath", oldFilePath + "," + jsonObject.getString("filepath"));
                    int valueIndex = 0;
                    for (String fieldName : fieldIndexList) {
                        Object obj = (jsonObject.containsKey(fieldName) && jsonObject.get(fieldName) != null) ? jsonObject.get(fieldName) : null;
                        ps.setObject(valueIndex + 1, obj);
                        valueIndex++;
                    }
                    count++;
                    ps.addBatch();
                    sqlList.add(ps.getQueryString());
                    if (count % batchSize == 0) {
                        log.info("提交一次{}", count);
                        ps.executeBatch();
                        ps.clearBatch();
                    }
                }
                // 提交最后一次
                if (dataList.size() % batchSize != 0) {
                    ps.executeBatch();
                    log.info("提交最后一次{}", dataList.size() % batchSize);
                    ps.clearBatch();
                }
                log.info("该批次处理完毕!");
                conn.commit();
            }
            else
            {
                // 获取所有全面的字段的key
                Set<String> fieldNames = new HashSet<>();
                for (JSONObject jsonObject : dataList) {
                    fieldNames.addAll(jsonObject.keySet());
                }
                if(fieldNames.size() == 0)
                {
                    return ;
                }
                StringBuffer fieldSb = new StringBuffer();
                StringBuffer valueSb = new StringBuffer();
                for (String field : fieldNames) {
                    fieldSb.append(field).append(",");
                    valueSb.append(" ? ").append(",");
                }
                String sql = "insert into " + tableName + "(" + fieldSb.substring(0, fieldSb.length() - 1) + ") values("
                        + valueSb.substring(0, valueSb.length() - 1) + ")";
//            ps = conn.prepareStatement(sql);
                ps = new LoggableStatementUtil(conn, sql);
                log.info("预前置sql:{}", sql);
                int count = 0;
                for (JSONObject jsonObject : dataList) {
                    int valueIndex = 0;
                    for (String fieldName : fieldNames) {
                        Object obj = (jsonObject.containsKey(fieldName) && jsonObject.get(fieldName) != null) ? jsonObject.get(fieldName) : null;
                        ps.setObject(valueIndex + 1, obj);
                        valueIndex++;
                    }
                    count++;
                    ps.addBatch();
                    sqlList.add(ps.getQueryString());
                    if (count % batchSize == 0) {
                        log.info("提交一次{}", count);
                        ps.executeBatch();
                        ps.clearBatch();
                    }
                }
                // 提交最后一次
                if (dataList.size() % batchSize != 0) {
                    ps.executeBatch();
                    log.info("提交最后一次{}", dataList.size() % batchSize);
                    ps.clearBatch();
                }
                log.info("该批次处理完毕!");
                conn.commit();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            try {
                conn.close();
//                conn.rollback();
                conn = dataSource.getConnection();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            log.info("开始逐条入库>>>>>>>");
            Statement st = null;
            try {
                st = conn.createStatement();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            if(st != null)
            {
                for(String sql : sqlList)
                {
                    try {
                        st.executeUpdate(sql);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
            }
            sqlList.clear();
            try {
                st.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private static void updateMicapsStationDataTcc(List<JSONObject> dataList, String tableName)
    {
        int batchSize = 250;
        String sql = "update " + tableName + " set tcc=?, tccupdatetime=? where station_id_d = ? and datetime = ? ";
        List<String> sqlList = new ArrayList<>();
        try(Connection conn = dataSource.getConnection();
            LoggableStatementUtil ps = new LoggableStatementUtil(conn, sql)) {
//            log.info("预前置sql:{}", sql);
            int count = 0;
            // 不自动提交事务
            conn.setAutoCommit(false);
            for(JSONObject data : dataList)
            {
                ps.setObject(1, data.getDouble("tcc"));
                ps.setObject(2, TimeUtil.date2String(new Date(), "yyyy-MM-dd HH:mm:ss"));
                ps.setObject(3, data.getString("station_id_d"));
                ps.setObject(4, data.getString("datetime"));

                count++;
                ps.addBatch();
                sqlList.add(ps.getQueryString());
//                System.out.println(ps.getQueryString());
                if (count % batchSize == 0) {
//                    log.info("提交一次{}", count);
                    ps.executeBatch();
                    ps.clearBatch();
                }
            }

            // 提交最后一次
            if (dataList.size() % batchSize != 0) {
                ps.executeBatch();
//                log.info("提交最后一次{}", dataList.size() % batchSize);
                ps.clearBatch();
            }
            conn.commit();
            log.info("该批次处理完毕!");
        } catch (Exception e) {
            e.printStackTrace();
            log.info("开始逐条入库>>>>>>>");
            try(Connection conn = dataSource.getConnection();
                Statement st = conn.createStatement();) {
                if(st != null)
                {
                    for(String sqlStr : sqlList)
                    {
                        try {
                            st.executeUpdate(sqlStr);
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
                sqlList.clear();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
    

    private static void insertFcDateTime(String dataTime)
    {
    	Calendar calendar = Calendar.getInstance();
    	calendar.setTime(new Date());
    	calendar.add(Calendar.HOUR_OF_DAY, 8);
    	String insertTime = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT);
    	String sql = "insert into public.surf_fc_datatime_tab(datatime,inserttime) values('" + dataTime + "','" + insertTime + "') "
    				+ " on conflict(datatime) do update set datatime = '" + dataTime + "', inserttime = '" + insertTime + "'";
//    	System.out.println(sql);
        try(Connection conn = dataSource.getConnection();
        	Statement st = conn.createStatement();) {
        	st.executeUpdate(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
