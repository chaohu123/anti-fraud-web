package com.xxx.antifraud.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.antifraud.common.Result;
import com.xxx.antifraud.dto.fraudcase.AdminCaseCreateRequest;
import com.xxx.antifraud.dto.fraudcase.FraudCaseQueryRequest;
import com.xxx.antifraud.entity.Achievement;
import com.xxx.antifraud.entity.AntiFraudArticle;
import com.xxx.antifraud.entity.FraudCase;
import com.xxx.antifraud.entity.User;
import com.xxx.antifraud.mapper.AchievementMapper;
import com.xxx.antifraud.mapper.UserMapper;
import com.xxx.antifraud.service.FraudCaseService;
import com.xxx.antifraud.service.KnowledgeService;
import com.xxx.antifraud.service.TrainingRecordService;
import com.xxx.antifraud.vo.admin.PageResultVO;
import com.xxx.antifraud.vo.admin.UserListVO;
import com.xxx.antifraud.vo.fraudcase.FraudCaseSimpleVO;
import com.xxx.antifraud.vo.train.TrainingStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * 后台管理接口
 *
 * 注意：毕业设计一般不要求完整权限体系，这里不做鉴权，仅用于展示"后台录入可扩展"。
 */
@Tag(name = "后台管理")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final FraudCaseService fraudCaseService;
    private final UserMapper userMapper;
    private final TrainingRecordService trainingRecordService;
    private final KnowledgeService knowledgeService;
    private final AchievementMapper achievementMapper;
    private final com.xxx.antifraud.mapper.RiskQuestionMapper riskQuestionMapper;
    private final com.xxx.antifraud.mapper.RiskOptionMapper riskOptionMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 案例管理 ====================
    
    @Operation(summary = "分页查询诈骗案例列表")
    @GetMapping("/cases")
    public Result<PageResultVO<FraudCaseSimpleVO>> listCases(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String answer,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        FraudCaseQueryRequest request = new FraudCaseQueryRequest();
        request.setPageNo(page != null ? page : 1);
        request.setPageSize(size != null ? size : 10);
        if (StringUtils.hasText(type)) {
            request.setType(type);
        }
        if (StringUtils.hasText(difficulty)) {
            request.setLevel(difficulty);
        }
        Page<FraudCaseSimpleVO> pageResult = fraudCaseService.pageQuery(request);
        return Result.success(PageResultVO.from(pageResult));
    }

    @Operation(summary = "新增诈骗案例")
    @PostMapping("/cases")
    public Result<Void> createCase(@RequestBody Map<String, Object> data) throws Exception {
        FraudCase entity = new FraudCase();
        
        // 支持从Map中读取字段，兼容AdminCaseCreateRequest格式
        if (data.containsKey("title")) {
            entity.setTitle(data.get("title").toString());
        } else {
            entity.setTitle("后台录入案例");
        }
        
        String type = data.containsKey("type") ? data.get("type").toString() : 
                (data.containsKey("type") ? data.get("type").toString() : "sms");
        entity.setType(type.toLowerCase(Locale.ROOT));
        
        String level = data.containsKey("difficulty") ? data.get("difficulty").toString() : "easy";
        entity.setLevel(level.toLowerCase(Locale.ROOT));
        
        entity.setContent(data.get("content").toString());
        
        if (data.containsKey("hint")) {
            entity.setHint(data.get("hint").toString());
        }
        
        String answer = data.containsKey("answer") ? data.get("answer").toString() : "fraud";
        entity.setCorrectAnswer(answer.toLowerCase(Locale.ROOT));
        
        // 设置可疑特征
        if (data.containsKey("hint")) {
            entity.setSuspiciousTags(objectMapper.writeValueAsString(List.of(data.get("hint").toString())));
        }
        
        entity.setEnableFlag(1);
        fraudCaseService.save(entity);
        return Result.success();
    }

    @Operation(summary = "更新诈骗案例")
    @PutMapping("/cases/{id}")
    public Result<Void> updateCase(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        FraudCase entity = fraudCaseService.getById(id);
        if (entity == null) {
            return Result.failure(404, "案例不存在");
        }
        if (data.containsKey("title")) {
            entity.setTitle(data.get("title").toString());
        }
        if (data.containsKey("type")) {
            entity.setType(data.get("type").toString().toLowerCase(Locale.ROOT));
        }
        if (data.containsKey("content")) {
            entity.setContent(data.get("content").toString());
        }
        if (data.containsKey("hint")) {
            entity.setHint(data.get("hint").toString());
        }
        if (data.containsKey("answer")) {
            entity.setCorrectAnswer(data.get("answer").toString().toLowerCase(Locale.ROOT));
        }
        if (data.containsKey("difficulty")) {
            entity.setLevel(data.get("difficulty").toString().toLowerCase(Locale.ROOT));
        }
        if (data.containsKey("mediaUrl")) {
            entity.setMediaUrl(data.get("mediaUrl").toString());
        }
        fraudCaseService.updateById(entity);
        return Result.success();
    }

    @Operation(summary = "删除诈骗案例")
    @DeleteMapping("/cases/{id}")
    public Result<Void> deleteCase(@PathVariable Long id) {
        fraudCaseService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "批量删除诈骗案例")
    @DeleteMapping("/cases/batch")
    public Result<Void> deleteCasesBatch(@RequestBody Map<String, List<Long>> data) {
        List<Long> ids = data.get("ids");
        if (ids != null && !ids.isEmpty()) {
            fraudCaseService.removeByIds(ids);
        }
        return Result.success();
    }

    // ==================== 用户管理 ====================
    
    @Operation(summary = "分页查询用户列表")
    @GetMapping("/users")
    public Result<PageResultVO<UserListVO>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getDeleted, 0);
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getEmail, keyword));
        }
        
        if (StringUtils.hasText(riskLevel)) {
            // riskLevel: LOW=0, MEDIUM=1, HIGH=2
            int levelValue = switch (riskLevel.toUpperCase()) {
                case "LOW" -> 0;
                case "MEDIUM" -> 1;
                case "HIGH" -> 2;
                default -> -1;
            };
            if (levelValue >= 0) {
                wrapper.eq(User::getRiskLevel, levelValue);
            }
        }
        
        wrapper.orderByDesc(User::getCreatedAt);
        Page<User> pageResult = new Page<>(page, size);
        Page<User> userPage = userMapper.selectPage(pageResult, wrapper);
        
        // 转换为 UserListVO
        Page<UserListVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        List<UserListVO> voList = userPage.getRecords().stream()
                .map(UserListVO::from)
                .collect(java.util.stream.Collectors.toList());
        voPage.setRecords(voList);
        
        return Result.success(PageResultVO.from(voPage));
    }

    @Operation(summary = "获取用户报告")
    @GetMapping("/users/{id}/report")
    public Result<Map<String, Object>> getUserReport(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.failure(404, "用户不存在");
        }
        
        Map<String, Object> report = new HashMap<>();
        report.put("userId", user.getId());
        report.put("username", user.getUsername());
        report.put("nickname", user.getNickname());
        report.put("email", user.getEmail());
        report.put("riskLevel", user.getRiskLevel() != null ? 
                switch (user.getRiskLevel()) {
                    case 0 -> "LOW";
                    case 1 -> "MEDIUM";
                    case 2 -> "HIGH";
                    default -> "LOW";
                } : "LOW");
        
        // 获取训练统计
        try {
            TrainingStatsVO trainingStats = trainingRecordService.getStats(id);
            report.put("trainingCount", trainingStats.getTotalTimes() != null ? trainingStats.getTotalTimes() : 0L);
            report.put("correctCount", trainingStats.getCorrectTimes() != null ? trainingStats.getCorrectTimes() : 0L);
            report.put("avgTimeSpent", 0); // TrainingStatsVO 中没有此字段，暂时返回 0
        } catch (Exception e) {
            report.put("trainingCount", 0L);
            report.put("correctCount", 0L);
            report.put("avgTimeSpent", 0);
        }
        
        // 测评统计（简化版）
        report.put("assessmentCount", 0);
        report.put("lastAssessmentTime", null);
        report.put("infoScore", 0);
        report.put("financeScore", 0);
        report.put("psychScore", 0);
        report.put("totalScore", 0);
        report.put("achievementCount", 0);
        
        return Result.success(report);
    }

    // ==================== 知识库管理 =====================
    
    @Operation(summary = "知识库管理 - 列表")
    @GetMapping("/knowledge")
    public Result<PageResultVO<Map<String, Object>>> listKnowledge(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        LambdaQueryWrapper<AntiFraudArticle> wrapper = new LambdaQueryWrapper<>();
        // MyBatis Plus的@TableLogic会自动处理逻辑删除，这里不需要手动添加deleted条件
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(AntiFraudArticle::getTitle, keyword)
                    .or().like(AntiFraudArticle::getSummary, keyword)
                    .or().like(AntiFraudArticle::getContent, keyword));
        }
        
        if (StringUtils.hasText(category)) {
            // 前端可能传中文分类，需要映射到数据库中的分类
            String dbCategory = category;
            // 如果前端传的是中文，转换为数据库格式（如"短信" -> "短信"）
            // 这里保持原值，因为数据库中的category字段存储的就是中文
            wrapper.eq(AntiFraudArticle::getCategory, dbCategory);
        }
        
        wrapper.orderByDesc(AntiFraudArticle::getCreatedAt);
        
        Page<AntiFraudArticle> pageResult = new Page<>(page, size);
        Page<AntiFraudArticle> articlePage = knowledgeService.page(pageResult, wrapper);
        
        // 转换为Map格式
        Page<Map<String, Object>> voPage = new Page<>(articlePage.getCurrent(), articlePage.getSize(), articlePage.getTotal());
        List<Map<String, Object>> voList = articlePage.getRecords().stream().map(article -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", article.getId());
            map.put("title", article.getTitle());
            map.put("category", article.getCategory());
            map.put("summary", article.getSummary());
            map.put("content", article.getContent());
            map.put("contentType", "ARTICLE"); // 默认文章类型
            map.put("status", "PUBLISHED"); // 默认已发布
            if (article.getCreatedAt() != null) {
                map.put("createdAt", article.getCreatedAt().toString());
            }
            if (article.getUpdatedAt() != null) {
                map.put("updatedAt", article.getUpdatedAt().toString());
            }
            return map;
        }).collect(Collectors.toList());
        voPage.setRecords(voList);
        
        return Result.success(PageResultVO.from(voPage));
    }

    @Operation(summary = "知识库管理 - 创建")
    @PostMapping("/knowledge")
    public Result<Void> createKnowledge(@RequestBody Map<String, Object> data) {
        AntiFraudArticle article = new AntiFraudArticle();
        if (data.containsKey("title")) {
            article.setTitle((String) data.get("title"));
        }
        if (data.containsKey("category")) {
            article.setCategory((String) data.get("category"));
        }
        if (data.containsKey("summary")) {
            article.setSummary((String) data.get("summary"));
        } else {
            article.setSummary("");
        }
        if (data.containsKey("content")) {
            article.setContent((String) data.get("content"));
        }
        knowledgeService.save(article);
        return Result.success();
    }

    @Operation(summary = "知识库管理 - 更新")
    @PutMapping("/knowledge/{id}")
    public Result<Void> updateKnowledge(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        AntiFraudArticle article = knowledgeService.getById(id);
        if (article == null) {
            return Result.failure(404, "知识不存在");
        }
        if (data.containsKey("title")) {
            article.setTitle((String) data.get("title"));
        }
        if (data.containsKey("category")) {
            article.setCategory((String) data.get("category"));
        }
        if (data.containsKey("summary")) {
            article.setSummary((String) data.get("summary"));
        }
        if (data.containsKey("content")) {
            article.setContent((String) data.get("content"));
        }
        knowledgeService.updateById(article);
        return Result.success();
    }

    @Operation(summary = "知识库管理 - 删除")
    @DeleteMapping("/knowledge/{id}")
    public Result<Void> deleteKnowledge(@PathVariable Long id) {
        knowledgeService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "知识库管理 - 批量删除")
    @DeleteMapping("/knowledge/batch")
    public Result<Void> deleteKnowledgeBatch(@RequestBody Map<String, List<Long>> data) {
        List<Long> ids = data.get("ids");
        if (ids != null && !ids.isEmpty()) {
            knowledgeService.removeByIds(ids);
        }
        return Result.success();
    }

    @Operation(summary = "成就管理 - 列表")
    @GetMapping("/achievements")
    public Result<PageResultVO<Map<String, Object>>> listAchievements(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        LambdaQueryWrapper<Achievement> wrapper = new LambdaQueryWrapper<>();
        // MyBatis Plus的@TableLogic会自动处理逻辑删除
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Achievement::getName, keyword)
                    .or().like(Achievement::getDescription, keyword));
        }
        
        if (StringUtils.hasText(condition)) {
            wrapper.eq(Achievement::getConditionType, condition);
        }
        
        if (StringUtils.hasText(status)) {
            wrapper.eq(Achievement::getStatus, status);
        }
        
        wrapper.orderByDesc(Achievement::getCreatedAt);
        
        Page<Achievement> pageResult = new Page<>(page, size);
        Page<Achievement> achievementPage = achievementMapper.selectPage(pageResult, wrapper);
        
        // 转换为Map格式
        Page<Map<String, Object>> voPage = new Page<>(achievementPage.getCurrent(), achievementPage.getSize(), achievementPage.getTotal());
        List<Map<String, Object>> voList = achievementPage.getRecords().stream().map(achievement -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", achievement.getId());
            map.put("name", achievement.getName());
            map.put("description", achievement.getDescription());
            map.put("condition", achievement.getConditionType());
            map.put("conditionValue", achievement.getConditionValue());
            map.put("rewardExp", achievement.getRewardExp());
            map.put("icon", achievement.getIcon());
            map.put("status", achievement.getStatus());
            if (achievement.getCreatedAt() != null) {
                map.put("createdAt", achievement.getCreatedAt().toString());
            }
            if (achievement.getUpdatedAt() != null) {
                map.put("updatedAt", achievement.getUpdatedAt().toString());
            }
            return map;
        }).collect(Collectors.toList());
        voPage.setRecords(voList);
        
        return Result.success(PageResultVO.from(voPage));
    }

    @Operation(summary = "成就管理 - 创建")
    @PostMapping("/achievements")
    public Result<Void> createAchievement(@RequestBody Map<String, Object> data) {
        Achievement achievement = new Achievement();
        achievement.setName((String) data.get("name"));
        achievement.setDescription((String) data.get("description"));
        achievement.setConditionType((String) data.get("condition"));
        if (data.get("conditionValue") != null) {
            achievement.setConditionValue(Integer.valueOf(data.get("conditionValue").toString()));
        }
        if (data.get("rewardExp") != null) {
            achievement.setRewardExp(Integer.valueOf(data.get("rewardExp").toString()));
        }
        achievement.setIcon((String) data.get("icon"));
        achievement.setStatus((String) data.getOrDefault("status", "ACTIVE"));
        achievementMapper.insert(achievement);
        return Result.success();
    }

    @Operation(summary = "成就管理 - 更新")
    @PutMapping("/achievements/{id}")
    public Result<Void> updateAchievement(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        Achievement achievement = achievementMapper.selectById(id);
        if (achievement == null) {
            return Result.failure(404, "成就不存在");
        }
        if (data.containsKey("name")) {
            achievement.setName((String) data.get("name"));
        }
        if (data.containsKey("description")) {
            achievement.setDescription((String) data.get("description"));
        }
        if (data.containsKey("condition")) {
            achievement.setConditionType((String) data.get("condition"));
        }
        if (data.containsKey("conditionValue")) {
            achievement.setConditionValue(Integer.valueOf(data.get("conditionValue").toString()));
        }
        if (data.containsKey("rewardExp")) {
            achievement.setRewardExp(Integer.valueOf(data.get("rewardExp").toString()));
        }
        if (data.containsKey("icon")) {
            achievement.setIcon((String) data.get("icon"));
        }
        if (data.containsKey("status")) {
            achievement.setStatus((String) data.get("status"));
        }
        achievementMapper.updateById(achievement);
        return Result.success();
    }

    @Operation(summary = "成就管理 - 删除")
    @DeleteMapping("/achievements/{id}")
    public Result<Void> deleteAchievement(@PathVariable Long id) {
        achievementMapper.deleteById(id);
        return Result.success();
    }

    @Operation(summary = "成就管理 - 批量删除")
    @DeleteMapping("/achievements/batch")
    public Result<Void> deleteAchievementsBatch(@RequestBody Map<String, List<Long>> data) {
        List<Long> ids = data.get("ids");
        if (ids != null && !ids.isEmpty()) {
            achievementMapper.deleteBatchIds(ids);
        }
        return Result.success();
    }

    @Operation(summary = "测评问题管理 - 列表")
    @GetMapping("/assessment/questions")
    public Result<PageResultVO<Map<String, Object>>> listAssessmentQuestions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String dimension,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        LambdaQueryWrapper<com.xxx.antifraud.entity.RiskQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(com.xxx.antifraud.entity.RiskQuestion::getDeleted, 0);
        
        if (StringUtils.hasText(keyword)) {
            wrapper.like(com.xxx.antifraud.entity.RiskQuestion::getContent, keyword);
        }
        
        if (StringUtils.hasText(dimension)) {
            wrapper.eq(com.xxx.antifraud.entity.RiskQuestion::getDimension, dimension);
        }
        
        wrapper.orderByDesc(com.xxx.antifraud.entity.RiskQuestion::getCreatedAt);
        
        Page<com.xxx.antifraud.entity.RiskQuestion> pageResult = new Page<>(page, size);
        Page<com.xxx.antifraud.entity.RiskQuestion> questionPage = riskQuestionMapper.selectPage(pageResult, wrapper);
        
        // 获取所有问题的选项
        List<Long> questionIds = questionPage.getRecords().stream()
                .map(com.xxx.antifraud.entity.RiskQuestion::getId)
                .collect(Collectors.toList());
        
        final Map<Long, List<com.xxx.antifraud.entity.RiskOption>> optionMap;
        if (!questionIds.isEmpty()) {
            LambdaQueryWrapper<com.xxx.antifraud.entity.RiskOption> optionWrapper = new LambdaQueryWrapper<>();
            optionWrapper.in(com.xxx.antifraud.entity.RiskOption::getQuestionId, questionIds);
            optionWrapper.eq(com.xxx.antifraud.entity.RiskOption::getDeleted, 0);
            optionWrapper.orderByAsc(com.xxx.antifraud.entity.RiskOption::getId);
            List<com.xxx.antifraud.entity.RiskOption> options = riskOptionMapper.selectList(optionWrapper);
            optionMap = options.stream()
                    .collect(Collectors.groupingBy(com.xxx.antifraud.entity.RiskOption::getQuestionId));
        } else {
            optionMap = new HashMap<>();
        }
        
        // 转换为Map格式
        Page<Map<String, Object>> voPage = new Page<>(questionPage.getCurrent(), questionPage.getSize(), questionPage.getTotal());
        List<Map<String, Object>> voList = questionPage.getRecords().stream().map(question -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", question.getId());
            map.put("question", question.getContent());
            map.put("dimension", question.getDimension());
            map.put("weight", question.getWeight());
            map.put("questionType", question.getQuestionType());
            
            // 转换选项格式
            List<com.xxx.antifraud.entity.RiskOption> options = optionMap.getOrDefault(question.getId(), new ArrayList<>());
            List<Map<String, Object>> optionList = options.stream().map(option -> {
                Map<String, Object> optMap = new HashMap<>();
                optMap.put("text", option.getLabel());
                optMap.put("score", option.getValue());
                return optMap;
            }).collect(Collectors.toList());
            map.put("options", optionList);
            
            if (question.getCreatedAt() != null) {
                map.put("createdAt", question.getCreatedAt().toString());
            }
            return map;
        }).collect(Collectors.toList());
        voPage.setRecords(voList);
        
        return Result.success(PageResultVO.from(voPage));
    }

    @Operation(summary = "测评问题管理 - 创建")
    @PostMapping("/assessment/questions")
    public Result<Void> createAssessmentQuestion(@RequestBody Map<String, Object> data) {
        com.xxx.antifraud.entity.RiskQuestion question = new com.xxx.antifraud.entity.RiskQuestion();
        question.setContent((String) data.get("question"));
        question.setDimension((String) data.get("dimension"));
        if (data.get("weight") != null) {
            question.setWeight(Double.valueOf(data.get("weight").toString()));
        } else {
            question.setWeight(1.0);
        }
        question.setQuestionType("SINGLE"); // 默认单选
        
        riskQuestionMapper.insert(question);
        
        // 保存选项
        if (data.containsKey("options") && data.get("options") instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> options = (List<Map<String, Object>>) data.get("options");
            for (Map<String, Object> optionData : options) {
                com.xxx.antifraud.entity.RiskOption option = new com.xxx.antifraud.entity.RiskOption();
                option.setQuestionId(question.getId());
                option.setLabel((String) optionData.get("text"));
                if (optionData.get("score") != null) {
                    option.setValue(Integer.valueOf(optionData.get("score").toString()));
                } else {
                    option.setValue(1);
                }
                riskOptionMapper.insert(option);
            }
        }
        
        return Result.success();
    }

    @Operation(summary = "测评问题管理 - 更新")
    @PutMapping("/assessment/questions/{id}")
    public Result<Void> updateAssessmentQuestion(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        com.xxx.antifraud.entity.RiskQuestion question = riskQuestionMapper.selectById(id);
        if (question == null) {
            return Result.failure(404, "问题不存在");
        }
        
        if (data.containsKey("question")) {
            question.setContent((String) data.get("question"));
        }
        if (data.containsKey("dimension")) {
            question.setDimension((String) data.get("dimension"));
        }
        if (data.containsKey("weight")) {
            question.setWeight(Double.valueOf(data.get("weight").toString()));
        }
        if (data.containsKey("questionType")) {
            question.setQuestionType((String) data.get("questionType"));
        }
        
        riskQuestionMapper.updateById(question);
        
        // 更新选项：先删除旧选项，再插入新选项
        if (data.containsKey("options") && data.get("options") instanceof List) {
            // 删除旧选项
            LambdaQueryWrapper<com.xxx.antifraud.entity.RiskOption> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(com.xxx.antifraud.entity.RiskOption::getQuestionId, id);
            riskOptionMapper.delete(deleteWrapper);
            
            // 插入新选项
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> options = (List<Map<String, Object>>) data.get("options");
            for (Map<String, Object> optionData : options) {
                com.xxx.antifraud.entity.RiskOption option = new com.xxx.antifraud.entity.RiskOption();
                option.setQuestionId(id);
                option.setLabel((String) optionData.get("text"));
                if (optionData.get("score") != null) {
                    option.setValue(Integer.valueOf(optionData.get("score").toString()));
                } else {
                    option.setValue(1);
                }
                riskOptionMapper.insert(option);
            }
        }
        
        return Result.success();
    }

    @Operation(summary = "测评问题管理 - 删除")
    @DeleteMapping("/assessment/questions/{id}")
    public Result<Void> deleteAssessmentQuestion(@PathVariable Long id) {
        // 删除问题（逻辑删除）
        riskQuestionMapper.deleteById(id);
        // 删除相关选项（逻辑删除）
        LambdaQueryWrapper<com.xxx.antifraud.entity.RiskOption> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(com.xxx.antifraud.entity.RiskOption::getQuestionId, id);
        List<com.xxx.antifraud.entity.RiskOption> options = riskOptionMapper.selectList(wrapper);
        for (com.xxx.antifraud.entity.RiskOption option : options) {
            riskOptionMapper.deleteById(option.getId());
        }
        return Result.success();
    }

    @Operation(summary = "测评问题管理 - 批量删除")
    @DeleteMapping("/assessment/questions/batch")
    public Result<Void> deleteAssessmentQuestionsBatch(@RequestBody Map<String, List<Long>> data) {
        List<Long> ids = data.get("ids");
        if (ids != null && !ids.isEmpty()) {
            // 删除问题
            riskQuestionMapper.deleteBatchIds(ids);
            // 删除相关选项
            LambdaQueryWrapper<com.xxx.antifraud.entity.RiskOption> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(com.xxx.antifraud.entity.RiskOption::getQuestionId, ids);
            List<com.xxx.antifraud.entity.RiskOption> options = riskOptionMapper.selectList(wrapper);
            for (com.xxx.antifraud.entity.RiskOption option : options) {
                riskOptionMapper.deleteById(option.getId());
            }
        }
        return Result.success();
    }

    @Operation(summary = "训练题目管理 - 列表")
    @GetMapping("/training/questions")
    public Result<PageResultVO<Map<String, Object>>> listTrainingQuestions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long caseId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        // 训练题目基于诈骗案例，查询诈骗案例并转换为训练题目格式
        LambdaQueryWrapper<FraudCase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FraudCase::getDeleted, 0);
        wrapper.eq(FraudCase::getEnableFlag, 1);
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(FraudCase::getTitle, keyword)
                    .or().like(FraudCase::getContent, keyword)
                    .or().like(FraudCase::getHint, keyword));
        }
        
        if (caseId != null) {
            wrapper.eq(FraudCase::getId, caseId);
        }
        
        wrapper.orderByDesc(FraudCase::getCreatedAt);
        
        Page<FraudCase> pageResult = new Page<>(page, size);
        Page<FraudCase> casePage = fraudCaseService.page(pageResult, wrapper);
        
        // 转换为训练题目格式
        Page<Map<String, Object>> voPage = new Page<>(casePage.getCurrent(), casePage.getSize(), casePage.getTotal());
        List<Map<String, Object>> voList = casePage.getRecords().stream().map(fraudCase -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", fraudCase.getId());
            map.put("caseId", fraudCase.getId()); // 关联案例ID就是案例本身
            map.put("title", fraudCase.getTitle()); // 标题
            map.put("question", fraudCase.getContent()); // 题目内容就是案例内容
            map.put("type", fraudCase.getType()); // 类型
            map.put("level", fraudCase.getLevel()); // 难度
            map.put("hint", fraudCase.getHint()); // 提示
            map.put("answer", fraudCase.getCorrectAnswer()); // 正确答案
            map.put("correctAnswer", fraudCase.getCorrectAnswer()); // 正确答案（兼容字段）
            map.put("mediaUrl", fraudCase.getMediaUrl()); // 媒体URL
            // 解析可疑特征作为选项
            List<String> suspiciousTags = new ArrayList<>();
            if (fraudCase.getSuspiciousTags() != null && !fraudCase.getSuspiciousTags().isEmpty()) {
                try {
                    suspiciousTags = objectMapper.readValue(fraudCase.getSuspiciousTags(), 
                            new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
                } catch (Exception e) {
                    // 解析失败，使用空列表
                }
            }
            // 构建选项（基于可疑特征）
            List<Map<String, Object>> options = new ArrayList<>();
            for (String tag : suspiciousTags) {
                Map<String, Object> option = new HashMap<>();
                option.put("text", tag);
                option.put("isCorrect", true); // 可疑特征都是正确答案
                options.add(option);
            }
            // 添加一个"正常"选项作为干扰项
            Map<String, Object> normalOption = new HashMap<>();
            normalOption.put("text", "正常信息");
            normalOption.put("isCorrect", fraudCase.getCorrectAnswer() != null && 
                    fraudCase.getCorrectAnswer().equalsIgnoreCase("safe"));
            options.add(normalOption);
            map.put("options", options);
            map.put("fraudFeatures", suspiciousTags); // 诈骗特征列表
            map.put("caseTitle", fraudCase.getTitle()); // 案例标题（兼容字段）
            if (fraudCase.getCreatedAt() != null) {
                map.put("createdAt", fraudCase.getCreatedAt().toString());
            }
            return map;
        }).collect(Collectors.toList());
        voPage.setRecords(voList);
        
        return Result.success(PageResultVO.from(voPage));
    }

    @Operation(summary = "训练题目管理 - 创建")
    @PostMapping("/training/questions")
    public Result<Void> createTrainingQuestion(@RequestBody Map<String, Object> data) {
        // 训练题目基于诈骗案例，如果caseId存在则更新，不存在则创建新案例
        Long caseId = null;
        if (data.containsKey("caseId") && data.get("caseId") != null) {
            try {
                caseId = Long.valueOf(data.get("caseId").toString());
            } catch (Exception e) {
                // caseId无效，将创建新案例
            }
        }
        
        FraudCase fraudCase;
        if (caseId != null) {
            fraudCase = fraudCaseService.getById(caseId);
            if (fraudCase == null) {
                // 案例不存在，创建新案例
                fraudCase = new FraudCase();
            }
        } else {
            // 没有提供caseId，创建新案例
            fraudCase = new FraudCase();
        }
        
        // 设置或更新案例字段
        if (data.containsKey("question")) {
            fraudCase.setContent(data.get("question").toString());
        }
        if (data.containsKey("title")) {
            fraudCase.setTitle(data.get("title").toString());
        } else if (fraudCase.getTitle() == null || fraudCase.getTitle().isEmpty()) {
            fraudCase.setTitle("训练题目案例");
        }
        if (data.containsKey("type")) {
            fraudCase.setType(data.get("type").toString().toLowerCase(Locale.ROOT));
        } else if (fraudCase.getType() == null || fraudCase.getType().isEmpty()) {
            fraudCase.setType("sms");
        }
        if (data.containsKey("level")) {
            fraudCase.setLevel(data.get("level").toString().toLowerCase(Locale.ROOT));
        } else if (fraudCase.getLevel() == null || fraudCase.getLevel().isEmpty()) {
            fraudCase.setLevel("easy");
        }
        if (data.containsKey("hint")) {
            fraudCase.setHint(data.get("hint").toString());
        }
        if (data.containsKey("answer")) {
            fraudCase.setCorrectAnswer(data.get("answer").toString().toLowerCase(Locale.ROOT));
        } else if (fraudCase.getCorrectAnswer() == null || fraudCase.getCorrectAnswer().isEmpty()) {
            fraudCase.setCorrectAnswer("fraud");
        }
        
        // 更新可疑特征（从诈骗特征列表）
        if (data.containsKey("fraudFeatures")) {
            @SuppressWarnings("unchecked")
            List<String> fraudFeatures = (List<String>) data.get("fraudFeatures");
            try {
                fraudCase.setSuspiciousTags(objectMapper.writeValueAsString(fraudFeatures));
            } catch (Exception e) {
                return Result.failure(500, "更新失败：" + e.getMessage());
            }
        }
        
        // 设置启用标志
        if (fraudCase.getEnableFlag() == null) {
            fraudCase.setEnableFlag(1);
        }
        
        // 保存或更新
        if (fraudCase.getId() == null) {
            fraudCaseService.save(fraudCase);
        } else {
            fraudCaseService.updateById(fraudCase);
        }
        
        return Result.success();
    }

    @Operation(summary = "训练题目管理 - 更新")
    @PutMapping("/training/questions/{id}")
    public Result<Void> updateTrainingQuestion(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        FraudCase fraudCase = fraudCaseService.getById(id);
        if (fraudCase == null) {
            return Result.failure(404, "题目不存在");
        }
        
        // 更新内容
        if (data.containsKey("question")) {
            fraudCase.setContent(data.get("question").toString());
        }
        
        // 更新标题
        if (data.containsKey("title")) {
            fraudCase.setTitle(data.get("title").toString());
        }
        
        // 更新类型
        if (data.containsKey("type")) {
            fraudCase.setType(data.get("type").toString().toLowerCase(Locale.ROOT));
        }
        
        // 更新难度
        if (data.containsKey("level")) {
            fraudCase.setLevel(data.get("level").toString().toLowerCase(Locale.ROOT));
        }
        
        // 更新提示
        if (data.containsKey("hint")) {
            fraudCase.setHint(data.get("hint").toString());
        }
        
        // 更新正确答案
        if (data.containsKey("answer")) {
            fraudCase.setCorrectAnswer(data.get("answer").toString().toLowerCase(Locale.ROOT));
        }
        
        // 更新媒体URL
        if (data.containsKey("mediaUrl")) {
            fraudCase.setMediaUrl(data.get("mediaUrl").toString());
        }
        
        // 更新可疑特征
        if (data.containsKey("fraudFeatures")) {
            @SuppressWarnings("unchecked")
            List<String> fraudFeatures = (List<String>) data.get("fraudFeatures");
            try {
                fraudCase.setSuspiciousTags(objectMapper.writeValueAsString(fraudFeatures));
            } catch (Exception e) {
                return Result.failure(500, "更新失败：" + e.getMessage());
            }
        }
        
        fraudCaseService.updateById(fraudCase);
        return Result.success();
    }

    @Operation(summary = "训练题目管理 - 删除")
    @DeleteMapping("/training/questions/{id}")
    public Result<Void> deleteTrainingQuestion(@PathVariable Long id) {
        fraudCaseService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "训练题目管理 - 批量删除")
    @DeleteMapping("/training/questions/batch")
    public Result<Void> deleteTrainingQuestionsBatch(@RequestBody Map<String, List<Long>> data) {
        List<Long> ids = data.get("ids");
        if (ids != null && !ids.isEmpty()) {
            fraudCaseService.removeByIds(ids);
        }
        return Result.success();
    }

    @Operation(summary = "系统设置 - 获取")
    @GetMapping("/settings")
    public Result<Map<String, Object>> getSettings() {
        Map<String, Object> settings = new HashMap<>();
        
        // 风险等级阈值配置
        Map<String, Object> riskThresholds = new HashMap<>();
        riskThresholds.put("low", 30);
        riskThresholds.put("medium", 60);
        riskThresholds.put("high", 90);
        settings.put("riskThresholds", riskThresholds);
        
        // 经验值配置
        Map<String, Object> expConfig = new HashMap<>();
        expConfig.put("trainingExp", 10);
        expConfig.put("correctExp", 5);
        expConfig.put("assessmentExp", 20);
        settings.put("expConfig", expConfig);
        
        // 兼容前端直接使用的格式
        settings.put("lowRiskThreshold", 30);
        settings.put("mediumRiskThreshold", 60);
        settings.put("highRiskThreshold", 90);
        settings.put("expPerTraining", 10);
        settings.put("expPerCorrect", 5);
        settings.put("expPerAssessment", 20);
        
        return Result.success(settings);
    }

    @Operation(summary = "系统设置 - 更新")
    @PutMapping("/settings")
    public Result<Void> updateSettings(@RequestBody Map<String, Object> data) {
        // 验证阈值逻辑
        Integer lowRiskThreshold = null;
        Integer mediumRiskThreshold = null;
        Integer highRiskThreshold = null;
        
        if (data.containsKey("lowRiskThreshold")) {
            lowRiskThreshold = Integer.valueOf(data.get("lowRiskThreshold").toString());
        } else if (data.containsKey("riskThresholds")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> thresholds = (Map<String, Object>) data.get("riskThresholds");
            if (thresholds != null && thresholds.containsKey("low")) {
                lowRiskThreshold = Integer.valueOf(thresholds.get("low").toString());
            }
        }
        
        if (data.containsKey("mediumRiskThreshold")) {
            mediumRiskThreshold = Integer.valueOf(data.get("mediumRiskThreshold").toString());
        } else if (data.containsKey("riskThresholds")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> thresholds = (Map<String, Object>) data.get("riskThresholds");
            if (thresholds != null && thresholds.containsKey("medium")) {
                mediumRiskThreshold = Integer.valueOf(thresholds.get("medium").toString());
            }
        }
        
        if (data.containsKey("highRiskThreshold")) {
            highRiskThreshold = Integer.valueOf(data.get("highRiskThreshold").toString());
        } else if (data.containsKey("riskThresholds")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> thresholds = (Map<String, Object>) data.get("riskThresholds");
            if (thresholds != null && thresholds.containsKey("high")) {
                highRiskThreshold = Integer.valueOf(thresholds.get("high").toString());
            }
        }
        
        // 验证阈值逻辑
        if (lowRiskThreshold != null && mediumRiskThreshold != null && lowRiskThreshold >= mediumRiskThreshold) {
            return Result.failure(400, "低风险阈值必须小于中风险阈值");
        }
        if (mediumRiskThreshold != null && highRiskThreshold != null && mediumRiskThreshold >= highRiskThreshold) {
            return Result.failure(400, "中风险阈值必须小于高风险阈值");
        }
        
        // 注意：这里只是验证和返回成功，实际项目中应该将配置保存到数据库或配置文件
        // 由于没有系统配置表，这里仅做验证处理
        // 实际使用时可以创建系统配置表来持久化存储
        
        return Result.success();
    }
}
