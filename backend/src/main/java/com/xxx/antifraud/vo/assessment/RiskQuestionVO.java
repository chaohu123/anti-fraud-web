package com.xxx.antifraud.vo.assessment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 问卷题目 VO
 *
 * 结构尽量贴近原 JSON，方便前端复用。
 */
@Data
public class RiskQuestionVO {

    private Long id;

    @Schema(description = "题干文本")
    private String text;

    @Schema(description = "维度编码：info/finance/psych")
    private String dimension;

    @Schema(description = "题目权重")
    private Double weight;

    @Schema(description = "题型：single/multi")
    private String type;

    @Schema(description = "选项列表")
    private List<RiskQuestionOptionVO> options;
}

