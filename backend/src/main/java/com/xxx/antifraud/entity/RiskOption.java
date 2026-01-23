package com.xxx.antifraud.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 防骗风险测评选项实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("af_risk_option")
public class RiskOption extends BaseEntity {

    private Long questionId;

    /**
     * 选项文案
     */
    private String label;

    /**
     * 风险分值（数值越高表示越高风险）
     */
    @TableField("option_value")
    private Integer value;
}

