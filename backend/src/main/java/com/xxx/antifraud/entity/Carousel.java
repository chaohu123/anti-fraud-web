package com.xxx.antifraud.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识页轮播图实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("af_carousel")
public class Carousel extends BaseEntity {

    /**
     * 图片地址（完整 URL 或 /uploads/... 路径）
     */
    @TableField("image_url")
    private String imageUrl;

    /**
     * 标题（可选）
     */
    private String title;

    /**
     * 点击跳转链接（可选）
     */
    @TableField("link_url")
    private String linkUrl;

    /**
     * 排序，数值越小越靠前
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 是否启用：1 启用 0 停用
     */
    @TableField("enable_flag")
    private Integer enableFlag;
}
