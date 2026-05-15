package com.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;

/**
 * @category
 * @date 2025/9/3 20:46
 * @description TODO
 */
public class ProvinceBoundaryDrawer2400 {
    // 存储省份边界数据：使用double类型的经纬度坐标
//    private static List<Province> provinces = new ArrayList<>();
    // 地图坐标范围（中国大致经纬度范围）
    private static final double MIN_LON = 73.0;    // 最小经度
    private static final double MAX_LON = 135.0;   // 最大经度
    private static final double MIN_LAT = 18.0;    // 最小纬度
    private static final double MAX_LAT = 53.0;    // 最大纬度
    // 组件尺寸变量
//    private static int width = 821 * 4;
//    private static int height = 501 * 4;
    private static int width = 2400;
    private static int height = 1500;
    
	private static List<int[]> rainColorList = new ArrayList<>();
	private static List<int[]> snowColorList = new ArrayList<>();
//	private static String[] rainColorValues = new String[]{"0.1", "10", "25", "50", "100"};
//	private static String[] snowColorValues = new String[]{"0.1", "2.5", "5", "10"};
	private static String[] rainColorNameValues = new String[]{"小  雨", "中  雨", "大  雨", "暴  雨", "大暴雨", "雨夹雪"};
	private static String[] snowColorNameValues = new String[]{"小  雪", "中  雪", "大  雪", "暴  雪", "轻雾霾", "  雾  "};
	private static String[] wwColorNameValues = new String[]{"雷  暴", "冻  雨", "大  风", "沙尘暴", "扬  沙", "浮  尘"};
	private static int[] wwInts = new int[]{17,67,6,31,7,6};
	private static Map<String, double[]> cityMap = new HashMap<>();
	private static Font font = null;
	static
	{
		rainColorList.add(new int[]{170, 240, 144});//0.1 10
		rainColorList.add(new int[]{61, 172, 5});//10 25
		rainColorList.add(new int[]{104, 185, 249});//25 50
		rainColorList.add(new int[]{5, 4, 247});//50 100
		rainColorList.add(new int[]{253, 4, 255});//100 250
		rainColorList.add(new int[]{251, 201, 252});//100 250
//		rainColorList.add(new int[]{253, 4, 255});//250
		
		snowColorList.add(new int[]{209, 210, 211});//0.1 2.5
		snowColorList.add(new int[]{166, 167, 168});//2.5 5
		snowColorList.add(new int[]{117, 118, 119});//5 10
		snowColorList.add(new int[]{77, 78, 79});//10 20
//		snowColorList.add(new int[]{77, 78, 79});//20 30
//		snowColorList.add(new int[]{77, 78, 79});//30
		snowColorList.add(new int[]{240, 203, 125});//20 30
		snowColorList.add(new int[]{246, 130, 29});//30
		
		cityMap.put("成都", new double[]{104.1600, 30.5900, 1});
		cityMap.put("西昌", new double[]{102.2700, 27.9000});
		cityMap.put("理塘", new double[]{100.2300, 29.9900});
		
		cityMap.put("重庆", new double[]{106.4600, 29.5800, 1});
		cityMap.put("万州", new double[]{108.4100, 30.8000});
		
		cityMap.put("拉萨", new double[]{91.1400, 29.6600, 1});
		cityMap.put("班公湖", new double[]{78.9850, 33.7517});
		cityMap.put("典角", new double[]{79.6700, 32.5100});
		cityMap.put("普兰", new double[]{81.1400, 30.3500});
		cityMap.put("聂拉木", new double[]{85.9800, 28.1600});
		cityMap.put("拉多拉", new double[]{88.5100, 28.2700});
		cityMap.put("洞朗", new double[]{89.0200, 27.2600});
		cityMap.put("日喀则", new double[]{88.8900, 29.2500});
		cityMap.put("错那", new double[]{91.9600, 27.9900});
		cityMap.put("林芝", new double[]{94.3600, 29.6500});
		cityMap.put("昌都", new double[]{97.1800, 31.1500});
		
		cityMap.put("兰州", new double[]{103.8800, 36.0400, 1});
		cityMap.put("酒泉", new double[]{98.4900, 39.7700});
		cityMap.put("武威", new double[]{102.8700, 37.8900});
		
		cityMap.put("西宁", new double[]{101.7300, 36.6600, 1});
		cityMap.put("格尔木", new double[]{94.9100, 36.4200});
		
		cityMap.put("银川", new double[]{106.2100, 38.4700, 1});
		
		cityMap.put("乌鲁木齐", new double[]{87.7400, 43.8100, 1});
		cityMap.put("和田", new double[]{79.9200, 37.1200});
		cityMap.put("哈密", new double[]{93.5200, 42.8000});
		cityMap.put("喀什", new double[]{75.7500, 39.4900});
		cityMap.put("阿勒泰", new double[]{88.0700, 47.7400});
		cityMap.put("塔城", new double[]{82.9800, 46.7300});
		cityMap.put("伊宁", new double[]{81.3300, 43.9400});
		cityMap.put("阿克苏", new double[]{80.3800, 41.1200});
		cityMap.put("库尔勒", new double[]{85.8200, 41.7300});
		cityMap.put("加勒万", new double[]{78.2456, 34.7506});
		
		
		File fontFile = new File("/data/font/simhei.ttf");
//		File fontFile = new File("E:/fl/20251010/simhei.ttf");
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
//		String filePathPng = "E:/fl/datas/images/deep/tp_999_deeplearning_2025112108_048_048.png";
//		String filePathPng = "E:/fl/datas/images/deep/tp_999_deeplearning_2025112108_168_120.png";
		String filePathPng = "E:/fl/导入20251202/python_image/tp_999_deeplearning_2025120208_084_024.png";
		String outPath = "test.png";
		String basePath = "E:/fl/datas/images/";
		ProvinceBoundaryDrawer2400 drawer = new ProvinceBoundaryDrawer2400();
		String title = "战区未来24小时天气趋势预报";
		Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        int month =  calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        int nextDay = calendar.get(Calendar.DAY_OF_MONTH);
        String subTitle = month + "月" + day + "日20时～" + nextDay + "日20时";
		drawer.combine(basePath, filePathPng, outPath, title, subTitle);
	}

