/**
 * 
 */
package com.image;

//import contour.math.geo.ShapeCreate;

import java.util.*;

import com.util.GribUtil;

//import contour.math.geo.ShapeCreate;


/**
 * @author
 * @2015-8-26 下午5:32:10
 * @TODO :
 */

/**
 * @author
 * @2015-8-27 下午12:50:40
 * @TODO :
 */
public class ContourTool {

	private final int LINE_EDGE = 1;
	private final int COLUMN_EDGE = 2;
	private final int CORNER = 3;

	private boolean isBetween(double value1, double value2, double value) {
		if (value > value1 && value < value2)
			return true;
		if (value < value1 && value > value2)
			return true;

		return false;
	}

	/**
	 * TODO :根据两个要素的值获取某一个数值与两个要素间的距离，并根据两个要素的一维坐标获取某数值在一维坐标轴上的坐标
	 * 
	 * @param value
	 *            ：所要获取值坐标的值
	 * @param value1
	 *            ：参考要素值1
	 * @param value2
	 *            ：参考要素值2
	 * @param column1
	 *            ：要素1的一维坐标
	 * @param column2
	 *            ：要素2的一维坐标
	 * @return : double :参考坐标
	 * @author 李志楷 2015-9-21 上午9:33:39
	 */
	private double calColumnOrLine(double value, double value1, double value2,
			int column1, int column2) {
		double distance = (value - value2) / (value1 - value2);
		double columnD = (column1 - column2) * distance + column2;

		return columnD;
	}

	/**
	 * TODO
	 * :传入两个相邻矩阵要素，且这两个要素在同一行上，并且为相邻要素，获取这两个要素之间是否有等值点，如果有则放进集合中，如果没有则不做任何操作
	 * 
	 * @param column1
	 *            ：某一个要素的列号
	 * @param value1
	 *            ：某一个要素的值
	 * @param column2
	 *            ：另一个要素的列号
	 * @param value2
	 *            ：另一个要素的值
	 * @param contourValue
	 *            ：等值线的值
	 * @param lineWidth
	 *            ：矩阵的行宽
	 * @param line
	 *            ：要素的行号
	 * @param map
	 *            ：储存等值点的集合对象
	 * @param invalidValue
	 *            ::无效值 void :
	 * @author 李志楷 2015-9-21 上午9:36:54
	 */
	private void getContourPointByLine(int column1, double value1, int column2,
			double value2, double contourValue, int lineWidth, int line,
			Map<MapKey, double[]> map, double invalidValue) {
		// 如果两个要素相等则认为这两个点之间不存在等值点
		if (value1 == invalidValue || value2 == invalidValue)
			return;

		// 如果某一个要素等于等值先的值，则将根据另一个要素的值对它的值做相应的变化
		if (value1 == contourValue) {
			value1 = Math.abs(value1 - value2) / 100. + value1;
		}
		if (value2 == contourValue) {
			value2 = Math.abs(value1 - value2) / 100. + value2;
		}

		// 判断等值线值是否介于两个要素的值之间
		if (isBetween(value1, value2, contourValue)) {
			// 计算等值点所在的列坐标
			double column = calColumnOrLine(contourValue, value1, value2,
					column1, column2);

			// 将等值点放入集合中
			double[] coord = new double[] { line, column };
			MapKey key = new MapKey(LINE_EDGE, (long) Math.floor(column)
					+ (long) line * lineWidth);
			map.put(key, coord);
		}
		/*
		 * else if (value1 == contourValue && value2 < contourValue) { double[]
		 * coord = new double[] { line, column1 }; MapKey key = new
		 * MapKey(CORNER, (long) column1 + (long) line lineWidth); if
		 * (map.containsKey(key)) return; map.put(key, coord); } else if (value2
		 * == contourValue && value1 < contourValue) { double[] coord = new
		 * double[] { line, column2 }; MapKey key = new MapKey(CORNER, (long)
		 * column2 + (long) line lineWidth); if (map.containsKey(key)) return;
		 * map.put(key, coord); }
		 */
	}

	/**
	 * TODO
	 * :传入两个相邻矩阵要素，且这两个要素在同一列上，并且为相邻要素，获取这两个要素之间是否有等值点，如果有则放进集合中，如果没有则不做任何操作
	 * 
	 * @param line1
	 *            ：某一个要素的行号
	 * @param value1
	 *            ：某一个要素的值
	 * @param line2
	 *            ：另一个要素的行号
	 * @param value2
	 *            ：另一个要素的列号
	 * @param contourValue
	 *            ：等值线的值
	 * @param lineWidth
	 *            ：矩阵的行宽
	 * @param column
	 *            ：要素的列号
	 * @param map
	 *            ：储存等值点的集合对象
	 * @param invalidValue
	 *            :无效值 void :
	 * @author 李志楷 2015-9-21 上午9:27:33
	 */
	private void getContourPointByColumn(int line1, double value1, int line2,
			double value2, double contourValue, int lineWidth, int column,
			Map<MapKey, double[]> map, double invalidValue) {
		// 如果两个要素相等则认为这两个点之间不存在等值点
		if (value1 == invalidValue || value2 == invalidValue)
			return;

		// 如果某一个要素等于等值先的值，则将根据另一个要素的值对它的值做相应的变化
		if (value1 == contourValue) {
			value1 = Math.abs(value1 - value2) / 100. + value1;
		}
		if (value2 == contourValue) {
			value2 = Math.abs(value1 - value2) / 100. + value2;
		}

		// 判断等值线值是否介于两个要素的值之间
		if (isBetween(value1, value2, contourValue)) {
			// 计算等值点所在的行坐标
			double line = calColumnOrLine(contourValue, value1, value2, line1,
					line2);

			// 将等值点放入集合中
			double[] coord = new double[] { line, column };
			MapKey key = new MapKey(COLUMN_EDGE, column
					+ (long) Math.floor(line) * lineWidth);
			map.put(key, coord);
		}
		/*
		 * else if (value1 == contourValue && value2 < contourValue) { double[]
		 * coord = new double[] { line1, column }; MapKey key = new
		 * MapKey(CORNER, column + (long) line1 * lineWidth); if
		 * (map.containsKey(key)) return; map.put(key, coord); } else if (value2
		 * == contourValue && value1 < contourValue) { double[] coord = new
		 * double[] { line2, column }; MapKey key = new MapKey(CORNER, column +
		 * (long) line2 * lineWidth); if (map.containsKey(key)) return;
		 * map.put(key, coord); }
		 */
	}

	/**
	 * TODO :
	 * 
	 * @param array
	 *            ： 矩阵
	 * @param line
	 *            ： 行号
	 * @param column
	 *            ： 列号
	 * @param map
	 *            ：将等值点都塞进该集合中
	 * @param contourValue
	 *            ：等值线的值
	 * @param invalidValue
	 *            :无效值 void :
	 * @author 李志楷 2015-9-21 上午9:26:17
	 */
	private void getContourPointMapByCoord(double[][] array, int line,
			int column, Map<MapKey, double[]> map, double contourValue,
			double invalidValue) {
		if (line + 1 < array.length) {
			// 获取矩阵列边上的等值点
			getContourPointByColumn(line, array[line][column], line + 1,
					array[line + 1][column], contourValue, array[0].length,
					column, map, invalidValue);
		}

		if (column + 1 < array[0].length) {
			// 获取矩阵行边上的等值点
			getContourPointByLine(column, array[line][column], column + 1,
					array[line][column + 1], contourValue, array[0].length,
					line, map, invalidValue);
		}
	}

	/**
	 * TODO :
	 * 
	 * @param array
	 *            : 需要抽离等值线的二维数组
	 * @param contourValue
	 *            : 需要抽离等值线的值
	 * @param invalidValue
	 *            : 矩阵中无效数据的值
	 * @return : 返回所有等值点的集合 Map<MapKey,double[]> :key记录了点所在矩阵元素中的索引号，各等值点的矩阵坐标
	 * @author 李志楷 2015-9-21 上午9:22:42
	 */
	private Map<MapKey, double[]> getContourPointsMap(double[][] array,
			double contourValue, double invalidValue) {
		Map<MapKey, double[]> pointsMap = new HashMap<MapKey, double[]>();
		for (int i = 0; i < array.length; i++) {
			for (int j = 0; j < array[0].length; j++) {
				// 逐个矩阵元素追踪等值线
				getContourPointMapByCoord(array, i, j, pointsMap, contourValue,
						invalidValue);
			}
		}

		return pointsMap;
	}

	private double calDistance(double[] coord1, double[] coord2) {
		double distance = Math.sqrt(Math.pow(coord1[0] - coord2[0], 2)
				+ Math.pow(coord1[1] - coord2[1], 2));
		return distance;
	}

	private double calDistance(MapKey startKey, MapKey endKey,
			double[] startCoord, double[] endCoord) {
		if (startKey.getType() == COLUMN_EDGE) {
			return Math.abs(endCoord[1] - startCoord[1]);
		} else if (startKey.getType() == LINE_EDGE) {
			return Math.abs(endCoord[0] - startCoord[0]);
		} else {
			return calDistance(startCoord, endCoord);
		}
	}

	/**
	 * TODO :获取某一个点的附近所有的点
	 * 
	 * @param keys
	 * @param pointsMap
	 * @param startCoord
	 * @return : List<MapKey> :
	 * @author 李志楷 2015-9-2 下午5:33:10
	 */
	private List<MapKey> getAroundKeys(MapKey[] keys,
			Map<MapKey, double[]> pointsMap, double[] startCoord) {
		List<MapKey> keyList = new LinkedList<MapKey>();
		for (int i = 0; i < keys.length; i++) {
			if (pointsMap.containsKey(keys[i])) {
				keyList.add(keys[i]);
			}
		}
		return keyList;
	}

