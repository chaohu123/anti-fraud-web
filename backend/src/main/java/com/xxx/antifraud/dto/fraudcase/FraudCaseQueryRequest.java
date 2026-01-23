package com.xxx.antifraud.dto.fraudcase;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 案例查询请求 DTO
 */
@Data
public class FraudCaseQueryRequest {

    @Schema(description = "案例类型：SMS/EMAIL/WEB/CALL/OTHER，可选")
    private String type;

    @Schema(description = "难度：EASY/MEDIUM/HARD，可选")
    private String level;

    @Schema(description = "页码，从 1 开始")
    private Integer pageNo = 1;

    @Schema(description = "每页数量")
    private Integer pageSize = 10;
}

