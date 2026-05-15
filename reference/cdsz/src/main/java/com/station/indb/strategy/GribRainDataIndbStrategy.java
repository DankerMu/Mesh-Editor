package com.station.indb.strategy;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import cn.hutool.core.io.FileUtil;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.JSONObject;
import com.constants.DataTypeEnum;
import com.model.task.HttpTool;
import com.station.indb.inf.DataIndbStrategy;
import com.station.indb.util.GribToPng;
import com.station.indb.util.QueryStationsInfoFromDBUtil;
import com.station.indb.util.ReadTableConfigMapUtils;
import com.util.DataTypeUtil;
import com.util.DbUtils;
import com.util.GribUtil;
import com.util.ReadGribRainUtil;
import com.util.ReadPropertiesUtil;
import com.util.ThreadPoolUtil;
import com.util.TimeUtil;

/**
 * @category
 * @date 2025/3/21 17:41
 * @description TODO
 */
public class GribRainDataIndbStrategy implements DataIndbStrategy {

//    Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
//    Map<String, double[]> stationLonlats = ReadPropertiesUtil.getStationInfoConfigMap("stations_info.properties");
//    Map<String, double[]> stationLonlats = QueryStationsInfoFromDBUtil.getInstance().getStationsInfo();
//    Map<String, Set<String>> configElementMap = ReadPropertiesUtil.getConfigMap("elements.properties");
	private ExecutorService service = ThreadPoolUtil.getInstance();
    @Override
    public Map<String, List<JSONObject>> getMapDataIndbList(String filePath, String tableName) {
        Map<String, List<JSONObject>> result = new HashMap<>();
        Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");        
//        pr_999_ecmwf_2025032020_240.txt
//        String name = FileUtil.getName(filePath);
        Map<String, double[]> stationLonlats = QueryStationsInfoFromDBUtil.queryAllStationsInfo();
        String[] vtiDataTime = GribUtil.getVtiDataTime(filePath, DataTypeEnum.RAIN.getDataType());
        int vti = Integer.valueOf(vtiDataTime[0]);
        String dataTime = vtiDataTime[1];
        Date date = TimeUtil.dateTimeStr2date(dataTime, TimeUtil.DATE_FMT_YMDH, TimeUtil.DATE_FMT_YMDH);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
//        calendar.set(Calendar.HOUR_OF_DAY, 8);
        dataTime = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        calendar.add(Calendar.HOUR_OF_DAY, vti);
        String validDate = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT);
        long fileSize = FileUtil.size(new File(filePath));
        String inserttime = TimeUtil.date2String(new Date(), TimeUtil.DEFAULT_DATETIME_FORMAT);
        Map<String, String> gribMap = ReadTableConfigMapUtils.rainMap;
        String dataType = DataTypeUtil.getDataType(filePath);
        if(dataType.equals(DataTypeEnum.PTYPE.getDataType()))
        {
            gribMap = ReadTableConfigMapUtils.ptypeMap;
        }
        double[][] datas = GribUtil.readGribDatasFromTxt(filePath, ",");
        String imagesPath = configMap.get("custom_images");
//        imagesPath = "E:\\fl\\datas\\images";
        String lonlat = configMap.get("lonlat");
        String[] split = lonlat.split(",");
        double[] lonlats = new double[split.length];
        lonlats[0] = Double.valueOf(split[0]);
        lonlats[1] = Double.valueOf(split[1]);
        lonlats[2] = Double.valueOf(split[2]);
        lonlats[3] = Double.valueOf(split[3]);
        System.out.println("getDatas filePath: " + filePath);
        
