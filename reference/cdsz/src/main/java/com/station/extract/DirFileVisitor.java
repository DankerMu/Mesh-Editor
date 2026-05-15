package com.station.extract;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import cn.hutool.core.io.FileUtil;

import com.constants.DataTypeEnum;
import com.station.indb.DataIndbWork;
import com.util.ReadPropertiesUtil;

public class DirFileVisitor extends SimpleFileVisitor<Path> {
	private Path path;
	private String dataType;
	private int dataStartTime;
	private int dataEndTime;
	private Map<String, double[]> stationInfo;
	private Map<String, String> tableNameMap = ReadPropertiesUtil.getUserConfigMap("data_table.properties");
	private static Map<String, String> map = new HashMap<>();
	static
	{
		map.put(DataTypeEnum.CLDAS.getDataType(), "PRE,TEM,WIN,VIS");
		map.put(DataTypeEnum.CLDAS.getDataType() + "_p5", "TEM,WIN,VIS");
	}
	
	public DirFileVisitor(Path path, String dataType, Map<String, double[]> stationInfo) {
		this.path = path;
		this.dataType = dataType;
		this.stationInfo = stationInfo;
	}
	
	public DirFileVisitor(Path path, String dataType, int dataStartTime, int dataEndTime, Map<String, double[]> stationInfo) {
		this.path = path;
		this.dataType = dataType;
		this.dataStartTime = dataStartTime;
		this.dataEndTime = dataEndTime;
		this.stationInfo = stationInfo;
	}
	
	@Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exec) throws IOException {
		
        // 访问文件夹之后调用
        return FileVisitResult.SKIP_SUBTREE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
    	// 访问文件夹之前调用
    	if(path.equals(dir))
    	{
    		return FileVisitResult.CONTINUE;
    	}
    	else
    	{     
    		return FileVisitResult.SKIP_SUBTREE;
    	}
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        // 访问文件后调用        
//    	System.out.println("visitFile " + file);
    	String filePath = file.toString();
    	String suffix = FileUtil.getSuffix(filePath);
    	if("ncx4".equals(suffix) || "gbx9".equals(suffix) || "idx".equals(suffix))
    	{
    		return FileVisitResult.CONTINUE;
    	}
    	
    	String dataTypeSuffix = "";
    	if(dataType.startsWith(DataTypeEnum.CLDAS.getDataType()))
    	{
    		String ele = FileUtil.getName(filePath).split("_")[10].split("-")[1];
    		if(!map.get(dataType).contains(ele))
    		{
    			return FileVisitResult.CONTINUE;
    		}
    		if(!"PRE".equals(ele))
    		{
    			dataTypeSuffix = "_p5";
    		}
    	}
    	
//    	String[] vtiDataTime = GribUtil.getVtiDataTime(filePath, dataType);
//    	int fileDataTime = Integer.parseInt(vtiDataTime[1]);
    	
//    	if(dataEndTime < dataStartTime)
//    	{
//    		return FileVisitResult.CONTINUE;
//    	}
    	
    	String tableName = tableNameMap.get(dataType);
    	
    	System.out.println(filePath + " 文件开始处理...");
    	DataIndbWork.indb(filePath, dataType + dataTypeSuffix, tableName, stationInfo);
    	System.out.println(filePath + " 文件处理完成。");
    	
    	
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        // 文件不可访问时调用
        return FileVisitResult.CONTINUE;
    }
}
