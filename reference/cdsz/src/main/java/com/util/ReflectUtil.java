package com.util;

import java.lang.reflect.Field;
import java.util.*;

public class ReflectUtil {
	
	public static Object getSuperFieldValueByName(Object obj, String fieldName)
	{
		Object object = null;
		try {
			Field field = obj.getClass().getSuperclass().getDeclaredField(fieldName);
			field.setAccessible(true);
			object = field.get(obj);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		
		return object;
	}
	
	public static Object getFieldValueByName(Object obj, String fieldName)
	{
		Object object = null;
		try {
			Field field = obj.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			object = field.get(obj);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		
		return object;
	}
	
	public static void setFieldValueAutoByType(Object obj, Object value, String fieldName)
	{
		try {
			Field field = obj.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(obj, getFieldRealTypeValue(field, value));
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}
	
	public static void setFieldValueByName(Object obj, Object value, String fieldName)
	{
		try {
			Field field = obj.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(obj, value);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}
	
	public static void setFieldsValue(Object obj, Map<String, Object> map)
	{
		Set<String> keySet = map.keySet();
		Field[] fields = obj.getClass().getDeclaredFields();
		Field[] fields2 = obj.getClass().getSuperclass().getDeclaredFields();
		List<Field> list = new ArrayList<>(Arrays.asList(fields));
		list.addAll(Arrays.asList(fields2));
		
		try {
			for(Field field : list) 
			{
				field.setAccessible(true);
				if(keySet.contains(field.getName()))
				{
					field.set(obj, map.get(field.getName()));
				}
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	public static void setFieldsValueAuto(Object obj, Map<String, String> map)
	{
		Set<String> keySet = map.keySet();
		Field[] fields = obj.getClass().getDeclaredFields();
		Field[] fields2 = obj.getClass().getSuperclass().getDeclaredFields();
		List<Field> list = new ArrayList<>(Arrays.asList(fields));
		list.addAll(Arrays.asList(fields2));
		
		try {
			for(Field field : list)
			{
				field.setAccessible(true);
				if(keySet.contains(field.getName()))
				{
					field.set(obj, getFieldRealType(field, map.get(field.getName())));
				}
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	private static Object getFieldRealTypeValue(Field field, Object value)
	{
		Object result = null;
		String valueStr = String.valueOf(value);
		Class<?> type = field.getType();
		if(type.getName().contains("float") || type == Float.class)
		{
			result = Float.parseFloat(valueStr);
		}
		else if(type.getName().contains("int") || type == Integer.class)
		{
			result = Integer.parseInt(valueStr);
		}
		else if(type.getName().contains("double") || type == Double.class)
		{
			result = Double.parseDouble(valueStr);
		}
		else if(type.getName().contains("long") || type == Long.class)
		{
			result = Long.parseLong(valueStr);
		}
		else if(type.getName().contains("short") || type == Short.class)
		{
			result = Short.parseShort(valueStr);
		}
		else if(type == String.class)
		{
			result = valueStr;
		}
		
		return result;
	}
	
	private static Object getFieldRealType(Field field, String valueStr)
	{
		Object result = null;
		Class<?> type = field.getType();
		if(type.getName().contains("float") || type == Float.class)
		{
			result = Float.parseFloat(valueStr);
		}
		else if(type.getName().contains("int") || type == Integer.class)
		{
			result = Integer.parseInt(valueStr);
		}
		else if(type.getName().contains("double") || type == Double.class)
		{
			result = Double.parseDouble(valueStr);
		}
		else if(type.getName().contains("long") || type == Long.class)
		{
			result = Long.parseLong(valueStr);
		}
		else if(type.getName().contains("short") || type == Short.class)
		{
			result = Short.parseShort(valueStr);
		}
		else if(type == String.class)
		{
			result = valueStr;
		}
		
		return result;
	}

	public static void setFieldValueByNameTest(Object obj, Object value, String fieldName)
	{
		try {
//			Field[] declaredFields = obj.getClass().getSuperclass().getDeclaredFields();
//			for(Field field : declaredFields)
//			{
//				System.out.println(field.getName());
//			}
			Field field = obj.getClass().getSuperclass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(obj, value);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}
}
