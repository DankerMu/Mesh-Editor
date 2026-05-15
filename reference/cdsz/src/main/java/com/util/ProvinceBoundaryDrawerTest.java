package com.util;

import cn.hutool.core.io.FileUtil;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;

import javax.imageio.ImageIO;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @category
 * @date 2025/9/3 20:46
 * @description TODO
 */
public class ProvinceBoundaryDrawerTest {
    // 存储省份边界数据：使用double类型的经纬度坐标
    private static List<Province> provinces = new ArrayList<>();
    // 地图坐标范围（中国大致经纬度范围）
    private static final double MIN_LON = 73.0;    // 最小经度
    private static final double MAX_LON = 135.0;   // 最大经度
    private static final double MIN_LAT = 18.0;    // 最小纬度
    private static final double MAX_LAT = 53.0;    // 最大纬度
    
    private static List<int[]> rainColorList = new ArrayList<>();
	private static List<int[]> snowColorList = new ArrayList<>();
    private static String[] rainColorNameValues = new String[]{"小  雨", "中  雨", "大  雨", "暴  雨", "大暴雨", "雨夹雪"};
	private static String[] snowColorNameValues = new String[]{"小  雪", "中  雪", "大  雪", "暴  雪"};
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
	}
    // 组件尺寸变量
    private static int width = 821;
    private static int height = 501;

    public static void main(String[] args) {
        String filePath = "E:/fl/导入20250904/20250904/中华人民共和国.json";
        List<JSONObject> jsonObjects = readJsonFile(filePath);
        for(JSONObject jsonObject : jsonObjects)
        {
            String name = String.valueOf(jsonObject.getJSONObject("properties").get("name"));
            Province province = createSampleProvince(name, jsonObject);
            provinces.add(province);
        }

        String filePathPng = "E:/fl/datas/images/deep/tp_999_deeplearning_2025062708_027_003.png";
        String outPath = "E:/fl/datas/images/";
        try {
            BufferedImage image1 = ImageIO.read(new File(filePathPng));
            int addWidth = 0;
            int addHeight = 0;
            BufferedImage image = new BufferedImage(821 + addWidth * 2, 501 + addHeight * 2, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, 821 + addWidth * 2, 501 + addHeight * 2);
            graphics.drawImage(image1, 0 + addWidth, 0 + addHeight, null);

            // 创建一个组合路径，包含所有省份的边界
            GeneralPath allProvincesPath = new GeneralPath();
            for (Province province : provinces) {
                GeneralPath provincePath = createProvincePath(province);
                allProvincesPath.append(provincePath, false);
            }

            // 设置裁剪区域为所有省份的外部
            GeneralPath clipPath = new GeneralPath();
            clipPath.append(new Rectangle(0, 0, 821 + addWidth * 2, 501 + addHeight * 2), false);
            clipPath.append(allProvincesPath, false);
            clipPath.setWindingRule(GeneralPath.WIND_EVEN_ODD);

            // 将外部区域涂成白色
            graphics.setClip(clipPath);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, 821 + addWidth * 2, 501 + addHeight * 2);

            // 重置裁剪区域，以便绘制省份边界
            graphics.setClip(null);

            // 绘制省份边界
            graphics.setColor(Color.BLACK);
            graphics.setStroke(new BasicStroke(2));
            for (Province province : provinces) {
                GeneralPath path = createProvincePath(province);
                graphics.draw(path);
                // 绘制省份名称
//              drawProvinceName(g2d, province, path);
            }



            addWidth = 50;
            addHeight = 50;
            BufferedImage image2 = new BufferedImage(821 + addWidth * 2, 501 + addHeight * 2, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics2 = image2.createGraphics();
//            graphics2.setColor(Color.BLACK);
            graphics2.setColor(Color.WHITE);
            graphics2.fillRect(0, 0, image2.getWidth(), image2.getHeight());
            int borderSize = 6;
            graphics2.fillRect(borderSize / 2, borderSize / 2, image2.getWidth() - borderSize, image2.getHeight() - borderSize);
            graphics2.drawImage(image, 0 + addWidth, 0 + addHeight, null);

            graphics2.setColor(Color.BLACK);
//            graphics2.setFont(new Font("宋体", Font.PLAIN, 40));
            
            Font font = null;
            File fontFile = new File("E:/fl/导入20250828/20250828/Fonts/simhei.ttf");
    		try {
    			FileInputStream fis = new FileInputStream(fontFile);
    			font = Font.createFont(Font.TRUETYPE_FONT, fis);
    			Font font2 = font.deriveFont(Font.PLAIN, 50);
    			System.out.println("font size:" + font.getSize() + "," + font.getSize2D());
    			System.out.println("font name: " + font.getFontName());
    			graphics2.setFont(font2);
    		} catch (FontFormatException e) {
    			e.printStackTrace();
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
            
            String fileName = FileUtil.getPrefix(filePathPng);
            String[] split = fileName.split("_");
            String title = split[3] + "_" + split[4] + " " + Integer.parseInt(split[5]) + "小时降水量"; 
            graphics2.drawString(title, image2.getWidth() / 4, addHeight - 10);
            
            int i = 0;
            graphics2.setFont(new Font("宋体", Font.PLAIN, 10));
            for(int[] color : rainColorList)
            {
//            	graphics2.setColor(new Color(color[0], color[1], color[2]));
//            	graphics2.fillRect(image2.getWidth() / 4 + i * addWidth, image2.getHeight() - 40, addWidth - 10, addHeight - 35);
//            	graphics2.setColor(Color.BLACK);
//            	graphics2.drawString(rainColorNameValues[i], image2.getWidth() / 4 + i * addWidth - 8 * rainColorNameValues[i].length() / 2, image2.getHeight() - 5);
            	graphics2.setColor(new Color(color[0], color[1], color[2]));
            	graphics2.fillRect(20, 400 + i * 18, addWidth - 10, addHeight - 35);
            	graphics2.setColor(Color.BLACK);
            	graphics2.drawString(rainColorNameValues[i], 20 + 48, 400 + i * 18 + 10);
                i++;
            }
            
//            i = 0;
            for(int[] color : snowColorList)
            {
//            	graphics2.setColor(new Color(color[0], color[1], color[2]));
//            	graphics2.fillRect(image2.getWidth() / 4 + 6 * addWidth + i * addWidth, image2.getHeight() - 40, addWidth, addHeight - 30);
//            	graphics2.setColor(Color.BLACK);
//            	graphics2.drawString(snowColorNameValues[i], image2.getWidth() / 4 + 6 * addWidth + i * addWidth - 8 * snowColorNameValues[i].length() / 2, image2.getHeight() - 5);
            	graphics2.setColor(new Color(color[0], color[1], color[2]));
            	graphics2.fillRect(20, 400 + i * 18, addWidth - 10, addHeight - 35);
            	graphics2.setColor(Color.BLACK);
            	graphics2.drawString(snowColorNameValues[i - rainColorNameValues.length], 20 + 48, 400 + i * 18 + 10);
                i++;
            }



            graphics2.dispose();
            ImageIO.write(image2, "png", new File(outPath + "province_combine.png"));
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
        double lon = transLon(points.get(0).longitude, width);
        double lat = transLat(points.get(0).latitude, height);
        path.moveTo(lon, lat);

        // 连接其他点
        for (int i = 1; i < points.size(); i++) {
//            Point p = latLonToScreen(points.get(i));
            lon = transLon(points.get(i).longitude, width);
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

