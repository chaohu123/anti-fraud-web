package com.xxx.antifraud.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 防骗风险测评结果实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("af_risk_assessment")
public class RiskAssessment extends BaseEntity {

    private Long userId;

    /**
     * 综合风险指数（0-100，越高越危险）
     */
    private BigDecimal totalScore;

    private BigDecimal infoScore;

    private BigDecimal financeScore;

    private BigDecimal psychScore;

    /**
     * 风险等级：LOW/MEDIUM/HIGH
     */
    private String riskLevel;

    /**
     * 整体风险说明文本（可解释性说明）
     */
    private String explanation;

    /**
     * 个性化防骗建议（可为多条建议 JSON 或纯文本）
     */
    private String suggestions;
}

