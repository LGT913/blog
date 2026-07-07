package com.blog.blog.common;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 捕获所有 RuntimeException（包括我们抛出的“用户名已存在”、“密码错误”等）
    @ExceptionHandler(RuntimeException.class)
    public Result<String> handleRuntimeException(RuntimeException e) {
        return Result.error(e.getMessage());
    }

    // 捕获其他所有未知异常
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        return Result.error("服务器内部错误");
    }
}
