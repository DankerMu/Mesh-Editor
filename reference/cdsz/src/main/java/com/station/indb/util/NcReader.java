package com.station.indb.util;

import com.constants.DecodeConstants;
import com.util.DataTypeUtil;
import com.util.ReadPropertiesUtil;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 读取格点文件数据
 */
public class NcReader {

	//	private static Properties p = PropertiesUtil.loadProperties(NcReader.class.getClassLoader().getResourceAsStream("range.properties"));
	public static String lon = "lon";
	public static String lat = "lat";

	/**
	 * 读取格距
	 * @param dataMap
	 * @param lonName
	 * @param latName
	 * @return
	 */
	public static double[] getGridInterval(Map<String, Object> dataMap, String lonName, String latName)
	{
		Array array_lon = getArray(dataMap, lonName);
		Array array_lat = getArray(dataMap, latName);
//		float[] lons = (float[])getArray(dataMap, lonName).copyToNDJavaArray();
		if(array_lon == null)
		{
			lon = "longitude";
			lat = "latitude";
			array_lon = getArray(dataMap, lon);
			array_lat = getArray(dataMap, lat);
		}
		if(array_lon == null)
		{
			lon = "lon1";
			lat = "lat1";
			array_lon = getArray(dataMap, lon);
			array_lat = getArray(dataMap, lat);
		}
		if(array_lon == null)
		{
			lon = "x";
			lat = "y";
			array_lon = getArray(dataMap, lon);
			array_lat = getArray(dataMap, lat);
		}
		if(array_lon == null)
		{
			lon = "xc";
			lat = "yc";
			array_lon = getArray(dataMap, lon);
			array_lat = getArray(dataMap, lat);
		}
		int rank = array_lon.getRank();
		Object obj_lon = null;
		Object obj_lat = null;
		if(rank == 3)
		{
			lon = "xc";
			lat = "yc";
			array_lon = getArray(dataMap, lon);
			array_lat = getArray(dataMap, lat);
			obj_lon = array_lon.copyToNDJavaArray();
			obj_lat = array_lat.copyToNDJavaArray();
		}
		else
		{
			obj_lon = array_lon.copyTo1DJavaArray();
			obj_lat = array_lat.copyTo1DJavaArray();
		}
		float[] lons =toFloats(obj_lon);
		float[] lats =toFloats(obj_lat);

//		return new double[]{Math.abs(lons[0] - lons[1]), Math.abs(lats[0] - lats[1])};
		
		
		return new double[]{lons[1] - lons[0], lats[1] - lats[0]};
	}

	/**
	 * 读取格距
	 * @param dataMap
	 * @param lonName
	 * @param latName
	 * @return
	 */
	public static double[] getGridIntervalGroup(Map<String, Object> dataMap, String lonName, String latName, int group)
	{
		Array array_lon = getArray(dataMap, lonName);
		Array array_lat = getArray(dataMap, latName);
//		float[] lons = (float[])getArray(dataMap, lonName).copyToNDJavaArray();
		if(array_lon == null)
		{
			lon = "xxx" + group + lonName;
			lat = "xxx" + group + latName;
			array_lon = getArray(dataMap, lon);
			array_lat = getArray(dataMap, lat);
		}
		Object obj_lon = array_lon.copyTo1DJavaArray();
		Object obj_lat = array_lat.copyTo1DJavaArray();
		float[] lons =toFloats(obj_lon);
		float[] lats =toFloats(obj_lat);

//		return new double[]{Math.abs(lons[0] - lons[1]), Math.abs(lats[0] - lats[1])};
		return new double[]{lons[1] - lons[0], lats[1] - lats[0]};
	}

	/**
	 * 读取格距
	 * @param dataMap
	 * @param lonName
	 * @param latName
	 * @return
	 */
	public static double[] getGridIntervalFromGroup(Map<String, Object> dataMap, String lonName, String latName, String prefix)
	{
		Array array_lon = getArray(dataMap, lonName);
		Array array_lat = getArray(dataMap, latName);
//		float[] lons = (float[])getArray(dataMap, lonName).copyToNDJavaArray();
		if(array_lon == null)
		{
			lon = prefix + "longitude";
			lat = prefix + "latitude";
			array_lon = getArray(dataMap, lon);
			array_lat = getArray(dataMap, lat);
		}
		if(array_lon == null)
		{
			lon = prefix + "lon1";
			lat = prefix + "lat1";
			array_lon = getArray(dataMap, lon);
			array_lat = getArray(dataMap, lat);
		}
		if(array_lon == null)
		{
			lon = prefix + "x";
			lat = prefix + "y";
			array_lon = getArray(dataMap, lon);
			array_lat = getArray(dataMap, lat);
		}
		if(array_lon == null)
		{
			lon = prefix + "xc";
			lat = prefix + "yc";
			array_lon = getArray(dataMap, lon);
			array_lat = getArray(dataMap, lat);
		}
		int rank = array_lon.getRank();
		Object obj_lon = null;
		Object obj_lat = null;
		if(rank == 3)
		{
			lon = prefix + "xc";
			lat = prefix + "yc";
			array_lon = getArray(dataMap, lon);
			array_lat = getArray(dataMap, lat);
			obj_lon = array_lon.copyToNDJavaArray();
			obj_lat = array_lat.copyToNDJavaArray();
		}
		else
		{
			obj_lon = array_lon.copyTo1DJavaArray();
			obj_lat = array_lat.copyTo1DJavaArray();
		}
		float[] lons =toFloats(obj_lon);
		float[] lats =toFloats(obj_lat);

		return new double[]{Math.abs(lons[0] - lons[1]), Math.abs(lats[0] - lats[1])};
	}

	public static float[] toFloats(Object obj)
	{
		float[] result =  null;
		if(obj.getClass() == double[].class)
		{
			double[] datas = (double[]) obj;
			result = new float[datas.length];
			for(int i = 0, count = datas.length; i < count; i++)
			{
				result[i] = (float) datas[i];
			}
		}
		else if(obj.getClass() == int[].class)
		{
			int[] datas = (int[]) obj;
			result = new float[datas.length];
			for(int i = 0, count = datas.length; i < count; i++)
			{
				result[i] = datas[i];
			}
		}
		else if(obj.getClass() == long[].class)
		{
			long[] datas = (long[]) obj;
			result = new float[datas.length];
			for(int i = 0, count = datas.length; i < count; i++)
			{
				result[i] = datas[i];
			}
		}
		else if(obj.getClass() == String[].class)
		{
			String[] datas = (String[])obj;
			result = new float[datas.length];
			for(int i = 0, count = datas.length; i < count; i++)
			{
				result[i] = Float.parseFloat(datas[i]);
			}
		}
		else if(obj.getClass() == double[][].class)
		{
			double[][] value = (double[][]) obj;
			result = new float[value.length * value[0].length];
			for(int i = 0, count = value.length; i < count; i++)
			{
				for(int j = 0, num = value[i].length; j < num; j++)
				{
					result[i * num + j] = (float) value[i][j];
				}
			}
		}
		else
		{
			result = (float[])obj;
		}

		return result;
	}

