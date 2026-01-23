package com.xxx.antifraud.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xxx.antifraud.dto.train.TrainingSubmitRequest;
import com.xxx.antifraud.entity.TrainingRecord;
import com.xxx.antifraud.vo.train.TrainingStatsVO;

/**
 * 训练记录 Service
 */
public interface TrainingRecordService extends IService<TrainingRecord> {

    void submit(TrainingSubmitRequest request);

    TrainingStatsVO getStats(Long userId);
}

