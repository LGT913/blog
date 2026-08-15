package com.blog.article.feign;

import com.blog.common.result.Result;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class UserFeignClientFallback implements UserFeignClient {
    @Override
    public Result<Map<String, Object>> getUserById(Long id) {
        // 降级：返回匿名用户，保证评论列表不因 user 服务宕机而 500
        return Result.success(Map.of("id", id, "username", "匿名", "nickname", "匿名"));
    }
}