package com.xxx.antifraud.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xxx.antifraud.dto.fraudcase.FraudCaseQueryRequest;
import com.xxx.antifraud.entity.FraudCase;
import com.xxx.antifraud.vo.fraudcase.FraudCaseDetailVO;
import com.xxx.antifraud.vo.fraudcase.FraudCaseSimpleVO;
import com.xxx.antifraud.vo.fraudcase.TrainCaseVO;

/**
 * 诈骗案例 Service
 */
public interface FraudCaseService extends IService<FraudCase> {

    /**
     * 训练页案例列表（与前端现有协议对齐）
     */
    java.util.List<TrainCaseVO> listTrainCases(String type, String level);

    Page<FraudCaseSimpleVO> pageQuery(FraudCaseQueryRequest request);

    FraudCaseDetailVO getDetail(Long id);

    FraudCaseDetailVO getRandomCase(String type);
}

