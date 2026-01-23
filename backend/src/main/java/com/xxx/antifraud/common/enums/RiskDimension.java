package com.xxx.antifraud.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 测评维度枚举
 *
 * INFO：信息保护意识（如隐私、链接、验证码等）
 * FINANCE：金融安全意识（如转账、理财、账户安全等）
 * PSYCH：心理风险倾向（如轻信、贪婪、恐惧等）
 */
@Getter
@AllArgsConstructor
public enum RiskDimension {
    INFO("信息保护意识"),
    FINANCE("金融安全意识"),
    PSYCH("心理风险倾向");

    private final String desc;
}

