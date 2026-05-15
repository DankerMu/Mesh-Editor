import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @category
 * @date 2025/4/2 11:44
 * @description TODO
 */
public class ExcelTest {
    public static void main(String[] args) {
        List<String> lines = new ArrayList<>();
        lines.add("vis,at,ws,wd,");
        lines.add("72,72,72,72,");
        lines.add(",MAE,CORR,RMSE,MAE,CORR,RMSE,MAE,CORR,RMSE,MAE,CORR,RMSE,");
        lines.add("55026,1936436.486,NaN,1936436.486,31.924000000000003,NaN,31.924000000000003,27.6,NaN,27.6,146.176,NaN,292.352,");
        lines.add("合计,1936436.486,NaN,1936436.486,15.962000000000002,NaN,15.962000000000002,13.8,NaN,13.8,146.176,NaN,146.176,");
//        vis,at,ws,wd,
//        72,72,72,72,
//        MAE,CORR,RMSE,MAE,CORR,RMSE,MAE,CORR,RMSE,MAE,CORR,RMSE,
//        1936436.486,NaN,1936436.486,31.924000000000003,NaN,31.924000000000003,27.6,NaN,27.6,146.176,NaN,292.352,
//        1936436.486,NaN,1936436.486,15.962000000000002,NaN,15.962000000000002,13.8,NaN,13.8,146.176,NaN,146.176
        int columnCount = lines.get(2).split(",").length;
        int elementColumnStep = 3;
        int vtiColumnStep = 3;
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
            for(int j = 0; j < 2; j++)
            {
                String[] elements = lines.get(j).split(",");
                for(int i = 0, count = elements.length; i < count; i++)
                {
                    region = new CellRangeAddress(j, j, 1 + i * columnStep[j],i * columnStep[j] + columnStep[j]);
                    sheet.addMergedRegion(region);
                    // 获取合并区域的左上角单元格
                    Cell cell = getFirstCell(sheet, region);
                    cell.setCellValue(elements[i]);
                    cell.setCellStyle(cellStyle);
                }
            }
            region = new CellRangeAddress(0, 2, 0,0);
            sheet.addMergedRegion(region);
            Cell cell = getFirstCell(sheet, region);
            cell.setCellValue("站点");


            for(int i = 2, num = lines.size(); i < num; i++)
            {
                Row dataRow = sheet.createRow(i);
                for(int j = 0, total = columnCount ; j < total; j++)
                {
                    String[] split = lines.get(i).split(",");
                    dataRow.createCell(j).setCellValue(split[j]);
                }
            }
            // 5. 自动调整列宽
//            for (int i = 0; i < headers.length; i++) {
//                sheet.autoSizeColumn(i);
//            }
//            sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, startCol, endCol));
            // 6. 写入文件
            ByteArrayOutputStream tt = new ByteArrayOutputStream();
            workbook.write(tt);
            byte[] byteArray = tt.toByteArray();
            try (FileOutputStream outputStream = new FileOutputStream("Report.xlsx")) {
                workbook.write(outputStream);

                System.out.println("Excel 文件生成成功！");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
