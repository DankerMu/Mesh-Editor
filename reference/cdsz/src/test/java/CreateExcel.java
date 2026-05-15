import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import cn.hutool.core.io.FileUtil;

import com.tool.ExcelTool;


public class CreateExcel {
	public static void main(String[] args) {
		String filePath = "E:/fl/导入观测/20251117/sql/岗巴.sql";
		String outPath = "E:/fl/导入观测/20251117/excel/test1.xls";
		
//		filePath = "E:/fl/导入观测/202510/sql/噶尔县扎西岗.sql";
//		outPath = "E:/fl/导入观测/202510/excel/test1.xls";
		
//		filePath = "E:/fl/导入观测/20251117/sql/加勒万.sql";
//		outPath = "E:/fl/导入观测/20251117/excel/test2.xls";
		
//		filePath = "E:/fl/导入观测/20251117/sql/库尔那克堡.sql";
//		outPath = "E:/fl/导入观测/20251117/excel/test3.xls";
		
//		filePath = "E:/fl/导入观测/20251117/sql/拉马斯.sql";
//		outPath = "E:/fl/导入观测/20251117/excel/test4.xls";
		
//		filePath = "E:/fl/导入观测/20251117/sql/陇.sql";
//		outPath = "E:/fl/导入观测/20251117/excel/test5.xls";
		
//		filePath = "E:/fl/导入观测/20251117/sql/墨脱.sql";
//		outPath = "E:/fl/导入观测/20251117/excel/test6.xls";
		
//		filePath = "E:/fl/导入观测/202510/sql/皮山县空喀山口.sql";
//		outPath = "E:/fl/导入观测/202510/excel/test6.xls";
		
//		filePath = "E:/fl/导入观测/20251117/sql/日挺布.sql";
//		outPath = "E:/fl/导入观测/20251117/excel/test7.xls";
		
//		filePath = "E:/fl/导入观测/202510/sql/五二四三.sql";
//		outPath = "E:/fl/导入观测/202510/excel/test8.xls";
		
//		filePath = "E:/fl/导入观测/202510/sql/五二四三.sql";
//		outPath = "E:/fl/导入观测/202510/excel/model.xls";
		
		create(filePath, outPath);
	}
	
	public static void create(String filePath, String outPath)
	{
//		String filePath = "E:/fl/导入观测/天文点.xls";
		List<String> lines = FileUtil.readLines(filePath, "utf-8");
		Workbook reader = new HSSFWorkbook();
		reader.createSheet();
		String[] split = null;
		reader.getSheetAt(0).createRow(0);
		split = lines.get(0).split(",");
		Row row = null;
		int flagIndex = -1;
		int k = 0;
		for(int i = 0, num = split.length; i < num; i++)
		{
			if(split[i].equals("分钟降水量/翻斗式"))
			{
				flagIndex = i;
				continue;
			}
			if(flagIndex != -1 && i > flagIndex)
			{
				k = 1;
			}
			row = reader.getSheetAt(0).getRow(0);
			row.createCell(i - k);
			row.getCell(i - k).setCellValue(split[i]);
		}
		short lastCellNum = row.getLastCellNum();
		row.createCell(lastCellNum + 0);
		row.createCell(lastCellNum + 1);
		row.createCell(lastCellNum + 2);
		row.createCell(lastCellNum + 3);
		row.getCell(lastCellNum + 0).setCellValue("经度");
		row.getCell(lastCellNum + 1).setCellValue("纬度");
		row.getCell(lastCellNum + 2).setCellValue("能见度");
		row.getCell(lastCellNum + 3).setCellValue("总云量");
		for(int i = 1, count = lines.size(); i < count; i++)
		{
			row = reader.getSheetAt(0).createRow(i);
			split = lines.get(i).split(",", -1);
			k = 0;
			for(int j = 0, num = split.length; j < num; j++)
			{
				if(j == flagIndex)
				{
					continue;
				}
				if(flagIndex != -1 && j > flagIndex)
				{
					k = 1;
				}
				row.createCell(j - k);
				row.getCell(j - k).setCellValue(split[j]);
			}
			lastCellNum = row.getLastCellNum();
			row.createCell(lastCellNum + 0);
			row.createCell(lastCellNum + 1);
			row.createCell(lastCellNum + 2);
			row.createCell(lastCellNum + 3);
			row.getCell(lastCellNum + 0).setCellValue("");
			row.getCell(lastCellNum + 1).setCellValue("");
			row.getCell(lastCellNum + 2).setCellValue("0");
			row.getCell(lastCellNum + 3).setCellValue("0");
		}
		
	    try (FileOutputStream outputStream1 = new FileOutputStream(outPath)) {
		    reader.write(outputStream1);
		    System.out.println("Excel 文件生成成功！");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void createExcel()
	{
		String filePath = "E:/fl/导入观测/天文点.xls";
		
		Workbook reader = ExcelTool.readExcel(filePath);
		List<Sheet> sheets = new ArrayList<>();
		sheets.add(reader.getSheetAt(0));
		for(Sheet sheet : sheets)
		{
			int firstRowNum = sheet.getFirstRowNum();
			int lastRowNum = sheet.getLastRowNum();
//			lastRowNum = 1679;
			Row firstRow = sheet.getRow(firstRowNum);
			short lastCellNum = firstRow.getLastCellNum();
			firstRow.createCell(lastCellNum + 0);
			firstRow.createCell(lastCellNum + 1);
			firstRow.createCell(lastCellNum + 2);
			firstRow.createCell(lastCellNum + 3);
			firstRow.getCell(lastCellNum + 0).setCellValue("经度");
			firstRow.getCell(lastCellNum + 1).setCellValue("纬度");
			firstRow.getCell(lastCellNum + 2).setCellValue("能见度");
			firstRow.getCell(lastCellNum + 3).setCellValue("总云量");
			for(int i = firstRowNum + 1; i < lastRowNum; i++)
			{
				Row row = sheet.getRow(i);
				row.createCell(lastCellNum + 0);
				row.createCell(lastCellNum + 1);
				row.createCell(lastCellNum + 2);
				row.createCell(lastCellNum + 3);
				row.getCell(lastCellNum + 0).setCellValue("");
				row.getCell(lastCellNum + 1).setCellValue("");
				row.getCell(lastCellNum + 2).setCellValue("0");
				row.getCell(lastCellNum + 3).setCellValue("0");
				if(i > 1679)
				{
					sheet.removeRow(row);
				}
			}
			System.out.println(lastRowNum);
		}
		
      try (FileOutputStream outputStream1 = new FileOutputStream("E:/fl/导入观测/Report.xls")) {
	      reader.write(outputStream1);
	      System.out.println("Excel 文件生成成功！");
	  } catch (Exception e) {
		  e.printStackTrace();
	  }
	}
}
