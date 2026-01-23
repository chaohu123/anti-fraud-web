package com.xxx.antifraud.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xxx.antifraud.dto.assessment.AssessmentSubmitRequest;
import com.xxx.antifraud.entity.RiskAssessment;
import com.xxx.antifraud.vo.assessment.AssessmentReportVO;
import com.xxx.antifraud.vo.assessment.RiskQuestionVO;

import java.util.List;

/**
 * 防骗风险测评与评估 Service
 */
public interface AssessmentService extends IService<RiskAssessment> {

    /**
     * 查询测评问卷题目列表
     */
    List<RiskQuestionVO> listQuestions();

    /**
     * 提交问卷并生成评估报告
     */
    AssessmentReportVO submitAndEvaluate(AssessmentSubmitRequest request);
}

