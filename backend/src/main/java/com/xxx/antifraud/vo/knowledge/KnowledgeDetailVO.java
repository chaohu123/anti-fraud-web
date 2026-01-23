package com.xxx.antifraud.vo.knowledge;

import io.swagger.v3.oas.annotations.media.Schema;
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
}

