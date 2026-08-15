package com.blog.gateway.handler;

import com.blog.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@Slf4j
@RestControllerAdvice
public class GatewayExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Mono<Result<Void>> handle(Exception e) {
        log.error("[网关] 异常: ", e);
        return Mono.just(Result.error("网关异常: " + e.getMessage()));
    }
}