	/**
	 * TODO :线追踪，追踪原则为闭合优先
	 * 
	 * @return : List<double[]> :
	 * @author 李志楷 2015-9-2 下午5:35:34
	 */
	private List<double[]> trackLineClosedFirst() {
		List<double[]> lineList = new LinkedList<double[]>();

		return lineList;
	}

	private List<int[]> getArrayIndexByFloatCoord(double[] coord) {
		List<int[]> indexList = new LinkedList<int[]>();
		indexList.add(new int[] { (int) Math.floor(coord[0]),
				(int) Math.floor(coord[1]) });
		// 点在列边上
		if (Math.floor(coord[0]) == coord[0]) {
			// 点也在行边上，说明点正好处于网格点交汇处
			if (Math.floor(coord[1]) == coord[1]) {
				if (coord[0] - 1 >= 0) {
					indexList.add(new int[] { (int) Math.floor(coord[0] - 1),
							(int) Math.floor(coord[1]) });
				}
				if (coord[1] - 1 >= 0) {
					indexList.add(new int[] { (int) Math.floor(coord[0]),
							(int) Math.floor(coord[1] - 1) });
				}
				if (coord[0] - 1 >= 0 && coord[1] - 1 >= 0) {
					indexList.add(new int[] { (int) Math.floor(coord[0] - 1),
							(int) Math.floor(coord[1] - 1) });
				}
				return indexList;
			} else {
				if (coord[0] - 1 >= 0)
					indexList.add(new int[] { (int) Math.floor(coord[0] - 1),
							(int) Math.floor(coord[1]) });
				return indexList;
			}
			// 点在行边上
		} else if (Math.floor(coord[1]) == coord[1]) {
			if (coord[1] - 1 >= 0)
				indexList.add(new int[] { (int) Math.floor(coord[0]),
						(int) Math.floor(coord[1] - 1) });
			return indexList;
		} else {
			return indexList;
		}
	}

	private boolean haveSameArrayIndex(List<int[]> indexList1,
			List<int[]> indexList2) {
		for (Iterator iterator = indexList2.iterator(); iterator.hasNext();) {
			int[] is2 = (int[]) iterator.next();
			for (Iterator iterator2 = indexList1.iterator(); iterator2
					.hasNext();) {
				int[] is1 = (int[]) iterator2.next();
				if (is2[0] == is1[0] && is2[1] == is1[1])
					return true;
			}
		}
		return false;
	}

	private boolean isIntherSameLineOrColumnEdge(double[] coord1,
			double[] coord2) {
		if ((Math.floor(coord1[0]) == coord2[0] && coord1[0] == coord2[0])
				|| (Math.floor(coord1[1]) == coord2[1] && coord1[1] == coord2[1]))
			return true;
		return false;
	}

	private boolean isIntheSameLine(double[] coord1, double[] coord2) {
		if (Math.floor(coord1[0]) == Math.floor(coord2[0]))
			return true;
		return false;
	}

	private boolean isIntheSameColumn(double[] coord1, double[] coord2) {
		if (Math.floor(coord1[1]) == Math.floor(coord2[1]))
			return true;
		return false;
	}

	private boolean isIntheSamePixel(double[] coord1, double[] coord2) {
		if (coord1 == null || coord2 == null)
			return false;
		// if (isIntherSameLineOrColumnEdge(coord1, coord2))
		// return false;
		//
		// if (isIntheSameColumn(coord1, coord2)) {
		// if (isIntheSameLine(coord1, coord2))
		// return true;
		// if (Math.abs(coord1[0] - coord2[0]) <= 1) {
		// return true;
		// } else {
		// return false;
		// }
		// } else {
		// if (isIntheSameLine(coord1, coord2)) {
		// if (Math.abs(coord1[1] - coord2[1]) <= 1) {
		// return true;
		// } else {
		// return false;
		// }
		// } else {
		// return false;
		// }
		// }
		List<int[]> indexList1 = getArrayIndexByFloatCoord(coord1);
		List<int[]> indexList2 = getArrayIndexByFloatCoord(coord2);
		return haveSameArrayIndex(indexList1, indexList2);
	}

	private boolean isIntheSamePixel(double[] coord1, double[] coord2,
			double[] coord3) {
		if (isIntheSamePixel(coord1, coord2)
				&& isIntheSamePixel(coord1, coord3)
				&& isIntheSamePixel(coord3, coord2))
			return true;

		return false;
	}

	private double calAngle(double[] headP, double[] p1, double[] p2) {
		double a = calDistance(headP, p1);
		double c = calDistance(headP, p2);
		double b = calDistance(p1, p2);

		double cosAngle = Math.acos((Math.pow(a, 2) + Math.pow(c, 2) - Math
				.pow(b, 2)) / (2 * a * c));
		return cosAngle;
	}

	/**
	 * TODO :按照等值线追踪原则获取最近点
	 * 
	 * @param keys
	 *            ：附近像元的点键对象
	 * @param lineList
	 *            ：有序点集合
	 * @param pointsMap
	 *            ：等值点无序集合
	 * @param startKey
	 *            ：起始点键对象
	 * @param startCoord
	 *            ：起始点坐标
	 * @param lastCoord
	 *            ：起始点上一点坐标，无为null
	 * @return : MapKey :下一点键对象
	 * @author 李志楷 2015-9-21 上午10:29:28
	 */
	private MapKey trackNearestKey(MapKey[] keys, List<double[]> lineList,
			Map<MapKey, double[]> pointsMap, MapKey startKey,
			double[] startCoord, double[] lastCoord) {
		double distance = 0;
		double angle = -1;
		int minIndex = -1;
		for (int i = 0; i < keys.length; i++) {
			if (pointsMap.containsKey(keys[i])) {
				if (isIntheSamePixel(startCoord, lastCoord,
						pointsMap.get(keys[i])))
					continue;
				// if (canClose(lineList,pointsMap.get(keys[i]))) return
				// keys[i];
				if (minIndex == -1) {
					// 按照等值线追踪原则计算点距离
					distance = calDistance(startKey, keys[i], startCoord,
							pointsMap.get(keys[i]));
					// 如果起始点的前一个点存在的话
					if (lastCoord != null) {
						angle = calAngle(startCoord, lastCoord,
								pointsMap.get(keys[i]));
					}
					minIndex = i;
				} else {
					// 按照等值线追踪原则计算点距离
					double tmpDis = calDistance(startKey, keys[i], startCoord,
							pointsMap.get(keys[i]));
					if (distance > tmpDis) {
						distance = tmpDis;
						minIndex = i;
					} else if (distance == tmpDis) {
						// 如果起始点的前一个点存在的话，比较两个相同距离的点与起始点构成的线段与起始点与上一点构成线段的夹角，取夹角比较小的
						if (lastCoord != null) {
							double tmpAngle = calAngle(startCoord, lastCoord,
									pointsMap.get(keys[i]));
							if (angle > tmpAngle) {
								angle = tmpAngle;
								minIndex = i;
							}
						}
					}
				}
			}

		}
		if (minIndex == -1)
			return null;

		return keys[minIndex];
	}

	private void addKeyToList(Map<MapKey, double[]> pointsMap, MapKey key,
			List<double[]> lineList, boolean tail) {
		if (tail) {
			lineList.add(pointsMap.get(key));
		} else {
			lineList.add(0, pointsMap.get(key));
		}
		pointsMap.remove(key);
	}

	/**
	 * TODO :获取行边等值点附近周围点集合
	 * 
	 * @param startKey
	 *            ：起始点的索引对象
	 * @param arrayWidth
	 *            ：矩阵的宽度
	 * @return : MapKey[] :等值点索引对象集合
	 * @author 李志楷 2015-9-21 上午10:23:41
	 */
	private MapKey[] catchArroundLineEdgePointKey(MapKey startKey,
			int arrayWidth) {
		// 边上部的点包括左上列边点、左上角点、右上列边点、右上角点、正上行边点
		MapKey leftUpperColumnKey = new MapKey(COLUMN_EDGE, startKey.getIndex()
				- arrayWidth);
		MapKey leftUpperCornerKey = new MapKey(CORNER, startKey.getIndex()
				- arrayWidth);
		MapKey rightUpperColumnKey = new MapKey(COLUMN_EDGE,
				startKey.getIndex() - arrayWidth + 1);
		MapKey ringhtUpperCornerKey = new MapKey(CORNER, startKey.getIndex()
				- arrayWidth + 1);
		MapKey upperLineKey = new MapKey(LINE_EDGE, startKey.getIndex()
				- arrayWidth);

		// 边下部点包括左下列边点、左下角点、右下列边点、右下角点、正下行边点
		MapKey leftBelowColumnKey = new MapKey(COLUMN_EDGE, startKey.getIndex());
		MapKey leftBelowCornerKey = new MapKey(CORNER, startKey.getIndex()
				+ arrayWidth);
		MapKey rightBelowColumnKey = new MapKey(COLUMN_EDGE,
				startKey.getIndex() + 1);
		MapKey ringhtBelowCornerKey = new MapKey(CORNER, startKey.getIndex()
				+ arrayWidth + 1);
		MapKey belowLineKey = new MapKey(LINE_EDGE, startKey.getIndex()
				+ arrayWidth);

		// 形成点集合
		MapKey[] keys = new MapKey[] { leftUpperColumnKey, leftUpperCornerKey,
				rightUpperColumnKey, ringhtUpperCornerKey, upperLineKey,
				leftBelowColumnKey, leftBelowCornerKey, rightBelowColumnKey,
				ringhtBelowCornerKey, belowLineKey };

		return keys;
	}

