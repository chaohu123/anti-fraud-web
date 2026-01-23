package com.xxx.antifraud.controller;

import com.xxx.antifraud.common.Result;
import com.xxx.antifraud.service.KnowledgeService;
import com.xxx.antifraud.vo.knowledge.KnowledgeArticleVO;
import com.xxx.antifraud.vo.knowledge.KnowledgeDetailVO;
import com.xxx.antifraud.vo.knowledge.LearningProgressVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 防骗知识库与学习进度模块 Controller
 */
@Tag(name = "防骗知识库模块")
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
@Validated
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @Operation(summary = "查询防骗知识列表（按类别 / 关键词）")
    @GetMapping
    public Result<List<KnowledgeArticleVO>> list(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "q", required = false) String keyword) {
        return Result.success(knowledgeService.listArticles(category, keyword));
    }

    @Operation(summary = "查询防骗知识详情")
    @GetMapping("/{id}")
    public Result<KnowledgeDetailVO> detail(@PathVariable Long id) {
        return Result.success(knowledgeService.getDetail(id));
    }

    @Operation(summary = "记录用户学习行为并返回整体学习进度与成就等级")
    @PostMapping("/{id}/learn")
    public Result<LearningProgressVO> learn(
            @PathVariable("id") Long articleId,
            @RequestParam("userId") @NotNull Long userId,
            @RequestParam(value = "progress", required = false, defaultValue = "100") Integer progress) {
        return Result.success(knowledgeService.learn(articleId, userId, progress));
    }
}