	public static double[] toDoubles(Object obj)
	{
		double[] result =  null;
		if(obj.getClass() == double[].class)
		{
			double[] datas = (double[]) obj;
			result = new double[datas.length];
			for(int i = 0, count = datas.length; i < count; i++)
			{
				result[i] = datas[i];
			}
		}
		else if(obj.getClass() == int[].class)
		{
			int[] datas = (int[]) obj;
			result = new double[datas.length];
			for(int i = 0, count = datas.length; i < count; i++)
			{
				result[i] = datas[i];
			}
		}
		else if(obj.getClass() == long[].class)
		{
			long[] datas = (long[]) obj;
			result = new double[datas.length];
			for(int i = 0, count = datas.length; i < count; i++)
			{
				result[i] = datas[i];
			}
		}
		else if(obj.getClass() == String[].class)
		{
			String[] datas = (String[])obj;
			result = new double[datas.length];
			for(int i = 0, count = datas.length; i < count; i++)
			{
				result[i] = Double.parseDouble(datas[i]);
			}
		}
		else if(obj.getClass() == float[].class)
		{
			float[] datas = (float[])obj;
			result = new double[datas.length];
			for(int i = 0, count = datas.length; i < count; i++)
			{
				result[i] = datas[i];
			}
		}
		else if(obj.getClass() == double[][].class)
		{
			double[][] value = (double[][]) obj;
			result = new double[value.length * value[0].length];
			for(int i = 0, count = value.length; i < count; i++)
			{
				for(int j = 0, num = value[i].length; j < num; j++)
				{
					result[i * num + j] = (float) value[i][j];
				}
			}
		}
		else
		{
			result = (double[])obj;
		}

		return result;
	}

	public static String[] toStrings(Object obj)
	{
		String[] result =  null;
		if(obj.getClass() == String[].class)
		{
			result = (String[]) obj;
		}
		else if(obj.getClass() == double[].class)
		{
			double[] datas = (double[]) obj;
			result = new String[datas.length];
			for(int i = 0, count = datas.length; i < count; i++)
			{
				result[i] = datas[i] + "";
			}
		}
		else if(obj.getClass() == float[].class)
		{
			float[] datas = (float[]) obj;
			result = new String[datas.length];
			for(int i = 0, count = datas.length; i < count; i++)
			{
				result[i] = (float)datas[i] + "";
			}
		}

		return result;
	}

	public static double[] getLonWestLatSouth(Map<String, Object> dataMap, String lonName, String latName)
	{
		Array lonArray = getArray(dataMap, lonName);
		Array latArray = getArray(dataMap, latName);
		if(lonArray == null || latArray == null)
		{
			lon = "longitude";
			lat = "latitude";
			lonArray = getArray(dataMap, lon);
			latArray = getArray(dataMap, lat);
		}
		if(lonArray == null || latArray == null)
		{
			lon = "lon1";
			lat = "lat1";
			lonArray = getArray(dataMap, lon);
			latArray = getArray(dataMap, lat);
		}
		Object lonObj = lonArray.copyTo1DJavaArray();
		Object latObj = latArray.copyTo1DJavaArray();
//		float[] lons = (float[]) lonArray.copyToNDJavaArray();
//		float[] lats = (float[])latArray.copyToNDJavaArray();
		float[] lons = toFloats(lonObj);
		float[] lats = toFloats(latObj);

		return new double[]{lons[0], lats[0]};
	}


	public static float readByNameLayer(Map<String, Object> dataMap, String elName, String layer, float lon, float lat)
	{
		elName = elName.toLowerCase();
		Variable v = (Variable) dataMap.get(elName);
		int rank = v.getRank();
//		Object copyToNDJavaArray = getArray(dataMap, elName).copyToNDJavaArray();
		float[][] data = null;
		Array array = getArray(v);

		if(array == null)
		{
			return DecodeConstants.UNDEF_INT_VALUE;
		}
		Object copyToNDJavaArray = array.copyToNDJavaArray();
		if(rank == 3)
		{
			data = ((float[][][]) copyToNDJavaArray)[0];
		}
		if(rank == 4)
		{
			float[][][] datas = ((float[][][][]) copyToNDJavaArray)[0];
			int index = getLayerIndex(dataMap, elName, layer);
			data = datas[index];

		}
		int[] index = getPointIndex((float[])getArray(dataMap, "lon").copyToNDJavaArray(), (float[])getArray(dataMap, "lat").copyToNDJavaArray(), lon, lat);


		return data[index[0]][index[1]];
	}

	public static int getRank(Map<String, Object> dataMap, String elName, String layer)
	{
		elName = elName.toLowerCase();
		Variable v = (Variable) dataMap.get(elName);
		int rank = v.getRank();

		return rank;
	}

	public static String[] getDimensions(Map<String, Object> dataMap, String elName)
	{
		elName = elName.toLowerCase();
		Variable v = (Variable) dataMap.get(elName);
		List<Dimension> dimensions = v.getDimensions();
		int count = dimensions.size();
		String[] result = new String[count];
		for(int i = 0; i < count; i++)
		{
			result[i] = dimensions.get(i).getShortName();
		}

		return result;
	}

	public synchronized static double[][][] readByElemNameLayerSlice(Map<String, Object> dataMap, String elName, String layer)
	{
		double[][][] result = null;
		elName = elName.toLowerCase();
		Variable v = (Variable) dataMap.get(elName);
		if(v == null)
		{
			return result;
		}
		List<Attribute> attributes = v.getAttributes();
		float missValue = -999999f;
		for(Attribute att : attributes)
		{
			if(att.getFullName().equals("missing_value") || att.getFullName().equals("_FillValue"))
			{
				DataType dataType = att.getDataType();
				if(dataType == DataType.FLOAT)
				{
					missValue = (float) att.getNumericValue();
				}
				else if(dataType == DataType.INT)
				{
					missValue = (int) att.getNumericValue();
				}
			}
		}
		int rank = v.getRank();
		int[] sOrigin = new int[rank];
		int[] sShape = new int[rank];
		for(int i = 0; i < rank; i++) {
			sOrigin[i] = 0;
			sShape[i] = 1;
		}

		int[] shape = v.getShape();
		sShape[rank - 2] = shape[rank - 2];
		sShape[rank - 1] = shape[rank - 1];
		int layerIndex = getLayerIndex(dataMap, elName, layer);
//		boolean flag = DecodeGribFile.fileName.startsWith("era5_") ? true : false;
		boolean flag = false;
		int index = 3;
		if(flag)
		{
			index = 4;
		}
		int temp = rank - index;
		if(temp < 0)
		{
			temp = 0;
		}
		sOrigin[temp] = layerIndex;
		if(flag)
		{
			if(shape[0] == 2 && shape[1] == 2)
			{
				sOrigin[0] = layerIndex / 2;
				sOrigin[1] = layerIndex % 2;
			}
		}
		try {
			Array array = v.read(sOrigin, sShape);

			Object objArray = array.copyToNDJavaArray();

			result = toDoubleArray(objArray, rank, dataMap, elName, layer, missValue);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidRangeException e) {
			e.printStackTrace();
		}
		return result;
	}

