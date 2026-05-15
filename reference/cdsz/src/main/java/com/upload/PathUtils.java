package com.upload;

import java.util.UUID;

public class PathUtils {

    public static String generateFilePath(String fileName){
        //uuid作为文件名
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        //后缀和文件后缀一致,获取文件.jpg
        int index = fileName.lastIndexOf(".");
        // test.jpg -> .jpg
        String fileType = fileName.substring(index);
        return new StringBuilder().append(uuid).append(fileType).toString();
    }
}
