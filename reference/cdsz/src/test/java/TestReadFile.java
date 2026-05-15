import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.util.TimeUtil;

import cn.hutool.core.io.FileUtil;


public class TestReadFile {
	public static void main(String[] args) {
		String filePath = "E:/fl/导入观测/Report.xls";
		
		try {
//			InputStream input = new FileInputStream(filePath);
//			filePath = "E:/fl/导入观测/log.txt";
//			List<String> list = new ArrayList<>();
//			list.add("dsfdsafkjllkjlkjlkjlkjlkj");
//			list.add("dsfdsafkjllkjlkjlkjlkjlkj");
//			list.add("dsfdsafkjllkjlkjlkjlkjlkj");
//			list.add("dsfdsafkjllkjlkjlkjlkjlkj");
//			list.add("dsfdsafkjllkjlkjlkjlkjlkj");
//			FileUtil.writeLines(list, filePath, "utf-8", true);
			
			Calendar calendar = Calendar.getInstance();
        	calendar.setTime(new Date());
        	String endTime = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATE_FORMAT);
        	calendar.add(Calendar.HOUR_OF_DAY, -24);
        	String startTime = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATE_FORMAT);
			
			System.out.println();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
