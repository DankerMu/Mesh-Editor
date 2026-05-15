package com.tool;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * @Description: 分页
 * @FileName: PageBuilder
 * @Date: 2023/5/22 14:43
 * @Version: 1.0
 */
public class PageBuilder {
    public static IPage build(Integer page, Integer size) {
        return new Page(page, size);
    }
}
