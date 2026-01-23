package com.xxx.antifraud.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 测评题型枚举
 */
@Getter
@AllArgsConstructor
public enum QuestionType {
    SINGLE("单选"),
    MULTI("多选");

    private final String desc;
}

