package com.xxx.antifraud.entity;

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

    private Long articleId;

    /**
     * 学习进度百分比（0-100）
     */
    private Integer progress;

    /**
     * 学习状态：READING/FINISHED
     */
    private String status;

    private LocalDateTime learnedAt;
}