	/**
	 * TODO :获取列边等值点附近周围点集合
	 * 
	 * @param startKey
	 *            ：起始点的索引对象
	 * @param arrayWidth
	 *            ：矩阵的宽度
	 * @return : MapKey[] :等值点索引对象集合
	 * @author 李志楷 2015-9-21 上午10:23:41
	 */
	private MapKey[] catchArroundColumnEdgePointKey(MapKey startKey,
			int arrayWidth) {
		// 列左部的点包括左上行边点、左上角点、左下行边点、左下角点、正右列边点
		MapKey leftUpperLineKey = new MapKey(LINE_EDGE, startKey.getIndex() - 1);
		MapKey leftUpperCornerKey = new MapKey(CORNER, startKey.getIndex() - 1);
		MapKey leftBelowLineKey = new MapKey(LINE_EDGE, startKey.getIndex()
				+ arrayWidth - 1);
		MapKey leftBelowCornerKey = new MapKey(CORNER, startKey.getIndex()
				+ arrayWidth - 1);
		MapKey leftColumnKey = new MapKey(COLUMN_EDGE, startKey.getIndex() - 1);

		// 列右部点包括右上行边点、右上角点、右下行边点、右下角点、正下列边点
		MapKey rightUpperLineKey = new MapKey(LINE_EDGE, startKey.getIndex());
		MapKey rightUpperCornerKey = new MapKey(CORNER, startKey.getIndex() + 1);
		MapKey rightBelowLineKey = new MapKey(LINE_EDGE, startKey.getIndex()
				+ arrayWidth);
		MapKey rightBelowCornerKey = new MapKey(CORNER, startKey.getIndex()
				+ arrayWidth + 1);
		MapKey rightColumnKey = new MapKey(COLUMN_EDGE, startKey.getIndex() + 1);

		MapKey[] keys = new MapKey[] { leftUpperLineKey, leftUpperCornerKey,
				leftBelowLineKey, leftBelowCornerKey, leftColumnKey,
				rightUpperLineKey, rightUpperCornerKey, rightBelowLineKey,
				rightBelowCornerKey, rightColumnKey };
		return keys;
	}

	private MapKey[] catchArroundCornerPointKey(MapKey startKey, int arrayWidth) {
		MapKey leftUpperLineKey = new MapKey(LINE_EDGE, startKey.getIndex()
				- arrayWidth - 1);
		MapKey rightUpperLineKey = new MapKey(LINE_EDGE, startKey.getIndex()
				- arrayWidth);
		MapKey leftBelowLineKey = new MapKey(LINE_EDGE, startKey.getIndex()
				+ arrayWidth - 1);
		MapKey rightBelowLineKey = new MapKey(LINE_EDGE, startKey.getIndex()
				+ arrayWidth);

		MapKey leftUpperColumnKey = new MapKey(COLUMN_EDGE, startKey.getIndex()
				- arrayWidth - 1);
		MapKey rightUpperColumnKey = new MapKey(COLUMN_EDGE,
				startKey.getIndex() - arrayWidth + 1);
		MapKey leftBelowColumnKey = new MapKey(COLUMN_EDGE,
				startKey.getIndex() - 1);
		MapKey rightBelowColumnKey = new MapKey(COLUMN_EDGE,
				startKey.getIndex() + 1);

		MapKey leftUpperCornerKey = new MapKey(CORNER, startKey.getIndex()
				- arrayWidth - 1);
		MapKey rightUpperCornerKey = new MapKey(CORNER, startKey.getIndex()
				- arrayWidth + 1);
		MapKey leftBelowCornerKey = new MapKey(CORNER, startKey.getIndex()
				+ arrayWidth - 1);
		MapKey rightBelowCornerKey = new MapKey(CORNER, startKey.getIndex()
				+ arrayWidth + 1);

		MapKey leftCornerKey = new MapKey(CORNER, startKey.getIndex() - 1);
		MapKey rightCornerKey = new MapKey(CORNER, startKey.getIndex() + 1);
		MapKey upperCornerKey = new MapKey(CORNER, startKey.getIndex()
				- arrayWidth);
		MapKey bottomCornerKey = new MapKey(CORNER, startKey.getIndex()
				+ arrayWidth);

		MapKey[] keys = new MapKey[] { leftUpperLineKey, rightUpperLineKey,
				leftBelowLineKey, rightBelowLineKey, leftUpperColumnKey,
				rightUpperColumnKey, leftBelowColumnKey, rightBelowColumnKey,
				leftUpperCornerKey, rightUpperCornerKey, leftBelowCornerKey,
				rightBelowCornerKey, leftCornerKey, rightCornerKey,
				upperCornerKey, bottomCornerKey };
		return keys;
	}

	/**
	 * TODO :获取距离周围点的键对象数组
	 * 
	 * @param startKey
	 *            ：起始点的键
	 * @param arrayWidth
	 *            ：矩阵的宽度
	 * @return : MapKey[] :周围点键对象
	 * @author 李志楷 2015-9-21 上午10:21:40
	 */
	private MapKey[] catchAroundPointKey(MapKey startKey, int arrayWidth) {
		// 判断起始点是在矩阵的行边上还是列边上，根据点的特色查找临近的点
		if (startKey.getType() == LINE_EDGE)
			return catchArroundLineEdgePointKey(startKey, arrayWidth);
		if (startKey.getType() == COLUMN_EDGE)
			return catchArroundColumnEdgePointKey(startKey, arrayWidth);
		if (startKey.getType() == CORNER)
			return catchArroundCornerPointKey(startKey, arrayWidth);
		return null;
	}

	/**
	 * TODO :从某一点开始追踪该点所在等值线，形成一个有序点集合
	 * 
	 * @param pointsMap
	 *            ：等值点集合
	 * @param startKey
	 *            ：起始点key值
	 * @param startCoord
	 *            ：起始点坐标
	 * @param arrayWidth
	 *            ：矩阵宽度
	 * @param lineList
	 *            ：生成的有序点集合
	 * @param lastCoord
	 *            ：上一个点的坐标，如果没有上一个，则为null
	 * @param tail
	 *            :如果设为true将追踪到的点放置于队列的尾部，如果为false则放置于头部 void :
	 * @author 李志楷 2015-9-21 上午10:08:40
	 */
	private void trackLine(Map<MapKey, double[]> pointsMap, MapKey startKey,
			double[] startCoord, int arrayWidth, List<double[]> lineList,
			double[] lastCoord, boolean tail) {
		// if(startCoord[0] > 6.2 && startCoord[0] < 6.3 && startCoord[1] ==
		// 180){
		// System.out.println();
		// }
		// 获取距离该点较近的相邻像元的等值点
		MapKey[] nearestKeys = catchAroundPointKey(startKey, arrayWidth);

		// 按照等值线追踪原则获取最近的等值点
		MapKey nearestKey = trackNearestKey(nearestKeys, lineList, pointsMap,
				startKey, startCoord, lastCoord);

		if (nearestKey == null)
			return;

		double[] nextCoord = pointsMap.get(nearestKey);
		if (Math.abs(nextCoord[0] - startCoord[0]) > 2
				|| Math.abs(nextCoord[1] - startCoord[1]) > 2)
			return;

		// 将获取到的最近的等值点添加到等值点队列中
		addKeyToList(pointsMap, nearestKey, lineList, tail);

		// 判断该队列是否能够闭合，如果可以闭合则等值线追踪结束
		if((!hasMoreNearestPoint(pointsMap, nearestKey, lineList.get(lineList.size()-1), arrayWidth, lineList, null))){
			if (canClose(lineList)){
				hasMoreNearestPoint(pointsMap, nearestKey, lineList.get(lineList.size()-1), arrayWidth, lineList, null);
				return;
			}
		}

		// 如果不能闭合则继续追踪下一点，直至获取不到点或等值线可以闭合
		trackLine(pointsMap, nearestKey, lineList.get(lineList.size() - 1),
				arrayWidth, lineList, startCoord, tail);
	}

	private boolean hasMoreNearestPoint(Map<MapKey, double[]> pointsMap, MapKey startKey,
			double[] startCoord, int arrayWidth, List<double[]> lineList,
			double[] lastCoord) {
		double[] initStartCoord=lineList.get(0);
		// 获取距离该点较近的相邻像元的等值点
		MapKey[] nearestKeys = catchAroundPointKey(startKey, arrayWidth);

		// 按照等值线追踪原则获取最近的等值点
		MapKey nearestKey = trackNearestKey(nearestKeys, lineList, pointsMap,
				startKey, startCoord, lastCoord);

		if (nearestKey == null)
			return false;

		double[] nextCoord = pointsMap.get(nearestKey);
		double distanceOfHead2Tail = calDistance(startCoord,initStartCoord);
		if(distanceOfHead2Tail==0) return false;
		double distanceOfNextPoint=calDistance(startCoord,nextCoord);
		if (distanceOfHead2Tail < distanceOfNextPoint){
			return false;
			
		}else {
			return true;
		}
	}

