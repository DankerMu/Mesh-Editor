package com.station.indb.util;

import com.alibaba.druid.pool.DruidDataSource;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @category
 * @date 2025/6/25 9:41
 * @description TODO
 */
public class QueryStationsInfoFromDBUtil {

    private static Map<String, double[]> stationsInfo;

    public Map<String, double[]> getStationsInfo()
    {
        return stationsInfo;
    }
    private QueryStationsInfoFromDBUtil()
    {
        stationsInfo = new HashMap<>();
        DruidDataSource dataSource = DbUtils.getInstance().getDataSource();
        try(Connection conn = dataSource.getConnection();
            Statement pstmt = conn.createStatement();
            ResultSet rs = pstmt.executeQuery("select station_id_d,lon,lat from station_info_tab where enabled = 0");) {
            while (rs.next()) {
                String station_id = rs.getString("station_id_d");
                double lon = rs.getDouble("lon");
                double lat = rs.getDouble("lat");
                stationsInfo.put(station_id, new double[] {lon, lat});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static Map<String, double[]> queryStationInfo()
    {
    	Map<String, double[]> stationsInfo = new HashMap<>();
        DruidDataSource dataSource = DbUtils.getInstance().getDataSource();
        try(Connection conn = dataSource.getConnection();
            Statement pstmt = conn.createStatement();
            ResultSet rs = pstmt.executeQuery("select station_id_d,lon,lat from station_info_tab where enabled = 0");
        	){
            while (rs.next()) {
                String station_id = rs.getString("station_id_d");
                double lon = rs.getDouble("lon");
                double lat = rs.getDouble("lat");
                stationsInfo.put(station_id, new double[] {lon, lat});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return stationsInfo;
    }
    
    
    public static Map<String, double[]> queryAllStationsInfo()
    {
    	Map<String, double[]> result = new HashMap<>();
    	
    	result.putAll(queryStationsInfoBySql("select station_id_d,lon,lat from station_info_tab where enabled = 0"));
    	result.putAll(queryStationsInfoBySql("select station_id_d,lon,lat from station_info_tab_zj where enabled = 0"));
    	result.putAll(queryStationsInfoBySql("select station_id_d,lon,lat from station_info_tab_upload where enabled = 0"));
    	
    	return result;
    }
    
    public static Map<String, double[]> queryStationsInfo()
    {
    	Map<String, double[]> result = new HashMap<>();
    	
    	result.putAll(queryStationsInfoBySql("select station_id_d,lon,lat from station_info_tab where enabled = 0"));
    	result.putAll(queryStationsInfoBySql("select station_id_d,lon,lat from station_info_tab_zj where enabled = 0"));
    	
    	return result;
    }
    
    public static Map<String, double[]> queryStationsInfoBySql(String sql)
    {
    	Map<String, double[]> stationsInfo = new HashMap<>();
    	DruidDataSource dataSource = DbUtils.getInstance().getDataSource();
    	try(Connection conn = dataSource.getConnection();
    			Statement pstmt = conn.createStatement();
    		ResultSet rs = pstmt.executeQuery(sql);
    			){
    		while (rs.next()) {
    			String station_id = rs.getString("station_id_d");
    			double lon = rs.getDouble("lon");
    			double lat = rs.getDouble("lat");
    			stationsInfo.put(station_id, new double[] {lon, lat});
    		}
    	} catch (SQLException e) {
    		e.printStackTrace();
    	}
    	
    	return stationsInfo;
    }

    public static Map<String, double[]> queryWzdStationInfo()
    {
    	Map<String, double[]> stationsInfo = new HashMap<>();
        DruidDataSource dataSource = DbUtils.getInstance().getDataSource();
        try(Connection conn = dataSource.getConnection();
            Statement pstmt = conn.createStatement();
            ResultSet rs = pstmt.executeQuery("select station_id_d,lon,lat from station_info_tab_zj where enabled = 0");
        	){
            while (rs.next()) {
                String station_id = rs.getString("station_id_d");
                double lon = rs.getDouble("lon");
                double lat = rs.getDouble("lat");
                stationsInfo.put(station_id, new double[] {lon, lat});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return stationsInfo;
    }

    public static QueryStationsInfoFromDBUtil getInstance()
    {
        return QueryStationsInfoFromDButilHolder.instance;
    }

    private static class QueryStationsInfoFromDButilHolder
    {
        private final static QueryStationsInfoFromDBUtil instance = new QueryStationsInfoFromDBUtil();
    }
}
