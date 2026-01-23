package com.xxx.antifraud.dto.train;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交训练结果请求 DTO
 */
@Data
public class TrainingSubmitRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "案例ID不能为空")
    private Long caseId;

    /**
     * 用户选择的答案：FRAUD / SAFE
     */
    @NotNull(message = "作答结果不能为空")
    private String answer;

    /**
     * 是否判断正确（由前端或后端均可判定，这里以请求字段为准，后端可做校验）
     */
    @NotNull(message = "是否正确不能为空")
    private Boolean correct;

    @Min(value = 0, message = "耗时不能为负数")
    private Integer timeSpentMs;
}

