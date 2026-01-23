package com.xxx.antifraud.service;

import com.xxx.antifraud.vo.admin.StatisticsVO;

/**
 * 管理员统计服务接口
 */
public interface AdminStatisticsService {

    /**
     * 获取用户总数
     */
    Long getUserCount();

    /**
     * 获取风险等级分布
     */
    StatisticsVO.RiskDistribution getRiskDistribution();

    /**
     * 获取活跃趋势（最近7天）
     */
    StatisticsVO.ActiveTrend getActiveTrend();
}