        if(vti > 0)
        {
        	Map<String, List<String>> datasPath = getDatasPath(filePath, dataType, vti, configMap);
        	if(datasPath == null || datasPath.size() == 0)
        	{
        		System.out.println("文件" + filePath + " 没有获取到数据。");
        		return result;
        	}
        	List<JSONObject> list = new ArrayList<>();
        	for(String key : datasPath.keySet())
        	{
        		if(key.endsWith("_ptype"))
        		{
        			continue;
        		}
        		String out = filePath.replace(String.format("%03d", vti) + ".txt", key + ".txt");
        		String outPath = imagesPath + File.separator + dataType + File.separator + FileUtil.getPrefix(out) + ".png";
        		outPath = outPath.replace("_rain.png", ".png");
        		
        		List<String> list2 = datasPath.get(key);
        		String precip_file = list2.get(0);
        		String precip_pre_path = list2.get(1);
        		List<String> list3 = datasPath.get(key.replace("_rain", "_ptype"));
        		com.alibaba.fastjson2.JSONObject jsonParam = new com.alibaba.fastjson2.JSONObject();
        		jsonParam.put("precip_file", precip_file);
        		jsonParam.put("precip_pre_path", precip_pre_path);
        		jsonParam.put("output_path", outPath);
        		jsonParam.put("phase_path", list3);
        		
//        		HttpTool.doPost(configMap.get("plot_image_url"), jsonParam.toString());
        		service.execute(new HttpRunThread(configMap.get("plot_image_url"), jsonParam.toString()));
        		
        		String suffix = FileUtil.getSuffix(outPath);
        		JSONObject json = new JSONObject();
        		json.put(gribMap.get("filename"), FileUtil.getName(outPath));
        		json.put(gribMap.get("dataTime"), dataTime);
        		json.put(gribMap.get("validdate"), validDate);
        		json.put(gribMap.get("hour"), hour);
        		json.put(gribMap.get("vti"), vti);
        		json.put(gribMap.get("rainvti"), Integer.parseInt(key.split("_")[1]));
        		json.put(gribMap.get("datatype"), DataTypeEnum.RAIN.getDataType());
        		json.put(gribMap.get("datasource"), dataType);
        		json.put(gribMap.get("sourcepath"), filePath);
        		json.put(gribMap.get("filepath"), outPath);
//        		json.put(gribMap.get("urlpath"), configMap.get("server_http") + dataType + File.separator + FileUtil.getPrefix(filePath) + ".png");
        		json.put(gribMap.get("urlpath"), File.separator + dataType + File.separator + FileUtil.getName(outPath));
        		json.put(gribMap.get("inserttime"), inserttime);
        		json.put(gribMap.get("filesize"), fileSize);
        		json.put(gribMap.get("filetype"), suffix);
        		
        		list.add(json);
        	}
        	result.put(tableName, list);
        }
        
//        if(vti > 0)
//        {
//        	Map<String, double[][]> calDatas = getDatas(filePath, dataType, vti, datas, configMap);
//        	Map<String, List<String>> datasPath = getDatasPath(filePath, dataType, vti, configMap);
//        	if(calDatas == null || calDatas.size() == 0)
//        	{
//        		System.out.println("文件" + filePath + " 没有获取到数据。");
//        		return result;
//        	}
//        	if(datasPath == null || datasPath.size() == 0)
//        	{
//        		System.out.println("文件" + filePath + " 没有获取到数据。");
//        		return result;
//        	}
//        	List<JSONObject> list = new ArrayList<>();
//        	for(String key : calDatas.keySet())
//        	{
//        		if(key.endsWith("_ptype"))
//        		{
//        			continue;
//        		}
//        		String out = filePath.replace(String.format("%03d", vti) + ".txt", key + ".txt");
//        		String outPath = imagesPath + File.separator + dataType + File.separator + FileUtil.getPrefix(out) + ".png";
//        		if(calDatas.get(key + "_ptype") == null)
//        		{
//        			GribToPng.getInstance().png(outPath, calDatas.get(key), lonlats[0], lonlats[1], lonlats[2], lonlats[3], 0.05, 0.05, "");
//        		}
//        		else
//        		{
//        			GribToPng.getInstance().pngCombinePtype(outPath, calDatas.get(key), calDatas.get(key + "_ptype"), lonlats[0], lonlats[1], lonlats[2], lonlats[3], 0.05, 0.05, "");
//        		}
//        		String suffix = FileUtil.getSuffix(outPath);
//        		JSONObject json = new JSONObject();
//        		json.put(gribMap.get("filename"), FileUtil.getName(outPath));
//        		json.put(gribMap.get("dataTime"), dataTime);
//        		json.put(gribMap.get("validdate"), validDate);
//        		json.put(gribMap.get("hour"), hour);
//        		json.put(gribMap.get("vti"), vti);
//        		json.put(gribMap.get("rainvti"), Integer.parseInt(key.split("_")[1]));
//        		json.put(gribMap.get("datatype"), DataTypeEnum.RAIN.getDataType());
//        		json.put(gribMap.get("datasource"), dataType);
//        		json.put(gribMap.get("sourcepath"), filePath);
//        		json.put(gribMap.get("filepath"), outPath);
////        		json.put(gribMap.get("urlpath"), configMap.get("server_http") + dataType + File.separator + FileUtil.getPrefix(filePath) + ".png");
//        		json.put(gribMap.get("urlpath"), File.separator + dataType + File.separator + FileUtil.getName(outPath));
//        		json.put(gribMap.get("inserttime"), inserttime);
//        		json.put(gribMap.get("filesize"), fileSize);
//        		json.put(gribMap.get("filetype"), suffix);
//        		
//        		list.add(json);
//        	}
//        	result.put(tableName, list);
//        }

