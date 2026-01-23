package com.xxx.antifraud.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.antifraud.common.Result;
import com.xxx.antifraud.dto.fraudcase.AdminCaseCreateRequest;
import com.xxx.antifraud.dto.fraudcase.FraudCaseQueryRequest;
import com.xxx.antifraud.entity.FraudCase;
import com.xxx.antifraud.entity.User;
import com.xxx.antifraud.mapper.UserMapper;
import com.xxx.antifraud.service.FraudCaseService;
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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

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
    public Result<Void> createCase(@Valid @RequestBody AdminCaseCreateRequest request) throws Exception {
        FraudCase entity = new FraudCase();
        entity.setTitle("后台录入案例");
        entity.setType(request.getType().toLowerCase(Locale.ROOT));
        entity.setLevel("easy");
        entity.setContent(request.getContent());
        entity.setHint(request.getHint());
        entity.setCorrectAnswer(request.getAnswer().toLowerCase(Locale.ROOT));
        entity.setSuspiciousTags(objectMapper.writeValueAsString(List.of(request.getHint())));
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

    // ==================== 其他管理接口（占位实现）====================
    
    @Operation(summary = "知识库管理 - 列表")
    @GetMapping("/knowledge")
    public Result<PageResultVO<Map<String, Object>>> listKnowledge(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<Map<String, Object>> pageResult = new Page<>(page, size);
        pageResult.setTotal(0);
        pageResult.setRecords(List.of());
        return Result.success(PageResultVO.from(pageResult));
    }

    @Operation(summary = "知识库管理 - 创建")
    @PostMapping("/knowledge")
    public Result<Void> createKnowledge(@RequestBody Map<String, Object> data) {
        return Result.success();
    }

    @Operation(summary = "知识库管理 - 更新")
    @PutMapping("/knowledge/{id}")
    public Result<Void> updateKnowledge(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        return Result.success();
    }

    @Operation(summary = "知识库管理 - 删除")
    @DeleteMapping("/knowledge/{id}")
    public Result<Void> deleteKnowledge(@PathVariable Long id) {
        return Result.success();
    }

    @Operation(summary = "知识库管理 - 批量删除")
    @DeleteMapping("/knowledge/batch")
    public Result<Void> deleteKnowledgeBatch(@RequestBody Map<String, List<Long>> data) {
        return Result.success();
    }

    @Operation(summary = "成就管理 - 列表")
    @GetMapping("/achievements")
    public Result<PageResultVO<Map<String, Object>>> listAchievements(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<Map<String, Object>> pageResult = new Page<>(page, size);
        pageResult.setTotal(0);
        pageResult.setRecords(List.of());
        return Result.success(PageResultVO.from(pageResult));
    }

    @Operation(summary = "成就管理 - 创建")
    @PostMapping("/achievements")
    public Result<Void> createAchievement(@RequestBody Map<String, Object> data) {
        return Result.success();
    }

    @Operation(summary = "成就管理 - 批量删除")
    @DeleteMapping("/achievements/batch")
    public Result<Void> deleteAchievementsBatch(@RequestBody Map<String, List<Long>> data) {
        return Result.success();
    }

    @Operation(summary = "测评问题管理 - 列表")
    @GetMapping("/assessment/questions")
    public Result<PageResultVO<Map<String, Object>>> listAssessmentQuestions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<Map<String, Object>> pageResult = new Page<>(page, size);
        pageResult.setTotal(0);
        pageResult.setRecords(List.of());
        return Result.success(PageResultVO.from(pageResult));
    }

    @Operation(summary = "测评问题管理 - 创建")
    @PostMapping("/assessment/questions")
    public Result<Void> createAssessmentQuestion(@RequestBody Map<String, Object> data) {
        return Result.success();
    }

    @Operation(summary = "测评问题管理 - 批量删除")
    @DeleteMapping("/assessment/questions/batch")
    public Result<Void> deleteAssessmentQuestionsBatch(@RequestBody Map<String, List<Long>> data) {
        return Result.success();
    }

    @Operation(summary = "训练题目管理 - 列表")
    @GetMapping("/training/questions")
    public Result<PageResultVO<Map<String, Object>>> listTrainingQuestions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<Map<String, Object>> pageResult = new Page<>(page, size);
        pageResult.setTotal(0);
        pageResult.setRecords(List.of());
        return Result.success(PageResultVO.from(pageResult));
    }

    @Operation(summary = "训练题目管理 - 创建")
    @PostMapping("/training/questions")
    public Result<Void> createTrainingQuestion(@RequestBody Map<String, Object> data) {
        return Result.success();
    }

    @Operation(summary = "训练题目管理 - 批量删除")
    @DeleteMapping("/training/questions/batch")
    public Result<Void> deleteTrainingQuestionsBatch(@RequestBody Map<String, List<Long>> data) {
        return Result.success();
    }

    @Operation(summary = "系统设置 - 获取")
    @GetMapping("/settings")
    public Result<Map<String, Object>> getSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("riskThresholds", Map.of(
                "low", 40,
                "medium", 70,
                "high", 100
        ));
        settings.put("expConfig", Map.of(
                "trainingExp", 10,
                "correctExp", 20,
                "assessmentExp", 30
        ));
        return Result.success(settings);
    }

    @Operation(summary = "系统设置 - 更新")
    @PutMapping("/settings")
    public Result<Void> updateSettings(@RequestBody Map<String, Object> data) {
        return Result.success();
    }
}
