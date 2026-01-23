package com.xxx.antifraud.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 训练题答案枚举
 */
@Getter
@AllArgsConstructor
public enum FraudAnswer {
    FRAUD("诈骗"),
    SAFE("正常");

    private final String desc;
}

