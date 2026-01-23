package com.xxx.antifraud.vo.knowledge;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户学习进度与成就等级 VO
 */
@Data
public class LearningProgressVO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "知识总数")
    private Long totalArticles;

    @Schema(description = "已完成学习的知识条数")
    private Long finishedArticles;

    @Schema(description = "整体完成率（0-1）")
    private Double completionRate;

    @Schema(description = "成就等级描述")
    private String level;
}

