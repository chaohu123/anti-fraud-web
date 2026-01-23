package com.xxx.antifraud.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxx.antifraud.dto.train.TrainingSubmitRequest;
import com.xxx.antifraud.entity.FraudCase;
import com.xxx.antifraud.entity.TrainingRecord;
import com.xxx.antifraud.mapper.TrainingRecordMapper;
import com.xxx.antifraud.service.FraudCaseService;
import com.xxx.antifraud.service.TrainingRecordService;
import com.xxx.antifraud.vo.train.TrainingStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 训练记录 Service 实现
 */
@Service
@RequiredArgsConstructor
public class TrainingRecordServiceImpl extends ServiceImpl<TrainingRecordMapper, TrainingRecord>
        implements TrainingRecordService {

    private final FraudCaseService fraudCaseService;

    @Override
    public void submit(TrainingSubmitRequest request) {
        // 简单校验案例是否存在，避免垃圾数据
        FraudCase fraudCase = fraudCaseService.getById(request.getCaseId());
        if (fraudCase == null) {
            return;
        }
        TrainingRecord record = new TrainingRecord();
        record.setUserId(request.getUserId());
        record.setCaseId(request.getCaseId());
        record.setAnswer(request.getAnswer());
        record.setIsCorrect(Boolean.TRUE.equals(request.getCorrect()) ? 1 : 0);
        record.setTimeSpentMs(request.getTimeSpentMs());
        record.setSubmittedAt(LocalDateTime.now());
        this.save(record);
    }

    @Override
    public TrainingStatsVO getStats(Long userId) {
        List<TrainingRecord> list = this.list(new LambdaQueryWrapper<TrainingRecord>()
                .eq(TrainingRecord::getUserId, userId));
        long total = list.size();
        long correct = list.stream().filter(r -> r.getIsCorrect() != null && r.getIsCorrect() == 1).count();

        // 统计各类型错误率
        Map<Long, FraudCase> caseMap = fraudCaseService.listByIds(
                        list.stream().map(TrainingRecord::getCaseId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(FraudCase::getId, c -> c));

        Map<String, Integer> typeTotal = new HashMap<>();
        Map<String, Integer> typeWrong = new HashMap<>();
        for (TrainingRecord r : list) {
            FraudCase c = caseMap.get(r.getCaseId());
            if (c == null) {
                continue;
            }
            String type = c.getType();
            typeTotal.merge(type, 1, Integer::sum);
            if (r.getIsCorrect() == null || r.getIsCorrect() == 0) {
                typeWrong.merge(type, 1, Integer::sum);
            }
        }
        Map<String, Double> typeErrorRateMap = new HashMap<>();
        typeTotal.forEach((type, t) -> {
            int w = typeWrong.getOrDefault(type, 0);
            typeErrorRateMap.put(type, t == 0 ? 0.0 : (w * 1.0 / t));
        });

        TrainingStatsVO vo = new TrainingStatsVO();
        vo.setTotalTimes(total);
        vo.setCorrectTimes(correct);
        vo.setAccuracy(total == 0 ? 0.0 : (correct * 1.0 / total));
        vo.setTypeErrorRateMap(typeErrorRateMap);
        return vo;
    }
}