	public synchronized static double[][][] readByElemNameLayerSliceEra5(Map<String, Object> dataMap, String elName, String layer)
	{
		double[][][] result = null;
		elName = elName.toLowerCase();
		Variable v = (Variable) dataMap.get(elName);
		if(v == null)
		{
			return result;
		}
		List<Attribute> attributes = v.getAttributes();
		float missValue = -999999f;
		for(Attribute att : attributes)
		{
			if(att.getFullName().equals("missing_value") || att.getFullName().equals("_FillValue"))
			{
				DataType dataType = att.getDataType();
				if(dataType == DataType.FLOAT)
				{
					missValue = (float) att.getNumericValue();
				}
				else if(dataType == DataType.INT)
				{
					missValue = (int) att.getNumericValue();
				}
			}
		}

		int[] shape = v.getShape();
		int rank = shape.length;
		int[] origin = new int[rank];
		int[] sizeShape = new int[rank];
		for(int i = 0; i < rank; i++) {
			origin[i] = 0;
			sizeShape[i] = 1;
		}
		sizeShape[rank - 2] = shape[rank - 2];
		sizeShape[rank - 1] = shape[rank - 1];

		int layerIndex = getLayerIndex(dataMap, elName, layer);
		origin[0] = layerIndex / 12;
		origin[1] = layerIndex % 12;

		try {
			Array array = v.read(origin, sizeShape);

			Object objArray = array.copyToNDJavaArray();

			result = toDoubleArray(objArray, rank, dataMap, elName, layer, missValue);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidRangeException e) {
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * 读取指定要素和层次的数据
	 * @param dataMap
	 * @param elName
	 * @param layer
	 * @return
	 */
	public static double[][][] readByNameLayer(Map<String, Object> dataMap, String elName, String layer)
	{
		double[][][] result = null;
		elName = elName.toLowerCase();
		Variable v = (Variable) dataMap.get(elName);
		if(v == null)
		{
			return result;
		}
		List<Attribute> attributes = v.getAttributes();
		float missValue = -999999f;
		for(Attribute att : attributes)
		{
			if(att.getFullName().equals("missing_value"))
			{
				missValue = (float) att.getNumericValue();
			}
		}
		int rank = v.getRank();
		Array array = getArray(dataMap, elName);
		Object objArray = array.copyToNDJavaArray();
		result = toDoubleArray(objArray, rank, dataMap, elName, layer, missValue);

		return result;
	}

	public static double[][][] readByNameLayerF(Map<String, Object> dataMap, String elName, String layer)
	{
		double[][][] result = null;
		elName = elName.toLowerCase();
		Variable v = (Variable) dataMap.get(elName);
		List<Attribute> attributes = v.getAttributes();
		float missValue = -999999f;
		for(Attribute att : attributes)
		{
			if(att.getFullName().equals("missing_value"))
			{
				missValue = (float) att.getNumericValue();
			}
		}
//		int rank = v.getRank();
//		long time = System.currentTimeMillis();
//		Object copyToNDJavaArray = getArray(dataMap, elName).copyToNDJavaArray();
		Array array = getArray(dataMap, elName);
//		System.out.println("000000000000000000000000: " + (System.currentTimeMillis() - time));
//		time = System.currentTimeMillis();
		Object objArray = array.copyToNDJavaArray();
//		System.out.println("读 " + elName + "_" + layer + " 数据耗时: " + (System.currentTimeMillis() - time));
		Class<? extends Object> arrayClass = objArray.getClass();
		if(arrayClass == byte[][][][][].class)
		{
			byte[][][] data = null;
			byte[][][][] datas = ((byte[][][][][]) objArray)[0];
			int index = getLayerIndex(dataMap, elName, layer);
			data = datas[index];
			result = new double[data.length][data[0].length][data[0][0].length];
			for(int i = 0; i < data.length; i++)
			{
				for(int j = 0; j < data[i].length; j++)
				{
					for(int k = 0; k < data[i][j].length; k++)
					{
						if(data[i][j][k] == missValue)
						{
							result[i][j][k] = (byte) DecodeConstants.UNDEF_INT_VALUE;
						}
						else
						{
							result[i][j][k] = data[i][j][k];
						}
					}
				}
			}
		}
		else if(arrayClass == short[][][][][].class)
		{
			short[][][] data = null;

			short[][][][] datas = ((short[][][][][]) objArray)[0];
			int index = getLayerIndex(dataMap, elName, layer);
			data = datas[index];
			result = new double[data.length][data[0].length][data[0][0].length];
			for(int i = 0; i < data.length; i++)
			{
				for(int j = 0; j < data[i].length; j++)
				{
					for(int k = 0; k < data[i][j].length; k++)
					{
						if(data[i][j][k] == missValue)
						{
							result[i][j][k] = (short) DecodeConstants.UNDEF_INT_VALUE;
						}
						else
						{
							result[i][j][k] = data[i][j][k];
						}
					}
				}
			}
		}
		else if(arrayClass == float[][][][][].class)
		{
			float[][][] data = null;

			float[][][][] datas = ((float[][][][][]) objArray)[0];
			int index = getLayerIndex(dataMap, elName, layer);
			data = datas[index];

			result = new double[data.length][data[0].length][data[0][0].length];
			for(int i = 0; i < data.length; i++)
			{
				for(int j = 0; j < data[i].length; j++)
				{
					for(int k = 0; k < data[i][j].length; k++)
					{
						if(data[i][j][k] == missValue)
						{
							result[i][j][k] = (float) DecodeConstants.UNDEF_INT_VALUE;
						}
						else
						{
							result[i][j][k] = data[i][j][k];
						}
					}
				}
			}
		}
		else if(arrayClass == double[][][][][].class)
		{
			double[][][] data = null;

			double[][][][] datas = ((double[][][][][]) objArray)[0];
			int index = getLayerIndex(dataMap, elName, layer);
			data = datas[index];

			result = new double[data.length][data[0].length][data[0][0].length];
			for(int i = 0; i < data.length; i++)
			{
				for(int j = 0; j < data[i].length; j++)
				{
					for(int k = 0; k < data[i][j].length; k++)
					{
						if(data[i][j][k] == missValue)
						{
							result[i][j][k] = (float) DecodeConstants.UNDEF_INT_VALUE;
						}
						else
						{
							result[i][j][k] = data[i][j][k];
						}
					}
				}
			}
		}

		return result;
	}

	/**
	 * 读取指定要素和层次的数据
	 * @param dataMap
	 * @param elName
	 * @param layer
	 * @return
	 */
	public static double[][][] readByNameLayerSynchronized(Map<String, Object> dataMap, String elName, String layer)
	{
		double[][][] result = null;
		elName = elName.toLowerCase();
		Variable v = (Variable) dataMap.get(elName);
//		System.out.println("--------------------: " + elName);
		List<Attribute> attributes = v.getAttributes();
		float missValue = -999999f;
		for(Attribute att : attributes)
		{
			if(att.getFullName().equals("missing_value"))
			{
				missValue = (float) att.getNumericValue();
			}
		}
		int rank = v.getRank();
		long time = System.currentTimeMillis();
		Array array = getArraySynchronized(dataMap, elName);
		Object copyToNDJavaArray = array.copyToNDJavaArray();
		result = toDoubleArray(copyToNDJavaArray, rank, dataMap, elName, layer, missValue);

		return result;
	}

	/**
	 * 读取指定要素和层次的数据
	 * @param dataMap
	 * @param elName
	 * @param layer
	 * @return
	 */
	public static double[][][] readByNameLayer(Map<String, Object> dataMap, String elName, String layer, int group)
	{
		double[][][] result = null;
		elName = elName.toLowerCase();
		Variable v = (Variable) dataMap.get(elName);
		if(v == null)
		{
			elName = "xxx" + group + elName;
			v = (Variable) dataMap.get(elName);
		}
		List<Attribute> attributes = v.getAttributes();
		float missValue = -999999f;
		for(Attribute att : attributes)
		{
			if(att.getFullName().equals("missing_value"))
			{
				missValue = (float) att.getNumericValue();
			}
		}
		int rank = v.getRank();
		long time = System.currentTimeMillis();
//		Object copyToNDJavaArray = getArray(dataMap, elName).copyToNDJavaArray();
		Array array = getArray(v);
//		System.out.println("000000000000000000000000: " + (System.currentTimeMillis() - time));
//		time = System.currentTimeMillis();
		Object copyToNDJavaArray = array.copyToNDJavaArray();
		result = toDoubleArray(copyToNDJavaArray, rank, dataMap, elName, layer, missValue);

		return result;
	}

	public static double[] readByName(Map<String, Object> dataMap, String elName)
	{
		double[] result = null;
		elName = elName.toLowerCase();
		Variable v = (Variable) dataMap.get(elName);
		if(v == null)
		{
			result = new double[]{-1};
			return result;
		}
		int rank = v.getRank();
		Array array = getArray(v);

		if(array == null)
		{
			return result;
		}
		Object copyToNDArray = array.copyToNDJavaArray();
		if(copyToNDArray.getClass() == byte[].class || copyToNDArray.getClass() == byte[][].class || copyToNDArray.getClass() == byte[][][].class || copyToNDArray.getClass() == byte[][][][].class)
		{
			byte[][] data = null;
			if(rank == 2)
			{
				data = ((byte[][]) copyToNDArray);
			}
			else if(rank == 3)
			{
				data = ((byte[][][]) copyToNDArray)[0];
			}
			else if(rank == 4)
			{
				byte[][][] datas = ((byte[][][][]) copyToNDArray)[0];
				int x = datas.length;
				int y = datas[0].length;
				int z = datas[0][0].length;
				result = new double[x * y * z];
				for(int i = 0; i < x; i++)
				{
					for(int j = 0; j < y; j++)
					{
						for(int k = 0; k < z; k++)
						{
							result[i * y * z + (j * z + k)] = datas[i][j][k];
						}
					}
				}
			}
		}
		else if(copyToNDArray.getClass() == short[][].class || copyToNDArray.getClass() == short[][][].class || copyToNDArray.getClass() == short[][][][].class)
		{
			short[][] data = null;
			if(rank == 2)
			{
				data = ((short[][]) copyToNDArray);

			}
			else if(rank == 3)
			{
				data = ((short[][][]) copyToNDArray)[0];
			}
			else if(rank == 4)
			{
				short[][][] datas = ((short[][][][]) copyToNDArray)[0];
				int x = datas.length;
				int y = datas[0].length;
				int z = datas[0][0].length;
				result = new double[x * y * z];
				for(int i = 0; i < x; i++)
				{
					for(int j = 0; j < y; j++)
					{
						for(int k = 0; k < z; k++)
						{
							result[i * y * z + (j * z + k)] = datas[i][j][k];
						}
					}
				}
			}
		}
		else if(copyToNDArray.getClass() == int[].class || copyToNDArray.getClass() == int[][].class || copyToNDArray.getClass() == int[][][].class || copyToNDArray.getClass() == int[][][][].class)
		{
			int[][] data = null;
			if(rank == 1)
			{
				int[] temp = (int[])copyToNDArray;
				int count = temp.length;
				result = new double[count];
				for(int i = 0; i < count; i++)
				{
					result[i] = temp[i];
				}
			}
			else if(rank == 2)
			{
				data = ((int[][]) copyToNDArray);

			}
			else if(rank == 3)
			{
				data = ((int[][][]) copyToNDArray)[0];
			}
			else if(rank == 4)
			{
				int[][][] datas = ((int[][][][]) copyToNDArray)[0];
				int x = datas.length;
				int y = datas[0].length;
				int z = datas[0][0].length;
				result = new double[x * y * z];
				for(int i = 0; i < x; i++)
				{
					for(int j = 0; j < y; j++)
					{
						for(int k = 0; k < z; k++)
						{
							result[i * y * z + (j * z + k)] = datas[i][j][k];
						}
					}
				}
			}
		}else if(copyToNDArray.getClass() == long[].class || copyToNDArray.getClass() == long[][].class || copyToNDArray.getClass() == long[][][].class || copyToNDArray.getClass() == long[][][][].class)
		{
			long[][] data = null;
			if(rank == 1)
			{
				long[] temp = (long[])copyToNDArray;
				int count = temp.length;
				result = new double[count];
				for(int i = 0; i < count; i++)
				{
					result[i] = temp[i];
				}
			}
			else if(rank == 2)
			{
				data = ((long[][]) copyToNDArray);

			}
			else if(rank == 3)
			{
				data = ((long[][][]) copyToNDArray)[0];
			}
			else if(rank == 4)
			{
				long[][][] datas = ((long[][][][]) copyToNDArray)[0];
				int x = datas.length;
				int y = datas[0].length;
				int z = datas[0][0].length;
				result = new double[x * y * z];
				for(int i = 0; i < x; i++)
				{
					for(int j = 0; j < y; j++)
					{
						for(int k = 0; k < z; k++)
						{
							result[i * y * z + (j * z + k)] = datas[i][j][k];
						}
					}
				}
			}
		}

		else if(copyToNDArray.getClass() == float[].class || copyToNDArray.getClass() == float[][].class || copyToNDArray.getClass() == float[][][].class || copyToNDArray.getClass() == float[][][][].class)
		{
			float[][] data = null;
			if(rank == 1)
			{
				float[] temp = (float[])copyToNDArray;
				int count = temp.length;
				result = new double[count];
				for(int i = 0; i < count; i++)
				{
					result[i] = temp[i];
				}
			}
			else if(rank == 2)
			{
				data = ((float[][]) copyToNDArray);
				result = new double[data.length * data[0].length];
				for(int i = 0, count = data.length; i < count; i++)
				{
					for(int j = 0, num = data[i].length; j < num; j++)
					{
						result[i * num + j] = data[i][j];
					}
				}
			}
			else if(rank == 3)
			{
				data = ((float[][][]) copyToNDArray)[0];
			}
			else if(rank == 4)
			{
				float[][][] datas = ((float[][][][]) copyToNDArray)[0];
				int x = datas.length;
				int y = datas[0].length;
				int z = datas[0][0].length;
				result = new double[x * y * z];
				for(int i = 0; i < x; i++)
				{
					for(int j = 0; j < y; j++)
					{
						for(int k = 0; k < z; k++)
						{
							result[i * y * z + (j * z + k)] = datas[i][j][k];
						}
					}
				}
			}
		}else if(copyToNDArray.getClass() == double[].class || copyToNDArray.getClass() == double[][].class || copyToNDArray.getClass() == double[][][].class || copyToNDArray.getClass() == double[][][][].class)
		{
			double[][] data = null;
			if(rank == 1)
			{
				double[] temp = (double[])copyToNDArray;
				int count = temp.length;
				result = new double[count];
				for(int i = 0; i < count; i++)
				{
					result[i] = temp[i];
				}
			}
			else if(rank == 2)
			{
				data = ((double[][]) copyToNDArray);
			}
			else if(rank == 3)
			{
				data = ((double[][][]) copyToNDArray)[0];
			}
			else if(rank == 4)
			{
				double[][][] datas = ((double[][][][]) copyToNDArray)[0];
				int x = datas.length;
				int y = datas[0].length;
				int z = datas[0][0].length;
				result = new double[x * y * z];
				for(int i = 0; i < x; i++)
				{
					for(int j = 0; j < y; j++)
					{
						for(int k = 0; k < z; k++)
						{
							result[i * y * z + (j * z + k)] = datas[i][j][k];
						}
					}
				}
			}
		}


		return result;
	}

	public static double[] readByName(Map<String, Object> dataMap, String elName, int group)
	{
		double[] result = null;
		elName = elName.toLowerCase();
		Variable v = (Variable) dataMap.get(elName);

		if(v == null)
		{
			elName = "xxx" + group + elName;
			v = (Variable) dataMap.get(elName);
		}

		if(v == null)
		{
			result = new double[]{-1};
			return result;
		}
		int rank = v.getRank();

		Array array = getArray(v);
		if(array == null)
		{
			return result;
		}
		Object copyToNDArray = getArray(v).copyToNDJavaArray();
		if(copyToNDArray.getClass() == byte[][].class || copyToNDArray.getClass() == byte[][][].class || copyToNDArray.getClass() == byte[][][][].class)
		{
			byte[][] data = null;
			if(rank == 2)
			{
				data = ((byte[][]) copyToNDArray);
			}
			else if(rank == 3)
			{
				data = ((byte[][][]) copyToNDArray)[0];
			}
			else if(rank == 4)
			{
				byte[][][] datas = ((byte[][][][]) copyToNDArray)[0];
				int x = datas.length;
				int y = datas[0].length;
				int z = datas[0][0].length;
				result = new double[x * y * z];
				for(int i = 0; i < x; i++)
				{
					for(int j = 0; j < y; j++)
					{
						for(int k = 0; k < z; k++)
						{
							result[i * y * z + (j * z + k)] = datas[i][j][k];
						}
					}
				}
			}
		}
		else if(copyToNDArray.getClass() == short[][].class || copyToNDArray.getClass() == short[][][].class || copyToNDArray.getClass() == short[][][][].class)
		{
			short[][] data = null;
			if(rank == 2)
			{
				data = ((short[][]) copyToNDArray);
			}
			else if(rank == 3)
			{
				data = ((short[][][]) copyToNDArray)[0];
			}
			else if(rank == 4)
			{
				short[][][] datas = ((short[][][][]) copyToNDArray)[0];
				int x = datas.length;
				int y = datas[0].length;
				int z = datas[0][0].length;
				result = new double[x * y * z];
				for(int i = 0; i < x; i++)
				{
					for(int j = 0; j < y; j++)
					{
						for(int k = 0; k < z; k++)
						{
							result[i * y * z + (j * z + k)] = datas[i][j][k];
						}
					}
				}
			}
		}
		else if(copyToNDArray.getClass() == float[].class || copyToNDArray.getClass() == float[][].class || copyToNDArray.getClass() == float[][][].class || copyToNDArray.getClass() == float[][][][].class)
		{
			float[][] data = null;
			if(rank == 1)
			{
				float[] temp = (float[])copyToNDArray;
				int count = temp.length;
				result = new double[count];
				for(int i = 0; i < count; i++)
				{
					result[i] = temp[i];
				}
			}
			else if(rank == 2)
			{
				data = ((float[][]) copyToNDArray);
			}
			else if(rank == 3)
			{
				data = ((float[][][]) copyToNDArray)[0];
			}
			else if(rank == 4)
			{
				float[][][] datas = ((float[][][][]) copyToNDArray)[0];
				int x = datas.length;
				int y = datas[0].length;
				int z = datas[0][0].length;
				result = new double[x * y * z];
				for(int i = 0; i < x; i++)
				{
					for(int j = 0; j < y; j++)
					{
						for(int k = 0; k < z; k++)
						{
							result[i * y * z + (j * z + k)] = datas[i][j][k];
						}
					}
				}
			}
		}


		return result;
	}


	public static float[][] readByNameLayerRange(Map<String, Object> dataMap, String elName, String layer, float startLon, float startLat, float endLon, float endLat)
	{
		float[][] result = null;
		Variable v = (Variable) dataMap.get(elName.toLowerCase());
		int rank = v.getRank();
		Array array = getArray(v);
		if(array == null)
		{
			return result;
		}
		Object copyToNDJavaArray = getArray(v).copyToNDJavaArray();
		float[][] data = null;
		if(rank == 3)
		{
			data = ((float[][][]) copyToNDJavaArray)[0];
		}
		if(rank == 4)
		{
			float[][][] datas = ((float[][][][]) copyToNDJavaArray)[0];
			int index = getLayerIndex(dataMap, elName, layer);
			data = datas[index];

		}
		String[] lonLatName = NcReader.getLonLatName(dataMap);
		int[][] indexArray = getIndexArray((float[])getArray(dataMap, lonLatName[0]).copyToNDJavaArray(), (float[])getArray(dataMap, lonLatName[1]).copyToNDJavaArray(), startLon, startLat, endLon, endLat);
		if(indexArray[0][0] < indexArray[1][0])
		{
			result = new float[Math.abs(indexArray[0][1] - indexArray[1][1]) + 1][Math.abs(indexArray[0][0] - indexArray[1][0]) + 1];
			for(int i = indexArray[0][1], x = 0; i <= indexArray[1][1]; i++, x++)
			{
				for(int j = indexArray[0][0], y = 0; j <= indexArray[1][0]; j++, y++)
				{
					result[x][y] = data[i][j];
				}
			}
		}
		else
		{
			int m = 0;
			result = new float[Math.abs(indexArray[0][1] - indexArray[1][1]) + 1][Math.abs(indexArray[0][0] - 359) + 1 + indexArray[1][0] + 1];
			for(int i = indexArray[0][1], x = 0; i <= indexArray[1][1]; i++, x++)
			{
				for(int j = indexArray[0][0], y = 0; j <= 359; j++, y++)
				{
					result[x][y] = data[i][j];
					m = y;
				}
			}
			for(int i = indexArray[0][1], z = 0; i <= indexArray[1][1]; i++, z++)
			{
				for(int j = 0, k = m + 1; j <= indexArray[1][0]; j++, k++)
				{
					result[z][k] = data[i][j];
				}
			}
		}

		return result;
	}


	private static double[][][] toDoubleArray(Object objArray, int rank, Map<String, Object> dataMap, String elName, String layer, float missValue)
	{
		double[][][] result = null;
		Class<? extends Object> arrayClass = objArray.getClass();
		if(arrayClass == byte[][].class || arrayClass == byte[][][].class || arrayClass == byte[][][][].class || arrayClass == byte[][][][][].class)
		{
			byte[][][] data = null;
			if(rank == 2)
			{
				data = new byte[1][][];
				data[0] = ((byte[][]) objArray);
			}
			else if(rank == 3)
			{
				data = ((byte[][][]) objArray);
			}
			else if(rank == 4)
			{
				byte[][][] datas = ((byte[][][][]) objArray)[0];
//				int index = getLayerIndex(dataMap, elName, layer);
				data = datas;
			}
			result = new double[data.length][data[0].length][data[0][0].length];
			for(int i = 0; i < data.length; i++)
			{
				for(int j = 0; j < data[0].length; j++)
				{
					for(int k = 0; k < data[i][j].length; k++)
					{
						if(data[i][j][k] == missValue)
						{
							result[i][j][k] = (byte) DecodeConstants.UNDEF_INT_VALUE;
						}
						else
						{
							result[i][j][k] = data[i][j][k];
						}
					}
				}
			}
		}
		else if(arrayClass == short[][].class || arrayClass == short[][][].class || arrayClass == short[][][][].class || arrayClass == short[][][][][].class)
		{
			short[][][] data = null;
			if(rank == 2)
			{
//				data = ((short[][][]) objArray);
				data = new short[1][][];
				data[0] = ((short[][]) objArray);
			}
			else if(rank == 3)
			{
				data = ((short[][][]) objArray);
			}
			else if(rank == 4)
			{
				short[][][] datas = ((short[][][][]) objArray)[0];
//				int index = getLayerIndex(dataMap, elName, layer);
				data = datas;
			}
			result = new double[data.length][data[0].length][data[0][0].length];
			for(int i = 0; i < data.length; i++)
			{
				for(int j = 0; j < data[0].length; j++)
				{
					for(int k = 0; k < data[i][j].length; k++)
					{
						if(data[i][j][k] == missValue)
						{
							result[i][j][k] = (short) DecodeConstants.UNDEF_INT_VALUE;
						}
						else
						{
							result[i][j][k] = data[i][j][k];
						}
					}
				}
			}
		}
		else if(arrayClass == float[][].class || arrayClass == float[][][].class || arrayClass == float[][][][].class || arrayClass == float[][][][][].class)
		{
			float[][][] data = null;
			if(rank == 2)
			{
//				data = ((float[][][]) objArray);
				data = new float[1][][];
				data[0] = ((float[][]) objArray);
			}
			else if(rank == 3)
			{
				data = ((float[][][]) objArray);
			}
			else if(rank == 4)
			{
				float[][][] datas = ((float[][][][]) objArray)[0];
//				int index = getLayerIndex(dataMap, elName, layer);
				data = datas;
			}
			result = new double[data.length][data[0].length][data[0][0].length];
			for(int i = 0; i < data.length; i++)
			{
				for(int j = 0; j < data[0].length; j++)
				{
					for(int k = 0; k < data[i][j].length; k++)
					{
						if(data[i][j][k] == missValue)
						{
							result[i][j][k] = (float) DecodeConstants.UNDEF_INT_VALUE;
						}
						else
						{
							result[i][j][k] = data[i][j][k];
						}
					}
				}
			}
		}
		else if(arrayClass == double[][].class || arrayClass == double[][][].class || arrayClass == double[][][][].class || arrayClass == double[][][][][].class)
		{
			double[][][] data = null;
			if(rank == 2)
			{
				data = new double[1][][];
				data[0] = ((double[][]) objArray);
			}
			else if(rank == 3)
			{
				data = ((double[][][]) objArray);
			}
			else if(rank == 4)
			{
				double[][][] datas = ((double[][][][]) objArray)[0];
//				int index = getLayerIndex(dataMap, elName, layer);
				data = datas;
			}
			else if(rank == 5)
			{
				double[][][][] datas = ((double[][][][][]) objArray)[0];
				int num = 0;
//				int index = getLayerIndex(dataMap, elName, layer);
				data = datas[num];
			}
			result = new double[data.length][data[0].length][data[0][0].length];
			for(int i = 0; i < data.length; i++)
			{
				for(int j = 0; j < data[0].length; j++)
				{
					for(int k = 0; k < data[i][j].length; k++)
					{
						if(data[i][j][k] == missValue)
						{
							result[i][j][k] = (double) DecodeConstants.UNDEF_INT_VALUE;
						}
						else
						{
							result[i][j][k] = data[i][j][k];
						}
					}
				}
			}
		}

		return result;
	}

	private static double[][][] numToDoubleArray(double[][][] data, double missValue)
	{
		double[][][] result = new double[data.length][data[0].length][data[0][0].length];
		for(int i = 0; i < data.length; i++)
		{
			for(int j = 0; j < data[0].length; j++)
			{
				for(int k = 0; k < data[i][j].length; )
				{
					if(data[i][j][k] == missValue)
					{
						result[i][j][k] = (byte) DecodeConstants.UNDEF_INT_VALUE;
					}
					else
					{
						result[i][j][k] = data[i][j][k];
					}
				}
			}
		}

		return result;
	}


	public synchronized static Map<String, Object> getDatasMap(String filePath)
	{
		System.out.println("start read file: " + filePath);
		Map<String, Object> result = new ConcurrentHashMap<String, Object>();
		try {
			String dataType = DataTypeUtil.getDataType(filePath);
			NetcdfFile dataset = NetcdfFile.open(filePath);
			Group rootGroup = dataset.getRootGroup();
			List<Group> groups = rootGroup.getGroups();
			int pngCount = 0;
			if(groups.size() > 0)
			{
				pngCount = getTotalCountGroup(groups, result, dataType);
			}
			else
			{
				pngCount = getTotalCount(dataset, result, dataType);
			}

			result.put("pngCount", pngCount);



		} catch (IOException e) {
			e.printStackTrace();
			return result;
		}


		return result;
	}

	private static int getTotalCountGroup(List<Group> groups, Map<String, Object> result, String dataType)
	{
		int total = 0;

		for(int i = 0, count = groups.size(); i < count; i++)
		{
			List<Variable> list = groups.get(i).getVariables();
			for(Variable v : list)
			{
				String name = v.getShortName().toLowerCase();
				result.put("xxx" + i + name, v);
//				System.out.println("=======================>" + name);
				total += getCount(name, dataType, v);
			}
		}

		return total;
	}

	private static int getTotalCount(NetcdfFile dataset, Map<String, Object> result, String dataType)
	{
		int total = 0;

		List<Variable> list = dataset.getVariables();
		for(Variable v : list)
		{
			String name = v.getShortName().toLowerCase();
			result.put(name, v);
			total += getCount(name, dataType, v);
		}

		return total;
	}

	private static int getCount(String name, String dataType, Variable v)
	{
		int total = 0;
//		if(ReadPropertiesUtil.decodeElementsMap.get(dataType) == null)
//		{
//			if(name.contains("u_") || name.contains("u-"))
//			{
//				total += getLayerCount(v.getShape());
//			}
//			if(v.getRank() > 2)
//			{
//				total += getLayerCount(v.getShape());
//			}
//		}
//		else
//		{
//			if(ReadPropertiesUtil.decodeElementsMap.get(dataType).contains(name))
//			{
//				if(name.contains("u_") || name.contains("u-"))
//				{
//					total += getLayerCount(v.getShape(), dataType, name);
//				}
//				if(v.getRank() > 2)
//				{
//					total += getLayerCount(v.getShape(), dataType, name);
//				}
//			}
//
//			if("grapes".equals(dataType) && name.startsWith("total_precipitation_surface_"))
//			{
//				total++;
//			}
//		}

		return total;
	}

	public static Map<String, Object> getHdf5DataMap(String filePath)
	{
		Map<String, Object> result = new ConcurrentHashMap<String, Object>();
		try {
			NetcdfFile dataset = NetcdfFile.open(filePath);
			Group rootGroup = dataset.getRootGroup();
			List<Group> groups = rootGroup.getGroups();
			int pngCount = 0;
			int[] shape = null;
			if(groups.size() > 0)
			{
				for(int i = 0, count = groups.size(); i < count; i++)
				{
					List<Variable> list = groups.get(i).getVariables();
					for(Variable v : list)
					{
						String name = v.getShortName();
						result.put("xxx" + i + name, v);
						shape = v.getShape();
						if(shape.length >= 2)
						{
							pngCount += getLayerCount(v.getShape());
						}
					}
				}
			}
			else
			{
				List<Variable> list = dataset.getVariables();
				for(Variable v : list)
				{
					String name = v.getShortName();
					result.put(name, v);
					shape = v.getShape();
					if(shape.length >= 2)
					{
						pngCount += getLayerCount(v.getShape());
					}
				}
			}

			result.put("pngCount", pngCount);

		} catch (IOException e) {
			e.printStackTrace();
			return result;
		}


		return result;
	}


	private static int getLayerCount(int[] shape)
	{
		int result = 0;
		int size = shape.length;
		result = getDefaultLayerCount(shape);

		return result;
	}

//	private static int getLayerCount(int[] shape, String dataType, String elem)
//	{
//		int result = 0;
//		int size = shape.length;
//		int scale = 1;
//		if(size == 5)
//		{
//			scale = shape[1];
//		}
//		Set<String> set = ReadPropertiesUtil.decodeElementsMap.get(dataType + "@default");
//		if(set == null)
//		{
//			set = ReadPropertiesUtil.decodeElementsMap.get(dataType + "@" + elem);
//			if(set == null)
//			{
//				result = getDefaultLayerCount(shape);
//			}
//			else
//			{
//				result = scale * set.size();
//			}
//		}
//		else
//		{
//			Set<String> elementSet = ReadPropertiesUtil.decodeElementsMap.get(dataType + "@" + elem);
//			if(elementSet == null)
//			{
//				if(size <= 3)
//				{
//					result = 1;
//				}
//				else if(size == 4)
//				{
//					if(shape[1] == 1)
//					{
//						result = 1;
//					}
//					else
//					{
//						result = set.size();
//					}
//				}
//				else
//				{
//					result = scale * set.size();
//					set = ReadPropertiesUtil.decodeElementsMap.get(dataType + "@" + elem);
//					if(set != null)
//					{
//						result = scale * set.size();
//					}
//				}
//			}
//			else
//			{
//				result = elementSet.size();
//			}
//
//		}
//
//
//		return result;
//	}

	private static int getDefaultLayerCount(int[] shape)
	{
		int result = 0;
		int size = shape.length;
		if(size == 2)
		{
			result = 1;
		}
		else if(size == 3)
		{
			result =  shape[0];
		}
		else if(size == 4)
		{
			result = shape[0] * shape[1];
		}
		else if(size == 5)
		{
			result = shape[0] * shape[1] * shape[2];
		}

		return result;
	}

	/**
	 * @category  获取要素的层次
	 * @param datasMap
	 * @param elName
	 * @return
	 */
	public static float[] getElementLayers(Map<String, Object> datasMap, String elName)
	{
		float[] result = null;
		String resultStr = null;
		Variable variable = (Variable) datasMap.get(elName.toLowerCase());
		if(variable != null)
		{
			int count = variable.getDimensions().size() - 2;
			Map<String, Integer> map = new HashMap<>();
			String[] temp = null;
			result = new float[count];
			for(int i = 0; i < count; i++)
			{
				temp = variable.getDimensions().get(i).toString().split("=");
				map.put(temp[0], Integer.parseInt(temp[1].trim().replace(";", "")));
			}

			String dimension = null;
			for(Map.Entry entry : map.entrySet())
			{
//				System.out.println(Integer.parseInt(entry.getValue().toString()));
				int total = Integer.parseInt(entry.getValue().toString());
				if(total != 1 && count >= 2)
				{
					dimension = entry.getKey().toString().trim();
					break;
				}
			}

			System.out.println("=========================== " + elName);
			result = NcReader.toFloats(NcReader.getArray(datasMap, dimension).copyToNDJavaArray());


		}

		return result;
	}

	public static String[] getLonLatName(Map<String, Object> datasMap)
	{
		String[] result = new String[]{"lon", "lat"};
		String elName = null;
		for(String key : datasMap.keySet())
		{
			if(!(datasMap.get(key) instanceof Variable))
			{
				continue;
			}
			Variable v = (Variable) datasMap.get(key);
			int rank = v.getRank();
			if(rank >= 2)
			{
				elName = key;
				break;
			}

		}
		Variable variable = (Variable) datasMap.get(elName.toLowerCase());
		if(variable != null)
		{
			int count = variable.getDimensions().size();

			String lon = variable.getDimensions().get(count - 1).toString().replace(";", "").replace(" ", "");
			if(lon.contains("UNLIMITED"))
			{
				lon = lon.replace("UNLIMITED//(", "").replace("currently)", "");
			}
			String[] split = lon.split("=");
			if(Integer.parseInt(split[1].trim()) >= 1)
			{
				lon = split[0];
			}
			String lat = variable.getDimensions().get(count - 2).toString().replace(";", "").replace(" ", "");
			if(lat.contains("UNLIMITED"))
			{
				lat = lat.replace("UNLIMITED//(", "").replace("currently)", "");
			}
			split = lat.split("=");
			if(Integer.parseInt(split[1].trim()) >= 1)
			{
				lat = split[0];
			}

			result[0] = lon;
			result[1] = lat;
		}

		return result;
	}
	public static String[] getLonLatName_(String fileName, Map<String, Object> datasMap)
	{
		String[] result = new String[]{"lon", "lat"};
		String elName = null;
		for(String key : datasMap.keySet())
		{
			if(!(datasMap.get(key) instanceof Variable))
			{
				continue;
			}
			Variable v = (Variable) datasMap.get(key);
			int rank = v.getRank();
			if(fileName.toLowerCase().startsWith("hj") || fileName.toLowerCase().startsWith("era5_"))
			{
				if(rank > 2)
				{
					elName = key;
					break;
				}
			}
			else
			{
				if(rank >= 2)
				{
					elName = key;
//					break;
				}
			}
			if(elName == null || elName.length() == 0)
			{
				continue;
			}

			Variable variable = (Variable) datasMap.get(elName.toLowerCase());
			if(variable != null)
			{
				int count = variable.getDimensions().size();

				String lon = variable.getDimensions().get(count - 1).toString().replace(";", "").replace(" ", "");
				if(lon.contains("UNLIMITED"))
				{
					lon = lon.replace("UNLIMITED//(", "").replace("currently)", "");
				}
				String[] split = lon.split("=");
				if(Integer.parseInt(split[1].trim()) >= 1)
				{
					lon = split[0];
				}
				String lat = variable.getDimensions().get(count - 2).toString().replace(";", "").replace(" ", "");
				if(lat.contains("UNLIMITED"))
				{
					lat = lat.replace("UNLIMITED//(", "").replace("currently)", "");
				}
				split = lat.split("=");
				if(Integer.parseInt(split[1].trim()) >= 1)
				{
					lat = split[0];
				}

				result[0] = lon;
				result[1] = lat;
				if(!result[0].equals("null") && !result[1].equals("null"))
				{
					break;
				}
			}
		}


		return result;
	}

	public static String getElementUnit(Map<String, Object> datasMap, String elName)
	{
		String prefix = "";
		String result = null;
		Variable variable = (Variable) datasMap.get(elName.toLowerCase());
		if(variable != null)
		{
			result = ((Variable)datasMap.get(prefix + elName)).getUnitsString();
		}
		if(result == null)
		{
			result = "";
		}
		result = result.replace(" ", ".").replace("**", "");

		return result;
	}

	public static String[] getDimensionNameAndUnit(Map<String, Object> datasMap, String elName)
	{
		String prefix = "";
		if(elName.contains(":"))
		{
			prefix = elName.split(":")[0] + ":";
		}
		String[] result = new String[4];
		Variable variable = (Variable) datasMap.get(elName.toLowerCase());
		if(variable != null)
		{
			String tempStr = null;
			int count = variable.getDimensions().size();
			if(count > 3)
			{
				tempStr = variable.getDimensions().get(1).toString();
				result[0] = tempStr.substring(0, tempStr.indexOf("=")).replace(" ", "").toLowerCase();
				result[1] = ((Variable)datasMap.get(prefix + result[0])).getUnitsString();
				if(result[1] == null)
				{
					result[1] = "";
				}
				tempStr = variable.getDimensions().get(2).toString();
				result[2] = tempStr.substring(0, tempStr.indexOf("=")).replace(" ", "").toLowerCase();
				result[3] = ((Variable)datasMap.get(prefix + result[2])).getUnitsString();
				if(result[3] == null)
				{
					result[3] = "";
				}
			}
			else if(count == 3)
			{
				tempStr = variable.getDimensions().get(0).toString().replace(";", "").replace(" ", "");
				if(tempStr.contains("UNLIMITED"))
				{
					result[0] = tempStr.replace("UNLIMITED//(", "").replace("currently)", "");
				}
				String[] split = tempStr.split("=");
				if(split[1].contains("UNLIMITED"))
				{
					result[0] = "";
					result[1] = "";
				}
				else if(Integer.parseInt(split[1].trim()) > 1)
				{
					result[0] = split[0].toLowerCase();
					result[1] = ((Variable)datasMap.get(prefix + result[0])).getUnitsString();
					if(result[1] == null)
					{
						result[1] = "";
					}
				}
				else
				{
					result[0] = "";
					result[1] = "";
				}
			}
		}

		return result;
	}

	public static String[] getCldasDimensionName(Map<String, Object> datasMap, String elName)
	{
		String prefix = "";
		String[] result = new String[4];
		Variable variable = (Variable) datasMap.get(elName.toLowerCase());
		if(variable != null)
		{
			String tempStr = null;
			int count = variable.getDimensions().size();
			if(count == 2)
			{
				tempStr = variable.getDimensions().get(0).toString().replace(";", "").replace(" ", "");
				String[] split = tempStr.split("=");
				result[0] = split[0].toLowerCase();
				tempStr = variable.getDimensions().get(1).toString().replace(";", "").replace(" ", "");
				split = tempStr.split("=");
				result[1] = split[0].toLowerCase();
			}
		}

		return result;
	}


	public static String getDimensionName(Map<String, Object> datasMap, String elName)
	{
		String result = null;
		Variable variable = (Variable) datasMap.get(elName.toLowerCase());
		if(variable != null)
		{
			int count = variable.getDimensions().size();
			if(count > 3)
			{
				result = variable.getDimensions().get(1).toString();
				result = result.substring(0, result.indexOf("=")).replace(" ", "");
			}
			else if(count == 3)
			{
				result = variable.getDimensions().get(0).toString().replace(";", "").replace(" ", "");
				if(result.contains("UNLIMITED"))
				{
					result = result.replace("UNLIMITED//(", "").replace("currently)", "");
				}
				String[] split = result.split("=");
				if(Integer.parseInt(split[1].trim()) > 1)
				{
					result = split[0];
				}
				else
				{
					result = "";
				}
			}
		}


		return result;
	}

	private static int[][] getIndexArray(float[] lon, float[] lat, float startLon, float startLat, float endLon, float endLat)
	{
//		float startX = lon[0];
//		float startY = lat[0];
//		float endX = lon[lon.length - 1];
//		float endY = lat[lat.length - 1];
		if(startLon < 0)
		{
			startLon = 0;
		}
		if(startLon > 359)
		{
			startLon = 359;
		}
		if(startLat < -90)
		{
			startLat = -90;
		}
		if(startLat > 90)
		{
			startLat = 90;
		}
		if(endLon < 0)
		{
			endLon = 0;
		}
		if(endLon > 359)
		{
			endLon = 359;
		}
		if(endLat < -90)
		{
			endLat = -90;
		}
		if(endLat > 90)
		{
			endLat = 90;
		}



		int startLonIndex = 0;
		int startLatIndex = 0;
		int endLonIndex = 0;
		int endLatIndex = 0;
		for(int i = 0, count = lon.length; i < count; i++)
		{
			if(startLon <= lon[i])
			{
				startLonIndex = i;
				break;
			}
//			if(startLon > 0)
//			{
//			}
//			else
//			{
//				if(startLon <= lon[i + 181])
//				{
//					startLonIndex = i + 181;
//					break;
//				}
//			}
		}
		for(int i = 0, count = lat.length; i < count; i++)
		{
			if(startLat > 0)
			{
				if(startLat >= lat[i])
				{
//					startLatIndex = i - 1;
					startLatIndex = i;
					break;
				}
			}
			else
			{
				if(startLat >= lat[i + 91])
				{
					startLatIndex = i + 91;
					break;
				}
			}
		}
		for(int i = 0, count = lon.length; i < count; i++)
		{
			if(endLon <= lon[i])
			{
				endLonIndex = i;
				break;
			}
//			if(endLon > 0)
//			{
//			}
//			else
//			{
//				if(endLon <= lon[i + 181])
//				{
//					endLonIndex = i + 181;
//					break;
//				}
//			}
		}
		for(int i = 1, count = lat.length; i < count; i++)
		{
			if(endLat > 0)
			{
				if(endLat >= lat[i])
				{
//					endLatIndex = i - 1;
					endLatIndex = i;
					break;
				}
			}
			else
			{
				if(endLat >= lat[i + 91])
				{
					endLatIndex = i + 91;
					break;
				}
			}
		}

		int[][] result = new int[][]{{startLonIndex, startLatIndex}, {endLonIndex, endLatIndex}};

		return result;
	}



	/**
	 * 获取相应层的数据数组的索引
	 * @param dataMap
	 * @param elName
	 * @param layer
	 * @return
	 */
	private static int getLayerIndex(Map<String, Object> dataMap, String elName, String layer)
	{
		int result = 0;
		if(layer == null)
		{
			return result;
		}
		Variable v = (Variable) dataMap.get(elName);
		int[] shape = v.getShape();
		int index = 0;
		if(shape.length >= 4)
		{
			index = 1;
		}
		Dimension dimension = v.getDimension(index);
		String name = dimension.getShortName().toLowerCase();
		String prefix = "";
		if (elName.contains(":")) {
			String[] split = elName.split(":");
			prefix = split[0] + ":";
		}
		Array array = getArray(dataMap, prefix + name);
		DataType dataType = array.getDataType();
		float[] layers = null;
		if(dataType == DataType.INT)
		{
			int[] layersInt = (int[]) array.copyTo1DJavaArray();
			layers = new float[layersInt.length];
			for(int i = 0, count = layers.length; i < count; i++)
			{
				layers[i] = layersInt[i];
			}
		}
		else if(dataType == DataType.LONG)
		{
			long[] layersInt = (long[]) array.copyTo1DJavaArray();
			layers = new float[layersInt.length];
			for(int i = 0, count = layers.length; i < count; i++)
			{
				layers[i] = layersInt[i];
			}
		}
		else if(dataType == DataType.FLOAT)
		{
			layers = (float[]) array.copyTo1DJavaArray();
		}
		else if(dataType == DataType.DOUBLE)
		{
			double[] layersD = (double[]) array.copyTo1DJavaArray();
			layers = new float[layersD.length];
			for(int i = 0, count = layersD.length; i < count; i++)
			{
				layers[i] = (float) layersD[i];
			}
		}
//		float[] layers = getElementLayers(dataMap, elName);
		float layerNum = Float.parseFloat(layer);
		for(int i = 0, count = layers.length; i < count; i++)
		{
			if(layers[i] == layerNum)
			{
				result = i;
				break;
			}
		}

		return result;
	}

	public static Array getArray(Variable v)
	{
		Array result = null;
		if(v == null)
		{
			return result;
		}
		try {
			result = v.read();
		} catch (IOException e) {
			e.printStackTrace();
			return result;
		}
		return result;
	}

	public static Array getArray(Map<String, Object> dataMap, String elName)
	{
		Array result = null;
		Variable v = (Variable) dataMap.get(elName);
		if(v == null)
		{
			return result;
		}
		try {
			result = v.read();
		} catch (Exception e) {
			try {
				result = v.read();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
			return result;
		}

		return result;
	}


	public static Array getArraySynchronized(Map<String, Object> dataMap, String elName)
	{
		Array result = null;
		Variable v = (Variable) dataMap.get(elName);
		if(v == null)
		{
			return result;
		}
		try {
			synchronized (lon) {
				result = v.read();
			}
		} catch (IOException e) {
			e.printStackTrace();
			return result;
		}
		return result;
	}

	private static int[] getPointIndex(float[] lon, float[] lat, float x, float y)
	{
		int xIndex = getPointIndex(lon, x);
		int yIndex = getPointIndex(lat, y);

		return new int[]{xIndex, yIndex};
	}

	private static int getPointIndex(float[] values, float value)
	{
		int result = 0;
		for(int i = 0, count = values.length; i < count - 1; i++)
		{
			if(value <= values[i + 1] && value > values[i])
			{
				result = Math.abs(value - values[i + 1]) <= Math.abs(value - values[i]) ? (i + 1) : i;
				break;
			}
		}

		return result;
	}

	public static int getValueByName(Map<String, Object> dataMap, String name)
	{
		int gate = 0;
		name = name.toLowerCase();
		Variable v = (Variable) dataMap.get(name);
		if(v == null)
		{
			return gate;
		}
		gate = (int) v.getSize();

		return gate;
	}

	public static void main(String[] args) {
//		String filePath = "E:/环境特性库/2019.09/fnl_20190901_00_00.grib2";
//		String elName = "Best_4_layer_Lifted_Index_surface".toLowerCase();
//		String layer = "100000";
//		float startLon = PropertiesUtil.getFloat(p, "startLon");
//		float startLat = PropertiesUtil.getFloat(p, "startLat");
//		float endLon = PropertiesUtil.getFloat(p, "endLon");
//		float endLat = PropertiesUtil.getFloat(p, "endLat");
//		Map<String, Object> dataMap = getDatasMap(filePath);
//		
//		float[][] read = readByNameLayerRange(dataMap, elName, layer, startLon, startLat, endLon, endLat);
////		float[][] read = readByNameLayer(dataMap, elName, layer);
//		for(int i = 0, count = read.length; i < count; i++)
//		{
//			System.out.println(Arrays.toString(read[i]));
//		}
	}
}
