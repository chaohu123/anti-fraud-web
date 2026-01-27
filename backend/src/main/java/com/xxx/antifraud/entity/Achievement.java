package com.xxx.antifraud.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 成就实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("af_achievement")
public class Achievement extends BaseEntity {

    /**
     * 成就名称
     */
    private String name;

    /**
     * 成就描述
     */
    private String description;

    /**
     * 成就条件类型
     */
    @TableField("condition_type")
    private String conditionType;

    /**
     * 条件值
     */
    @TableField("condition_value")
    private Integer conditionValue;

    /**
     * 奖励经验值
     */
    @TableField("reward_exp")
    private Integer rewardExp;

    /**
     * 图标URL
     */
    private String icon;

    /**
     * 状态：ACTIVE/INACTIVE
     */
    private String status;
}
