import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSONObject;

import cn.hutool.core.io.FileUtil;


public class DistinctStation {
	public static void main(String[] args) {
		String filePath = "E:/fl/datas/bfz_station.sql";
		List<String> lines = FileUtil.readLines(filePath, "utf-8");
		Map<String, List<String>> map = new HashMap<>();
		String[] split = null;
		String key = null;
		for(String line : lines)
		{
			split = line.split(",");
			key = split[1] + "," + split[2];
			if(!map.containsKey(key))
			{
				map.put(key, new ArrayList<>());
			}
			map.get(key).add(split[0]);
		}
		for(String lonlat : map.keySet())
		{
			if(map.get(lonlat).size() > 1)
			{
				System.out.println(JSONObject.toJSONString(map.get(lonlat)));
			}
		}
	}
}
