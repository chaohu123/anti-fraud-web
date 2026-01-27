package com.xxx.antifraud.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 学习状态枚举
 */
@Getter
@AllArgsConstructor
public enum LearningStatus {
    NOT_STARTED(0, "未开始"),
    READING(1, "学习中"),
    FINISHED(2, "已完成");

    /**
     * 对应数据库中的数值状态：
     * 0 未开始 / 1 学习中 / 2 已完成
     */
    private final int code;
    private final String desc;
}

