package com.util;

import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GribFileReaderUtil {
	public static Map<String, Object> getDatasMap(String filePath)
    {
        Map<String, Object> result = new HashMap<>();
        try {
        	File file = new File(filePath);
            if(!file.exists())
            {
                System.out.println("文件不存在: " + filePath);
                return  result;
            }
            if(file.length() == 0)
            {
                System.out.println("文件大小不能为: 0, 文件路径是: " + filePath);
                return  result;
            }
//            NetcdfFile netcdfFile = NetcdfFiles.open(filePath);
            NetcdfFile ncFile = NetcdfFiles.open(filePath);
            Group rootGroup = ncFile.getRootGroup();
            List<Group> groups = rootGroup.getGroups();
            if(groups.size() == 0)
            {
                List<Variable> variables = ncFile.getVariables();
                for(Variable var : variables)
                {
                    result.put(var.getName().toLowerCase(), var);
                }
            }
            else
            {
                String groupName = null;
                for(int i = 0, count = groups.size(); i < count; i++)
                {
                    groupName = groups.get(i).getName().toLowerCase();
                    List<Variable> list = groups.get(i).getVariables();
                    for(Variable v : list)
                    {
                        String name = v.getShortName().toLowerCase();
                        result.put(groupName + ":" + name, v);
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return result;
        }

    }
}
