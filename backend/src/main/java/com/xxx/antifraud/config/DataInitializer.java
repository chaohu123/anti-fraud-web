package com.xxx.antifraud.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.antifraud.entity.AntiFraudArticle;
import com.xxx.antifraud.entity.FraudCase;
import com.xxx.antifraud.entity.RiskOption;
import com.xxx.antifraud.entity.RiskQuestion;
import com.xxx.antifraud.mapper.RiskOptionMapper;
import com.xxx.antifraud.mapper.RiskQuestionMapper;
import com.xxx.antifraud.service.FraudCaseService;
import com.xxx.antifraud.service.KnowledgeService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * 启动初始化数据（从 resources/data/*.json 导入到数据库）
 *
 * 目的：
 * - H2 内存数据库开箱即用，适合答辩演示；
 * - 也可在 MySQL 环境首次启动时快速填充示例数据。
 *
 * 规则：仅当对应表为空时才导入，避免覆盖用户已有数据。
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final FraudCaseService fraudCaseService;
    private final KnowledgeService knowledgeService;
    private final RiskQuestionMapper riskQuestionMapper;
    private final RiskOptionMapper riskOptionMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void run(ApplicationArguments args) throws Exception {
        initCases();
        initKnowledge();
        initQuestions();
    }

    private void initCases() throws Exception {
        if (fraudCaseService.count() > 0) {
            return;
        }
        String json = readClasspath("data/cases.json");
        List<CaseJson> list = objectMapper.readValue(json, new TypeReference<List<CaseJson>>() {});
        for (CaseJson c : list) {
            FraudCase entity = new FraudCase();
            entity.setId((long) c.getId());
            entity.setTitle("案例#" + c.getId());
            entity.setType(c.getType().toLowerCase(Locale.ROOT));
            entity.setLevel(c.getLevel().toLowerCase(Locale.ROOT));
            entity.setContent(c.getContent());
            entity.setHint(c.getHint());
            entity.setMediaUrl(c.getMediaUrl());
            entity.setCorrectAnswer(c.getAnswer().toLowerCase(Locale.ROOT));
            entity.setSuspiciousTags(objectMapper.writeValueAsString(c.getSuspiciousPoints()));
            entity.setEnableFlag(1);
            fraudCaseService.save(entity);
        }
    }

    private void initKnowledge() throws Exception {
        if (knowledgeService.count() > 0) {
            return;
        }
        String json = readClasspath("data/knowledge.json");
        List<KnowledgeJson> list = objectMapper.readValue(json, new TypeReference<List<KnowledgeJson>>() {});
        for (KnowledgeJson k : list) {
            AntiFraudArticle a = new AntiFraudArticle();
            a.setId((long) k.getId());
            a.setCategory(k.getCategory()); // 保持中文分类，前端可直接展示/筛选
            a.setTitle(k.getTitle());
            a.setSummary(k.getSummary());
            a.setContent(buildKnowledgeContent(k.getTitle(), k.getSummary()));
            knowledgeService.save(a);
        }
    }

    private void initQuestions() throws Exception {
        if (riskQuestionMapper.selectCount(null) > 0) {
            return;
        }
        String json = readClasspath("data/questions.json");
        List<QuestionJson> list = objectMapper.readValue(json, new TypeReference<List<QuestionJson>>() {});
        for (QuestionJson q : list) {
            RiskQuestion rq = new RiskQuestion();
            rq.setId((long) q.getId());
            rq.setContent(q.getText());
            rq.setDimension(q.getDimension().toUpperCase(Locale.ROOT));
            rq.setWeight(q.getWeight());
            rq.setQuestionType(q.getType().toUpperCase(Locale.ROOT));
            riskQuestionMapper.insert(rq);

            for (OptionJson opt : q.getOptions()) {
                RiskOption ro = new RiskOption();
                ro.setQuestionId(rq.getId());
                ro.setLabel(opt.getLabel());
                ro.setValue(opt.getValue());
                riskOptionMapper.insert(ro);
            }
        }
    }

    private String readClasspath(String path) throws Exception {
        ClassPathResource resource = new ClassPathResource(path);
        byte[] bytes = resource.getInputStream().readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String buildKnowledgeContent(String title, String summary) {
        return """
                【知识点】%s

                【摘要】
                %s

                【识别要点】
                1）核验来源：优先使用官方 App/官网/客服电话，不点击陌生短链。
                2）保持冷静：遇到“限时”“恐吓”“高收益”等话术，先暂停再决策。
                3）信息最小化：不透露验证码/密码/身份证号/银行卡号等敏感信息。

                【建议练习】
                - 结合训练模块做 3 道相关案例题，加深记忆。
                """.formatted(title, summary);
    }

    @Data
    private static class CaseJson {
        private int id;
        private String type;
        private String content;
        private String hint;
        private List<String> suspiciousPoints;
        private String level;
        private String mediaUrl;
        private String answer;
    }

    @Data
    private static class KnowledgeJson {
        private int id;
        private String category;
        private String title;
        private String summary;
    }

    @Data
    private static class QuestionJson {
        private int id;
        private String text;
        private String dimension;
        private double weight;
        private String type;
        private List<OptionJson> options;
    }

    @Data
    private static class OptionJson {
        private String label;
        private int value;
    }
}

