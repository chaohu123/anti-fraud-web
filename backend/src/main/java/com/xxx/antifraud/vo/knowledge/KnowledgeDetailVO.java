package com.xxx.antifraud.vo.knowledge;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

/**
 * 知识详情 VO
 */
@Data
public class KnowledgeDetailVO {

    private Long id;

    private String category;

    private String title;

    private String summary;

    @Schema(description = "正文内容")
    private String content;

    @Schema(description = "风险等级：高/中/低")
    private String riskLevel;

    @Schema(description = "防范要点列表")
    private List<String> preventionTips;

    @Schema(description = "常见诈骗话术列表")
    private List<String> commonTactics;

    @Schema(description = "真实案例说明列表")
    private List<String> cases;

    @Schema(description = "推荐训练模块标题")
    private String relatedTraining;
}

