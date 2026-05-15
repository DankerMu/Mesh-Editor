package com.util;

import java.util.concurrent.*;

/**
 * @PROJECT_NAME: decode
 * @Package org.decode.util
 * @Description: java类作用描述
 * @date 2022/10/31 19:08
 * @Version: 1.0
 */
public class ThreadPoolUtil {

    private ExecutorService threadService;

    private ThreadPoolUtil()
    {
        int cpus = Runtime.getRuntime().availableProcessors();
        System.out.println("系统线程数: " + cpus);
        threadService = Executors.newFixedThreadPool(cpus);
//        ArrayBlockingQueue queue = new ArrayBlockingQueue(100);
//        threadService = new ThreadPoolExecutor(cpus, cpus, 10, TimeUnit.SECONDS, queue, new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    public static ExecutorService getInstance()
    {
        return ThreadPoolUtilHolder.instance.threadService;
    }

    private static class ThreadPoolUtilHolder
    {
        private final static ThreadPoolUtil instance = new ThreadPoolUtil();
    }

}
