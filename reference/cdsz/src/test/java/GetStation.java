import java.util.List;

import cn.hutool.core.io.FileUtil;


public class GetStation {
	public static void main(String[] args) {
		String filePath1 = "E:/fl/datas/station_bfz/station1.sql";
		String filePath2 = "E:/fl/datas/station_bfz/station2.sql";
		
		List<String> list1 = FileUtil.readLines(filePath1, "utf-8");
		List<String> list2 = FileUtil.readLines(filePath2, "utf-8");//shao
		for(String str : list1)
		{
			if(!list2.contains(str))
			{
				System.out.println(str);
			}
		}
		
	}
}
