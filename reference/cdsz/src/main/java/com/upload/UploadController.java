package com.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;

import com.obs.dao.ObsDataMapper;
import com.station.dao.StationInfoMapper;
import com.station.pojo.StationInfoEntity;
import com.util.DataTypeUtil;
import com.util.ReadPropertiesUtil;
import com.util.ThreadPoolUtil;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

@RestController
@RequestMapping("/upload")
public class UploadController {

//    @Value("${upload.path}")
    private String filePath;

    //根据日期生成路径   2022/1/15/
//    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd/");
    private static Map<String, String> configMap = ReadPropertiesUtil.getUserConfigMap("config.properties");
    private static ExecutorService executorService = ThreadPoolUtil.getInstance();
    private static Map<String, String> headerMap = new HashMap<>();
    static
    {
    	headerMap.put("站点名称", "station_name");
		headerMap.put("观测时间", "datetime");
		headerMap.put("经度", "lon");
		headerMap.put("纬度", "lat");
		headerMap.put("气温", "tem");
		headerMap.put("本站气压", "prs");
		headerMap.put("相对湿度", "rhu");
		headerMap.put("二分钟平均风向", "win_d_avg_10mi");
		headerMap.put("二分钟平均风速", "win_s_avg_10mi");
		headerMap.put("极大风向", "win_d_max");
		headerMap.put("极大风速", "win_s_max");
		headerMap.put("小时降水量/翻斗式", "pre");
		headerMap.put("最低气温", "tem_min_24h");
		headerMap.put("最高气温", "tem_max_24h");
		headerMap.put("能见度", "vis");
		headerMap.put("总云量", "tcc");
    }
    
    @Resource
    private ObsDataMapper obsDataMapper;
    
    @Resource
    private StationInfoMapper stationInfoMapper;
    /**
     * 上传文件
     */
    @PostMapping("/uploadFile")
    public R<String> uploadFile(@RequestBody MultipartFile[] files) {

        if (files == null)
        {
            return R.error("文件不能为空");
        }
        StationInfoEntity station = stationInfoMapper.queryMaxUploadStationNum();
        if(station == null)
        {
        	station = new StationInfoEntity();
        	station.setMaxNum(981000);
        }
        
        List<StationInfoEntity> uploadStationInfo = stationInfoMapper.queryUploadStationInfo();
        Map<String, String> uploadStationMap = new HashMap<>();
        for(StationInfoEntity data : uploadStationInfo)
        {
        	uploadStationMap.put(data.getStationName(), data.getStationIdD() + "");
//        	System.out.println(data.getStationName() + ":" + data.getStationIdD());
        }
        
        station.setStationIdD(station.getMaxNum() + 1);
        int filesCount = files.length;
        int[] stationIds = new int[filesCount];
        for(int i = 0; i < filesCount; i++)
        {
        	stationIds[i] = station.getStationIdD() + i;
        }
        int i = 0;
        List<StationInfoEntity> stationList = new CopyOnWriteArrayList<>();
        Map<String, List<String>> dateListMap = new ConcurrentHashMap<>();
        CountDownLatch latch = new CountDownLatch(files.length);
        for(MultipartFile file : files)
        {
            String suffix = FileUtil.getSuffix(file.getOriginalFilename()).toLowerCase();
            if(!"xls,xlsx,csv".contains(suffix))
            {
                return R.error("文件格式不符合要求，必须是xls,xlsx,csv中的一种");
            }
            try {
				InputStream inputStream = file.getInputStream();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
//            ExcelReader reader = ExcelUtil.getReader(filePath);
//            List<Sheet> sheets = reader.getSheets();
//            int count = 0;
//            for(Sheet sheet : sheets)
//    		{
//    			int firstRowNum = sheet.getFirstRowNum();
//    			Row firstRow = sheet.getRow(firstRowNum);
//    			short firstCellNum = firstRow.getFirstCellNum();
//    			short lastCellNum = firstRow.getLastCellNum();
//    			for(int j = firstCellNum; j < lastCellNum; j++)
//    			{
//    				Cell cell = firstRow.getCell(j);
//    				String cellValue = cell.getStringCellValue();
//    				if(headerMap.containsKey(cellValue))
//    				{
//    					count++;
//    				}
//    			}
//    		}
//            if(count != headerMap.size())
//            {
//            	return R.error("文件内容格式不符合要求");
//            }
            
            //1、获取上传文件的名字
            String fileName = file.getOriginalFilename();
            String dataType = DataTypeUtil.getDataType(fileName);
            filePath = configMap.get(dataType + "_upload");
//          //3、使用工具类,防止重复 返回:af66cdc12867443787362575b92ca514.jpg
//          String fileName = PathUtils.generateFilePath(originalFilename);
//          //4、创建存储日期的文件夹
//          String datePath = sdf.format(new Date());
            //5、创建目录
            File dir = new File(filePath + File.separator + dataType);
//            File dir = new File(filePath + "test");
            //6、如果不存在就创建该文件夹
            if (!dir.exists()) {
                dir.mkdirs();
            }
            Path uploadPath = Paths.get(dir.getAbsolutePath());
            String newFileName = stationIds[i] + "." + suffix;
            System.out.println("============uploadPath============: " + (filePath + dataType + File.separator + newFileName));
            Path filePath0 = uploadPath.resolve(newFileName);

            try(InputStream inputStream = file.getInputStream();
                ReadableByteChannel inputChannel = Channels.newChannel(inputStream);
                FileChannel outputChannel = FileChannel.open(filePath0, StandardOpenOption.CREATE, StandardOpenOption.WRITE);) {
                long size = file.getSize();
                long position = 0;
                while (position < size) {
                    position += outputChannel.transferFrom(inputChannel, position, 1048576);// 1024 * 1024
                }

                executorService.execute(new UploadDataIndbThread(filePath + dataType + File.separator + newFileName, uploadStationMap, i + 1, obsDataMapper, 
                		stationInfoMapper, stationList, dateListMap, latch));
                i++;
            } catch (Exception e) {
                e.printStackTrace();
                return R.error(e.getMessage());
            }
//            System.out.println(station + " " + obsDataMapper + stationInfoMapper);
        }
        
        try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        
//        TODO 上传完数据后开始抽取历史数据
        executorService.execute(new ExtractUploadStationHisDataThread(stationInfoMapper, stationList, dateListMap));
        

        return R.success("文件上传成功");
    }

    @RequestMapping(value = "/uploadStatus")
    @ResponseBody
    public Integer uploadStatus(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Object percent = session.getAttribute("upload_percent");
        return null != percent ? (Integer) percent : 0;
    }
}
