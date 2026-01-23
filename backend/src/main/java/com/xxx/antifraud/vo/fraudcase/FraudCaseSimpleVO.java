package com.xxx.antifraud.vo.fraudcase;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 案例列表展示 VO
 */
@Data
public class FraudCaseSimpleVO {

    private Long id;

    @Schema(description = "案例标题")
    private String title;

    @Schema(description = "案例类型")
    private String type;

    @Schema(description = "难度")
    private String level;

    @Schema(description = "提示/摘要")
    private String hint;
}

