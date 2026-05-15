import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.druid.pool.DruidDataSource;
import com.constants.DecodeConstants;
import com.station.indb.util.DbUtils;
import com.util.NumberFormatUtil;
import com.util.TimeUtil;


public class TestCheck {
	
	public static void main(String[] args) {
//		String dataTime = "2025-10-21 08:00:00";
		String startDate = "2025-10-21 00:00:00";
		String endDate = "2025-10-22 12:00:00";
		int vti = 24;
//		String tableName = "public.nafp_ecmf_value_tab";
		String tableName = "public.deep_grib_rain_value_tab";
		
//		check(startDate, endDate, vti, tableName);
		
//		System.out.println(NumberFormatUtil.numFormat(1.5054113345521039, 2));
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
        endDate = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATE_FORMAT);
        calendar.add(Calendar.DAY_OF_MONTH, -16);
        startDate = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATE_FORMAT);
        String stationStr = "853094,42182,42299,42071,859710,899550,857001,857002,857015,857016,857017,857020,857022,857023,857026,857027,857040,857042,857045,857046,857047,857048,857050,857051,857052,857061,857650,857651,857802,857901,899270,899271,899296,899557,899558,899559,899560,899561,899562,899563,899565,899566,899567,899569,899572,899573,899574,899575,55323,55325,55122,55125,857000,857024,857025,857028,857029,857030,857031,857035,857036,857037,857038,857039,55228,55234,55026,55430,55437,852033,852037,852038,852039,852040,852043,852046,852049,52267,52378,52495,53602,56692,56697,57119,42105,42111,42189,57124,38353,41516,38954,38957,38878,42027,42056,852050,852051,852101,852052,852056,852057,852058,852059,852062,852063,852064,852065,852066,852067,852068,852069,852071,852072,852074,852075,852076,852077,852078,852079,852080,852081,852082,852083,852084,852085,852086,852088,852089,852091,852092,852097,852098,852107,852112,852113,852117,852119,852120,852132,852133,852134,852135,852136,852137,852138,852139,852147,852152,852165,852166,852167,852168,852182,852191,852211,852225,852226,852227,852229,57722,42398,42410,42314,852230,852231,852232,852233,852234,852235,852236,852237,852238,852239,852240,852241,852242,852087,852243,852244,852245,852262,852703,55664,55665,55666,55670,55675,55677,852200,852208,852209,852210,852212,852214,852215,852216,852217,852218,852030,852031,852032,852034,852035,852036,852000,852004,852060,852005,852006,852008,42273,42415,56434,851029,851033,851044,851045,851047,851048,851803,853032,853033,853046,853047,853048,853049,853050,853051,853052,853054,853055,853056,853057,853058,853059,853060,853066,853801,854000,854023,854025,854027,854029,854030,854031,854032,854035,854038,854039,854040,854041,854042,854043,854044,854045,854046,854047,854048,854050,854052,854053,854054,854055,854057,854060,854061,854063,854064,854065,854066,854067,854319,854703,854705,854706,855045,855046,855047,855048,855104,855126,855502,855503,855504,855505,855506,55593,55594,55595,55597,853074,853075,855007,855064,55792,853901,853902,853903,853904,55693,56301,56307,42369,42361,42463,42348,42181,42131,42170,831672,42101,42343,42452,42379,896165,831325,833806,835739,836161,838930,856031,856048,830421,42492,830207,41700,41718,42479,41598,41571,41573,41565,41863,41850,41859,41858,852024,42079,42498,42404,41535,41529,41675,41678,41640,41630,41624,41600,7129,56034,857071,55676,852002,852017,852018,852019,852025,852026,55686,55690,55132,55294,55359,55364,55491,56128,56223,56341,56342,56347,56187,56584,57615,56670,56671,56674,56675,57303,57304,56459,57306,57307,57308,57309,57314,57315,57317,57313,57318,56473,57320,51378,51166,52859,52910,52942,52889,55193,55196,57516,882177,856026,856102,55691,55694,55696,56227,56228,56314,56331,831003,831851,56096,42123,42165,42328,42339,42435,42516,57341,57622,53821,53707,57518,57519,57523,57525,57522,57535,57536,56479,57326,57328,56474,56475,57324,57329,56480,56485,56487,56380,56381,57237,56386,56387,56388,56389,56393,56394,56395,56396,56390,56391,56399,56186,56189,56190,56195,56196,56197,56198,56193,56194,56199,57403,57405,57401,57407,57408,57413,57414,57415,56569,57416,57411,56565,57417,57419,56571,56578,57420,56575,56580,56592,56593,57204,57206,57208,57216,57217,57600,57603,57608,57604,57605,56493,56494,56490,56491,56496,56498,56499,56272,56273,56276,56278,56279,56285,56286,56280,57348,57349,57345,57633,57425,57426,57437,57438,57338,57339,57333,51931,51747,51482,51053,51881,51452,51466,51806,51827,52633,52645,52654,52657,52856,52862,52863,52866,52868,52869,52870,52871,52874,52875,52876,52877,52878,56371,53706,53712,53716,57517,57520,851004,53603,53727,57409,57537,52881,56281,56287,56288,56289,56295,56296,56297,56290,56291,56298,51774,51777,51542,51783,51559,51323,51568,51567,51328,51329,51571,51573,51330,51572,51335,51334,51576,51337,51581,51342,51346,51345,51353,51352,51357,53614,56091,56092,56093,56094,56095,53704,52978,53829,52982,52983,52980,52981,52986,52984,52985,52988,52993,53723,52998,52995,52996,53609,52884,53610,52643,53611,53617,53618,53619,52652,52895,52656,52418,52417,52661,52424,53518,52427,53517,53881,52674,52795,52436,52679,52681,53935,51356,52203,51359,51358,52207,51377,51137,51379,51381,51388,51145,51149,51156,51159,52008,51158,51722,51724,52118,51288,51058,51059,51060,51067,51068,51076,51463,51462,51465,51811,51814,51815,51818,51817,51821,51823,51822,51825,52447,52446,56012,56173,56178,56180,56182,56183,56184,56185,52885,53612,53615,52896,52668,53519,52548,56357,52671,56441,56443,56455,56462,52675,51467,57612,53934,831543,51709,856106,853000,853007,853008,853009,853010,853013,853014,853015,853016,853017,853018,853019,853020,853021,853028,853029,853030,853031,854001,854004,854006,854007,854008,854009,854010,854011,854012,854013,854014,854015,854016,854018,854019,854021,857043,56136,51920,56137,56133,56370,56374,56373,56376,56378,56382,56383,56384,57502,57509,57505,57506,57512,57513,57514,57503,57507,57508,56665,56666,57510,57511,55357,55361,55375,852148,852149,852163,55564,55568,55569,55572,55574,55578,55579,55576,55587,853001,853003,853004,853005,853011,853012,56426,55585,55586,55589,55590,55591,55592,56202,56211,55142,55391,51362,52211,51365,51368,51367,52214,51369,51371,51372,51133,55398,56233,56235,56001,56004,56247,56478,55542,55554,53705,6110,51186,51187,51189,51760,51523,51765,51526,52851,52853,52854,52855,51702,55298,55299,56151,57001,57006,55773,55556,852803,852804,852805,55598,55599,56207,56209,56219,56237,851515,853068,853069,853071,853072,853073,51701,51703,57007,51705,57008,57002,51708,57004,57011,57012,51707,51711,51710,51715,51717,51716,51720,52814,57014,52816,53903,52818,53902,53908,53906,56144,56146,56147,56152,56158,56164,56167,56168,56171,56172,56385,51730,55447,53910,53914,51732,53915,53913,51084,899025,53916,51087,51991,52602,52824,52825,52829,53917,56192,53925,53926,52833,53923,53924,52836,53927,52838,52842,53928,53930,53937,56181,56188,57635,55248,55472,55482,55493,55497,56104,56106,55680,55681,55489,57402,51241,51243,55267,55279,51245,51005,55650,51246,51004,51008,51495,52101,56109,56113,56114,56116,52106,52112,57431,57432,52557,56125,51639,51643,51642,51644,51656,51655,51661,51663,51430,51431,51434,51433,51436,51435,51438,51437,51440,53817,51886,51894,51896,51899,52737,52745,52754,52765,52972,52974,56097,53818,52515,52532,52530,55652,55655,55656,30,56308,56309,56312,56315,56317,56319,56325,56326,55499,52533,52784,52546,52787,51469,52313,51468,51470,51232,55178,51477,51238,51803,51802,51805,51804,51810,52905,57105,57102,57110,57111,52797,52319,52906,52908,52911,52323,56013,56015,56016,56018,56021,56028,56029,56033,56038,56251,56257,56263,56267,56492,51826,51829,51828,51839,51844,51855,51858,52931,52941,52943,55195,56043,56045,56046,56065,56067,56284,852001,854801,854802,854803,855707,9168,51862,51627,51629,51628,56074,56071,53801,51631,53806,56080,51633,56081,56082,56084,53811,53810,53814,51636,52707,52713,52714,52953,52955,52957,52958,52963,52968,56077,895570,894058,56079,899582,857063,852009,852010,852011,852013,852014,852015,852016,852250,852255,852256,852257,852258,852259,852260";
        String sql = null; 
        int[] vtis = new int[]{24, 48, 72};
        String[] stations = stationStr.split(",");
        double mae = 0;
        double total = 0;
        int count = 0;
        for(String station : stations)
        {
        	sql = "select station,datatime,datasource,atmae24,atmae48,atmae72 from public.station_check_value_tab where datatime >='" + 
 				   startDate + "' and datatime <= '" + endDate + "' and station = '" + station + "' and datasource = 'fst'"; 
        	mae = querytLatestMae("fst", "at", "mae", vtis, sql, station);
//        	if("852031,852035,852060,852065,852076,852089,853055,854029,854042,857016,857063,899557".contains(station))
//        	{
//        		continue;
//        	}
        	if(mae > 2.2 && station.startsWith("8"))
        	{
        		System.out.println(station + ": " + mae);
        	}
        	else
        	{
        		total += mae;
        		count++;
        	}
        }
		System.out.println("avg mae: " + (total / count));
	}
	
	public static double querytLatestMae(String dataSource, String element, String method, int[] vtis, String sql, String station)
    {
    	double resultD = 0;
    	DruidDataSource dataSourceJDBC = DbUtils.getInstance().getDataSource();
    	
    	String checkWzdStationSql = "select * from public.station_info_tab_zj where station_id_d = '" + station + "'";
    	try (Connection conn = dataSourceJDBC.getConnection();
				Statement st = conn.createStatement();
				ResultSet rs = st.executeQuery(checkWzdStationSql);){
			while(rs.next())
			{
				return -1;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	String checkUploadStationSql = "select * from public.station_info_tab_upload where station_id_d = '" + station + "'";
    	try (Connection conn = dataSourceJDBC.getConnection();
				Statement st = conn.createStatement();
				ResultSet rs = st.executeQuery(checkUploadStationSql);){
			while(rs.next())
			{
				return -1;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	Map<String, Map<String, Map<String, Map<String, Double>>>> result = new HashMap<>();
    	
		Map<String, List<List<Double>>> rsMap = new HashMap<>();
		Map<String, double[]> rsResultMap = new HashMap<>();
		List<List<Double>> listss = new ArrayList<>();
		int vtisCount = vtis.length;
		for(int i = 0; i < vtisCount; i++)
		{
			listss.add(new ArrayList<>());
		}
		rsMap.put(dataSource, listss);
		rsResultMap.put(dataSource, new double[vtisCount]);
		result.put(dataSource, new HashMap<>());
		result.get(dataSource).put(element, new HashMap<>());
		result.get(dataSource).get(element).put(method, new LinkedHashMap<>());
		try (Connection conn = dataSourceJDBC.getConnection();
				Statement st = conn.createStatement();
				ResultSet rs = st.executeQuery(sql);){
			while(rs.next())
			{
				for(int i = 4; i <= 6; i++)
				{
					if(rs.getDouble(i) != DecodeConstants.UNDEF_DOUBLE_VALUE)
					{
						rsMap.get(rs.getString(3)).get(i - 4).add(rs.getDouble(i));
					}
				}
			}
			int[] scaleNum = new int[2];
			if(element.equals("atmax") || element.equals("atmin"))
			{
				scaleNum[0] = 100;
				scaleNum[1] = 1;
			}
			else if(element.equals("at"))
			{
				scaleNum[0] = 1;
				scaleNum[1] = 2;
			}
			else if(element.equals("ws"))
			{
				scaleNum[0] = 1;
				scaleNum[1] = 1;
			}
			else
			{
				scaleNum[0] = 1;
				scaleNum[1] = 1;
			}
			
			List<List<Double>> lists = rsMap.get(dataSource);
			for(int i = 0; i < vtisCount; i++)
			{
				List<Double> list = lists.get(i);
				double sum = 0;
				for(double value : list)
				{
					sum += value;
				}
				if(list.size() == 0)
				{
					rsResultMap.get(dataSource)[i] = DecodeConstants.UNDEF_DOUBLE_VALUE;
				}
				else
				{
					rsResultMap.get(dataSource)[i] = NumberFormatUtil.numFormat(scaleNum[0] * sum / list.size(), scaleNum[1]);
				}
			}
			
			
			for(int i = 0; i < vtisCount; i++)
			{
				result.get(dataSource).get(element).get(method).put(vtis[i] + "", rsResultMap.get(dataSource)[i] != DecodeConstants.UNDEF_DOUBLE_VALUE ? rsResultMap.get(dataSource)[i] : null);
			}
			if(element.equals("at") || element.equals("ws"))
			{
				Double v24 = result.get(dataSource).get(element).get(method).get("24");
				if(v24 == null)
				{
					resultD = -1;
				}
				else
				{
					Double v48 = result.get(dataSource).get(element).get(method).get("48");
					Double v72 = result.get(dataSource).get(element).get(method).get("72");
					if(v48 == null || v72 == null)
					{
						return -1;
					}
					result.get(dataSource).get(element).get(method).put("0-72", NumberFormatUtil.numFormat(v24 * 0.5 + v48 * 0.333 + v72 * 0.167, 2));
					resultD = NumberFormatUtil.numFormat(v24 * 0.5 + v48 * 0.333 + v72 * 0.167, 2);
				}
				
			}
    	
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	
    	return resultD;
    }
	
	public static void check(String startDate, String endDate, int vti, String tableName)
	{
		DruidDataSource dataSource = DbUtils.getInstance().getDataSource();
		
		String sql = "select station, datatime, validdate, rain, vti from " + tableName + " " + 
//			         "public.nafp_ecmf_value_tab " +
			         "where datatime = '" + "2025-10-21 08:00:00" + "' and vti in " + "(0, 24)" + " and validdate >= '" + startDate + "' and validdate <= '" + endDate + "' ";
//					 "where vti = " + vti + " and validdate >= '" + startDate + "' and validdate <= '" + endDate + "' ";
	    System.out.println(sql);
		Map<String, Double> queryDataMap = new HashMap<>();
		try(Connection conn = dataSource.getConnection();
		     Statement st = conn.createStatement();
		     ResultSet rs = st.executeQuery(sql);) {
		     while (rs.next()) {
		    	 queryDataMap.put(rs.getString(1) + "_" + rs.getString(3) + "_" + rs.getInt(5), rs.getDouble(4));
		     }
	     
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Map<String, Double> fstDataMap = new HashMap<>();
		Map<String, Double> fstDataMap12 = new HashMap<>();
		
		for(String key : queryDataMap.keySet())
		{
			String[] split = key.split("_");
			if("24".equals(split[2]))
			{
				fstDataMap.put(key, queryDataMap.get(key));
			}
		}
		
		for(String key : queryDataMap.keySet())
		{
			String[] split = key.split("_");
			if("0".equals(split[2]))
			{
				fstDataMap12.put(key, queryDataMap.get(key));
			}
		}
		
		
		startDate = TimeUtil.addHours(startDate, -8);
		endDate = TimeUtil.addHours(endDate, -8);
		sql = "select station_id_d, datetime, pre_24h from public.surf_chn_mul_hor_micaps where datetime >= '" + startDate + "' and datetime <= '" + endDate + "' ";
		Map<String, Double> obsDataMap = new HashMap<>();
		try(Connection conn = dataSource.getConnection();
		     Statement st = conn.createStatement();
		     ResultSet rs = st.executeQuery(sql);) {
		     while (rs.next()) {
		    	 obsDataMap.put(rs.getString(1) + "_" + TimeUtil.addHours(rs.getString(2), 8), rs.getDouble(3));
		     }
	     
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		double h = 0;
        double tn = 0;
        double f = 0;
        double m = 0;
		Map<String, double[]> result = new HashMap<>();
		for(String key : fstDataMap.keySet())
		{
			String[] split = key.split("_");
			String station = split[0];
			String date = split[1];
			date = TimeUtil.addHours(date, -24);
//			String vtiStr = split[2];
//			if(!result.containsKey(station))
//			{
//				result.put(station, new double[4]);
//			}
			Double obs = obsDataMap.get(station + "_" + split[1]);
			if(queryDataMap.get(station + "_" + date + "_0") == null)
			{
				continue;
			}
			Double fst = fstDataMap.get(key) - fstDataMap12.get(station + "_" + date + "_0");
			
//			fst = fst * 1000;
			if(obs == null || fst == null)
			{
				continue;
			}
			if(obs >= 0.1 && fst >= 0.1)
            {
				System.out.println(key + " = " + obs + " : " + fst);
                h++;
            }
            else if(obs < 0.1 && fst < 0.1)
            {
                tn++;
            }
            else if(obs >= 0.1 && fst < 0.1)
            {
                m++;
            }
            else if(obs < 0.1 && fst >= 0.1)
            {
                f++;
            }
		}
		
		System.out.println("h:" + h + ",tn:" + tn + ",m:" + m + ",f:" + f);
	}
}