	/**
	 * TODO :从某一点开始递归追踪该点所在的等值线
	 * 
	 * @param pointsMap
	 *            ：等值点集合
	 * @param key
	 *            ：起始点的Key
	 * @param startCoord
	 *            ：起始点的坐标
	 * @param arrayWidth
	 *            ：矩阵要素的宽度
	 * @param lineList
	 *            :等值线点队列 void :
	 * @author 李志楷 2015-9-21 上午10:05:03
	 */
	private void startTrackPoint2(Map<MapKey, double[]> pointsMap, MapKey key,
			double[] startCoord, int arrayWidth, List<double[]> lineList) {
		List<double[]> forwardList = new LinkedList<double[]>();
		forwardList.add(lineList.get(0));
		// 首先向前查找
		trackLine(pointsMap, key, startCoord, arrayWidth, forwardList, null,
				true);

		// 将向前追踪到的点队列添加至等值线点队列中
		for (int i = 1; i < forwardList.size(); i++) {
			lineList.add(0, forwardList.get(i));
		}
		if((!hasMoreNearestPoint(pointsMap, key, forwardList.get(forwardList.size()-1), arrayWidth, forwardList, null))){
			if (canClose(forwardList) ){
				return;
			}
		}

		List<double[]> backwardList = new LinkedList<double[]>();
		backwardList.add(lineList.get(lineList.size() - 1));
		// 向后查找
		trackLine(pointsMap, key, startCoord, arrayWidth, lineList, null, true);
		// // 将向后追踪到的点队列添加至等值线点队列中
		// for (int i = 1; i < backwardList.size(); i++) {
		// lineList.add(backwardList.get(i));
		// }
	}

	/**
	 * TODO :从某一点开始追踪等值线
	 * 
	 * @param pointsMap
	 *            ：等值点集合
	 * @param key
	 *            :开始点的key
	 * @param arrayWidth
	 *            ：矩阵宽度
	 * @return : List<double[]> :等值线的点序列
	 * @author 李志楷 2015-9-21 上午9:49:03
	 */
	private List<double[]> subLineOfPointsMap(Map<MapKey, double[]> pointsMap,
			MapKey key, int arrayWidth) {
		// 初始化等值线点集合
		List<double[]> linePointList = new LinkedList<double[]>();

		// 将起始点加入点集合中
		double[] startCoord = pointsMap.get(key);
		linePointList.add(startCoord);
		MapKey startKey = new MapKey(key.getType(), key.getIndex());
		// 避免同一点被追踪多次，凡是被追踪到一次，就将该点从集合中删除
		pointsMap.remove(key);

		// 递归追踪下一点
		startTrackPoint2(pointsMap, startKey, startCoord, arrayWidth,
				linePointList);

		return linePointList;
	}

	private double getValueByIndex(double[][] array, double[] coord) {
		int x = (int) Math.round(coord[0]);
		int y = (int) Math.round(coord[1]);
		return array[x][y];
	}

	/**
	 * TODO :将同一等值点的散点序列，追踪等值线，生成N条等值线队列
	 * 
	 * @param pointsMap
	 *            ：等值点无序集合
	 * @param rasterData
	 *            ：矩阵
	 * @param levelValue
	 *            ：等值线的值
	 * @return : List<List> :追踪排序后的等值线集合
	 * @author 李志楷 2015-9-21 上午9:41:34
	 */
	private List<List> linesOfPointsMap(Map<MapKey, double[]> pointsMap,
			double[][] rasterData, double levelValue) {
		int arrayWidth = rasterData[0].length;
		List<List> lineList = new LinkedList<List>();

		// 遍历等值点集合，从任意一个点开始追踪等值线，直到集合中所有点追踪完毕
		while (!pointsMap.isEmpty()) {
			Set<MapKey> keyset = pointsMap.keySet();

			// 遍历集合
			Iterator iterator = keyset.iterator();
			if (iterator.hasNext()) {
				MapKey key = (MapKey) iterator.next();

				// 查找一个点，该点索引所对应的矩阵要素的值与等值线的值不相等，从不相等的点开始追踪等值线
				while (true) {
					if (iterator.hasNext()) {
						// 如果获取到的点对应的阵要素的值与等值线的值相等，则继续获取下一个点，直到获取到不相等的点
						if (getValueByIndex(rasterData, pointsMap.get(key)) == levelValue) {
							key = (MapKey) iterator.next();
						} else {
							break;
						}
					} else {
						// 如果遍历完了所有的点，仍然没有找到满足条件的点，则等值线不再追踪
						System.out.println("没有找到剩余满足条件的点");
						return lineList;
					}
				}

				// 从某一点开始追踪等值线
				List<double[]> pointsList = subLineOfPointsMap(pointsMap, key,
						arrayWidth);

				// 将追踪得出的等值线放入等值线集合中
				if (pointsList.size() > 0)
					lineList.add(pointsList);
			}
		}
		return lineList;
	}

	private int bezPointNumbers = 100;

	/**
	 * TODO :判断某一个值value是否处于两个值（value1和value2）之间
	 * 
	 * @param value
	 *            ：输入值
	 * @param value1
	 *            ：最大值或最小值
	 * @param value2
	 *            ：最小值或最大值
	 * @return : boolean :如果在区间范围内则返回true，否则返回false
	 * @author 李志楷 2015-8-27 上午8:53:52
	 */
	private boolean isBetweenValues(double value, double value1, double value2) {
		if (value < value1 && value > value2) {
			return true;
		} else if (value > value1 && value < value2) {
			return true;
		} else {
			return false;
		}
	}

	private boolean gt(double value, double[] compareValues) {
		for (int i = 0; i < compareValues.length; i++) {
			if (value > compareValues[i])
				return true;
		}
		return false;
	}

	private boolean lt(double value, double[] compareValues) {
		for (int i = 0; i < compareValues.length; i++) {
			if (value < compareValues[i])
				return true;
		}
		return false;
	}

	private boolean eq(double value, double[] compareValues) {
		for (int i = 0; i < compareValues.length; i++) {
			if (value == compareValues[i])
				return true;
		}
		return false;
	}

	/**
	 * TODO :比较某一数值是否在数组中存在大于、等于、小于该数值的元素
	 * 
	 * @param value
	 * @param compareValues
	 * @return : boolean[]
	 *         :包含有三个元素，第一个元素表明数组中是否有大于指定数值的，第二个元素表明数组中是否有等于指定数值的，第三个元素表明数组中是否有小于指定数值的
	 * @author 李志楷 2015-8-27 下午12:38:23
	 */
	private boolean[] compareValues(double value, double[] compareValues) {
		boolean[] result = new boolean[] { false, false, false };
		for (int i = 0; i < compareValues.length; i++) {
			if (value == compareValues[i]) {
				result[1] = true;
			} else if (value < compareValues[i]) {
				result[0] = true;
			} else {
				result[2] = true;
			}
		}
		return result;
	}

	/**
	 * TODO :判断某个数值是否在数组值域范围内
	 * 
	 * @param value
	 * @param validValues
	 *            :数值数组，该数组中不存在无效值
	 * @return : boolean :
	 * @author 李志楷 2015-8-27 下午12:42:45
	 */
	private boolean isBetweenValues(double value, double[] validValues) {
		boolean[] compareResult = compareValues(value, validValues);
		if (compareResult[1])
			return true;
		if (compareResult[0] && compareResult[2])
			return true;
		return false;
	}

	/**
	 * TODO :计算数组元素中的最大值，并返回最大值以及对应的索引位置
	 * 
	 * @param values
	 * @param invalidValue
	 * @return : double[] :第一个元素为数组的最大值，第二个元素为对应数值的索引
	 * @author 李志楷 2015-8-27 下午2:46:55
	 */
	private double[] max(double[] values, double invalidValue) {
		double max = invalidValue;
		double index = -1;
		double[] result;

		for (int i = 0; i < values.length; i++) {
			if (values[i] != invalidValue) {
				if (max == invalidValue) {
					max = values[i];
					index = i;
				} else {
					if (values[i] > max) {
						max = values[i];
						index = i;
					}
				}
			}
		}

		result = new double[] { max, index };
		return result;
	}

	/**
	 * TODO :计算数组元素中的最小值，并返回最小值以及对应的索引位置
	 * 
	 * @param values
	 * @param invalidValue
	 * @return : double[] :第一个元素为数组的最小值，第二个元素为对应数值的索引
	 * @author 李志楷 2015-8-27 下午3:03:39
	 */
	private double[] min(double[] values, double invalidValue) {
		double min = invalidValue;
		double index = -1;
		double[] result;

		for (int i = 0; i < values.length; i++) {
			if (values[i] != invalidValue) {
				if (min == invalidValue) {
					min = values[i];
					index = i;
				} else {
					if (values[i] < min) {
						min = values[i];
						index = i;
					}
				}
			}
		}

		result = new double[] { min, index };
		return result;
	}

	/**
	 * TODO :
	 * 
	 * @param value
//	 * @param validValues
	 *            ：数值数组，并且该数组中不存在无效值
	 * @param xs
	 * @param ys
	 * @return :返回一个索引坐标 double[] :
	 * @author 李志楷 2015-8-27 下午12:50:57
	 */
	private double[] calValueIndexOfArray(double value, double[] values,
			double[] xs, double[] ys, double invalidValue) {
		double[] maxResult = max(values, invalidValue);
		double[] minResult = min(values, invalidValue);

		double maxValue = maxResult[0];
		double minValue = minResult[0];
		double maxX = xs[(int) maxResult[1]];
		double maxY = ys[(int) maxResult[1]];
		double minX = xs[(int) minResult[1]];
		double minY = ys[(int) minResult[1]];

		double k = (value - minValue) / (maxValue - minValue);
		double x = k * (maxX - minX) + minX;
		double y = k * (maxY - minY) + minY;
		double[] coord = new double[] { x, y };

		return coord;
	}

