package com.station.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.model.pojo.ModelManagerDetailEntity;
import com.station.pojo.City;
import com.station.pojo.Cnty;
import com.station.pojo.Province;
import com.station.pojo.StationEntity;
import com.station.pojo.StationInfoEntity;
import com.station.pojo.StationInfoParams;
import com.station.pojo.TaskListEntity;
import com.station.pojo.TaskParam;
import com.station.pojo.TaskStationEntity;

@Mapper
public interface StationInfoMapper {
    List<StationInfoEntity> queryStationInfo(StationInfoParams params);
    List<StationInfoEntity> queryStationInfoZj(StationInfoParams params);
    List<StationInfoEntity> queryStationInfoUpload(StationInfoParams params);
    List<StationInfoEntity> queryStationInfoZd(StationInfoParams params);
    List<StationInfoEntity> queryStationInfoNotModel(StationInfoParams params);
    List<StationInfoEntity> queryAllStations();
    List<StationInfoEntity> queryAllStationsZj();
    List<StationInfoEntity> queryAllStationsUpload();
    List<StationInfoEntity> queryStationInfoZd();
    List<StationInfoEntity> queryAllStationsNotModel();
    List<StationInfoEntity> queryStationsNotModelByNum(StationInfoParams params);

    int addZjStation(StationInfoEntity stationInfo);
    StationInfoEntity queryZjStationByStationName(StationInfoEntity params);
    int addWzdModel(ModelManagerDetailEntity modelManagerDetail);
    int addYzdModel(ModelManagerDetailEntity modelManagerDetail);
    int addStation(StationInfoEntity stationInfo);
    int addUploadStation(StationInfoEntity stationInfo);
    int updateStationNotModelStatus(StationInfoParams params);
    
    StationInfoEntity queryStationInfoByNum(StationInfoEntity stationInfo);

    StationInfoEntity queryMaxStationNum();
    StationInfoEntity queryStationByLonLat(StationInfoEntity stationInfo);
    StationInfoEntity queryMaxUploadStationNum();
    List<StationInfoEntity> queryUploadStationInfo();
    List<StationInfoEntity> queryForcastStations();
    List<StationInfoEntity> queryForcastStationsByTaskId(TaskParam param);
    List<TaskListEntity> queryTaskList();
    IPage<TaskListEntity> queryTaskListAndStationsByPage(IPage page, TaskListEntity param);
    List<TaskStationEntity> queryTaskStations(TaskListEntity param);
    int addTaskList(TaskListEntity param);
    int addTaskStation(TaskStationEntity param);
    int deleteTaskStation(TaskStationEntity param);
    int deleteTaskStationsByTaskId(TaskListEntity param);
    int deleteTask(TaskListEntity param);
    int updateTaskName(TaskListEntity param);
    TaskListEntity queryTaskName(TaskListEntity param);
    

    List<Province> queryProvince();

    List<City> queryCityById(Province province);

    List<Cnty> queryCntyById(City city);

    List<StationEntity> queryStations(Cnty cnty);
    
    List<StationEntity> queryYyzdStationByStationName(StationInfoParams params);
}
