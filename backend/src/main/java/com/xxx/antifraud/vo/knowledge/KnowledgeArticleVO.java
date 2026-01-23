package com.xxx.antifraud.vo.knowledge;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 知识列表 VO
 */
@Data
public class KnowledgeArticleVO {

    private Long id;

    @Schema(description = "类别：SMS/WEB/FINANCE/SOCIAL/OTHER")
    private String category;

    private String title;

    private String summary;
}

