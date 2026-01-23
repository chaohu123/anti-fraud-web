package com.xxx.antifraud.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xxx.antifraud.entity.AssessmentResult;
import com.xxx.antifraud.entity.User;
import com.xxx.antifraud.mapper.AssessmentResultMapper;
import com.xxx.antifraud.mapper.UserMapper;
import com.xxx.antifraud.service.AdminStatisticsService;
import com.xxx.antifraud.vo.admin.StatisticsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员统计服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStatisticsServiceImpl implements AdminStatisticsService {

    private final UserMapper userMapper;
    private final AssessmentResultMapper assessmentResultMapper;

    @Override
    public Long getUserCount() {
        long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getDeleted, 0)
        );
        return count;
    }

    @Override
    public StatisticsVO.RiskDistribution getRiskDistribution() {
        // 查询所有未删除的用户，统计风险等级分布
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getDeleted, 0)
                        .isNotNull(User::getRiskLevel)
        );

        // 统计各风险等级数量
        Map<Integer, Long> riskLevelCount = users.stream()
                .collect(Collectors.groupingBy(
                        User::getRiskLevel,
                        Collectors.counting()
                ));

        // 构建返回数据
        List<StatisticsVO.RiskDistribution.RiskItem> data = new ArrayList<>();
        
        // 低风险 (0)
        StatisticsVO.RiskDistribution.RiskItem lowRisk = new StatisticsVO.RiskDistribution.RiskItem();
        lowRisk.setName("低风险");
        lowRisk.setValue(riskLevelCount.getOrDefault(0, 0L).intValue());
        data.add(lowRisk);
        
        // 中风险 (1)
        StatisticsVO.RiskDistribution.RiskItem mediumRisk = new StatisticsVO.RiskDistribution.RiskItem();
        mediumRisk.setName("中风险");
        mediumRisk.setValue(riskLevelCount.getOrDefault(1, 0L).intValue());
        data.add(mediumRisk);
        
        // 高风险 (2)
        StatisticsVO.RiskDistribution.RiskItem highRisk = new StatisticsVO.RiskDistribution.RiskItem();
        highRisk.setName("高风险");
        highRisk.setValue(riskLevelCount.getOrDefault(2, 0L).intValue());
        data.add(highRisk);

        StatisticsVO.RiskDistribution result = new StatisticsVO.RiskDistribution();
        result.setData(data);
        return result;
    }

    @Override
    public StatisticsVO.ActiveTrend getActiveTrend() {
        try {
            // 获取最近7天的数据
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(6);
            
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

            // 查询最近7天的测评记录（使用 af_assessment_result 表）
            List<AssessmentResult> assessments = assessmentResultMapper.selectList(
                    new LambdaQueryWrapper<AssessmentResult>()
                            .eq(AssessmentResult::getDeleted, 0)
                            .between(AssessmentResult::getCreatedAt, startDateTime, endDateTime)
            );

            // 按日期分组统计
            Map<String, Long> dateCountMap = assessments.stream()
                    .collect(Collectors.groupingBy(
                            assessment -> assessment.getCreatedAt().toLocalDate()
                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                            Collectors.counting()
                    ));

            // 构建返回数据（确保包含所有7天，即使某天没有数据）
            List<StatisticsVO.ActiveTrend.TrendItem> data = new ArrayList<>();
            for (int i = 6; i >= 0; i--) {
                LocalDate date = endDate.minusDays(i);
                String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                
                StatisticsVO.ActiveTrend.TrendItem item = new StatisticsVO.ActiveTrend.TrendItem();
                item.setDate(dateStr);
                item.setCount(dateCountMap.getOrDefault(dateStr, 0L).intValue());
                data.add(item);
            }

            StatisticsVO.ActiveTrend result = new StatisticsVO.ActiveTrend();
            result.setData(data);
            return result;
        } catch (Exception e) {
            // 如果表不存在或其他错误，返回空数据
            log.warn("获取活跃趋势失败: {}", e.getMessage());
            
            // 返回最近7天的空数据
            LocalDate endDate = LocalDate.now();
            List<StatisticsVO.ActiveTrend.TrendItem> data = new ArrayList<>();
            for (int i = 6; i >= 0; i--) {
                LocalDate date = endDate.minusDays(i);
                String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                
                StatisticsVO.ActiveTrend.TrendItem item = new StatisticsVO.ActiveTrend.TrendItem();
                item.setDate(dateStr);
                item.setCount(0);
                data.add(item);
            }
            
            StatisticsVO.ActiveTrend result = new StatisticsVO.ActiveTrend();
            result.setData(data);
            return result;
        }
    }
}
