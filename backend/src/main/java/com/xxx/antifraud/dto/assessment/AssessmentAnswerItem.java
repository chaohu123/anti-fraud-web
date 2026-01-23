package com.xxx.antifraud.dto.assessment;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 单题作答项 DTO
 */
@Data
public class AssessmentAnswerItem {

    @NotNull(message = "题目ID不能为空")
    private Long questionId;

    /**
     * 选中的选项 ID 列表
     */
    @NotEmpty(message = "选项不能为空")
    private List<Long> optionIds;
}

