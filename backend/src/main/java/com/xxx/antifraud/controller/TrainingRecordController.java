package com.xxx.antifraud.controller;

import com.xxx.antifraud.common.Result;
import com.xxx.antifraud.dto.train.TrainingSubmitRequest;
import com.xxx.antifraud.service.TrainingRecordService;
import com.xxx.antifraud.vo.train.TrainingStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 交互式识别训练记录模块 Controller
 */
@Tag(name = "训练记录模块")
@RestController
@RequestMapping("/api/train")
@RequiredArgsConstructor
public class TrainingRecordController {

    private final TrainingRecordService trainingRecordService;

    @Operation(summary = "提交训练结果")
    @PostMapping("/records")
    public Result<Void> submit(@Valid @RequestBody TrainingSubmitRequest request) {
        trainingRecordService.submit(request);
        return Result.success();
    }

    @Operation(summary = "查询用户训练统计数据")
    @GetMapping("/stats/{userId}")
    public Result<TrainingStatsVO> stats(@PathVariable Long userId) {
        return Result.success(trainingRecordService.getStats(userId));
    }
}

