package com.upload;

import lombok.Data;

@Data
public class UploadStationData {
	private int stationNum;
	private String stationName;
	private String dateTime;
	private String insertTime;
	private Double lon;
	private Double lat;
	private double tem;
	private double prs;
	private double rhu;
	private double wd10mi;
	private double ws10mi;
	private double wdMax;
	private double wsMax;
	private double pre;
	private double temMin;
	private double temMax;
	private Double tcc;
	private Double vis;
	private String fileName;
	private String filePath;
}
