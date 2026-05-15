import java.util.HashMap;
import java.util.Map;

import com.station.indb.DataIndbWork;


public class TestCldas {
	public static void main(String[] args) {
		String filePath = "E:/fl/datas/swc9km/SWCWARMS_20250814000000_F49_P10.grb";
		String dataType = "swc9km";
		String tableName = "1111";
		Map<String, double[]> stationInfo = new HashMap<>();
		stationInfo.put("831543", new double[]{85.747093,30.636427});
		DataIndbWork.indb(filePath, dataType, tableName, stationInfo);
	}
}