        JSONObject stationJson = null;
        List<JSONObject> stationRainList = new ArrayList<>();
        for(String station : stationLonlats.keySet())
        {
            stationJson = new JSONObject();
            double[] lonLats = stationLonlats.get(station);
            stationJson.put(gribMap.get("station"), station);
            stationJson.put(gribMap.get("dataTime"), dataTime);
            stationJson.put(gribMap.get("validdate"), validDate);
            stationJson.put(gribMap.get("hour"), hour);
            stationJson.put(gribMap.get("vti"), vti);
            stationJson.put(gribMap.get("rain"), getFstValue(lonLats[0], lonLats[1], datas));
            stationJson.put(gribMap.get("sourcepath"), filePath);
            stationJson.put(gribMap.get("inserttime"), TimeUtil.date2String(new Date(), TimeUtil.DEFAULT_DATETIME_FORMAT));
            stationRainList.add(stationJson);
        }
        result.put(tableName.replace("_tab", "_value_tab"), stationRainList);

        return result;
    }

    private Map<String, double[][]> getDatas(String filePathOrg, String dataType, int vti, double[][] datas, Map<String, String> configMap)
    {
    	System.out.println("dataType:" + dataType);
    	dataType = dataType.split("_")[0];
    	System.out.println("config: " + configMap.get(dataType + "_model_txt"));
    	String filePath = filePathOrg.replace(configMap.get(dataType + "_model_txt"), configMap.get(dataType + "_ptype_txt"));
    	filePath = "E:/fl/datas/ptype_deeplearning/" + FileUtil.getName(filePath);
    	filePath = filePath.replace("tp_999_" + (dataType.equals("deep") ? "deeplearning" : dataType), "ptype_999_revised");
    	
//    	filePath = filePath.replace("ecmwf_rain", "ptype_deeplearning");
        Map<String, double[][]> result = new HashMap<>();
        int[] disVtis = new int[]{3, 6, 12, 24, 48, 72, 96, 120, 144, 168};
//        if(vti > 72 && vti <= 168)
//        {
//            disVtis = new int[]{6, 12};
//        }
//        else if(vti > 168)
//        {
//            disVtis = new int[]{12};
//        }

        for(int i = 0, count = disVtis.length; i < count; i++)
        {
//            int dis = vti - disVtis[i];
        	String disVtiStr = String.format("%03d", vti - disVtis[i]);
        	if(vti == disVtis[i])
        	{
        		disVtiStr = String.format("%03d", 0);
        	}
        	if(vti >= disVtis[i])
            {
                String preFilePath = filePathOrg.replace(String.format("%03d", vti) + ".txt", disVtiStr + ".txt");
//                File f = new File(preFilePath);
//                if(!f.exists())
//                {
//                	return result;
//                }
                String prePtypeFilePath = filePath.replace(String.format("%03d", vti) + ".txt", disVtiStr + ".txt");
                double[][] preDatas = GribUtil.readGribDatasFromTxt(preFilePath, ",");
                if(preDatas == null)
                {
                	continue;
                }
                double[][] disDatas = getDisDatas(datas, preDatas);
                double[][] ptypeDatas = getPtypeDatas(prePtypeFilePath, vti, disVtis[i]);
                result.put(String.format("%03d", vti) + "_" + String.format("%03d", disVtis[i]), disDatas);
                result.put(String.format("%03d", vti) + "_" + String.format("%03d", disVtis[i]) + "_ptype", ptypeDatas);
            }
        }


        return result;
    }
    
    
    private Map<String, List<String>> getDatasPath(String filePathOrg, String dataType, int vti, Map<String, String> configMap)
    {
    	System.out.println("dataType:" + dataType);
    	dataType = dataType.split("_")[0];
    	System.out.println("config: " + configMap.get(dataType + "_model_txt"));
    	String filePath = filePathOrg.replace(configMap.get(dataType + "_model_txt"), configMap.get(dataType + "_ptype_txt"));
//    	filePath = "E:/fl/datas/ptype_deeplearning/" + FileUtil.getName(filePath);
    	filePath = filePath.replace("tp_999_" + (dataType.equals("deep") ? "deeplearning" : dataType), "ptype_999_revised");
    	
//    	filePath = filePath.replace("ecmwf_rain", "ptype_deeplearning");
        Map<String, List<String>> result = new HashMap<>();
        
        int[] disVtis = new int[]{3, 6, 12, 24, 48, 72, 96, 120, 144, 168};
        File file = null;
        for(int i = 0, count = disVtis.length; i < count; i++)
        {
        	String disVtiStr = String.format("%03d", vti - disVtis[i]);
        	if(vti == disVtis[i])
        	{
        		disVtiStr = String.format("%03d", 0);
        	}
        	if(vti >= disVtis[i])
            {
                String preFilePath = filePathOrg.replace(String.format("%03d", vti) + ".txt", disVtiStr + ".txt");
                String prePtypeFilePath = filePath.replace(String.format("%03d", vti) + ".txt", disVtiStr + ".txt");
                file = new File(preFilePath);
    			if(!file.exists())
    			{
    				continue;
    			}
                String key = String.format("%03d", vti) + "_" + String.format("%03d", disVtis[i]);
                result.put(key + "_rain", new ArrayList<>());
                result.get(key + "_rain").add(filePathOrg);
                result.get(key + "_rain").add(preFilePath);
                List<String> ptypeDatasPath = getPtypeDatasPath(prePtypeFilePath, vti, disVtis[i]);
                result.put(key + "_ptype", ptypeDatasPath);
            }
        }


        return result;
    }
    
    
    private static int[] vtisAll = new int[]{3 ,6 ,9 ,12 ,15 ,18 ,21 ,24 ,27 ,30 ,33 ,36 ,39 ,42 ,45 ,48 ,51 ,54 ,57 ,60 ,63 ,66 ,69 ,72 ,78 ,84 ,90 ,96 ,102, 108, 114, 120, 126, 132, 138, 144, 150, 156, 162, 168, 174, 180, 186, 192, 198, 204, 210, 216, 222, 228, 234, 240};

    private double[][] getPtypeDatas(String filePath, int vti, int dis)
    {
    	double[][] result = null;
    	int start = vti - dis;
    	List<double[][]> datasList = new ArrayList<>();
    	for(int v : vtisAll)
    	{
    		if(v >= start && v <= vti)
    		{
//    			String disVtiStr = String.format("%03d", vti);
    			int filePathLength = filePath.length();
//    			String preFilePath = filePath.replace(String.format("%03d", v) + ".txt", disVtiStr + ".txt");
    			String preFilePath = filePath.substring(0, filePathLength - 7) + String.format("%03d", v) + ".txt";
//    			System.out.println(preFilePath);
    			double[][] preDatas = GribUtil.readGribDatasFromTxt(preFilePath, ",");
    			if(preDatas == null)
    			{
    				return result;
    			}
    			datasList.add(preDatas);
    		}
    	}
    	if(datasList.size() == 1)
    	{
    		result = datasList.get(0);
    	}
    	else if(datasList.size() == 0)
    	{
    		result = null;
    	}
    	else
    	{
    		int vCount = datasList.get(0).length;
    		int cCount = datasList.get(0)[0].length;
    		int[][] count0 = new int[vCount][cCount];//晴
    		int[][] count1 = new int[vCount][cCount];//雨
    		int[][] count2 = new int[vCount][cCount];//雪
    		double value = 0;
    		for(int i = 0; i < vCount; i++)
    		{
    			for(int j = 0; j < cCount; j++)
    			{
    				for(int k = 0, count = datasList.size(); k < count; k++)
    				{
    					value = datasList.get(k)[i][j];
    					if(value == 0)
    					{
    						count0[i][j] += 1;
    					}
    					else if(value == 1)
    					{
    						count1[i][j] += 1;
    					}
    					else if(value == 2)
    					{
    						count2[i][j] += 1;
    					}
    				}
    			}
    		}
//    		double rainCount = datasList.size() / 10.0;
//    		double snowCount = datasList.size() / 2.0;
    		result = new double[vCount][cCount];
    		for(int i = 0; i < vCount; i++)
    		{
    			for(int j = 0; j < cCount; j++)
    			{
//    				if(count1[i][j] > 0 && count1[i][j] <= rainCount)
//    				{
//    					result[i][j] = 1;
//    				}
//    				if(count2[i][j] > rainCount && count2[i][j] <= snowCount)
//    				{
//    					result[i][j] = 3;
//    				}
//    				if(count2[i][j] > snowCount)
//    				{
//    					result[i][j] = 2;
//    				}
    				
    				if(count1[i][j] > 0 && count2[i][j] == 0)
    				{
    					result[i][j] = 1;
    				}
    				else if(count1[i][j] == 0 && count2[i][j] > 0)
    				{
    					result[i][j] = 2;
    				}
    				else if(count1[i][j] > 0 && count2[i][j] > 0)
    				{
    					result[i][j] = 3;
//    					if(count1[i][j] > count2[i][j])
//    					{
//    					}
//    					else if(count1[i][j] <= count2[i][j])
//    					{
//    						result[i][j] = 2;
//    					}
    				}
    			}
    		}
    	}
    	
    	return result;
    }
    
    private List<String> getPtypeDatasPath(String filePath, int vti, int dis)
    {
    	List<String> result = new ArrayList<>();
    	int start = vti - dis;
    	File file = null;
    	for(int v : vtisAll)
    	{
    		if(v >= start && v <= vti)
    		{
    			int filePathLength = filePath.length();
    			String preFilePath = filePath.substring(0, filePathLength - 7) + String.format("%03d", v) + ".txt";
    			file = new File(preFilePath);
    			if(file.exists())
    			{
    				result.add(preFilePath);
    			}
    		}
    	}
    	
    	return result;
    }

    private double[][] getDisDatas(double[][] datas, double[][] preDatas)
    {
        double[][] result = new double[datas.length][datas[0].length];
        for(int i = 0, count = datas.length; i < count; i++)
        {
            for(int j = 0, num = datas[i].length; j < num; j++)
            {
                result[i][j] = datas[i][j] - preDatas[i][j];
            }
        }

        return result;
    }

    private double getFstValue(double lon, double lat, double[][] values)
    {
        Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
        String lonlat = configMap.get("lonlat");
        String[] split = lonlat.split(",");
        double[] lonlats = new double[split.length];
        for(int i = 0, count = lonlats.length; i < count; i++)
        {
            lonlats[i] = Double.parseDouble(split[i]);
        }
        double lon_dis = Double.parseDouble(configMap.get("lon_dis"));
        double lat_dis = Double.parseDouble(configMap.get("lat_dis"));
        double[] findFirstPoint = ReadGribRainUtil.findFindFirstPoint(lon, lat, lon_dis, lat_dis, lonlats);
//        System.out.println("lonCount:" + values[0].length + ", latCount" + values.length);
//        System.out.println("lon:" + lon + ", lat:" + lat);
//        findFirstPoint: 670.0, 405.0
//        System.out.println("findFirstPoint: " + findFirstPoint[0] + ", " + findFirstPoint[1]);
        double topLeft = values[(int) findFirstPoint[1]][(int) findFirstPoint[0]];
        double topRight = values[(int) findFirstPoint[1]][(int) findFirstPoint[0] + 1];
        double bottomLeft = values[(int) findFirstPoint[1] + 1][(int) findFirstPoint[0]];
        double bottomRight = values[(int) findFirstPoint[1] + 1][(int) findFirstPoint[0] + 1];
        double value = ReadGribRainUtil.bilinearInterpolation(topLeft, topRight, bottomLeft, bottomRight, findFirstPoint[2], findFirstPoint[3]);

        return value;
    }
    
    private void warnningIndb(String filePath)
    {
    	DbUtils dbUtils = DbUtils.getInstance();
    	DruidDataSource dataSource = dbUtils.getDataSource();
    	try (Connection conn = dataSource.getConnection();Statement st = conn.createStatement();){
    		
    		String sql = "insert into warn_tab(type,content,inserttime) values(1,'"
                    + filePath + "文件对应的雨雪相态文件缺失!','" + TimeUtil.date2String(new Date(), TimeUtil.DEFAULT_DATETIME_FORMAT) + "')";
    		System.out.println("告警SQL: " + sql);
    		st.execute(sql);
    	} catch (SQLException e) {
			e.printStackTrace();
		}
    }

	@Override
	public void setStationLonlats(Map<String, double[]> stationLonlats) {
		
	}
}
class HttpRunThread implements Runnable{

	private String url;
	private String json;
	public HttpRunThread(String url, String json) {
		this.url = url;
		this.json = json;
	}
	
	@Override
	public void run() {
		
		HttpTool.doPost(url, json);
	}
	
}