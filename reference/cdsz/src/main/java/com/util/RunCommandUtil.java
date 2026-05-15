package com.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

//import org.apache.log4j.Logger;

public class RunCommandUtil {



	/**
	 * 运行Linux命令
	 *
	 * @return 返回结果
	 * @date 2022/5/24 16:56
	 */

	public static List<String> run(String[] command) {
		String result = "";
		List<String> rspList = new ArrayList<>();
		// String[] command = {"/app/unpack"};
//		String[] command = {"chmod", "-R", "777", path};
		// String[] args = {path};

		Runtime run = Runtime.getRuntime();

		InputStreamReader stdISR = null;
		InputStreamReader errISR = null;

		Process process = null;

		BufferedReader stdBR = null;
		BufferedReader errBR = null;

		try {
			for (int i = 0; i < command.length; i++) {
				System.out.println("参数" + i + ": " + command[i] + "\r");
			}
			process = run.exec(command);
			String line = null;
			// 读取脚本中的输出信息
			stdISR = new InputStreamReader(process.getInputStream());
			stdBR = new BufferedReader(stdISR);
			while ((line = stdBR.readLine()) != null) {
//				System.out.println("STD LINE： " + line);
				rspList.add(line);
			}

			// 执行脚本后出现的错误信息
			errISR = new InputStreamReader(process.getErrorStream());
			errBR = new BufferedReader(errISR);
			while ((line = errBR.readLine()) != null) {
				System.out.println("ERR line: " + line);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// 释放内存空间
			if (stdISR != null) {
				try {
					stdISR.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			if (errISR != null) {
				try {
					errISR.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			if (stdBR != null) {
				try {
					stdBR.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			if (errBR != null) {
				try {
					errBR.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			if (process != null) {
				process.destroy();
				process = null;
			}
		}
		return rspList;
	}



	
	/**
	 * @category 运行管道命令
	 * 2018-3-4 上午11:15:58
	 * @param commands
	 */
	public static void runShFile(String[] commands){
		// sh是运行脚本的命令(可以省略)；空格；第二个是.sh脚本文件的绝对路径；空格；后面跟参数，参数之间使用空格隔开;
		Process process = null;
		Runtime runtime = Runtime.getRuntime();
		try {
			process = runtime.exec(commands[2]);
			
			CommandStreamGobbler errorGobbler = new CommandStreamGobbler(process, process.getErrorStream(), commands[2], "ERR");
			CommandStreamGobbler outputGobbler = new CommandStreamGobbler(process, process.getInputStream(), commands[2], "STD");
			errorGobbler.start();
			while(!errorGobbler.isReady())
			{
				Thread.sleep(10);
			}
			
			outputGobbler.start();
			while(!outputGobbler.isReady())
			{
				Thread.sleep(10);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * 通过命令运行shell脚本
	 * @param commands -- 脚本命令
	 * @throws IOException
	 * void
	 * 2017年2月21日 上午10:08:29
	 */
//	Logger log = LogUtil.getLogger(RunCommandUtil.class);
	public static List<String> runShFile(String commands){
		// sh是运行脚本的命令(可以省略)；空格；第二个是.sh脚本文件的绝对路径；空格；后面跟参数，参数之间使用空格隔开;
		Process process = null;
		Runtime runtime = Runtime.getRuntime();
		List<String> result = new ArrayList<>();
//		log.info("运行时: " + runtime.toString());
		try {
//			if(commands.contains("testBatch.ncl"))
//			{
//				process = runtime.exec(new String[]{"/bin/bash", "-c", commands});
//			}
//			else
//			{
//			}
			process = runtime.exec(commands);
//			process.waitFor();
//			log.info("命令已执行: " + commands);
			InputStreamReader stdISR = null;
			InputStreamReader errISR = null;
			String line = null;
			// 读取脚本中的输出信息
			stdISR = new InputStreamReader(process.getInputStream());
			BufferedReader stdBR = new BufferedReader(stdISR);
			while ((line = stdBR.readLine()) != null) {
				result.add(line);
				System.out.println("STD LINE： " + line);
			}
			// 执行脚本后出现的错误信息
			errISR = new InputStreamReader(process.getErrorStream());
			BufferedReader errBR = new BufferedReader(errISR);
			while ((line = errBR.readLine()) != null) {
				result.add(line);
				System.out.println("ERR line: " + line);
			}
//			log.info("开始释放内存空间");
			// 释放内存空间
			if (stdISR != null) {
				stdISR.close();
			}
			if (errISR != null) {
				errISR.close();
			}
			if (stdBR != null) {
				stdBR.close();
			}
			if (errBR != null) {
				errBR.close();
			}
			if (process != null) {
				process.destroy();
				process = null;
			}
//			log.info("任务执行结束");
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}
	
	public static void main(String[] args) {
		RunCommandUtil run = new RunCommandUtil();
		for(int i = 1; i <= 30; i++)
		{
			if(i == 16)
			{
				continue;
			}
			String command = "cp /root/netcdf/GRB/shp/month/OuZhou/24/echhce50.160 /root/netcdf/GRB/shp/month/OuZhou/24/echhce50." + (i < 10 ? "0" + i : i + "") + "0";
//			command = "cp /root/netcdf/GRB/shp/month/ZongCan/024/zc_mwf_height_500_201409250800.024 /root/netcdf/GRB/shp/month/ZongCan/024/zc_mwf_height_500_201409" + (i < 10 ? "0" + i : i + "") + "0800.024";
//			run.runShFile(command);
		}
	}
}
