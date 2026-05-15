package com.station.pojo;

import java.util.List;

import com.tool.PageParam;

import lombok.Data;

@Data
public class TaskListEntity extends PageParam{
	private int id;
	private String taskName;
	private int enabled;
	private String inserttime;
	private List<StationInfoEntity> stationList;
	private List<TaskStationEntity> stations;
	private int flag;
}
