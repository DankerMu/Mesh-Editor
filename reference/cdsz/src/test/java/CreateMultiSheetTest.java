import com.tool.ExcelTool;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @category
 * @date 2025/6/20 16:38
 * @description TODO
 */
public class CreateMultiSheetTest {
    public static void main(String[] args) {
        Map<String, List<String>> mapLines = new HashMap<>();
        List<String> list1 = new ArrayList<>();
        list1.add("1,1,1,1,1,1");
        List<String> list2 = new ArrayList<>();
        list2.add("2,2,2,2,2,2");
        mapLines.put("1", list1);
        mapLines.put("2", list2);

        byte[] bytes = ExcelTool.createSimpleExcel(mapLines);
    }
}
