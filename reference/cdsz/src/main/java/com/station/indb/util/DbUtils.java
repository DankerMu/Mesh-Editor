package com.station.indb.util;

import com.alibaba.druid.pool.DruidDataSource;
import com.util.ReadPropertiesUtil;

import java.sql.SQLException;
import java.util.Map;

/**
 * @Package com.geovis.util
 * @Description: java类作用描述
 * @date 2024/1/22 17:05
 * @Version: 1.0
 */
public class DbUtils {
    private static DruidDataSource dataSource;

    public DruidDataSource getDataSource()
    {
        return dataSource;
    }

    private DbUtils()
    {
        Map<String, String> dbMap = ReadPropertiesUtil.getTableConfigMap("db.properties");

        dataSource = new DruidDataSource();
        dataSource.setUrl(dbMap.get("odb.url"));
        dataSource.setDriverClassName(dbMap.get("odb.driverClassName"));
        dataSource.setUsername(dbMap.get("odb.username"));
        dataSource.setPassword(dbMap.get("odb.password"));


//        dataSource.setMaxWait(Long.parseLong(dbMap.get("rdb.maxWait")));
//        dataSource.setInitialSize(Integer.parseInt(dbMap.get("rdb.initialSize")));
//        dataSource.setMaxActive(Integer.parseInt(dbMap.get("rdb.maxActive")));
//        dataSource.setMinEvictableIdleTimeMillis(Long.parseLong(dbMap.get("rdb.minEvictableIdleTimeMillis")));
//        dataSource.setMaxEvictableIdleTimeMillis(Long.parseLong(dbMap.get("rdb.maxEvictableIdleTimeMillis")));
//
//        // 设置重试时间
//        dataSource.setBreakAfterAcquireFailure(Boolean.parseBoolean(dbMap.get("rdb.setBreakAfterAcquireFailure")));
//        // 尝试连接1次
//        dataSource.setConnectionErrorRetryAttempts(Integer.parseInt(dbMap.get("rdb.setConnectionErrorRetryAttempts")));
//        // 开启缓存 有助于游标提升
//        dataSource.setPoolPreparedStatements(Boolean.parseBoolean(dbMap.get("rdb.poolPreparedStatements")));
//        try {
//            dataSource.setFilters(dbMap.get("rdb.filters"));
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        dataSource.setValidationQuery(dbMap.get("rdb.validationQuery"));
//        // 连接前执行测试
//        dataSource.setTestOnBorrow(Boolean.parseBoolean(dbMap.get("rdb.testOnBorrow")));
    }

    public static DbUtils getInstance()
    {
        return DbUtilsHolder.instance;
    }

    private static class DbUtilsHolder
    {
        private final static DbUtils instance = new DbUtils();

    }
}
