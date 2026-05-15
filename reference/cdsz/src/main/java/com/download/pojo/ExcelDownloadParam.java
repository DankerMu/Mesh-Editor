package com.download.pojo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @category
 * @date 2025/6/20 15:02
 * @description TODO
 */
@Data
public class ExcelDownloadParam {
    private String fileName;
    private String[] contents;
    private Map<String, List<String>> contentsMap;
}