    public void combine(String basePath, String filePathPng, String outPath, String title, String subTitle) {
//        String filePath = "E:/fl/导入20250904/20250904/中华人民共和国.json";
        String filePath = basePath + "中华人民共和国.json";
        List<Province> provinces = new ArrayList<>();
        List<JSONObject> jsonObjects = readJsonFile(filePath);
        for(JSONObject jsonObject : jsonObjects)
        {
            String name = String.valueOf(jsonObject.getJSONObject("properties").get("name"));
            Province province = createSampleProvince(name, jsonObject);
            provinces.add(province);
        }
//        String tuli = "E:/fl/参考资料/图例.png";
//        String tuli = basePath + "tuli.png";
        try {
            BufferedImage image1 = ImageIO.read(new File(filePathPng));
            Graphics2D g2d = image1.createGraphics();
            g2d.setColor(Color.RED);
            Font font4 = font.deriveFont(Font.BOLD, 30);
            Font font6 = font.deriveFont(Font.BOLD, 40);
            
            int addWidth = 0;
            int addHeight = 0;
            BufferedImage image = new BufferedImage(width + addWidth * 2, height + addHeight * 2, 1);
            Graphics2D graphics = image.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width + addWidth * 2, height + addHeight * 2);
            graphics.drawImage(image1, 0 + addWidth - 100, 0 + addHeight, null);
            GeneralPath allProvincesPath = new GeneralPath();
            for (Province province : provinces) {
              GeneralPath provincePath = createProvincePath(province);
              allProvincesPath.append(provincePath, false);
            } 
            GeneralPath clipPath = new GeneralPath();
            clipPath.append(new Rectangle(0, 0, width + addWidth * 2, height + addHeight * 2), false);
            clipPath.append(allProvincesPath, false);
            clipPath.setWindingRule(0);
            graphics.setClip(clipPath);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width + addWidth * 2, height + addHeight * 2);
            graphics.setClip(null);
            graphics.setColor(Color.BLACK);
            for (Province province : provinces) {
              GeneralPath path = createProvincePath(province);
              graphics.draw(path);
            } 
            