	/**
	 * TODO :
	 * 
	 * @param valueArray
	 * @param i
	 * @param j
	 * @param valueList
	 *            :『『x下标、y下标、值』，『x下标、y下标、值』，『x下标、y下标、值』』 void :
	 * @author 李志楷 2015-8-27 下午3:20:30
	 */
	private void addValueToListByIndex(double[][] valueArray, int i, int j,
			List<double[]> valueList) {
		if (i < 0 || j < 0 || i > valueArray.length - 1
				|| j > valueArray[0].length - 1) {
			return;
		} else {
			valueList.add(new double[] { (double) i, (double) j,
					valueArray[i][j] });
		}
	}

	/**
	 * TODO :根据传入的数组下标，获取相应下标右方、下方、右下方及本身四个元素，如果下标越界则返回不越界的元素
	 * 
	 * @param valueArray
	 * @param i
	 * @param j
	 * @return : double[][] :『『x下标1。。。』，『y下标。。。』，『值。。。』』
	 * @author 李志楷 2015-8-27 下午3:16:01
	 */
	private double[][] catch4ValuesFromArray(double[][] valueArray, int i, int j) {
		List<double[]> valueList = new LinkedList<double[]>();
		addValueToListByIndex(valueArray, i, j, valueList);
		addValueToListByIndex(valueArray, i + 1, j, valueList);
		addValueToListByIndex(valueArray, i, j + 1, valueList);
		addValueToListByIndex(valueArray, i + 1, j + 1, valueList);

		double[][] result = new double[3][valueList.size()];
		for (int k = 0; k < valueList.size(); k++) {
			double[] values = valueList.get(k);
			result[0][k] = values[0];
			result[1][k] = values[1];
			result[2][k] = values[2];
		}

		return result;
	}

	/**
	 * TODO :获取value在数组valueArray中等值点的索引集合，等值点行列号整值通过索引二维转一维计算作为集合的Key，
	 * 等值点的浮点型行列号作为集合的value
	 * 
	 * @param valueArray
	 * @param value
	 * @param invalidValue
	 * @return : Map<Integer,double[]>
	 *         :value在数组valueArray中等值点的索引集合，等值点行列号整值通过索引二维转一维计算作为集合的Key
	 *         ，等值点的浮点型行列号作为集合的value
	 * @author 李志楷 2015-8-27 下午3:35:36
	 */
	private Map<Integer, double[]> catchContourPointMap(double[][] valueArray,
			double value, double invalidValue) {
		Map<Integer, double[]> map = new HashMap<Integer, double[]>();

		for (int i = 0; i < valueArray.length - 1; i++) {
			for (int j = 0; j < valueArray[i].length; j++) {

				double[][] valuesCollection = catch4ValuesFromArray(valueArray,
						i, j);
				double[] values = valuesCollection[2];
				double[] xs = valuesCollection[0];
				double[] ys = valuesCollection[1];
				if (!isBetweenValues(value, values))
					continue;
				double[] indexD = calValueIndexOfArray(value, values, xs, ys,
						invalidValue);
				int key = (int) (Math.floor(indexD[0]) * valueArray[i].length + Math
						.floor(indexD[1]));
				map.put(key, indexD);
			}
		}

		return map;
	}

	/**
	 * TODO :根据一个点抽离出集合中与该点有关联的点，生成一个点序列列表
	 * 
	 * @param indexMap
	 * @param startKey
//	 * @param startIndexesD
	 *            : void :
	 * @author 李志楷 2015-8-27 下午3:46:31
	 */
	private void catchOneLineFromMap(Map<Integer, double[]> indexMap,
			int startKey, Map<Integer, double[]> linePointMap, int width) {
		// 根据起始点的索引值计算出其对应的行列号
		int line = startKey / width;
		int column = startKey % width;
		Set<Integer> indexKey = indexMap.keySet();

		// 根据获取到的行列号，以该点为中心，3*3为窗口，查找窗口内的数组下标索引是否存在与集合中
		for (int i = line - 1; i < line + 2; i++) {
			// 如果行号索引越界则跳过循环
			if (i < 0)
				continue;
			for (int j = column - 1; j < column + 2; j++) {
				if (j < 0 || j >= width)
					continue;

				int key = i * width + j;
				if (indexKey.contains(key)) {
					linePointMap.put(key, indexMap.get(key));
					indexMap.remove(key);
					catchOneLineFromMap(indexMap, key, linePointMap, width);
				}
			}
		}

	}

	/**
	 * TODO :查找三点队列里面最左边的点
	 * 
//	 * @param coordList
	 * @return : int :
	 * @author 李志楷 2015-8-27 下午5:04:09
	 */
	private int mostLeftCoordIndex(Map<Integer, double[]> coordMap) {
		Set<Integer> keyset = coordMap.keySet();
		double leftX = 0;
		int index = -1;
		int count = 0;
		for (Iterator iterator = keyset.iterator(); iterator.hasNext();) {
			Integer keyIndex = (Integer) iterator.next();
			double value = coordMap.get(keyIndex)[0];

			if (count == 0) {
				leftX = value;
				index = keyIndex;
			} else {
				if (leftX > value) {
					leftX = value;
					index = keyIndex;
				}
			}

			count++;
		}
		return index;
	}

	/**
	 * TODO
	 * :从散点集合中查找临近像元点（包括上、右上、右、右下，下等8个方向），如果查找到多个点按照相应优先原则匹配（两点连线与正左方夹角最小为原则）
	 * 
//	 * @param coordMap
	 * @param index
	 * @return : int :返回最近像元点在集合中的key，如果没有找到则返回-1
	 * @author 李志楷 2015-8-27 下午5:14:25
	 */
	private int findElementLeftFirst(Map<Integer, double[]> coord, int index,
			int width) {
		int line = index / width;
		int column = index % width;

		for (int i = line - 1; i < line + 2; i++) {
			if (i < 0)
				continue;
			for (int j = column - 1; j < column + 2; j++) {
				if (j < 0 || j >= width)
					continue;

				int index1D = i * width + j;
				if (coord.containsKey(index1D)) {
					return index1D;
				}
			}
		}
		return -1;
	}

	/**
	 * TODO :仅判断上下左右四个方向
	 * 
	 * @param index
	 * @param coord
	 * @param width
	 * @return : Set<Integer> :
	 * @author 李志楷 2015-8-28 下午6:37:58
	 */
	private Set<Integer> anotherPointIndexSet(int index,
			Map<Integer, double[]> coord, int width) {
		int line = index / width;
		int column = index % width;
		Set<Integer> indexSet = new HashSet<Integer>();

		for (int i = line - 1; i < line + 2; i++) {
			if (i < 0)
				continue;
			for (int j = column - 1; j < column + 2; j++) {
				if (j < 0 || j >= width)
					continue;

				if ((i == line - 1 && j == column - 1)
						|| (i == line + 1 && j == column - 1)
						|| (i == line - 1 && j == column + 1)
						|| (i == line + 1 && j == column + 1))
					continue;

				int index1D = i * width + j;
				if (coord.containsKey(index1D)) {
					indexSet.add(index1D);
				}
			}
		}
		return indexSet;
	}

	private double catchDitance(int index1, int index2, int width) {
		int line1 = index1 / width;
		int column1 = index1 % width;

		int line2 = index2 / width;
		int column2 = index2 % width;

		double distance = Math.pow((line1 - line2), 2)
				+ Math.pow((column1 - column2), 2);
		return distance;
	}

	private List<Double> catchDistanceList(int startIndex, List<Integer> index,
			int width) {
		List<Double> distanceList = new LinkedList<Double>();
		for (Iterator iterator = index.iterator(); iterator.hasNext();) {
			Integer ind = (Integer) iterator.next();
			distanceList.add(catchDitance(startIndex, ind, width));
		}
		return distanceList;
	}

	private int getMinDistanceListIndex(List<Double> distanceList) {
		double min = distanceList.get(0);
		int index = 0;
		for (int i = 0; i < distanceList.size(); i++) {
			if (distanceList.get(i) < min) {
				min = distanceList.get(i);
				index = i;
			}
		}
		return index;
	}

	private int selectNearestIndex(int startIndex, List<Integer> index,
			int width) {
		int ind = getMinDistanceListIndex(catchDistanceList(startIndex, index,
				width));

		return ind;
	}

	/**
	 * TODO :获取一个线下一个节点的位置，查找下一个点的原则是该点上下左右包含有一个临近点不含有两个点
	 * 
	 * @param startIndex
	 * @param index
	 * @param coord
	 * @param width
	 * @return : int :
	 * @author 李志楷 2015-8-28 下午6:53:48
	 */
	private int selectOneIndex(int startIndex, List<Integer> index,
			Map<Integer, double[]> coord, int width) {
		for (int i = 0; i < index.size(); i++) {
			Set<Integer> anotherIndexSet = anotherPointIndexSet(index.get(i),
					coord, width);
			for (int j = i; j < index.size(); j++) {
				if (!anotherIndexSet.contains(index.get(j)))
					anotherIndexSet.remove(index.get(j));
			}
			if (anotherIndexSet.size() <= 0)
				return index.get(i);
		}
		return selectNearestIndex(startIndex, index, width);
	}

