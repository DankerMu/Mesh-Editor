package com.tool;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import cn.hutool.core.io.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class ExcelTool {
	
	public static Workbook readExcel(String filePath)
	{
		String suffix = FileUtil.getSuffix(filePath);
		File file = new File(filePath);
		Workbook workbook = null;
		if(!file.exists())
		{
			System.out.println(filePath + " 文件不存在!");
			return workbook;
		}
		try (InputStream input = new FileInputStream(file)){
			if(suffix.equals("xls"))
			{
				workbook = new HSSFWorkbook(input);
			}
			else if(suffix.equals("xlsx"))
			{
				workbook = new XSSFWorkbook(input);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return workbook;
	}

    public static byte[] createSimpleExcel(List<String> lines)
    {
        byte[] result = null;
        int columnCount = lines.get(0).split(",").length;
        int rowCount = lines.size();
        try (Workbook workbook = new XSSFWorkbook()) {
            // 2. 创建工作表
            Sheet sheet = workbook.createSheet("数据报表");
            // 设置单元格样式（可选）
            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setAlignment(HorizontalAlignment.CENTER); // 水平居中
            cellStyle.setVerticalAlignment(VerticalAlignment.CENTER); // 垂直居中

            for(int i = 0; i < rowCount; i++)
            {
                Row row = sheet.createRow(i);
                String[] split = lines.get(i).split(",");
                for (int j = 0; j < columnCount; j++) {
                    Cell cell = row.createCell(j);
                    cell.setCellValue(split[j]);
                    cell.setCellStyle(cellStyle);
                }
            }

            // 6. 写入文件
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            result = outputStream.toByteArray();

//            try (FileOutputStream outputStream1 = new FileOutputStream("Report.xlsx")) {
//                workbook.write(outputStream1);
//                System.out.println("Excel 文件生成成功！");
//            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        return result;
    }
    public static byte[] createSimpleExcel(Map<String, List<String>> mapLines)
    {
        byte[] result = null;
        try (Workbook workbook = new XSSFWorkbook()) {
            for(String key : mapLines.keySet())
            {
                int columnCount = mapLines.get(key).get(0).split(",").length;
                int rowCount = mapLines.get(key).size();
                // 2. 创建工作表
                Sheet sheet = workbook.createSheet(key);
                // 设置单元格样式（可选）
                CellStyle cellStyle = workbook.createCellStyle();
                cellStyle.setAlignment(HorizontalAlignment.CENTER); // 水平居中
                cellStyle.setVerticalAlignment(VerticalAlignment.CENTER); // 垂直居中

                for(int i = 0; i < rowCount; i++)
                {
                    Row row = sheet.createRow(i);
                    String[] split = mapLines.get(key).get(i).split(",", -1);
                    for (int j = 0; j < columnCount; j++) {
                        Cell cell = row.createCell(j);
                        cell.setCellValue(split[j]);
                        cell.setCellStyle(cellStyle);
                    }
                }
            }

            // 6. 写入文件
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            result = outputStream.toByteArray();

//            try (FileOutputStream outputStream1 = new FileOutputStream("E:\\fl/Report.xlsx")) {
//                workbook.write(outputStream1);
//                System.out.println("Excel 文件生成成功！");
//            }

        } catch (IOException e) {
            e.printStackTrace();
        }


        return result;
    }

    public static byte[] createExcel(List<String> lines, Map<String, String> elementNameMap, int headCount)
    {
        byte[] result = null;
        int columnCount = lines.get(2).split(",").length;
        int elementColumnStep = 0;
        int vtiColumnStep = 0;

        if(lines.get(1).contains("240"))
        {
            elementColumnStep = columnCount / lines.get(0).split(",").length;
            vtiColumnStep = elementColumnStep / 2;
        }
        else
        {
            elementColumnStep = columnCount / lines.get(0).split(",").length;
            vtiColumnStep = elementColumnStep;
        }

        int[] columnStep = {elementColumnStep, vtiColumnStep};
        // 1. 创建工作簿（.xlsx格式）
        try (Workbook workbook = new XSSFWorkbook()) {
            // 2. 创建工作表
            Sheet sheet = workbook.createSheet("数据报表");
            // 设置单元格样式（可选）
            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setAlignment(HorizontalAlignment.CENTER); // 水平居中
            cellStyle.setVerticalAlignment(VerticalAlignment.CENTER); // 垂直居中

            for(int i = 0; i < 3; i++)
            {
                Row row = sheet.createRow(i);
                for (int j = 0; j < columnCount; j++) {
                    Cell cell = row.createCell(j);
                    cell.setCellValue(j);
                    cell.setCellStyle(cellStyle);
                }
            }

            CellRangeAddress region = null;
            for(int j = 0; j < headCount; j++)
            {
                String[] elements = lines.get(j).split(",");
                for(int i = 0, count = elements.length; i < count; i++)
                {
                    region = new CellRangeAddress(j, j, 1 + i * columnStep[j],i * columnStep[j] + columnStep[j]);
                    sheet.addMergedRegion(region);
                    // 获取合并区域的左上角单元格
                    Cell cell = getFirstCell(sheet, region);
                    cell.setCellValue(elementNameMap.get(elements[j]));
                    cell.setCellStyle(cellStyle);
                }
            }
            region = new CellRangeAddress(0, 2, 0,0);
            sheet.addMergedRegion(region);
            Cell cell = getFirstCell(sheet, region);
            cell.setCellValue("站点");


            for(int i = headCount, num = lines.size(); i < num; i++)
            {
                Row dataRow = sheet.createRow(i);
                for(int j = 0, total = columnCount ; j < total; j++)
                {
                    String[] split = lines.get(i).split(",");
                    dataRow.createCell(j).setCellValue(split[j]);
                }
            }
//            result = workbook;
            // 5. 自动调整列宽
//            for (int i = 0; i < headers.length; i++) {
//                sheet.autoSizeColumn(i);
//            }
//            sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, startCol, endCol));
            // 6. 写入文件
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            result = outputStream.toByteArray();
//            try (FileOutputStream outputStream = new FileOutputStream("Report.xlsx")) {
//                workbook.write(outputStream);
//                System.out.println("Excel 文件生成成功！");
//            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    private static Cell getFirstCell(Sheet sheet, CellRangeAddress region)
    {
        Row row = sheet.getRow(region.getFirstRow());
        if (row == null) {
            row = sheet.createRow(region.getFirstRow());
        }
        Cell cell = row.getCell(region.getFirstColumn());
        if (cell == null) {
            cell = row.createCell(region.getFirstColumn());
        }

        return cell;
    }
}
