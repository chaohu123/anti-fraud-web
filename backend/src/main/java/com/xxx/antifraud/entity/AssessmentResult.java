package com.xxx.antifraud.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 测评结果实体（对应 af_assessment_result 表）
 * 
 * 注意：此表结构与 schema_mysql.sql 中的 af_assessment_result 表对应
 */
@Data
@TableName("af_assessment_result")
public class AssessmentResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("total_score")
    private Integer totalScore;

    @TableField("risk_level")
    private Integer riskLevel; // 0=低风险, 1=中风险, 2=高风险

    @TableField("info_score")
    private Integer infoScore;

    @TableField("finance_score")
    private Integer financeScore;

    @TableField("psych_score")
    private Integer psychScore;

    @TableField("raw_detail_json")
    private String rawDetailJson;

    @TableField("suggestions_json")
    private String suggestionsJson;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
