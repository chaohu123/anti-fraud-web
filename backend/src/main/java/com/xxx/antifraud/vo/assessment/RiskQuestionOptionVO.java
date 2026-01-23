package com.xxx.antifraud.vo.assessment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 问卷选项 VO
 */
@Data
public class RiskQuestionOptionVO {

    @Schema(description = "选项ID（用于提交时回传）")
    private Long id;

    private String label;

    @Schema(description = "风险分值（用于解释性展示，提交时前端可不必回传该值）")
    private Integer value;
}

