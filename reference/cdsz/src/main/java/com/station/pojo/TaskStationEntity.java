package com.station.pojo;

import lombok.Data;

@Data
public class TaskStationEntity {
	private int taskId;
	private String station;
	private String stationIdD;
	private String stationName;
	private double lon;
	private double lat;
	private int enabled;
	private String inserttime;
	private int flag;
	private int index;
}
