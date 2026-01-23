package com.xxx.antifraud.vo.assessment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 单个维度评分 VO
 */
@Data
public class RiskDimensionScoreVO {

    @Schema(description = "维度编码：info/finance/psych")
    private String dimension;

    @Schema(description = "维度中文名称")
    private String name;

    @Schema(description = "该维度风险指数（0-100，越高越危险）")
    private Double score;

    @Schema(description = "维度风险等级：LOW/MEDIUM/HIGH")
    private String level;
}

