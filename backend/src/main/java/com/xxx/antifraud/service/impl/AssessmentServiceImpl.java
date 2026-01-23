package com.xxx.antifraud.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.antifraud.common.BusinessException;
import com.xxx.antifraud.common.ErrorCode;
import com.xxx.antifraud.common.enums.RiskDimension;
import com.xxx.antifraud.common.enums.RiskLevel;
import com.xxx.antifraud.dto.assessment.AssessmentAnswerItem;
import com.xxx.antifraud.dto.assessment.AssessmentSubmitRequest;
import com.xxx.antifraud.entity.RiskAssessment;
import com.xxx.antifraud.entity.RiskOption;
import com.xxx.antifraud.entity.RiskQuestion;
import com.xxx.antifraud.entity.User;
import com.xxx.antifraud.mapper.RiskAssessmentMapper;
import com.xxx.antifraud.mapper.RiskOptionMapper;
import com.xxx.antifraud.mapper.RiskQuestionMapper;
import com.xxx.antifraud.mapper.UserMapper;
import com.xxx.antifraud.service.AssessmentService;
import com.xxx.antifraud.vo.assessment.AssessmentReportVO;
import com.xxx.antifraud.vo.assessment.RiskDimensionScoreVO;
import com.xxx.antifraud.vo.assessment.RiskQuestionOptionVO;
import com.xxx.antifraud.vo.assessment.RiskQuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 防骗风险测评与评估 Service 实现
 *
 * 评估算法具有“可解释性”：
 * - 明确记录每个维度的加权得分与理论最高分；
 * - 风险等级由分数阈值划分，逻辑透明；
 * - 返回 explanation & suggestions 文字说明，指出高风险维度与改进方向。
 */
