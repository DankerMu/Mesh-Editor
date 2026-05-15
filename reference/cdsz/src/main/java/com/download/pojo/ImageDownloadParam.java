package com.download.pojo;

import lombok.Data;

/**
 * @category
 * @date 2025/6/20 15:09
 * @description TODO
 */
@Data
public class ImageDownloadParam {
    private String url;
    private String fileName;
    private String fileType;
    private String title;
    private String subTitle;
}
