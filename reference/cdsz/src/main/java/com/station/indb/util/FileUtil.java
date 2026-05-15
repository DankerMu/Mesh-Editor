package com.station.indb.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;


public class FileUtil {
    public static byte[] getBytes(String filePath) {

        byte[] buffer = null;
        File file = new File(filePath);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file); ByteArrayOutputStream bos = new ByteArrayOutputStream(1000);) {
                byte[] b = new byte[1000];
                int n;
                while ((n = fis.read(b)) != -1) {
                    bos.write(b, 0, n);
                }
                buffer = bos.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return buffer;
    }

    public static List<String> readDataFromBytes(byte[] datas, String encoding, int count) {
        List<String> readList = new ArrayList<String>();
        InputStream input = new ByteArrayInputStream(datas);

        try (InputStreamReader read = new InputStreamReader(input, encoding); BufferedReader bufferedReader = new BufferedReader(read);) {
            String lineTxt = null;
            List<String> tempList = new ArrayList<String>();
            while ((lineTxt = bufferedReader.readLine()) != null) {
                if (!lineTxt.equals("")) {

                    if (lineTxt.endsWith("=")) {
                        tempList.add(lineTxt.substring(0, lineTxt.length() - 1));
                        if (tempList.size() == count) {
                            readList.addAll(tempList);
                            tempList.clear();
                        } else if (tempList.size() == 3) {
                            tempList.add(0, tempList.get(0) + " 000");
                            tempList.remove(1);
                            tempList.add("");
                            readList.addAll(tempList);
                            tempList.clear();
                        }
                    } else {
                        tempList.add(lineTxt);
                    }
                }
            }
            read.close();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return readList;
    }

    public static void writeBytesToFile(String outFilePath, List<byte[]> values) {
        File file = new File(outFilePath);
        File parent = file.getParentFile();
        if ((!parent.exists()) && !parent.mkdirs()) {
            System.out.println("文件不存在");
            return;
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            for (byte[] value : values) {
                fos.write(value);
            }
            fos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeBytesToFile(String outFilePath, double[][] values) {

        File file = new File(outFilePath);
        File parent = file.getParentFile();
        if ((!parent.exists()) && !parent.mkdirs()) {
            System.out.println("文件不存在");
            return;
        }


        List<byte[]> list = new ArrayList<>();
        for (double[] value : values) {
            for (double v : value) {
                list.add(ByteUtil.getBytes(v));
            }
        }

        try (FileOutputStream fos = new FileOutputStream(file); BufferedOutputStream bi = new BufferedOutputStream(fos)) {
            for (byte[] value : list) {
                bi.write(value);
            }
            fos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeBytesToFile(String outFilePath, double[][] values, double[][] values1) {

        File file = new File(outFilePath);
        File parent = file.getParentFile();
        if ((!parent.exists()) && !parent.mkdirs()) {
            System.out.println("文件不存在");
            return;
        }


        List<byte[]> list = new ArrayList<>();
        for (double[] value : values) {
            for (double v : value) {
                list.add(ByteUtil.getBytes(v));
            }
        }
        for (double[] value : values1) {
            for (double v : value) {
                list.add(ByteUtil.getBytes(v));
            }
        }

        try (FileOutputStream fos = new FileOutputStream(file); BufferedOutputStream bi = new BufferedOutputStream(fos)) {
            for (byte[] value : list) {
                bi.write(value);
            }
            fos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
//			System.out.println(outFilePath + "  文件输出完成");
        }

    }

    public static void writeBytesToFile(String outFilePath, float[][] values) {
        File file = new File(outFilePath);
        File parent = file.getParentFile();
        if ((!parent.exists()) && !parent.mkdirs()) {
            System.out.println("文件不存在");
            return;
        }

        List<byte[]> list = new ArrayList<>();
        for (float[] value : values) {
            for (float v : value) {
                list.add(ByteUtil.getBytes(v));
            }
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            for (byte[] value : list) {
                fos.write(value);
            }
            fos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void writeBytesToFile(String outFilePath, float[][] values, float[][] values1) {

        File file = new File(outFilePath);
        File parent = file.getParentFile();
        if ((!parent.exists()) && !parent.mkdirs()) {
            System.out.println("文件不存在");
            return;
        }


        List<byte[]> list = new ArrayList<>();
        for (float[] value : values) {
            for (float v : value) {
                list.add(ByteUtil.getBytes(v));
            }
        }
        for (float[] value : values1) {
            for (float v : value) {
                list.add(ByteUtil.getBytes(v));
            }
        }

        try (FileOutputStream fos = new FileOutputStream(file); BufferedOutputStream bi = new BufferedOutputStream(fos)) {
            for (byte[] value : list) {
                bi.write(value);
            }
            fos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
//			System.out.println(outFilePath + "  文件输出完成");
        }

    }
    
//    private static ExecutorService threadService = ThreadPoolUtil.getInstance();
//    public static void writeBytesToFileMulti(String outFilePath, double[] values) {
//        File file = new File(outFilePath);
//        File parent = file.getParentFile();
//        if ((!parent.exists()) && !parent.mkdirs()) {
//            System.out.println("文件不存在");
//            return;
//        }
//        RandomAccessFile fileAcc = null;
//		try {
//			fileAcc = new RandomAccessFile(outFilePath, "rw");
//		} catch (FileNotFoundException e1) {
//			e1.printStackTrace();
//		}
//		List<byte[]> list = new ArrayList<>();
//        List<Thread> lists = new ArrayList<>();
//        double threadCount = 5;
//        int dataCount = (int) Math.ceil(values.length / threadCount);
//        int count = 0, i = 0;
//        CountDownLatch countLatch = new CountDownLatch((int) threadCount);
//        for (double value : values) {
//            list.add(ByteUtil.getBytes(value));
//            count++;
//            if(count % dataCount == 0)
//            {
//            	threadService.execute(new MultiThreadFileWriter(fileAcc, list, i * dataCount, countLatch));
//            	i++;
//            	list.clear();
//            	list = new CopyOnWriteArrayList<>();
//            }
//        }
//        if(list.size() != 0)
//        {
//        	threadService.execute(new MultiThreadFileWriter(fileAcc, list, i * dataCount, countLatch));
//        }
//        try {
//        	countLatch.await();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//        System.out.println("RandomAccessFile");
//
//    }

    public static void writeBytesToFileByNio(String outFilePath, double[] values){
        File file = new File(outFilePath);
        File parent = file.getParentFile();
        if ((!parent.exists()) && !parent.mkdirs()) {
            System.out.println("文件不存在");
            return;
        }
        
        byte[] bb = new byte[values.length * 8];
		for(int i = 0, count = values.length; i < count; i = i + 8)
		{
			byte[] bs = ByteUtil.getBytes(values[i]);
			bb[i] = bs[0];
			bb[i + 1] = bs[1];
			bb[i + 2] = bs[2];
			bb[i + 3] = bs[3];
			bb[i + 4] = bs[4];
			bb[i + 5] = bs[5];
			bb[i + 6] = bs[6];
			bb[i + 7] = bs[7];
		}
		ByteBuffer b = ByteBuffer.allocate(bb.length);
		b.put(bb);
		b.flip();
		try {
			FileOutputStream out = new FileOutputStream(new File(outFilePath));
			FileChannel channel = out.getChannel();
			channel.write(b);
			channel.close();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
        
    }
    
    public static void writeBytesToFile(String outFilePath, double[] values){
    	File file = new File(outFilePath);
    	File parent = file.getParentFile();
    	if ((!parent.exists()) && !parent.mkdirs()) {
    		System.out.println("文件不存在");
    		return;
    	}
    	List<byte[]> list = new ArrayList<>();
    	for (double value : values) {
    		list.add(ByteUtil.getBytes(value));
    	}
    	
    	try (FileOutputStream fos = new FileOutputStream(file)) {
    		for (byte[] value : list) {
    			fos.write(value);
    		}
    		fos.flush();
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	
    }

    public static void writeBytesToFile(String outFilePath, float[] values) {
        File file = new File(outFilePath);
        File parent = file.getParentFile();
        if ((!parent.exists()) && !parent.mkdirs()) {
            System.out.println("文件不存在");
            return;
        }
        List<byte[]> list = new ArrayList<>();
        for (float value : values) {
            list.add(ByteUtil.getBytes(value));
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            for (byte[] value : list) {
                fos.write(value);
            }
            fos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeStrToFile(String value, String outPath) {
        File file = new File(outPath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try (OutputStreamWriter write = new OutputStreamWriter(new FileOutputStream(file), "utf-8");) {

            write.write(value);
            write.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeDoubleToFile(double[] value, String outPath) {
        File file = new File(outPath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < value.length; i++) {
            stringBuilder.append(value[i]);
            if (i != value.length - 1) {
                stringBuilder.append(",");
            }
        }
        try (OutputStreamWriter write = new OutputStreamWriter(new FileOutputStream(file), "utf-8");) {

            write.write(stringBuilder.toString());
            write.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeStrToFile(String value, String outPath, String encoding) {
        File file = new File(outPath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try (OutputStreamWriter write = new OutputStreamWriter(new FileOutputStream(file), encoding);) {

            write.write(value);
            write.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeListToFile(List<String> list, String outPath) {
        File file = new File(outPath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try (OutputStreamWriter write = new OutputStreamWriter(new FileOutputStream(file), "utf-8");
             BufferedWriter bw = new BufferedWriter(write);) {

            for (String line : list) {
                bw.append(line);
                bw.newLine();
            }
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeListToFile(List<String> list, String outPath, String encoding) {
        File file = new File(outPath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try (OutputStreamWriter write = new OutputStreamWriter(new FileOutputStream(file), encoding);
             BufferedWriter bw = new BufferedWriter(write);) {

            for (String line : list) {
                bw.append(line);
                bw.newLine();
            }
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> readFileContent(String filePath, String encoding) {
        List<String> result = new ArrayList<>();
        File file = new File(filePath);
        if (file.exists()) {
            try (InputStreamReader input = new InputStreamReader(new FileInputStream(file), encoding); BufferedReader br = new BufferedReader(input);) {
                String line = null;
                while ((line = br.readLine()) != null) {
                    result.add(line);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        return result;
    }

    public static List<String> readFileContent(String filePath) {
        List<String> result = new ArrayList<>();
        File file = new File(filePath);
        if (file.exists()) {
            try (InputStreamReader input = new InputStreamReader(new FileInputStream(file), "utf-8"); BufferedReader br = new BufferedReader(input);) {
                String line = null;
                while ((line = br.readLine()) != null) {
                    result.add(line);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        return result;
    }

    public static List<String> readFileContent(InputStream inputStream) {
        List<String> result = new ArrayList<>();

        try (InputStreamReader read = new InputStreamReader(inputStream); BufferedReader br = new BufferedReader(read);) {
            String line = null;
            while ((line = br.readLine()) != null) {
                result.add(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        return result;
    }

    public static String readFileFirstLine(String filePath) {
        File file = new File(filePath);
        String line = null;
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file));) {
                line = br.readLine();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        return line;
    }

    public static void appendFileContent(List<String> lines, String outPath) {
        try (FileWriter fw = new FileWriter(outPath, true); PrintWriter bw = new PrintWriter(fw)) {

            for (String line : lines) {
                bw.println(line);
                bw.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void combineCsvFile(String filePath, String filePath1)
    {
        List<String> lines = readFileContent(filePath);
        List<String> lines1 = readFileContent(filePath1);
        String[] split = null;
        Map<String, Integer> map = new HashMap<>();
        Map<String, Integer> map1 = new HashMap<>();
        for(int i = 0, count = lines.size(); i < count; i++)
        {
            split = lines.get(i).split(",");
            map.put(split[0] + split[1], i);
        }
        for(int i = 0, count = lines1.size(); i < count; i++)
        {
            split = lines1.get(i).split(",");
            map1.put(split[0] + split[1], i);
        }

        for(String key : map1.keySet())
        {
            if(map.containsKey(key))
            {
                Integer i = map.get(key);
                if(i == 0)
                {
                    continue;
                }
                String line = lines.get(i);
                Integer ii = map1.get(key);
                String line1 = lines1.get(ii);
                List<Integer> indices = IntStream.range(0, line1.length())
                        .filter(j -> line1.charAt(j) == ',')
                        .boxed()
                        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
                lines.remove(line);
                lines.add(i, line + line1.substring(indices.get(1)));
            }
        }
        List<Integer> indices = IntStream.range(0, lines1.get(0).length())
                .filter(j -> lines1.get(0).charAt(j) == ',')
                .boxed()
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        String header = lines.get(0);
        lines.remove(header);
        lines.add(0, header + lines1.get(0).substring(indices.get(1)));

        cn.hutool.core.io.FileUtil.writeLines(lines, "E:\\datas\\grib\\grapes-gfs/test.csv", "utf-8");
    }

    public static boolean exist(String filePath)
    {
        boolean result = false;
        File file = new File(filePath);
        if(file.exists() && file.length() > 0)
        {
            result = true;
        }

        return result;
    }

    public static boolean exists(String filePath)
    {
        File file = new File(filePath);

        return file.exists();
    }
}
