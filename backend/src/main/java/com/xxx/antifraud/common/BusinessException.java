package com.xxx.antifraud.common;

import lombok.Getter;

/**
 * 自定义业务异常
 *
 * 用于在 Service 层中显式抛出可预期的业务错误，
 * 由全局异常处理器统一转换为友好的 Result 返回。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}

