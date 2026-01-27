package com.xxx.antifraud.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 防骗知识学习记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("af_learning_record")
public class LearningRecord extends BaseEntity {

    private Long userId;

    /**
     * 对应表字段：knowledge_id
     * 这里沿用业务语义中的 articleId，避免改动过多业务代码
     */
    @TableField("knowledge_id")
    private Long articleId;

    /**
     * 学习进度百分比（0-100）
     */
    private Integer progress;

    /**
     * 学习状态：0未开始/1学习中/2已完成
     */
    private Integer status;

    /**
     * 对应表字段：last_viewed_at（最近查看/学习时间）
     */
    @TableField("last_viewed_at")
    private LocalDateTime learnedAt;
}

