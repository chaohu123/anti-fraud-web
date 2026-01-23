package com.xxx.antifraud.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 风险等级枚举
 */
@Getter
@AllArgsConstructor
public enum RiskLevel {
    LOW("低风险"),
    MEDIUM("中风险"),
    HIGH("高风险");

    private final String desc;
}