            addWidth = 50;
            addHeight = 50;
            BufferedImage image2 = new BufferedImage(width + addWidth * 2 + 2, height + addHeight * 2 + 50, 1);
            Graphics2D graphics2 = image2.createGraphics();
            graphics2.setColor(Color.WHITE);
            graphics2.fillRect(-50, 0, image2.getWidth(), image2.getHeight());
            int borderSize = 2;
            graphics2.fillRect(borderSize / 2, borderSize / 2, image2.getWidth() - borderSize, image2.getHeight() - borderSize);
            graphics2.drawImage(image, -40, 50 + addHeight, null);
            graphics2.setColor(Color.BLACK);
            graphics2.drawLine(0, 0, image2.getWidth(), 0);
            graphics2.drawLine(0, 0, 0, image2.getHeight());
            graphics2.drawLine(0, image2.getHeight() - 4, image2.getWidth(), image2.getHeight() - 4);
            
            
            double[] lonlat = null;
            double x = 0;
            double y = 0;
            int redis = 4;
            graphics2.setColor(Color.RED);
            for (String city : cityMap.keySet()) {
              lonlat = cityMap.get(city);
              x = transLon(lonlat[0], width) - 40;
              y = transLat(lonlat[1], height) + addHeight * 2;
              if (lonlat.length == 3) {
                redis = 10;
              } else {
                redis = 8;
              } 
              if ("加勒万".equals(city)) {
            	  graphics2.fillOval((int)x + 5 - 100, (int)y - 5, redis * 2, redis * 2);
              } else {
            	  graphics2.fillOval((int)x - redis - 100, (int)y - redis, redis * 2, redis * 2);
              } 
              float lon = (float)x - (city.length() * 18);
              float lat = (float)y + 45.0F;
              if ("酒泉,武威".contains(city)) //左
              {
                lon = (float)x - (city.length() * 40);
                lat = (float)y + 5.0F;
              } 
              else if ("塔城,重庆,班公湖,典角,林芝,错那".contains(city)) //右
              {
                lon = (float)x + 10.0F;
                lat = (float)y + 5.0F;
              }
              else if ("加勒万".contains(city)) //右
              {
                lon = (float)x + 25.0F;
                lat = (float)y + 12.0F;
              } 
              else if ("万州,日喀则,洞朗,聂拉木,普兰".contains(city)) //上
              {
                lon = (float)x - (city.length() * 18);
                lat = (float)y - 15.0F;
              } 
              else if ("拉多拉".contains(city)) //上
              {
                lon = (float)x - (city.length() * 18);
                lat = (float)y - 11.0F;
              } 
              else if ("洞朗".contains(city)) //上
              {
                lon = (float)x - (city.length() * 6);
                lat = (float)y - 100.0F;
              } 
              if ("成都,重庆,拉萨,兰州,西宁,银川,乌鲁木齐".contains(city)) {
            	  graphics2.setFont(font6);
              } else {
            	  graphics2.setFont(font4);
              } 
              graphics2.drawString(city, lon - 100, lat);
            }
            
            graphics2.setColor(Color.BLACK);
            Font font1 = font.deriveFont(0, 80.0F);
            graphics2.setFont(font1);
            graphics2.drawString(title, (int)(image2.getWidth() / 3.3), addHeight + 30);
            Font font3 = font.deriveFont(0, 55.0F);
            graphics2.setFont(font3);
            graphics2.drawString(subTitle, (int)(image2.getWidth() / 2.48), 90 + 50);
            
            Font font5 = font.deriveFont(1, 55.0F);
            graphics2.setFont(font5);
            int tuLiY = 150;
            int tuLiWidth = 150;
            int tuLiHeight = 60;
            
