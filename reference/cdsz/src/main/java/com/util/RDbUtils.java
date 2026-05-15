package com.util;

import java.util.Map;

import com.alibaba.druid.pool.DruidDataSource;

/**
 * @Package com.geovis.util
 * @Description: java类作用描述
 * @date 2024/1/22 17:05
 * @Version: 1.0
 */
public class RDbUtils {
    private static DruidDataSource dataSource;

    public DruidDataSource getDataSource()
    {
        return dataSource;
    }

    private RDbUtils()
    {
        Map<String, String> dbMap = ReadPropertiesUtil.getTableConfigMap("db.properties");

        dataSource = new DruidDataSource();
        dataSource.setUrl(dbMap.get("rdb.url"));
        dataSource.setDriverClassName(dbMap.get("rdb.driverClassName"));
        dataSource.setUsername(dbMap.get("rdb.username"));
        dataSource.setPassword(dbMap.get("rdb.password"));

    }

    public static RDbUtils getInstance()
    {
        return DbUtilsHolder.instance;
    }

    private static class DbUtilsHolder
    {
        private final static RDbUtils instance = new RDbUtils();

    }
}
