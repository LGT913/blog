package com.blog.article.feign;


import com.blog.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "blog-user-service", fallback = UserFeignClientFallback.class)
public interface UserFeignClient {

    @GetMapping("/api/user/internal/{id}")
    Result<Map<String, Object>> getUserById(@PathVariable("id") Long id);
}