	/**
	 * TODO :基于查找到的等值点的索引集合，逐点查询形成线集合
	 * 
	 * @param indexMap
	 *            ：等值点的索引集合
	 * @param width
	 *            ：数组矩阵宽度，列数
	 * @return : List<List> :每个元素是一个队列对象，该队列对象记录了各线段的索引坐标
	 * @author 李志楷 2015-8-27 下午4:12:01
	 */
	private List<Map> catchLinesFromMap(Map<Integer, double[]> indexMap,
			int width) {
		List<Map> lineList = new LinkedList<Map>();
		while (!indexMap.isEmpty()) {
			Map<Integer, double[]> coordMap = new HashMap<Integer, double[]>();

			Set<Integer> indexKeyset = indexMap.keySet();
			Iterator iterator = indexKeyset.iterator();
			Integer index = (Integer) iterator.next();
			coordMap.put(index, indexMap.get(index));

			indexMap.remove(index);

			catchOneLineFromMap(indexMap, index, coordMap, width);
			lineList.add(coordMap);
		}

		return lineList;
	}

	/**
	 * TODO :计算值value距离value1的权重距离
	 * 
	 * @param value
	 *            ：输入值
	 * @param value1
	 *            ：临界值1
	 * @param value2
	 *            ：临界值2
	 * @return : double :返回value距离value1的距离比例
	 * @author 李志楷 2015-8-27 上午8:55:30
	 */
	private double calDistanceByValueDiff(double value, double value1,
			double value2) {
		double distance = (value1 - value) / (value1 - value2);
		return distance;
	}

	/**
	 * TODO :计算某一个值value在一维数组中的索引位置，索引位置是指如果value值在数组中相邻两个元素值之间，则判定该值存在与这两个元素之间，
	 * 并计算距离两个元素的距离转换为一维数组下表记录于集合中
	 * 
	 * @param values
	 *            ：一维数组数据
	 * @param value
	 *            ：输入数值
	 * @return : Map<Integer,Double> :记录值在一维数组中的索引，其中key为整型索引，value为浮点型索引
	 * @author 李志楷 2015-8-27 上午8:56:53
	 */
	private Map<Integer, Double> catchIndexOfArrayValue(double[] values,
			double value) {
		Map<Integer, Double> indexMap = new HashMap<Integer, Double>();

		for (int i = 1; i < values.length; i++) {
			if (isBetweenValues(value, values[i - 1], values[i])) {
				double index = calDistanceByValueDiff(value, values[i - 1],
						values[i]) * 1 + i;
				indexMap.put(i - 1, index);
			}
		}

		return indexMap;
	}

	/**
	 * TODO :将每一行的索引转换为某一值在二维数组中的索引集合
	 * 
	 * @param indexMap
	 *            ：所要导入到的集合，key为在二维数组中的整型索引，value为double型有小数点的索引
	 * @param columnIndexMap
	 *            ：所导入的行数据的索引集合
	 * @param width
	 *            ：数组的宽度
	 * @param line
	 *            :数组的行索引 void :
	 * @author 李志楷 2015-8-27 上午9:15:07
	 */
	private void insertIndexToMap(Map<Integer, Double> indexMap,
			Map<Integer, Double> columnIndexMap, int width, int line) {
		Set<Integer> keyset = columnIndexMap.keySet();
		for (Iterator iterator = keyset.iterator(); iterator.hasNext();) {
			Integer column = (Integer) iterator.next();
			int indexInt = column + line * width;
			double indexDouble = columnIndexMap.get(column) + line * width;

			indexMap.put(indexInt, indexDouble);
		}
	}

	/**
	 * TODO :计算得出阶乘数组
	 * 
	 * @param nCtrlPoints
	 *            ：阶乘次数
	 * @return : int[] :个数为nCtrlPoints的阶乘结果数组
	 * @author 李志楷 2015-8-28 下午4:07:11
	 */
	private long[] binomialCoeffs(int nCtrlPoints) {
		long[] c = new long[nCtrlPoints + 1];
		for (int k = 0; k < c.length; k++) {
			c[k] = 1;
			for (int j = nCtrlPoints; j >= k + 1; j--) {
				c[k] = c[k] * j;
			}
			for (int j = nCtrlPoints - k; j >= 2; j--) {
				c[k] = c[k] / j;
			}
		}

		return c;
	}

	/**
	 * TODO :根据控制点计算样条曲线上某点u对应的坐标
	 * 
	 * @param u
	 * @param ctrlPts
	 * @param binomialC
	 * @return : double[] :
	 * @author 李志楷 2015-8-28 下午4:17:59
	 */
	private double[] computeBezPoint(double u, List<double[]> ctrlPts,
			long[] binomialC) {
		double[] bezCoord = new double[2];
		int nCtrlPoints = ctrlPts.size();
		int n = nCtrlPoints - 1;

		for (int k = 0; k < nCtrlPoints; k++) {
			double bezBlendFunction = binomialC[k] * Math.pow(u, k)
					* Math.pow(1 - u, n - k);
			bezCoord[0] = bezCoord[0] + ctrlPts.get(k)[0] * bezBlendFunction;
			bezCoord[1] = bezCoord[1] + ctrlPts.get(k)[1] * bezBlendFunction;
		}

		return bezCoord;
	}

	/**
	 * TODO :贝叶斯曲线插值
	 * 
	 * @param ctrlPoints
	 * @param nBezCurvePoints
	 * @return : double[][] :
	 * @author 李志楷 2015-8-28 下午4:25:48
	 */
	private List<double[]> bezier(List<double[]> ctrlPoints, int nBezCurvePoints) {
		List<double[]> curvePoints = new LinkedList<double[]>();

		long[] binomialC = binomialCoeffs(ctrlPoints.size() - 1);
		for (int k = 0; k <= nBezCurvePoints; k++) {
			double u = k * 1.0d / nBezCurvePoints;
			double[] bezPoint = computeBezPoint(u, ctrlPoints, binomialC);
			curvePoints.add(bezPoint);
		}

		return curvePoints;
	}

	private double car0(double u, double s) {
		double value = -1 * s * Math.pow(u, 3) + 2 * s * Math.pow(u, 2) - s * u;
		return value;
	}

	private double car1(double u, double s) {
		double value = (2 - s) * Math.pow(u, 3) + (s - 3) * Math.pow(u, 2) + 1;
		return value;
	}

	private double car2(double u, double s) {
		double value = (s - 2) * Math.pow(u, 3) + (3 - 2 * s) * Math.pow(u, 2)
				+ s * u;
		return value;
	}

	private double car3(double u, double s) {
		double value = s * Math.pow(u, 3) - s * Math.pow(u, 2);
		return value;
	}

	/**
	 * TODO :
	 * 
	 * @param startCoord
	 * @param endCoord
	 * @param scale
	 * @param forward
	 *            :在靠近结尾点方向扩展线上则为true,在靠近起始点方向扩展线上为false
	 * @return : double[] :
	 * @author 李志楷 2015-9-2 下午12:39:40
	 */
	private double[] getExtendPoint(double[] startCoord, double[] endCoord,
			double scale, boolean forward) {
		if (forward) {
			double x = (endCoord[0] - startCoord[0]) * scale + endCoord[0];
			double y = (endCoord[1] - startCoord[1]) * scale + endCoord[1];

			return new double[] { x, y };
		} else {
			double x = -1 * (endCoord[0] - startCoord[0]) * scale
					+ startCoord[0];
			double y = -1 * (endCoord[1] - startCoord[1]) * scale
					+ startCoord[1];

			return new double[] { x, y };
		}
	}

	private boolean isClozed(List<double[]> ctrlPoints) {
		if (ctrlPoints.size() < 3)
			return false;
		double[] startCoord = ctrlPoints.get(0);
		double[] endCoord = ctrlPoints.get(ctrlPoints.size() - 1);

		if (startCoord[0] == endCoord[0] && startCoord[1] == endCoord[1])
			return true;

		return false;
	}

	private void herimateInterpolate(List<double[]> valueList,
			double[] pForward1, double[] p, double[] pBackward1,
			double[] pBackward2, int points, double s) {
		for (int i = 0; i < points; i++) {
			double u = i * 1. / (points - 1);
			double x = pForward1[0] * car0(u, s) + p[0] * car1(u, s)
					+ pBackward1[0] * car2(u, s) + pBackward2[0] * car3(u, s);
			double y = pForward1[1] * car0(u, s) + p[1] * car1(u, s)
					+ pBackward1[1] * car2(u, s) + pBackward2[1] * car3(u, s);

			double[] coord = new double[] { x, y };
			valueList.add(coord);
		}
	}

	private List<double[]> herimateMain(List<double[]> ctrlPoints, double t,
			int nBezCurvePoints) {
		List<double[]> smoothLinePointList = new LinkedList<double[]>();
		double s = (1 - t) / 2;
		for (int i = 1; i < ctrlPoints.size() - 2; i++) {
			herimateInterpolate(smoothLinePointList, ctrlPoints.get(i - 1),
					ctrlPoints.get(i), ctrlPoints.get(i + 1),
					ctrlPoints.get(i + 2), nBezCurvePoints, s);
		}

		return smoothLinePointList;
	}

	private List<double[]> herimateProcess(List<double[]> ctrlPoints, double t,
			int nBezCurvePoints) {

		if (isClozed(ctrlPoints)) {
			ctrlPoints.add(0, ctrlPoints.get(ctrlPoints.size() - 2));
			ctrlPoints.add(ctrlPoints.get(1));
			return herimateMain(ctrlPoints, t, nBezCurvePoints);
		} else {
			ctrlPoints.add(
					0,
					getExtendPoint(ctrlPoints.get(0), ctrlPoints.get(1), 0.5,
							false));
			ctrlPoints.add(getExtendPoint(
					ctrlPoints.get(ctrlPoints.size() - 2),
					ctrlPoints.get(ctrlPoints.size() - 1), 0.5, true));
			return herimateMain(ctrlPoints, t, nBezCurvePoints);
		}

	}

