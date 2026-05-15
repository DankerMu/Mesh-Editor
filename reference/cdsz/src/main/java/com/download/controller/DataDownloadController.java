package com.download.controller;

import io.swagger.annotations.ApiOperation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import cn.hutool.core.io.FileUtil;

import com.alibaba.fastjson.JSON;
import com.check.pojo.CheckDataParams;
import com.check.service.inf.CheckDataService;
import com.compare.pojo.CompareDataParams;
import com.compare.service.inf.CompareService;
import com.constants.DecodeConstants;
import com.download.pojo.ExcelDownloadParam;
import com.download.pojo.ImageDownloadParam;
import com.download.pojo.WordDownloadParam;
import com.forecast.dao.StationForecastDataMapper;
import com.forecast.pojo.FcDateTimeEntity;
import com.forecast.pojo.StationForecastDataEntity;
import com.forecast.pojo.StationForecastDataParams;
import com.forecast.util.WindUtil;
import com.original.pojo.DataQcManagerEntity;
import com.original.pojo.DataQcManagerParams;
import com.original.service.QueryNafpDataServiceImpl;
import com.station.dao.StationInfoMapper;
import com.station.indb.util.LoggableStatementUtil;
import com.station.pojo.StationInfoEntity;
import com.station.pojo.TaskListEntity;
import com.station.pojo.TaskParam;
import com.tool.ExcelTool;
import com.util.NumberFormatUtil;
import com.util.ProvinceBoundaryDrawer;
import com.util.ProvinceBoundaryDrawer2400;
import com.util.ReadPropertiesUtil;
import com.util.TimeUtil;
import com.util.WordUtil;

@RestController
public class DataDownloadController {
    private static Map<String, String> elementNameMap = new HashMap<String, String>();
    private static Map<String, String> wwMap = new HashMap<String, String>();
    static {
        elementNameMap.put("at", "2米气温");
        elementNameMap.put("atmax", "日最高气温");
        elementNameMap.put("atmin", "日最低气温");
        elementNameMap.put("element", "气象要素");
        elementNameMap.put("lcc", "低云量");
        elementNameMap.put("n", "总云量");
        elementNameMap.put("ptype", "天气现象");
        elementNameMap.put("rain", "降水");
        elementNameMap.put("rain24", "累积降水");
        elementNameMap.put("rh", "相对湿度");
        elementNameMap.put("slp", "气压");
        elementNameMap.put("vis", "能见度");
        elementNameMap.put("wd", "10米风向");
        elementNameMap.put("ws", "10米风速");
        
        wwMap.put("0", "晴");
        wwMap.put("1", "雨");
        wwMap.put("2", "雪");
        wwMap.put("3", "少云");
        wwMap.put("4", "多云");
        wwMap.put("5", "阴");
    }
    @Resource
    private CompareService compareService;
    @Resource
    private CheckDataService checkDataService;
    @Resource
    private QueryNafpDataServiceImpl queryNafpDataServiceImpl;
    @Resource
    private StationInfoMapper stationInfoMapper;
    @Resource
    private StationForecastDataMapper stationForecastDataMapper;
    
