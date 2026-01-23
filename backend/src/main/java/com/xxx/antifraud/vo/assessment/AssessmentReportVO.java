package com.xxx.antifraud.vo.assessment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 风险评估报告 VO
 *
 * 前端可以直接用该结构渲染饼图/雷达图/文字建议。
 */
@Data
public class AssessmentReportVO {

    @Schema(description = "综合风险指数（0-100，越高越危险）")
    private Double score;

    @Schema(description = "总体风险等级：LOW/MEDIUM/HIGH")
    private String level;

    @Schema(description = "总体风险说明文本")
    private String explanation;

    @Schema(description = "各维度评分列表")
    private List<RiskDimensionScoreVO> dimensions;

    @Schema(description = "个性化防骗建议列表")
    private List<String> suggestions;

    @Schema(description = "评估生成时间")
    private LocalDateTime createdAt;
}

