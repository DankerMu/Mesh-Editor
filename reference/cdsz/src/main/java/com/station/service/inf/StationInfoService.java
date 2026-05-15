package com.station.service.inf;

import com.station.pojo.*;
import com.tool.PageResult;

import java.util.List;

/**
 * @category
 * @date 2025/3/14 15:20
 * @description TODO
 */
public interface StationInfoService {
    List<StationInfoEntity> queryStationInfo(StationInfoParams params);
    List<StationInfoEntity> queryStationInfoShow(StationInfoParams params);
    List<StationInfoEntity> queryStationInfoZd(StationInfoParams params);
    List<StationInfoEntity> queryStationInfoNotModel(StationInfoParams params);
    List<StationInfoEntity> queryAllStations();
    List<StationInfoEntity> queryAllStationsFlag();
    List<StationInfoEntity> queryAllStationsZj();
    List<StationInfoEntity> queryAllStationsNotModel();
    StationInfoEntity queryZjStationByStationName(StationInfoEntity params);
    List<StationInfoEntity> queryForcastStations();
    List<TaskListEntity> queryTaskList();
    PageResult queryTaskListByPage(TaskListEntity task);
    List<TaskStationEntity> queryTaskStations(TaskListEntity param);

    int addZjStation(StationInfoEntity stationInfo);
    int addWzdModel(StationInfoEntity stationInfo);
    int addStations(StationInfoParams stationInfoParams, int managerId);
    int addTaskStation(TaskStationEntity param);
    int addTaskList(TaskListEntity param);
    int deleteTaskStation(TaskStationEntity param);
    int deleteTaskStationsByTaskId(TaskListEntity param);
    int deleteTask(TaskListEntity param);
    int queryTaskName(TaskListEntity param);
    int modifyTaskList(TaskListEntity param);
    int updateTaskName(TaskListEntity param);

    List<Province> queryProvince();

    List<City> queryCityById(Province province);

    List<Cnty> queryCntyById(City city);

    List<StationEntity> queryStations(Cnty cnty);
}
