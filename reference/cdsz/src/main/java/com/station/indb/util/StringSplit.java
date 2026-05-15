package com.station.indb.util;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class StringSplit {
	/**
	 * @Title: stringSplit 
	 * @Description: TODO(字符串分割函数) 
	 * @param str 需要分割的字符串
	 * @param regex 分割符
	 * @return String[] 返回分割后的数组
	 * @throws:
	 */
	public static String[] stringSplit(String str, String regex) {
		String[] items = str.split(regex);
		List<String> list = new ArrayList<>();
		for (String string : items) {
			if(string != null && string.length() != 0 && !string.trim().equals("")) {
				list.add(string);
			}
		}
		items = list.toArray(new String[0]);
		return items;
	}
	
	/**
	 * @Title: split
	 * @Description: 按字符分隔字符串
	 * @param content 需要被分隔的字符串
	 * @param regex 分隔符，如果是多个字符需以空隔分隔，如：“- . _ |”
	 * @return String[] 分隔之后的字符数组
	 */
	public static String[] split(String content, String regex) {
		return content.split("(" + string2Regex(regex).replaceAll("\\s+", "|") + ")+");
	}
	
	/**
	 * @Title: formatRegex
	 * @Description: 将普通字符串转换成需要的正则表达式，用于replaceAll replaceFirst split, 字符串中的几个特殊字符需要转换
	 * @param reg
	 * @return String
	 */
	public static String string2Regex(String reg) {
		char[] flags = { '.', '(', ')', '[', ']', '{', '}', '$', '^', '\\', '|' };

		char[] chars = reg.toCharArray();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < chars.length; i++) {
			char c = chars[i];
			boolean isContain = ArrayUtils.contains(flags, c);
			if (isContain) {
				if (i == 0 || chars[i - 1] != '\\') {
					sb.append("\\");
				}
			}
			sb.append(c);
		}

		return sb.toString();
	}
	
	
	public static void main(String[] args) {
		Calendar calendar = Calendar.getInstance();
		System.out.println(calendar.getTime());
		System.out.println(calendar.getTime().getTime());
	}
}
