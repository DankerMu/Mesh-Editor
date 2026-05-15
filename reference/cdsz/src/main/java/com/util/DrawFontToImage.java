package com.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * @category
 * @date 2025/11/26 23:48
 * @description TODO
 */
public class DrawFontToImage {
    public static void main(String[] args) {
        BufferedImage image = new BufferedImage(500, 1000, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.WHITE);
        // 开启抗锯齿，让文字更平滑
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        File fontFile = new File("E:/font/Weather_std_htht.ttf");
        // 加载自定义字体文件（例如Weather_std_htht.ttf）
        Font customFont = null;
        try {
            customFont = Font.createFont(Font.TRUETYPE_FONT, fontFile).deriveFont(Font.PLAIN, 80);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(customFont); // 注册字体到系统
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 使用自定义字体绘制符号
        if (customFont != null) {
            g2d.setFont(customFont);
            g2d.setColor(Color.BLACK);
            WeatherSymbol ws = new WeatherSymbol();
            for(int i = 17; i < 27; i++)
            {
            	g2d.drawString(ws.weather(i), 10, 30 * i); // 位置(x, y)
            }
            File file = new File("E:/font/image.png");
            try {
                ImageIO.write(image, "png", file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