    @PostMapping("/download-text")
    public ResponseEntity<byte[]> downloadText() {
        String data = "Hello, 这是动态生成的文本数据！";
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"data.txt\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(bytes);
    }

//    @PostMapping("/downloadImage")
//    public ResponseEntity<byte[]> downloadImage(@RequestBody ImageDownloadParam param)
//    {
//        String imageUrl = param.getUrl();
//        String fileName = FileUtil.getName(imageUrl);
//        byte[] bytes = null;
//        try {
//            BufferedImage image = ImageIO.read(new File(imageUrl));
//            Raster data = image.getData();
//            Object value = ReflectUtil.getSuperFieldValueByName(data, "data");
//            bytes = (byte[]) value;
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + "_" + fileName + "\"")
//                .contentType(MediaType.APPLICATION_JSON)
//                .body(bytes);
//    }
    private static Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
    @PostMapping("/downloadImage")
    public ResponseEntity<org.springframework.core.io.Resource> downloadImage(@RequestBody ImageDownloadParam param)
    {
    	String basePath = configMap.get("custom_images");
        Path imagePath1 = Paths.get(basePath);
        String imageUrl = param.getUrl();
        
//      TODO 将国界叠加到图片上，然后下载合成后的图片
        
        imageUrl = imageUrl.replace("/cdsz/", "");
//        String cmbPath = configMap.get("cmb_image_file_path");
//        String outPath = "/data/images/";
//        System.out.println("0000000000000000000: " + imageUrl);
        
//        ImageUtil.combineImages(basePath, imagePath1 + "/" + imageUrl, cmbPath, "download_" + imageUrl);
        ProvinceBoundaryDrawer2400 drawer = new ProvinceBoundaryDrawer2400();
        String title = param.getTitle();
        String subTitle = param.getSubTitle();
//        title = "战区未来24小时天气趋势预报";
//        int month = 11;
//        int day = 19;
//        int nextDay = 20;
//        subTitle = month + "月" + day + "日20时～" + nextDay + "日20时";
        drawer.combine(basePath, imagePath1 + "/" + imageUrl, "download_" + imageUrl, title, subTitle);
        
        String newimageUrl = "download_" + imageUrl;
        String fileName = FileUtil.getName(imageUrl);
        try {
            Path imagePath = imagePath1.resolve(newimageUrl);
            org.springframework.core.io.Resource resource = new UrlResource(imagePath.toUri());
            if(resource.exists() && resource.isReadable())
            {
                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
                headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
                headers.add(HttpHeaders.PRAGMA, "no-cache");
                headers.add(HttpHeaders.EXPIRES, "0");

                return ResponseEntity.ok().headers(headers).contentLength(resource.contentLength()).contentType(MediaType.IMAGE_PNG)
                        .body(resource);
            }
            else
            {
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            e.printStackTrace();

            return ResponseEntity.notFound().build();
        }

    }
    
    @ApiOperation("下载批量上传文件格式模板")
    @PostMapping("/downloadModelFile")
    public ResponseEntity<byte[]> downloadModelFile()
    {
    	String filePath = "/data/upload/model.xls";
    	Workbook workbook = ExcelTool.readExcel(filePath);
    	ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
			workbook.write(outputStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
        byte[] result = outputStream.toByteArray();
    	
    	
        String fileName = "model.xls";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + "_" + fileName + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }

    @ApiOperation("下载CSV文件")
    @PostMapping("/downloadExcel")
    public ResponseEntity<byte[]> downloadExcel(@RequestBody ExcelDownloadParam param) {
        String[] contents = param.getContents();
        Map<String, List<String>> contentsMap = param.getContentsMap();
        byte[] result = null;
        if(contentsMap != null && contentsMap.size() > 0)
        {
            List<String> headers = contentsMap.get("columns");
            StringBuilder headerStr = new StringBuilder();
//            List<String> lines = new ArrayList<>();
            for(String head : headers)
            {
                headerStr.append(head).append(",");
            }
            String header = headerStr.substring(0, headerStr.length()-1);

            for(String data : contentsMap.keySet())
            {
                if(!data.equals("columns"))
                {
                    List<String> stringList = contentsMap.get(data);
                    List<String> list = new ArrayList<>();
                    list.add(header);
                    list.addAll(stringList);
                    contentsMap.put(data, list);
                }
            }
            contentsMap.remove("columns");

            List<String> sortedList = contentsMap.keySet().stream().sorted().collect(Collectors.toList());
            Map<String, List<String>> sortedMap = new LinkedHashMap<>();
            for(String date : sortedList)
            {
                sortedMap.put(date, contentsMap.get(date));
            }

            result = ExcelTool.createSimpleExcel(sortedMap);
        }
        else if(contents != null && contents.length > 0)
        {
            List<String> lines = Arrays.asList(contents);
            result = ExcelTool.createSimpleExcel(lines);
        }

        String fileName = param.getFileName() + "_" + TimeUtil.date2String(new Date(), "yyyyMMddHHmmss") + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + "_" + fileName + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }
    
    
    @ApiOperation("下载Word文件")
    @PostMapping("/downloadWord")
    public ResponseEntity<byte[]> downloadWordWithExcel(@RequestBody WordDownloadParam params) {
        byte[] result = null;
//      TODO 添加任务id过滤条件
        TaskParam taskParam = new TaskParam();
        taskParam.setTaskId(params.getTaskId());
//        List<StationInfoEntity> forcastStations = stationInfoMapper.queryForcastStations();
        List<StationInfoEntity> forcastStations = stationInfoMapper.queryForcastStationsByTaskId(taskParam);

        int count = forcastStations.size();
        String[] stations = new String[count];
        Map<String, String> stationNameMap = new LinkedHashMap<>();
        Map<String, StationForecastDataEntity> stationDataValueMap = new LinkedHashMap<>();
        Map<String, List<StationForecastDataEntity>> stationDataListMap = new HashMap<>();
        for(int i = 0; i < count; i++)
        {
        	stations[i] = String.valueOf(forcastStations.get(i).getStationIdD());
        	stationNameMap.put(stations[i], forcastStations.get(i).getStationName());
        	stationDataValueMap.put(stations[i], new StationForecastDataEntity());
        	stationDataListMap.put(stations[i], new ArrayList<>());
        }
        StationForecastDataParams param = new StationForecastDataParams();
        param.setStations(stations);
        param.setDataTime(TimeUtil.addHours(params.getDateTime(), -8));
        
//        Date date = new Date();
        Calendar calendar = Calendar.getInstance();
//        calendar.setTime(date);
//        calendar.add(Calendar.HOUR_OF_DAY, 8);
//        String currentTime = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT);
//        String yyyyMMdd = currentTime.split(" ")[0];
//        if(currentTime.compareTo(yyyyMMdd + " 07:20:00") > 0 && currentTime.compareTo(yyyyMMdd + " 15:20:00") < 0)
//        {
//        	param.setDataTime(yyyyMMdd + " 00:00:00");
//        	param.setStartVti(12);
//        	param.setEndVti(36);
//        }
//        else if(currentTime.compareTo(yyyyMMdd + " 15:20:00") > 0)
//        {
//        	param.setDataTime(yyyyMMdd + " 12:00:00");
//        	param.setStartVti(0);
//        	param.setEndVti(24);
//        }
//        else
//        {
//        	calendar.add(Calendar.DAY_OF_MONTH, -1);
//        	yyyyMMdd = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT).split(" ")[0];
//        	param.setDataTime(yyyyMMdd + " 12:00:00");
//        	param.setStartVti(24);
//        	param.setEndVti(48);
//        }
        
        List<FcDateTimeEntity> fcDataTimeList = stationForecastDataMapper.queryFcDateTime();
        String dataTime = "";
        if(fcDataTimeList != null && fcDataTimeList.size() != 0)
        {
        	dataTime = fcDataTimeList.get(0).getDataTime();
        	param.setDataTime(dataTime);
        }
        String currentDate = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATE_FORMAT);
        currentDate = "2026-01-01";
        param.setStartDateTime(currentDate + " 12:00:00");
        calendar.setTime(TimeUtil.String2Date(param.getStartDateTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
        calendar.add(Calendar.HOUR_OF_DAY, 24);
        param.setEndDateTime(TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT));
        
        List<StationForecastDataEntity> list = stationForecastDataMapper.queryStationForecastDownload(param);
        int rowCount = stations.length + 1;
        String[][] contents = new String[rowCount][4];
//        contents[0] = new String[]{"城市", "天气现象", "温度", "风速", "风向"};
        contents[0] = new String[]{"点位", "天气", "风", "温度"};
        String station = null;
        
        if(list.get(0).getAt() == DecodeConstants.UNDEF_DOUBLE_VALUE)
    	{
        	dataTime = fcDataTimeList.get(1).getDataTime();
        	calendar.setTime(TimeUtil.String2Date(dataTime, TimeUtil.DEFAULT_DATETIME_FORMAT));
        	calendar.add(Calendar.HOUR_OF_DAY, -12);
        	param.setDataTime(dataTime);
        	list = stationForecastDataMapper.queryStationForecastDownload(param);
    	}
        
        for(int i = 0; i < list.size(); i++)
        {
        	station = list.get(i).getStation();
        	stationDataListMap.get(station).add(list.get(i));
        }
        
        for(String stationNum : stationDataListMap.keySet())
        {
        	List<StationForecastDataEntity> dataList = stationDataListMap.get(stationNum);
        	if(dataList == null || dataList.size() == 0)
        	{
        		continue;
        	}
        	double maxAt = -999999;
            double minAt = 999999;
            double maxWs = -999999;
            double maxWd = 0;
            int maxPtype = 0;
        	for(StationForecastDataEntity entity : dataList)
        	{
        		if(maxAt < entity.getAt())
        		{
        			maxAt = entity.getAt();
        		}
        		if(minAt > entity.getAt())
        		{
        			minAt = entity.getAt();
        		}
        		if(maxWs < entity.getWs())
        		{
        			maxWs = entity.getWs();
        			maxWd = entity.getWd();
        		}
        		if(maxPtype < entity.getPtype())
        		{
        			maxPtype = (int) entity.getPtype();
        		}
        	}
        	StationForecastDataEntity dataEntity = stationDataValueMap.get(stationNum);
        	dataEntity.setAtmax(maxAt);
        	dataEntity.setAtmin(minAt);
        	dataEntity.setWsStr(WindUtil.getWs(NumberFormatUtil.numFormat(maxWs, 1)));
        	dataEntity.setWdStr(WindUtil.getWd(maxWd));
//        	dataEntity.setPtype(maxPtype);
        	dataEntity.setPtypeStr(getPtype(dataList, param.getStartDateTime()));
        }
        int i = 0;
        for(String stationNum : stationDataValueMap.keySet())
        {
        	StationForecastDataEntity dataEntity = stationDataValueMap.get(stationNum);
    		contents[i + 1][0] = stationNameMap.get(stationNum);
//    		contents[i + 1][1] = wwMap.get(String.valueOf((int)dataEntity.getPtype()));
    		contents[i + 1][1] = dataEntity.getPtypeStr();
    		contents[i + 1][3] = (int)NumberFormatUtil.numFormat(dataEntity.getAtmin(), 0) + "～" + (int)NumberFormatUtil.numFormat(dataEntity.getAtmax(), 0) + "℃";
//    		contents[i + 1][3] = String.valueOf(NumberFormatUtil.numFormat(dataEntity.getWs(), 1));
//    		contents[i + 1][4] = String.valueOf(NumberFormatUtil.numFormat(dataEntity.getWd(), 1));
    		contents[i + 1][2] = dataEntity.getWdStr() + dealWs(dataEntity.getWsStr());
//    		contents[i + 1][4] = dataEntity.getWdStr();
    		i++;
        }
        
        XWPFDocument doc = new XWPFDocument();
        calendar.setTime(new Date());
        
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        int nextYear = calendar.get(Calendar.YEAR);
        int nextMonth = calendar.get(Calendar.MONTH) + 1;
        int nextDay = calendar.get(Calendar.DAY_OF_MONTH); 
        
        String title = year + "年" + month + "月" + day + "日20时～" + nextYear + "年" + nextMonth + "月" + nextDay + "日20时预报单";
        int titleFontSize = 20;
        int contentFontSize = 16;
        WordUtil.addTitle(doc, title, 1, titleFontSize);
        WordUtil.addTable(doc, contents, contentFontSize);
        result = WordUtil.toBytes(doc);
        
        String fileName = param.getDataTime() + "_" + TimeUtil.date2String(new Date(), "yyyyMMddHHmmss") + "预报单.docx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + "_" + fileName + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }

    private String dealWs(String ws)
    {
        String result = "";
        String t = ws.replace("级", "");
        int wsNum = Integer.parseInt(t);
        if(wsNum <= 2)
        {
            result = "1～2级";
        }
        else
        {
            result = (wsNum - 1) + "～" + wsNum + "级";
        }


        return result;
    }

    @GetMapping("/downloadWordTest")
    public ResponseEntity<byte[]> downloadWordWithExcelTest() {
        byte[] result = null;
        String[][] contents = new String[4][4];
//        contents[0] = new String[]{"城市", "天气现象", "温度", "风速", "风向"};
        contents[0] = new String[]{"点 位", "天 气", "风", "温 度"};
        contents[1] = new String[]{"乌鲁木齐", "晴天", "风西北风4~5级", "20~32℃"};
        contents[2] = new String[]{"拉萨", "晴天", "风西北风1~2级", "10~22℃"};
        contents[3] = new String[]{"兰州", "晴天", "风西北风3~4级", "21~28℃"};

        Calendar calendar = Calendar.getInstance();
        XWPFDocument doc = new XWPFDocument();
        calendar.setTime(new Date());

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        int nextYear = calendar.get(Calendar.YEAR);
        int nextMonth = calendar.get(Calendar.MONTH) + 1;
        int nextDay = calendar.get(Calendar.DAY_OF_MONTH);

        String title = year + "年" + month + "月" + day + "日20时～" + nextYear + "年" + nextMonth + "月" + nextDay + "日20时预报单";
        WordUtil.addTitle(doc, title, 1, 10);
        WordUtil.addTable(doc, contents, 10);
        result = WordUtil.toBytes(doc);

        String fileName = "预报单.docx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + "_" + fileName + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }

    private String getPtype(List<StationForecastDataEntity> dataList, String startValidDate)
    {
//        ptypeMap.put("0.0", "晴");
//        ptypeMap.put("1.0", "雨");
//        ptypeMap.put("2.0", "雪");
//        ptypeMap.put("3.0", "少云");
//        ptypeMap.put("4.0", "多云");
//        ptypeMap.put("5.0", "阴");
    	String result = "";
    	Calendar calendar = Calendar.getInstance();
    	calendar.setTime(TimeUtil.String2Date(startValidDate, TimeUtil.DEFAULT_DATETIME_FORMAT));
    	calendar.add(Calendar.HOUR_OF_DAY, 12);
    	String str = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT);
    	StationForecastDataEntity data = null;
    	Set<Integer> set = new HashSet<>();
    	double rain = 0;
    	int index = 0;
    	for(int i = 0; ; i++)
    	{
    		data = dataList.get(i);
    		if(data.getValiddate().compareTo(str) > 0)
    		{
    			index = i;
    			break;
    		}
    		set.add((int)data.getPtype());
    		rain += data.getRain();
    	}
    	int onePtype1 = getOnePtype(set);
    	
    	set.clear();
    	for(int i = index, count = dataList.size(); i < count; i++)
    	{
    		data = dataList.get(i);
    		set.add((int)data.getPtype());
    		rain += data.getRain();
    	}
    	int onePtype2 = getOnePtype(set);
    	
    	if(onePtype1 == onePtype2)
    	{
    		if(onePtype1 == 1)
    		{
    			result = getRain(rain);
    		}
    		else if(onePtype1 == 2)
    		{
    			result = getSnow(rain);
    		}
    		else
    		{
    			result = wwMap.get(onePtype1 + "");
    		}
    	}
    	else
    	{
    		String str1 = "";
    		if(onePtype1 == 1)
    		{
    			str1 = getRain(rain);
    		}
    		else if(onePtype1 == 2)
    		{
    			str1 = getSnow(rain);
    		}
    		else
    		{
    			str1 = wwMap.get(onePtype1 + "");
    		}
    		String str2 = "";
    		if(onePtype2 == 1)
    		{
    			str2 = getRain(rain);
    		}
    		else if(onePtype2 == 2)
    		{
    			str2 = getSnow(rain);
    		}
    		else
    		{
    			str2 = wwMap.get(onePtype2 + "");
    		}
    		
    		result = str1 + "转" + str2;
    	}
    	
    	return result;
    }
    
    private String getRain(double value)
    {
    	String result = "";
    	if(value >= 0.1 && value < 10)
    	{
    		result = "小雨";
    	}
    	else if(value >= 10 && value < 25)
    	{
    		result = "中雨";
    	}
    	else if(value >= 25 && value < 50)
    	{
    		result = "大雨";
    	}
    	else if(value >= 50 && value < 100)
    	{
    		result = "暴雨";
    	}
    	else if(value >= 100 && value < 250)
    	{
    		result = "大暴雨";
    	}
    	else if(value >= 250)
    	{
    		result = "大暴雨";
    	}
    	
    	return result;
    }
    
    private String getSnow(double value)
    {
    	String result = "";
    	if(value >= 0.1 && value < 2.5)
    	{
    		result = "小雪";
    	}
    	else if(value >= 2.5 && value < 5)
    	{
    		result = "中雪";
    	}
    	else if(value >= 5 && value < 10)
    	{
    		result = "大雪";
    	}
    	else if(value >= 10)
    	{
    		result = "暴雪";
    	}
    	
    	return result;
    }
    
    private int getOnePtype(Set<Integer> set)
    {
    	int result = 0;
    	if(set.contains(2))
    	{
    		result = 2;
    	}
    	else if(set.contains(1))
    	{
    		result = 1;
    	}
    	else if(set.contains(5))
    	{
    		result = 5;
    	}
    	else if(set.contains(4))
    	{
    		result = 4;
    	}
    	else if(set.contains(3))
    	{
    		result = 3;
    	}
    	
    	
    	return result;
    }
    
    
//   ============================================================================================================================================================== 

    @PostMapping("/downloadDataManagerData")
    public ResponseEntity<byte[]> downloadDataManagerData(@RequestBody DataQcManagerParams params)
    {
        List<DataQcManagerEntity> list = null;
        if(params.getDataSource().equals("all"))
        {
        	list = queryNafpDataServiceImpl.queryDataManagerCountAll(params);
        }
        else
        {
        	list = queryNafpDataServiceImpl.queryDataManagerCount(params);
        }
        String header = "序号,数据类型,时间,应到数量,实到数量,缺失数量,到报率(%)";
        List<String> lines = new ArrayList<>();
        lines.add(header);
        StringBuilder line = null;
        int i = 0;
        for(DataQcManagerEntity data : list)
        {
            line = new StringBuilder();
            line.append(++i).append(",");
            line.append(data.getDataSource()).append(",");
            line.append(data.getDataTime()).append(",");
            line.append(data.getTotal()).append(",");
            line.append(data.getArrived()).append(",");
            line.append(data.getUnarrived()).append(",");
            line.append(data.getRate());
            lines.add(line.toString());
        }

        byte[] result = ExcelTool.createSimpleExcel(lines);
        String fileName = params.getDataSource() + "_" + TimeUtil.date2String(new Date(), "yyyyMMddHHmmss") + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + "_" + fileName + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }




    @PostMapping("/downloadGribCheckData")
    public ResponseEntity<byte[]> downloadGribCheckData(@RequestBody CheckDataParams params) {
//          datasource  method      vti
        Map<String, Map<String, Map<String, Double>>> datasValueMap = checkDataService.checkGribDataHour(params);
//        String jsonData = JSON.toJSONString(datasValueMap);

        StringBuilder vtiHeader = new StringBuilder();
        StringBuilder methodHeader = new StringBuilder(",");
        StringBuilder values = null;
        List<String> lines = new ArrayList<>();
        boolean first = true;
        String totalLine = null;
        for(String station : datasValueMap.keySet())
        {
            values = new StringBuilder();
//            Map<String, double[]> vtiMap = datasValueMap.get(station);
            Map<String, double[]> vtiMap = new HashMap<>();
            for(String vti : vtiMap.keySet())
            {
                if(first)
                {
                    vtiHeader.append(vti).append(",");
                    methodHeader.append("准确率,空报率,漏报率").append(",");
                }
                double[] doubles = vtiMap.get(vti);
                values.append(doubles[0]).append(",")
                       .append(doubles[1]).append(",")
                       .append(doubles[2]).append(",");
            }
            if(first)
            {
                lines.add(vtiHeader.toString());
                lines.add(methodHeader.toString());
            }
            first = false;
            if(station.equals("total"))
            {
                totalLine = "合计," + values;
            }
            else
            {
                lines.add(station + "," + values);
            }
        }
        lines.add(totalLine);

        byte[] bytes = ExcelTool.createExcel(lines, elementNameMap, 1);
        // 将工作簿写入字节数组输出流
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        try {
//            excel.write(outputStream);
//            excel.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        byte[] bytes = outputStream.toByteArray();
        Calendar calendar = Calendar.getInstance();
        String fileName = TimeUtil.date2String(calendar.getTime(), "yyyyMMddHHmmss") + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + "rain" + "_" + fileName + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(bytes);
    }

    @PostMapping("/downloadStationCheckData")
    public ResponseEntity<byte[]> downloadStationCheckData(@RequestBody CheckDataParams param) {
        String csvData = "Name,Age,City\nAlice,30,Beijing\nBob,25,Shanghai";
        Map<String, Map<String, Map<String, Map<String, Double>>>> datasValueMap = checkDataService.checkStationDataHour(param);
//      station element vti method
        StringBuilder elementHeader = new StringBuilder();
        StringBuilder vtiHeader = new StringBuilder();
        StringBuilder methodHeader = new StringBuilder();
        StringBuilder values = null;
        List<String> lines = new ArrayList<>();
        boolean first = true;
        String totalLine = null;
        for(String station : datasValueMap.keySet())
        {
            Map<String, Map<String, Map<String, Double>>> elementMap = datasValueMap.get(station);
            values = new StringBuilder();
            for(String element : elementMap.keySet())
            {
                if(first)
                {
                    elementHeader.append(element).append(",");
                }
                Map<String, Map<String, Double>> vtiMap = elementMap.get(element);
                for(String vti : vtiMap.keySet())
                {
                    if(first)
                    {
                        vtiHeader.append(vti).append(",");
                    }
                    Map<String, Double> methodMap = vtiMap.get(vti);
                    for(String method : methodMap.keySet())
                    {
                        if(first)
                        {
                            methodHeader.append(method).append(",");
                        }
                        values.append(methodMap.get(method)).append(",");
                    }
                }
            }
            if(first)
            {
                lines.add(elementHeader.toString());
                lines.add(vtiHeader.toString());
                lines.add(methodHeader.toString());
            }
            first = false;
            if(station.equals("total"))
            {
                totalLine = "合计," + values;
            }
            else
            {
                lines.add(station + "," + values);
            }
        }
        lines.add(totalLine);
//        vis,at,ws,wd,
//        72,72,72,72,
//        MAE,CORR,RMSE,MAE,CORR,RMSE,MAE,CORR,RMSE,MAE,CORR,RMSE,
//        1936436.486,NaN,1936436.486,31.924000000000003,NaN,31.924000000000003,27.6,NaN,27.6,146.176,NaN,292.352,
//        1936436.486,NaN,1936436.486,15.962000000000002,NaN,15.962000000000002,13.8,NaN,13.8,146.176,NaN,146.176

        byte[] bytes = ExcelTool.createExcel(lines, elementNameMap, 2);

        // 将工作簿写入字节数组输出流
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        try {
//            excel.write(outputStream);
//            excel.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        byte[] bytes = outputStream.toByteArray();
        Calendar calendar = Calendar.getInstance();
        String fileName = TimeUtil.date2String(calendar.getTime(), "yyyyMMddHHmmss") + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + "station" + "_" + fileName + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(bytes);
    }

    @PostMapping("/downloadGribCompareData")
    public ResponseEntity<byte[]> downloadGribCompareData(@RequestBody CompareDataParams params) {

        Map<String, Map<String, Double>> stringMap = compareService.compareGribData(params);
        StringBuilder header = new StringBuilder(",");
        StringBuilder line = null;
        boolean first = true;
        List<String> lines = new ArrayList<>();
        for(String dataSource : stringMap.keySet())
        {
            line = new StringBuilder();
            // 使用 TreeMap 按 Key 升序排序
            Map<String, Double> sortedMap = new TreeMap<>(stringMap.get(dataSource));

            for(String time : sortedMap.keySet())
            {
                if(first)
                {
                    header.append(time).append(",");
                }
                line.append(sortedMap.get(time) + ",");
            }
            if(first)
            {
                lines.add(line.toString());
            }
            first = false;
        }

        String jsonData = JSON.toJSONString(stringMap);
        byte[] bytes = jsonData.getBytes(StandardCharsets.UTF_8);

        Calendar calendar = Calendar.getInstance();
        String fileName = TimeUtil.date2String(calendar.getTime(), "yyyyMMddHHmmss") + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + params.getStation() + "_" + fileName + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(bytes);
    }

    @PostMapping("/downloadStationCompareData")
    public ResponseEntity<byte[]> downloadStationCompareData(@RequestBody CompareDataParams params) {

//        Map<String, Map<String, List<Map<String, String>>>> stringListMap = compareService.compareStationData(params);

//        Calendar calendar = Calendar.getInstance();
//        String fileName = TimeUtil.date2String(calendar.getTime(), "yyyyMMddHHmmss") + ".csv";
//        StringBuilder line = null;
//        StringBuilder header = new StringBuilder(",");
//        StringBuilder obs = null;
//        List<String> lines = new ArrayList<>();
//        boolean first = true;
//        for(String element : stringListMap.keySet())
//        {
//            line = new StringBuilder("预报 " + elementNameMap.get(element) + ",");
//            obs = new StringBuilder("实况 " + elementNameMap.get(element) + ",");
//
//            // 按 Map 中的唯一 Key 升序排序整个 List
//            List<Map<String, String>> sortedList = stringListMap.get(element).stream()
//                    .sorted(Comparator.comparing(
//                            map -> map.keySet().iterator().next() // 提取每个 Map 的 Key
//                    ))
//                    .collect(Collectors.toList());
//
//            for(Map<String, String> map : sortedList)
//            {
//                for(String time : map.keySet())
//                {
//                    String[] split = map.get(time).split(",");
//                    if(first)
//                    {
//                        header.append(time);
//                        header.append(",");
//                    }
//                    obs.append(split[0]);
//                    obs.append(",");
//                    line.append(split[1]);
//                    line.append(",");
//                }
//            }
//            if(first)
//            {
//                lines.add(header.toString());
//            }
//            first = false;
//            lines.add(obs.toString());
//            lines.add(line.toString());
//        }
//        String jsonData = JSON.toJSONString(lines);
//        byte[] bytes = jsonData.getBytes(StandardCharsets.UTF_8);

//        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + params.getStation() + "_" + fileName + "\"")
//                .contentType(MediaType.APPLICATION_JSON)
//                .body(bytes);
        return null;
    }
}