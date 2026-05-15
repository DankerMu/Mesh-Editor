import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.hutool.core.io.FileUtil;

import com.alibaba.fastjson2.JSONObject;
import com.station.indb.util.GribToPng;
import com.util.GribUtil;
import com.util.NumberFormatUtil;
import com.util.ReadPropertiesUtil;


public class RainTest {
	public static void main(String[] args) {
		String filePath = "E:/fl/datas/deep/tp_999_deeplearning_2025120208_084.txt";
		String preFilePath = "E:/fl/datas/deep/tp_999_deeplearning_2025120208_060.txt";
		double[][] datas = GribUtil.readGribDatasFromTxt(filePath, ",");
		double[][] preDatas = GribUtil.readGribDatasFromTxt(preFilePath, ",");
		double[][] disDatas = getDisDatas(datas, preDatas);
		double[][] ptypeDatas = getPtypeDatas(filePath, 84, 24);
//		for(int i = 0, count = ptypeDatas.length; i < count; i++)
//        {
//        	System.out.println(JSONObject.toJSONString(ptypeDatas[i]));
//        }
		GribToPng.getInstance().png("E:/fl/datas/images/tt.png", disDatas, 70, 50, 111, 25, 0.05, 0.05, "");
		Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties"); 
		Map<String, List<String>> datasPath = getDatasPath(filePath, "deep", 84, configMap);
		for(String key : datasPath.keySet())
		{
			String out = filePath.replace(String.format("%03d", 84) + ".txt", key + ".txt");
    		String outPath = File.separator + "deep" + File.separator + FileUtil.getPrefix(out) + ".png";
    		System.out.println(outPath);
		}
		
		System.out.println(datasPath);
		
	}
	
	private static Map<String, List<String>> getDatasPath(String filePathOrg, String dataType, int vti, Map<String, String> configMap)
    {
    	System.out.println("dataType:" + dataType);
    	dataType = dataType.split("_")[0];
    	System.out.println("config: " + configMap.get(dataType + "_model_txt"));
    	String filePath = filePathOrg.replace(configMap.get(dataType + "_model_txt"), configMap.get(dataType + "_ptype_txt"));
    	filePath = "E:/fl/datas/ptype_deeplearning/" + FileUtil.getName(filePath);
    	filePath = filePath.replace("tp_999_" + (dataType.equals("deep") ? "deeplearning" : dataType), "ptype_999_revised");
    	
//    	filePath = filePath.replace("ecmwf_rain", "ptype_deeplearning");
        Map<String, List<String>> result = new HashMap<>();
        
        int[] disVtis = new int[]{3, 6, 12, 24, 48, 72, 96, 120, 144, 168};

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
	
	private static List<String> getPtypeDatasPath(String filePath, int vti, int dis)
    {
    	List<String> result = new ArrayList<>();
    	int start = vti - dis;
    	for(int v : vtisAll)
    	{
    		if(v >= start && v <= vti)
    		{
    			int filePathLength = filePath.length();
    			String preFilePath = filePath.substring(0, filePathLength - 7) + String.format("%03d", v) + ".txt";
    			result.add(preFilePath);
    		}
    	}
    	
    	return result;
    }
	
	private static double[][] getDisDatas(double[][] datas, double[][] preDatas)
    {
        double[][] result = new double[datas.length][datas[0].length];
        for(int i = 0, count = datas.length; i < count; i++)
        {
            for(int j = 0, num = datas[i].length; j < num; j++)
            {
                result[i][j] = NumberFormatUtil.numFormat(datas[i][j] - preDatas[i][j], 1);
            }
        }
        
        return result;
    }
	
	private static int[] vtisAll = new int[]{3 ,6 ,9 ,12 ,15 ,18 ,21 ,24 ,27 ,30 ,33 ,36 ,39 ,42 ,45 ,48 ,51 ,54 ,57 ,60 ,63 ,66 ,69 ,72 ,78 ,84 ,90 ,96 ,102, 108, 114, 120, 126, 132, 138, 144, 150, 156, 162, 168, 174, 180, 186, 192, 198, 204, 210, 216, 222, 228, 234, 240};

    private static double[][] getPtypeDatas(String filePath, int vti, int dis)
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
    			System.out.println(preFilePath);
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
    		result = new double[vCount][cCount];
    		for(int i = 0; i < vCount; i++)
    		{
    			for(int j = 0; j < cCount; j++)
    			{
    				
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
    				}
    			}
    		}
    	}
    	
    	return result;
    }
}
