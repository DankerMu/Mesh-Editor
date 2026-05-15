package com.tool;
 
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;
 
import javax.imageio.ImageIO;
 
/**
 * 	生成验证码的工具类
 * @author 王
 *
 */
public class VerifyCode {
	private int w = 70;//设置缓冲区的宽
	private int h = 35;//设置缓冲区的宽
	private Random r = new Random();
//	 {"宋体", "华文楷体", "黑体", "华文新魏", "华文隶书", "微软雅黑", "楷体_GB2312"}
	private String[] fontNames  = {"宋体", "华文楷体", "黑体", "华文新魏", "华文隶书", "微软雅黑", "楷体_GB2312"};
	//源
	private String codes  = "123456789abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ";
	// 背景颜色
	private Color bgColor  = new Color(255, 255, 255);
	// 保存随机生成的图片当中的内容。
	private String text ;
 
	// 随机生成颜色
	private Color randomColor () {
		int red = r.nextInt(150);
		int green = r.nextInt(150);
		int blue = r.nextInt(150);
		return new Color(red, green, blue);
	}
 
	// 随机生成字体
	private Font randomFont () {
		int index = r.nextInt(fontNames.length);
		String fontName = fontNames[index];//根据随机的索引，获取随机字体
		int style = r.nextInt(4);//0,1,2,3, 0：没有任何样式，1,加粗，2，斜体，3，加粗和斜体  PLAIN（0）、BOLD(1)、ITALIC(2) 或 BOLD+ITALIC(3)。
		int size = r.nextInt(5) + 24; //随机生成字号
		return new Font(fontName, style, size);
	}
 
	// 画干扰线
	private void drawLine (BufferedImage image) {
		int num  = 3;//花三条干扰线
		Graphics2D g2 = (Graphics2D)image.getGraphics();
		for(int i = 0; i < num; i++) {
			int x1 = r.nextInt(w);
			int y1 = r.nextInt(h);
			int x2 = r.nextInt(w);
			int y2 = r.nextInt(h);
			g2.setStroke(new BasicStroke(1.5F));
			g2.setColor(Color.BLUE); //给干扰线设置了颜色
			g2.drawLine(x1, y1, x2, y2);//划线
		}
	}
 
	//随机生成字符
	private char randomChar () {
		int index = r.nextInt(codes.length());
		return codes.charAt(index);
	}
 
	// 得到一个缓冲区
	private BufferedImage createImage () {
		// 获取一个缓冲区
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		// 得到一个画笔
		Graphics2D g2 = (Graphics2D)image.getGraphics();
		// 设置画笔的颜色 白颜色
		g2.setColor(this.bgColor);
		// 填充图片的缓冲区。
		g2.fillRect(0, 0, w, h);
		// 将缓冲区返回。
		return image;
	}
 
	// 调用该方法，可以得到验证码
	public BufferedImage getImage () {
		BufferedImage image = createImage();//创建图片的缓冲区
		Graphics2D g2 = (Graphics2D)image.getGraphics();//得到绘制环境(画笔)
		StringBuilder sb = new StringBuilder();//定义一个容器，用来装在生成的验证码
		//向图片当中画四个字符
		for(int i = 0; i < 4; i++)  {//循环四次，每次生成一个字符
			String s = randomChar() + "";//随机成成一个字符
			sb.append(s); //将生成的字符放在缓冲区
			float x = i * 1.0F * w / 4; //设置当前字符的x轴坐标
			g2.setFont(randomFont()); //设置随机生成的字体
			g2.setColor(randomColor()); //设置字符的随机颜色
			g2.drawString(s, x, h-5); //画图
		}
		this.text = sb.toString(); //随机生成的图片的内容复制给this.text
		drawLine(image); //画干扰线
		return image;
	}
 
	// 获取图片当中的内容
	public String getText() {
		return text;
	}
 
	// 保存图片到指定的输出流
	public static void output (BufferedImage image, OutputStream out)
			throws IOException {
		ImageIO.write(image, "JPEG", out);
	}
}