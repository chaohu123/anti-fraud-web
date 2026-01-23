package com.xxx.antifraud.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 案例难度枚举
 */
@Getter
@AllArgsConstructor
public enum FraudCaseLevel {
    EASY("简单"),
    MEDIUM("中等"),
    HARD("困难");

    private final String desc;
}

