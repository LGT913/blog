package com.blog.blog.common.exception;

import com.blog.blog.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 参数校验失败(如 @Valid 校验不通过)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("请求参数校验失败");
        return Result.error(msg);
    }

    // 缺少请求参数
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<String> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.error("缺少必要参数: " + e.getParameterName());
    }

    // 请求体不可读(JSON 格式错误等)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<String> handleMessageNotReadable(HttpMessageNotReadableException e) {
        return Result.error("请求数据格式错误");
    }

    // 数据完整性违反(如唯一约束冲突)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<String> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("数据完整性异常", e);
        return Result.error("数据冲突,请检查后重试");
    }

    // 参数非法(如 IllegalArgumentException)
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<String> handleIllegalArgument(IllegalArgumentException e) {
        return Result.error(e.getMessage());
    }

    // 业务异常(自定义运行时异常)
    @ExceptionHandler(RuntimeException.class)
    public Result<String> handleRuntimeException(RuntimeException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getMessage());
    }

    // 兜底:未知异常
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        log.error("服务器内部错误", e);
        return Result.error("服务器内部错误");
    }
}