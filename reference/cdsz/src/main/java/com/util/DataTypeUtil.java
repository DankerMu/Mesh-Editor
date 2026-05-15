package com.util;

import cn.hutool.core.io.FileUtil;
import com.constants.DataTypeEnum;

public class DataTypeUtil {

	public static String getDataType(String filePath)
	{
		String result = null;
		String prefix = FileUtil.getPrefix(filePath).toLowerCase();
		if(prefix.contains("-grapes-") || prefix.contains("_grapes_"))
		{
			result = DataTypeEnum.GRAPES.getDataType();
		}
		else if(prefix.startsWith("et"))
		{
			result = DataTypeEnum.QQZQ.getDataType();
		}
		else if(prefix.contains("_ecmf_") || prefix.startsWith("china-ec-"))
		{
			result = DataTypeEnum.ECMF.getDataType();
		}
		else if(prefix.startsWith("zz_"))
		{
			result = DataTypeEnum.T1279.getDataType();
		}
		else if(prefix.contains("kwbc"))
		{
			result = DataTypeEnum.KWBC.getDataType();
		}
		else if(prefix.startsWith("soda"))
		{
			result = DataTypeEnum.SODA.getDataType();
		}
		else if(prefix.startsWith("hjc"))
		{
			result = DataTypeEnum.HJC.getDataType();
		}
		else if(prefix.startsWith("ht"))
		{
			result = DataTypeEnum.HT.getDataType();
		}
		else if(prefix.startsWith("ec"))
		{
			result = DataTypeEnum.EC.getDataType();
		}
		else if(prefix.startsWith("ed"))
		{
			result = DataTypeEnum.ED.getDataType();
		}
		else if(prefix.startsWith("kw"))
		{
			result = DataTypeEnum.KW.getDataType();
		}
		else if(prefix.startsWith("rj"))
		{
			result = DataTypeEnum.RJ.getDataType();
		}
		else if(prefix.startsWith("era5"))
		{
			result = DataTypeEnum.ERA5.getDataType();
		}
		else if(prefix.startsWith("ww3"))
		{
			result = DataTypeEnum.WW3.getDataType();
		}
		else if(prefix.startsWith("kt"))
		{
			result = DataTypeEnum.KT.getDataType();
		}
		else if(prefix.startsWith("swcwarms"))
		{
			result = DataTypeEnum.SWC9KM.getDataType();
		}
		else if(prefix.startsWith("swcwings"))
		{
			result = DataTypeEnum.SWC3KM.getDataType();
		}
		else if(prefix.startsWith("z_nwgd_c_babj_"))
		{
			result = DataTypeEnum.SCMOC.getDataType();
		}
		else if(prefix.contains("_p_cmpa_"))
		{
			result = DataTypeEnum.CMPA.getDataType();
		}
		else if(prefix.contains("_p_cldas_"))
		{
			result = DataTypeEnum.CLDAS.getDataType();
			if(prefix.contains("_0p05_"))
			{
				result = DataTypeEnum.CLDAS.getDataType() + "_p5";
			}
		}
		else if(prefix.contains("_deeplearning_"))
		{
			result = DataTypeEnum.DEEP.getDataType();
		}
		else if(prefix.startsWith("ptype_999_revised"))
		{
			result = DataTypeEnum.PTYPE.getDataType();
		}
		else if(filePath.endsWith(".txt"))
		{
			result = DataTypeEnum.PRODUCT.getDataType();
		}
		else if(filePath.endsWith(".xls") || filePath.endsWith(".xlsx") || filePath.endsWith(".csv"))
		{
			result = DataTypeEnum.EXCEL.getDataType();
		}
		else if(checkIsEra5(prefix))
		{
			result = DataTypeEnum.ERA5.getDataType();
		}
		else
		{
			result = DataTypeEnum.DEFAULT.getDataType();
		}
		
		return result;
	}

	private static boolean checkIsEra5(String prefix)
	{
		boolean result = false;
		if(prefix.startsWith("10m_u_component_of_wind"))
		{
			result = true;
		}
		else if(prefix.startsWith("10m_v_component_of_wind"))
		{
			result = true;
		}
		else if(prefix.startsWith("2m_temperature"))
		{
			result = true;
		}
		else if(prefix.startsWith("precipitation_type"))
		{
			result = true;
		}
		else if(prefix.startsWith("total_precipitation"))
		{
			result = true;
		}
		else if(prefix.startsWith("low_cloud_cover"))
		{
			result = true;
		}
		else if(prefix.startsWith("total_cloud_cover"))
		{
			result = true;
		}

		return result;
	}
}
