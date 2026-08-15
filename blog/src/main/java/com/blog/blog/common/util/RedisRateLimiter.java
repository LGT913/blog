package com.blog.blog.common.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class RedisRateLimiter {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // Lua 脚本:令牌桶算法(原子操作,无并发问题)
    // KEYS[1] = 限流 key, ARGV[1] = 桶容量, ARGV[2] = 每秒生成令牌数, ARGV[3] = 当前时间戳(秒)
    private static final String TOKEN_BUCKET_SCRIPT =
            "local capacity = tonumber(ARGV[1])\n" +
                    "local rate = tonumber(ARGV[2])\n" +
                    "local now = tonumber(ARGV[3])\n" +
                    "local key = KEYS[1]\n" +
                    "local data = redis.call('HMGET', key, 'tokens', 'lastTime')\n" +
                    "local tokens = tonumber(data[1]) or capacity\n" +
                    "local lastTime = tonumber(data[2]) or now\n" +
                    "local delta = math.max(0, now - lastTime)\n" +
                    "tokens = math.min(capacity, tokens + delta * rate)\n" +
                    "if tokens >= 1 then\n" +
                    "  tokens = tokens - 1\n" +
                    "  redis.call('HMSET', key, 'tokens', tokens, 'lastTime', now)\n" +
                    "  redis.call('EXPIRE', key, 86400)\n" +
                    "  return 1\n" +
                    "else\n" +
                    "  return 0\n" +
                    "end";

    /**
     * 尝试获取令牌
     * @param key      限流维度(如 "deepseek:summary")
     * @param capacity 令牌桶容量(瞬时最大突发)
     * @param rate     每秒生成令牌数(长期平均速率)
     * @return true=获取到,允许请求;false=限流,拒绝请求
     */
    public boolean tryAcquire(String key, int capacity, int rate) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(TOKEN_BUCKET_SCRIPT, Long.class);
        Long nowSeconds = System.currentTimeMillis() / 1000;
        Long result = stringRedisTemplate.execute(script,
                Collections.singletonList(key),
                String.valueOf(capacity),
                String.valueOf(rate),
                String.valueOf(nowSeconds));
        return Long.valueOf(1L).equals(result);
    }
}