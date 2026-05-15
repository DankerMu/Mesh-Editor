package com.station.cut;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import com.util.ReadPropertiesUtil;
import com.util.ThreadPoolUtil;

public class StartCutDataThread implements Runnable{

	private String dataType;
	private String date;
	private ExecutorService executor = ThreadPoolUtil.getInstance();
	public StartCutDataThread(String dataType, String date) {
		this.dataType = dataType;
		this.date = date;
	}
	
	@Override
	public void run() {
		Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
		String inputDir = configMap.get(dataType + "_his_path");
		String outputDir = configMap.get(dataType + "_his_cut_path");
		Map<String, String> suffixMap = new HashMap<>();
		suffixMap.put("ecmf", ".grib");
		suffixMap.put("grapes", ".grib2");
		Map<String, String[]> map = new HashMap<>();
		map.put("ecmf", new String[]{"lon", "lat"});
		map.put("grapes", new String[]{"lon", "lat", "height_above_ground2"});
		String suffix = suffixMap.get(dataType);
		File file = new File(inputDir + date);
		if(file.exists())
		{
			List<File> listFiles = new ArrayList<>();
			File[] files = file.listFiles(new FilenameFilter() {
				
				@Override
				public boolean accept(File dir, String name) {
					
					return name.endsWith(suffix);
				}
			});
			if(files != null && files.length != 0)
			{
				for(File f : files)
				{
					System.out.println("发现文件: " + f.getAbsolutePath());
					listFiles.add(f);
				}
			}
			
			Map<String, String[]> elementsMap = new HashMap<>();
			elementsMap.put("ecmf", new String[]{"2_metre_temperature_surface", "total_precipitation_surface", "visibility_surface", "low_cloud_cover_surface", 
					  						     "total_cloud_cover_surface", "10_metre_u_wind_component_surface", "10_metre_v_wind_component_surface"});
			elementsMap.put("grapes",  new String[]{"temperature_height_above_ground", "total_precipitation_surface_$_hour_accumulation", "visibility_surface", "low_cloud_cover_surface_layer", 
											         "total_cloud_cover_surface_layer", "u-component_of_wind_height_above_ground", "v-component_of_wind_height_above_ground"});
			Map<String, String> prefixMap = new HashMap<>();
			prefixMap.put("ecmf", "latlon_1441x2880-0p06s-180p00e:");
			prefixMap.put("grapes", "");
			String[] elements = elementsMap.get(dataType);
			String prefix = prefixMap.get(dataType);
			String[] lonlat = map.get(dataType);
			
			CutDataThread cutDataThread = new CutDataThread(dataType, listFiles, outputDir, prefix, elements, lonlat);
			executor.execute(cutDataThread);
		}
	}

}