            int tuLiColorX = 260;
            
            
            graphics2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2.drawString("图 例", image2.getWidth() - 260, tuLiY);
            int i = 0;
            Font font2 = font.deriveFont(0, 40.0F);
            graphics2.setFont(font2);
            for (int[] color : rainColorList) {
              graphics2.setColor(new Color(color[0], color[1], color[2]));
              graphics2.fillRect(width + addWidth - tuLiColorX, tuLiY + i * (tuLiHeight + 10) + 30, tuLiWidth, tuLiHeight);
              graphics2.setColor(Color.BLACK);
              graphics2.drawString(rainColorNameValues[i], width + addWidth - tuLiColorX + tuLiWidth + 20, tuLiY + i * (tuLiHeight + 10) + 68);
              i++;
            } 
            for (int[] color : snowColorList) {
              graphics2.setColor(new Color(color[0], color[1], color[2]));
              graphics2.fillRect(width + addWidth - tuLiColorX, tuLiY + i * (tuLiHeight + 10) + 30, tuLiWidth, tuLiHeight);
              graphics2.setColor(Color.BLACK);
              graphics2.drawString(snowColorNameValues[i - rainColorNameValues.length], width + addWidth - tuLiColorX + tuLiWidth + 20, tuLiY + i * (tuLiHeight + 10) + 68);
              i++;
            } 
            
            
            graphics2.setColor(Color.WHITE);
            // 开启抗锯齿，让文字更平滑
            

//            File fontFile = new File("E:/fl/导入20251127/Weather_std_htht.ttf");
//            File windFontFile = new File("E:/font/Wind_std_htht.ttf");
            File fontFile = new File("/data/font/Weather_std_htht.ttf");
//            File windFontFile = new File("/data/font/Wind_std_htht_dom.ttf");
            // 加载自定义字体文件（例如Weather_std_htht.ttf）
            Font customFont = null;
            Font customFont1 = null;
//            Font customWindFont = null;
            try {
                customFont = Font.createFont(Font.TRUETYPE_FONT, fontFile).deriveFont(Font.PLAIN, 80);
                customFont1 = Font.createFont(Font.TRUETYPE_FONT, fontFile).deriveFont(Font.BOLD, 70);
//                customWindFont = Font.createFont(Font.TRUETYPE_FONT, windFontFile).deriveFont(Font.PLAIN, 40);
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(customFont); // 注册字体到系统
//                ge.registerFont(customWindFont); // 注册字体到系统
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            int wwFontHeight = 70;
            Color color = new Color(92, 33, 29);
         // 使用自定义字体绘制符号
            if (customFont != null) {
            	graphics2.setColor(Color.BLACK);
                WeatherSymbol ws = new WeatherSymbol();
                for(int j = 0, count = wwColorNameValues.length; j < count; j++)
                {
                	if(j == 2)
                	{
//                		graphics2.setFont(customWindFont);
                	}
                	else
                	{
                		graphics2.setFont(customFont);
                	}
                	if(j < 2)
                	{
                		graphics2.setColor(Color.RED);
                	}
                	else
                	{
                		graphics2.setColor(color);
                	}
                	i++;
                	if(j == 2)
                	{
                		Point p = new Point(width + addWidth - tuLiColorX + 120, tuLiY + (tuLiHeight + 10) * (i - j) + j * wwFontHeight);
                		graphics2.setStroke(new BasicStroke(3));
                		new Wind().plotWind(graphics2, p, 305, 11);
                	}
                	else
                	{
                		graphics2.setFont(customFont);
                		if(j == count - 1)
                		{
                			graphics2.setFont(customFont1);
                		}
                		graphics2.drawString(ws.weather(wwInts[j]), width + addWidth - tuLiColorX + 50, tuLiY + (tuLiHeight + 10) * (i - j) + j * wwFontHeight + 10); // 位置(x, y)
                	}
                	graphics2.setFont(font2);
                	graphics2.setColor(Color.BLACK);
                	graphics2.drawString(wwColorNameValues[j], width + addWidth - tuLiColorX + tuLiWidth + 20, tuLiY + (tuLiHeight + 10) * (i - j) + j * wwFontHeight - 3);
                }
            }
            
            
            graphics2.dispose();
            ImageIO.write(image2, "png", new File(basePath + outPath));
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 绘制省份名称
    private void drawProvinceName(Graphics2D g2d, Province province, GeneralPath path) {
        Rectangle bounds = path.getBounds();
        int centerX = bounds.x + bounds.width / 2;
        int centerY = bounds.y + bounds.height / 2;

        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("SimHei", Font.BOLD, 16));
        g2d.drawString(province.getName(), centerX - 20, centerY);
    }

