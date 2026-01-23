package com.xxx.antifraud.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户基础信息 VO
 */
@Data
public class UserInfoVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "当前风险等级：0=低风险, 1=中风险, 2=高风险")
    private String riskLevel;
}

