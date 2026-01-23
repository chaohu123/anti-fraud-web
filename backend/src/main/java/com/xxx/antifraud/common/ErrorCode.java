package com.xxx.antifraud.common;

import lombok.Getter;

/**
 * 通用业务错误码定义
 *
 * 方便在全局异常处理与 Result 中复用，论文中也可以单独说明。
 */
@Getter
public enum ErrorCode {

    SUCCESS(200, "成功"),

    BAD_REQUEST(400, "请求参数不合法"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有访问权限"),
    NOT_FOUND(404, "资源不存在"),

    BUSINESS_ERROR(1000, "业务异常"),
    USER_ALREADY_EXISTS(1001, "用户名已存在"),
    USER_NOT_FOUND(1002, "用户不存在"),
    PASSWORD_INCORRECT(1003, "密码错误"),

    ASSESSMENT_NOT_FOUND(2001, "测评记录不存在"),

    SYSTEM_ERROR(5000, "系统内部错误");

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    private final int code;
    private final String msg;
}

