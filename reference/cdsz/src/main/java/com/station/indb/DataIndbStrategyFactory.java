package com.station.indb;

import com.station.indb.inf.DataIndbStrategy;

/**
 * @author renzhitong
 * @PROJECT_NAME: data-indb
 * @Package com.data.strategy
 * @Description: java类作用描述
 * @date 2024/2/29 10:09
 * @Version: 1.0
 */
public class DataIndbStrategyFactory {
    public static DataIndbStrategy getInstance(Class<? extends DataIndbStrategy> clazz)
    {
        DataIndbStrategy strategy = null;
        try {
            strategy = (DataIndbStrategy)Class.forName(clazz.getName()).newInstance();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return strategy;
    }
}
