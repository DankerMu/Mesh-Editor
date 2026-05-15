package com.station.cut;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;

import com.util.GribFileReaderUtil;
import com.util.GribUtil;

public class CutDataThread implements Runnable {

	private String dataType;
	private List<File> inputFiles;
	private String outputDir;
	private String prefix;
	private String[] elements;
	private String[] lonlat;
	
	public CutDataThread(String dataType, List<File> inputFiles, String outputDir, String prefix, String[] elements, String[] lonlat) {
		this.dataType = dataType;
		this.inputFiles = inputFiles;
		this.outputDir = outputDir;
		this.prefix = prefix;
		this.elements = elements;
		this.lonlat = lonlat;
	}
	
	@Override
	public void run() {
		
		for(File f : inputFiles)
		{
			cut(dataType, f.getAbsolutePath(), outputDir + f.getParentFile().getName() + File.separator + f.getName(), prefix, elements, lonlat);
		}
	}

	@SuppressWarnings("deprecation")
	public static void cut(String dataType, String inputFilePath, String outputFilePath, String prefix, String[] elements, String[] lonlat)
	{
		try {
            // 1. 打开原始文件
            NetcdfFile ncFile = NetcdfFiles.open(inputFilePath);
            Map<String, Object> datasMap = GribFileReaderUtil.getDatasMap(inputFilePath);
            
            Variable lonVar = (Variable) datasMap.get(prefix + lonlat[0]);
            Variable latVar = (Variable) datasMap.get(prefix + lonlat[1]);
            if(lonVar == null || latVar == null)
            {
            	return;
            }
            Array latData = latVar.read();
            Array lonData = lonVar.read();
            File out = new File(outputFilePath);
            if(!out.getParentFile().exists())
            {
            	out.getParentFile().mkdirs();
            }
            // 5. 创建新的NetCDF文件写入器
            NetcdfFileWriter writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, outputFilePath, null);
            
            List<Object[]> list = new ArrayList<>();
            Set<Dimension> set = new HashSet<>();
            for(String element : elements)
            {
            	if(element.contains("$"))
                {
                    int[] disVtiAndVti = GribUtil.getDisVtiAndVtiStation(inputFilePath, dataType);
//                    str = str.replace("$", String.format("%03d", disVtiAndVti[1]));
                    element = element.replace("$", disVtiAndVti[1] + "");
                }
            	
            	// 2. 查找目标变量
            	Variable targetVariable = (Variable) datasMap.get(prefix + element);
            	if(targetVariable == null)
            	{
            		continue;
            	}
            	// 3. 读取变量数据
            	Array targetData = targetVariable.read();
            	// 6. 添加维度（包括经纬度维度）
            	List<Dimension> dimensions = targetVariable.getDimensions();
            	for (Dimension dim : dimensions) 
            	{
            		if(!set.contains(dim))
            		{
            			set.add(dim);
            			writer.addDimension(null, dim.getShortName(), dim.getLength(), dim.isUnlimited(), dim.isVariableLength());
            		}
            	}
//            	dataVariable = ncFile.addVariable(null, elementName, DataType.DOUBLE, dims);
            	// 7. 在新文件中创建变量（包括目标变量和经纬度变量）
            	Variable newTargetVar = writer.addVariable(null, targetVariable.getShortName(), targetVariable.getDataType(), targetVariable.getDimensionsString());
            	copyAttributes(targetVariable, newTargetVar, writer);
            	
            	// 10. 创建文件并写入数据
            	list.add(new Object[]{newTargetVar, targetData});
            }
            if(list.size() == 0)
            {
            	return;
            }

            // 创建经纬度变量
            System.out.println(inputFilePath);
            Variable newLatVar = writer.addVariable(null, latVar.getShortName(), latVar.getDataType(), latVar.getDimensionsString());
            Variable newLonVar = writer.addVariable(null, lonVar.getShortName(), lonVar.getDataType(), lonVar.getDimensionsString());

            // 8. 复制变量属性
            copyAttributes(latVar, newLatVar, writer);
            copyAttributes(lonVar, newLonVar, writer);
            
            Variable newV = null;
            Array array = null;
            if("grapes".endsWith(dataType))
            {
            	Variable v = (Variable) datasMap.get(prefix + lonlat[2]);
            	array = v.read();
            	newV = writer.addVariable(null, v.getShortName(), v.getDataType(), v.getDimensionsString());
            	copyAttributes(v, newV, writer);
            }

            // 9. 可选：复制全局属性
            List<Attribute> globalAttributes = ncFile.getGlobalAttributes();
            for (Attribute attr : globalAttributes) 
            {
                writer.addGroupAttribute(null, attr);
            }

            writer.create();
            
            for(Object[] objs : list)
            {
            	writer.write((Variable)objs[0], (Array)objs[1]);
            }
            
            writer.write(newLatVar, latData);
            writer.write(newLonVar, lonData);
            if(array != null && newV != null)
            {
            	writer.write(newV, array);
            }
            
            // 9. 关闭资源
            writer.close();
            ncFile.close();
            
//            System.out.println("成功提取变量 '" + targetVariableName + "' 到文件: " + outputFilePath);
            
        } catch (IOException e) {
            System.err.println("文件读写错误: " + e.getMessage());
            e.printStackTrace();
        } catch (InvalidRangeException e) {
            System.err.println("数据范围错误: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("发生意外错误: " + e.getMessage());
            e.printStackTrace();
        }
	}
	
	/**
     * 复制变量属性
     */
    @SuppressWarnings("deprecation")
	private static void copyAttributes(Variable sourceVar, Variable targetVar, NetcdfFileWriter writer) {
        List<Attribute> attributes = sourceVar.getAttributes();
        for (Attribute attr : attributes) {
            writer.addVariableAttribute(targetVar, attr);
        }
    }
}
