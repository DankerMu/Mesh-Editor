package com.check.pojo;

import com.constants.DecodeConstants;

import lombok.Data;

@Data
public class CheckDataGribIndbEntity {
	private String station;
	private String dataTime;
	private String inserttime;
	private String dataSource;
	private int hour;
	private int zone;
	private int zone2;
	private int rain24h24 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24tn24 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24m24 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24f24 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24h48 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24tn48 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24m48 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24f48 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24h72 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24tn72 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24m72 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24f72 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24h96 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24tn96 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24m96 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24f96 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24h120 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24tn120 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24m120 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24f120 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24h144 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24tn144 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24m144 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24f144 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24h168 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24tn168 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24m168 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24f168 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24h192 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24tn192 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24m192 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24f192 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24h216 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24tn216 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24m216 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24f216 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24h240 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24tn240 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24m240 = DecodeConstants.UNDEF_INT_VALUE;
	private int rain24f240 = DecodeConstants.UNDEF_INT_VALUE;

}
