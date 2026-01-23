package com.xxx.antifraud.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 诈骗案例类型枚举
 *
 * 与前端展示/筛选保持一致，便于扩展更多类型。
 */
@Getter
@AllArgsConstructor
public enum FraudCaseType {
    SMS("短信诈骗"),
    EMAIL("钓鱼邮件"),
    WEB("虚假网站"),
    CALL("冒充客服/金融诈骗"),
    OTHER("其他");

    private final String desc;
}

