package com.xxx.antifraud.vo.knowledge;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

/**
 * 知识列表 VO
 */
@Data
public class KnowledgeArticleVO {

    private Long id;

    @Schema(description = "类别：短信/电话/网站/社交等")
    private String category;

    private String title;

    private String summary;

    @Schema(description = "风险等级：高/中/低")
    private String riskLevel;

    @Schema(description = "防范要点（用于知识卡片展示，建议 2~3 条）")
    private List<String> preventionTips;
}

