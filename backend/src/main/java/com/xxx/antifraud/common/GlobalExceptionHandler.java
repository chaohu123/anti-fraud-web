package com.xxx.antifraud.common;

import javax.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * 将常见异常统一封装为 Result，避免堆栈信息直接暴露给前端。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.failure(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    public Result<Void> handleValidationException(Exception e) {
        String msg;
        if (e instanceof MethodArgumentNotValidException ex) {
            msg = ex.getBindingResult().getAllErrors().stream()
                    .findFirst()
                    .map(objectError -> objectError.getDefaultMessage())
                    .orElse(ErrorCode.BAD_REQUEST.getMsg());
        } else if (e instanceof BindException ex) {
            msg = ex.getBindingResult().getAllErrors().stream()
                    .findFirst()
                    .map(objectError -> objectError.getDefaultMessage())
                    .orElse(ErrorCode.BAD_REQUEST.getMsg());
        } else {
            msg = e.getMessage();
        }
        return Result.failure(ErrorCode.BAD_REQUEST.getCode(), msg);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        return Result.failure(ErrorCode.BAD_REQUEST.getCode(), "请求体格式不正确或缺失必填字段");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMethod());
        return Result.failure(ErrorCode.BAD_REQUEST.getCode(), 
                String.format("请求方法 %s 不支持，支持的方法: %s", 
                        e.getMethod(), 
                        e.getSupportedMethods() != null ? String.join(", ", e.getSupportedMethods()) : "未知"));
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOtherException(Exception e) {
        // 记录异常日志，便于排查问题
        log.error("系统内部错误", e);
        return Result.failure(ErrorCode.SYSTEM_ERROR);
    }
}

