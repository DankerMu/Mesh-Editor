import com.alibaba.fastjson.JSONObject;
import com.constants.DataTypeEnum;
import com.station.indb.DataIndbWork;
import com.util.ReadPropertiesUtil;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @category
 * @date 2025/5/21 10:11
 * @description TODO
 */
public class DataIndbTest {
    public static void main(String[] args) {
        String tableName = "nafp_ecmf_tab";
        String filePath = "E:\\fl\\datas\\ecmwf/W_NAFP_C_ECMF_20250523171444_P_C1D05231200052315001.grib";
//        EcmwfDataIndbStrategy ecmwfDataIndbStrategy = new EcmwfDataIndbStrategy();
//        Map<String, List<JSONObject>> mapDataIndbList = ecmwfDataIndbStrategy.getMapDataIndbList(filePath, tableName);
//        System.out.println(mapDataIndbList);

        tableName = "nafp_t1279_tab";
        filePath = "E:\\fl\\datas\\t1279_temp/zz_mwf_global_lc_999_202505160800.024";
//        T1279DataIndbStrategy t1279DataIndbStrategy = new T1279DataIndbStrategy();
//        Map<String, List<JSONObject>> mapDataIndbList = t1279DataIndbStrategy.getMapDataIndbList(filePath, tableName);

        tableName = "nafp_swc9km_tab";
        filePath = "E:\\fl\\datas\\swc9km/SWCWARMS_20250218000000_F08_P10.grb";

//        tableName = "nafp_grapes_tab";
//        filePath = "E:\\fl\\datas\\grapes/Z_NAFP_C_BABJ_20230604120000_P_NWPC-GRAPES-GFS-HNEHE-02700.grib2";

        tableName = "surf_obs_tab111111111111111";
        filePath = "E:\\fl\\datas\\mdfs/20250518040000.000.csv";

        tableName = "surf_obs_tab111111111111111";
        filePath = "E:\\fl\\datas\\station/990004-2025082920.csv";
        
//        tableName = "deep_grib_rain_tab";
//        filePath = "E:\\fl\\datas\\deep/tp_999_deeplearning_2025060108_027.txt";

//        tableName = "surf_obs_tab";
//        filePath = "E:\\fl\\datas\\micaps/20250707000000.000.csv";
//        
//        tableName = "surf_obs_tab111111111111111";
//        filePath = "E:/fl/datas/rain_png/deeplearning/20250812/tp_999_deeplearning_2025081208_024.txt";
//        
//        tableName = "surf_obs_tab111111111111111";
//        filePath = "E:\\fl\\datas\\micaps/all_stations_202407_202409_bjt_for_pg_9999.csv";
//        
//        tableName = "surf_obs_tab111111111111111";
//        filePath = "E:\\fl\\datas\\kt1279/KTVVA2025071712700026.grb";

//        tableName = "surf_obs_tab111111111";
//        filePath = "E:\\fl\\datas\\fy4b/20250625230000.000.csv";
        
//        tableName = "surf_obs_tab111111111";
//        filePath = "E:\\fl\\datas\\swc9km/SWCWARMS_20250805120000_F00_P10.grb";
        
//        tableName = "surf_obs_tab111111111";
//        filePath = "E:\\fl\\datas\\cldas/Z_NAFP_C_BABJ_20250104180838_P_CLDAS_RT_CHN_0P05_HOR-WIN-2025010418.GRB2";
//        
//        tableName = "surf_obs_tab111111111";
//        filePath = "E:\\fl\\datas\\micaps/20251115150000.000.csv";
        
//        tableName = "surf_obs_tab111111111";
//        filePath = "E:\\fl\\datas\\ecmwf/W_NAFP_C_ECMF_20250721174053_P_C1D07211200072712001.grib";

//        DataIndbWork.indb(filePath, DataTypeEnum.MICAPS.getDataType(), tableName, null);
        
        tableName = "surf_obs_tab111111111";
        filePath = "E:\\fl\\datas\\deep/";
        File file = new File(filePath);
        File[] files = file.listFiles();
        for(File f : files)
        {
        	if(f.getAbsolutePath().endsWith("2025112720_072.txt"))
        	{
        	}
        	DataIndbWork.indb(f.getAbsolutePath(), DataTypeEnum.DEEP.getDataType() + "_" + DataTypeEnum.RAIN.getDataType(), tableName, null);
        }

        System.out.println("ok");
    }
}
