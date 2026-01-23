package com.xxx.antifraud.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口返回结果包装类
 *
 * 所有 REST 接口均返回 Result，便于前后端分离与错误统一处理。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统一返回结果包装")
public class Result<T> {

    @Schema(description = "是否成功")
    private boolean success;

    @Schema(description = "业务状态码，200 表示成功")
    private Integer code;

    @Schema(description = "错误或提示信息")
    private String message;

    @Schema(description = "业务数据载体")
    private T data;

    public static <T> Result<T> success(T data) {
        return new Result<>(true, ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), data);
    }

    public static Result<Void> success() {
        return new Result<>(true, ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), null);
    }

    public static <T> Result<T> failure(ErrorCode errorCode) {
        return new Result<>(false, errorCode.getCode(), errorCode.getMsg(), null);
    }

    public static <T> Result<T> failure(Integer code, String message) {
        return new Result<>(false, code, message, null);
    }
}