    private static Province createSampleProvince(String name, JSONObject json) {
        List<LatLonPoint> points = new ArrayList<>();
        List<List<List<Double>>> lll = new ArrayList<>();

        JSONArray object = (JSONArray) json.getJSONObject("geometry").get("coordinates");
        List<List<Double>> ll = null;
        for(int j = 0; j < object.size(); j++)
        {
            ll = new ArrayList<>();
            JSONArray jsonObject = (JSONArray) object.get(j);
            JSONArray object1 = (JSONArray) jsonObject.get(0);
            List<Double> l = null;
            if(object1.get(0) instanceof BigDecimal)
            {
                l = new ArrayList<>();

                l.add(object1.getDouble(0));
                l.add(object1.getDouble(1));
                ll.add(l);
            }
            else
            {
                for(int k = 0; k < object1.size(); k++)
                {
                    l = new ArrayList<>();

                    JSONArray object2 = (JSONArray) object1.get(k);
                    l.add(object2.getDouble(0));
                    l.add(object2.getDouble(1));
                    ll.add(l);
                }
            }
            lll.add(ll);
        }

        List<List<Double>> lists = lll.get(0);
        lll.clear();
        lll.add(lists);
        // 创建一个省份边界
        for(List<List<Double>> lls : lll)
        {
            for(List<Double> l : lls)
            {
                points.add(new LatLonPoint(l.get(0), l.get(1)));
            }
        }


        return new Province(name, points);
    }


    // 创建省份边界路径
    private static GeneralPath createProvincePath(Province province) {
        GeneralPath path = new GeneralPath();
        List<LatLonPoint> points = province.getBoundaryPoints();

        if (points.isEmpty()) return path;

        // 转换第一个经纬度点为屏幕坐标并移动到该点
//        Point firstPoint = latLonToScreen(points.get(0));
        double lon = transLon(points.get(0).longitude, width) - 100;
        double lat = transLat(points.get(0).latitude, height);
        path.moveTo(lon, lat);

        // 连接其他点
        for (int i = 1; i < points.size(); i++) {
//            Point p = latLonToScreen(points.get(i));
            lon = transLon(points.get(i).longitude, width) - 100;
            lat = transLat(points.get(i).latitude, height);
            path.lineTo(lon, lat);
        }

        path.closePath();
        return path;
    }

    // 将经纬度坐标转换为屏幕坐标
    private static Point latLonToScreen(LatLonPoint latLon) {
        // 计算经度对应的x坐标
        double lonRange = MAX_LON - MIN_LON;
        double xRatio = (latLon.longitude - MIN_LON) / lonRange;
        int x = (int) (xRatio * width);

        // 计算纬度对应的y坐标（注意纬度越高，y值越小，因为屏幕y向下增长）
        double latRange = MAX_LAT - MIN_LAT;
        double yRatio = (MAX_LAT - latLon.latitude) / latRange;
        int y = (int) (yRatio * height);

        return new Point(x, y);
    }

    private static double transLon(double value, int width)
    {
//        (经度 - left) × (width / (right - left))
        return (value - 70) * (width / (111 - 70));
    }

    private static double transLat(double value, int height)
    {
//        (top - 纬度) × (height / (top - bottom))
        return (50 - value) * (height / (50 - 25));
    }

    public static List<JSONObject> readJsonFile(String jsonFile) {
        String provinces = "重庆市,四川省,甘肃省,青海省,宁夏回族自治区,西藏自治区,新疆维吾尔自治区";
//        String provinces = "重庆市,四川省,甘肃省,宁夏回族自治区";
        List<JSONObject> list = new ArrayList<>();
        String geoJson = null;
        try {
            geoJson = new String(Files.readAllBytes(Paths.get(jsonFile)));
            Map<String, Object> map = JSON.parseObject(geoJson, new TypeReference<Map<String, Object>>() { });

            JSONArray array = (JSONArray) map.get("features");
            for (int i = 0; i < array.size(); i++) {
                JSONObject json = (JSONObject) array.get(i);
                String name = String.valueOf(json.getJSONObject("properties").get("name"));
                if(!provinces.contains(name) || name.length() == 0)
                {
                    continue;
                }
                list.add(json);
            }


        } catch (IOException e) {
            e.printStackTrace();
        }

        return list;
    }

    // 省份数据类
    private static class Province {
        private String name;
        private List<LatLonPoint> boundaryPoints;

        public Province(String name, List<LatLonPoint> boundaryPoints) {
            this.name = name;
            this.boundaryPoints = boundaryPoints;
        }

        public String getName() {
            return name;
        }

        public List<LatLonPoint> getBoundaryPoints() {
            return boundaryPoints;
        }
    }

    // 经纬度点类（使用double类型）
    private static class LatLonPoint {
        double latitude;   // 纬度
        double longitude;  // 经度

        public LatLonPoint(double longitude, double latitude) {
            this.longitude = longitude;
            this.latitude = latitude;
        }
    }
}
