import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.util.WeatherSymbol;


public class FontTest {
	private static int width = 821;
    private static int height = 501;
	public static void main(String[] args) {
		int addWidth = 50;
		int addHeight = 50;
		BufferedImage image2 = new BufferedImage(width + addWidth * 2 + 2, height + addHeight * 2 + 50, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2 = image2.createGraphics();
		graphics2.setColor(Color.WHITE);
        // 开启抗锯齿，让文字更平滑
        graphics2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        File fontFile = new File("E:/font/Weather_std_htht.ttf");
        File windFontFile = new File("E:/font/Wind_std_htht.ttf");
        // 加载自定义字体文件（例如Weather_std_htht.ttf）
        Font customFont = null;
        Font customWindFont = null;
        try {
            customFont = Font.createFont(Font.TRUETYPE_FONT, fontFile).deriveFont(Font.PLAIN, 40);
            customWindFont = Font.createFont(Font.TRUETYPE_FONT, windFontFile).deriveFont(Font.PLAIN, 40);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(customFont); // 注册字体到系统
            ge.registerFont(customWindFont); // 注册字体到系统
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        int wwFontHeight = 30;
        // 使用自定义字体绘制符号
       if (customFont != null) 
       {
    	   graphics2.setColor(Color.BLACK);
       	   for(int i = 0; i < 10; i++)
       	   {
	            WeatherSymbol ws = new WeatherSymbol();
	            graphics2.drawString(ws.weather(i), width + addWidth - 80, 0); // 位置(x, y)
       	   }
       }
           
       String outPath = "";
       graphics2.dispose();
       try {
    	   ImageIO.write(image2, "png", new File(outPath));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
