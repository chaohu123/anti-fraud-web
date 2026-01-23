package com.xxx.antifraud.vo.train;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 用户训练统计数据 VO
 */
@Data
public class TrainingStatsVO {

    @Schema(description = "总训练次数")
    private Long totalTimes;

    @Schema(description = "正确次数")
    private Long correctTimes;

    @Schema(description = "正确率（0-1）")
    private Double accuracy;

    /**
     * 各诈骗类型错误率：key 为案例类型（如 SMS/EMAIL/...），value 为错误率（0-1）
     */
    @Schema(description = "各诈骗类型错误率")
    private Map<String, Double> typeErrorRateMap;
}

