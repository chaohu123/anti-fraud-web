package com.xxx.antifraud.controller;

import com.xxx.antifraud.common.Result;
import com.xxx.antifraud.dto.assessment.AssessmentSubmitRequest;
import com.xxx.antifraud.service.AssessmentService;
import com.xxx.antifraud.vo.assessment.AssessmentReportVO;
import com.xxx.antifraud.vo.assessment.RiskQuestionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 防骗风险测评模块 Controller
 */
@Tag(name = "防骗风险测评模块")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;

    @Operation(summary = "查询测评问卷题目列表")
    @GetMapping("/questions")
    public Result<List<RiskQuestionVO>> listQuestions() {
        return Result.success(assessmentService.listQuestions());
    }

    @Operation(summary = "提交问卷作答并计算风险评估报告")
    @PostMapping("/assessment")
    public Result<AssessmentReportVO> submit(@Valid @RequestBody AssessmentSubmitRequest request) {
        return Result.success(assessmentService.submitAndEvaluate(request));
    }
}

