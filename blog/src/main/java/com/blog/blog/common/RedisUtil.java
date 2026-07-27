package com.blog.blog.common;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

     //存入缓存（无过期时间）
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

     //存入缓存并设置过期时间
     //@param timeout 过期时间（单位：秒）
    public void set(String key, Object value, long timeout) {
        redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
    }

     //从缓存取值
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    //删除缓存
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    //判断 key 是否存在
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    //分布式锁：setIfAbsent（不存在才写入），返回是否加锁成功
    public Boolean setIfAbsent(String key, Object value, long timeout) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, TimeUnit.SECONDS);
    }

    //模糊删除：用 SCAN 遍历匹配的 key 并删除（避免 KEYS 阻塞）
    public void deleteByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    //执行 Lua 脚本（用于安全释放分布式锁）
    public <T> T executeLua(DefaultRedisScript<T> script, String key, Object... args) {
        return redisTemplate.execute(script, Collections.singletonList(key), args);
    }

    // RedisUtil.java 新增，setIfAbsent 和 executeLua，tryLock 和 unlock 对这些能力的封装
    public  boolean tryLock(String key, String value, long timeout) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue()
                .setIfAbsent(key, value, timeout, TimeUnit.SECONDS));
    }

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    public boolean unlock(String key, String value) {
        Long result = redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), value);
        return Long.valueOf(1).equals(result);
    }
}
