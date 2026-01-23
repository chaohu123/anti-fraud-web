package com.xxx.antifraud.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 交互式识别训练记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("af_training_record")
public class TrainingRecord extends BaseEntity {

    private Long userId;

    private Long caseId;

    /**
     * 是否判断正确：1-正确 0-错误
     */
    private Integer isCorrect;

    /**
     * 用户实际选择的答案：FRAUD / SAFE
     */
    private String answer;

    /**
     * 答题耗时（毫秒）
     */
    private Integer timeSpentMs;

    /**
     * 提交时间
     */
    private LocalDateTime submittedAt;
}

