package com.util;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

public class ReadPropertiesUtil {
	private static List<String> pathList = new ArrayList<>();
	static
	{
		pathList.add(System.getProperty("user.dir") + "/config/");
		pathList.add(System.getProperty("user.dir") + "/src/main/resources/");
	}
//	public static Map<String, Set<String>> decodeElementsMap = new HashMap<>();

	public static Map<String, double[]> getStationInfoConfigMap(String fileName)
	{
		Map<String, double[]> result = new HashMap<>();
		Properties p = new Properties();
		try {
//			InputStream is = ReadPropertiesUtil.class.getClassLoader().getResourceAsStream(fileName);
			InputStream is = getInputStream(fileName);
			InputStreamReader reader = new InputStreamReader(is, "utf-8");
			p.load(reader);
			Set<Entry<Object, Object>> entrySet = p.entrySet();
			for(Entry<Object, Object> en : entrySet)
			{
				String[] split = en.getValue().toString().split(",");
				result.put(en.getKey().toString(), new double[]{Double.parseDouble(split[0]), Double.parseDouble(split[1])});
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	public static Map<String, String> getElementDealConfigMap(String fileName)
	{
		Map<String, String> result = new HashMap<>();
		Properties p = new Properties();
		try {
//			InputStream is = ReadPropertiesUtil.class.getClassLoader().getResourceAsStream(fileName);
			InputStream is = getInputStream(fileName);
			InputStreamReader reader = new InputStreamReader(is, "utf-8");
			p.load(reader);
			Set<Entry<Object, Object>> entrySet = p.entrySet();
			for(Entry<Object, Object> en : entrySet)
			{
				result.put(en.getKey().toString(), en.getValue().toString());
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	public static Map<String, Set<String>> getConfigMap(String fileName)
	{
//		decodeElementsMap.clear();
		Map<String, Set<String>> decodeElementsMap = new HashMap<>();
		Properties p = new Properties();
		try {
			Set<Entry<Object, Object>> entrySet = p.entrySet();
//			InputStream is = new FileInputStream(System.getProperty("user.dir") + "/config/" + fileName);
//			InputStream is = ReadPropertiesUtil.class.getClassLoader().getResourceAsStream(fileName);
			InputStream is = getInputStream(fileName);
			p.load(is);
			entrySet = p.entrySet();
			Set<String> set = null;
			for(Entry<Object, Object> en : entrySet)
			{
				String[] split = en.getValue().toString().split(",");
				set = new HashSet<>();
				for(String str : split)
				{
					set.add(str);
				}
				decodeElementsMap.put(en.getKey().toString(), set);
			}

			return decodeElementsMap;

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Map<String, String> getUserConfigMap(String fileName)
	{
		Map<String, String> result = new HashMap<>();
		Properties p = new Properties();
		try {
//			InputStream is = ReadPropertiesUtil.class.getClassLoader().getResourceAsStream(fileName);
			InputStream is = getInputStream(fileName);
			InputStreamReader reader = new InputStreamReader(is, "utf-8");
			p.load(reader);
			Set<Entry<Object, Object>> entrySet = p.entrySet();
			for(Entry<Object, Object> en : entrySet)
			{
				result.put(en.getKey().toString(), en.getValue().toString());
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}
	public static Map<String, String> getDbConfigMap(String fileName)
	{
		Map<String, String> result = new HashMap<>();
		Properties p = new Properties();
		try {
//			InputStream is = ReadPropertiesUtil.class.getClassLoader().getResourceAsStream(fileName);
			InputStream is = getInputStream(fileName);
			InputStreamReader reader = new InputStreamReader(is, "utf-8");
			p.load(reader);
			Set<Entry<Object, Object>> entrySet = p.entrySet();
			for(Entry<Object, Object> en : entrySet)
			{
				result.put(en.getKey().toString(), en.getValue().toString());
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	public static Map<String, String> getTableConfigMap(String fileName)
	{
		Map<String, String> result = new HashMap<>();
		Properties p = new Properties();
		try {
//			InputStream is = ReadPropertiesUtil.class.getClassLoader().getResourceAsStream(fileName);
			InputStream is = getInputStream(fileName);
			InputStreamReader reader = new InputStreamReader(is, "utf-8");
			p.load(reader);
			Set<Entry<Object, Object>> entrySet = p.entrySet();
			for(Entry<Object, Object> en : entrySet)
			{
				result.put(en.getKey().toString(), en.getValue().toString());
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	private static InputStream getInputStream(String fileName)
	{
		List<String> pathList = new ArrayList<>();
		pathList.add(System.getProperty("user.dir") + "/config/");
		pathList.add(System.getProperty("user.dir") + "/src/main/resources/");
		InputStream is = null;
		try {
			String configFilePath = pathList.get(0) + fileName;
			File configFile = new File(configFilePath);
			if(configFile.exists())
			{
				is = new FileInputStream(configFile);
				if(is != null)
				{
					return is;
				}
			}
			configFilePath = pathList.get(1) + fileName;
			configFile = new File(configFilePath);
			if(configFile.exists())
			{
				is = new FileInputStream(configFile);
				if(is != null)
				{
					return is;
				}
			}
//			configFilePath = pathList.get(1) + fileName;
//			configFile = new File(configFilePath);
//			if(configFile.exists())
//			{
//			}
			is = ReadPropertiesUtil.class.getClassLoader().getResourceAsStream(fileName);
			if(is != null)
			{
				return is;
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return is;
	}
}