	/**
	 * TODO :
	 * 
	 * @param indexList
	 *            :数组队列，每一个元素是由两个元素的数组组成，第一个元素为行号，第二个元素为列号
	 * @param prjValues
	 *            :[起始纬度、起始经度、纬度分辨率、经度分辨率] void :
	 * @author 李志楷 2015-8-28 下午4:47:23
	 */
	private void arrayIndexToPrjCoord(List<double[]> indexList,
			double[] prjValues) {
		for (int i = 0; i < indexList.size(); i++) {
			double[] index = indexList.get(i);

			double line = index[0];
			double column = index[1];
			// 列号转经度
			index[0] = prjValues[0] + (column + 0.5) * prjValues[3];
			// 行号转纬度
			index[1] = prjValues[1] - (line + 0.5) * prjValues[2];
			if (index[0] > 122 && index[0] < 126 && index[1] > 70 && index[1] < 75) {
				System.out.println();
			}
			indexList.set(i, index);
		}
	}

	private boolean canClose(List<double[]> pointList, double[] coord) {
		if (pointList.size() < 3)
			return false;
		double[] startPoint = pointList.get(0);
		double[] endPoint = coord;
		double distance = calDistance(startPoint, endPoint);
		if (distance <= Math.sqrt(2)) {
			return true;
		}
		return false;
	}

	private boolean canClose(List<double[]> pointList) {
		if (pointList.size() <= 3)
			return false;
		double[] startPoint = pointList.get(0);
		double[] endPoint = pointList.get(pointList.size() - 1);

		double distance = calDistance(startPoint, endPoint);
		if (Math.abs(startPoint[0] - endPoint[0]) > 1)
			return false;
		if (Math.abs(startPoint[1] - endPoint[1]) > 1)
			return false;
		double lat=90 - 0.5 - endPoint[0];
		double lon=endPoint[1] + 0.5;
		if (lat > 52 && lat < 56 && lon > 105
				&& lon < 115) {
			// if(startPoint[0] > 43 && startPoint[0] < 47 && startPoint[1] >
			// 175 && startPoint[1] < 179){
			System.out.println("Start X:" + startPoint[0] + "     End X:"
					+ endPoint[0]);
			System.out.println("Start Y:" + startPoint[1] + "     End Y:"
					+ endPoint[1]);
			System.out.println("Start lat:" + (90 - 0.5 - startPoint[0])
					+ "     End lat:" + (90 - 0.5 - endPoint[0]));
			System.out.println("Start lon:" + (startPoint[1] + 0.5)
					+ "     End lon:" + (endPoint[1] + 0.5));
			System.out.println("Distance:" + distance);
			System.out.println();
		}
		if (distance <= Math.sqrt(2)) {
			return true;
		}
		return false;
	}

	private boolean canClose(double[] startPoint, double[] endPoint) {
		double distance = calDistance(startPoint, endPoint);
		if (Math.abs(startPoint[0] - endPoint[0]) > 1)
			return false;
		if (Math.abs(startPoint[1] - endPoint[1]) > 1)
			return false;
		if (distance <= Math.sqrt(2)) {
			return true;
		}
		return false;
	}

	private boolean isClosed(List<double[]> pointList) {
		double[] startC = pointList.get(0);
		double[] endC = pointList.get(pointList.size() - 1);
		if (startC[0] == endC[0] && startC[1] == endC[1]) {
			return true;
		} else {
			return false;
		}
	}

	private boolean isOnTheArrayEdge(double[] coord, int width, int height) {

		if (Math.floor(coord[0]) == coord[0] && coord[0] == 0) {
			return true;
		} else if (Math.floor(coord[0]) == coord[0] && coord[0] == height - 1) {
			return true;
		} else if (Math.floor(coord[1]) == coord[1] && coord[1] == 0) {
			return true;
		} else if (Math.floor(coord[1]) == coord[1] && coord[1] == width - 1) {
			return true;
		} else
			return false;
	}

	/**
	 * TODO :
	 * 
	 * @param coord
	 * @param width
	 * @param height
	 * @return : int :矩阵上下分别为2x，4x矩阵左右分别为x2、x4，其他为0
	 * @author 李志楷 2015-9-21 上午11:39:00
	 */
	private int getArrayAdgeType(double[] coord, int width, int height) {
		int ten = 1;
		int one = 1;
		if (Math.floor(coord[0]) == coord[0] && coord[0] == 0) {
			ten = 2;
		} else if (Math.floor(coord[0]) == coord[0] && coord[0] == height - 1) {
			ten = 4;
		}
		if (Math.floor(coord[1]) == coord[1] && coord[1] == 0) {
			one = 2;
		} else if (Math.floor(coord[1]) == coord[1] && coord[1] == width - 1) {
			one = 4;
		}

		return ten * 10 + one;
	}

	private boolean subListForward(List<double[]> lineList, int index) {
		boolean flag = false;
		int endIndex = -1;
		for (int i = index + 4; i < lineList.size(); i++) {
			if (canClose(lineList.get(index), lineList.get(i))) {
				flag = true;
				endIndex = i;
				break;
			}
		}

		if (flag) {
			for (int i = 0; i < index; i++) {
				lineList.remove(i);
			}
			for (int i = endIndex - index; i < lineList.size(); i++) {
				lineList.remove(i);
			}
			lineList.add(lineList.get(0));
		}
		return flag;
	}

	private boolean subListBackward(List<double[]> lineList, int index) {
		boolean flag = false;
		int endIndex = -1;
		for (int i = index - 4; i >= 0; i--) {
			if (canClose(lineList.get(index), lineList.get(i))) {
				flag = true;
				endIndex = i;
				break;
			}
		}

		if (flag) {
			for (int i = index; i < lineList.size(); i++) {
				lineList.remove(i);
			}
			for (int i = 0; i < endIndex; i++) {
				lineList.remove(i);
			}
		}
		return flag;
	}

	private void processUnexecptionLineList(List<double[]> lineList) {
		boolean flag = subListForward(lineList, 0);
		if (flag)
			return;
		subListBackward(lineList, lineList.size() - 1);
	}

	private void closeLine(List<double[]> pointList, int width, int height) {
		if (pointList.size() < 2)
			return;
		double[] startPoint = pointList.get(0);
		double[] endPoint = pointList.get(pointList.size() - 1);

		// 判断起始点和终止点两点的距离，如果距离满足条件则对线进行闭合处理
		double distance = calDistance(startPoint, endPoint);
		if ((Math.abs(startPoint[0] - endPoint[0]) <= 1 && Math
				.abs(startPoint[1] - endPoint[1]) <= 1)
				|| distance <= Math.sqrt(2)) {
			pointList.add(startPoint);
			return;
		}

		// 判断线的起始点和终止点是否都在矩阵的边上，如果在边上则不进行闭合处理
		boolean startOnEdge = isOnTheArrayEdge(startPoint, width, height);
		boolean endOnEdge = isOnTheArrayEdge(endPoint, width, height);
		if (startOnEdge && endOnEdge) {
			return;
		} else {

			if (!startOnEdge && !endOnEdge) {
				System.out.println("线异常");
				pointList.add(startPoint);
				// processUnexecptionLineList(pointList);
			}
		}
	}

	private void unionNearestPointLine(List<double[]> pointList, int startIndex) {
		for (int i = startIndex + 1; i < pointList.size() - 2; i++) {
			double distance = calDistance(pointList.get(i),
					pointList.get(i + 1));
			double distance1 = calDistance(pointList.get(i - 1),
					pointList.get(i));
			double distance2 = calDistance(pointList.get(i + 1),
					pointList.get(i + 2));
			double distance3 = calDistance(pointList.get(i - 1),
					pointList.get(i + 1));

			// 如果第二个点与第三个点之间的距离小于第一、二点距离及第三、四点距离最大值的1/5，则认为第二点与第三点距离相对较近，则删除第三点，第三点与第二点合并为一点
			double scale = Math.max(distance1, distance2) / distance;
			System.out.println(scale);
			if (scale > 5) {
				pointList.remove(i + 1);
				// 如果前三个点构成的夹角小于5度的时候，则去除第二个点
				double cosAngle = (Math.pow(distance, 2)
						+ Math.pow(distance1, 2) - Math.pow(distance3, 2))
						/ (2 * distance * distance1);
				if (cosAngle > Math.cos(10. / 180 * Math.PI)) {
					pointList.remove(i);
				}
				unionNearestPointLine(pointList, i - 1);
				break;
			} else {
				// 如果前三个点构成的夹角小于5度的时候，则去除第二个点
				double cosAngle = (Math.pow(distance, 2)
						+ Math.pow(distance1, 2) - Math.pow(distance3, 2))
						/ (2 * distance * distance1);
				if (cosAngle > Math.cos(30. / 180 * Math.PI)) {
					pointList.remove(i);
					unionNearestPointLine(pointList, i - 1);
				}
			}
		}
	}

