package com.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import cn.hutool.core.io.FileUtil;

public class ImageUtil {
	
	private static List<int[]> rainColorList = new ArrayList<>();
	private static List<int[]> snowColorList = new ArrayList<>();
	private static String[] rainColorValues = new String[]{"0.1", "10", "25", "50", "100"};
	private static String[] snowColorValues = new String[]{"0.1", "2.5", "5", "10"};
	static
	{
		rainColorList.add(new int[]{170, 240, 144});//0.1 10
		rainColorList.add(new int[]{61, 172, 5});//10 25
		rainColorList.add(new int[]{104, 185, 249});//25 50
		rainColorList.add(new int[]{5, 4, 247});//50 100
		rainColorList.add(new int[]{253, 4, 255});//100 250
//		rainColorList.add(new int[]{253, 4, 255});//250
		
		snowColorList.add(new int[]{209, 210, 211});//0.1 2.5
		snowColorList.add(new int[]{166, 167, 168});//2.5 5
		snowColorList.add(new int[]{117, 118, 119});//5 10
		snowColorList.add(new int[]{77, 78, 79});//10 20
//		snowColorList.add(new int[]{77, 78, 79});//20 30
//		snowColorList.add(new int[]{77, 78, 79});//30
	}
	
	public static void main(String[] args) {
		String basePath = "E:/fl/datas/images/";
		String orgPath = "E:/fl/datas/images/deep/tp_999_deeplearning_2025062708_027_003.png";
		String cmbPath = "E:/fl/导入20250702/line_new.png";
		String outPath = "tp_999_deeplearning_2025081308_003_003.png";
		combineImages(basePath, orgPath, cmbPath, outPath);
	}
	
	public static void combineImages(String basePath, String orgPath, String cmbPath, String outPath)
	{
		String filePath = orgPath;
        String filePath2 = cmbPath;
        BufferedImage image1 = null;
        BufferedImage image2 = null;
		try {
//			System.out.println("1111111111111111111: " + filePath);
			image1 = ImageIO.read(new File(filePath));
			image2 = ImageIO.read(new File(filePath2));
		} catch (IOException e1) {
			e1.printStackTrace();
		}

        int addWidth = 50;
        int addHeight = 50;
        BufferedImage image = new BufferedImage(821 + addWidth * 2, 501 + addHeight * 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setColor(Color.WHITE);
        int borderSize = 6;
        graphics.fillRect(borderSize / 2, borderSize / 2, image.getWidth() - borderSize, image.getHeight() - borderSize);

        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font("宋体", Font.PLAIN, 40));
//        tp_999_deeplearning_2025081308_003_003.png
        String fileName = FileUtil.getPrefix(outPath);
        String[] split = fileName.split("_");
        String title = split[3] + "_" + split[4] + " " + Integer.parseInt(split[5]) + "小时降水预报"; 
        graphics.drawString(title, image.getWidth() / 4, addHeight - 10);
//        java.util.List<int[]> colors = new ArrayList<>();
//        colors.add(new int[]{222, 247, 216});
//        colors.add(new int[]{183, 246, 170});
//        colors.add(new int[]{116, 213, 108});
//        colors.add(new int[]{65, 182, 65});
//        colors.add(new int[]{100, 179, 253});
//        colors.add(new int[]{16, 0, 251});
        int i = 0;
//        String[] colorsValues = new String[]{"0.1", "10", "25", "50", "100", "150", "200"};
        graphics.setFont(new Font("宋体", Font.PLAIN, 15));
        for(int[] color : rainColorList)
        {
            graphics.setColor(new Color(color[0], color[1], color[2]));
            graphics.fillRect(image.getWidth() / 4 + i * addWidth, image.getHeight() - 40, addWidth, addHeight - 30);
            graphics.setColor(Color.BLACK);
            graphics.drawString(rainColorValues[i], image.getWidth() / 4 + i * addWidth - 8 * rainColorValues[i].length() / 2, image.getHeight() - 5);
            i++;
        }
        
        i = 0;
        for(int[] color : snowColorList)
        {
            graphics.setColor(new Color(color[0], color[1], color[2]));
            graphics.fillRect(image.getWidth() / 4 + 6 * addWidth + i * addWidth, image.getHeight() - 40, addWidth, addHeight - 30);
            graphics.setColor(Color.BLACK);
            graphics.drawString(snowColorValues[i], image.getWidth() / 4 + 6 * addWidth + i * addWidth - 8 * snowColorValues[i].length() / 2, image.getHeight() - 5);
            i++;
        }
        graphics.setColor(Color.BLACK);


        graphics.drawImage(image1, 0 + addWidth, 0 + addHeight, null);
        graphics.drawImage(image2, 0 + addWidth, 0 + addHeight, null);
        graphics.dispose();
        outPath = basePath + outPath;
//        System.out.println("2222222222222222222: " + outPath);
        try {
			ImageIO.write(image, "png", new File(outPath));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
