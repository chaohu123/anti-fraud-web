package com.xxx.antifraud.controller;

import com.xxx.antifraud.common.Result;
import com.xxx.antifraud.entity.Carousel;
import com.xxx.antifraud.service.CarouselService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 轮播图前台接口（知识页 Banner 左侧轮播）
 */
@Tag(name = "轮播图")
@RestController
@RequestMapping("/api/carousel")
@RequiredArgsConstructor
public class CarouselController {

    private final CarouselService carouselService;

    @Operation(summary = "获取已启用的轮播列表")
    @GetMapping
    public Result<List<Carousel>> list() {
        return Result.success(carouselService.listEnabled());
    }
}
