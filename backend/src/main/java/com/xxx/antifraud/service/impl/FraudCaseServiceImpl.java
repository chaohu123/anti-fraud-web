package com.xxx.antifraud.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.antifraud.common.BusinessException;
import com.xxx.antifraud.common.ErrorCode;
import com.xxx.antifraud.dto.fraudcase.FraudCaseQueryRequest;
import com.xxx.antifraud.entity.FraudCase;
import com.xxx.antifraud.mapper.FraudCaseMapper;
import com.xxx.antifraud.service.FraudCaseService;
import com.xxx.antifraud.vo.fraudcase.FraudCaseDetailVO;
import com.xxx.antifraud.vo.fraudcase.FraudCaseSimpleVO;
import com.xxx.antifraud.vo.fraudcase.TrainCaseVO;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 诈骗案例 Service 实现
 */
@Service
public class FraudCaseServiceImpl extends ServiceImpl<FraudCaseMapper, FraudCase> implements FraudCaseService {

    private final Random random = new Random();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<TrainCaseVO> listTrainCases(String type, String level) {
        LambdaQueryWrapper<FraudCase> wrapper = new LambdaQueryWrapper<>();
        if (type != null && !type.isEmpty()) {
            wrapper.eq(FraudCase::getType, type.toLowerCase(Locale.ROOT));
        }
        if (level != null && !level.isEmpty()) {
            wrapper.eq(FraudCase::getLevel, level.toLowerCase(Locale.ROOT));
        }
        wrapper.orderByAsc(FraudCase::getId);
        return this.list(wrapper).stream().map(this::toTrainCaseVO).toList();
    }

    @Override
    public Page<FraudCaseSimpleVO> pageQuery(FraudCaseQueryRequest request) {
        int pageNo = request.getPageNo() == null || request.getPageNo() < 1 ? 1 : request.getPageNo();
        int pageSize = request.getPageSize() == null || request.getPageSize() < 1 ? 10 : request.getPageSize();

        LambdaQueryWrapper<FraudCase> wrapper = new LambdaQueryWrapper<>();
        if (request.getType() != null && !request.getType().isEmpty()) {
            wrapper.eq(FraudCase::getType, request.getType());
        }
        if (request.getLevel() != null && !request.getLevel().isEmpty()) {
            wrapper.eq(FraudCase::getLevel, request.getLevel());
        }
        wrapper.orderByDesc(FraudCase::getCreatedAt);

        Page<FraudCase> page = this.page(new Page<>(pageNo, pageSize), wrapper);
        Page<FraudCaseSimpleVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<FraudCaseSimpleVO> records = page.getRecords().stream().map(this::toSimpleVO).collect(Collectors.toList());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public FraudCaseDetailVO getDetail(Long id) {
        FraudCase entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return toDetailVO(entity);
    }

    @Override
    public FraudCaseDetailVO getRandomCase(String type) {
        LambdaQueryWrapper<FraudCase> wrapper = new LambdaQueryWrapper<>();
        if (type != null && !type.isEmpty()) {
            wrapper.eq(FraudCase::getType, type);
        }
        List<FraudCase> list = this.list(wrapper);
        if (list.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        FraudCase picked = list.get(random.nextInt(list.size()));
        return toDetailVO(picked);
    }

    private FraudCaseSimpleVO toSimpleVO(FraudCase entity) {
        FraudCaseSimpleVO vo = new FraudCaseSimpleVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setType(entity.getType());
        vo.setLevel(entity.getLevel());
        vo.setHint(entity.getHint());
        return vo;
    }

    private FraudCaseDetailVO toDetailVO(FraudCase entity) {
        FraudCaseDetailVO vo = new FraudCaseDetailVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setType(entity.getType());
        vo.setLevel(entity.getLevel());
        vo.setContent(entity.getContent());
        vo.setMediaUrl(entity.getMediaUrl());
        vo.setHint(entity.getHint());
        vo.setCorrectAnswer(entity.getCorrectAnswer());
        vo.setAnalysis(entity.getAnalysis());
        vo.setSuspiciousPoints(parseSuspiciousPoints(entity.getSuspiciousPoints()));
        return vo;
    }

    private TrainCaseVO toTrainCaseVO(FraudCase entity) {
        TrainCaseVO vo = new TrainCaseVO();
        vo.setId(entity.getId());
        vo.setType(entity.getType());
        vo.setContent(entity.getContent());
        vo.setHint(entity.getHint());
        // 兼容前端：answer 字段使用 fraud/safe 小写
        vo.setAnswer(entity.getCorrectAnswer());
        vo.setLevel(entity.getLevel());
        vo.setSuspiciousPoints(parseSuspiciousPoints(entity.getSuspiciousPoints()));
        vo.setMediaUrl(entity.getMediaUrl());
        return vo;
    }

    private List<String> parseSuspiciousPoints(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            // 容错处理：如果解析失败，则返回单条字符串
            return Collections.singletonList(json);
        }
    }
}

