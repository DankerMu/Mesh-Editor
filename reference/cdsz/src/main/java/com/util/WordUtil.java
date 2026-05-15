package com.util;

import org.apache.poi.xwpf.usermodel.*;

import java.awt.*;
import java.io.*;
import java.math.BigInteger;

public class WordUtil {
    private static Font font = null;
    static
    {
        File fontFile = new File("/data/font/simfang.ttf");
//        File fontFile = new File("E:/work/workspaces/cdsz_dev/font/simfang.ttf");
        try {
            FileInputStream fis = new FileInputStream(fontFile);
            font = Font.createFont(Font.TRUETYPE_FONT, fis);
        } catch (FontFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // 创建一个新的Word文档
        XWPFDocument document = new XWPFDocument();
        
        try {
            // 添加标题
            addTitle(document, "Java生成Word文档示例", 1, 10);
            
            // 添加段落
            addParagraph(document, "这是一个使用Apache POI库生成Word文档的示例。" +
                    "Apache POI是一个开源的Java库，用于处理Microsoft Office格式的文件。", 
                    false);
            
            // 添加子标题
            addTitle(document, "主要功能", 2, 10);
            
            // 添加列表
            addBulletList(document, new String[]{
                "创建和编辑Word文档(.docx)",
                "设置文本格式（字体、大小、颜色等）",
                "添加标题、段落和列表",
                "插入表格、图片等元素"
            });
            
            // 添加另一个子标题
            addTitle(document, "表格示例", 2, 10);
            
            // 添加表格
            addTable(document);
            
            // 保存文档
            String filePath = "D:/generated_word_document.docx";
            FileOutputStream out = new FileOutputStream(filePath);
            document.write(out);
            out.close();
            document.close();
            
            System.out.println("Word文档已成功生成: " + filePath);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 添加标题
     */
    public static void addTitle(XWPFDocument document, String text, int level, int fontSize) {
        XWPFParagraph title = document.createParagraph();
        title.setStyle("Heading" + level);
        title.setAlignment(ParagraphAlignment.CENTER);
        
        XWPFRun run = title.createRun();
        run.setFontFamily(font.getName());
        run.setText(text);
        run.setBold(true);
        run.setFontSize(fontSize);
    }
    
    /**
     * 添加段落
     */
    private static void addParagraph(XWPFDocument document, String text, boolean isBold) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(isBold);
        run.setFontSize(12);
    }
    
    /**
     * 添加项目符号列表
     */
    private static void addBulletList(XWPFDocument document, String[] items) {
        for (String item : items) {
            XWPFParagraph paragraph = document.createParagraph();
            // 设置为项目符号列表
            paragraph.setNumID(BigInteger.valueOf(1));
            
            XWPFRun run = paragraph.createRun();
            run.setText(item);
            run.setFontSize(12);
        }
    }
    
    /**
     * 添加表格
     */
    private static void addTable(XWPFDocument document) {
        // 创建一个3行3列的表格
        XWPFTable table = document.createTable(3, 3);
        
        // 设置表头
        table.getRow(0).getCell(0).setText("ID");
        table.getRow(0).getCell(1).setText("名称");
        table.getRow(0).getCell(2).setText("描述");
        
        // 设置表格内容
        table.getRow(1).getCell(0).setText("1");
        table.getRow(1).getCell(1).setText("Apache POI");
        table.getRow(1).getCell(2).setText("处理Office文档的Java库");
        
        table.getRow(2).getCell(0).setText("2");
        table.getRow(2).getCell(1).setText("Docx4j");
        table.getRow(2).getCell(2).setText("另一个处理Word文档的库");
        
        // 设置表格宽度
        table.setWidth("100%");
    }
    
    public static void addTable(XWPFDocument doc, String[][] contents, int fontSize)
    {


    	int rowCount = contents.length;
    	int columnCount = contents[0].length;
    	
    	XWPFTable table = doc.createTable(rowCount, columnCount);
    	for(int i = 0; i < rowCount; i++)
    	{
    		for(int j = 0; j < columnCount; j++)
    		{
    			table.getRow(i).getCell(j).setText(contents[i][j]);

                for (XWPFParagraph paragraph : table.getRow(i).getCell(j).getParagraphs()) {
                    paragraph.setAlignment(ParagraphAlignment.CENTER);
                    for (XWPFRun run : paragraph.getRuns()) {
                        // 优先使用TTF文件对应的字体名称
                        run.setFontFamily(font.getName());
                        if(i == 0)
                        {
                            run.setBold(true);
                        }
                        run.setFontSize(fontSize);
//                            run.setFontFamily(font.getName(), CTRFontSize.ENGLISH);
//
//                            run.setBold(isBold);
//                            run.setColor(fontColor);
                    }
                    table.getRow(i).getCell(j).setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
                }
                if(i > 0 && j == 1)
                {
                    WordCellImageUtil.insertImageToCell(table.getRow(i).getCell(j), "/home/ubuntu/fl/font/duoyun.png", 1, 1);
                }
    		}
    	}
    	table.setWidth("100%");
    }


    /**
     * 加载自定义TTF字体并设置到表格
     * @param table 目标表格
     * @param fontSize 字号（磅）
     * @param isBold 是否加粗
     * @param fontColor 字体颜色
     */
    public static void setTableCustomFont(XWPFTable table, int fontSize, boolean isBold, String fontColor) {
        try {

            File fontFile = new File("/data/font/simfang.ttf");
            try {
                FileInputStream fis = new FileInputStream(fontFile);
                font = Font.createFont(Font.TRUETYPE_FONT, fis);
            } catch (FontFormatException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // 遍历表格设置字体
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph paragraph : cell.getParagraphs()) {
                        for (XWPFRun run : paragraph.getRuns()) {
                            // 优先使用TTF文件对应的字体名称
                            run.setFontFamily(font.getName());
//                            run.setFontFamily(font.getName(), CTRFontSize.ENGLISH);
//                            run.setFontSize(fontSize);
//                            run.setBold(isBold);
//                            run.setColor(fontColor);
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("加载仿宋TTF文件失败：" + e.getMessage());
            // 降级方案：直接设置字体名称为“仿宋”
//            setTableFontStyle(table, "仿宋", fontSize, isBold, fontColor);
        }
    }
    
    public static byte[] toBytes(XWPFDocument doc)
    {
    	byte[] result = null;
    	ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
			doc.write(outputStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
        result = outputStream.toByteArray();
    	
    	return result;
    }
}
    