package com.station.pojo;

import java.util.List;

import lombok.Data;

@Data
public class TaskParam {
	private String[] stations;
	private int taskId;
	private String author;
	private List<StationInfoEntity> stationList;
}
