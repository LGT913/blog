package com.blog.article.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.blog.article.feign.UserFeignClient;
import com.blog.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRemoteService {

    private final UserFeignClient userFeignClient;

    /**
     * Sentinel熔断资源标记，兜底方法 fallbackGetUser
     */
    @SentinelResource(value = "userServiceRemote", fallback = "fallbackGetUser")
    public Result<Map<String, Object>> getUserInfo(Long userId) {
        return userFeignClient.getUserById(userId);
    }

    /**
     * 熔断降级兜底方法
     * 要求：1. static静态 2. 参数与原方法一致 3. 末尾追加Throwable捕获异常
     */
    public static Result<Map<String, Object>> fallbackGetUser(Long userId, Throwable ex) {
        log.error("调用用户服务熔断降级，用户ID：{}，异常信息：{}", userId, ex.getMessage());
        return Result.error("用户服务暂时不可用，请稍后刷新评论");
    }
}
