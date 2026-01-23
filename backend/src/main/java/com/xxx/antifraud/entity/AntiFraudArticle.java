package com.xxx.antifraud.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 防骗知识库文章实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("af_knowledge")
public class AntiFraudArticle extends BaseEntity {

    /**
     * 知识类别：SMS/WEB/FINANCE/SOCIAL/OTHER
     */
    private String category;

    private String title;

    private String summary;

    private String content;
}

