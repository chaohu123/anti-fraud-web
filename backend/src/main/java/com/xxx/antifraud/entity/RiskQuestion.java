package com.xxx.antifraud.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 防骗风险测评题目实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("af_risk_question")
public class RiskQuestion extends BaseEntity {

    /**
     * 题目内容
     */
    private String content;

    /**
     * 测评维度：INFO/FINANCE/PSYCH
     */
    private String dimension;

    /**
     * 题目权重
     */
    private Double weight;

    /**
     * 题型：SINGLE/MULTI
     * 数据库字段名：question_type
     */
    @TableField("question_type")
    private String questionType;
}

