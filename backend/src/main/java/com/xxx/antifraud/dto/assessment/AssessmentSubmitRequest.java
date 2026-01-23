package com.xxx.antifraud.dto.assessment;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 提交防骗风险测评问卷请求 DTO
 */
@Data
public class AssessmentSubmitRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Valid
    @NotEmpty(message = "作答内容不能为空")
    private List<AssessmentAnswerItem> answers;
}

