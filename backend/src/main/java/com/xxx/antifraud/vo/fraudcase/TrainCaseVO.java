package com.xxx.antifraud.vo.fraudcase;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 训练页案例 VO（与前端 Challenge.vue 的结构对齐）
 *
 * 字段命名与枚举值保持小写：type(sms/email/audio/site)、level(easy/medium/hard)、answer(fraud/safe)。
 */
@Data
public class TrainCaseVO {

    private Long id;

    @Schema(description = "案例类型：sms/email/audio/site")
    private String type;

    @Schema(description = "案例正文内容")
    private String content;

    @Schema(description = "套路提示")
    private String hint;

    @Schema(description = "标准答案：fraud/safe")
    private String answer;

    @Schema(description = "难度：easy/medium/hard")
    private String level;

    @Schema(description = "可疑特征列表")
    private List<String> suspiciousPoints;

    @Schema(description = "媒体资源 URL（可选）")
    private String mediaUrl;
}