@Service
@RequiredArgsConstructor
public class AssessmentServiceImpl extends ServiceImpl<RiskAssessmentMapper, RiskAssessment>
        implements AssessmentService {

    private final RiskQuestionMapper riskQuestionMapper;
    private final RiskOptionMapper riskOptionMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<RiskQuestionVO> listQuestions() {
        List<RiskQuestion> questions = riskQuestionMapper.selectList(
                new LambdaQueryWrapper<RiskQuestion>().orderByAsc(RiskQuestion::getId));
        if (questions.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> qIds = questions.stream().map(RiskQuestion::getId).toList();
        List<RiskOption> options = riskOptionMapper.selectList(
                new LambdaQueryWrapper<RiskOption>().in(RiskOption::getQuestionId, qIds)
                        .orderByAsc(RiskOption::getId));
        Map<Long, List<RiskOption>> optionMap = options.stream()
                .collect(Collectors.groupingBy(RiskOption::getQuestionId));

        return questions.stream().map(q -> {
            RiskQuestionVO vo = new RiskQuestionVO();
            vo.setId(q.getId());
            vo.setText(q.getContent());
            vo.setDimension(toFrontDimension(q.getDimension()));
            vo.setWeight(q.getWeight());
            vo.setType(q.getQuestionType().toLowerCase());
            List<RiskQuestionOptionVO> optionVOList = optionMap
                    .getOrDefault(q.getId(), Collections.emptyList())
                    .stream().map(o -> {
                        RiskQuestionOptionVO ov = new RiskQuestionOptionVO();
                        ov.setId(o.getId());
                        ov.setLabel(o.getLabel());
                        ov.setValue(o.getValue());
                        return ov;
                    }).toList();
            vo.setOptions(optionVOList);
            return vo;
        }).toList();
    }

    @Override
    public AssessmentReportVO submitAndEvaluate(AssessmentSubmitRequest request) {
        if (request.getAnswers() == null || request.getAnswers().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        // 1. 读取题目和选项配置
        List<Long> questionIds = request.getAnswers().stream()
                .map(AssessmentAnswerItem::getQuestionId)
                .distinct().toList();
        List<RiskQuestion> questions = riskQuestionMapper.selectBatchIds(questionIds);
        if (questions.size() != questionIds.size()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "存在无效的题目ID");
        }
        Map<Long, RiskQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(RiskQuestion::getId, q -> q));

        List<RiskOption> options = riskOptionMapper.selectList(
                new LambdaQueryWrapper<RiskOption>().in(RiskOption::getQuestionId, questionIds));
        Map<Long, List<RiskOption>> optionsByQuestion = options.stream()
                .collect(Collectors.groupingBy(RiskOption::getQuestionId));
        Map<Long, RiskOption> optionById = options.stream()
                .collect(Collectors.toMap(RiskOption::getId, o -> o));

        // 2. 计算每个维度的实际得分和理论最高分
        Map<RiskDimension, Double> actualScore = new EnumMap<>(RiskDimension.class);
        Map<RiskDimension, Double> maxScore = new EnumMap<>(RiskDimension.class);
        for (RiskDimension dim : RiskDimension.values()) {
            actualScore.put(dim, 0.0);
            maxScore.put(dim, 0.0);
        }

        // 2.1 先计算各维度理论最高分：每题权重 * 该题可达到的最大选项分值之和
        for (RiskQuestion q : questions) {
            RiskDimension dim = RiskDimension.valueOf(q.getDimension());
            List<RiskOption> optList = optionsByQuestion.getOrDefault(q.getId(), Collections.emptyList());
            if (optList.isEmpty()) {
                continue;
            }
            int bestSum;
            if ("SINGLE".equalsIgnoreCase(q.getQuestionType())) {
                bestSum = optList.stream().mapToInt(RiskOption::getValue).max().orElse(0);
            } else {
                bestSum = optList.stream().mapToInt(RiskOption::getValue).sum();
            }
            double add = bestSum * q.getWeight();
            maxScore.put(dim, maxScore.get(dim) + add);
        }

        // 2.2 根据用户作答计算各维度实际得分
        for (AssessmentAnswerItem answerItem : request.getAnswers()) {
            RiskQuestion q = questionMap.get(answerItem.getQuestionId());
            if (q == null) {
                continue;
            }
            RiskDimension dim = RiskDimension.valueOf(q.getDimension());
            double sumOption = 0.0;
            for (Long optionId : answerItem.getOptionIds()) {
                RiskOption opt = optionById.get(optionId);
                if (opt != null && Objects.equals(opt.getQuestionId(), q.getId())) {
                    sumOption += opt.getValue();
                }
            }
            actualScore.put(dim, actualScore.get(dim) + sumOption * q.getWeight());
        }

        // 3. 将每个维度得分归一化到 0–100
        Map<RiskDimension, Double> normalizedScore = new EnumMap<>(RiskDimension.class);
        for (RiskDimension dim : RiskDimension.values()) {
            double max = maxScore.get(dim);
            double act = actualScore.get(dim);
            double norm = max <= 0 ? 0.0 : (act / max * 100.0);
            normalizedScore.put(dim, round2(norm));
        }

        // 综合风险指数 = 各维度平均
        double total = normalizedScore.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        total = round2(total);

        RiskLevel level = toRiskLevel(total);

        // 4. 生成可解释的维度说明和建议
        List<RiskDimensionScoreVO> dimensionVOList = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        StringBuilder explanationBuilder = new StringBuilder();
        explanationBuilder.append("本次测评从信息保护、金融安全、心理风险三大维度进行综合评估：");

        for (RiskDimension dim : RiskDimension.values()) {
            double s = normalizedScore.get(dim);
            RiskLevel dimLevel = toRiskLevel(s);
            RiskDimensionScoreVO vo = new RiskDimensionScoreVO();
            vo.setDimension(toFrontDimension(dim.name()));
            vo.setName(dim.getDesc());
            vo.setScore(s);
            // 与前端统一：low/medium/high 小写
            vo.setLevel(dimLevel.name().toLowerCase(Locale.ROOT));
            dimensionVOList.add(vo);

            explanationBuilder.append(String.format(Locale.CHINA,
                    "%s维度得分为 %.1f 分（等级：%s）；",
                    dim.getDesc(), s, dimLevel.name()));

            suggestions.addAll(generateSuggestionsForDimension(dim, s));
        }

        // 去重建议
        suggestions = suggestions.stream().distinct().toList();

        String explanation = explanationBuilder.toString();

        // 5. 结果入库（持久化）
        RiskAssessment entity = new RiskAssessment();
        entity.setUserId(request.getUserId());
        entity.setTotalScore(BigDecimal.valueOf(total));
        entity.setInfoScore(BigDecimal.valueOf(normalizedScore.get(RiskDimension.INFO)));
        entity.setFinanceScore(BigDecimal.valueOf(normalizedScore.get(RiskDimension.FINANCE)));
        entity.setPsychScore(BigDecimal.valueOf(normalizedScore.get(RiskDimension.PSYCH)));
        entity.setRiskLevel(level.name());
        entity.setExplanation(explanation);
        try {
            entity.setSuggestions(objectMapper.writeValueAsString(suggestions));
        } catch (JsonProcessingException e) {
            entity.setSuggestions(String.join("；", suggestions));
        }
        this.save(entity);

        // 5.1 同步用户表的最新风险等级（便于个人中心展示）
        // 将 RiskLevel 枚举转换为整数值：LOW=0, MEDIUM=1, HIGH=2
        User user = userMapper.selectById(request.getUserId());
        if (user != null) {
            int riskLevelValue = level.ordinal(); // LOW=0, MEDIUM=1, HIGH=2
            user.setRiskLevel(riskLevelValue);
            userMapper.updateById(user);
        }

        // 6. 组装返回 VO
        AssessmentReportVO report = new AssessmentReportVO();
        report.setScore(total);
        // 与前端统一：low/medium/high 小写
        report.setLevel(level.name().toLowerCase(Locale.ROOT));
        report.setExplanation(explanation);
        report.setDimensions(dimensionVOList);
        report.setSuggestions(suggestions);
        report.setCreatedAt(LocalDateTime.now());
        return report;
    }

    private String toFrontDimension(String dbCode) {
        return dbCode.toLowerCase();
    }

    private RiskLevel toRiskLevel(double score) {
        if (score < 40) {
            return RiskLevel.LOW;
        } else if (score < 70) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.HIGH;
        }
    }

    private double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 针对不同维度和得分区间生成可解释的建议文案
     */
    private List<String> generateSuggestionsForDimension(RiskDimension dim, double score) {
        List<String> list = new ArrayList<>();
        switch (dim) {
            case INFO -> {
                if (score >= 70) {
                    list.add("信息保护意识偏弱，建议不要随意点击陌生链接或扫描来历不明二维码。");
                    list.add("涉及验证码、银行卡号等敏感信息时，应通过官方渠道再次核验。");
                } else if (score >= 40) {
                    list.add("在处理含链接或附件的短信邮件时，建议多看域名与发件人是否官方。");
                } else {
                    list.add("信息防护意识较好，建议继续保持“核实来源再操作”的习惯。");
                }
            }
            case FINANCE -> {
                if (score >= 70) {
                    list.add("金融安全风险较高，切勿轻信“高收益、稳赚不赔”的投资宣传。");
                    list.add("大额转账前建议与家人或官方客服多方核实，避免单独决策。");
                } else if (score >= 40) {
                    list.add("在进行转账、贷款、理财操作时，建议慢一点，多核对一次收款方信息。");
                } else {
                    list.add("金融安全意识较好，可以适当帮助身边亲友识别常见金融诈骗。");
                }
            }
            case PSYCH -> {
                if (score >= 70) {
                    list.add("心理上较容易受到“恐吓、催促或高收益诱惑”影响，建议重大决策前先冷静 10 分钟。");
                    list.add("遇到“限时”“最后一次机会”等话术时，可以刻意暂停一下，避免冲动操作。");
                } else if (score >= 40) {
                    list.add("可以尝试给自己设置“冷静期规则”，比如任何转账前至少多确认一遍信息来源。");
                } else {
                    list.add("心理防线较稳健，建议继续保持理性思考，也可以分享给亲友一些防骗技巧。");
                }
            }
        }
        return list;
    }
}

