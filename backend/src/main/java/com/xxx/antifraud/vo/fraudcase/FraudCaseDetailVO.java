package com.xxx.antifraud.vo.fraudcase;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 案例详情 VO
 */
@Data
public class FraudCaseDetailVO {

    private Long id;

    private String title;

    private String type;

    private String level;

    @Schema(description = "正文内容")
    private String content;

    @Schema(description = "媒体资源 URL（可选）")
    private String mediaUrl;

    @Schema(description = "简要提示")
    private String hint;

    @Schema(description = "可疑特征列表")
    private List<String> suspiciousPoints;

    @Schema(description = "正确答案：FRAUD / SAFE")
    private String correctAnswer;

    @Schema(description = "诈骗解析说明")
    private String analysis;
}

