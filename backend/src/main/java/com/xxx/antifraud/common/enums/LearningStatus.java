package com.xxx.antifraud.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 学习状态枚举
 */
@Getter
@AllArgsConstructor
public enum LearningStatus {
    READING("学习中"),
    FINISHED("已完成");

    private final String desc;
}

