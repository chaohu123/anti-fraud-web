package com.xxx.antifraud.dto.fraudcase;

import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理端：新增案例请求 DTO（用于前端 Admin.vue 演示）
 *
 * 说明：该接口属于“加分项”的后台录入示例，真实项目可再补充权限校验与字段更丰富的录入能力。
 */
@Data
public class AdminCaseCreateRequest {

    /**
     * 案例类型：sms/email/audio/site
     */
    @NotBlank(message = "类型不能为空")
    private String type;

    @NotBlank(message = "内容不能为空")
    private String content;

    @NotBlank(message = "提示不能为空")
    private String hint;

    /**
     * 标准答案：fraud/safe
     */
    @NotBlank(message = "答案不能为空")
    private String answer;
}

