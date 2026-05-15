import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;

import com.alibaba.fastjson2.JSONObject;
import com.util.NumberFormatUtil;
import com.util.TimeUtil;


public class TestNum {
	public static void main(String[] args) {
		double d = 1.3954665384440506;
		System.out.println(NumberFormatUtil.scienceD(d, 2));
		
		JSONObject json = new JSONObject();
		Date date = new Date();
		String dateStr = TimeUtil.date2String(date, TimeUtil.DEFAULT_DATE_FORMAT);
		String start = dateStr.substring(0, 8);
		int lastDayOfMonth = DateUtil.getLastDayOfMonth(date);
		
		json.put("startTime", start + "01");
		json.put("endTime", start + lastDayOfMonth);
		System.out.println(json);
		
		FileUtil.appendString("aaaa" + "\r\n", "E:/fl/test.txt", "utf-8");
		FileUtil.appendString("bbbb" + "\r\n", "E:/fl/test.txt", "utf-8");
		
		List<String> list1 = FileUtil.readLines(new File("E:/data/micaps.sql"), "utf-8");
		List<String> list2 = FileUtil.readLines(new File("E:/data/ecmf.sql"), "utf-8");
		List<String> list3 = FileUtil.readLines(new File("E:/data/grapes.sql"), "utf-8");
		int total = 0;
		List<String> list = new ArrayList<>();
		Map<String, Integer> countMap = new HashMap<>();
		for(int i = 0, count = list1.size(); i < count; i++)
		{
			for(int j = 0, num = list2.size(); j < num; j++)
			{
				
				if(list1.get(i).equals(list2.get(j)))
				{
//					if(!countMap.containsKey(list1.get(i)))
//					{
//						countMap.put(list1.get(i), 0);
//					}
//					countMap.put(list1.get(i), countMap.get(list1.get(i)) + 1);
					list.add(list1.get(i));
				}
			}
		}
		
		Set<String> set = new HashSet<>();
		for(String str : list)
		{
			set.add(str);
		}
		
		for(String s : set)
		{
			for(int j = 0, num = list3.size(); j < num; j++)
			{
				if(s.equals(list3.get(j)))
				{
					total++;
				}
			}
		}
		
//		for(int i = 0, count = list.size(); i < count; i++)
//		{
//			for(int j = 0, num = list3.size(); j < num; j++)
//			{
//				
//				if(list.get(i).equals(list3.get(j)))
//				{
////					if(!countMap.containsKey(list.get(i)))
////					{
////						countMap.put(list.get(i), 0);
////					}
////					countMap.put(list.get(i), countMap.get(list.get(i)) + 1);
//					total++;
//				}
//			}
//		}
		
//		System.out.println(JSONObject.toJSONString(countMap));
		System.out.println(total);
		String[] path = "E:/data/micaps.sql".split("/");
		System.out.println(path[path.length - 2]);
	}
}
