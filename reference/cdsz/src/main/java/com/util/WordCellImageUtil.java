package com.util;

import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STVerticalJc;

import java.io.*;
import java.math.BigInteger;

/**
 * Word 单元格插入图片工具
 */
public class WordCellImageUtil {

    /**
     * 单元格插入本地图片（推荐）
     * @param cell 目标单元格
     * @param imgPath 图片本地路径（如：/data/img/logo.png）
     * @param width 图片宽度（单位：厘米，如 2.0）
     * @param height 图片高度（单位：厘米，如 1.5）
     * @throws Exception 图片读取/插入异常
     */
    public static void insertImageToCell(XWPFTableCell cell, String imgPath, double width, double height) {
        // 1. 读取图片为字节流
        File imgFile = new File(imgPath);
        try (FileInputStream fis = new FileInputStream(imgFile)) {
            insertImageToCell(cell, fis, getPictureType(imgPath), width, height);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 单元格插入图片（字节流方式，兼容网络流/内存流）
     * @param cell 目标单元格
     * @param imgStream 图片字节流
     * @param picType 图片类型（XWPFDocument.PICTURE_TYPE_*）
     * @param width 图片宽度（厘米）
     * @param height 图片高度（厘米）
     * @throws Exception 图片插入异常
     */
    public static void insertImageToCell(XWPFTableCell cell, InputStream imgStream, int picType, double width, double height) throws Exception {
        if (cell == null || imgStream == null) {
            throw new IllegalArgumentException("单元格或图片流不能为空");
        }

        // 2. 获取单元格段落（若无则创建）
        XWPFParagraph paragraph = cell.getParagraphs().isEmpty() ? cell.addParagraph() : cell.getParagraphs().get(0);
        // 清除段落原有文本（如需保留文本，注释此行）
        while (paragraph.getRuns().size() > 0) {
            paragraph.removeRun(0);
        }

        // 3. 创建 Run 并插入图片
        XWPFRun run = paragraph.createRun();
        // 厘米转EMU（Word 内部单位：1厘米 = 360000 EMU）
        int widthEMU = (int) (width * 360000);
        int heightEMU = (int) (height * 360000);

        // 插入图片（核心方法）
        run.addPicture(
                imgStream,
                picType,
                "image_" + System.currentTimeMillis(), // 图片ID（唯一即可）
                widthEMU,
                heightEMU
        );

        // 4. 可选：设置单元格垂直居中（上下居中）
        cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        // 可选：设置段落水平居中（左右居中）
        paragraph.setAlignment(ParagraphAlignment.CENTER);
    }

    /**
     * 根据图片后缀获取 POI 图片类型
     * @param imgPath 图片路径
     * @return XWPFDocument.PICTURE_TYPE_*
     */
    private static int getPictureType(String imgPath) {
        // 处理路径中无后缀的情况，避免空指针
        int lastDotIndex = imgPath.lastIndexOf(".");
        if (lastDotIndex == -1 || lastDotIndex == imgPath.length() - 1) {
            throw new IllegalArgumentException("图片路径无有效后缀：" + imgPath);
        }

        String suffix = imgPath.substring(lastDotIndex + 1).toLowerCase();
        int pictureType;

        // JDK 8 传统 switch 语句（替换 switch 表达式）
        switch (suffix) {
            case "png":
                pictureType = XWPFDocument.PICTURE_TYPE_PNG;
                break;
            case "jpg":
            case "jpeg": // 多个case共用逻辑
                pictureType = XWPFDocument.PICTURE_TYPE_JPEG;
                break;
            case "gif":
                pictureType = XWPFDocument.PICTURE_TYPE_GIF;
                break;
            case "bmp":
                pictureType = XWPFDocument.PICTURE_TYPE_BMP;
                break;
            default:
                throw new IllegalArgumentException("不支持的图片类型：" + suffix);
        }

        return pictureType;
    }

    // ------------------- 测试方法 -------------------
    public static void main(String[] args) {
        XWPFDocument doc = new XWPFDocument();
        // 创建2行2列表格
        XWPFTable table = doc.createTable(2, 2);

        try {
            // 1. 第一行第一列：插入本地图片（宽度2cm，高度1.5cm）
            XWPFTableCell cell1 = table.getRow(0).getCell(0);
            insertImageToCell(cell1, "/data/img/logo.png", 2.0, 1.5); // 替换为你的图片路径

            // 2. 第一行第二列：插入文本+图片（示例）
            XWPFTableCell cell2 = table.getRow(0).getCell(1);
            cell2.setText("LOGO：");
            // 追加图片（不清除原有文本）
            XWPFParagraph para2 = cell2.getParagraphs().get(0);
            XWPFRun run2 = para2.createRun();
            try (FileInputStream fis = new FileInputStream("/data/img/logo.png")) {
                run2.addPicture(fis, XWPFDocument.PICTURE_TYPE_PNG, "logo2", (int) (1.5 * 360000), (int) (1.0 * 360000));
            }
            para2.setAlignment(ParagraphAlignment.CENTER);
            cell2.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);

            // 3. 第二行：普通文本（保留原有逻辑）
            table.getRow(1).getCell(0).setText("姓名");
            table.getRow(1).getCell(1).setText("张三");
            // 设置文本居中+仿宋字体（复用你原有逻辑）
            int i = 0;
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph para : cell.getParagraphs()) {
                        para.setAlignment(ParagraphAlignment.CENTER);
                        for (XWPFRun run : para.getRuns()) {
                            run.setFontFamily("仿宋");
                            run.setFontSize(12);
                            if (i == 0) {
                                run.setBold(true);
                            }
                        }
                    }
                    cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
                }
                i++;
            }

            // 保存文档
            try (FileOutputStream out = new FileOutputStream("table_with_image.docx")) {
                doc.write(out);
                System.out.println("含图片表格生成成功！");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                doc.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}