package com.xxx.antifraud.controller;

import com.xxx.antifraud.common.Result;
import com.xxx.antifraud.service.AdminStatisticsService;
import com.xxx.antifraud.vo.admin.StatisticsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员统计接口 Controller
 */
@Tag(name = "管理员统计")
@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
public class AdminStatisticsController {

    private final AdminStatisticsService statisticsService;

    @Operation(summary = "获取用户总数")
    @GetMapping("/users")
    public Result<StatisticsVO.UserCount> getUserCount() {
        Long total = statisticsService.getUserCount();
        StatisticsVO.UserCount result = new StatisticsVO.UserCount();
        result.setTotal(total);
        return Result.success(result);
    }

    @Operation(summary = "获取风险等级分布")
    @GetMapping("/risk-distribution")
    public Result<StatisticsVO.RiskDistribution> getRiskDistribution() {
        StatisticsVO.RiskDistribution result = statisticsService.getRiskDistribution();
        return Result.success(result);
    }

    @Operation(summary = "获取活跃趋势（最近7天）")
    @GetMapping("/active-trend")
    public Result<StatisticsVO.ActiveTrend> getActiveTrend() {
        StatisticsVO.ActiveTrend result = statisticsService.getActiveTrend();
        return Result.success(result);
    }
}
