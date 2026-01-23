package com.xxx.antifraud.vo.admin;

import lombok.Data;
import java.util.List;

/**
 * 分页结果 VO（统一前端格式）
 * 
 * 前端期望格式：{ content: [], total: number }
 * MyBatis Plus Page 格式：{ records: [], total: number }
 */
@Data
public class PageResultVO<T> {
    private List<T> content;
    private Long total;
    private Integer current;
    private Integer size;

    public static <T> PageResultVO<T> from(com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> page) {
        PageResultVO<T> result = new PageResultVO<>();
        result.setContent(page.getRecords());
        result.setTotal(page.getTotal());
        result.setCurrent((int) page.getCurrent());
        result.setSize((int) page.getSize());
        return result;
    }
}
