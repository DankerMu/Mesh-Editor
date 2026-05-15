package com.util;

import cn.hutool.core.io.FileUtil;

import com.constants.DataTypeEnum;

import java.io.File;
import java.io.FileFilter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class GribUtil {
	
	public static void main(String[] args) {
		String filePath = "Z_NAFP_C_BABJ_20251025000000_P_NWPC-GRAPES-GFS-HNEHE-00600.grib2";
		String dataType = "grapes";
		getVtiDataTime(filePath, dataType);
	}

    public static int[] createStationVti(String dataType, int vti)
    {
        int[] result = null;
        if(dataType.equals(DataTypeEnum.ECMF.getDataType()))
        {
            if(vti == 0)
            {
                result = new int[1];
                result[0] = vti;
            }
            else if(vti > 0 && vti <= 3)
            {
                result = new int[4];
                result[0] = vti - 3;
                result[1] = vti - 2;
                result[2] = vti - 1;
                result[3] = vti;
            }
            else if(vti > 3 && vti <= 36)
            {
                result = new int[3];
                result[0] = vti - 2;
                result[1] = vti - 1;
                result[2] = vti;
            }
            else if(vti > 36 && vti <= 72)
            {
                result = new int[1];
                result[0] = vti;
            }
            else if(vti > 72 && vti <= 120)
            {
                result = new int[2];
                result[0] = vti - 3;
                result[1] = vti;
            }
            else if(vti > 120 && vti <= 240)
            {
                result = new int[2];
                result[0] = vti - 6;
                result[1] = vti;
            }
        }
        else if(dataType.equals(DataTypeEnum.GRAPES.getDataType()))
        {
            if(vti == 0)
            {
                result = new int[1];
                result[0] = vti;
            }
            else if(vti <= 3)
            {
                result = new int[4];
                result[0] = vti - 3;
                result[1] = vti - 2;
                result[2] = vti - 1;
                result[3] = vti;
            }
            else if(vti > 3 && vti <= 36)
            {
                result = new int[3];
                result[0] = vti - 2;
                result[1] = vti - 1;
                result[2] = vti;
            }
            else if(vti > 36 && vti <= 168)
            {
                result = new int[1];
                result[0] = vti;
            }
            else if(vti > 168 && vti <= 240)
            {
                result = new int[2];
                result[0] = vti - 6;
                result[1] = vti;
            }
        }
        else if(dataType.equals(DataTypeEnum.T1279.getDataType()))
        {
            if(vti == 0)
            {
                result = new int[1];
                result[0] = vti;
            }
            else if(vti <= 3)
            {
                result = new int[4];
                result[0] = vti - 3;
                result[1] = vti - 2;
                result[2] = vti - 1;
                result[3] = vti;
            }
            else if(vti > 3 && vti <= 36)
            {
                result = new int[3];
                result[0] = vti - 2;
                result[1] = vti - 1;
                result[2] = vti;
            }
            else if(vti > 36 && vti <= 72)
            {
                result = new int[1];
                result[0] = vti;
            }
            else if(vti > 72 && vti <= 120)
            {
                result = new int[2];
                result[0] = vti - 3;
                result[1] = vti;
            }
            else if(vti > 120 && vti <= 240)
            {
                result = new int[2];
                result[0] = vti - 6;
                result[1] = vti;
            }
        }
        else if(dataType.equals(DataTypeEnum.SWC9KM.getDataType()))
        {
            if(vti == 1)
            {
                result = new int[2];
                result[0] = 0;
                result[1] = vti;
            }
            else
            {
                result = new int[1];
                result[0] = vti;
            }
        }
        else if(dataType.equals(DataTypeEnum.SWC3KM.getDataType()))
        {
            if(vti == 1)
            {
                result = new int[2];
                result[0] = 0;
                result[1] = vti;
            }
            else
            {
                result = new int[1];
                result[0] = vti;
            }
        }

        return result;
    }

    public static int[] createZoneVti(String dataType, int vti)
    {
        int[] result = null;
        if(dataType.equals(DataTypeEnum.ECMF.getDataType()))
        {
            if(vti <= 3)
            {
                result = new int[2];
                result[0] = vti - 3;
                result[1] = vti;
            }
            else if(vti > 3 && vti <= 240)
            {
                result = new int[1];
                result[0] = vti;
            }
        }
        else if(dataType.equals(DataTypeEnum.GRAPES.getDataType()))
        {
            if(vti <= 3)
            {
                result = new int[2];
                result[0] = vti - 3;
                result[1] = vti;

            }
            else if(vti > 3 && vti <= 168)
            {
                result = new int[1];
                result[0] = vti;
            }
            else if(vti > 168 && vti <= 240)
            {
                result = new int[2];
                result[0] = vti - 6;
                result[1] = vti;
            }
        }

        return result;
    }

    public static String[] getVtiDataTime(String filePath, String dataType) {
        String[] result = new String[2];
        String prefix = FileUtil.getPrefix(filePath);
        if(dataType.equals(DataTypeEnum.ECMF.getDataType()))
        {
//        W_NAFP_C_ECMF_20240922055055_P_D1D09220000092209001
            String[] splitPrefix = prefix.split("_");
            String dataTime = splitPrefix[4].substring(0, 4) + splitPrefix[6].substring(3, 9);
            String t1 = splitPrefix[6].substring(3, 9);
            String t2 = splitPrefix[6].substring(11, 17);
            int vti = TimeUtil.getTimeDis(t1, t2);
            result[0] = vti + "";
            result[1] = dataTime;
        }
        else if(dataType.equals(DataTypeEnum.GRAPES.getDataType()))
        {
//        	Z_NAFP_C_BABJ_20251025000000_P_NWPC-GRAPES-GFS-HNEHE-00600.grib2
//          Z_NAFP_C_BABJ_20230418180000_P_NWPC-GRAPES-GFS-GLB-11700.grib2
//          Z_NWGD_C_BABJ_20211008160825_P_RFFC_SCMOC-PPH-6H_202110082000_24006.GRB2
//        	System.out.println("grapes: " + filePath);
            String[] split = prefix.split("_");
            String dataTime = split[4].substring(0, 10);
            String[] split1 = prefix.split("-");
            String vti = split1[4].substring(0, 3);
            result[0] = vti;
            result[1] = dataTime;
        }
        else if(dataType.equals(DataTypeEnum.T1279.getDataType()))
        {
            //        zz_mwf_global_t2_999_202502110800.000
            String[] split = prefix.split("_");
            String dataTime = split[5].substring(0, 10);
            String vti = FileUtil.getSuffix(filePath);
            result[0] = vti;
            result[1] = dataTime;
        }
        else if(dataType.equals(DataTypeEnum.KT.getDataType()))
        {
//            KTRHA2025021700010051.grb
            String dataTime = prefix.substring(5, 15);
            String vti = prefix.substring(18, 21);
            result[0] = vti;
            result[1] = dataTime;
        }
        else if(dataType.equals(DataTypeEnum.SWC9KM.getDataType()))
        {
//            SWCWARMS_20250218000000_F58_P10.grb
            String[] split = prefix.split("_");
            String dataTime = split[1].substring(0, 10);
            String vti = split[2].substring(1, 3);
            result[0] = vti;
            result[1] = dataTime;
        }
        else if(dataType.equals(DataTypeEnum.SWC3KM.getDataType()))
        {
//            SWCWINGS_20221001000000_F02_3KM.grb2
            String[] split = prefix.split("_");
            String dataTime = split[1].substring(0, 10);
            String vti = split[2].substring(1, 3);
            result[0] = vti;
            result[1] = dataTime;
        }
        else if(dataType.equals(DataTypeEnum.SCMOC.getDataType()))
        {
//            Z_NWGD_C_BABJ_20241220185556_P_RFFC_SCMOC-PPH_202412202000_24003.GRB2
            String[] split = prefix.split("_");
            String dataTime = split[8];
            result[0] = 3 + "";
            result[1] = dataTime;
        }
        else if(dataType.equals(DataTypeEnum.CLDAS.getDataType()))
        {
//            Z_NAFP_C_BABJ_20250219070900_P_CLDAS_NRT_ASI_0P0625_HOR-TMP-2025021707.nc
            String[] split = prefix.split("-");
            String dataTime = split[2];
            result[0] = 1 + "";
            result[1] = dataTime;
        }
        else if(dataType.equals(DataTypeEnum.CMPA.getDataType()))
        {
//            Z_SURF_C_BABJ_20241201000944_P_CMPA_FRT_CHN_0P05_HOR-PRE-2024120100.GRB2
            String[] split = prefix.split("-");
            String dataTime = split[2];
            result[0] = 1 + "";
            result[1] = dataTime;
        }
        else if(dataType.equals(DataTypeEnum.RAIN.getDataType()))
        {
//            pr_999_ecmwf_2025032020_240.txt
            String[] split = prefix.split("_");
            String dataTime = split[3];
            result[0] = split[4];
            result[1] = dataTime;
        }

        return result;
    }

    public static double[] getCzVtiStation(String dataType, int dis_vti, int vti)
    {
        double[] result = null;
        if(dataType.equals(DataTypeEnum.ECMF.getDataType()))
        {
            if(vti > 0 && vti <= 36)
            {
                result = new double[2];
                result[0] = 1 / 3.0;
                result[1] = 2 / 3.0;
            }
            else if(vti > 72 && vti <= 240)
            {
                result = new double[1];
                result[0] = 0.5;
            }
        }
        else if(dataType.equals(DataTypeEnum.T1279.getDataType()))
        {
            if(vti > 0 && vti <= 36)
            {
                result = new double[2];
                result[0] = 1 / 3.0;
                result[1] = 2 / 3.0;
            }
            else if(vti > 72 && vti <= 240)
            {
                result = new double[1];
                result[0] = 0.5;
            }
        }
        else if(dataType.equals(DataTypeEnum.GRAPES.getDataType()))
        {
            if(vti > 0 && vti <= 36)
            {
                result = new double[2];
                result[0] = 1 / 3.0;
                result[1] = 2 / 3.0;
            }
            else if(vti > 168 && vti <= 240)
            {
                result = new double[1];
                result[0] = 0.5;
            }
        }

        return result;
    }

    public static double[] getCzVtiZone(String dataType, int dis_vti, int vti)
    {
        double[] result = null;
        if(dataType.equals(DataTypeEnum.ECMF.getDataType()))
        {
            //不需要时间插值
        }
        else if(dataType.equals(DataTypeEnum.T1279.getDataType()))
        {
            //不需要时间插值
        }
        else if(dataType.equals(DataTypeEnum.SCMOC.getDataType()))
        {
            //不需要时间插值
        }
        else if(dataType.equals(DataTypeEnum.KT.getDataType()))
        {
            if(vti > 120 && vti <= 240)
            {
                result = new double[3];
                result[0] = 0.25;
                result[1] = 0.5;
                result[2] = 0.75;
            }
        }
        else if(dataType.equals(DataTypeEnum.GRAPES.getDataType()))
        {
            if(vti >= 168 && vti <= 240)
            {
                result = new double[1];
                result[0] = 0.5;
            }
        }

        return result;
    }

    public static int[] getDisVtiAndVtiStation(String filePath, String dataType)
    {
        String fileName = FileUtil.getPrefix(filePath);
        int[] result = new int[2];
        if(dataType.equals(DataTypeEnum.ECMF.getDataType()))
        {
            String[] split = fileName.split("_");
            String t1 = split[6].substring(3, 9);
            String t2 = split[6].substring(11, 17);
            int vti = TimeUtil.getTimeDis(t1, t2);
            if(vti == 0)
            {
                result[0] = 0;
            }
            else if(vti > 0 && vti <= 72)
            {
                result[0] = 3;
            }
            else
            {
                result[0] = 6;
            }
            result[1] = vti;
        }
        else if(dataType.equals(DataTypeEnum.T1279.getDataType()))
        {
            String suffix = FileUtil.getSuffix(filePath);
            int vti = Integer.parseInt(suffix);
            int dis_vti = 0;
            if(vti > 0 && vti <= 120)
            {
                dis_vti = 3;
            }
            else if(vti > 120 && vti <= 168)
            {
                dis_vti = 6;
            }
            else if(vti > 168 && vti <= 240)
            {
                dis_vti = 12;
            }
            result[0] = dis_vti;
            result[1] = vti;
        }
        else if(dataType.equals(DataTypeEnum.GRAPES.getDataType()))
        {
//            Z_NAFP_C_BABJ_20230418180000_P_NWPC-GRAPES-GFS-GLB-11700.grib2
            String prefix = FileUtil.getPrefix(filePath);
            String[] split = prefix.split("-");
            int vti = Integer.parseInt(split[4].substring(0, 3));
            int dis_vti = 0;
            if(vti > 0 && vti <= 120)
            {
                dis_vti = 3;
            }
            else if(vti > 120 && vti <= 240)
            {
                dis_vti = 12;
            }
            result[0] = dis_vti;
            result[1] = vti;
        }
        else if(dataType.equals(DataTypeEnum.KT.getDataType()))
        {
//            KTRHA2025021700000008.grb
            String prefix = FileUtil.getPrefix(filePath);
            String[] split = prefix.split("-");
            int vti = Integer.parseInt(prefix.substring(18, 21));
            int dis_vti = 0;
            if(vti > 0 && vti <= 72)
            {
                dis_vti = 1;
            }
            else if(vti > 72 && vti <= 120)
            {
                dis_vti = 6;
            }
            else if(vti > 120 && vti <= 240)
            {
                dis_vti = 24;
            }
            result[0] = dis_vti;
            result[1] = vti;
        }
        else if(dataType.equals(DataTypeEnum.SWC9KM.getDataType()))
        {
//            SWCWARMS_20250218000000_F01_P10.grb
            String prefix = FileUtil.getPrefix(filePath);
            String[] split = prefix.split("_");
            int vti = Integer.parseInt(split[2].substring(1, 3));
            result[0] = 1;
            result[1] = vti;
        }
        else if(dataType.equals(DataTypeEnum.SWC3KM.getDataType()))
        {
//            SWCWINGS_20221001000000_F02_3KM.grb2
            String prefix = FileUtil.getPrefix(filePath);
            String[] split = prefix.split("_");
            int vti = Integer.parseInt(split[2].substring(1, 3));
            result[0] = 1;
            result[1] = vti;
        }

        return result;
    }

    public static int[] getDisVtiAndVtiZone(String filePath, String dataType)
    {
        String fileName = FileUtil.getPrefix(filePath);
        int[] result = new int[2];
        if(dataType.equals(DataTypeEnum.ECMF.getDataType()))
        {
            String[] split = fileName.split("_");
            String t1 = split[6].substring(3, 9);
            String t2 = split[6].substring(11, 17);
            int vti = TimeUtil.getTimeDis(t1, t2);
            if(vti > 0 && vti <= 72)
            {
                result[0] = 3;
            }
            else
            {
                result[0] = 6;
            }
            result[1] = vti;
        }
        else if(dataType.equals(DataTypeEnum.T1279.getDataType()))
        {
            String suffix = FileUtil.getSuffix(filePath);
            int vti = Integer.parseInt(suffix);
            int dis_vti = 0;
            if(vti > 0 && vti <= 72)
            {
                dis_vti = 3;
            }
            else if(vti > 72 && vti <= 120)
            {
                dis_vti = 6;
            }
            else if(vti > 120 && vti <= 240)
            {
                dis_vti = 12;
            }
            result[0] = dis_vti;
            result[1] = vti;
        }

        return result;
    }

    public static String createPrefFilePath(String filePath, int vti)
    {
        String result = "";
        String dataType = DataTypeUtil.getDataType(filePath);
        if(dataType.equals(DataTypeEnum.ECMF.getDataType()))
        {
            result = createEcmfFilePath(filePath, vti);
        }
        else if(dataType.equals(DataTypeEnum.T1279.getDataType()))
        {

        }
        else if(dataType.equals(DataTypeEnum.GRAPES.getDataType()))
        {
            result = createGrapesGfsFilePath(filePath, vti);
        }
        else if(dataType.equals(DataTypeEnum.KT.getDataType()))
        {
            result = createKtFilePath(filePath, vti);
        }
        else if(dataType.equals(DataTypeEnum.SWC9KM.getDataType()))
        {
            result = createSwc9kmFilePath(filePath, vti);
        }
        else if(dataType.equals(DataTypeEnum.SWC3KM.getDataType()))
        {
            result = createSwc3kmFilePath(filePath, vti);
        }

        return result;
    }

    private static String createEcmfFilePath(String filePath, int dis_vti)
    {
//		W_NAFP_C_ECMF_20221106054248_P_C1D11060000110600001.grib
//		W_NAFP_C_ECMF_20240922054248_P_D1D09220000092200001
        String result = "";
        String fileName = FileUtil.getPrefix(filePath);
        String[] split = fileName.split("_");
        String t2 = split[6].substring(11, 17);
        String fileTime = split[4].substring(0, 4) + t2;
        Date date = TimeUtil.String2Date(fileTime, "yyyyMMddHH");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR_OF_DAY, -dis_vti);
        String dateStr = TimeUtil.date2String(calendar.getTime(), "yyyyMMddHH");
        dateStr = dateStr.substring(4);
//		dateStr = dateStr.substring(0, 4) + String.format("%03d", Integer.parseInt(dateStr.substring(4)));
        String regex = fileName.substring(0, 22) + "\\d{6}" + fileName.substring(28, 42) + dateStr + fileName.substring(fileName.length() - 3) + ".grib";

        String[] splitTT = regex.split("_");
        String tt0 = splitTT[6].substring(3, 9);
        String tt1 = splitTT[6].substring(11, 17);
        if(regex.endsWith("001.grib") && tt0.equals(tt1))
        {
            regex = regex.replace("001.", "011.");
        }
        String regexValue = regex;
        String path = FileUtil.getParent(filePath, 1);
        File file = new File(path);
        File[] files = file.listFiles(new FileFilter() {

            @Override
            public boolean accept(File ff) {
                String name = ff.getName();

                return name.matches(regexValue);
            }
        });
        if(files.length > 0)
        {
            result = files[0].getAbsolutePath();

        }

        return result;
    }

    private static String createGrapesGfsFilePath(String filePath, int dis_vti)
    {
//        Z_NAFP_C_BABJ_20230418180000_P_NWPC-GRAPES-GFS-GLB-11700.grib2
        String result = "";
        String prefix = FileUtil.getPrefix(filePath);
        String[] split = prefix.split("-");
        Integer vti = Integer.parseInt(split[4].substring(0, 3));
        result = filePath.replace(prefix.split("-")[4], String.format("%03d", vti - dis_vti) + "00");

        return result;
    }

    private static String createKtFilePath(String filePath, int dis_vti)
    {
//        KTRHA2025021700010060.grb
        String result = "";
        String prefix = FileUtil.getPrefix(filePath);
        String vitStr = prefix.substring(18, 21);
        Integer vti = Integer.parseInt(vitStr);
        result = filePath.replace(vitStr + ".grb", String.format("%03d", vti - dis_vti) + ".grb");

        return result;
    }

    private static String createSwc9kmFilePath(String filePath, int dis_vti)
    {
//        SWCWARMS_20250218000000_F01_P10.grb
        String result = "";
        String prefix = FileUtil.getPrefix(filePath);
        String[] split = prefix.split("_");
        String vitStr = split[2];
        Integer vti = Integer.parseInt(split[2].substring(1, 3));
        result = filePath.replace("_" + vitStr + "_", "_F" + String.format("%02d", vti - dis_vti) + "_");

        return result;
    }

    private static String createSwc3kmFilePath(String filePath, int dis_vti)
    {
//        SWCWARMS_20250218000000_F01_P10.grb
        String result = "";
        String prefix = FileUtil.getPrefix(filePath);
        String[] split = prefix.split("_");
        String vitStr = split[2];
        Integer vti = Integer.parseInt(split[2].substring(1, 3));
        result = filePath.replace("_" + vitStr + "_", "_F" + String.format("%02d", vti - dis_vti) + "_");

        return result;
    }

    public static String getCsvFileName(String fileName, String staion)
    {
        String result = "";
        String dataType = DataTypeUtil.getDataType(fileName);
        if(dataType.equals(DataTypeEnum.ECMF.getDataType()))
        {
            result = getEcmfCsvFileName(fileName, staion);
        }
        else if(dataType.equals(DataTypeEnum.T1279.getDataType()))
        {
            result = getT1279CsvFileName(fileName, staion);
        }
        else if(dataType.equals(DataTypeEnum.GRAPES.getDataType()))
        {
            result = getGrapesGfsCsvFileName(fileName, staion);
        }
        else if(dataType.equals(DataTypeEnum.SWC9KM.getDataType()))
        {
            result = getSwc9kmCsvFileName(fileName, staion);
        }
        else if(dataType.equals(DataTypeEnum.SWC3KM.getDataType()))
        {
            result = getSwc3kmCsvFileName(fileName, staion);
        }

        return result;
    }

    private static String getEcmfCsvFileName(String fileName, String staion)
    {
        String result = "";
        String prefix = FileUtil.getPrefix(fileName);
        String[] split = prefix.split("_");
        String mmddhh = split[6].substring(3, 7) + (split[6].substring(7, 9).equals("00") ? "08" : "20");
        String dataTime = split[4].substring(0, 4) + mmddhh;
        result = staion + "-" + DataTypeEnum.ECMWF.getDataType() + "-" + dataTime + ".csv";

        return result;
    }

    private static String getGrapesGfsCsvFileName(String fileName, String staion)
    {
//        Z_NAFP_C_BABJ_20230418180000_P_NWPC-GRAPES-GFS-GLB-11400.grib2
        String result = "";
        String prefix = FileUtil.getPrefix(fileName);
        String[] split = prefix.split("_");
        String dataTime = split[4].substring(0, 10);
        Date date = TimeUtil.dateTimeStr2date(dataTime, TimeUtil.DATE_FMT_YMDH, TimeUtil.DEFAULT_DATETIME_FORMAT);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR_OF_DAY, 8);
        String dataTimeStr = TimeUtil.date2String(calendar.getTime(), "yyyyMMddHH");
        result = staion + "-" + DataTypeEnum.GRAPES.getDataType() + "-" + dataTimeStr + ".csv";

        return result;
    }
    private static String getSwc9kmCsvFileName(String fileName, String staion)
    {
//        SWCWARMS_20250218000000_F28_P10.grb
        String result = "";
        String prefix = FileUtil.getPrefix(fileName);
        String[] split = prefix.split("_");
        String dataTime = split[1].substring(0, 10);
        Date date = TimeUtil.dateTimeStr2date(dataTime, TimeUtil.DATE_FMT_YMDH, TimeUtil.DEFAULT_DATETIME_FORMAT);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR_OF_DAY, 8);
        String dataTimeStr = TimeUtil.date2String(calendar.getTime(), "yyyyMMddHH");
        result = staion + "-" + DataTypeEnum.SWC9KM.getDataType() + "-" + dataTimeStr + ".csv";

        return result;
    }

    private static String getSwc3kmCsvFileName(String fileName, String staion)
    {
//        SWCWINGS_20221001000000_F06_3KM.grb2
        String result = "";
        String prefix = FileUtil.getPrefix(fileName);
        String[] split = prefix.split("_");
        String dataTime = split[1].substring(0, 10);
        Date date = TimeUtil.dateTimeStr2date(dataTime, TimeUtil.DATE_FMT_YMDH, TimeUtil.DEFAULT_DATETIME_FORMAT);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR_OF_DAY, 8);
        String dataTimeStr = TimeUtil.date2String(calendar.getTime(), "yyyyMMddHH");
        result = staion + "-" + DataTypeEnum.SWC3KM.getDataType() + "-" + dataTimeStr + ".csv";

        return result;
    }

    private static String getT1279CsvFileName(String fileName, String staion)
    {
//        zz_mwf_global_rh_700_202301260800.138
        String result = "";
        String prefix = FileUtil.getPrefix(fileName);
        String[] split = prefix.split("_");
        String dataTime = split[5].substring(0, 10);
        Date date = TimeUtil.dateTimeStr2date(dataTime, TimeUtil.DATE_FMT_YMDH, TimeUtil.DEFAULT_DATETIME_FORMAT);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        String dataTimeStr = TimeUtil.date2String(calendar.getTime(), "yyyyMMddHH");
        result = staion + "-" + DataTypeEnum.T1279.getDataType() + "-" + split[3] + "-" + dataTimeStr + ".csv";

        return result;
    }

    public static double[][] readGribDatasFromTxt(String filePath)
    {
        List<String> lines = FileUtil.readLines(filePath, "UTF-8");
        double[][] result = new double[lines.size()][];

        for(int i = 0, count = lines.size(); i < count; i++)
        {
            String[] split = lines.get(i).split(" ");
            double[] values = new double[split.length];
            for(int j = 0, num = values.length; j < num; j++)
            {
                values[j] = Double.parseDouble(split[j].trim());
            }
            result[i] = values;
        }

        return result;
    }
    public static double[][] readGribDatasFromTxt(String filePath, String regex)
    {
    	File file = new File(filePath);
    	if(!file.exists())
        {
        	return null;
        }
        List<String> lines = FileUtil.readLines(filePath, "UTF-8");
        double[][] result = new double[lines.size()][];

        for(int i = 0, count = lines.size(); i < count; i++)
        {
            String[] split = lines.get(i).split(regex);
            double[] values = new double[split.length];
            for(int j = 0, num = values.length; j < num; j++)
            {
                values[j] = Double.parseDouble(split[j].trim());
            }
            result[i] = values;
        }

        return result;
    }
}