	private void insertValueToList(double[][] rasterData, int line, int column,
			List<Double> valueList) {
		try {
			valueList.add(rasterData[line][column]);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	private double[] getAroundValues(double[][] rasterData, int line, int column) {
		List<Double> valueList = new LinkedList<Double>();
		insertValueToList(rasterData, line - 1, column, valueList);
		insertValueToList(rasterData, line, column - 1, valueList);
		insertValueToList(rasterData, line, column + 1, valueList);
		insertValueToList(rasterData, line + 1, column, valueList);

		if (valueList.size() <= 0)
			return null;

		double[] result = new double[valueList.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = valueList.get(i);
		}

		return result;
	}

	private double resetValue(double[] values, double levelValue, double value) {
		// 是否有比levelValue
		// boolean min = true;
		// boolean max = true;
		// for (int i = 0; i < values.length; i++) {
		// if (values[i] >= levelValue) {
		// min = false;
		// }
		// if (values[i] <= levelValue) {
		// max = false;
		// }
		// }
		//
		// if (min || max) {
		// double sum = 0;
		// for (int i = 0; i < values.length; i++) {
		// sum = sum + values[i];
		// }
		// double mean = sum / values.length;
		// return mean;
		// }

		double sum = 0;
		for (int i = 0; i < values.length; i++) {
			sum = sum + (values[i] - levelValue);
		}

		value = sum / values.length / 10. + value;

		return value;
	}

	private void preProcess(double[][] rasterData, double[] levelValues) {
		double[][] newRasterData = new double[rasterData.length][rasterData[0].length];
		for (int i = 0; i < newRasterData.length; i++) {
			newRasterData[i] = rasterData[i];
		}

		for (int i = 0; i < rasterData.length; i++) {
			for (int j = 0; j < rasterData[i].length; j++) {
				for (int k = 0; k < levelValues.length; k++) {
					if (rasterData[i][j] != levelValues[k])
						continue;
					newRasterData[i][j] = resetValue(
							getAroundValues(rasterData, i, j), levelValues[k],
							rasterData[i][j]);
				}
			}
		}
		rasterData = newRasterData;
	}

	private double[] getRasterCornerCoord(double[] coord,
			double[][] rasterData, double levelValue, int edgeType) {
		int width = rasterData[0].length;
		int height = rasterData.length;
		if ((edgeType / 10) == (edgeType % 10))
			return null;

		if (edgeType > 40) {
			if (rasterData[(int) Math.floor(coord[0])][(int) Math
					.floor(coord[1])] > levelValue) {
				return new double[] { height - 1, width - 1 };
			} else {
				return new double[] { height - 1, 0 };
			}
		} else if (edgeType > 20) {
			if (rasterData[(int) Math.floor(coord[0])][(int) Math
					.floor(coord[1])] > levelValue) {
				return new double[] { 0, width - 1 };
			} else {
				return new double[] { 0, 0 };
			}
		} else if (edgeType > 12) {
			if (rasterData[(int) Math.floor(coord[0])][(int) Math
					.floor(coord[1])] > levelValue) {
				return new double[] { height - 1, width - 1 };
			} else {
				return new double[] { 0, width - 1 };
			}
		} else if (edgeType > 11) {
			if (rasterData[(int) Math.floor(coord[0])][(int) Math
					.floor(coord[1])] > levelValue) {
				return new double[] { 0, 0 };
			} else {
				return new double[] { height - 1, 0 };
			}
		} else {
			return null;
		}
	}

	private void closeList(List<double[]> lineList, double[][] rasterData,
			double levelValue) {
		double[] startPoint = lineList.get(0);
		double[] endPoint = lineList.get(lineList.size() - 1);
		int width = rasterData[0].length;
		int height = rasterData.length;

		// 判断线的起始点和终止点是否都在矩阵的边上，如果在边上则不进行闭合处理
		int startEdgeType = getArrayAdgeType(startPoint, width, height);
		int endEdgeType = getArrayAdgeType(endPoint, width, height);
		if (startEdgeType != 11 && endEdgeType != 11) {
			// 判断线的端点与矩阵定点的关系，并根据关系闭合线
			if (startEdgeType == endEdgeType) {
				// 如果起始点都在同一侧边，则直接首尾闭合
				lineList.add(lineList.get(0));
			} else {
				double[] startCornerCoord = getRasterCornerCoord(startPoint,
						rasterData, levelValue, startEdgeType);
				if (startCornerCoord != null)
					lineList.add(0, startCornerCoord);
				double[] endCornerCoord = getRasterCornerCoord(endPoint,
						rasterData, levelValue, endEdgeType);
				if (endCornerCoord != null)
					lineList.add(endCornerCoord);
				lineList.add(lineList.get(0));
			}
		}
	}

	public Map<Double, List> contourPolygon(double[][] rasterData,
			double[] levelValues, double[] prjValues, double invalidValue) {
		Map<Double, List> lineListMap = contourLine(rasterData, levelValues,
				prjValues, invalidValue);
		Map<Double, List> polygonListMap = new HashMap<Double, List>();

		for (int i = 0; i < levelValues.length; i++) {
			List<List> polygonList = new LinkedList<List>();
			List lineList = lineListMap.get(levelValues[i]);
			for (Iterator iterator = lineList.iterator(); iterator.hasNext();) {
				List<double[]> pointList = (List<double[]>) iterator.next();
				if (isClosed(pointList)) {
					polygonList.add(pointList);
					continue;
				} else {
					closeList(pointList, rasterData, levelValues[i]);
					polygonList.add(pointList);
				}
			}
			if (polygonList.size() > 0) {
				polygonListMap.put(levelValues[i], polygonList);
			}

		}

		return polygonListMap;
	}

	public Map<Double, List> contourLine(double[][] rasterData,
			double[] levelValues, double[] prjValues, double invalidValue) {
		Map<Double, List> contourLineMap = new HashMap<Double, List>();

		// 对栅格数据进行预处理，使得所有与等值分界点相等的独立点等于周围点的均值
		preProcess(rasterData, levelValues);
		// 逐级获取等值线
		for (int i = 0; i < levelValues.length; i++) {
			// 获取等值点集合
			Map<MapKey, double[]> pointsMap = getContourPointsMap(rasterData,
					levelValues[i], invalidValue);
			// 根据等值点追踪等值线，生成等值线队列
			List<List> contourLineList = linesOfPointsMap(pointsMap,
					rasterData, levelValues[i]);

			List<List> lineList = new LinkedList<List>();

			// 循环等值线队列，对等值先做平滑处理
			for (int j = 0; j < contourLineList.size(); j++) {
				List<double[]> tmpList = contourLineList.get(j);
				if (tmpList.size() < 4)
					continue;
				 // 删除合并冗余点
				 unionNearestPointLine(tmpList, 0);
				 // 闭合相应的线段
				 closeLine(tmpList, rasterData[0].length, rasterData.length);
				
				 // 平滑处理
				 // List<double[]> bezPointsList = bezier(tmpList,
				 // bezPointNumbers);
				 List<double[]> herimatePointsList = herimateProcess(tmpList,
				 0,
				 20);
				 // 将数组的索引坐标转换为地图投影坐标
				 arrayIndexToPrjCoord(herimatePointsList, prjValues);
				
				 lineList.add(herimatePointsList);

//				arrayIndexToPrjCoord(tmpList, prjValues);
//
//				lineList.add(tmpList);
			}

			contourLineMap.put(levelValues[i], lineList);

			/*
			 * // 获取无序等值点 Map<Integer, double[]> indexMap =
			 * catchContourPointMap(rasterData, levelValues[i], invalidValue);
			 * // 将位于一条线上的点进行聚类 List<Map> coordMapList =
			 * catchLinesFromMap(indexMap, rasterData[0].length);
			 * 
			 * // 将同一条线上的点进行排序，并平滑处理 for (int j = 0; j < coordMapList.size();
			 * j++) { Map coordMap = coordMapList.get(j); // 线上点排序
			 * List<double[]> ctrlPoints = synchronizerPoints(coordMap,
			 * rasterData[0].length); // 平滑处理 List<double[]> bezPointsList =
			 * bezier(ctrlPoints, bezPointNumbers); // 将数组的索引坐标转换为地图投影坐标 //
			 * arrayIndexToPrjCoord(ctrlPoints,prjValues);
			 * 
			 * lineList.add(ctrlPoints); }
			 */

			// contourLineMap.put(levelValues[i], lineList);

		}

		return contourLineMap;
	}

	public static void main(String[] args) {
		ContourTool tool = new ContourTool();
		double[] c0 = new double[] { 0.5, 0 };
		double[] c1 = new double[] { 1, 0.5 };
		double[] c2 = new double[] { 0.5, 1 };
		System.out.println(tool.isIntheSamePixel(c0, c1, c2));
		// double[] c3=new double[]{0,3};
		// List<double[]> pointList=new LinkedList<double[]>();
		// pointList.add(c0);
		// pointList.add(c1);
		// pointList.add(c2);
		// pointList.add(c3);
		//
		// System.out.println(Math.cos(5./180*Math.PI));
		//
		// tool.unionNearestPointLine(pointList, 0);
//		double[][] values = new double[][] { { 3, 3, 3, 3, 3, 3, 3, 3, 3, 3 },
//				{ 3, 3, 1, 3, 3, 3, 3, 3, 3, 3 },
//				{ 3, 1, 3, 3, 3, 3, 3, 3, 3, 3 },
//				{ 3, 3, 3, 3, 3, 3, 3, 3, 3, 3 } };
		String filePath = "D:\\fl\\datas\\txt\\ecmwf/pr_999_ecmwf_2025032020_066.txt";
		double[][] values = GribUtil.readGribDatasFromTxt(filePath);
		double[] levelValues = new double[] { 0.1, 2.5, 10, 25, 50, 100, 250};
		double[] prjValues = new double[] { 70, 25, 0.05, 0.05 };
		// 起始经纬度,经纬度格局
//		double[] prjValues = new double[] { t13000.getLon_s(), t13000.getLat_s(), t13000.getLon_con(),
//				t13000.getLat_con() };
		Map<Double, List> lineMap = tool.contourLine(values, levelValues,
				prjValues, -1);
		System.out.println(lineMap);
//		ShapeCreate.createLineShapeFromCoordList(lineMap,
//				"/mnt/hgfs/dat/test9.shp");
	}
}
