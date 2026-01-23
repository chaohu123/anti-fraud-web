package com.xxx.antifraud.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxx.antifraud.common.Result;
import com.xxx.antifraud.dto.fraudcase.FraudCaseQueryRequest;
import com.xxx.antifraud.service.FraudCaseService;
import com.xxx.antifraud.vo.fraudcase.FraudCaseDetailVO;
import com.xxx.antifraud.vo.fraudcase.FraudCaseSimpleVO;
import com.xxx.antifraud.vo.fraudcase.TrainCaseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 诈骗案例库模块 Controller
 */
@Tag(name = "诈骗案例库模块")
@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
public class FraudCaseController {

    private final FraudCaseService fraudCaseService;

    @Operation(summary = "训练页：查询诈骗案例列表（兼容前端 mock 协议，支持类型、难度筛选）")
    @GetMapping
    public Result<List<TrainCaseVO>> list(String type, String level) {
        return Result.success(fraudCaseService.listTrainCases(type, level));
    }

    @Operation(summary = "管理/扩展：分页查询诈骗案例列表（支持类型、难度筛选）")
    @GetMapping("/page")
    public Result<Page<FraudCaseSimpleVO>> page(FraudCaseQueryRequest request) {
        return Result.success(fraudCaseService.pageQuery(request));
    }

    @Operation(summary = "随机获取一个诈骗案例（可按类型过滤）")
    @GetMapping("/random")
    public Result<FraudCaseDetailVO> random(String type) {
        return Result.success(fraudCaseService.getRandomCase(type));
    }

    @Operation(summary = "查询诈骗案例详情")
    @GetMapping("/{id}")
    public Result<FraudCaseDetailVO> detail(@PathVariable Long id) {
        return Result.success(fraudCaseService.getDetail(id));
    }
}

